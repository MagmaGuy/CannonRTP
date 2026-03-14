package com.magmaguy.cannonrtp.config;

import com.magmaguy.magmacore.config.ConfigurationEngine;
import com.magmaguy.magmacore.config.ConfigurationFile;
import com.magmaguy.magmacore.util.Logger;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class DefaultConfig extends ConfigurationFile {
    private static DefaultConfig instance;
    private static boolean setupDone;
    @Getter
    private static int scanIntervalTicks;
    @Getter
    private static int particleIntervalTicks;
    @Getter
    private static int searchAttemptsPerTick;
    @Getter
    private static int preloadedLocationsPerCannon;
    @Getter
    private static int chargedLocationsPerCannon;
    @Getter
    private static int searchTimeoutSeconds;
    @Getter
    private static int slowFallingSeconds;
    @Getter
    private static boolean failOpenOnProtectionErrors;
    @Getter
    private static Sound levitationStartSound;
    @Getter
    private static float levitationStartSoundVolume;
    @Getter
    private static float levitationStartSoundPitch;
    @Getter
    private static Sound blastOffSound;
    @Getter
    private static float blastOffSoundVolume;
    @Getter
    private static float blastOffSoundPitch;
    @Getter
    private static String prefix;
    @Getter
    private static String helpHeader;
    @Getter
    private static String createdCannonMessage;
    @Getter
    private static String deletedCannonMessage;
    @Getter
    private static String movedCannonMessage;
    @Getter
    private static String targetWorldUpdatedMessage;
    @Getter
    private static String searchCenterUpdatedMessage;
    @Getter
    private static String reloadMessage;
    @Getter
    private static String noPermissionMessage;
    @Getter
    private static String cannonDisabledMessage;
    @Getter
    private static String queueCalibrationMessage;
    @Getter
    private static String noValidLocationYetMessage;
    @Getter
    private static String noValidLocationFoundMessage;
    @Getter
    private static String invalidConfigurationMessage;
    @Getter
    private static String launchQueuedTitle;
    @Getter
    private static String launchQueuedSubtitle;
    @Getter
    private static String destinationPreviewTitle;
    @Getter
    private static String destinationPreviewSubtitle;
    @Getter
    private static String destinationConfirmedTitle;
    @Getter
    private static String destinationConfirmedSubtitle;
    @Getter
    private static List<String> arrivalSubtitles = Collections.emptyList();
    @Getter
    private static String statusLineMessage;
    @Getter
    private static String probeAllowedMessage;
    @Getter
    private static String probeBlockedMessage;
    @Getter
    private static String unknownCommandMessage;
    @Getter
    private static boolean worldGuardEnabled;
    @Getter
    private static boolean worldGuardAllowGlobalRegionOnly;
    @Getter
    private static boolean worldGuardAllowBuildAllowedRegions;
    @Getter
    private static boolean worldGuardAllowPassthroughRegions;
    @Getter
    private static boolean townyEnabled;
    @Getter
    private static boolean townyAllowWilderness;
    @Getter
    private static boolean townyAllowNationZones;
    @Getter
    private static boolean townyAllowClaimedTownBlocks;
    @Getter
    private static boolean landsEnabled;
    @Getter
    private static boolean landsAllowUnclaimedAreas;
    @Getter
    private static boolean landsAllowClaimedAreas;
    @Getter
    private static boolean griefPreventionEnabled;
    @Getter
    private static boolean griefPreventionAllowWilderness;
    @Getter
    private static boolean griefPreventionAllowAdminClaims;
    @Getter
    private static boolean griefPreventionAllowPlayerClaims;
    @Getter
    private static boolean huskTownsEnabled;
    @Getter
    private static boolean huskTownsAllowWilderness;
    @Getter
    private static boolean huskTownsAllowAdminClaims;
    @Getter
    private static boolean huskTownsAllowRegularClaims;
    @Getter
    private static boolean huskTownsAllowFarmClaims;
    @Getter
    private static boolean huskTownsAllowPlotClaims;
    @Getter
    private static boolean huskClaimsEnabled;
    @Getter
    private static boolean huskClaimsAllowWilderness;
    @Getter
    private static boolean huskClaimsAllowAdminClaims;
    @Getter
    private static boolean huskClaimsAllowPlayerClaims;
    private static Set<Material> unsafeGroundMaterials = Collections.emptySet();
    private static Set<Material> unsafeBodyMaterials = Collections.emptySet();

    public DefaultConfig() {
        super("config.yml");
        instance = this;
    }

    public static boolean isSetupDone() {
        return setupDone;
    }

    public static void toggleSetupDone(boolean value) {
        setupDone = value;
        ConfigurationEngine.writeValue(setupDone, instance.file, instance.getFileConfiguration(), "setupDone");
    }

    @Override
    public void initializeValues() {
        setupDone = ConfigurationEngine.setBoolean(
                List.of("Tracks whether the first-time setup guidance has been completed."),
                fileConfiguration, "setupDone", false);
        scanIntervalTicks = Math.max(2, ConfigurationEngine.setInt(
                List.of("How frequently CannonRTP checks whether players have stepped into a cannon.",
                        "Cannon scanning currently runs every other tick."),
                fileConfiguration, "runtime.scanIntervalTicks", 2));
        particleIntervalTicks = Math.max(5, ConfigurationEngine.setInt(
                List.of("How frequently CannonRTP renders idle particles for enabled cannons."),
                fileConfiguration, "runtime.particleIntervalTicks", 15));
        searchAttemptsPerTick = Math.max(1, ConfigurationEngine.setInt(
                List.of("How many landing-location attempts each cannon gets per tick while preloading."),
                fileConfiguration, "landing.searchAttemptsPerTick", 2));
        preloadedLocationsPerCannon = Math.max(1, ConfigurationEngine.setInt(
                List.of("How many validated landing locations CannonRTP tries to keep ready per cannon."),
                fileConfiguration, "landing.preloadedLocationsPerCannon", 10));
        chargedLocationsPerCannon = Math.max(1, Math.min(preloadedLocationsPerCannon, ConfigurationEngine.setInt(
                List.of("How many validated landing locations a cannon must have stored before it is considered charged and launch-ready."),
                fileConfiguration, "landing.chargedLocationsPerCannon", 5)));
        searchTimeoutSeconds = Math.max(10, ConfigurationEngine.setInt(
                List.of("How long CannonRTP should keep searching for a valid location before giving up."),
                fileConfiguration, "landing.searchTimeoutSeconds", 1000));
        slowFallingSeconds = Math.max(1, ConfigurationEngine.setInt(
                List.of("Maximum slow-falling duration after the cannon airdrops a player above the destination.",
                        "The effect is removed early as soon as the player lands on the ground."),
                fileConfiguration, "landing.slowFallingSeconds", 60));
        failOpenOnProtectionErrors = ConfigurationEngine.setBoolean(
                List.of("If true, protection-plugin query errors will allow landings instead of blocking them.",
                        "False is safer for public servers and is the default."),
                fileConfiguration, "landing.failOpenOnProtectionErrors", false);
        levitationStartSound = parseSound(ConfigurationEngine.setString(
                List.of("Sound played when the launch warmup and levitation effect begin."),
                fileConfiguration, "landing.sounds.levitationStart.sound", "BLOCK_BEACON_ACTIVATE"), "landing.sounds.levitationStart.sound");
        levitationStartSoundVolume = Math.max(0f, (float) ConfigurationEngine.setDouble(
                List.of("Volume for the levitation start sound."),
                fileConfiguration, "landing.sounds.levitationStart.volume", 1.0));
        levitationStartSoundPitch = Math.max(0f, (float) ConfigurationEngine.setDouble(
                List.of("Pitch for the levitation start sound."),
                fileConfiguration, "landing.sounds.levitationStart.pitch", 1.15));
        blastOffSound = parseSound(ConfigurationEngine.setString(
                List.of("Sound played when the teleport launch commits."),
                fileConfiguration, "landing.sounds.blastOff.sound", "ENTITY_GENERIC_EXPLODE"), "landing.sounds.blastOff.sound");
        blastOffSoundVolume = Math.max(0f, (float) ConfigurationEngine.setDouble(
                List.of("Volume for the blast off sound."),
                fileConfiguration, "landing.sounds.blastOff.volume", 1.0));
        blastOffSoundPitch = Math.max(0f, (float) ConfigurationEngine.setDouble(
                List.of("Pitch for the blast off sound."),
                fileConfiguration, "landing.sounds.blastOff.pitch", 0.9));

        unsafeGroundMaterials = parseMaterials(
                asStringList(ConfigurationEngine.setList(
                        List.of("Ground blocks that should never be considered safe to stand on."),
                        fileConfiguration, "landing.unsafeGroundMaterials",
                        List.of("LAVA", "MAGMA_BLOCK", "CAMPFIRE", "SOUL_CAMPFIRE", "CACTUS", "POWDER_SNOW"))),
                "landing.unsafeGroundMaterials");
        unsafeBodyMaterials = parseMaterials(
                asStringList(ConfigurationEngine.setList(
                        List.of("Blocks that should never occupy the feet or head space of a landing location."),
                        fileConfiguration, "landing.unsafeBodyMaterials",
                        List.of("LAVA", "WATER", "FIRE", "SOUL_FIRE", "SWEET_BERRY_BUSH", "POWDER_SNOW", "COBWEB"))),
                "landing.unsafeBodyMaterials");

        prefix = ConfigurationEngine.setString(
                List.of("Prefix used by CannonRTP messages.",
                        "Supports MagmaCore gradients, mini tags and regular color codes."),
                fileConfiguration, "messages.prefix", "<gradient:#ff9a3d:#ffd166>CannonRTP</gradient> &8|");
        helpHeader = ConfigurationEngine.setString(
                List.of("Header shown by /cannonrtp."),
                fileConfiguration, "messages.helpHeader", "$prefix &fCommands: &e/cannonrtp create <id> [display_name]&f, &e/cannonrtp move <id>&f, &e/cannonrtp delete <id>&f, &e/cannonrtp target <id> <world>&f, &e/cannonrtp center <id>&f, &e/cannonrtp list&f, &e/cannonrtp status&f, &e/cannonrtp probe&f, &e/cannonrtp setup&f, &e/cannonrtp downloadall&f, &e/cannonrtp updatecontent&f, &e/cannonrtp initialize&f, &e/cannonrtp reload");
        createdCannonMessage = ConfigurationEngine.setString(
                List.of("Message sent after creating a cannon."),
                fileConfiguration, "messages.createdCannon", "$prefix &aCreated cannon &f$cannon &7(id: &f$id&7)&a.");
        deletedCannonMessage = ConfigurationEngine.setString(
                List.of("Message sent after deleting a cannon."),
                fileConfiguration, "messages.deletedCannon", "$prefix &cDeleted cannon &f$cannon&c.");
        movedCannonMessage = ConfigurationEngine.setString(
                List.of("Message sent after moving a cannon."),
                fileConfiguration, "messages.movedCannon", "$prefix &aMoved cannon &f$cannon&a to your current location.");
        targetWorldUpdatedMessage = ConfigurationEngine.setString(
                List.of("Message sent after changing a cannon target world."),
                fileConfiguration, "messages.targetWorldUpdated", "$prefix &aCannon &f$cannon &awill now land players in &f$world&a.");
        searchCenterUpdatedMessage = ConfigurationEngine.setString(
                List.of("Message sent after changing a cannon search center."),
                fileConfiguration, "messages.searchCenterUpdated", "$prefix &aUpdated the search center for &f$cannon&a.");
        reloadMessage = ConfigurationEngine.setString(
                List.of("Message sent after reloading CannonRTP."),
                fileConfiguration, "messages.reload", "$prefix &aReloaded CannonRTP. Loaded &f$count &acannons.");
        noPermissionMessage = ConfigurationEngine.setString(
                List.of("Message sent when a player can see a cannon but cannot use it."),
                fileConfiguration, "messages.noPermission", "$prefix &cYou do not have permission to use &f$cannon&c.");
        cannonDisabledMessage = ConfigurationEngine.setString(
                List.of("Message sent when a player steps into a disabled or invalid cannon."),
                fileConfiguration, "messages.cannonDisabled", "$prefix &cCannon &f$cannon &cis disabled or not ready yet.");
        queueCalibrationMessage = ConfigurationEngine.setString(
                List.of("Message sent while a cannon is still preloading valid locations."),
                fileConfiguration, "messages.queueCalibration", "$prefix &e$cannon is still calibrating safe landing locations. &7($queued/$target ready, $seconds s left)");
        noValidLocationYetMessage = ConfigurationEngine.setString(
                List.of("Message sent when a cannon has not found its first safe location yet."),
                fileConfiguration, "messages.noValidLocationYet", "$prefix &eNo valid landing location exists yet for &f$cannon&e. It is still searching.");
        noValidLocationFoundMessage = ConfigurationEngine.setString(
                List.of("Message sent when CannonRTP gives up after timing out."),
                fileConfiguration, "messages.noValidLocationFound", "$prefix &cNo valid landing location could be found for &f$cannon&c. Most likely reasons: &f$reason&c.");
        invalidConfigurationMessage = ConfigurationEngine.setString(
                List.of("Message sent when the target world or search center configuration is invalid."),
                fileConfiguration, "messages.invalidConfiguration", "$prefix &cCannon &f$cannon &chas an invalid configuration: &f$reason&c.");
        launchQueuedTitle = ConfigurationEngine.setString(
                List.of("Title shown when a launch starts."),
                fileConfiguration, "messages.titles.launchQueuedTitle", "<gradient:#ff8a3d:#ffd166>Launch sequence engaged</gradient>");
        launchQueuedSubtitle = ConfigurationEngine.setString(
                List.of("Subtitle shown when a launch starts."),
                fileConfiguration, "messages.titles.launchQueuedSubtitle", "<gradient:#fff4c2:#ffc36b>Spooling vectors for $cannon</gradient>");
        destinationPreviewTitle = ConfigurationEngine.setString(
                List.of("Title shown while previewing the chosen destination."),
                fileConfiguration, "messages.titles.destinationPreviewTitle", "<gradient:#ff9f43:#ffe08a>Calibrating destination</gradient>");
        destinationPreviewSubtitle = ConfigurationEngine.setString(
                List.of("Subtitle shown while previewing the chosen destination."),
                fileConfiguration, "messages.titles.destinationPreviewSubtitle", "<gradient:#fef3d0:#ffcb75>$x</gradient> &8| <gradient:#fef3d0:#ffcb75>$y</gradient> &8| <gradient:#fef3d0:#ffcb75>$z</gradient>");
        destinationConfirmedTitle = ConfigurationEngine.setString(
                List.of("Title shown right before teleporting."),
                fileConfiguration, "messages.titles.destinationConfirmedTitle", "<gradient:#7dffb3:#d7ff95>Launch confirmed</gradient>");
        destinationConfirmedSubtitle = ConfigurationEngine.setString(
                List.of("Subtitle shown right before teleporting."),
                fileConfiguration, "messages.titles.destinationConfirmedSubtitle", "<gradient:#d7fff1:#92f7c6>$x</gradient> &8| <gradient:#d7fff1:#92f7c6>$y</gradient> &8| <gradient:#d7fff1:#92f7c6>$z</gradient> &8in <gradient:#ffffff:#bfe7ff>$world</gradient>");
        arrivalSubtitles = Collections.unmodifiableList(asStringList(ConfigurationEngine.setList(
                List.of("Subtitle pool shown after the player is dropped over the destination.",
                        "CannonRTP picks one line at random each launch."),
                fileConfiguration, "messages.titles.arrivalSubtitles", List.of(
                        "<gradient:#ffffff:#cfe8ff>Good luck.</gradient>",
                        "<gradient:#ffffff:#d7ffd1>Stick the landing.</gradient>",
                        "<gradient:#ffe8c4:#ffd27a>Eyes up. Ground soon.</gradient>",
                        "<gradient:#e6f8ff:#9ee0ff>Wind check complete. Good luck.</gradient>",
                        "<gradient:#fff0d7:#ffc97d>Drop zone acquired. Good luck.</gradient>"))));
        statusLineMessage = ConfigurationEngine.setString(
                List.of("Line format used by /cannonrtp status."),
                fileConfiguration, "messages.statusLine", "$prefix &f$cannon &8- &e$status &8- &f$queued/$target ready &8- &7$reason");
        probeAllowedMessage = ConfigurationEngine.setString(
                List.of("Message sent by /cannonrtp probe when the location is safe."),
                fileConfiguration, "messages.probeAllowed", "$prefix &aThis location is currently valid for random landings.");
        probeBlockedMessage = ConfigurationEngine.setString(
                List.of("Message sent by /cannonrtp probe when the location is blocked."),
                fileConfiguration, "messages.probeBlocked", "$prefix &cThis location is blocked by &f$plugin&c: &f$reason");
        unknownCommandMessage = ConfigurationEngine.setString(
                List.of("Message sent when an admin uses an invalid command."),
                fileConfiguration, "messages.unknownCommand", "$prefix &cUnknown command. Use &f/cannonrtp &cfor help.");

        worldGuardEnabled = ConfigurationEngine.setBoolean(
                List.of("Enables WorldGuard landing checks."),
                fileConfiguration, "protection.worldGuard.enabled", true);
        worldGuardAllowGlobalRegionOnly = ConfigurationEngine.setBoolean(
                List.of("If true, a location that is only inside WorldGuard's __global__ region is considered safe."),
                fileConfiguration, "protection.worldGuard.allowGlobalRegionOnly", true);
        worldGuardAllowBuildAllowedRegions = ConfigurationEngine.setBoolean(
                List.of("If true, regions with an explicit build=ALLOW flag are treated as public landings."),
                fileConfiguration, "protection.worldGuard.allowBuildAllowedRegions", true);
        worldGuardAllowPassthroughRegions = ConfigurationEngine.setBoolean(
                List.of("If true, regions with passthrough=ALLOW are treated as public landings."),
                fileConfiguration, "protection.worldGuard.allowPassthroughRegions", true);

        townyEnabled = ConfigurationEngine.setBoolean(
                List.of("Enables Towny landing checks."),
                fileConfiguration, "protection.towny.enabled", true);
        townyAllowWilderness = ConfigurationEngine.setBoolean(
                List.of("If true, Towny wilderness is considered safe."),
                fileConfiguration, "protection.towny.allowWilderness", true);
        townyAllowNationZones = ConfigurationEngine.setBoolean(
                List.of("If true, Towny nation zones are considered safe."),
                fileConfiguration, "protection.towny.allowNationZones", false);
        townyAllowClaimedTownBlocks = ConfigurationEngine.setBoolean(
                List.of("If true, CannonRTP may land players inside claimed Towny town blocks."),
                fileConfiguration, "protection.towny.allowClaimedTownBlocks", false);

        landsEnabled = ConfigurationEngine.setBoolean(
                List.of("Enables Lands landing checks."),
                fileConfiguration, "protection.lands.enabled", true);
        landsAllowUnclaimedAreas = ConfigurationEngine.setBoolean(
                List.of("If true, unclaimed Lands areas are considered safe."),
                fileConfiguration, "protection.lands.allowUnclaimedAreas", true);
        landsAllowClaimedAreas = ConfigurationEngine.setBoolean(
                List.of("If true, CannonRTP may land players inside claimed Lands areas."),
                fileConfiguration, "protection.lands.allowClaimedAreas", false);

        griefPreventionEnabled = ConfigurationEngine.setBoolean(
                List.of("Enables GriefPrevention landing checks."),
                fileConfiguration, "protection.griefPrevention.enabled", true);
        griefPreventionAllowWilderness = ConfigurationEngine.setBoolean(
                List.of("If true, GriefPrevention wilderness is considered safe."),
                fileConfiguration, "protection.griefPrevention.allowWilderness", true);
        griefPreventionAllowAdminClaims = ConfigurationEngine.setBoolean(
                List.of("If true, CannonRTP may land players inside GriefPrevention admin claims."),
                fileConfiguration, "protection.griefPrevention.allowAdminClaims", false);
        griefPreventionAllowPlayerClaims = ConfigurationEngine.setBoolean(
                List.of("If true, CannonRTP may land players inside regular GriefPrevention claims."),
                fileConfiguration, "protection.griefPrevention.allowPlayerClaims", false);

        huskTownsEnabled = ConfigurationEngine.setBoolean(
                List.of("Enables HuskTowns landing checks."),
                fileConfiguration, "protection.huskTowns.enabled", true);
        huskTownsAllowWilderness = ConfigurationEngine.setBoolean(
                List.of("If true, HuskTowns wilderness is considered safe."),
                fileConfiguration, "protection.huskTowns.allowWilderness", true);
        huskTownsAllowAdminClaims = ConfigurationEngine.setBoolean(
                List.of("If true, CannonRTP may land players inside HuskTowns admin claims."),
                fileConfiguration, "protection.huskTowns.allowAdminClaims", false);
        huskTownsAllowRegularClaims = ConfigurationEngine.setBoolean(
                List.of("If true, CannonRTP may land players inside standard HuskTowns claims."),
                fileConfiguration, "protection.huskTowns.allowRegularClaims", false);
        huskTownsAllowFarmClaims = ConfigurationEngine.setBoolean(
                List.of("If true, CannonRTP may land players inside HuskTowns farm claims."),
                fileConfiguration, "protection.huskTowns.allowFarmClaims", false);
        huskTownsAllowPlotClaims = ConfigurationEngine.setBoolean(
                List.of("If true, CannonRTP may land players inside HuskTowns plot claims."),
                fileConfiguration, "protection.huskTowns.allowPlotClaims", false);

        huskClaimsEnabled = ConfigurationEngine.setBoolean(
                List.of("Enables HuskClaims landing checks."),
                fileConfiguration, "protection.huskClaims.enabled", true);
        huskClaimsAllowWilderness = ConfigurationEngine.setBoolean(
                List.of("If true, HuskClaims wilderness is considered safe."),
                fileConfiguration, "protection.huskClaims.allowWilderness", true);
        huskClaimsAllowAdminClaims = ConfigurationEngine.setBoolean(
                List.of("If true, CannonRTP may land players inside HuskClaims admin claims."),
                fileConfiguration, "protection.huskClaims.allowAdminClaims", false);
        huskClaimsAllowPlayerClaims = ConfigurationEngine.setBoolean(
                List.of("If true, CannonRTP may land players inside regular HuskClaims claims."),
                fileConfiguration, "protection.huskClaims.allowPlayerClaims", false);
    }

    public static boolean isUnsafeGroundMaterial(Material material) {
        return unsafeGroundMaterials.contains(material);
    }

    public static boolean isUnsafeBodyMaterial(Material material) {
        return unsafeBodyMaterials.contains(material);
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

    private Sound parseSound(String rawSound, String key) {
        if (rawSound == null || rawSound.isBlank()) {
            return null;
        }

        String normalizedSound = rawSound.trim().toLowerCase(Locale.ROOT);

        Sound sound = Registry.SOUNDS.get(NamespacedKey.fromString(normalizedSound));
        if (sound == null && !normalizedSound.contains(":")) {
            sound = Registry.SOUNDS.get(NamespacedKey.minecraft(normalizedSound.replace('_', '.')));
        }
        if (sound != null) {
            return sound;
        }

        Logger.warn("CannonRTP config entry " + rawSound + " is not a valid sound for " + key + ".");
        return null;
    }
}

