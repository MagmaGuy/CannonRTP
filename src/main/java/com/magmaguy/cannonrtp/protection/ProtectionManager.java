package com.magmaguy.cannonrtp.protection;

import com.magmaguy.cannonrtp.config.DefaultConfig;
import com.magmaguy.cannonrtp.protection.adapters.GriefPreventionProtectionAdapter;
import com.magmaguy.cannonrtp.protection.adapters.HuskClaimsProtectionAdapter;
import com.magmaguy.cannonrtp.protection.adapters.HuskTownsProtectionAdapter;
import com.magmaguy.cannonrtp.protection.adapters.LandsProtectionAdapter;
import com.magmaguy.cannonrtp.protection.adapters.TownyProtectionAdapter;
import com.magmaguy.cannonrtp.protection.adapters.WorldGuardProtectionAdapter;
import com.magmaguy.magmacore.util.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class ProtectionManager {
    private static final List<ProtectionAdapter> activeAdapters = new ArrayList<>();

    public static void initialize() {
        activeAdapters.clear();
        tryRegister("WorldGuard", DefaultConfig.isWorldGuardEnabled(), WorldGuardProtectionAdapter::new);
        tryRegister("Towny", DefaultConfig.isTownyEnabled(), TownyProtectionAdapter::new);
        tryRegister("Lands", DefaultConfig.isLandsEnabled(), LandsProtectionAdapter::new);
        tryRegister("GriefPrevention", DefaultConfig.isGriefPreventionEnabled(), GriefPreventionProtectionAdapter::new);
        tryRegister("HuskTowns", DefaultConfig.isHuskTownsEnabled(), HuskTownsProtectionAdapter::new);
        tryRegister("HuskClaims", DefaultConfig.isHuskClaimsEnabled(), HuskClaimsProtectionAdapter::new);
    }

    private static void tryRegister(String pluginName, boolean configEnabled, Supplier<ProtectionAdapter> factory) {
        if (!configEnabled) return;
        if (Bukkit.getPluginManager().getPlugin(pluginName) == null) return;
        try {
            activeAdapters.add(factory.get());
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
