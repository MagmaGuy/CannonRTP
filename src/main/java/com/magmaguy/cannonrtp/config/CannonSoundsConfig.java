package com.magmaguy.cannonrtp.config;

import com.magmaguy.magmacore.config.ConfigurationEngine;
import com.magmaguy.magmacore.config.ConfigurationFile;
import com.magmaguy.magmacore.util.Logger;
import lombok.Getter;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;

import java.util.List;
import java.util.Locale;

public class CannonSoundsConfig extends ConfigurationFile {
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

    public CannonSoundsConfig() {
        super("sounds.yml");
    }

    @Override
    public void initializeValues() {
        levitationStartSound = parseSound(ConfigurationEngine.setString(
                List.of("Sound played when the launch warmup and levitation effect begin."),
                fileConfiguration, "levitationStart.sound", "BLOCK_BEACON_ACTIVATE"), "levitationStart.sound");
        levitationStartSoundVolume = Math.max(0f, (float) ConfigurationEngine.setDouble(
                List.of("Volume for the levitation start sound."),
                fileConfiguration, "levitationStart.volume", 1.0));
        levitationStartSoundPitch = Math.max(0f, (float) ConfigurationEngine.setDouble(
                List.of("Pitch for the levitation start sound."),
                fileConfiguration, "levitationStart.pitch", 1.15));
        blastOffSound = parseSound(ConfigurationEngine.setString(
                List.of("Sound played when the teleport launch commits."),
                fileConfiguration, "blastOff.sound", "ENTITY_GENERIC_EXPLODE"), "blastOff.sound");
        blastOffSoundVolume = Math.max(0f, (float) ConfigurationEngine.setDouble(
                List.of("Volume for the blast off sound."),
                fileConfiguration, "blastOff.volume", 1.0));
        blastOffSoundPitch = Math.max(0f, (float) ConfigurationEngine.setDouble(
                List.of("Pitch for the blast off sound."),
                fileConfiguration, "blastOff.pitch", 0.9));
    }

    private Sound parseSound(String rawSound, String key) {
        if (rawSound == null || rawSound.isBlank()) {
            return null;
        }

        String normalizedSound = rawSound.trim().toLowerCase(Locale.ROOT);

        Sound sound = getSound(NamespacedKey.fromString(normalizedSound));
        if (sound == null && !normalizedSound.contains(":")) {
            sound = getSound(minecraftKey(normalizedSound.replace('_', '.')));
        }
        if (sound != null) {
            return sound;
        }

        Logger.warn("CannonRTP config entry " + rawSound + " is not a valid sound for " + key + ".");
        return null;
    }

    private Sound getSound(NamespacedKey namespacedKey) {
        return namespacedKey == null ? null : Registry.SOUNDS.get(namespacedKey);
    }

    private NamespacedKey minecraftKey(String key) {
        try {
            return NamespacedKey.minecraft(key);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
