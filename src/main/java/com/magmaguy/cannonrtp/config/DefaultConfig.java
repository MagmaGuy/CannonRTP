package com.magmaguy.cannonrtp.config;

import com.magmaguy.magmacore.config.ConfigurationEngine;
import com.magmaguy.magmacore.config.ConfigurationFile;
import com.magmaguy.magmacore.nightbreak.NightbreakPluginUpdater;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class DefaultConfig extends ConfigurationFile {
    private static DefaultConfig instance;
    private static boolean setupDone;
    @Getter
    private static String language;
    @Getter
    private static int particleIntervalTicks;
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
        particleIntervalTicks = Math.max(5, ConfigurationEngine.setInt(
                List.of("How frequently CannonRTP renders idle particles for enabled cannons."),
                fileConfiguration, "runtime.particleIntervalTicks", 15));
        cannonModelPriority = asStringList(ConfigurationEngine.setList(
                List.of("Priority list of cannon model names, checked top to bottom.",
                        "The first model found on the server will be used.",
                        "If none are found, particles are used instead."),
                fileConfiguration, "runtime.cannonModelPriority",
                List.of("cannonrtp_premium", "cannonrtp")));
        spigotResourceId = ConfigurationEngine.setString(
                List.of("Spigot resource ID used by the version checker. Leave blank to disable."),
                fileConfiguration, "runtime.spigotResourceId", "");
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
}
