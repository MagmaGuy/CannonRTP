package com.magmaguy.cannonrtp.services;

import com.magmaguy.magmacore.config.ConfigurationEngine;
import com.magmaguy.magmacore.config.CustomConfig;
import com.magmaguy.magmacore.util.ConfigurationLocation;
import com.magmaguy.cannonrtp.CannonRTP;
import com.magmaguy.cannonrtp.config.DefaultConfig;
import com.magmaguy.cannonrtp.config.CannonRTPConfig;
import com.magmaguy.cannonrtp.config.CannonRTPConfigFields;
import com.magmaguy.cannonrtp.protection.ProtectionManager;
import com.magmaguy.cannonrtp.protection.ProtectionQueryResult;
import com.magmaguy.cannonrtp.util.MessageUtils;
import lombok.Getter;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class CannonRTPManager implements Listener {
    private static final int SCAN_CADENCE_TICKS = 2;
    private final CannonRTP plugin;
    private final Random random = new Random();
    private final Map<String, ConfiguredCannonRTP> configuredCannons = new LinkedHashMap<>();
    private final Map<UUID, LaunchSequence> activeLaunches = new LinkedHashMap<>();
    private BukkitTask mainTask;
    @Getter
    private CannonRTPConfig cannonRTPConfig;
    private int visualAnimationTick = 0;

    public CannonRTPManager(CannonRTP plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        reload(null);
    }

    public void reload(CommandSender sender) {
        shutdownTasks();
        destroyConfiguredCannonVisuals();
        configuredCannons.clear();

        cannonRTPConfig = new CannonRTPConfig();
        ProtectionManager.initialize();

        for (Map.Entry<String, CannonRTPConfigFields> entry : CannonRTPConfig.getCannonRTPs().entrySet()) {
            ConfiguredCannonRTP configuredCannonRTP = new ConfiguredCannonRTP(entry.getKey(), entry.getValue());
            if (configuredCannonRTP.getCannonLocation() == null || configuredCannonRTP.getCannonLocation().getWorld() == null) {
                configuredCannonRTP.markInvalidConfiguration("The cannon location points at an unloaded world.");
            }
            configuredCannons.put(entry.getKey(), configuredCannonRTP);
        }

        startTasks();
        if (sender != null) {
            MessageUtils.send(sender, DefaultConfig.getReloadMessage(), "count", String.valueOf(configuredCannons.size()));
        }
    }

    public void shutdown() {
        shutdownTasks();
        destroyConfiguredCannonVisuals();
        configuredCannons.clear();
        ProtectionManager.shutdown();
    }

    public boolean isPotentialLandingLocationAllowed(Location location) {
        return ProtectionManager.isPotentialLandingLocationAllowed(location);
    }

    public List<String> getKnownCannonIds() {
        List<String> ids = new ArrayList<>();
        for (String key : configuredCannons.keySet()) {
            ids.add(key.replace(".yml", ""));
        }
        return ids;
    }

    public void sendCannonList(CommandSender sender) {
        if (configuredCannons.isEmpty()) {
            MessageUtils.send(sender, DefaultConfig.getInvalidConfigurationMessage(), "cannon", "CannonRTP", "reason", "No cannons are configured yet.");
            return;
        }
        MessageUtils.sendRaw(sender, DefaultConfig.getHelpHeader());
        for (ConfiguredCannonRTP configuredCannonRTP : configuredCannons.values()) {
            MessageUtils.send(sender, DefaultConfig.getStatusLineMessage(),
                    "cannon", configuredCannonRTP.getDisplayName(),
                    "status", configuredCannonRTP.getStatusDisplay(),
                    "queued", String.valueOf(configuredCannonRTP.getQueuedLocations().size()),
                    "target", String.valueOf(DefaultConfig.getPreloadedLocationsPerCannon()),
                    "reason", configuredCannonRTP.getLastStatusDetail());
        }
    }

    public void sendStatus(CommandSender sender) {
        sendCannonList(sender);
    }

    public void probeLocation(CommandSender sender, Location location) {
        ProtectionQueryResult result = ProtectionManager.inspect(location);
        if (result.allowed()) {
            MessageUtils.sendRaw(sender, DefaultConfig.getProbeAllowedMessage());
            return;
        }
        MessageUtils.send(sender, DefaultConfig.getProbeBlockedMessage(),
                "plugin", result.pluginName(),
                "reason", result.reason());
    }

    public void createCannon(String id, String displayName, Player player) {
        String sanitizedId = sanitizeId(id);
        if (configuredCannons.containsKey(sanitizedId + ".yml")) {
            MessageUtils.send(player, DefaultConfig.getInvalidConfigurationMessage(), "cannon", sanitizedId, "reason", "A cannon with that id already exists.");
            return;
        }
        String resolvedDisplayName = displayName == null || displayName.isBlank()
                ? "CannonRTP"
                : displayName.trim();
        CannonRTPConfigFields fields = new CannonRTPConfigFields(
                sanitizedId,
                true,
                resolvedDisplayName,
                player.getLocation(),
                player.getWorld().getName(),
                player.getWorld().getSpawnLocation());
        new CustomConfig("fun_rtps", CannonRTPConfigFields.class, fields);
        reload(player);
        CannonRTPConfigFields reloadedFields = CannonRTPConfig.getCannonRTPs().get(sanitizedId + ".yml");
        String configPath = reloadedFields != null && reloadedFields.getFile() != null
                ? reloadedFields.getFile().getAbsolutePath()
                : "plugins/CannonRTP/custom/fun_rtps/" + sanitizedId + ".yml";
        MessageUtils.send(player, DefaultConfig.getCreatedCannonMessage(),
                "cannon", resolvedDisplayName,
                "id", sanitizedId,
                "path", configPath);
        MessageUtils.sendRaw(player, MessageUtils.format(
                "$prefix &7Config file: &f" + configPath +
                        "&7. Edit &fdisplayName&7, &fcustomModel&7, &frequiredPermission&7, &flaunchWarmupSeconds&7, &fverticalBoostTicks&7, and &fverticalBoostVelocity&7 there."));
    }

    public void moveCannon(String id, Player player) {
        String filename = resolveFilename(id);
        CannonRTPConfigFields fields = CannonRTPConfig.getCannonRTPs().get(filename);
        if (fields == null) {
            MessageUtils.send(player, DefaultConfig.getInvalidConfigurationMessage(), "cannon", id, "reason", "That cannon does not exist.");
            return;
        }
        ConfigurationEngine.writeValue(ConfigurationLocation.deserialize(player.getLocation()), fields.getFile(), fields.getFileConfiguration(), "cannonLocation");
        reload(player);
        MessageUtils.send(player, DefaultConfig.getMovedCannonMessage(), "cannon", fields.getDisplayName());
    }

    public void deleteCannon(String id, CommandSender sender) {
        String filename = resolveFilename(id);
        CannonRTPConfigFields fields = CannonRTPConfig.getCannonRTPs().get(filename);
        if (fields == null) {
            MessageUtils.send(sender, DefaultConfig.getInvalidConfigurationMessage(), "cannon", id, "reason", "That cannon does not exist.");
            return;
        }
        File file = fields.getFile();
        if (!file.delete()) {
            MessageUtils.send(sender, DefaultConfig.getInvalidConfigurationMessage(), "cannon", id, "reason", "Failed to delete " + file.getName() + ".");
            return;
        }
        reload(sender);
        MessageUtils.send(sender, DefaultConfig.getDeletedCannonMessage(), "cannon", id);
    }

    public void updateTargetWorld(String id, World world, CommandSender sender) {
        String filename = resolveFilename(id);
        CannonRTPConfigFields fields = CannonRTPConfig.getCannonRTPs().get(filename);
        if (fields == null) {
            MessageUtils.send(sender, DefaultConfig.getInvalidConfigurationMessage(), "cannon", id, "reason", "That cannon does not exist.");
            return;
        }
        ConfigurationEngine.writeValue(world.getName(), fields.getFile(), fields.getFileConfiguration(), "targetWorld");
        reload(sender);
        MessageUtils.send(sender, DefaultConfig.getTargetWorldUpdatedMessage(), "cannon", fields.getDisplayName(), "world", world.getName());
    }

    public void updateSearchCenter(String id, Location location, CommandSender sender) {
        String filename = resolveFilename(id);
        CannonRTPConfigFields fields = CannonRTPConfig.getCannonRTPs().get(filename);
        if (fields == null) {
            MessageUtils.send(sender, DefaultConfig.getInvalidConfigurationMessage(), "cannon", id, "reason", "That cannon does not exist.");
            return;
        }
        ConfigurationEngine.writeValue(ConfigurationLocation.deserialize(location), fields.getFile(), fields.getFileConfiguration(), "searchCenter");
        reload(sender);
        MessageUtils.send(sender, DefaultConfig.getSearchCenterUpdatedMessage(), "cannon", fields.getDisplayName());
    }

    private void startTasks() {
        mainTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tickAll, 20L, 1L);
    }

    private void shutdownTasks() {
        if (mainTask != null) {
            mainTask.cancel();
            mainTask = null;
        }
        for (LaunchSequence sequence : activeLaunches.values()) {
            sequence.cleanup();
        }
        activeLaunches.clear();
    }

    private void tickAll() {
        visualAnimationTick = (visualAnimationTick + 1) % 7200;

        // Advance all active launch sequences
        Iterator<Map.Entry<UUID, LaunchSequence>> launchIterator = activeLaunches.entrySet().iterator();
        while (launchIterator.hasNext()) {
            Map.Entry<UUID, LaunchSequence> entry = launchIterator.next();
            LaunchSequence sequence = entry.getValue();
            if (!sequence.tick()) {
                launchIterator.remove();
            }
        }

        // Per-cannon work: visuals, preloading, player scanning
        int particleCadence = Math.max(1, DefaultConfig.getParticleIntervalTicks() / 5);
        boolean shouldScanPlayers = visualAnimationTick % SCAN_CADENCE_TICKS == 0;

        for (ConfiguredCannonRTP configuredCannonRTP : configuredCannons.values()) {
            Location cannonLocation = configuredCannonRTP.getCannonLocation();
            if (cannonLocation == null || cannonLocation.getWorld() == null) {
                configuredCannonRTP.removeVisuals();
                continue;
            }

            if (!configuredCannonRTP.isChunkLoaded()) {
                configuredCannonRTP.removeVisuals();
                continue;
            }

            // Visuals (labels always, particles on cadence)
            configuredCannonRTP.refreshLabel();
            if (configuredCannonRTP.isEnabled() && configuredCannonRTP.getConfigFields().isEnableParticles()) {
                if (!getNearbyPlayers(cannonLocation, 36).isEmpty()) {
                    renderParticleAnimation(configuredCannonRTP, cannonLocation);
                }
            }

            // Preload landing locations
            if (configuredCannonRTP.isEnabled()) {
                preloadForCannon(configuredCannonRTP);
            }

            // Scan for new players stepping in
            if (shouldScanPlayers && configuredCannonRTP.isEnabled()) {
                scanCannonForPlayers(configuredCannonRTP);
            }
        }
    }

    private void preloadForCannon(ConfiguredCannonRTP configuredCannonRTP) {
        if (configuredCannonRTP.getSearchState() == CannonSearchState.INVALID_CONFIGURATION ||
                configuredCannonRTP.getSearchState() == CannonSearchState.EXHAUSTED ||
                !configuredCannonRTP.needsMoreLocations()) {
            return;
        }

        World targetWorld = configuredCannonRTP.getTargetWorld();
        if (targetWorld == null) {
            configuredCannonRTP.markInvalidConfiguration("Target world " + configuredCannonRTP.getConfigFields().getTargetWorldName() + " is not loaded.");
            return;
        }

        Location searchCenter = configuredCannonRTP.getResolvedSearchCenter();
        if (searchCenter == null || searchCenter.getWorld() == null) {
            configuredCannonRTP.markInvalidConfiguration("Search center is invalid.");
            return;
        }

        for (int attempt = 0; attempt < DefaultConfig.getSearchAttemptsPerTick() && configuredCannonRTP.needsMoreLocations(); attempt++) {
            if (configuredCannonRTP.hasTimedOut()) {
                configuredCannonRTP.exhaustSearch();
                break;
            }
            attemptPreload(configuredCannonRTP, targetWorld, searchCenter);
        }
    }

    private void scanCannonForPlayers(ConfiguredCannonRTP configuredCannonRTP) {
        Location cannonLocation = configuredCannonRTP.getCannonLocation();
        if (cannonLocation == null || cannonLocation.getWorld() == null) return;

        Collection<Player> nearbyPlayers = getNearbyPlayers(cannonLocation, configuredCannonRTP.getConfigFields().getTriggerRadius());
        for (Player player : nearbyPlayers) {
            if (!player.isOnline()) continue;

            // Player already in a launch sequence -- skip entirely
            if (activeLaunches.containsKey(player.getUniqueId())) continue;

            if (!player.hasPermission("cannonrtp.use")) continue;

            if (!configuredCannonRTP.canUse(player)) {
                MessageUtils.send(player, DefaultConfig.getNoPermissionMessage(), "cannon", configuredCannonRTP.getDisplayName());
                continue;
            }

            if (configuredCannonRTP.getSearchState() == CannonSearchState.INVALID_CONFIGURATION) {
                MessageUtils.send(player, DefaultConfig.getInvalidConfigurationMessage(),
                        "cannon", configuredCannonRTP.getDisplayName(),
                        "reason", configuredCannonRTP.getLastStatusDetail());
                continue;
            }

            if (configuredCannonRTP.getQueuedLocations().isEmpty()) {
                if (configuredCannonRTP.getSearchState() == CannonSearchState.EXHAUSTED) {
                    MessageUtils.send(player, DefaultConfig.getNoValidLocationFoundMessage(),
                            "cannon", configuredCannonRTP.getDisplayName(),
                            "reason", configuredCannonRTP.buildFailureSummary());
                } else {
                    MessageUtils.send(player, DefaultConfig.getQueueCalibrationMessage(),
                            "cannon", configuredCannonRTP.getDisplayName(),
                            "queued", String.valueOf(configuredCannonRTP.getQueuedLocations().size()),
                            "target", String.valueOf(DefaultConfig.getChargedLocationsPerCannon()),
                            "seconds", String.valueOf(configuredCannonRTP.getSecondsRemaining()));
                }
                continue;
            }

            // Start launch
            Location destination = configuredCannonRTP.consumeQueuedLocation();
            if (destination == null) {
                MessageUtils.send(player, DefaultConfig.getNoValidLocationYetMessage(), "cannon", configuredCannonRTP.getDisplayName());
                continue;
            }

            LaunchSequence sequence = new LaunchSequence(player, configuredCannonRTP, destination);
            activeLaunches.put(player.getUniqueId(), sequence);
        }
    }

    private void attemptPreload(ConfiguredCannonRTP configuredCannonRTP, World targetWorld, Location searchCenter) {
        Location candidateLocation = randomizeLocation(targetWorld, searchCenter, configuredCannonRTP.getConfigFields().getMinSearchRadius(), configuredCannonRTP.getConfigFields().getMaxSearchRadius());
        if (!targetWorld.getWorldBorder().isInside(candidateLocation)) {
            configuredCannonRTP.markSearchFailure(SearchFailureReason.OUTSIDE_WORLD_BORDER);
            return;
        }

        Block highestBlock = targetWorld.getHighestBlockAt(candidateLocation);
        if (highestBlock == null || highestBlock.getType().isAir()) {
            configuredCannonRTP.markSearchFailure(SearchFailureReason.NO_SAFE_SURFACE);
            return;
        }

        Location landingLocation = highestBlock.getLocation().add(0.5, 1, 0.5);
        Block feetBlock = landingLocation.getBlock();
        Block headBlock = landingLocation.clone().add(0, 1, 0).getBlock();
        if (!feetBlock.isPassable() || !headBlock.isPassable()) {
            configuredCannonRTP.markSearchFailure(SearchFailureReason.NO_SAFE_SURFACE);
            return;
        }
        if (!highestBlock.getType().isSolid() || highestBlock.isLiquid()) {
            configuredCannonRTP.markSearchFailure(SearchFailureReason.NO_SAFE_SURFACE);
            return;
        }
        if (DefaultConfig.isUnsafeGroundMaterial(highestBlock.getType()) ||
                DefaultConfig.isUnsafeBodyMaterial(feetBlock.getType()) ||
                DefaultConfig.isUnsafeBodyMaterial(headBlock.getType())) {
            configuredCannonRTP.markSearchFailure(SearchFailureReason.HAZARDOUS_TERRAIN);
            return;
        }

        ProtectionQueryResult protectionQueryResult = ProtectionManager.inspect(landingLocation);
        if (!protectionQueryResult.allowed()) {
            configuredCannonRTP.markSearchFailure(SearchFailureReason.PROTECTED_LAND);
            return;
        }

        configuredCannonRTP.markSearchSuccess(landingLocation);
        if (!configuredCannonRTP.needsMoreLocations()) {
            configuredCannonRTP.markReady();
        }
    }

    private Location randomizeLocation(World world, Location center, int minRadius, int maxRadius) {
        double angle = random.nextDouble() * Math.PI * 2;
        double distance = minRadius + random.nextDouble() * Math.max(1, maxRadius - minRadius);
        double x = center.getX() + Math.cos(angle) * distance;
        double z = center.getZ() + Math.sin(angle) * distance;
        return new Location(world, x, world.getHighestBlockYAt((int) Math.round(x), (int) Math.round(z)) + 1.0, z);
    }

    private void renderParticleAnimation(ConfiguredCannonRTP configuredCannonRTP, Location cannonLocation) {
        World world = cannonLocation.getWorld();
        if (world == null) {
            return;
        }

        Location center = cannonLocation.clone().add(0, 1.0, 0);
        Color primaryColor = configuredCannonRTP.getPrimaryVisualColor();
        Color accentColor = configuredCannonRTP.getAccentVisualColor();
        double rotation = visualAnimationTick * 0.06;
        double orbitRadius = 0.78 + Math.sin(visualAnimationTick * 0.04) * 0.05;

        spawnOrbitTrack(world, center, rotation, orbitRadius, 0.22, 0.30, primaryColor, accentColor, 1.0f, true);
        spawnOrbitTrack(world, center, rotation + Math.PI, orbitRadius, 0.22, -0.30, accentColor, primaryColor, 1.0f, true);

        world.spawnParticle(
                Particle.DUST_COLOR_TRANSITION,
                center.clone().add(0, 0.18 + Math.sin(visualAnimationTick * 0.04) * 0.05, 0),
                1,
                0,
                0,
                0,
                0,
                new Particle.DustTransition(primaryColor, accentColor, 0.95f));
    }

    private void spawnOrbitTrack(World world, Location center, double angle, double radius, double baseHeight, double verticalWaveDirection,
                                 Color fromColor, Color toColor, float size, boolean spawnFireworkTrail) {
        double verticalWave = Math.sin(angle * 2.0 + visualAnimationTick * 0.02) * 0.20 * verticalWaveDirection;
        Location particleLocation = center.clone().add(
                Math.cos(angle) * radius,
                baseHeight + verticalWave,
                Math.sin(angle) * radius);
        world.spawnParticle(
                Particle.DUST_COLOR_TRANSITION,
                particleLocation,
                1,
                0,
                0,
                0,
                0,
                new Particle.DustTransition(fromColor, toColor, size));

        if (!spawnFireworkTrail) {
            return;
        }

        world.spawnParticle(Particle.FIREWORK, particleLocation, 1, 0.015, 0.015, 0.015, 0.0);
    }

    private Collection<Player> getNearbyPlayers(Location location, double radius) {
        Collection<Entity> nearbyEntities = location.getWorld().getNearbyEntities(
                location,
                radius,
                Math.max(2.5, radius),
                radius,
                entity -> entity instanceof Player);
        List<Player> nearbyPlayers = new ArrayList<>(nearbyEntities.size());
        for (Entity entity : nearbyEntities) {
            if (entity instanceof Player player) {
                nearbyPlayers.add(player);
            }
        }
        return nearbyPlayers;
    }

    private String sanitizeId(String id) {
        return id.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "_");
    }

    private String resolveFilename(String id) {
        return id.endsWith(".yml") ? id : sanitizeId(id) + ".yml";
    }

    private void destroyConfiguredCannonVisuals() {
        for (ConfiguredCannonRTP configuredCannonRTP : configuredCannons.values()) {
            configuredCannonRTP.removeVisuals();
        }
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        removeVisualsForChunk(event.getChunk());
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        handleChunkLoad(event.getChunk());
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent event) {
        for (ConfiguredCannonRTP configuredCannonRTP : configuredCannons.values()) {
            Location location = configuredCannonRTP.getCannonLocation();
            if (location != null && event.getWorld().equals(location.getWorld())) {
                configuredCannonRTP.removeVisuals();
            }
        }
    }

    private void removeVisualsForChunk(Chunk chunk) {
        for (ConfiguredCannonRTP configuredCannonRTP : configuredCannons.values()) {
            if (configuredCannonRTP.isInChunk(chunk)) {
                configuredCannonRTP.removeVisuals();
            }
        }
    }

    private void handleChunkLoad(Chunk chunk) {
        for (ConfiguredCannonRTP configuredCannonRTP : configuredCannons.values()) {
            if (configuredCannonRTP.isInChunk(chunk)) {
                configuredCannonRTP.handleChunkLoad();
            }
        }
    }
}
