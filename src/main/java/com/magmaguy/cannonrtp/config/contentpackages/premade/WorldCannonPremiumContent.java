package com.magmaguy.cannonrtp.config.contentpackages.premade;

import com.magmaguy.cannonrtp.config.contentpackages.ContentPackageConfigFields;

import java.util.List;

public class WorldCannonPremiumContent extends ContentPackageConfigFields {
    public WorldCannonPremiumContent() {
        super("world_cannon_premium",
                true,
                "&5Premium CannonRTP",
                List.of("&fInstalls the premium Nightbreak CannonRTP cannon package.",
                        "&7Includes premium cannon model content. Requires premium supporter access."),
                "https://nightbreak.io/plugin/world_cannon_premium/",
                "world_cannon_premium");
        setNightbreakSlug("world_cannon_premium");
        setContentFilePrefixes(List.of("world_cannon_premium"));
    }
}
