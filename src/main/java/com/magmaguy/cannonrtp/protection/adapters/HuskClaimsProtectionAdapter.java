package com.magmaguy.cannonrtp.protection.adapters;

import com.magmaguy.cannonrtp.config.DefaultConfig;
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
    public ProtectionQueryResult query(Location location) {
        BukkitHuskClaimsAPI api = BukkitHuskClaimsAPI.getInstance();
        Optional<Claim> claim = api.getClaimAt(api.getPosition(location));
        if (claim.isEmpty()) {
            return DefaultConfig.isHuskClaimsAllowWilderness()
                    ? ProtectionQueryResult.pass()
                    : blocked("HuskClaims wilderness");
        }

        if (claim.get().isAdminClaim()) {
            return DefaultConfig.isHuskClaimsAllowAdminClaims()
                    ? ProtectionQueryResult.pass()
                    : blocked("a HuskClaims admin claim");
        }
        return DefaultConfig.isHuskClaimsAllowPlayerClaims()
                ? ProtectionQueryResult.pass()
                : blocked("a HuskClaims player claim");
    }

    private ProtectionQueryResult blocked(String reason) {
        return ProtectionQueryResult.blocked(PLUGIN_NAME, reason);
    }
}

