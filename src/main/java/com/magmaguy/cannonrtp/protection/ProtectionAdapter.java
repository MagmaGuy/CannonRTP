package com.magmaguy.cannonrtp.protection;

import org.bukkit.Location;

public interface ProtectionAdapter {
    String getPluginName();

    ProtectionQueryResult query(Location location) throws Exception;
}
