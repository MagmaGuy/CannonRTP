package com.magmaguy.cannonrtp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CannonRTPFirstTimeSetupContractTest {

    @Test
    void guidedSetupUsesTheRegisteredInitializeCommand() {
        assertFalse(CannonRTP.NIGHTBREAK_PLUGIN_SPEC.hasPresetModes());
        assertEquals(
                "/" + CannonRTP.NIGHTBREAK_PLUGIN_SPEC.rootCommand() + " initialize",
                CannonRTP.FIRST_TIME_SETUP_SPEC.initializeCommand());
    }
}
