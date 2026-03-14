package com.magmaguy.cannonrtp.protection;

import com.magmaguy.magmacore.util.Logger;
import com.magmaguy.cannonrtp.config.DefaultConfig;
import com.magmaguy.cannonrtp.protection.adapters.GriefPreventionProtectionAdapter;
import com.magmaguy.cannonrtp.protection.adapters.HuskClaimsProtectionAdapter;
import com.magmaguy.cannonrtp.protection.adapters.HuskTownsProtectionAdapter;
import com.magmaguy.cannonrtp.protection.adapters.LandsProtectionAdapter;
import com.magmaguy.cannonrtp.protection.adapters.TownyProtectionAdapter;
import com.magmaguy.cannonrtp.protection.adapters.WorldGuardProtectionAdapter;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

public class ProtectionQueryService {
    private final List<ProtectionAdapter> adapters = new ArrayList<>();

    public ProtectionQueryService() {
        adapters.add(new WorldGuardProtectionAdapter());
        adapters.add(new TownyProtectionAdapter());
        adapters.add(new LandsProtectionAdapter());
        adapters.add(new GriefPreventionProtectionAdapter());
        adapters.add(new HuskTownsProtectionAdapter());
        adapters.add(new HuskClaimsProtectionAdapter());
    }

    public boolean isPotentialLandingLocationAllowed(Location location) {
        return inspect(location).allowed();
    }

    public ProtectionQueryResult inspect(Location location) {
        for (ProtectionAdapter adapter : adapters) {
            if (!adapter.isAvailable()) {
                continue;
            }
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
}

