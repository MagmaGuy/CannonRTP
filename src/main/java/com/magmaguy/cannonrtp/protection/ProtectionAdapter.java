package com.magmaguy.cannonrtp.protection;

import org.bukkit.Location;

public interface ProtectionAdapter {
    String getPluginName();

    ProtectionQueryResult query(Location location) throws Exception;

    default ProtectionQueryResult blocked(String reason) {
        return ProtectionQueryResult.blocked(getPluginName(), reason);
    }

    static <T> T requireProvider(T provider, String providerDescription)
            throws ProtectionProviderUnavailableException {
        if (provider == null) {
            throw new ProtectionProviderUnavailableException(providerDescription + " is unavailable");
        }
        return provider;
    }
}
