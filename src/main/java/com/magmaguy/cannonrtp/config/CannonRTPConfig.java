package com.magmaguy.cannonrtp.config;

import com.magmaguy.magmacore.config.CustomConfig;
import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;

public class CannonRTPConfig extends CustomConfig {
    @Getter
    private static final Map<String, CannonRTPConfigFields> cannonRTPs = new LinkedHashMap<>();

    public CannonRTPConfig() {
        super("fun_rtps", "com.magmaguy.cannonrtp.config.cannonrtps.premade", CannonRTPConfigFields.class);
        cannonRTPs.clear();
        for (Map.Entry<String, ? extends com.magmaguy.magmacore.config.CustomConfigFields> entry : super.getCustomConfigFieldsHashMap().entrySet()) {
            cannonRTPs.put(entry.getKey(), (CannonRTPConfigFields) entry.getValue());
        }
    }
}


