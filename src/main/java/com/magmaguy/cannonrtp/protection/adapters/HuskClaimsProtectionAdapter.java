package com.magmaguy.cannonrtp.protection.adapters;

import com.magmaguy.cannonrtp.config.ProtectionSettingsConfig;
import com.magmaguy.cannonrtp.protection.ProtectionAdapter;
import com.magmaguy.cannonrtp.protection.ProtectionQueryResult;
import net.william278.huskclaims.api.BukkitHuskClaimsAPI;
import net.william278.huskclaims.claim.Claim;
import org.bukkit.Location;

import java.util.Optional;

public class HuskClaimsProtectionAdapter implements ProtectionAdapter {
    private static final String PLUGIN_NAME = "HuskClaims";

    @Override
    public String getPluginName() {
        return PLUGIN_NAME;
    }

    @Override
    public ProtectionQueryResult query(Location location) throws Exception {
        BukkitHuskClaimsAPI api = ProtectionAdapter.requireProvider(
                BukkitHuskClaimsAPI.getInstance(),
                "HuskClaims Bukkit API");
        var position = ProtectionAdapter.requireProvider(
                api.getPosition(location),
                "HuskClaims position");
        Optional<Claim> claim = ProtectionAdapter.requireProvider(
                api.getClaimAt(position),
                "HuskClaims claim query");
        if (claim.isEmpty()) {
            return ProtectionSettingsConfig.isHuskClaimsAllowWilderness()
                    ? ProtectionQueryResult.pass()
                    : blocked("HuskClaims wilderness");
        }

        if (claim.get().isAdminClaim()) {
            return ProtectionSettingsConfig.isHuskClaimsAllowAdminClaims()
                    ? ProtectionQueryResult.pass()
                    : blocked("a HuskClaims admin claim");
        }
        return ProtectionSettingsConfig.isHuskClaimsAllowPlayerClaims()
                ? ProtectionQueryResult.pass()
                : blocked("a HuskClaims player claim");
    }
}

