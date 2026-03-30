package com.magmaguy.cannonrtp.config.contentpackages.premade;

import com.magmaguy.cannonrtp.config.contentpackages.ContentPackageConfigFields;

import java.util.List;

public class WorldCannonContent extends ContentPackageConfigFields {
    public WorldCannonContent() {
        super("world_cannon",
                true,
                "&aFree CannonRTP",
                List.of("&fInstalls the free Nightbreak CannonRTP cannon package.",
                        "&7Includes cannon configs and a free cannon model."),
                "https://nightbreak.io/plugin/world_cannon/",
                "world_cannon");
        setNightbreakSlug("world_cannon");
        setContentFilePrefixes(List.of("world_cannon"));
    }
}
