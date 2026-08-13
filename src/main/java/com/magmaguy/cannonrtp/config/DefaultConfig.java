package com.magmaguy.cannonrtp.config;

import com.magmaguy.magmacore.config.ConfigurationEngine;
import com.magmaguy.magmacore.config.ConfigurationFile;
import com.magmaguy.magmacore.nightbreak.NightbreakPluginUpdater;
import lombok.Getter;

import java.util.List;

public class DefaultConfig extends ConfigurationFile {
    private static DefaultConfig instance;
    private static boolean setupDone;
    @Getter
    private static String language;
    @Getter
    private static int particleIntervalTicks;
    @Getter
    private static int launchCooldownSeconds;
    @Getter
    private static List<String> cannonModelPriority;
    @Getter
    private static String spigotResourceId;
    @Getter
    private static boolean autoDownloadPluginUpdates;

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
        autoDownloadPluginUpdates = NightbreakPluginUpdater.setAutoDownloadConfigDefault(fileConfiguration);
        language = ConfigurationEngine.setString(
                List.of("Translation file to load. 'english' uses plugin defaults.",
                        "Release 1 ships English-only; this key exists so future translation packs can target it."),
                fileConfiguration, "language", "english");
        particleIntervalTicks = Math.max(1, ConfigurationEngine.setInt(
                List.of("Exact interval, in server ticks, between idle particle renders for enabled cannons.",
                        "Set to 1 to render every tick."),
                fileConfiguration, "runtime.particleIntervalTicks", 15));
        launchCooldownSeconds = Math.max(0, ConfigurationEngine.setInt(
                List.of("How many seconds a player must wait between accepted cannon launches.",
                        "The cooldown starts only after a launch event is accepted. Set to 0 to disable."),
                fileConfiguration, "runtime.launchCooldownSeconds", 30));
        cannonModelPriority = ConfigLists.asStringList(ConfigurationEngine.setList(
                List.of("Priority list of cannon model names, checked top to bottom.",
                        "The first model found on the server will be used.",
                        "If none are found, particles are used instead."),
                fileConfiguration, "runtime.cannonModelPriority",
                List.of("cannonrtp_premium", "cannonrtp")));
        spigotResourceId = ConfigurationEngine.setString(
                List.of("Spigot resource ID used by the version checker. Leave blank to disable."),
                fileConfiguration, "runtime.spigotResourceId", "");
    }
}
