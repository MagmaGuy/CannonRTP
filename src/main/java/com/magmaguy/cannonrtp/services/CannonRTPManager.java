package com.magmaguy.cannonrtp.services;

import com.magmaguy.cannonrtp.CannonRTP;
import com.magmaguy.cannonrtp.api.CannonRTPLaunchEvent;
import com.magmaguy.cannonrtp.api.CannonRTPLocationValidationEvent;
import com.magmaguy.cannonrtp.config.CannonMessagesConfig;
import com.magmaguy.cannonrtp.config.CannonRTPConfig;
import com.magmaguy.cannonrtp.config.CannonRTPConfigFields;
import com.magmaguy.cannonrtp.config.DefaultConfig;
import com.magmaguy.cannonrtp.config.LandingSearchConfig;
import com.magmaguy.cannonrtp.protection.ProtectionManager;
import com.magmaguy.cannonrtp.protection.ProtectionQueryResult;
import com.magmaguy.cannonrtp.util.MessageUtils;
import com.magmaguy.magmacore.config.ConfigurationEngine;
import com.magmaguy.magmacore.util.ConfigurationLocation;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class CannonRTPManager {
    private static final int SCAN_CADENCE_TICKS = 2;
    private static final long NOTIFY_THROTTLE_MS = 3000L;
    private static final int GLOBAL_PRELOAD_INTERVAL_TICKS = 1;

    private final CannonRTP plugin;
    private final Random random = new Random();

    /**
     * Flat map keyed by ConfiguredCannonRTP.getInstanceId(). One CannonRTPConfigFields
     * can produce many instances (one per entry in its cannonLocations list).
     */
    private final Map<String, ConfiguredCannonRTP> configuredCannons = new LinkedHashMap<>();
    private final Map<UUID, LaunchSequence> activeLaunches = new HashMap<>();
    private final List<ConfiguredCannonRTP> activeCannons = new ArrayList<>();
    private final Map<String, Map<Long, List<ConfiguredCannonRTP>>> cannonsByChunk = new HashMap<>();

    private List<String> cachedKnownCannonIds;
    private int preloadRoundRobinIndex = 0;
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
        for (ConfiguredCannonRTP instance : configuredCannons.values()) {
            instance.remove(RemovalReason.RELOAD);
        }
        configuredCannons.clear();
        activeCannons.clear();
        cannonsByChunk.clear();
        cachedKnownCannonIds = null;
        preloadRoundRobinIndex = 0;

        cannonRTPConfig = new CannonRTPConfig();
        ProtectionManager.initialize();

        for (Map.Entry<String, CannonRTPConfigFields> entry : CannonRTPConfig.getCannonRTPs().entrySet()) {
            String filename = entry.getKey();
            String configId = stripYml(filename);
            CannonRTPConfigFields fields = entry.getValue();
            List<String> strings = fields.getCannonLocations();
            if (strings == null) continue;
            for (int i = 0; i < strings.size(); i++) {
                String locationString = strings.get(i);
                if (locationString == null || locationString.isBlank()) continue;
                Location resolved = ConfigurationLocation.serialize(locationString);
                if (resolved == null) continue;
                // World may be null if not loaded — the instance still participates so it
                // can wake up via the world-load listener.
                String instanceId = configId + "#" + i;
                ConfiguredCannonRTP instance = new ConfiguredCannonRTP(configId, instanceId, fields, resolved, locationString);
                configuredCannons.put(instanceId, instance);
                indexCannon(instance);
                refreshActiveState(instance);
            }
        }

        startTasks();
        if (sender != null) {
            MessageUtils.send(sender, CannonMessagesConfig.getReloadMessage(),
                    "count", String.valueOf(configuredCannons.size()));
        }
    }

    public void shutdown() {
        shutdownTasks();
        for (ConfiguredCannonRTP instance : configuredCannons.values()) {
            instance.remove(RemovalReason.SHUTDOWN);
        }
        configuredCannons.clear();
        activeCannons.clear();
        cannonsByChunk.clear();
        cachedKnownCannonIds = null;
        ProtectionManager.shutdown();
    }

    public boolean isPotentialLandingLocationAllowed(Location location) {
        return ProtectionManager.isPotentialLandingLocationAllowed(location);
    }

    /**
     * Config ids known to the plugin — one entry per YAML file, regardless of how many
     * placements that file has. Used by tab-completion.
     */
    public List<String> getKnownCannonIds() {
        if (cachedKnownCannonIds == null) {
            List<String> ids = new ArrayList<>();
            for (String filename : CannonRTPConfig.getCannonRTPs().keySet()) {
                ids.add(stripYml(filename));
            }
            cachedKnownCannonIds = ids;
        }
        return cachedKnownCannonIds;
    }

    public void sendCannonList(CommandSender sender) {
        if (configuredCannons.isEmpty()) {
            MessageUtils.send(sender, CannonMessagesConfig.getInvalidConfigurationMessage(),
                    "cannon", "CannonRTP",
                    "reason", "No cannons are placed yet.");
            return;
        }
        MessageUtils.sendRaw(sender, CannonMessagesConfig.getHelpHeader());
        for (ConfiguredCannonRTP instance : configuredCannons.values()) {
            MessageUtils.send(sender, CannonMessagesConfig.getStatusLineMessage(),
                    "cannon", instance.getDisplayName(),
                    "status", instance.getStatusDisplay(),
                    "queued", String.valueOf(instance.getQueuedLocations().size()),
                    "target", String.valueOf(LandingSearchConfig.getPreloadedLocationsPerCannon()),
                    "reason", instance.getLastStatusDetail());
        }
    }

    public void sendStatus(CommandSender sender) {
        sendCannonList(sender);
    }

    public void probeLocation(CommandSender sender, Location location) {
        ProtectionQueryResult result = ProtectionManager.inspect(location);
        if (result.allowed()) {
            MessageUtils.sendRaw(sender, CannonMessagesConfig.getProbeAllowedMessage());
            return;
        }
        MessageUtils.send(sender, CannonMessagesConfig.getProbeBlockedMessage(),
                "plugin", result.pluginName(),
                "reason", result.reason());
    }

    /**
     * Creates a brand-new cannon config file with an initial placement at the player's
     * location. If a config with that id already exists, refuses — admins must use
     * {@link #placeCannon(String, Player)} to add more placements to an existing config.
     */
    public void createCannon(String id, String displayName, Player player) {
        String sanitizedId = sanitizeId(id);
        String filename = sanitizedId + ".yml";
        if (CannonRTPConfig.getCannonRTPs().containsKey(filename)) {
            MessageUtils.send(player, CannonMessagesConfig.getInvalidConfigurationMessage(),
                    "cannon", sanitizedId,
                    "reason", "A cannon with that id already exists. Use /wc place to add another placement.");
            return;
        }
        String resolvedDisplayName = displayName == null || displayName.isBlank()
                ? "CannonRTP"
                : displayName.trim();
        List<String> initialLocations = new ArrayList<>();
        initialLocations.add(ConfigurationLocation.deserialize(player.getLocation()));
        CannonRTPConfigFields fields = new CannonRTPConfigFields(
                sanitizedId,
                true,
                resolvedDisplayName,
                initialLocations,
                player.getWorld().getName(),
                null);
        new com.magmaguy.magmacore.config.CustomConfig("cannons", CannonRTPConfigFields.class, fields);
        reload(player);
        CannonRTPConfigFields reloadedFields = CannonRTPConfig.getCannonRTPs().get(filename);
        String configPath = reloadedFields != null && reloadedFields.getFile() != null
                ? reloadedFields.getFile().getAbsolutePath()
                : "plugins/CannonRTP/custom/cannons/" + filename;
        MessageUtils.send(player, CannonMessagesConfig.getCreatedCannonMessage(),
                "cannon", resolvedDisplayName,
                "id", sanitizedId,
                "path", configPath);
    }

    /**
     * Appends a new placement to an existing cannon config at the player's current
     * location. No-ops with an error message if the config id is unknown.
     */
    public void placeCannon(String id, Player player) {
        String filename = resolveFilename(id);
        CannonRTPConfigFields fields = CannonRTPConfig.getCannonRTPs().get(filename);
        if (fields == null) {
            MessageUtils.send(player, CannonMessagesConfig.getInvalidConfigurationMessage(),
                    "cannon", id,
                    "reason", "That cannon does not exist. Use /wc create to make a new one.");
            return;
        }
        List<String> updated = fields.addCannonLocation(player.getLocation());
        ConfigurationEngine.writeValue(updated, fields.getFile(), fields.getFileConfiguration(), "cannonLocations");
        reload(player);
        MessageUtils.send(player, CannonMessagesConfig.getPlacedCannonMessage(),
                "cannon", fields.getDisplayName());
    }

    /**
     * Removes the nearest placed instance of the given config to the player and persists
     * the change. If no instance of that config exists in the player's world, warns and
     * returns without modifying anything.
     */
    public void removeCannonNearPlayer(String id, Player player) {
        String filename = resolveFilename(id);
        CannonRTPConfigFields fields = CannonRTPConfig.getCannonRTPs().get(filename);
        if (fields == null) {
            MessageUtils.send(player, CannonMessagesConfig.getInvalidConfigurationMessage(),
                    "cannon", id,
                    "reason", "That cannon does not exist.");
            return;
        }
        String configId = stripYml(filename);
        ConfiguredCannonRTP nearest = null;
        double nearestDistanceSq = Double.MAX_VALUE;
        Location playerLocation = player.getLocation();
        for (ConfiguredCannonRTP instance : configuredCannons.values()) {
            if (!instance.getConfigId().equals(configId)) continue;
            Location cannonLocation = instance.getCannonLocation();
            if (cannonLocation == null || cannonLocation.getWorld() == null) continue;
            if (!cannonLocation.getWorld().equals(playerLocation.getWorld())) continue;
            double distanceSq = cannonLocation.distanceSquared(playerLocation);
            if (distanceSq < nearestDistanceSq) {
                nearestDistanceSq = distanceSq;
                nearest = instance;
            }
        }
        if (nearest == null) {
            MessageUtils.send(player, CannonMessagesConfig.getInvalidConfigurationMessage(),
                    "cannon", id,
                    "reason", "No placement of that cannon found in your world.");
            return;
        }
        fields.removeCannonLocation(nearest.getLocationString());
        ConfigurationEngine.writeValue(fields.getCannonLocations(), fields.getFile(), fields.getFileConfiguration(), "cannonLocations");
        nearest.remove(RemovalReason.REMOVE_COMMAND);
        reload(player);
        MessageUtils.send(player, CannonMessagesConfig.getRemovedCannonMessage(),
                "cannon", fields.getDisplayName());
    }

    public void deleteCannon(String id, CommandSender sender) {
        String filename = resolveFilename(id);
        CannonRTPConfigFields fields = CannonRTPConfig.getCannonRTPs().get(filename);
        if (fields == null) {
            MessageUtils.send(sender, CannonMessagesConfig.getInvalidConfigurationMessage(),
                    "cannon", id,
                    "reason", "That cannon does not exist.");
            return;
        }
        File file = fields.getFile();
        if (!file.delete()) {
            MessageUtils.send(sender, CannonMessagesConfig.getInvalidConfigurationMessage(),
                    "cannon", id,
                    "reason", "Failed to delete " + file.getName() + ".");
            return;
        }
        reload(sender);
        MessageUtils.send(sender, CannonMessagesConfig.getDeletedCannonMessage(), "cannon", id);
    }

    public void updateTargetWorld(String id, World world, CommandSender sender) {
        String filename = resolveFilename(id);
        CannonRTPConfigFields fields = CannonRTPConfig.getCannonRTPs().get(filename);
        if (fields == null) {
            MessageUtils.send(sender, CannonMessagesConfig.getInvalidConfigurationMessage(),
                    "cannon", id,
                    "reason", "That cannon does not exist.");
            return;
        }
        ConfigurationEngine.writeValue(world.getName(), fields.getFile(), fields.getFileConfiguration(), "targetWorld");
        reload(sender);
        MessageUtils.send(sender, CannonMessagesConfig.getTargetWorldUpdatedMessage(),
                "cannon", fields.getDisplayName(), "world", world.getName());
    }

    public void updateSearchCenter(String id, Location location, CommandSender sender) {
        String filename = resolveFilename(id);
        CannonRTPConfigFields fields = CannonRTPConfig.getCannonRTPs().get(filename);
        if (fields == null) {
            MessageUtils.send(sender, CannonMessagesConfig.getInvalidConfigurationMessage(),
                    "cannon", id,
                    "reason", "That cannon does not exist.");
            return;
        }
        ConfigurationEngine.writeValue(ConfigurationLocation.deserialize(location), fields.getFile(), fields.getFileConfiguration(), "searchCenter");
        reload(sender);
        MessageUtils.send(sender, CannonMessagesConfig.getSearchCenterUpdatedMessage(),
                "cannon", fields.getDisplayName());
    }

    // ---------------------------------------------------------------------
    // Tick loop
    // ---------------------------------------------------------------------

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

        Iterator<Map.Entry<UUID, LaunchSequence>> launchIterator = activeLaunches.entrySet().iterator();
        while (launchIterator.hasNext()) {
            Map.Entry<UUID, LaunchSequence> entry = launchIterator.next();
            LaunchSequence sequence = entry.getValue();
            if (!sequence.tick()) {
                launchIterator.remove();
            }
        }

        boolean shouldScanPlayers = visualAnimationTick % SCAN_CADENCE_TICKS == 0;
        int particleCadence = Math.max(1, DefaultConfig.getParticleIntervalTicks() / 5);
        boolean shouldRenderParticles = visualAnimationTick % particleCadence == 0;

        for (ConfiguredCannonRTP instance : activeCannons) {
            Location cannonLocation = instance.getCannonLocation();
            if (cannonLocation == null || cannonLocation.getWorld() == null) continue;

            instance.refreshLabel();

            if (shouldRenderParticles
                    && instance.getConfigFields().isEnableParticles()
                    && !instance.hasActiveModel()
                    && hasPlayerWithinRange(cannonLocation, 36.0)) {
                renderParticleAnimation(instance, cannonLocation);
            }

            if (shouldScanPlayers) {
                scanCannonForPlayers(instance);
            }
        }

        if (visualAnimationTick % GLOBAL_PRELOAD_INTERVAL_TICKS == 0) {
            runGlobalPreloadAttempt();
        }
    }

    private void runGlobalPreloadAttempt() {
        int n = activeCannons.size();
        if (n == 0) return;
        if (preloadRoundRobinIndex >= n) preloadRoundRobinIndex = 0;

        for (int i = 0; i < n; i++) {
            int idx = (preloadRoundRobinIndex + i) % n;
            ConfiguredCannonRTP cannon = activeCannons.get(idx);

            CannonSearchState state = cannon.getSearchState();
            if (state == CannonSearchState.INVALID_CONFIGURATION
                    || state == CannonSearchState.EXHAUSTED
                    || !cannon.needsMoreLocations()) {
                continue;
            }

            World targetWorld = cannon.getTargetWorld();
            if (targetWorld == null) {
                cannon.markInvalidConfiguration("Target world " + cannon.getConfigFields().getTargetWorldName() + " is not loaded.");
                continue;
            }

            Location searchCenter = cannon.getResolvedSearchCenter();
            if (searchCenter == null || searchCenter.getWorld() == null) {
                cannon.markInvalidConfiguration("Search center is invalid.");
                continue;
            }

            if (cannon.hasTimedOut()) {
                cannon.exhaustSearch();
                continue;
            }

            preloadRoundRobinIndex = (idx + 1) % n;
            attemptPreload(cannon, targetWorld, searchCenter);
            return;
        }
    }

    private void scanCannonForPlayers(ConfiguredCannonRTP cannon) {
        Location cannonLocation = cannon.getCannonLocation();
        if (cannonLocation == null || cannonLocation.getWorld() == null) return;

        World world = cannonLocation.getWorld();
        double triggerRadius = cannon.getConfigFields().getTriggerRadius();
        double triggerRadiusSq = triggerRadius * triggerRadius;
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distanceSquared(cannonLocation) > triggerRadiusSq) continue;
            if (activeLaunches.containsKey(player.getUniqueId())) continue;
            if (!player.hasPermission("cannonrtp.use")) continue;

            if (!cannon.canUse(player)) {
                if (cannon.shouldNotify(player, NOTIFY_THROTTLE_MS)) {
                    MessageUtils.send(player, CannonMessagesConfig.getNoPermissionMessage(),
                            "cannon", cannon.getDisplayName());
                }
                continue;
            }

            if (cannon.getSearchState() == CannonSearchState.INVALID_CONFIGURATION) {
                if (cannon.shouldNotify(player, NOTIFY_THROTTLE_MS)) {
                    MessageUtils.send(player, CannonMessagesConfig.getInvalidConfigurationMessage(),
                            "cannon", cannon.getDisplayName(),
                            "reason", cannon.getLastStatusDetail());
                }
                continue;
            }

            if (cannon.getQueuedLocations().isEmpty()) {
                if (cannon.shouldNotify(player, NOTIFY_THROTTLE_MS)) {
                    if (cannon.getSearchState() == CannonSearchState.EXHAUSTED) {
                        MessageUtils.send(player, CannonMessagesConfig.getNoValidLocationFoundMessage(),
                                "cannon", cannon.getDisplayName(),
                                "reason", cannon.buildFailureSummary());
                    } else {
                        MessageUtils.send(player, CannonMessagesConfig.getQueueCalibrationMessage(),
                                "cannon", cannon.getDisplayName(),
                                "queued", String.valueOf(cannon.getQueuedLocations().size()),
                                "target", String.valueOf(LandingSearchConfig.getChargedLocationsPerCannon()),
                                "attempts", String.valueOf(cannon.getAttemptsRemaining()));
                    }
                }
                continue;
            }

            Location destination = cannon.consumeQueuedLocation();
            if (destination == null) {
                if (cannon.shouldNotify(player, NOTIFY_THROTTLE_MS)) {
                    MessageUtils.send(player, CannonMessagesConfig.getNoValidLocationYetMessage(),
                            "cannon", cannon.getDisplayName());
                }
                continue;
            }

            // Fire the cancellable launch event. If anyone vetoes it, return the
            // destination to the queue and skip this player — the cannon resumes
            // normal behaviour on the next scan tick.
            CannonRTPLaunchEvent launchEvent = new CannonRTPLaunchEvent(
                    player,
                    cannon.getConfigId(),
                    cannon.getDisplayName(),
                    cannonLocation.clone(),
                    destination.clone());
            Bukkit.getPluginManager().callEvent(launchEvent);
            if (launchEvent.isCancelled()) {
                cannon.returnQueuedLocation(destination);
                continue;
            }

            cannon.clearNotifyThrottle(player);
            LaunchSequence sequence = new LaunchSequence(player, cannon, destination);
            activeLaunches.put(player.getUniqueId(), sequence);
        }
    }

    private void attemptPreload(ConfiguredCannonRTP cannon, World targetWorld, Location searchCenter) {
        Location candidateLocation = randomizeLocation(targetWorld, searchCenter,
                cannon.getConfigFields().getMinSearchRadius(),
                cannon.getConfigFields().getMaxSearchRadius());
        if (!targetWorld.getWorldBorder().isInside(candidateLocation)) {
            cannon.markSearchFailure(SearchFailureReason.OUTSIDE_WORLD_BORDER);
            return;
        }

        int chunkX = candidateLocation.getBlockX() >> 4;
        int chunkZ = candidateLocation.getBlockZ() >> 4;
        if (!targetWorld.isChunkLoaded(chunkX, chunkZ)
                && !targetWorld.loadChunk(chunkX, chunkZ, false)) {
            cannon.markSearchFailure(SearchFailureReason.NO_SAFE_SURFACE);
            return;
        }

        Block highestBlock = targetWorld.getHighestBlockAt(candidateLocation);
        if (highestBlock == null || highestBlock.getType().isAir()) {
            cannon.markSearchFailure(SearchFailureReason.NO_SAFE_SURFACE);
            return;
        }

        Location landingLocation = highestBlock.getLocation().add(0.5, 1, 0.5);
        Block feetBlock = landingLocation.getBlock();
        Block headBlock = landingLocation.clone().add(0, 1, 0).getBlock();
        if (!feetBlock.isPassable() || !headBlock.isPassable()) {
            cannon.markSearchFailure(SearchFailureReason.NO_SAFE_SURFACE);
            return;
        }
        if (!highestBlock.getType().isSolid() || highestBlock.isLiquid()) {
            cannon.markSearchFailure(SearchFailureReason.NO_SAFE_SURFACE);
            return;
        }
        if (LandingSearchConfig.isUnsafeGroundMaterial(highestBlock.getType()) ||
                LandingSearchConfig.isUnsafeBodyMaterial(feetBlock.getType()) ||
                LandingSearchConfig.isUnsafeBodyMaterial(headBlock.getType())) {
            cannon.markSearchFailure(SearchFailureReason.HAZARDOUS_TERRAIN);
            return;
        }

        ProtectionQueryResult protectionQueryResult = ProtectionManager.inspect(landingLocation);
        if (!protectionQueryResult.allowed()) {
            cannon.markSearchFailure(SearchFailureReason.PROTECTED_LAND);
            return;
        }

        // External listeners may veto a candidate after all built-in checks pass.
        CannonRTPLocationValidationEvent validationEvent = new CannonRTPLocationValidationEvent(
                cannon.getConfigId(), cannon.getDisplayName(), landingLocation.clone());
        Bukkit.getPluginManager().callEvent(validationEvent);
        if (validationEvent.isRejected()) {
            cannon.markSearchFailure(SearchFailureReason.PROTECTED_LAND);
            return;
        }

        cannon.markSearchSuccess(landingLocation);
        if (!cannon.needsMoreLocations()) {
            cannon.markReady();
        }
    }

    private Location randomizeLocation(World world, Location center, int minRadius, int maxRadius) {
        double angle = random.nextDouble() * Math.PI * 2;
        double distance = minRadius + random.nextDouble() * Math.max(1, maxRadius - minRadius);
        double x = center.getX() + Math.cos(angle) * distance;
        double z = center.getZ() + Math.sin(angle) * distance;
        return new Location(world, x, world.getMinHeight(), z);
    }

    private void renderParticleAnimation(ConfiguredCannonRTP cannon, Location cannonLocation) {
        World world = cannonLocation.getWorld();
        if (world == null) return;

        Location center = cannonLocation.clone().add(0, 1.0, 0);
        Color primaryColor = cannon.getPrimaryVisualColor();
        Color accentColor = cannon.getAccentVisualColor();
        double rotation = visualAnimationTick * 0.06;
        double orbitRadius = 0.78 + Math.sin(visualAnimationTick * 0.04) * 0.05;

        spawnOrbitTrack(world, center, rotation, orbitRadius, 0.22, 0.30, primaryColor, accentColor, 1.0f, true);
        spawnOrbitTrack(world, center, rotation + Math.PI, orbitRadius, 0.22, -0.30, accentColor, primaryColor, 1.0f, true);

        world.spawnParticle(
                Particle.DUST_COLOR_TRANSITION,
                center.clone().add(0, 0.18 + Math.sin(visualAnimationTick * 0.04) * 0.05, 0),
                1, 0, 0, 0, 0,
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
                1, 0, 0, 0, 0,
                new Particle.DustTransition(fromColor, toColor, size));

        if (!spawnFireworkTrail) return;
        world.spawnParticle(Particle.FIREWORK, particleLocation, 1, 0.015, 0.015, 0.015, 0.0);
    }

    private boolean hasPlayerWithinRange(Location location, double radius) {
        World world = location.getWorld();
        if (world == null) return false;
        double radiusSq = radius * radius;
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distanceSquared(location) <= radiusSq) return true;
        }
        return false;
    }

    private String sanitizeId(String id) {
        return id.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "_");
    }

    private String resolveFilename(String id) {
        return id.endsWith(".yml") ? id : sanitizeId(id) + ".yml";
    }

    private String stripYml(String filename) {
        return filename.endsWith(".yml") ? filename.substring(0, filename.length() - 4) : filename;
    }

    // ---------------------------------------------------------------------
    // Listener-facing handlers (called by the dedicated listener classes)
    // ---------------------------------------------------------------------

    public void handleChunkUnload(String worldName, int chunkX, int chunkZ) {
        List<ConfiguredCannonRTP> cannons = cannonsInChunk(worldName, chunkX, chunkZ);
        if (cannons == null) return;
        for (ConfiguredCannonRTP cannon : cannons) {
            cannon.remove(RemovalReason.CHUNK_UNLOAD);
            refreshActiveState(cannon);
        }
    }

    public void handleChunkLoad(String worldName, int chunkX, int chunkZ) {
        List<ConfiguredCannonRTP> cannons = cannonsInChunk(worldName, chunkX, chunkZ);
        if (cannons == null) return;
        for (ConfiguredCannonRTP cannon : cannons) {
            cannon.setChunkLoaded(true);
            cannon.handleChunkLoad();
            refreshActiveState(cannon);
        }
    }

    public void handleWorldLoad(String worldName) {
        for (ConfiguredCannonRTP cannon : configuredCannons.values()) {
            if (worldName.equals(cannon.getCannonWorldName())
                    || cannon.getLocationString().startsWith(worldName + ",")) {
                cannon.refreshWorldReference();
                indexCannon(cannon);
                refreshActiveState(cannon);
            }
            if (worldName.equals(cannon.getConfigFields().getTargetWorldName())) {
                cannon.invalidateCachedTargetWorld();
            }
        }
    }

    public void handleWorldUnload(String worldName) {
        for (ConfiguredCannonRTP cannon : configuredCannons.values()) {
            if (worldName.equals(cannon.getCannonWorldName())) {
                cannon.remove(RemovalReason.WORLD_UNLOAD);
                cannon.notifyCannonWorldUnloaded();
                refreshActiveState(cannon);
            }
            if (worldName.equals(cannon.getConfigFields().getTargetWorldName())) {
                cannon.invalidateCachedTargetWorld();
            }
        }
        cannonsByChunk.remove(worldName);
    }

    public void handlePlayerQuit(UUID playerId) {
        for (ConfiguredCannonRTP cannon : configuredCannons.values()) {
            cannon.clearNotifyThrottle(playerId);
        }
    }

    public void handleFMMStateChange() {
        for (ConfiguredCannonRTP cannon : configuredCannons.values()) {
            cannon.invalidateModelCache();
        }
    }

    // ---------------------------------------------------------------------
    // Chunk index + active-set maintenance
    // ---------------------------------------------------------------------

    private static long packChunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }

    private List<ConfiguredCannonRTP> cannonsInChunk(String worldName, int chunkX, int chunkZ) {
        Map<Long, List<ConfiguredCannonRTP>> chunkMap = cannonsByChunk.get(worldName);
        if (chunkMap == null) return null;
        return chunkMap.get(packChunkKey(chunkX, chunkZ));
    }

    private void indexCannon(ConfiguredCannonRTP cannon) {
        unindexCannon(cannon);
        String worldName = cannon.getCannonWorldName();
        if (worldName == null) return;
        long key = packChunkKey(cannon.getCannonChunkX(), cannon.getCannonChunkZ());
        cannonsByChunk
                .computeIfAbsent(worldName, k -> new HashMap<>())
                .computeIfAbsent(key, k -> new ArrayList<>(1))
                .add(cannon);
    }

    private void unindexCannon(ConfiguredCannonRTP cannon) {
        for (Map.Entry<String, Map<Long, List<ConfiguredCannonRTP>>> worldEntry : cannonsByChunk.entrySet()) {
            Map<Long, List<ConfiguredCannonRTP>> chunkMap = worldEntry.getValue();
            Iterator<Map.Entry<Long, List<ConfiguredCannonRTP>>> it = chunkMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Long, List<ConfiguredCannonRTP>> chunkEntry = it.next();
                if (chunkEntry.getValue().remove(cannon) && chunkEntry.getValue().isEmpty()) {
                    it.remove();
                }
            }
        }
    }

    private void refreshActiveState(ConfiguredCannonRTP cannon) {
        boolean shouldBeActive = cannon.isActive();
        boolean isActive = activeCannons.contains(cannon);
        if (shouldBeActive && !isActive) {
            activeCannons.add(cannon);
        } else if (!shouldBeActive && isActive) {
            activeCannons.remove(cannon);
        }
    }
}
