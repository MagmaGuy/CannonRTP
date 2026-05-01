package com.magmaguy.cannonrtp.util;

import java.util.List;
import java.util.Random;

/**
 * Picks a random entry from a list. Returns the fallback if the list is null or empty.
 */
public final class DialogPool {
    private static final Random RANDOM = new Random();

    private DialogPool() {
    }

    public static String pick(List<String> pool, String fallback) {
        if (pool == null || pool.isEmpty()) return fallback;
        return pool.get(RANDOM.nextInt(pool.size()));
    }
}
