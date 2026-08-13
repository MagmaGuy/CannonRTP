package com.magmaguy.cannonrtp.protection.adapters;

import com.magmaguy.cannonrtp.config.ProtectionSettingsConfig;
import com.magmaguy.cannonrtp.protection.ProtectionAdapter;
import com.magmaguy.cannonrtp.protection.ProtectionQueryResult;
import com.palmergames.bukkit.towny.TownyAPI;
import org.bukkit.Location;

public class TownyProtectionAdapter implements ProtectionAdapter {
    private static final String PLUGIN_NAME = "Towny";

    @Override
    public String getPluginName() {
        return PLUGIN_NAME;
    }

    @Override
    public ProtectionQueryResult query(Location location) throws Exception {
        if (location.getWorld() == null) {
            return ProtectionQueryResult.pass();
        }

        TownyAPI api = ProtectionAdapter.requireProvider(
                TownyAPI.getInstance(),
                "Towny API singleton");
        if (!api.isTownyWorld(location.getWorld())) {
            return ProtectionQueryResult.pass();
        }

        if (api.isWilderness(location)) {
            return ProtectionSettingsConfig.isTownyAllowWilderness()
                    ? ProtectionQueryResult.pass()
                    : blocked("Towny wilderness");
        }

        if (api.isNationZone(location)) {
            return ProtectionSettingsConfig.isTownyAllowNationZones()
                    ? ProtectionQueryResult.pass()
                    : blocked("a Towny nation zone");
        }

        return ProtectionSettingsConfig.isTownyAllowClaimedTownBlocks()
                ? ProtectionQueryResult.pass()
                : blocked("a claimed Towny town block");
    }
}

