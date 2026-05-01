package com.magmaguy.cannonrtp.config;

import com.magmaguy.magmacore.config.ConfigurationEngine;
import com.magmaguy.magmacore.config.ConfigurationFile;
import com.magmaguy.magmacore.util.Logger;
import lombok.Getter;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class LandingSearchConfig extends ConfigurationFile {
    @Getter
    private static int preloadedLocationsPerCannon;
    @Getter
    private static int chargedLocationsPerCannon;
    @Getter
    private static int searchTimeoutAttempts;
    @Getter
    private static int slowFallingSeconds;
    @Getter
    private static boolean failOpenOnProtectionErrors;
    private static Set<Material> unsafeGroundMaterials = Collections.emptySet();
    private static Set<Material> unsafeBodyMaterials = Collections.emptySet();

    public LandingSearchConfig() {
        super("landing.yml");
    }

    public static boolean isUnsafeGroundMaterial(Material material) {
        return unsafeGroundMaterials.contains(material);
    }

    public static boolean isUnsafeBodyMaterial(Material material) {
        return unsafeBodyMaterials.contains(material);
    }

    @Override
    public void initializeValues() {
        preloadedLocationsPerCannon = Math.max(1, ConfigurationEngine.setInt(
                List.of("How many validated landing locations CannonRTP tries to keep ready per cannon."),
                fileConfiguration, "preloadedLocationsPerCannon", 10));
        chargedLocationsPerCannon = Math.max(1, Math.min(preloadedLocationsPerCannon, ConfigurationEngine.setInt(
                List.of("How many validated landing locations a cannon must have stored before it is considered charged and launch-ready.",
                        "Cannons allow launches as soon as at least one location is queued; this threshold only affects the visual READY label."),
                fileConfiguration, "chargedLocationsPerCannon", 1)));
        searchTimeoutAttempts = Math.max(10, ConfigurationEngine.setInt(
                List.of("How many failed search attempts CannonRTP should run before giving up and marking a cannon as EXHAUSTED.",
                        "Search attempts are globally rate-limited to one per tick (20 per second), shared fairly across all active cannons."),
                fileConfiguration, "searchTimeoutAttempts", 100));
        slowFallingSeconds = Math.max(1, ConfigurationEngine.setInt(
                List.of("Maximum slow-falling duration after the cannon airdrops a player above the destination.",
                        "The effect is removed early as soon as the player lands on the ground."),
                fileConfiguration, "slowFallingSeconds", 60));
        failOpenOnProtectionErrors = ConfigurationEngine.setBoolean(
                List.of("If true, protection-plugin query errors will allow landings instead of blocking them.",
                        "False is safer for public servers and is the default."),
                fileConfiguration, "failOpenOnProtectionErrors", false);

        unsafeGroundMaterials = parseMaterials(
                asStringList(ConfigurationEngine.setList(
                        List.of("Ground blocks that should never be considered safe to stand on."),
                        fileConfiguration, "unsafeGroundMaterials",
                        List.of("LAVA", "MAGMA_BLOCK", "CAMPFIRE", "SOUL_CAMPFIRE", "CACTUS", "POWDER_SNOW"))),
                "unsafeGroundMaterials");
        unsafeBodyMaterials = parseMaterials(
                asStringList(ConfigurationEngine.setList(
                        List.of("Blocks that should never occupy the feet or head space of a landing location."),
                        fileConfiguration, "unsafeBodyMaterials",
                        List.of("LAVA", "WATER", "FIRE", "SOUL_FIRE", "SWEET_BERRY_BUSH", "POWDER_SNOW", "COBWEB"))),
                "unsafeBodyMaterials");
    }

    private List<String> asStringList(List<?> rawList) {
        List<String> values = new ArrayList<>();
        for (Object object : rawList) {
            if (object != null) {
                values.add(String.valueOf(object));
            }
        }
        return values;
    }

    private Set<Material> parseMaterials(List<String> strings, String key) {
        Set<Material> materials = EnumSet.noneOf(Material.class);
        for (String entry : strings) {
            try {
                materials.add(Material.valueOf(entry.toUpperCase()));
            } catch (IllegalArgumentException exception) {
                Logger.warn("CannonRTP config entry " + entry + " is not a valid material for " + key + ".");
            }
        }
        return Collections.unmodifiableSet(materials);
    }
}
