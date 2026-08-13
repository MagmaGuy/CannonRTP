package com.magmaguy.cannonrtp.services;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Prevents a listener that persistently cancels {@code CannonRTPLaunchEvent}
 * from being called every player-scan tick.
 */
final class LaunchCancellationBackoffTracker {
    private static final long BASE_DELAY_NANOS = 1_000_000_000L;
    private static final long MAX_DELAY_NANOS = 30_000_000_000L;

    private final Map<UUID, BackoffState> states = new HashMap<>();

    boolean isBlocked(UUID playerId, long nowNanos) {
        BackoffState state = states.get(playerId);
        return state != null && state.retryAtNanos() - nowNanos > 0;
    }

    void recordCancellation(UUID playerId, long nowNanos) {
        BackoffState previous = states.get(playerId);
        int cancellationCount = previous == null ? 1 : Math.min(previous.cancellationCount() + 1, 63);
        long delay = delayForCancellation(cancellationCount);
        states.put(playerId, new BackoffState(cancellationCount, saturatingAdd(nowNanos, delay)));
    }

    void clear(UUID playerId) {
        states.remove(playerId);
    }

    boolean isEmpty() {
        return states.isEmpty();
    }

    void retainPlayers(Set<UUID> playerIds) {
        states.keySet().retainAll(playerIds);
    }

    void clear() {
        states.clear();
    }

    private long delayForCancellation(int cancellationCount) {
        long delay = BASE_DELAY_NANOS;
        for (int i = 1; i < cancellationCount && delay < MAX_DELAY_NANOS; i++) {
            if (delay > MAX_DELAY_NANOS / 2) {
                return MAX_DELAY_NANOS;
            }
            delay *= 2;
        }
        return Math.min(delay, MAX_DELAY_NANOS);
    }

    private static long saturatingAdd(long value, long increment) {
        if (value > Long.MAX_VALUE - increment) {
            return Long.MAX_VALUE;
        }
        return value + increment;
    }

    private record BackoffState(int cancellationCount, long retryAtNanos) {
    }
}
