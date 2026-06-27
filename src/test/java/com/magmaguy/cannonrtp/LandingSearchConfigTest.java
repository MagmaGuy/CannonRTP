package com.magmaguy.cannonrtp;

import com.magmaguy.cannonrtp.config.LandingSearchConfig;
import com.magmaguy.magmacore.MagmaCore;
import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LandingSearchConfigTest {
    private ServerMock server;
    private MockCannonRTPPlugin plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = CannonRTPTestSupport.loadPlugin();
        MagmaCore.createInstance(plugin);
    }

    @AfterEach
    void tearDown() {
        MagmaCore.shutdown(plugin);
        CannonRTPTestSupport.setStaticField(MagmaCore.class, "instance", null);
        MockBukkit.unmock();
    }

    @Test
    void parsesMaterialListsAndClampsUnsafeNumbers() throws IOException {
        Path landingConfig = plugin.getDataFolder().toPath().resolve("landing.yml");
        Files.createDirectories(landingConfig.getParent());
        Files.writeString(landingConfig, """
                preloadedLocationsPerCannon: 0
                chargedLocationsPerCannon: 99
                searchTimeoutAttempts: 3
                slowFallingSeconds: 0
                failOpenOnProtectionErrors: true
                unsafeGroundMaterials:
                  - LAVA
                  - magma_block
                  - NOT_A_MATERIAL
                unsafeBodyMaterials:
                  - water
                  - COBWEB
                  - ALSO_NOT_A_MATERIAL
                """, StandardCharsets.UTF_8);

        new LandingSearchConfig();

        assertEquals(1, LandingSearchConfig.getPreloadedLocationsPerCannon());
        assertEquals(1, LandingSearchConfig.getChargedLocationsPerCannon());
        assertEquals(10, LandingSearchConfig.getSearchTimeoutAttempts());
        assertEquals(1, LandingSearchConfig.getSlowFallingSeconds());
        assertTrue(LandingSearchConfig.isFailOpenOnProtectionErrors());
        assertTrue(LandingSearchConfig.isUnsafeGroundMaterial(Material.LAVA));
        assertTrue(LandingSearchConfig.isUnsafeGroundMaterial(Material.MAGMA_BLOCK));
        assertFalse(LandingSearchConfig.isUnsafeGroundMaterial(Material.STONE));
        assertTrue(LandingSearchConfig.isUnsafeBodyMaterial(Material.WATER));
        assertTrue(LandingSearchConfig.isUnsafeBodyMaterial(Material.COBWEB));
        assertFalse(LandingSearchConfig.isUnsafeBodyMaterial(Material.AIR));
    }
}
