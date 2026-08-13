package com.magmaguy.cannonrtp.protection.adapters;

import com.magmaguy.cannonrtp.CannonRTP;
import com.magmaguy.cannonrtp.config.ProtectionSettingsConfig;
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
    public ProtectionQueryResult query(Location location) throws Exception {
        CannonRTP plugin = ProtectionAdapter.requireProvider(
                CannonRTP.getInstance(),
                "CannonRTP plugin instance for Lands");
        LandsIntegration integration = ProtectionAdapter.requireProvider(
                LandsIntegration.of(plugin),
                "Lands integration");
        Area area = integration.getArea(location);
        if (area == null) {
            area = integration.getUnloadedArea(location);
        }
        if (area == null) {
            return ProtectionSettingsConfig.isLandsAllowUnclaimedAreas()
                    ? ProtectionQueryResult.pass()
                    : blocked("an unclaimed Lands area that your config disallows");
        }

        return ProtectionSettingsConfig.isLandsAllowClaimedAreas()
                ? ProtectionQueryResult.pass()
                : blocked("a claimed Lands area");
    }
}

