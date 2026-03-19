package com.magmaguy.cannonrtp.protection.adapters;

import com.magmaguy.cannonrtp.config.DefaultConfig;
import com.magmaguy.cannonrtp.protection.ProtectionAdapter;
import com.magmaguy.cannonrtp.protection.ProtectionQueryResult;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Location;

public class WorldGuardProtectionAdapter implements ProtectionAdapter {
    private static final String PLUGIN_NAME = "WorldGuard";

    @Override
    public String getPluginName() {
        return PLUGIN_NAME;
    }

    @Override
    public ProtectionQueryResult query(Location location) {
        ApplicableRegionSet regionSet = WorldGuard.getInstance()
                .getPlatform()
                .getRegionContainer()
                .createQuery()
                .getApplicableRegions(BukkitAdapter.adapt(location));

        if (regionSet == null) {
            return ProtectionQueryResult.pass();
        }

        boolean onlyGlobal = true;
        for (ProtectedRegion region : regionSet) {
            String regionId = region.getId();
            if (ProtectedRegion.GLOBAL_REGION.equalsIgnoreCase(regionId)) {
                continue;
            }
            onlyGlobal = false;

            StateFlag.State passthrough = region.getFlag(Flags.PASSTHROUGH);
            if (DefaultConfig.isWorldGuardAllowPassthroughRegions() && passthrough == StateFlag.State.ALLOW) {
                continue;
            }

            StateFlag.State build = region.getFlag(Flags.BUILD);
            if (DefaultConfig.isWorldGuardAllowBuildAllowedRegions() && build == StateFlag.State.ALLOW) {
                continue;
            }

            return blocked("protected region " + regionId);
        }

        if (onlyGlobal && DefaultConfig.isWorldGuardAllowGlobalRegionOnly()) {
            return ProtectionQueryResult.pass();
        }

        return onlyGlobal
                ? blocked("a global protection rule")
                : ProtectionQueryResult.pass();
    }

    private ProtectionQueryResult blocked(String reason) {
        return ProtectionQueryResult.blocked(PLUGIN_NAME, reason);
    }
}

