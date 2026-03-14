package com.magmaguy.cannonrtp.services;

import com.magmaguy.cannonrtp.config.DefaultConfig;
import com.magmaguy.cannonrtp.config.CannonRTPConfigFields;
import com.magmaguy.freeminecraftmodels.api.ModeledEntityManager;
import com.magmaguy.freeminecraftmodels.customentity.StaticEntity;
import com.magmaguy.magmacore.util.ChatColorConverter;
import com.magmaguy.magmacore.util.ChunkLocationChecker;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
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
    @Getter
    private final String id;
    @Getter
    private final CannonRTPConfigFields configFields;
    @Getter
    private final ArrayDeque<Location> queuedLocations = new ArrayDeque<>();
    private final Map<SearchFailureReason, Integer> failures = new EnumMap<>(SearchFailureReason.class);
    @Getter
    private CannonSearchState searchState = CannonSearchState.SEARCHING;
    @Getter
    private int searchAttempts = 0;
    private long searchStartedAt = System.currentTimeMillis();
    @Getter
    private String lastStatusDetail = "Still searching.";
    private TextDisplay labelDisplay;
    private StaticEntity staticModel;
    private String lastLabelText = "";

    public ConfiguredCannonRTP(String id, CannonRTPConfigFields configFields) {
        this.id = id;
        this.configFields = configFields;
    }

    public Location getCannonLocation() {
        return configFields.getCannonLocation();
    }

    public String getDisplayName() {
        return configFields.getDisplayName();
    }

    public World getTargetWorld() {
        return Bukkit.getWorld(configFields.getTargetWorldName());
    }

    public Location getResolvedSearchCenter() {
        Location configuredCenter = configFields.getSearchCenter();
        World targetWorld = getTargetWorld();
        if (targetWorld == null) {
            return null;
        }
        if (configuredCenter == null) {
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
        return queuedLocations.size() < DefaultConfig.getPreloadedLocationsPerCannon();
    }

    public boolean isCharged() {
        return queuedLocations.size() >= DefaultConfig.getChargedLocationsPerCannon();
    }

    public boolean isChunkLoaded() {
        return ChunkLocationChecker.chunkAtLocationIsLoaded(getCannonLocation());
    }

    public boolean isInChunk(Chunk chunk) {
        Location cannonLocation = getCannonLocation();
        return cannonLocation != null && ChunkLocationChecker.locationIsInChunk(cannonLocation, chunk);
    }

    public void handleChunkLoad() {
        if (needsMoreLocations() && searchState != CannonSearchState.INVALID_CONFIGURATION) {
            restartSearchWindow();
        }
    }

    public String getCustomModelName() {
        return configFields.getCustomModel();
    }

    public boolean hasTimedOut() {
        return (System.currentTimeMillis() - searchStartedAt) / 1000L >= DefaultConfig.getSearchTimeoutSeconds();
    }

    public int getSecondsRemaining() {
        long elapsed = (System.currentTimeMillis() - searchStartedAt) / 1000L;
        return Math.max(0, DefaultConfig.getSearchTimeoutSeconds() - (int) elapsed);
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

    private void restartSearchWindow() {
        searchStartedAt = System.currentTimeMillis();
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
        return switch (getEffectiveSearchState()) {
            case READY -> "Ready";
            case SEARCHING -> queuedLocations.isEmpty() ? "Charging" : "Maintaining";
            case EXHAUSTED -> "Exhausted";
            case INVALID_CONFIGURATION -> "Invalid";
        };
    }

    public void refreshLabel() {
        Location cannonLocation = getCannonLocation();
        if (cannonLocation == null || cannonLocation.getWorld() == null) {
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

        if (labelDisplay == null || !labelDisplay.isValid()) {
            spawnLabel(cannonLocation);
        }
        if (labelDisplay == null || !labelDisplay.isValid()) {
            return;
        }

        labelDisplay.teleport(getLabelLocation(cannonLocation));

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
    }

    private void spawnLabel(Location cannonLocation) {
        Location labelLocation = getLabelLocation(cannonLocation);
        labelDisplay = labelLocation.getWorld().spawn(labelLocation, TextDisplay.class, display -> {
            display.setText(buildLabelText());
            display.setPersistent(false);
            display.setBillboard(Display.Billboard.CENTER);
            display.setShadowed(false);
            display.setAlignment(TextDisplay.TextAlignment.CENTER);
        });
        lastLabelText = buildLabelText();
    }

    private Location getLabelLocation(Location cannonLocation) {
        return cannonLocation.clone().add(0, staticModel != null ? 2.0 : 1.2, 0);
    }

    private String buildLabelText() {
        return ChatColorConverter.convert(getDisplayName() + "\n&7" + getStatusDisplay());
    }

    private boolean shouldUseCustomModel() {
        String modelName = getCustomModelName();
        return modelName != null
                && !modelName.isBlank()
                && Bukkit.getPluginManager().isPluginEnabled("FreeMinecraftModels")
                && ModeledEntityManager.modelExists(modelName);
    }

    public boolean shouldUseCustomLaunchAnimation() {
        return shouldUseCustomModel() && ModeledEntityManager.modelExists("fire");
    }

    private void initializeCustomModel() {
        String modelName = getCustomModelName();
        Location location = getCannonLocation();
        if (modelName == null || modelName.isBlank() || location == null || location.getWorld() == null) {
            return;
        }
        if (!Bukkit.getPluginManager().isPluginEnabled("FreeMinecraftModels")) {
            return;
        }
        if (!ModeledEntityManager.modelExists(modelName)) {
            return;
        }
        if (staticModel != null) {
            staticModel.remove();
        }
        staticModel = StaticEntity.create(modelName, location.clone());
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

