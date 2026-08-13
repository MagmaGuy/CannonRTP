package com.magmaguy.cannonrtp.config.cannonrtps.premade;

import com.magmaguy.cannonrtp.config.CannonRTPConfigFields;

import java.util.ArrayList;

public class CannonRTPFree extends CannonRTPConfigFields {
    public CannonRTPFree() {
        super("cannonrtp_free_cannon",
                true,
                "&aFree Cannon",
                new ArrayList<>(),
                null,
                null);
        this.customModel = "cannonrtp";
    }
}
