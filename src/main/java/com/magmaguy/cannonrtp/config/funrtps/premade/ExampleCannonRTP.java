package com.magmaguy.cannonrtp.config.cannonrtps.premade;

import com.magmaguy.cannonrtp.config.CannonRTPConfigFields;
import org.bukkit.Bukkit;
import org.bukkit.Location;

public class ExampleCannonRTP extends CannonRTPConfigFields {
    public ExampleCannonRTP() {
        super("example_world_cannon",
                false,
                "Example World Cannon",
                defaultLocation(),
                Bukkit.getWorlds().isEmpty() ? "world" : Bukkit.getWorlds().get(0).getName(),
                defaultLocation());
    }

    private static Location defaultLocation() {
        if (Bukkit.getWorlds().isEmpty()) {
            return new Location(null, 0, 64, 0);
        }
        return Bukkit.getWorlds().get(0).getSpawnLocation();
    }
}

