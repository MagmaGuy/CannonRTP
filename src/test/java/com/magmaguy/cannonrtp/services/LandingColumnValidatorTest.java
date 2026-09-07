package com.magmaguy.cannonrtp.services;

import com.magmaguy.cannonrtp.config.LandingSearchConfig;
import org.bukkit.Location;
import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.nio.file.Path;

import static com.magmaguy.cannonrtp.services.LandingColumnValidator.Result.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class LandingColumnValidatorTest {
    @TempDir Path directory;
    private WorldMock world;

    @BeforeEach
    void setUp() {
        world = MockBukkit.mock().addSimpleWorld("landing");
        new LandingSearchConfig(directory.resolve("landing.yml").toFile());
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void acceptsClearColumnAtBothUsableHeightLimitsAndRejectsOverflow() {
        assertEquals(SAFE, LandingColumnValidator.validate(column(world.getMinHeight() + 1)));
        assertEquals(SAFE, LandingColumnValidator.validate(column(world.getMaxHeight() - 52)));
        assertEquals(NO_SAFE_SURFACE, LandingColumnValidator.validate(column(world.getMaxHeight() - 51)));
    }

    @ParameterizedTest(name = "{displayName} [{index}] ground={0}")
    @CsvSource({"AIR, NO_SAFE_SURFACE", "WATER, NO_SAFE_SURFACE", "LAVA, NO_SAFE_SURFACE", "MAGMA_BLOCK, HAZARDOUS_TERRAIN"})
    void rejectsMissingLiquidAndConfiguredUnsafeSupport(Material ground, LandingColumnValidator.Result expected) {
        Location landing = column(100);
        world.getBlockAt(0, 99, 0).setType(ground);
        assertEquals(expected, LandingColumnValidator.validate(landing));
    }

    @Test
    void rejectsAbsentWorld() {
        assertEquals(NO_SAFE_SURFACE, LandingColumnValidator.validate(null));
        assertEquals(NO_SAFE_SURFACE, LandingColumnValidator.validate(new Location(null, 0, 100, 0)));
    }

    private Location column(int landingY) {
        world.getBlockAt(0, landingY - 1, 0).setType(Material.STONE);
        for (int y = landingY; y < Math.min(world.getMaxHeight(), landingY + 52); y++) {
            world.getBlockAt(0, y, 0).setType(Material.AIR);
        }
        return new Location(world, 0.5, landingY, 0.5);
    }
}
