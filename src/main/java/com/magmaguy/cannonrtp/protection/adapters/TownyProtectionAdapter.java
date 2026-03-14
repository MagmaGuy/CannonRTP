package com.magmaguy.cannonrtp.protection.adapters;

import com.magmaguy.cannonrtp.config.DefaultConfig;
import com.magmaguy.cannonrtp.protection.ProtectionAdapter;
import com.magmaguy.cannonrtp.protection.ProtectionQueryResult;
import com.palmergames.bukkit.towny.TownyAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;

public class TownyProtectionAdapter implements ProtectionAdapter {
    private static final String PLUGIN_NAME = "Towny";

    @Override
    public String getPluginName() {
        return PLUGIN_NAME;
    }

    @Override
    public boolean isAvailable() {
        return DefaultConfig.isTownyEnabled() &&
                Bukkit.getPluginManager().isPluginEnabled(PLUGIN_NAME);
    }

    @Override
    public ProtectionQueryResult query(Location location) {
        if (location.getWorld() == null) {
            return ProtectionQueryResult.pass();
        }

        TownyAPI api = TownyAPI.getInstance();
        if (!api.isTownyWorld(location.getWorld())) {
            return ProtectionQueryResult.pass();
        }

        if (api.isWilderness(location)) {
            return DefaultConfig.isTownyAllowWilderness()
                    ? ProtectionQueryResult.pass()
                    : blocked("Towny wilderness");
        }

        if (api.isNationZone(location)) {
            return DefaultConfig.isTownyAllowNationZones()
                    ? ProtectionQueryResult.pass()
                    : blocked("a Towny nation zone");
        }

        return DefaultConfig.isTownyAllowClaimedTownBlocks()
                ? ProtectionQueryResult.pass()
                : blocked("a claimed Towny town block");
    }

    private ProtectionQueryResult blocked(String reason) {
        return ProtectionQueryResult.blocked(PLUGIN_NAME, reason);
    }
}

