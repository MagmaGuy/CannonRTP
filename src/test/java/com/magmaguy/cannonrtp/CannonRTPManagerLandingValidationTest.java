package com.magmaguy.cannonrtp;

import com.magmaguy.cannonrtp.config.CannonRTPConfigFields;
import com.magmaguy.cannonrtp.config.LandingSearchConfig;
import com.magmaguy.cannonrtp.services.CannonRTPManager;
import com.magmaguy.cannonrtp.services.CannonSearchState;
import com.magmaguy.cannonrtp.services.ConfiguredCannonRTP;
import com.magmaguy.cannonrtp.services.SearchFailureReason;
import com.magmaguy.magmacore.util.ConfigurationLocation;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CannonRTPManagerLandingValidationTest {
    private ServerMock server;
    private MockCannonRTPPlugin plugin;
    private CannonRTPManager manager;

    @BeforeEach
    void setUp() throws Exception {
        server = MockBukkit.mock();
        plugin = CannonRTPTestSupport.loadPlugin();
        manager = new CannonRTPManager(null);
        CannonRTPTestSupport.setStaticField(LandingSearchConfig.class, "preloadedLocationsPerCannon", 2);
        CannonRTPTestSupport.setStaticField(LandingSearchConfig.class, "chargedLocationsPerCannon", 1);
        CannonRTPTestSupport.setStaticField(LandingSearchConfig.class, "searchTimeoutAttempts", 10);
        setStaticField(LandingSearchConfig.class, "unsafeGroundMaterials", EnumSet.of(Material.MAGMA_BLOCK));
        setStaticField(LandingSearchConfig.class, "unsafeBodyMaterials", EnumSet.of(Material.COBWEB));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void preloadRejectsLocationsOutsideWorldBorderBeforeLoadingTerrain() throws Exception {
        World world = server.addSimpleWorld("border_world");
        world.getWorldBorder().setCenter(0, 0);
        world.getWorldBorder().setSize(10);
        ConfiguredCannonRTP cannon = cannon(world, new Location(world, 1000, 64, 1000));

        attemptPreload(cannon, world, new Location(world, 1000, 64, 1000));

        assertEquals(1, cannon.getSearchAttempts());
        assertTrue(cannon.buildFailureSummary().contains("outside the world border"));
    }

    @Test
    @Disabled("MockBukkit Block#isPassable is not implemented; cover unsafe material landing in a real server/provider lab.")
    void preloadRejectsConfiguredUnsafeGroundMaterials() throws Exception {
        World world = server.addSimpleWorld("material_world");
        world.loadChunk(0, 0);
        world.setType(0, 64, 0, Material.MAGMA_BLOCK);
        ConfiguredCannonRTP cannon = cannon(world, new Location(world, 0, 64, 0));

        attemptPreload(cannon, world, new Location(world, 0, 64, 0));

        assertEquals(CannonSearchState.SEARCHING, cannon.getSearchState());
        assertTrue(cannon.buildFailureSummary().contains("hazardous"));
    }

    private void attemptPreload(ConfiguredCannonRTP cannon, World targetWorld, Location searchCenter) throws Exception {
        Method method = CannonRTPManager.class.getDeclaredMethod("attemptPreload", ConfiguredCannonRTP.class, World.class, Location.class);
        method.setAccessible(true);
        method.invoke(manager, cannon, targetWorld, searchCenter);
    }

    private static ConfiguredCannonRTP cannon(World world, Location location) {
        CannonRTPConfigFields fields = new CannonRTPConfigFields(
                "validator",
                true,
                "Validator",
                List.of(),
                world.getName(),
                location);
        CannonRTPTestSupport.setField(fields, "minSearchRadius", 0);
        CannonRTPTestSupport.setField(fields, "maxSearchRadius", 1);
        return new ConfiguredCannonRTP("validator", "validator#0", fields, location, ConfigurationLocation.deserialize(location));
    }

    private static void setStaticField(Class<?> type, String fieldName, Object value) throws Exception {
        Field field = type.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(null, value);
    }
}
