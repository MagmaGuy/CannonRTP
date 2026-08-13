package com.magmaguy.cannonrtp.services;

import com.magmaguy.cannonrtp.config.CannonRTPConfigFields;
import com.magmaguy.cannonrtp.config.CannonMessagesConfig;
import com.magmaguy.cannonrtp.config.DefaultConfig;
import com.magmaguy.cannonrtp.config.LandingSearchConfig;
import com.magmaguy.freeminecraftmodels.api.ModeledEntityManager;
import com.magmaguy.freeminecraftmodels.customentity.StaticEntity;
import com.magmaguy.magmacore.util.ChatColorConverter;
import com.magmaguy.magmacore.util.ConfigurationLocation;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;

import java.util.ArrayDeque;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ConfiguredCannonRTP {
    private static final int FMM_MODEL_RETRY_INTERVAL_TICKS = 20;
    /**
     * The configuration file's id (filename without .yml). Multiple ConfiguredCannonRTP
     * instances share the same configId when one config drives several in-world
     * placements. Used by API events and command lookups.
     */
    @Getter
    private final String configId;
    /**
     * Unique id for this specific in-world placement. Formatted "{configId}#{index}"
     * where index matches the entry in {@link CannonRTPConfigFields#getCannonLocations()}.
     */
    @Getter
    private final String instanceId;
    @Getter
    private final CannonRTPConfigFields configFields;
    /**
     * The serialized location string that produced this instance. Kept so removal
     * commands can match the exact list entry that spawned this cannon.
     */
    @Getter
    private final String locationString;
    @Getter
    private final ArrayDeque<Location> queuedLocations = new ArrayDeque<>();
    private final Map<SearchFailureReason, Integer> failures = new EnumMap<>(SearchFailureReason.class);
    private final Map<UUID, Long> lastNotifyMs = new java.util.HashMap<>();
    private final LaunchCancellationBackoffTracker launchCancellationBackoffs = new LaunchCancellationBackoffTracker();
    private final LaunchTriggerLatch launchTriggerLatch = new LaunchTriggerLatch();

    @Getter
    private CannonSearchState searchState = CannonSearchState.SEARCHING;
    private int searchAttempts = 0;
    @Getter
    private String lastStatusDetail = "Still searching.";
    private TextDisplay labelDisplay;
    private StaticEntity staticModel;
    private String lastRawLabelText = "";

    @Getter
    private Location cannonLocation;
    private boolean chunkLoaded;
    private boolean cannonWorldLoaded;
    private World cachedTargetWorld;
    private int cannonChunkX;
    private int cannonChunkZ;
    private boolean targetWorldUnavailable;

    private Boolean cachedUseCustomModel;
    private String cachedResolvedModelName;
    private boolean lastLabelHasModel = false;
    private boolean fmmModelRecreationPending;
    private int fmmModelRetryTicks;

    public ConfiguredCannonRTP(String configId,
                               String instanceId,
                               CannonRTPConfigFields configFields,
                               Location cannonLocation,
                               String locationString) {
        this.configId = configId;
        this.instanceId = instanceId;
        this.configFields = configFields;
        this.locationString = locationString;
        this.cannonLocation = cannonLocation;
        if (cannonLocation != null && cannonLocation.getWorld() != null) {
            this.cannonWorldLoaded = true;
            this.cannonChunkX = cannonLocation.getBlockX() >> 4;
            this.cannonChunkZ = cannonLocation.getBlockZ() >> 4;
            this.chunkLoaded = cannonLocation.getWorld().isChunkLoaded(cannonChunkX, cannonChunkZ);
        }
    }

    public String getCannonWorldName() {
        return cannonLocation != null && cannonLocation.getWorld() != null
                ? cannonLocation.getWorld().getName()
                : null;
    }

    /**
     * The world this cannon actually teleports to: the configured target world, or
     * this cannon's own world when no explicit target is set.
     */
    public String getEffectiveTargetWorldName() {
        String configured = configFields.getTargetWorldName();
        return configured == null || configured.isBlank() ? getCannonWorldName() : configured;
    }

    public int getCannonChunkX() {
        return cannonChunkX;
    }

    public int getCannonChunkZ() {
        return cannonChunkZ;
    }

    public void setChunkLoaded(boolean loaded) {
        this.chunkLoaded = loaded;
    }

    /**
     * Re-parses {@link #locationString} so the stored {@link Location} picks up the
     * freshly loaded world. Called by the world-lifecycle listener when a world
     * this cannon is bound to comes online after the plugin finished initializing.
     */
    public void refreshWorldReference() {
        Location newLocation = ConfigurationLocation.serialize(locationString);
        if (newLocation == null || newLocation.getWorld() == null) {
            cannonWorldLoaded = false;
            chunkLoaded = false;
            return;
        }
        cannonLocation = newLocation;
        cannonWorldLoaded = true;
        cannonChunkX = newLocation.getBlockX() >> 4;
        cannonChunkZ = newLocation.getBlockZ() >> 4;
        chunkLoaded = newLocation.getWorld().isChunkLoaded(cannonChunkX, cannonChunkZ);
    }

    public void notifyCannonWorldUnloaded() {
        cannonWorldLoaded = false;
        chunkLoaded = false;
    }

    private void invalidateCachedTargetWorld() {
        cachedTargetWorld = null;
    }

    /**
     * Removes destinations backed by an unloading Bukkit world instance. This
     * applies even when this cannon is physically placed in another world.
     */
    public int purgeQueuedLocationsForWorld(String worldName) {
        int before = queuedLocations.size();
        queuedLocations.removeIf(location -> location.getWorld() != null
                && worldName.equals(location.getWorld().getName()));
        int removed = before - queuedLocations.size();
        if (removed > 0) {
            restartSearchWindow();
        }
        return removed;
    }

    boolean isTargetWorldUnavailable() {
        return targetWorldUnavailable;
    }

    public void markTargetWorldUnavailable(String worldName) {
        targetWorldUnavailable = true;
        invalidateCachedTargetWorld();
        purgeQueuedLocationsForWorld(worldName);
        failures.clear();
        searchAttempts = 0;
        searchState = CannonSearchState.SEARCHING;
        lastStatusDetail = "Target world " + worldName + " is not loaded.";
    }

    public void notifyTargetWorldLoaded() {
        invalidateCachedTargetWorld();
        if (!targetWorldUnavailable) {
            return;
        }
        targetWorldUnavailable = false;
        restartSearchWindow();
    }

    /**
     * Unified teardown for this instance. Visuals are always removed; other side-effects
     * depend on the reason (chunk unload vs. world unload vs. reload vs. shutdown).
     */
    public void remove(RemovalReason reason) {
        removeVisuals();
        lastNotifyMs.clear();
        launchCancellationBackoffs.clear();
        launchTriggerLatch.clear();
        switch (reason) {
            case CHUNK_UNLOAD -> chunkLoaded = false;
            case WORLD_UNLOAD -> {
                // A queued destination holds a live Bukkit World reference. After a world
                // unload that reference points at a detached CraftWorld instance, so the
                // reserve is dropped and rebuilt once this cannon is active again.
                // CHUNK_UNLOAD deliberately retains the queue: the same World instance
                // stays live, so queued destinations remain valid.
                queuedLocations.clear();
                cannonWorldLoaded = false;
                chunkLoaded = false;
            }
            case RELOAD, SHUTDOWN -> queuedLocations.clear();
        }
        restartSearchWindow();
    }

    public boolean shouldNotify(Player player, long throttleMs) {
        long now = System.currentTimeMillis();
        Long last = lastNotifyMs.get(player.getUniqueId());
        if (last != null && now - last < throttleMs) return false;
        lastNotifyMs.put(player.getUniqueId(), now);
        return true;
    }

    public void clearNotifyThrottle(Player player) {
        lastNotifyMs.remove(player.getUniqueId());
    }

    public void clearNotifyThrottle(UUID playerId) {
        lastNotifyMs.remove(playerId);
    }

    boolean isLaunchCancellationBackoffActive(UUID playerId, long nowNanos) {
        return launchCancellationBackoffs.isBlocked(playerId, nowNanos);
    }

    void recordLaunchCancellation(UUID playerId, long nowNanos) {
        launchCancellationBackoffs.recordCancellation(playerId, nowNanos);
    }

    void clearLaunchCancellationBackoff(UUID playerId) {
        launchCancellationBackoffs.clear(playerId);
    }

    boolean hasLaunchCancellationBackoffs() {
        return !launchCancellationBackoffs.isEmpty();
    }

    void retainLaunchCancellationBackoffs(Set<UUID> playersInsideTrigger) {
        launchCancellationBackoffs.retainPlayers(playersInsideTrigger);
    }

    boolean isLaunchTriggerLatched(UUID playerId) {
        return launchTriggerLatch.isLatched(playerId);
    }

    Set<UUID> getLatchedLaunchTriggerPlayers() {
        return launchTriggerLatch.snapshot();
    }

    void latchLaunchTrigger(UUID playerId) {
        launchTriggerLatch.latch(playerId);
    }

    void observeLaunchTriggerPosition(
            UUID playerId,
            boolean insideTrigger,
            boolean launchActive
    ) {
        launchTriggerLatch.observePosition(
                playerId,
                insideTrigger,
                launchActive);
    }

    void releaseLaunchTrigger(UUID playerId) {
        launchTriggerLatch.release(playerId);
    }

    /**
     * A cannon participates in the manager tick loop only when it is enabled, valid,
     * has a resolvable location, and its world and chunk are both loaded.
     */
    public boolean isActive() {
        return isEnabled()
                && cannonLocation != null
                && cannonWorldLoaded
                && chunkLoaded;
    }

    public String getDisplayName() {
        return configFields.getDisplayName();
    }

    public World getTargetWorld() {
        if (cachedTargetWorld != null) {
            return cachedTargetWorld;
        }
        String targetWorldName = configFields.getTargetWorldName();
        if (targetWorldName == null || targetWorldName.isBlank()) {
            // Fall back to this cannon's own world when no explicit target is set.
            cachedTargetWorld = cannonLocation != null ? cannonLocation.getWorld() : null;
            return cachedTargetWorld;
        }
        World resolved = Bukkit.getWorld(targetWorldName);
        if (resolved != null) {
            cachedTargetWorld = resolved;
        }
        return resolved;
    }

    public Location getResolvedSearchCenter() {
        Location configuredCenter = configFields.getSearchCenter();
        World targetWorld = getTargetWorld();
        if (targetWorld == null) {
            return null;
        }
        if (configuredCenter == null) {
            // No explicit search center — radiate out from this cannon's location if it
            // is in the target world, otherwise use the target world's spawn.
            if (cannonLocation != null && cannonLocation.getWorld() != null
                    && cannonLocation.getWorld().equals(targetWorld)) {
                return cannonLocation.clone();
            }
            return targetWorld.getSpawnLocation();
        }
        if (configuredCenter.getWorld() != null && configuredCenter.getWorld().equals(targetWorld)) {
            return configuredCenter.clone();
        }
        return new Location(targetWorld, configuredCenter.getX(), configuredCenter.getY(), configuredCenter.getZ(), configuredCenter.getYaw(), configuredCenter.getPitch());
    }

    public boolean isEnabled() {
        return configFields.isEnabled();
    }

    public boolean canUse(Player player) {
        return configFields.getRequiredPermission().isBlank() || player.hasPermission(configFields.getRequiredPermission());
    }

    public boolean needsMoreLocations() {
        return queuedLocations.size() < LandingSearchConfig.getPreloadedLocationsPerCannon();
    }

    public boolean isCharged() {
        return queuedLocations.size() >= LandingSearchConfig.getChargedLocationsPerCannon();
    }

    public void handleChunkLoad() {
        if (needsMoreLocations()) {
            restartSearchWindow();
        }
    }

    private void resolveModelCache() {
        String configModel = configFields.getCustomModel();
        if (configModel != null && !configModel.isBlank()
                && Bukkit.getPluginManager().isPluginEnabled("FreeMinecraftModels")
                && ModeledEntityManager.modelExists(configModel)) {
            cachedResolvedModelName = configModel;
            cachedUseCustomModel = Boolean.TRUE;
            return;
        }
        String preferred = resolvePreferredModel();
        if (!preferred.isBlank()) {
            cachedResolvedModelName = preferred;
            cachedUseCustomModel = Boolean.TRUE;
        } else {
            cachedResolvedModelName = null;
            cachedUseCustomModel = Boolean.FALSE;
        }
    }

    private String resolvePreferredModel() {
        if (!Bukkit.getPluginManager().isPluginEnabled("FreeMinecraftModels")) {
            return "";
        }
        for (String modelName : DefaultConfig.getCannonModelPriority()) {
            if (modelName != null && !modelName.isBlank() && ModeledEntityManager.modelExists(modelName)) {
                return modelName;
            }
        }
        return "";
    }

    public void invalidateModelCache() {
        cachedUseCustomModel = null;
        cachedResolvedModelName = null;
    }

    /**
     * FMM tears down every StaticEntity during an imported-content reload while
     * consumer references remain non-null. Drop that stale reference so the next
     * label refresh resolves the rebuilt registry and creates a fresh model.
     */
    public void resetModelAfterFmmReload() {
        if (staticModel != null) {
            staticModel.remove();
            staticModel = null;
        }
        invalidateModelCache();
        fmmModelRecreationPending = true;
        fmmModelRetryTicks = 0;
    }

    public boolean hasTimedOut() {
        return searchAttempts >= LandingSearchConfig.getSearchTimeoutAttempts();
    }

    public int getAttemptsRemaining() {
        return Math.max(0, LandingSearchConfig.getSearchTimeoutAttempts() - searchAttempts);
    }

    public void markSearchFailure(SearchFailureReason reason) {
        failures.merge(reason, 1, Integer::sum);
        searchAttempts++;
        searchState = CannonSearchState.SEARCHING;
    }

    /**
     * As {@link #markSearchFailure(SearchFailureReason)}, additionally surfacing the
     * rejecting plugin's reason in the admin-facing status detail when one was provided.
     */
    public void markSearchFailure(SearchFailureReason reason, String rejectionReason) {
        markSearchFailure(reason);
        if (rejectionReason != null && !rejectionReason.isBlank()
                && searchState == CannonSearchState.SEARCHING) {
            lastStatusDetail = "Rejected by another plugin: " + rejectionReason;
        }
    }

    public void markSearchSuccess(Location location) {
        targetWorldUnavailable = false;
        queuedLocations.add(location);
        searchState = CannonSearchState.SEARCHING;
        if (!needsMoreLocations()) {
            lastStatusDetail = "Safe locations preloaded.";
        } else {
            lastStatusDetail = isCharged() ? "Ready." : "Maintaining destination reserve.";
        }
    }

    public void exhaustSearch() {
        searchState = CannonSearchState.EXHAUSTED;
        lastStatusDetail = buildFailureSummary();
    }

    public Location consumeQueuedLocation() {
        Location location = queuedLocations.poll();
        if (location != null && needsMoreLocations()) {
            restartSearchWindow();
        }
        return location;
    }

    /**
     * Returns a queued location to the head of the deque. Used when a launch is cancelled
     * via {@link com.magmaguy.cannonrtp.api.CannonRTPLaunchEvent} so the destination is
     * reused on the next launch rather than discarded.
     */
    public void returnQueuedLocation(Location location) {
        if (location != null) queuedLocations.offerFirst(location);
    }

    private void restartSearchWindow() {
        failures.clear();
        searchAttempts = 0;
        searchState = CannonSearchState.SEARCHING;
        lastStatusDetail = queuedLocations.isEmpty()
                ? "Charging teleport destinations."
                : "Maintaining destination reserve.";
    }

    public String buildFailureSummary() {
        if (failures.isEmpty()) {
            return "Every sampled location failed validation.";
        }
        StringBuilder summary = new StringBuilder();
        appendSummary(summary, SearchFailureReason.OUTSIDE_WORLD_BORDER, "most samples landed outside the world border");
        appendSummary(summary, SearchFailureReason.NO_SAFE_SURFACE, "many samples had no safe surface");
        appendSummary(summary, SearchFailureReason.HAZARDOUS_TERRAIN, "many samples were hazardous to land on");
        appendSummary(summary, SearchFailureReason.PROTECTED_LAND, "most samples were inside protected land");
        appendSummary(summary, SearchFailureReason.API_REJECTED, "many samples were rejected by another plugin");
        return summary.length() == 0 ? "Every sampled location failed validation." : summary.toString();
    }

    private void appendSummary(StringBuilder summary, SearchFailureReason reason, String text) {
        if (!failures.containsKey(reason) || failures.get(reason) <= 0) {
            return;
        }
        if (!summary.isEmpty()) {
            summary.append(", ");
        }
        summary.append(text);
    }

    public String getStatusDisplay() {
        if (!isEnabled()) return CannonMessagesConfig.getStatusDisabledLabel();
        return switch (getEffectiveSearchState()) {
            case READY -> CannonMessagesConfig.getStatusReadyLabel();
            case SEARCHING -> queuedLocations.isEmpty()
                    ? CannonMessagesConfig.getStatusChargingLabel()
                    : CannonMessagesConfig.getStatusMaintainingLabel();
            case EXHAUSTED -> CannonMessagesConfig.getStatusExhaustedLabel();
        };
    }

    public void refreshLabel() {
        // Sole caller (CannonRTPManager.tickAll) already skips instances whose
        // location or world is unresolved.
        Location loc = getCannonLocation();
        boolean modelInitializationAllowed = reconcileFmmModelAfterReload();

        if (shouldUseCustomModel()) {
            if (staticModel == null && modelInitializationAllowed) {
                initializeCustomModel();
            }
        } else if (staticModel != null) {
            staticModel.remove();
            staticModel = null;
        }

        boolean freshlySpawned = false;
        if (labelDisplay == null || !labelDisplay.isValid()) {
            spawnLabel(loc);
            freshlySpawned = true;
        }
        if (labelDisplay == null || !labelDisplay.isValid()) {
            return;
        }

        boolean modelPresent = staticModel != null;
        if (!freshlySpawned && modelPresent != lastLabelHasModel) {
            labelDisplay.teleport(getLabelLocation(loc));
        }
        lastLabelHasModel = modelPresent;

        String rawLabelText = buildRawLabelText();
        if (!rawLabelText.equals(lastRawLabelText)) {
            lastRawLabelText = rawLabelText;
            labelDisplay.setText(ChatColorConverter.convert(rawLabelText));
        }
    }

    public void removeVisuals() {
        if (labelDisplay != null && labelDisplay.isValid()) {
            labelDisplay.remove();
        }
        labelDisplay = null;
        if (staticModel != null) {
            staticModel.remove();
            staticModel = null;
        }
        lastRawLabelText = "";
        lastLabelHasModel = false;
    }

    private void spawnLabel(Location loc) {
        Location labelLocation = getLabelLocation(loc);
        String rawLabelText = buildRawLabelText();
        String labelText = ChatColorConverter.convert(rawLabelText);
        labelDisplay = labelLocation.getWorld().spawn(labelLocation, TextDisplay.class, display -> {
            display.setText(labelText);
            display.setPersistent(false);
            display.setBillboard(Display.Billboard.CENTER);
            display.setShadowed(false);
            display.setAlignment(TextDisplay.TextAlignment.CENTER);
        });
        lastRawLabelText = rawLabelText;
    }

    private Location getLabelLocation(Location loc) {
        return loc.clone().add(0, staticModel != null ? 2.0 : 1.2, 0);
    }

    private String buildRawLabelText() {
        return getDisplayName() + "\n&7" + getStatusDisplay();
    }

    public boolean hasActiveModel() {
        return staticModel != null;
    }

    private boolean shouldUseCustomModel() {
        if (cachedUseCustomModel == null) {
            resolveModelCache();
        }
        return cachedUseCustomModel == Boolean.TRUE;
    }

    private void initializeCustomModel() {
        if (cachedUseCustomModel == null) {
            resolveModelCache();
        }
        if (cachedUseCustomModel != Boolean.TRUE || cachedResolvedModelName == null) {
            return;
        }
        Location loc = getCannonLocation();
        if (loc == null || loc.getWorld() == null) {
            return;
        }
        staticModel = StaticEntity.create(cachedResolvedModelName, loc.clone());
        if (staticModel != null) {
            fmmModelRecreationPending = false;
            fmmModelRetryTicks = 0;
        } else if (fmmModelRecreationPending) {
            fmmModelRetryTicks = FMM_MODEL_RETRY_INTERVAL_TICKS;
        }
    }

    /**
     * FMM 2.4.0 predates FmmReloadedEvent but exposes ModeledEntity#isRemoved().
     * Detect its stale object after an imported-content reload, then re-probe the
     * rebuilt model registry at a bounded cadence until the model is available.
     */
    private boolean reconcileFmmModelAfterReload() {
        if (staticModel != null && staticModel.isRemoved()) {
            staticModel = null;
            invalidateModelCache();
            fmmModelRecreationPending = true;
            fmmModelRetryTicks = 0;
        }
        if (!fmmModelRecreationPending || staticModel != null) return true;
        if (!Bukkit.getPluginManager().isPluginEnabled("FreeMinecraftModels")) return false;
        if (fmmModelRetryTicks > 0) {
            fmmModelRetryTicks--;
            if (fmmModelRetryTicks > 0) return false;
        }

        invalidateModelCache();
        fmmModelRetryTicks = FMM_MODEL_RETRY_INTERVAL_TICKS;
        return true;
    }

    public void playFireAnimation() {
        if (staticModel == null) return;
        staticModel.playAnimation("fire", false, false);
    }

    public Color getPrimaryVisualColor() {
        return switch (getEffectiveSearchState()) {
            case READY -> Color.fromRGB(255, 179, 71);
            case SEARCHING -> Color.fromRGB(255, 140, 66);
            case EXHAUSTED -> Color.fromRGB(255, 107, 107);
        };
    }

    public Color getAccentVisualColor() {
        return switch (getEffectiveSearchState()) {
            case READY -> Color.fromRGB(255, 241, 168);
            case SEARCHING -> Color.fromRGB(255, 209, 102);
            case EXHAUSTED -> Color.fromRGB(255, 159, 104);
        };
    }

    private CannonSearchState getEffectiveSearchState() {
        if (isCharged()) {
            return CannonSearchState.READY;
        }
        if (searchState == CannonSearchState.EXHAUSTED) {
            return CannonSearchState.EXHAUSTED;
        }
        return CannonSearchState.SEARCHING;
    }
}
