package com.magmaguy.cannonrtp.config;

import com.magmaguy.cannonrtp.util.DialogPool;
import com.magmaguy.magmacore.config.ConfigurationEngine;
import com.magmaguy.magmacore.config.ConfigurationFile;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

public class CannonMessagesConfig extends ConfigurationFile {
    @Getter
    private static String prefix;
    @Getter
    private static String helpHeader;
    @Getter
    private static String createdCannonMessage;
    @Getter
    private static String deletedCannonMessage;
    @Getter
    private static String placedCannonMessage;
    @Getter
    private static String removedCannonMessage;
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
    private static String statusLineMessage;
    @Getter
    private static String probeAllowedMessage;
    @Getter
    private static String probeBlockedMessage;
    @Getter
    private static String unknownCommandMessage;
    @Getter
    private static String destinationPreviewSubtitle;
    @Getter
    private static String destinationConfirmedSubtitle;

    private static List<String> launchQueuedTitles = Collections.emptyList();
    private static List<String> launchQueuedSubtitles = Collections.emptyList();
    private static List<String> destinationPreviewTitles = Collections.emptyList();
    private static List<String> destinationConfirmedTitles = Collections.emptyList();
    private static List<String> arrivalTitles = Collections.emptyList();
    private static List<String> arrivalSubtitles = Collections.emptyList();

    public CannonMessagesConfig() {
        super("messages.yml");
    }

    public static String pickLaunchQueuedTitle() {
        return DialogPool.pick(launchQueuedTitles, "<gradient:#ff8a3d:#ffd166>Launching</gradient>");
    }

    public static String pickLaunchQueuedSubtitle() {
        return DialogPool.pick(launchQueuedSubtitles, "<gradient:#fff4c2:#ffc36b>$cannon</gradient>");
    }

    public static String pickDestinationPreviewTitle() {
        return DialogPool.pick(destinationPreviewTitles, "<gradient:#ff9f43:#ffe08a>Calibrating</gradient>");
    }

    public static String pickDestinationConfirmedTitle() {
        return DialogPool.pick(destinationConfirmedTitles, "<gradient:#7dffb3:#d7ff95>Locked</gradient>");
    }

    public static String pickArrivalTitle() {
        return DialogPool.pick(arrivalTitles, "<gradient:#ffffff:#cfe8ff>Arrived</gradient>");
    }

    public static String pickArrivalSubtitle() {
        return DialogPool.pick(arrivalSubtitles, "<gradient:#ffffff:#cfe8ff>Good luck.</gradient>");
    }

