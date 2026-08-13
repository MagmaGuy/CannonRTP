package com.magmaguy.cannonrtp.services;

import com.magmaguy.cannonrtp.api.CannonRTPLandingEvent;
import com.magmaguy.cannonrtp.api.CannonRTPLocationValidationEvent;
import com.magmaguy.cannonrtp.config.CannonMessagesConfig;
import com.magmaguy.cannonrtp.config.CannonSoundsConfig;
import com.magmaguy.cannonrtp.config.LandingSearchConfig;
import com.magmaguy.cannonrtp.protection.ProtectionManager;
import com.magmaguy.cannonrtp.util.MessageUtils;
import org.bukkit.Bukkit;
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
    private static final Random random = new Random();
    private static final int POTION_DURATION_TOLERANCE_TICKS = 2;

    private final Player player;
    private final ConfiguredCannonRTP cannon;
    private final Location destination;
    private final Location seatLocation;
    private final int searchingDurationTicks;
    private final int firingDurationTicks;
    private final double verticalBoostVelocity;
    private final int maxDroppingTicks;

    private LaunchPhase phase = LaunchPhase.SEARCHING;
    private int phaseTick = 0;
    private boolean finished = false;
    /**
     * Picked from the pool and color-converted once per launch in
     * {@link #enterSearching()}; the per-tick coordinate display only
     * re-formats the subtitle.
     */
    private String destinationPreviewTitle;
    /**
     * Per-launch invariants for the randomized coordinate preview, computed once
     * in {@link #enterSearching()} and reused by every warmup tick.
     */
    private boolean previewAvailable;
    private double previewAnchorX;
    private double previewAnchorZ;
    private int previewMinRadius;
    private int previewMaxRadius;
    private int previewMinY;
    private int elapsedTicks;
    private OwnedPotionEffect ownedLevitation;
    private OwnedPotionEffect ownedInvisibility;
    private OwnedPotionEffect ownedSlowFalling;
    private boolean ownsEntityInvisibility;

    private static final class OwnedPotionEffect {
        private final PotionEffect applied;
        private final PotionEffect previous;
        private final int appliedAtTick;
        private final int previousCapturedAtTick;
        private boolean released;

        private OwnedPotionEffect(PotionEffect applied, PotionEffect previous, int appliedAtTick) {
            this(applied, previous, appliedAtTick, appliedAtTick);
        }

        private OwnedPotionEffect(
                PotionEffect applied,
                PotionEffect previous,
                int appliedAtTick,
                int previousCapturedAtTick) {
            this.applied = applied;
            this.previous = previous;
            this.appliedAtTick = appliedAtTick;
            this.previousCapturedAtTick = previousCapturedAtTick;
        }

        private boolean matches(PotionEffect current, int currentTick) {
            if (current == null || !current.getType().equals(applied.getType())) return false;
            if (current.getAmplifier() != applied.getAmplifier()
                    || current.isAmbient() != applied.isAmbient()
                    || current.hasParticles() != applied.hasParticles()
                    || current.hasIcon() != applied.hasIcon()) {
                return false;
            }
            if (applied.isInfinite()) return current.isInfinite();
            if (current.isInfinite()) return false;

            int elapsed = Math.max(0, currentTick - appliedAtTick);
            int expectedDuration = Math.max(0, applied.getDuration() - elapsed);
            return Math.abs(current.getDuration() - expectedDuration)
                    <= POTION_DURATION_TOLERANCE_TICKS;
        }
    }

    public LaunchSequence(Player player, ConfiguredCannonRTP cannon, Location destination) {
        this.player = player;
        this.cannon = cannon;
        this.destination = destination;

        Location cannonLocation = cannon.getCannonLocation();
        this.seatLocation = cannonLocation != null ? cannonLocation.clone().add(0, 1, 0) : player.getLocation();

        this.searchingDurationTicks = Math.max(1, cannon.getConfigFields().getLaunchWarmupTicks());
        this.firingDurationTicks = Math.max(0, cannon.getConfigFields().getVerticalBoostTicks());
        this.verticalBoostVelocity = cannon.getConfigFields().getVerticalBoostVelocity();
        this.maxDroppingTicks = LandingSearchConfig.getSlowFallingSeconds() * 20;
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
        elapsedTicks++;
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

        if (phaseTick >= searchingDurationTicks - 1) {
            if (firingDurationTicks == 0) {
                // Zero explicitly disables the velocity phase, but the launch
                // still commits with its normal sound/title/effect feedback.
                enterFiring();
                transitionTo(LaunchPhase.TELEPORTING);
            } else {
                transitionTo(LaunchPhase.FIRING);
            }
        }
    }

    private void enterSearching() {
        destinationPreviewTitle = MessageUtils.format(CannonMessagesConfig.pickDestinationPreviewTitle());
        initializeCoordinatePreview();
        cannon.playFireAnimation();
        // Apply levitation for the searching duration
        ownedLevitation = applyOwnedPotionEffect(new PotionEffect(
                PotionEffectType.LEVITATION,
                searchingDurationTicks + 20, // slight buffer so it doesn't expire early
                0,
                true, false, false));

        // When a custom model is rendered, hide the player inside it so the animation reads cleanly.
        // Falls back to a visible player when no model is active (particle-only visuals).
        // Uses both the INVISIBILITY potion (hides from other players with ambient particles off)
        // and the entity invisibility flag via setInvisible(true) (also hides armor, belt-and-suspenders
        // in case another plugin or source interferes with the potion effect).
        if (cannon.hasActiveModel()) {
            boolean invisibilityAlreadyOwnedElsewhere = player.isInvisible()
                    || player.getPotionEffect(PotionEffectType.INVISIBILITY) != null;
            if (!invisibilityAlreadyOwnedElsewhere) {
                ownedInvisibility = applyOwnedPotionEffect(new PotionEffect(
                        PotionEffectType.INVISIBILITY,
                        searchingDurationTicks,
                        0,
                        true, false, false));
                if (ownedInvisibility != null) {
                    player.setInvisible(true);
                    ownsEntityInvisibility = true;
                }
            }
        }

        if (CannonSoundsConfig.getLevitationStartSound() != null) {
            player.playSound(player.getLocation(),
                    CannonSoundsConfig.getLevitationStartSound(),
                    CannonSoundsConfig.getLevitationStartSoundVolume(),
                    CannonSoundsConfig.getLevitationStartSoundPitch());
        }

        MessageUtils.sendTitle(player,
                CannonMessagesConfig.pickLaunchQueuedTitle(),
                CannonMessagesConfig.pickLaunchQueuedSubtitle(),
                "cannon", cannon.getDisplayName());
    }

    private void initializeCoordinatePreview() {
        World targetWorld = cannon.getTargetWorld();
        World previewWorld = targetWorld != null ? targetWorld : destination.getWorld();
        previewAvailable = previewWorld != null;
        if (!previewAvailable) return;

        Location searchCenter = cannon.getResolvedSearchCenter();
        Location anchor = searchCenter != null && searchCenter.getWorld() != null
                ? searchCenter
                : previewWorld.getSpawnLocation();
        previewAnchorX = anchor.getX();
        previewAnchorZ = anchor.getZ();
        // Config processing already guarantees minSearchRadius >= 0 and
        // maxSearchRadius >= minSearchRadius + 1.
        previewMinRadius = cannon.getConfigFields().getMinSearchRadius();
        previewMaxRadius = cannon.getConfigFields().getMaxSearchRadius();
        previewMinY = previewWorld.getMinHeight();
    }

    private void showRandomizedCoordinates() {
        if (!previewAvailable) return;

        double angle = random.nextDouble() * Math.PI * 2;
        double distance = previewMinRadius + random.nextDouble() * Math.max(1, previewMaxRadius - previewMinRadius);
        double x = previewAnchorX + Math.cos(angle) * distance;
        double z = previewAnchorZ + Math.sin(angle) * distance;
        double y = Math.max(previewMinY, 40 + random.nextInt(200));

        player.sendTitle(
                destinationPreviewTitle,
                MessageUtils.format(CannonMessagesConfig.getDestinationPreviewSubtitle(),
                        "x", String.format(Locale.US, "%.1f", x),
                        "y", String.format(Locale.US, "%.1f", y),
                        "z", String.format(Locale.US, "%.1f", z)),
                0, 5, 0);
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
        releaseOwnedPotionEffect(ownedLevitation);
        boolean invisibilityReleased = releaseOwnedPotionEffect(ownedInvisibility);
        releaseEntityInvisibility(invisibilityReleased);

        if (CannonSoundsConfig.getBlastOffSound() != null) {
            player.playSound(player.getLocation(),
                    CannonSoundsConfig.getBlastOffSound(),
                    CannonSoundsConfig.getBlastOffSoundVolume(),
                    CannonSoundsConfig.getBlastOffSoundPitch());
        }

        spawnBlastoffExplosion(player.getLocation());

        sendLaunchConfirmedTitle();
    }

    // --- TELEPORTING: single tick, teleport + slow falling ---

    private void tickTeleporting() {
        // Revalidate at commit time. A queued destination can become obstructed
        // or protected after preloading, and clamping an over-height arrival
        // would place the player inside the roof instead of preserving the
        // validated geometry.
        if (!CannonRTPManager.isCurrentWorldReference(destination)
                || LandingColumnValidator.validate(destination) != LandingColumnValidator.Result.SAFE
                || !ProtectionManager.inspect(destination).allowed()) {
            cancelAndRecover();
            return;
        }
        CannonRTPLocationValidationEvent validationEvent =
                new CannonRTPLocationValidationEvent(
                        cannon.getConfigId(),
                        cannon.getDisplayName(),
                        destination.clone());
        Bukkit.getPluginManager().callEvent(validationEvent);
        if (validationEvent.isRejected()) {
            cancelAndRecover();
            return;
        }

        Location airdropLocation = destination.clone().add(0, LandingColumnValidator.AIRDROP_HEIGHT_BLOCKS, 0);
        if (!player.teleport(airdropLocation)) {
            cancelAndRecover();
            return;
        }
        player.setFallDistance(0);
        player.setVelocity(new Vector());
        refreshSlowFallingSafety();

        MessageUtils.sendTitle(player,
                CannonMessagesConfig.pickArrivalTitle(),
                CannonMessagesConfig.pickArrivalSubtitle(),
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

        if (hasLanded()) {
            transitionTo(LaunchPhase.LANDING);
            return;
        }

        if (phaseTick >= maxDroppingTicks) {
            // A disappearing floor or external teleport must not leave an
            // immortal active launch that refreshes slow falling forever.
            // Recover to the cannon without emitting a successful landing.
            cancelAndRecover();
        }
    }

    // --- LANDING: impact burst, cleanup, fire CannonRTPLandingEvent ---

    private void tickLanding() {
        releaseOwnedPotionEffect(ownedSlowFalling);
        spawnLandingImpact(player.getLocation());
        fireLandingEvent();
        finished = true;
    }

    private void fireLandingEvent() {
        Bukkit.getPluginManager().callEvent(new CannonRTPLandingEvent(
                player,
                cannon.getConfigId(),
                cannon.getDisplayName(),
                destination.clone()));
    }

    // --- Helpers ---

    private void transitionTo(LaunchPhase nextPhase) {
        phase = nextPhase;
        phaseTick = -1; // will be incremented to 0 at end of tick()
    }

    /**
     * Removes any effects this sequence applied. Called when the player is detected
     * offline mid-launch and as the final step of cancelAndRecover().
     */
    private void cleanup() {
        cleanup(false);
    }

    private void cleanup(boolean retainSlowFallingForSafety) {
        finished = true;
        releaseOwnedPotionEffect(ownedLevitation);
        boolean invisibilityReleased = releaseOwnedPotionEffect(ownedInvisibility);
        releaseEntityInvisibility(invisibilityReleased);
        if (!retainSlowFallingForSafety) {
            releaseOwnedPotionEffect(ownedSlowFalling);
        }
        player.setFallDistance(0);
    }

    public boolean targetsWorld(String worldName) {
        World targetWorld = destination.getWorld();
        return targetWorld != null && worldName.equals(targetWorld.getName());
    }

    public void cancelForWorldUnload(String worldName) {
        cancelAndRecover(worldName);
    }

    public void cancelAndRecover() {
        cancelAndRecover(null);
    }

    public void terminateForQuit() {
        if (finished) return;
        cleanup();
    }

    private void cancelAndRecover(String unavailableWorldName) {
        if (finished) {
            return;
        }

        boolean recovered = false;
        if (player.isOnline()) {
            Location recoveryLocation = resolveRecoveryLocation(unavailableWorldName);
            if (recoveryLocation != null) {
                recovered = player.teleport(recoveryLocation);
                if (recovered) {
                    player.setVelocity(new Vector());
                    player.setFallDistance(0);
                }
            }
        }

        boolean retainSlowFallingForSafety = !recovered && player.isOnline();
        if (retainSlowFallingForSafety) {
            // If no non-unloading world can be resolved, retain safety rather
            // than stripping slow falling from an airborne player.
            refreshSlowFallingSafety();
        }
        cleanup(retainSlowFallingForSafety);
    }

    private Location resolveRecoveryLocation(String unavailableWorldName) {
        Location recoveredSeat = remapToLoadedWorld(seatLocation, unavailableWorldName);
        if (recoveredSeat != null) {
            return recoveredSeat;
        }

        World currentWorld = player.getWorld();
        if (currentWorld != null && !currentWorld.getName().equals(unavailableWorldName)) {
            return currentWorld.getSpawnLocation();
        }

        for (World world : Bukkit.getWorlds()) {
            if (!world.getName().equals(unavailableWorldName)) {
                return world.getSpawnLocation();
            }
        }
        return null;
    }

    private static Location remapToLoadedWorld(Location location, String unavailableWorldName) {
        if (location == null || location.getWorld() == null) {
            return null;
        }
        String worldName = location.getWorld().getName();
        if (worldName.equals(unavailableWorldName)) {
            return null;
        }
        World liveWorld = Bukkit.getWorld(worldName);
        if (liveWorld == null) {
            return null;
        }
        return new Location(
                liveWorld,
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch());
    }

    private void refreshSlowFallingSafety() {
        PotionEffect refreshed = new PotionEffect(
                PotionEffectType.SLOW_FALLING,
                maxDroppingTicks + 20,
                0, true, false, false);
        if (ownedSlowFalling == null || ownedSlowFalling.released) {
            ownedSlowFalling = applyOwnedPotionEffect(refreshed);
            return;
        }

        PotionEffect current = player.getPotionEffect(PotionEffectType.SLOW_FALLING);
        // A different live effect belongs to another source and already provides
        // slow-falling safety. Do not replace it merely because recovery failed.
        if (current != null && !ownedSlowFalling.matches(current, elapsedTicks)) return;
        if (current != null && (current.isInfinite()
                || current.getDuration() >= refreshed.getDuration())) {
            return;
        }
        if (!player.addPotionEffect(refreshed)) return;

        OwnedPotionEffect renewed = new OwnedPotionEffect(
                refreshed,
                ownedSlowFalling.previous,
                elapsedTicks,
                ownedSlowFalling.previousCapturedAtTick);
        PotionEffect renewedCurrent = player.getPotionEffect(PotionEffectType.SLOW_FALLING);
        if (renewed.matches(renewedCurrent, elapsedTicks)) {
            ownedSlowFalling.released = true;
            ownedSlowFalling = renewed;
        }
    }

    private OwnedPotionEffect applyOwnedPotionEffect(PotionEffect effect) {
        PotionEffect previous = player.getPotionEffect(effect.getType());
        if (!player.addPotionEffect(effect)) return null;

        PotionEffect current = player.getPotionEffect(effect.getType());
        OwnedPotionEffect owned = new OwnedPotionEffect(effect, previous, elapsedTicks);
        return owned.matches(current, elapsedTicks) ? owned : null;
    }

    private boolean releaseOwnedPotionEffect(OwnedPotionEffect owned) {
        if (owned == null || owned.released) return false;
        owned.released = true;

        PotionEffect current = player.getPotionEffect(owned.applied.getType());
        // A different effect means another source took ownership while the launch
        // was active. Leave it untouched instead of restoring stale state over it.
        if (current != null && !owned.matches(current, elapsedTicks)) return false;
        if (current != null) {
            player.removePotionEffect(owned.applied.getType());
        }

        PotionEffect previous = owned.previous;
        if (previous == null) return true;
        int remainingDuration = previous.getDuration();
        if (!previous.isInfinite()) {
            remainingDuration -= Math.max(0, elapsedTicks - owned.previousCapturedAtTick);
            if (remainingDuration <= 0) return true;
        }
        player.addPotionEffect(new PotionEffect(
                previous.getType(),
                remainingDuration,
                previous.getAmplifier(),
                previous.isAmbient(),
                previous.hasParticles(),
                previous.hasIcon()));
        return true;
    }

    private void releaseEntityInvisibility(boolean ownedEffectReleased) {
        if (!ownsEntityInvisibility) return;
        ownsEntityInvisibility = false;
        if (!ownedEffectReleased) return;
        // If another source already made the player visible, do not overwrite it.
        if (player.isInvisible()) {
            player.setInvisible(false);
        }
    }

    private boolean hasLanded() {
        // Player kept a deprecated duplicate of Entity#isOnGround; bind to the
        // non-deprecated Entity contract while using the same server state.
        return ((org.bukkit.entity.Entity) player).isOnGround()
                && Math.abs(player.getVelocity().getY()) < 0.08;
    }

    private void sendLaunchConfirmedTitle() {
        MessageUtils.sendTitle(player,
                CannonMessagesConfig.pickDestinationConfirmedTitle(),
                CannonMessagesConfig.getDestinationConfirmedSubtitle(),
                "x", String.format(Locale.US, "%.1f", destination.getX()),
                "y", String.format(Locale.US, "%.1f", destination.getY()),
                "z", String.format(Locale.US, "%.1f", destination.getZ()),
                "world", destination.getWorld() == null ? "unknown" : destination.getWorld().getName());
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
