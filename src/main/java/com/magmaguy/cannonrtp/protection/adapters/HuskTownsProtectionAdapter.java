package com.magmaguy.cannonrtp.protection.adapters;

import com.magmaguy.cannonrtp.config.ProtectionSettingsConfig;
import com.magmaguy.cannonrtp.protection.ProtectionAdapter;
import com.magmaguy.cannonrtp.protection.ProtectionQueryResult;
import net.william278.husktowns.HuskTowns;
import net.william278.husktowns.api.BukkitHuskTownsAPI;
import net.william278.husktowns.claim.Claim;
import net.william278.husktowns.claim.TownClaim;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.Optional;

public class HuskTownsProtectionAdapter implements ProtectionAdapter {
    private static final String PLUGIN_NAME = "HuskTowns";

    @Override
    public String getPluginName() {
        return PLUGIN_NAME;
    }

    @Override
    public ProtectionQueryResult query(Location location) throws Exception {
        BukkitHuskTownsAPI api = ProtectionAdapter.requireProvider(
                BukkitHuskTownsAPI.getInstance(),
                "HuskTowns Bukkit API");
        Optional<TownClaim> townClaim = ProtectionAdapter.requireProvider(
                api.getClaimAt(location),
                "HuskTowns claim query");
        if (townClaim.isEmpty()) {
            return ProtectionSettingsConfig.isHuskTownsAllowWilderness()
                    ? ProtectionQueryResult.pass()
                    : blocked("HuskTowns wilderness");
        }

        HuskTowns plugin = ProtectionAdapter.requireProvider(
                (HuskTowns) Bukkit.getPluginManager().getPlugin(PLUGIN_NAME),
                "HuskTowns plugin instance");
        if (townClaim.get().isAdminClaim(plugin)) {
            return ProtectionSettingsConfig.isHuskTownsAllowAdminClaims()
                    ? ProtectionQueryResult.pass()
                    : blocked("a HuskTowns admin claim");
        }

        Claim.Type type = townClaim.get().claim().getType();
        if (type == Claim.Type.FARM) {
            return ProtectionSettingsConfig.isHuskTownsAllowFarmClaims()
                    ? ProtectionQueryResult.pass()
                    : blocked("a HuskTowns farm claim");
        }
        if (type == Claim.Type.PLOT) {
            return ProtectionSettingsConfig.isHuskTownsAllowPlotClaims()
                    ? ProtectionQueryResult.pass()
                    : blocked("a HuskTowns plot claim");
        }
        return ProtectionSettingsConfig.isHuskTownsAllowRegularClaims()
                ? ProtectionQueryResult.pass()
                : blocked("a HuskTowns town claim");
    }
}

