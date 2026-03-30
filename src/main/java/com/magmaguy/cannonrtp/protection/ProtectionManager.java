package com.magmaguy.cannonrtp.protection;

import com.magmaguy.cannonrtp.config.DefaultConfig;
import com.magmaguy.magmacore.util.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ProtectionManager {
    private static final List<ProtectionAdapter> activeAdapters = new ArrayList<>();

    private static final String ADAPTERS_PACKAGE = "com.magmaguy.cannonrtp.protection.adapters.";

    public static void initialize() {
        activeAdapters.clear();
        tryRegister("WorldGuard", DefaultConfig.isWorldGuardEnabled(), ADAPTERS_PACKAGE + "WorldGuardProtectionAdapter");
        tryRegister("Towny", DefaultConfig.isTownyEnabled(), ADAPTERS_PACKAGE + "TownyProtectionAdapter");
        tryRegister("Lands", DefaultConfig.isLandsEnabled(), ADAPTERS_PACKAGE + "LandsProtectionAdapter");
        tryRegister("GriefPrevention", DefaultConfig.isGriefPreventionEnabled(), ADAPTERS_PACKAGE + "GriefPreventionProtectionAdapter");
        tryRegister("HuskTowns", DefaultConfig.isHuskTownsEnabled(), ADAPTERS_PACKAGE + "HuskTownsProtectionAdapter");
        tryRegister("HuskClaims", DefaultConfig.isHuskClaimsEnabled(), ADAPTERS_PACKAGE + "HuskClaimsProtectionAdapter");
    }

    private static void tryRegister(String pluginName, boolean configEnabled, String adapterClassName) {
        if (!configEnabled) return;
        if (Bukkit.getPluginManager().getPlugin(pluginName) == null) return;
        try {
            Class<?> clazz = Class.forName(adapterClassName);
            activeAdapters.add((ProtectionAdapter) clazz.getDeclaredConstructor().newInstance());
            Logger.info("Hooked into " + pluginName + " for landing protection checks.");
        } catch (NoClassDefFoundError | Exception e) {
            Logger.warn("Failed to hook into " + pluginName + ": " + e.getMessage());
        }
    }

    public static boolean isPotentialLandingLocationAllowed(Location location) {
        return inspect(location).allowed();
    }

    public static ProtectionQueryResult inspect(Location location) {
        for (ProtectionAdapter adapter : activeAdapters) {
            try {
                ProtectionQueryResult result = adapter.query(location);
                if (!result.allowed()) {
                    return result;
                }
            } catch (Exception exception) {
                Logger.warn("Failed to query " + adapter.getPluginName() + " protection at " + location + ": " + exception.getMessage());
                if (!DefaultConfig.isFailOpenOnProtectionErrors()) {
                    return ProtectionQueryResult.blocked(adapter.getPluginName(), "its API could not be queried safely");
                }
            }
        }
        return ProtectionQueryResult.pass();
    }

    public static List<ProtectionAdapter> getActiveAdapters() {
        return Collections.unmodifiableList(activeAdapters);
    }

    public static void shutdown() {
        activeAdapters.clear();
    }
}
