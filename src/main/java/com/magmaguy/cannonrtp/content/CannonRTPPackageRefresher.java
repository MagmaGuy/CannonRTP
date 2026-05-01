package com.magmaguy.cannonrtp.content;

import com.magmaguy.cannonrtp.MetadataHandler;
import com.magmaguy.magmacore.nightbreak.NightbreakContentRefresher;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;

public class CannonRTPPackageRefresher {
    private static final long REFRESH_COOLDOWN_MS = 5 * 60 * 1000L;
    private static long lastRefresh = 0L;

    private CannonRTPPackageRefresher() {
    }

    public static void refreshContentAndAccess() {
        long now = System.currentTimeMillis();
        if (now - lastRefresh < REFRESH_COOLDOWN_MS) return;
        lastRefresh = now;
        NightbreakContentRefresher.refreshAsync(
                (JavaPlugin) MetadataHandler.PLUGIN,
                new ArrayList<>(CannonRTPPackage.getCannonRTPPackages().values()),
                cannonRTPPackage -> true,
                outdated -> {
                });
    }

    public static void reset() {
        lastRefresh = 0L;
    }
}
