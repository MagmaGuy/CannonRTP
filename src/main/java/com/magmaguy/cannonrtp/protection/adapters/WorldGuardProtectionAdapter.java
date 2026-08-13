package com.magmaguy.cannonrtp.protection.adapters;

import com.magmaguy.cannonrtp.config.ProtectionSettingsConfig;
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
    public ProtectionQueryResult query(Location location) throws Exception {
        WorldGuard worldGuard = ProtectionAdapter.requireProvider(
                WorldGuard.getInstance(),
                "WorldGuard singleton");
        var platform = ProtectionAdapter.requireProvider(
                worldGuard.getPlatform(),
                "WorldGuard platform");
        var regionContainer = ProtectionAdapter.requireProvider(
                platform.getRegionContainer(),
                "WorldGuard region container");
        var regionQuery = ProtectionAdapter.requireProvider(
                regionContainer.createQuery(),
                "WorldGuard region query");
        ApplicableRegionSet regionSet = ProtectionAdapter.requireProvider(
                regionQuery.getApplicableRegions(BukkitAdapter.adapt(location)),
                "WorldGuard applicable-region set");

        boolean onlyGlobal = true;
        for (ProtectedRegion region : regionSet) {
            String regionId = region.getId();
            if (ProtectedRegion.GLOBAL_REGION.equalsIgnoreCase(regionId)) {
                continue;
            }
            onlyGlobal = false;

            StateFlag.State passthrough = region.getFlag(Flags.PASSTHROUGH);
            if (ProtectionSettingsConfig.isWorldGuardAllowPassthroughRegions() && passthrough == StateFlag.State.ALLOW) {
                continue;
            }

            StateFlag.State build = region.getFlag(Flags.BUILD);
            if (ProtectionSettingsConfig.isWorldGuardAllowBuildAllowedRegions() && build == StateFlag.State.ALLOW) {
                continue;
            }

            return blocked("protected region " + regionId);
        }

        if (onlyGlobal && !ProtectionSettingsConfig.isWorldGuardAllowGlobalRegionOnly()) {
            return blocked("a global protection rule");
        }
        return ProtectionQueryResult.pass();
    }
}

