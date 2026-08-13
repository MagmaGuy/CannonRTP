package com.magmaguy.cannonrtp.content;

import com.magmaguy.cannonrtp.MetadataHandler;
import com.magmaguy.magmacore.nightbreak.NightbreakContentRefresher;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;

public class CannonRTPPackageRefresher {
    private static final Duration REFRESH_COOLDOWN = Duration.ofMinutes(5);
    private static final String CATALOG_KEY = "nightbreak-packages";

    private CannonRTPPackageRefresher() {
    }

    public static void refreshContentAndAccess() {
        NightbreakContentRefresher.refreshAsyncIfDue(
                (JavaPlugin) MetadataHandler.PLUGIN,
                CATALOG_KEY,
                REFRESH_COOLDOWN,
                () -> CannonRTPPackage.getCannonRTPPackages().values(),
                cannonRTPPackage -> true,
                outdated -> {
                });
    }

    public static void reset() {
        NightbreakContentRefresher.resetRefreshCooldown(
                (JavaPlugin) MetadataHandler.PLUGIN, CATALOG_KEY);
    }
}
