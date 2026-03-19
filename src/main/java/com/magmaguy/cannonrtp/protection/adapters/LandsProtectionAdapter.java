package com.magmaguy.cannonrtp.protection.adapters;

import com.magmaguy.cannonrtp.CannonRTP;
import com.magmaguy.cannonrtp.config.DefaultConfig;
import com.magmaguy.cannonrtp.protection.ProtectionAdapter;
import com.magmaguy.cannonrtp.protection.ProtectionQueryResult;
import me.angeschossen.lands.api.LandsIntegration;
import me.angeschossen.lands.api.land.Area;
import org.bukkit.Location;

public class LandsProtectionAdapter implements ProtectionAdapter {
    private static final String PLUGIN_NAME = "Lands";

    @Override
    public String getPluginName() {
        return PLUGIN_NAME;
    }

    @Override
    public ProtectionQueryResult query(Location location) {
        if (CannonRTP.getInstance() == null) {
            return ProtectionQueryResult.pass();
        }

        LandsIntegration integration = LandsIntegration.of(CannonRTP.getInstance());
        Area area = integration.getArea(location);
        if (area == null) {
            area = integration.getUnloadedArea(location);
        }
        if (area == null) {
            return DefaultConfig.isLandsAllowUnclaimedAreas()
                    ? ProtectionQueryResult.pass()
                    : blocked("an unclaimed Lands area that your config disallows");
        }

        return DefaultConfig.isLandsAllowClaimedAreas()
                ? ProtectionQueryResult.pass()
                : blocked("a claimed Lands area");
    }

    private ProtectionQueryResult blocked(String reason) {
        return ProtectionQueryResult.blocked(PLUGIN_NAME, reason);
    }
}

