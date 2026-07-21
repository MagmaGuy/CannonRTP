package com.magmaguy.cannonrtp;

import com.magmaguy.cannonrtp.config.CannonMessagesConfig;
import com.magmaguy.magmacore.MagmaCore;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CannonMessagesConfigTest {
    private ServerMock server;
    private MockCannonRTPPlugin plugin;
    private Path messagesFile;

    @BeforeEach
    void setUp() throws IOException {
        server = MockBukkit.mock();
        plugin = CannonRTPTestSupport.loadPlugin();
        MagmaCore.createInstance(plugin);
        messagesFile = plugin.getDataFolder().toPath().resolve("messages.yml");
        Files.deleteIfExists(messagesFile);
    }

    @AfterEach
    void tearDown() {
        CannonRTPTestSupport.setStaticField(CannonMessagesConfig.class, "statusReadyLabel", "Ready");
        CannonRTPTestSupport.setStaticField(CannonMessagesConfig.class, "statusChargingLabel", "Charging");
        CannonRTPTestSupport.setStaticField(CannonMessagesConfig.class, "statusMaintainingLabel", "Maintaining");
        CannonRTPTestSupport.setStaticField(CannonMessagesConfig.class, "statusExhaustedLabel", "Exhausted");
        CannonRTPTestSupport.setStaticField(CannonMessagesConfig.class, "statusDisabledLabel", "Disabled");
        CannonRTPTestSupport.setStaticField(CannonMessagesConfig.class, "statusInvalidLabel", "Invalid");
        MagmaCore.shutdown(plugin);
        CannonRTPTestSupport.setStaticField(MagmaCore.class, "instance", null);
        MockBukkit.unmock();
    }

    @Test
    void generatesAllStatusLabelDefaults() {
        new CannonMessagesConfig();

        YamlConfiguration saved = YamlConfiguration.loadConfiguration(messagesFile.toFile());

        assertAll(
                () -> assertEquals("Ready", CannonMessagesConfig.getStatusReadyLabel()),
                () -> assertEquals("Charging", CannonMessagesConfig.getStatusChargingLabel()),
                () -> assertEquals("Maintaining", CannonMessagesConfig.getStatusMaintainingLabel()),
                () -> assertEquals("Exhausted", CannonMessagesConfig.getStatusExhaustedLabel()),
                () -> assertEquals("Disabled", CannonMessagesConfig.getStatusDisabledLabel()),
                () -> assertEquals("Invalid", CannonMessagesConfig.getStatusInvalidLabel()),
                () -> assertEquals("Ready", saved.getString("statusLabels.ready")),
                () -> assertEquals("Charging", saved.getString("statusLabels.charging")),
                () -> assertEquals("Maintaining", saved.getString("statusLabels.maintaining")),
                () -> assertEquals("Exhausted", saved.getString("statusLabels.exhausted")),
                () -> assertEquals("Disabled", saved.getString("statusLabels.disabled")),
                () -> assertEquals("Invalid", saved.getString("statusLabels.invalid")));
    }

    @Test
    void preservesCustomLabelsAndPersistsMissingDefaults() throws IOException {
        Files.createDirectories(messagesFile.getParent());
        Files.writeString(messagesFile, """
                statusLabels:
                  ready: Listo
                  exhausted: Agotado
                  invalid: Configuración inválida
                """, StandardCharsets.UTF_8);

        new CannonMessagesConfig();

        YamlConfiguration saved = YamlConfiguration.loadConfiguration(messagesFile.toFile());

        assertAll(
                () -> assertEquals("Listo", CannonMessagesConfig.getStatusReadyLabel()),
                () -> assertEquals("Agotado", CannonMessagesConfig.getStatusExhaustedLabel()),
                () -> assertEquals("Configuración inválida", CannonMessagesConfig.getStatusInvalidLabel()),
                () -> assertEquals("Charging", CannonMessagesConfig.getStatusChargingLabel()),
                () -> assertEquals("Maintaining", CannonMessagesConfig.getStatusMaintainingLabel()),
                () -> assertEquals("Disabled", CannonMessagesConfig.getStatusDisabledLabel()),
                () -> assertEquals("Listo", saved.getString("statusLabels.ready")),
                () -> assertEquals("Agotado", saved.getString("statusLabels.exhausted")),
                () -> assertEquals("Configuración inválida", saved.getString("statusLabels.invalid")),
                () -> assertEquals("Charging", saved.getString("statusLabels.charging")),
                () -> assertEquals("Maintaining", saved.getString("statusLabels.maintaining")),
                () -> assertEquals("Disabled", saved.getString("statusLabels.disabled")),
                () -> assertTrue(Files.exists(messagesFile)));
    }
}
