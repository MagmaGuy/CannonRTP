package com.magmaguy.cannonrtp.services;

import com.magmaguy.cannonrtp.config.CannonRTPConfigFields;
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

public class ConfiguredCannonRTP {
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
    private final Map<java.util.UUID, Long> lastNotifyMs = new java.util.HashMap<>();

    @Getter
    private CannonSearchState searchState = CannonSearchState.SEARCHING;
    @Getter
    private int searchAttempts = 0;
    @Getter
    private String lastStatusDetail = "Still searching.";
    private TextDisplay labelDisplay;
    private StaticEntity staticModel;
    private String lastLabelText = "";

    @Getter
    private Location cannonLocation;
    @Getter
    private boolean chunkLoaded;
    @Getter
    private boolean cannonWorldLoaded;
    private World cachedTargetWorld;
    private int cannonChunkX;
    private int cannonChunkZ;

    private Boolean cachedUseCustomModel;
    private String cachedResolvedModelName;
    private boolean lastLabelHasModel = false;

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

    public void invalidateCachedTargetWorld() {
        cachedTargetWorld = null;
    }

    /**
     * Unified teardown for this instance. Visuals are always removed; other side-effects
     * depend on the reason (chunk unload vs. world unload vs. reload vs. shutdown).
     */
    public void remove(RemovalReason reason) {
        removeVisuals();
        switch (reason) {
            case CHUNK_UNLOAD -> chunkLoaded = false;
            case WORLD_UNLOAD -> {
                cannonWorldLoaded = false;
                chunkLoaded = false;
            }
            case REMOVE_COMMAND, DELETE_COMMAND, RELOAD, SHUTDOWN -> {
                // Instance is being discarded; no further field updates needed.
            }
        }
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

    public void clearNotifyThrottle(java.util.UUID playerId) {
        lastNotifyMs.remove(playerId);
    }

    /**
     * A cannon participates in the manager tick loop only when it is enabled, valid,
     * has a resolvable location, and its world and chunk are both loaded.
     */
    public boolean isActive() {
        return isEnabled()
                && cannonLocation != null
                && cannonWorldLoaded
                && chunkLoaded
                && searchState != CannonSearchState.INVALID_CONFIGURATION;
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
        if (needsMoreLocations() && searchState != CannonSearchState.INVALID_CONFIGURATION) {
            restartSearchWindow();
        }
    }

    public String getCustomModelName() {
        if (cachedUseCustomModel != null) {
            return cachedResolvedModelName == null ? "" : cachedResolvedModelName;
        }
        resolveModelCache();
        return cachedResolvedModelName == null ? "" : cachedResolvedModelName;
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

    public boolean hasTimedOut() {
        return searchAttempts >= LandingSearchConfig.getSearchTimeoutAttempts();
    }

    public int getAttemptsRemaining() {
        return Math.max(0, LandingSearchConfig.getSearchTimeoutAttempts() - searchAttempts);
    }

    public void markReady() {
        searchState = CannonSearchState.READY;
        lastStatusDetail = "Safe locations preloaded.";
    }

    public void markInvalidConfiguration(String reason) {
        searchState = CannonSearchState.INVALID_CONFIGURATION;
        lastStatusDetail = reason;
    }

    public void markSearchFailure(SearchFailureReason reason) {
        failures.merge(reason, 1, Integer::sum);
        searchAttempts++;
        if (searchState != CannonSearchState.INVALID_CONFIGURATION) {
            searchState = CannonSearchState.SEARCHING;
        }
    }

    public void markSearchSuccess(Location location) {
        searchAttempts++;
        queuedLocations.add(location);
        searchState = isCharged() ? CannonSearchState.READY : CannonSearchState.SEARCHING;
        lastStatusDetail = isCharged() ? "Ready." : "Maintaining destination reserve.";
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
        if (searchState != CannonSearchState.INVALID_CONFIGURATION) {
            searchState = isCharged() ? CannonSearchState.READY : CannonSearchState.SEARCHING;
            lastStatusDetail = queuedLocations.isEmpty()
                    ? "Charging teleport destinations."
                    : "Maintaining destination reserve.";
        }
    }

    public String buildFailureSummary() {
        if (failures.isEmpty()) {
            return "Every sampled location failed validation.";
        }
        StringBuilder summary = new StringBuilder();
        appendSummary(summary, SearchFailureReason.INVALID_TARGET_WORLD, "the target world is not loaded");
        appendSummary(summary, SearchFailureReason.INVALID_SEARCH_CENTER, "the configured search center is invalid");
        appendSummary(summary, SearchFailureReason.OUTSIDE_WORLD_BORDER, "most samples landed outside the world border");
        appendSummary(summary, SearchFailureReason.NO_SAFE_SURFACE, "many samples had no safe surface");
        appendSummary(summary, SearchFailureReason.HAZARDOUS_TERRAIN, "many samples were hazardous to land on");
        appendSummary(summary, SearchFailureReason.PROTECTED_LAND, "most samples were inside protected land");
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
        if (!isEnabled()) return "Disabled";
        return switch (getEffectiveSearchState()) {
            case READY -> "Ready";
            case SEARCHING -> queuedLocations.isEmpty() ? "Charging" : "Maintaining";
            case EXHAUSTED -> "Exhausted";
            case INVALID_CONFIGURATION -> "Invalid";
        };
    }

    public void refreshLabel() {
        Location loc = getCannonLocation();
        if (loc == null || loc.getWorld() == null) {
            removeVisuals();
            return;
        }

        if (shouldUseCustomModel()) {
            if (staticModel == null) {
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

        String nextLabelText = buildLabelText();
        if (!nextLabelText.equals(lastLabelText)) {
            labelDisplay.setText(nextLabelText);
            lastLabelText = nextLabelText;
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
        lastLabelText = "";
        lastLabelHasModel = false;
    }

    private void spawnLabel(Location loc) {
        Location labelLocation = getLabelLocation(loc);
        labelDisplay = labelLocation.getWorld().spawn(labelLocation, TextDisplay.class, display -> {
            display.setText(buildLabelText());
            display.setPersistent(false);
            display.setBillboard(Display.Billboard.CENTER);
            display.setShadowed(false);
            display.setAlignment(TextDisplay.TextAlignment.CENTER);
        });
        lastLabelText = buildLabelText();
    }

    private Location getLabelLocation(Location loc) {
        return loc.clone().add(0, staticModel != null ? 2.0 : 1.2, 0);
    }

    private String buildLabelText() {
        return ChatColorConverter.convert(getDisplayName() + "\n&7" + getStatusDisplay());
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
        if (staticModel != null) {
            staticModel.remove();
        }
        staticModel = StaticEntity.create(cachedResolvedModelName, loc.clone());
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
            case INVALID_CONFIGURATION -> Color.fromRGB(201, 60, 60);
        };
    }

    public Color getAccentVisualColor() {
        return switch (getEffectiveSearchState()) {
            case READY -> Color.fromRGB(255, 241, 168);
            case SEARCHING -> Color.fromRGB(255, 209, 102);
            case EXHAUSTED -> Color.fromRGB(255, 159, 104);
            case INVALID_CONFIGURATION -> Color.fromRGB(255, 124, 124);
        };
    }

    private CannonSearchState getEffectiveSearchState() {
        if (searchState == CannonSearchState.INVALID_CONFIGURATION) {
            return CannonSearchState.INVALID_CONFIGURATION;
        }
        if (isCharged()) {
            return CannonSearchState.READY;
        }
        if (searchState == CannonSearchState.EXHAUSTED) {
            return CannonSearchState.EXHAUSTED;
        }
        return CannonSearchState.SEARCHING;
    }
}
