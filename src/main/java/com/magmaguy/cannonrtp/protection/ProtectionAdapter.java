package com.magmaguy.cannonrtp.protection;

import org.bukkit.Location;

public interface ProtectionAdapter {
    String getPluginName();

    boolean isAvailable();

    ProtectionQueryResult query(Location location) throws Exception;
}

