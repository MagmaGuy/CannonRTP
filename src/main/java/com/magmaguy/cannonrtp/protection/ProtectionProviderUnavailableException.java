package com.magmaguy.cannonrtp.protection;

/**
 * Signals that an installed protection plugin did not expose the provider/API
 * object needed for a trustworthy decision. The manager applies the configured
 * fail-open or fail-closed policy to this condition.
 */
public class ProtectionProviderUnavailableException extends Exception {
    public ProtectionProviderUnavailableException(String message) {
        super(message);
    }
}
