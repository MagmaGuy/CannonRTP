package com.magmaguy.cannonrtp.config.contentpackages.premade;

import com.magmaguy.cannonrtp.config.contentpackages.ContentPackageConfigFields;

import java.util.List;

public class WorldCannonContent extends ContentPackageConfigFields {
    public WorldCannonContent() {
        super("cannonrtp",
                true,
                "&aFree CannonRTP",
                List.of("&fInstalls the free Nightbreak CannonRTP cannon package.",
                        "&7Includes cannon configs and a free cannon model."),
                "https://nightbreak.io/plugin/cannonrtp/",
                "cannonrtp");
        setNightbreakSlug("cannonrtp");
        setContentFilePrefixes(List.of("cannonrtp"));
    }
}
