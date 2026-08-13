package com.magmaguy.cannonrtp.protection.adapters;

import com.magmaguy.cannonrtp.config.ProtectionSettingsConfig;
import com.magmaguy.cannonrtp.protection.ProtectionAdapter;
import com.magmaguy.cannonrtp.protection.ProtectionQueryResult;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Location;

public class GriefPreventionProtectionAdapter implements ProtectionAdapter {
    private static final String PLUGIN_NAME = "GriefPrevention";

    @Override
    public String getPluginName() {
        return PLUGIN_NAME;
    }

    @Override
    public ProtectionQueryResult query(Location location) throws Exception {
        GriefPrevention griefPrevention = ProtectionAdapter.requireProvider(
                GriefPrevention.instance,
                "GriefPrevention plugin singleton");
        var dataStore = ProtectionAdapter.requireProvider(
                griefPrevention.dataStore,
                "GriefPrevention data store");

        Claim claim = dataStore.getClaimAt(location, false, null);
        if (claim == null) {
            return ProtectionSettingsConfig.isGriefPreventionAllowWilderness()
                    ? ProtectionQueryResult.pass()
                    : blocked("GriefPrevention wilderness");
        }

        if (claim.isAdminClaim()) {
            return ProtectionSettingsConfig.isGriefPreventionAllowAdminClaims()
                    ? ProtectionQueryResult.pass()
                    : blocked("a GriefPrevention admin claim");
        }

        return ProtectionSettingsConfig.isGriefPreventionAllowPlayerClaims()
                ? ProtectionQueryResult.pass()
                : blocked("a GriefPrevention player claim");
    }
}

