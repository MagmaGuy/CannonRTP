package com.magmaguy.cannonrtp.services;

import com.magmaguy.cannonrtp.config.DefaultConfig;
import com.magmaguy.cannonrtp.util.MessageUtils;
import lombok.Getter;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Locale;
import java.util.Random;

public class LaunchSequence {
    private static final int SEARCHING_DURATION_TICKS = 66;
    private static final Random random = new Random();

    @Getter
    private final Player player;
    @Getter
    private final ConfiguredCannonRTP cannon;
    @Getter
    private final Location destination;
    private final Location seatLocation;
    private final int firingDurationTicks;
    private final double verticalBoostVelocity;
    private final int maxDroppingTicks;

    @Getter
    private LaunchPhase phase = LaunchPhase.SEARCHING;
    private int phaseTick = 0;
    @Getter
    private boolean finished = false;

    public LaunchSequence(Player player, ConfiguredCannonRTP cannon, Location destination) {
        this.player = player;
        this.cannon = cannon;
        this.destination = destination;

        Location cannonLocation = cannon.getCannonLocation();
        this.seatLocation = cannonLocation != null ? cannonLocation.clone().add(0, 1, 0) : player.getLocation();

        this.firingDurationTicks = Math.max(1, cannon.getConfigFields().getVerticalBoostTicks());
        this.verticalBoostVelocity = cannon.getConfigFields().getVerticalBoostVelocity();
        this.maxDroppingTicks = DefaultConfig.getSlowFallingSeconds() * 20;
    }

    /**
     * Called once per server tick by the manager. Returns true while still active,
     * false when finished (the manager should remove this sequence).
     */
    public boolean tick() {
        if (finished) return false;

        if (!player.isOnline()) {
            cleanup();
            return false;
        }

        switch (phase) {
            case SEARCHING -> tickSearching();
            case FIRING -> tickFiring();
            case TELEPORTING -> tickTeleporting();
            case DROPPING -> tickDropping();
            case LANDING -> tickLanding();
        }

        phaseTick++;
        return !finished;
    }

    // --- SEARCHING: ticks 0-65, player anchored, randomized coords displayed ---

    private void tickSearching() {
        if (phaseTick == 0) {
            enterSearching();
        }

        player.setFallDistance(0);

        // Randomized coordinate display every tick
        showRandomizedCoordinates();

        if (phaseTick >= SEARCHING_DURATION_TICKS - 1) {
            transitionTo(LaunchPhase.FIRING);
        }
    }

