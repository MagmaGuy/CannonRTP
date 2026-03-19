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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CannonRTPManager implements Listener {
    private static final int CUSTOM_MODEL_CHARGE_TICKS = 60;
    private final CannonRTP plugin;
    private final Random random = new Random();
    private final Map<String, ConfiguredCannonRTP> configuredCannons = new LinkedHashMap<>();
    private final Map<UUID, Long> interactionCooldowns = new ConcurrentHashMap<>();
    private BukkitTask scanTask;
    private BukkitTask preloadTask;
    private BukkitTask visualTask;
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
        interactionCooldowns.clear();

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
        interactionCooldowns.clear();
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
        scanTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::scanCannons, 20L, 2L);
        preloadTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::preloadLandingLocations, 20L, 1L);
        visualTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::renderIdleVisuals, 20L, 1L);
    }

    private void shutdownTasks() {
        if (scanTask != null) {
            scanTask.cancel();
            scanTask = null;
        }
        if (preloadTask != null) {
            preloadTask.cancel();
            preloadTask = null;
        }
        if (visualTask != null) {
            visualTask.cancel();
            visualTask = null;
        }
    }

    private void scanCannons() {
        for (ConfiguredCannonRTP configuredCannonRTP : configuredCannons.values()) {
            Location cannonLocation = configuredCannonRTP.getCannonLocation();
            if (cannonLocation == null || cannonLocation.getWorld() == null || !configuredCannonRTP.isEnabled() || !configuredCannonRTP.isChunkLoaded()) {
                continue;
            }
            Collection<Player> nearbyPlayers = getNearbyPlayers(cannonLocation, configuredCannonRTP.getConfigFields().getTriggerRadius());
            for (Player player : nearbyPlayers) {
                if (!player.isOnline()) {
                    continue;
                }
                if (isOnInteractionCooldown(player.getUniqueId())) {
                    continue;
                }
                if (!player.hasPermission("cannonrtp.use")) {
                    setInteractionCooldown(player.getUniqueId());
                    continue;
                }
                if (!configuredCannonRTP.canUse(player)) {
                    MessageUtils.send(player, DefaultConfig.getNoPermissionMessage(), "cannon", configuredCannonRTP.getDisplayName());
                    setInteractionCooldown(player.getUniqueId());
                    continue;
                }
                if (configuredCannonRTP.getSearchState() == CannonSearchState.INVALID_CONFIGURATION) {
                    MessageUtils.send(player, DefaultConfig.getInvalidConfigurationMessage(),
                            "cannon", configuredCannonRTP.getDisplayName(),
                            "reason", configuredCannonRTP.getLastStatusDetail());
                    setInteractionCooldown(player.getUniqueId());
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
                    setInteractionCooldown(player.getUniqueId());
                    continue;
                }
                launchPlayer(player, configuredCannonRTP);
                setInteractionCooldown(player.getUniqueId());
            }
        }
    }

    private void preloadLandingLocations() {
        for (ConfiguredCannonRTP configuredCannonRTP : configuredCannons.values()) {
            if (!configuredCannonRTP.isEnabled()) {
                continue;
            }
            if (!configuredCannonRTP.isChunkLoaded()) {
                continue;
            }
            if (configuredCannonRTP.getSearchState() == CannonSearchState.INVALID_CONFIGURATION ||
                    configuredCannonRTP.getSearchState() == CannonSearchState.EXHAUSTED ||
                    !configuredCannonRTP.needsMoreLocations()) {
                continue;
            }

            World targetWorld = configuredCannonRTP.getTargetWorld();
            if (targetWorld == null) {
                configuredCannonRTP.markInvalidConfiguration("Target world " + configuredCannonRTP.getConfigFields().getTargetWorldName() + " is not loaded.");
                continue;
            }

            Location searchCenter = configuredCannonRTP.getResolvedSearchCenter();
            if (searchCenter == null || searchCenter.getWorld() == null) {
                configuredCannonRTP.markInvalidConfiguration("Search center is invalid.");
                continue;
            }

            for (int attempt = 0; attempt < DefaultConfig.getSearchAttemptsPerTick() && configuredCannonRTP.needsMoreLocations(); attempt++) {
                if (configuredCannonRTP.hasTimedOut()) {
                    configuredCannonRTP.exhaustSearch();
                    break;
                }
                attemptPreload(configuredCannonRTP, targetWorld, searchCenter);
            }
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

    private void renderIdleVisuals() {
        visualAnimationTick = (visualAnimationTick + 1) % 7200;
        int particleCadence = Math.max(1, DefaultConfig.getParticleIntervalTicks() / 5);
        for (ConfiguredCannonRTP configuredCannonRTP : configuredCannons.values()) {
            Location location = configuredCannonRTP.getCannonLocation();
            if (location == null || location.getWorld() == null || !configuredCannonRTP.isChunkLoaded()) {
                configuredCannonRTP.removeVisuals();
                continue;
            }

            configuredCannonRTP.refreshLabel();
            if (!configuredCannonRTP.isEnabled() || !configuredCannonRTP.getConfigFields().isEnableParticles()) {
                continue;
            }
            if (getNearbyPlayers(location, 36).isEmpty()) {
                continue;
            }
            if (visualAnimationTick % particleCadence != 0) {
                continue;
            }
            renderParticleAnimation(configuredCannonRTP, location);
        }
    }

    private void launchPlayer(Player player, ConfiguredCannonRTP configuredCannonRTP) {
        Location destination = configuredCannonRTP.consumeQueuedLocation();
        if (destination == null) {
            MessageUtils.send(player, DefaultConfig.getNoValidLocationYetMessage(), "cannon", configuredCannonRTP.getDisplayName());
            return;
        }

        player.removePotionEffect(PotionEffectType.LEVITATION);
        MessageUtils.sendTitle(player,
                DefaultConfig.getLaunchQueuedTitle(),
                DefaultConfig.getLaunchQueuedSubtitle(),
                "cannon", configuredCannonRTP.getDisplayName());

        if (configuredCannonRTP.shouldUseCustomLaunchAnimation()) {
            runCustomModelLaunchSequence(player, destination, configuredCannonRTP);
            return;
        }

        int warmupTicks = configuredCannonRTP.getConfigFields().getLaunchWarmupSeconds() * 20;
        if (warmupTicks <= 0) {
            sendLaunchConfirmedTitle(player, destination);
            boostAndTeleport(player, destination, configuredCannonRTP, false);
            return;
        }

        applyWarmupLevitation(player, warmupTicks, 1);
        new BukkitRunnable() {
            private int ticksRemaining = warmupTicks;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }
                if (ticksRemaining <= 0) {
                    cancel();
                    sendLaunchConfirmedTitle(player, destination);
                    boostAndTeleport(player, destination, configuredCannonRTP, false);
                    return;
                }
                Location previewLocation = generateCalibrationPreview(configuredCannonRTP, destination);
                MessageUtils.sendTitle(player,
                        DefaultConfig.getDestinationPreviewTitle(),
                        DefaultConfig.getDestinationPreviewSubtitle(),
                        0,
                        5,
                        0,
                        "x", String.format(Locale.US, "%.1f", previewLocation.getX()),
                        "y", String.format(Locale.US, "%.1f", previewLocation.getY()),
                        "z", String.format(Locale.US, "%.1f", previewLocation.getZ()));
                ticksRemaining--;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void runCustomModelLaunchSequence(Player player, Location destination, ConfiguredCannonRTP configuredCannonRTP) {
        Location cannonLocation = configuredCannonRTP.getCannonLocation();
        if (cannonLocation == null || cannonLocation.getWorld() == null) {
            sendLaunchConfirmedTitle(player, destination);
            boostAndTeleport(player, destination, configuredCannonRTP, false);
            return;
        }

        Location cannonSeatLocation = cannonLocation.clone().add(0, 1, 0);
        applyWarmupLevitation(player, CUSTOM_MODEL_CHARGE_TICKS, 0);
        player.teleport(cannonSeatLocation);
        player.setVelocity(new Vector());
        player.setFallDistance(0);

        new BukkitRunnable() {
            private int ticksRemaining = CUSTOM_MODEL_CHARGE_TICKS;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }
                if (ticksRemaining <= 0) {
                    cancel();
                    player.removePotionEffect(PotionEffectType.LEVITATION);
                    sendLaunchConfirmedTitle(player, destination);
                    boostAndTeleport(player, destination, configuredCannonRTP, true);
                    return;
                }

                player.teleport(cannonSeatLocation);
                player.setVelocity(new Vector());
                player.setFallDistance(0);

                Location previewLocation = generateCalibrationPreview(configuredCannonRTP, destination);
                MessageUtils.sendTitle(player,
                        DefaultConfig.getDestinationPreviewTitle(),
                        DefaultConfig.getDestinationPreviewSubtitle(),
                        0,
                        5,
                        0,
                        "x", String.format(Locale.US, "%.1f", previewLocation.getX()),
                        "y", String.format(Locale.US, "%.1f", previewLocation.getY()),
                        "z", String.format(Locale.US, "%.1f", previewLocation.getZ()));
                ticksRemaining--;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void applyWarmupLevitation(Player player, int durationTicks, int amplifier) {
        if (durationTicks <= 0) {
            return;
        }
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.LEVITATION,
                durationTicks,
                amplifier,
                true,
                false,
                false));
        if (DefaultConfig.getLevitationStartSound() != null) {
            player.playSound(player.getLocation(),
                    DefaultConfig.getLevitationStartSound(),
                    DefaultConfig.getLevitationStartSoundVolume(),
                    DefaultConfig.getLevitationStartSoundPitch());
        }
    }

    private void boostAndTeleport(Player player, Location destination, ConfiguredCannonRTP configuredCannonRTP, boolean spawnBlastoffExplosion) {
        if (DefaultConfig.getBlastOffSound() != null) {
            player.playSound(player.getLocation(),
                    DefaultConfig.getBlastOffSound(),
                    DefaultConfig.getBlastOffSoundVolume(),
                    DefaultConfig.getBlastOffSoundPitch());
        }
        if (spawnBlastoffExplosion) {
            spawnBlastoffExplosion(player.getLocation());
        }
        int verticalBoostTicks = configuredCannonRTP.getConfigFields().getVerticalBoostTicks();
        double verticalBoostVelocity = configuredCannonRTP.getConfigFields().getVerticalBoostVelocity();
        if (verticalBoostTicks <= 0 || verticalBoostVelocity <= 0) {
            completeTeleport(player, destination);
            return;
        }

        new BukkitRunnable() {
            private int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }
                if (ticks < verticalBoostTicks) {
                    player.setVelocity(new Vector(0, verticalBoostVelocity, 0));
                    spawnSmokeTrail(player.getLocation());
                } else {
                    completeTeleport(player, destination);
                    cancel();
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void completeTeleport(Player player, Location destination) {
        Location airdropLocation = destination.clone().add(0, 50, 0);
        World world = airdropLocation.getWorld();
        if (world != null) {
            double maxArrivalY = world.getMaxHeight() - 1;
            if (airdropLocation.getY() > maxArrivalY) {
                airdropLocation.setY(maxArrivalY);
            }
        }

        player.teleport(airdropLocation);
        player.setFallDistance(0);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, DefaultConfig.getSlowFallingSeconds() * 20, 0, true, false, false));
        trackLanding(player);
        MessageUtils.sendTitle(player, "", getRandomArrivalSubtitle());
    }

    private void sendLaunchConfirmedTitle(Player player, Location destination) {
        MessageUtils.sendTitle(player,
                DefaultConfig.getDestinationConfirmedTitle(),
                DefaultConfig.getDestinationConfirmedSubtitle(),
                "x", String.format(Locale.US, "%.1f", destination.getX()),
                "y", String.format(Locale.US, "%.1f", destination.getY()),
                "z", String.format(Locale.US, "%.1f", destination.getZ()),
                "world", destination.getWorld() == null ? "unknown" : destination.getWorld().getName());
    }

    private Location generateCalibrationPreview(ConfiguredCannonRTP configuredCannonRTP, Location destination) {
        World targetWorld = configuredCannonRTP.getTargetWorld();
        Location searchCenter = configuredCannonRTP.getResolvedSearchCenter();
        World previewWorld = targetWorld != null ? targetWorld : destination.getWorld();
        if (previewWorld == null) {
            return destination.clone();
        }

        Location anchor = searchCenter != null && searchCenter.getWorld() != null
                ? searchCenter
                : previewWorld.getSpawnLocation();

        int maxRadius = Math.max(configuredCannonRTP.getConfigFields().getMinSearchRadius() + 1,
                configuredCannonRTP.getConfigFields().getMaxSearchRadius());
        int minRadius = Math.max(0, configuredCannonRTP.getConfigFields().getMinSearchRadius());
        Location previewLocation = randomizeLocation(previewWorld, anchor, minRadius, maxRadius);
        previewLocation.setY(Math.max(previewWorld.getMinHeight(), previewLocation.getY()));
        return previewLocation;
    }

    private void trackLanding(Player player) {
        new BukkitRunnable() {
            private int ticksRemaining = DefaultConfig.getSlowFallingSeconds() * 20;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }
                spawnSmokeTrail(player.getLocation());
                if (hasLanded(player) || ticksRemaining <= 0) {
                    player.removePotionEffect(PotionEffectType.SLOW_FALLING);
                    cancel();
                    return;
                }

                ticksRemaining--;
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private boolean hasLanded(Player player) {
        Location location = player.getLocation();
        Block feetBlock = location.getBlock();
        Block supportBlock = location.clone().add(0, -0.2, 0).getBlock();

        if (!feetBlock.isPassable()) {
            return true;
        }

        return !supportBlock.isPassable() && Math.abs(player.getVelocity().getY()) < 0.08;
    }

    private boolean isOnInteractionCooldown(UUID uuid) {
        Long lastInteraction = interactionCooldowns.get(uuid);
        return lastInteraction != null && System.currentTimeMillis() - lastInteraction < 3000L;
    }

    private void setInteractionCooldown(UUID uuid) {
        interactionCooldowns.put(uuid, System.currentTimeMillis());
    }

    private String sanitizeId(String id) {
        return id.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "_");
    }

    private String resolveFilename(String id) {
        return id.endsWith(".yml") ? id : sanitizeId(id) + ".yml";
    }

    private void renderParticleAnimation(ConfiguredCannonRTP configuredCannonRTP, Location cannonLocation) {
        World world = cannonLocation.getWorld();
        if (world == null) {
            return;
        }

        Location center = cannonLocation.clone().add(0, 1.0, 0);
        Color primaryColor = configuredCannonRTP.getPrimaryVisualColor();
        Color accentColor = configuredCannonRTP.getAccentVisualColor();
        double rotation = visualAnimationTick * 0.26;
        double orbitRadius = 0.78 + Math.sin(visualAnimationTick * 0.14) * 0.05;

        for (int trailIndex = 0; trailIndex < 5; trailIndex++) {
            double trailPhase = trailIndex * 0.32;
            float size = Math.max(0.6f, 1.0f - trailIndex * 0.08f);
            spawnOrbitTrack(world, center, rotation - trailPhase, orbitRadius, 0.22, 0.30, primaryColor, accentColor, size, trailIndex == 0);
            spawnOrbitTrack(world, center, rotation + Math.PI - trailPhase, orbitRadius, 0.22, -0.30, accentColor, primaryColor, size, trailIndex == 0);
        }

        world.spawnParticle(
                Particle.DUST_COLOR_TRANSITION,
                center.clone().add(0, 0.18 + Math.sin(visualAnimationTick * 0.18) * 0.05, 0),
                2,
                0.08,
                0.08,
                0.08,
                0,
                new Particle.DustTransition(primaryColor, accentColor, 0.95f));
    }

    private void spawnOrbitTrack(World world, Location center, double angle, double radius, double baseHeight, double verticalWaveDirection,
                                 Color fromColor, Color toColor, float size, boolean spawnFireworkTrail) {
        double verticalWave = Math.sin(angle * 2.0 + visualAnimationTick * 0.08) * 0.20 * verticalWaveDirection;
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
        for (int step = 1; step <= 3; step++) {
            Location fallingSparkLocation = particleLocation.clone().add(0, -0.16 * step, 0);
            world.spawnParticle(Particle.FIREWORK, fallingSparkLocation, 1, 0.02, 0.02, 0.02, 0.01);
        }
    }

    private void spawnBlastoffExplosion(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }

        world.spawnParticle(Particle.EXPLOSION_EMITTER, location.clone().add(0, 0.4, 0), 2, 0.3, 0.3, 0.3, 0.0);
        world.spawnParticle(Particle.EXPLOSION, location.clone().add(0, 0.4, 0), 12, 0.45, 0.45, 0.45, 0.02);
        world.spawnParticle(Particle.FLAME, location.clone().add(0, 0.4, 0), 40, 0.3, 0.3, 0.3, 0.08);
        world.spawnParticle(Particle.LARGE_SMOKE, location.clone().add(0, 0.4, 0), 20, 0.3, 0.3, 0.3, 0.03);
    }

    private void spawnSmokeTrail(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }

        Location smokeLocation = location.clone().add(0, 0.2, 0);
        world.spawnParticle(Particle.LARGE_SMOKE, smokeLocation, 5, 0.18, 0.18, 0.18, 0.015);
        world.spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, smokeLocation, 2, 0.12, 0.12, 0.12, 0.0);
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

    private String getRandomArrivalSubtitle() {
        List<String> arrivalSubtitles = DefaultConfig.getArrivalSubtitles();
        if (arrivalSubtitles == null || arrivalSubtitles.isEmpty()) {
            return "<gradient:#ffffff:#cfe8ff>Good luck.</gradient>";
        }
        return arrivalSubtitles.get(random.nextInt(arrivalSubtitles.size()));
    }
}


