package com.magmaguy.cannonrtp.services;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Remembers players whose entry into a cannon already started a launch.
 *
 * <p>The latch is released only after the player is outside the trigger and no
 * launch is active. This matters when a failed launch recovers the player to
 * the cannon seat: recovery must not be mistaken for a fresh trigger entry.</p>
 */
final class LaunchTriggerLatch {
    private final Set<UUID> latchedPlayers = new HashSet<>();

    boolean isLatched(UUID playerId) {
        return latchedPlayers.contains(playerId);
    }

    Set<UUID> snapshot() {
        return Set.copyOf(latchedPlayers);
    }

    void latch(UUID playerId) {
        latchedPlayers.add(playerId);
    }

    void observePosition(UUID playerId, boolean insideTrigger, boolean launchActive) {
        if (!insideTrigger && !launchActive) {
            latchedPlayers.remove(playerId);
        }
    }

    void release(UUID playerId) {
        latchedPlayers.remove(playerId);
    }

    void clear() {
        latchedPlayers.clear();
    }
}