    @Override
    public void initializeValues() {
        prefix = ConfigurationEngine.setString(
                List.of("Prefix used by CannonRTP messages.",
                        "Supports MagmaCore gradients, mini tags and regular color codes."),
                fileConfiguration, "prefix", "<gradient:#ff9a3d:#ffd166>CannonRTP</gradient> &8|");
        helpHeader = ConfigurationEngine.setRawString(
                List.of("Greeting shown at the top of /wc help."),
                fileConfiguration, "helpHeader", "$prefix &fCannonRTP commands:");
        createdCannonMessage = ConfigurationEngine.setRawString(
                List.of("Message sent after creating a cannon."),
                fileConfiguration, "createdCannon", "$prefix &aCreated cannon &f$cannon &7(id: &f$id&7)&a.");
        deletedCannonMessage = ConfigurationEngine.setRawString(
                List.of("Message sent after deleting a cannon configuration."),
                fileConfiguration, "deletedCannon", "$prefix &cDeleted cannon &f$cannon&c.");
        placedCannonMessage = ConfigurationEngine.setRawString(
                List.of("Message sent after placing a cannon instance at the player's location."),
                fileConfiguration, "placedCannon", "$prefix &aPlaced &f$cannon &aat your location.");
        removedCannonMessage = ConfigurationEngine.setRawString(
                List.of("Message sent after removing the nearest placed cannon instance."),
                fileConfiguration, "removedCannon", "$prefix &cRemoved cannon &f$cannon &cfrom the world.");
        targetWorldUpdatedMessage = ConfigurationEngine.setRawString(
                List.of("Message sent after changing a cannon target world."),
                fileConfiguration, "targetWorldUpdated", "$prefix &aCannon &f$cannon &awill now land players in &f$world&a.");
        searchCenterUpdatedMessage = ConfigurationEngine.setRawString(
                List.of("Message sent after changing a cannon search center."),
                fileConfiguration, "searchCenterUpdated", "$prefix &aUpdated the search center for &f$cannon&a.");
        reloadMessage = ConfigurationEngine.setRawString(
                List.of("Message sent after reloading CannonRTP."),
                fileConfiguration, "reload", "$prefix &aReloaded CannonRTP. Loaded &f$count &acannons.");
        noPermissionMessage = ConfigurationEngine.setRawString(
                List.of("Message sent when a player can see a cannon but cannot use it."),
                fileConfiguration, "noPermission", "$prefix &cYou do not have permission to use &f$cannon&c.");
        cannonDisabledMessage = ConfigurationEngine.setRawString(
                List.of("Message sent when a player steps into a disabled or invalid cannon."),
                fileConfiguration, "cannonDisabled", "$prefix &cCannon &f$cannon &cis disabled or not ready yet.");
        queueCalibrationMessage = ConfigurationEngine.setRawString(
                List.of("Message sent while a cannon is still preloading valid locations."),
                fileConfiguration, "queueCalibration", "$prefix &e$cannon is still calibrating safe landing locations. &7($queued/$target ready, $attempts attempts left)");
        noValidLocationYetMessage = ConfigurationEngine.setRawString(
                List.of("Message sent when a cannon has not found its first safe location yet."),
                fileConfiguration, "noValidLocationYet", "$prefix &eNo valid landing location exists yet for &f$cannon&e. It is still searching.");
        noValidLocationFoundMessage = ConfigurationEngine.setRawString(
                List.of("Message sent when CannonRTP gives up after timing out."),
                fileConfiguration, "noValidLocationFound", "$prefix &cNo valid landing location could be found for &f$cannon&c. Most likely reasons: &f$reason&c.");
        invalidConfigurationMessage = ConfigurationEngine.setRawString(
                List.of("Message sent when the target world or search center configuration is invalid."),
                fileConfiguration, "invalidConfiguration", "$prefix &cCannon &f$cannon &chas an invalid configuration: &f$reason&c.");
        statusLineMessage = ConfigurationEngine.setRawString(
                List.of("Line format used by /cannonrtp status."),
                fileConfiguration, "statusLine", "$prefix &f$cannon &8- &e$status &8- &f$queued/$target ready &8- &7$reason");
        probeAllowedMessage = ConfigurationEngine.setRawString(
                List.of("Message sent by /cannonrtp probe when the location is safe."),
                fileConfiguration, "probeAllowed", "$prefix &aThis location is currently valid for random landings.");
        probeBlockedMessage = ConfigurationEngine.setRawString(
                List.of("Message sent by /cannonrtp probe when the location is blocked."),
                fileConfiguration, "probeBlocked", "$prefix &cThis location is blocked by &f$plugin&c: &f$reason");
        unknownCommandMessage = ConfigurationEngine.setRawString(
                List.of("Message sent when an admin uses an invalid command."),
                fileConfiguration, "unknownCommand", "$prefix &cUnknown command. Use &f/cannonrtp &cfor help.");

        destinationPreviewSubtitle = ConfigurationEngine.setRawString(
                List.of("Subtitle shown while previewing the chosen destination."),
                fileConfiguration, "titles.destinationPreviewSubtitle", "<gradient:#fef3d0:#ffcb75>$x</gradient> &8| <gradient:#fef3d0:#ffcb75>$y</gradient> &8| <gradient:#fef3d0:#ffcb75>$z</gradient>");
        destinationConfirmedSubtitle = ConfigurationEngine.setRawString(
                List.of("Subtitle shown right before teleporting."),
                fileConfiguration, "titles.destinationConfirmedSubtitle", "<gradient:#d7fff1:#92f7c6>$x</gradient> &8| <gradient:#d7fff1:#92f7c6>$y</gradient> &8| <gradient:#d7fff1:#92f7c6>$z</gradient> &8in <gradient:#ffffff:#bfe7ff>$world</gradient>");

        launchQueuedTitles = Collections.unmodifiableList(asStringList(ConfigurationEngine.setList(
                List.of("Title pool shown when a launch starts. CannonRTP picks one at random each launch."),
                fileConfiguration, "titles.launchQueuedTitles", List.of(
                        "<gradient:#ff8a3d:#ffd166>Launching</gradient>",
                        "<gradient:#ff8a3d:#ffd166>Ignition</gradient>",
                        "<gradient:#ff8a3d:#ffd166>Liftoff</gradient>"))));
        launchQueuedSubtitles = Collections.unmodifiableList(asStringList(ConfigurationEngine.setList(
                List.of("Subtitle pool paired with the launch start title."),
                fileConfiguration, "titles.launchQueuedSubtitles", List.of(
                        "<gradient:#fff4c2:#ffc36b>$cannon</gradient>"))));
        destinationPreviewTitles = Collections.unmodifiableList(asStringList(ConfigurationEngine.setList(
                List.of("Title pool shown while the cannon is previewing destinations."),
                fileConfiguration, "titles.destinationPreviewTitles", List.of(
                        "<gradient:#ff9f43:#ffe08a>Calibrating</gradient>",
                        "<gradient:#ff9f43:#ffe08a>Scanning</gradient>",
                        "<gradient:#ff9f43:#ffe08a>Targeting</gradient>"))));
        destinationConfirmedTitles = Collections.unmodifiableList(asStringList(ConfigurationEngine.setList(
                List.of("Title pool shown right before teleport commits."),
                fileConfiguration, "titles.destinationConfirmedTitles", List.of(
                        "<gradient:#7dffb3:#d7ff95>Locked</gradient>",
                        "<gradient:#7dffb3:#d7ff95>Set</gradient>",
                        "<gradient:#7dffb3:#d7ff95>Engaged</gradient>"))));
        arrivalTitles = Collections.unmodifiableList(asStringList(ConfigurationEngine.setList(
                List.of("Title pool shown after the player is dropped over the destination."),
                fileConfiguration, "titles.arrivalTitles", List.of(
                        "<gradient:#ffffff:#cfe8ff>Arrived</gradient>",
                        "<gradient:#ffffff:#cfe8ff>Touchdown</gradient>",
                        "<gradient:#ffffff:#cfe8ff>Airborne</gradient>"))));
        arrivalSubtitles = Collections.unmodifiableList(asStringList(ConfigurationEngine.setList(
                List.of("Subtitle pool shown after the player is dropped over the destination.",
                        "CannonRTP picks one line at random each launch."),
                fileConfiguration, "titles.arrivalSubtitles", List.of(
                        "<gradient:#ffffff:#cfe8ff>Good luck.</gradient>",
                        "<gradient:#ffffff:#d7ffd1>Stick the landing.</gradient>",
                        "<gradient:#ffe8c4:#ffd27a>Eyes up. Ground soon.</gradient>",
                        "<gradient:#e6f8ff:#9ee0ff>Wind check complete. Good luck.</gradient>",
                        "<gradient:#fff0d7:#ffc97d>Drop zone acquired. Good luck.</gradient>"))));
    }

    private static java.util.List<String> asStringList(List<?> rawList) {
        java.util.List<String> values = new java.util.ArrayList<>();
        for (Object object : rawList) {
            if (object != null) {
                values.add(String.valueOf(object));
            }
        }
        return values;
    }
}
