package com.magmaguy.cannonrtp.config.contentpackages.premade;

import com.magmaguy.cannonrtp.config.contentpackages.ContentPackageConfigFields;

import java.util.List;

public class WorldCannonPremiumContent extends ContentPackageConfigFields {
    public WorldCannonPremiumContent() {
        super("cannonrtp_premium",
                true,
                "&5Premium CannonRTP",
                List.of("&fInstalls the premium Nightbreak CannonRTP cannon package.",
                        "&7Includes premium cannon model content. Requires premium supporter access."),
                "https://nightbreak.io/plugin/cannonrtp_premium/",
                "cannonrtp_premium");
        setNightbreakSlug("cannonrtp_premium");
        setContentFilePrefixes(List.of("cannonrtp_premium"));
    }
}
