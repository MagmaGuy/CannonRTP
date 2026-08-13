package com.magmaguy.cannonrtp.config.cannonrtps.premade;

import com.magmaguy.cannonrtp.config.CannonRTPConfigFields;

import java.util.ArrayList;

public class CannonRTPPremium extends CannonRTPConfigFields {
    public CannonRTPPremium() {
        super("cannonrtp_premium_cannon",
                true,
                "&5Premium Cannon",
                new ArrayList<>(),
                null,
                null);
        this.customModel = "cannonrtp_premium";
    }
}