    private void enterSearching() {
        // Apply levitation for the searching duration
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.LEVITATION,
                SEARCHING_DURATION_TICKS + 20, // slight buffer so it doesn't expire early
                0,
                true, false, false));

        if (DefaultConfig.getLevitationStartSound() != null) {
            player.playSound(player.getLocation(),
                    DefaultConfig.getLevitationStartSound(),
                    DefaultConfig.getLevitationStartSoundVolume(),
                    DefaultConfig.getLevitationStartSoundPitch());
        }

        MessageUtils.sendTitle(player,
                DefaultConfig.getLaunchQueuedTitle(),
                DefaultConfig.getLaunchQueuedSubtitle(),
                "cannon", cannon.getDisplayName());
    }

    private void showRandomizedCoordinates() {
        World targetWorld = cannon.getTargetWorld();
        Location searchCenter = cannon.getResolvedSearchCenter();
        World previewWorld = targetWorld != null ? targetWorld : destination.getWorld();
        if (previewWorld == null) return;

        Location anchor = searchCenter != null && searchCenter.getWorld() != null
                ? searchCenter
                : previewWorld.getSpawnLocation();

        int maxRadius = Math.max(cannon.getConfigFields().getMinSearchRadius() + 1,
                cannon.getConfigFields().getMaxSearchRadius());
        int minRadius = Math.max(0, cannon.getConfigFields().getMinSearchRadius());

        double angle = random.nextDouble() * Math.PI * 2;
        double distance = minRadius + random.nextDouble() * Math.max(1, maxRadius - minRadius);
        double x = anchor.getX() + Math.cos(angle) * distance;
        double z = anchor.getZ() + Math.sin(angle) * distance;
        double y = Math.max(previewWorld.getMinHeight(), 40 + random.nextInt(200));

        MessageUtils.sendTitle(player,
                DefaultConfig.getDestinationPreviewTitle(),
                DefaultConfig.getDestinationPreviewSubtitle(),
                0, 5, 0,
                "x", String.format(Locale.US, "%.1f", x),
                "y", String.format(Locale.US, "%.1f", y),
                "z", String.format(Locale.US, "%.1f", z));
    }

    // --- FIRING: vertical boost upward ---

    private void tickFiring() {
        if (phaseTick == 0) {
            enterFiring();
        }

        player.setVelocity(new Vector(0, verticalBoostVelocity, 0));
        spawnSmokeTrail(player.getLocation(), 0);

        if (phaseTick >= firingDurationTicks - 1) {
            transitionTo(LaunchPhase.TELEPORTING);
        }
    }

    private void enterFiring() {
        player.removePotionEffect(PotionEffectType.LEVITATION);

        if (DefaultConfig.getBlastOffSound() != null) {
            player.playSound(player.getLocation(),
                    DefaultConfig.getBlastOffSound(),
                    DefaultConfig.getBlastOffSoundVolume(),
                    DefaultConfig.getBlastOffSoundPitch());
        }

        spawnBlastoffExplosion(player.getLocation());

        sendLaunchConfirmedTitle();
    }

    // --- TELEPORTING: single tick, teleport + slow falling ---

    private void tickTeleporting() {
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
        player.setVelocity(new Vector());
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOW_FALLING,
                maxDroppingTicks + 20, // buffer
                0, true, false, false));

        MessageUtils.sendTitle(player,
                getRandomArrivalTitle(),
                DefaultConfig.getDestinationConfirmedSubtitle(),
                0, 60, 20,
                "x", String.format(Locale.US, "%.1f", destination.getX()),
                "y", String.format(Locale.US, "%.1f", destination.getY()),
                "z", String.format(Locale.US, "%.1f", destination.getZ()),
                "world", destination.getWorld() == null ? "unknown" : destination.getWorld().getName());

        transitionTo(LaunchPhase.DROPPING);
    }

    // --- DROPPING: smoke trail, check for landing ---

    private void tickDropping() {
        spawnSmokeTrail(player.getLocation(), 3);

        if (hasLanded() || phaseTick >= maxDroppingTicks) {
            transitionTo(LaunchPhase.LANDING);
        }
    }

    // --- LANDING: impact burst, cleanup ---

    private void tickLanding() {
        player.removePotionEffect(PotionEffectType.SLOW_FALLING);
        spawnLandingImpact(player.getLocation());
        finished = true;
    }

    // --- Helpers ---

    private void transitionTo(LaunchPhase nextPhase) {
        phase = nextPhase;
        phaseTick = -1; // will be incremented to 0 at end of tick()
    }

    /**
     * Emergency cleanup -- called on disconnect or shutdown.
     * Removes any effects this sequence applied.
     */
    public void cleanup() {
        finished = true;
        if (player.isOnline()) {
            player.removePotionEffect(PotionEffectType.LEVITATION);
            player.removePotionEffect(PotionEffectType.SLOW_FALLING);
            player.setFallDistance(0);
        }
    }

    private boolean hasLanded() {
        Location location = player.getLocation();
        org.bukkit.block.Block feetBlock = location.getBlock();
        org.bukkit.block.Block supportBlock = location.clone().add(0, -0.2, 0).getBlock();

        if (!feetBlock.isPassable()) return true;
        return !supportBlock.isPassable() && Math.abs(player.getVelocity().getY()) < 0.08;
    }

    private void sendLaunchConfirmedTitle() {
        MessageUtils.sendTitle(player,
                DefaultConfig.getDestinationConfirmedTitle(),
                DefaultConfig.getDestinationConfirmedSubtitle(),
                "x", String.format(Locale.US, "%.1f", destination.getX()),
                "y", String.format(Locale.US, "%.1f", destination.getY()),
                "z", String.format(Locale.US, "%.1f", destination.getZ()),
                "world", destination.getWorld() == null ? "unknown" : destination.getWorld().getName());
    }

    private String getRandomArrivalTitle() {
        java.util.List<String> titles = DefaultConfig.getArrivalSubtitles();
        if (titles == null || titles.isEmpty()) {
            return "<gradient:#ffffff:#cfe8ff>Good luck.</gradient>";
        }
        return titles.get(random.nextInt(titles.size()));
    }

    private void spawnSmokeTrail(Location location, double yOffset) {
        World world = location.getWorld();
        if (world == null) return;
        Location smokeLocation = location.clone().add(0, yOffset, 0);
        world.spawnParticle(Particle.LARGE_SMOKE, smokeLocation, 5, 0.18, 0.18, 0.18, 0.015);
        world.spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, smokeLocation, 2, 0.12, 0.12, 0.12, 0.0);
    }

    private void spawnBlastoffExplosion(Location location) {
        World world = location.getWorld();
        if (world == null) return;
        Location effectLoc = location.clone().add(0, 0.4, 0);
        world.spawnParticle(Particle.EXPLOSION_EMITTER, effectLoc, 2, 0.3, 0.3, 0.3, 0.0);
        world.spawnParticle(Particle.EXPLOSION, effectLoc, 12, 0.45, 0.45, 0.45, 0.02);
        world.spawnParticle(Particle.FLAME, effectLoc, 40, 0.3, 0.3, 0.3, 0.08);
        world.spawnParticle(Particle.LARGE_SMOKE, effectLoc, 20, 0.3, 0.3, 0.3, 0.03);
    }

    private void spawnLandingImpact(Location location) {
        World world = location.getWorld();
        if (world == null) return;
        Location impactLoc = location.clone();
        // Large shockwave ring expanding outward
        world.spawnParticle(Particle.CLOUD, impactLoc, 60, 1.5, 0.2, 1.5, 0.15);
        world.spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, impactLoc, 30, 1.0, 0.3, 1.0, 0.08);
        // Explosion flash
        world.spawnParticle(Particle.EXPLOSION, impactLoc, 5, 0.5, 0.3, 0.5, 0.02);
        world.spawnParticle(Particle.FLAME, impactLoc, 40, 1.2, 0.2, 1.2, 0.1);
        // Upward smoke column
        world.spawnParticle(Particle.LARGE_SMOKE, impactLoc.clone().add(0, 0.5, 0), 25, 0.4, 0.8, 0.4, 0.06);
        // Dust ring
        world.spawnParticle(Particle.DUST_COLOR_TRANSITION, impactLoc,
                30, 1.8, 0.15, 1.8, 0.1,
                new Particle.DustTransition(Color.fromRGB(255, 220, 160), Color.fromRGB(180, 140, 100), 1.8f));
    }
}
