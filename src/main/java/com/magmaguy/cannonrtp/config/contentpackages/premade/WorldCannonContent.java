package com.magmaguy.cannonrtp.config.contentpackages.premade;

import com.magmaguy.cannonrtp.config.contentpackages.ContentPackageConfigFields;

import java.util.List;

public class WorldCannonContent extends ContentPackageConfigFields {
    public WorldCannonContent() {
        super("world_cannon",
                true,
                "&6CannonRTP",
                List.of("&fInstalls the Nightbreak CannonRTP cannon package.",
                        "&7Includes cannon configs and supports optional FMM model content."),
                "https://nightbreak.io/plugin/world_cannon/",
                "world_cannon");
        setNightbreakSlug("world_cannon");
        setContentFilePrefixes(List.of("world_cannon"));
    }
}
