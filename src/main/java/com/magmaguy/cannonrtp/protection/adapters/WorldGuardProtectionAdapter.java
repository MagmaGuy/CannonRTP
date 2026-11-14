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
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Location;

public class WorldGuardProtectionAdapter implements ProtectionAdapter {
    private static final String PLUGIN_NAME = "WorldGuard";

    // WorldGuard's singleton, platform and region container live as long as WorldGuard
    // is enabled. Resolved on the first query; ProtectionManager rebuilds this adapter
    // on every CannonRTP reload. Each query still gets its own RegionQuery.
    private volatile RegionContainer regionContainer;

    @Override
    public String getPluginName() {
        return PLUGIN_NAME;
    }

    @Override
    public ProtectionQueryResult query(Location location) throws Exception {
        RegionContainer regionContainer = this.regionContainer;
        if (regionContainer == null) {
            WorldGuard worldGuard = ProtectionAdapter.requireProvider(
                    WorldGuard.getInstance(),
                    "WorldGuard singleton");
            var platform = ProtectionAdapter.requireProvider(
                    worldGuard.getPlatform(),
                    "WorldGuard platform");
            regionContainer = ProtectionAdapter.requireProvider(
                    platform.getRegionContainer(),
                    "WorldGuard region container");
            this.regionContainer = regionContainer;
        }
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

