package com.magmaguy.cannonrtp.protection;

public record ProtectionQueryResult(boolean allowed, String pluginName, String reason) {
    public static ProtectionQueryResult pass() {
        return new ProtectionQueryResult(true, "", "");
    }

    public static ProtectionQueryResult blocked(String pluginName, String reason) {
        return new ProtectionQueryResult(false, pluginName, reason);
    }
}

