package com.magmaguy.cannonrtp;

import com.magmaguy.cannonrtp.config.CannonRTPConfigFields;
import com.magmaguy.cannonrtp.config.CannonMessagesConfig;
import com.magmaguy.cannonrtp.config.LandingSearchConfig;
import com.magmaguy.cannonrtp.services.CannonSearchState;
import com.magmaguy.cannonrtp.services.ConfiguredCannonRTP;
import com.magmaguy.magmacore.util.ConfigurationLocation;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfiguredCannonRTPTest {
    private ServerMock server;
    private MockCannonRTPPlugin plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = CannonRTPTestSupport.loadPlugin();
        CannonRTPTestSupport.setStaticField(LandingSearchConfig.class, "preloadedLocationsPerCannon", 2);
        CannonRTPTestSupport.setStaticField(LandingSearchConfig.class, "chargedLocationsPerCannon", 1);
        CannonRTPTestSupport.setStaticField(LandingSearchConfig.class, "searchTimeoutAttempts", 10);
        setStatusLabels("Ready", "Charging", "Maintaining", "Exhausted", "Disabled", "Invalid");
    }

    @AfterEach
    void tearDown() {
        setStatusLabels("Ready", "Charging", "Maintaining", "Exhausted", "Disabled", "Invalid");
        MockBukkit.unmock();
    }

    @Test
    void defaultTargetAndSearchCenterUseCannonWorld() {
        World world = server.addSimpleWorld("world");
        world.loadChunk(0, 0);
        Location cannonLocation = new Location(world, 8, 70, 8);
        ConfiguredCannonRTP cannon = cannon("default", fields("default", "Launch Pad", null, null), cannonLocation);

        assertTrue(cannon.isActive());
        assertSame(world, cannon.getTargetWorld());
        assertEquals(cannonLocation, cannon.getResolvedSearchCenter());
        assertEquals("Charging", cannon.getStatusDisplay());
    }

    @Test
    void explicitSearchCenterIsMappedIntoTargetWorld() {
        World sourceWorld = server.addSimpleWorld("source");
        World targetWorld = server.addSimpleWorld("target");
        sourceWorld.loadChunk(0, 0);
        Location configuredCenter = new Location(sourceWorld, 100, 72, -50, 90, 15);
        CannonRTPConfigFields fields = fields("crossworld", "Cross World", targetWorld.getName(), configuredCenter);

        ConfiguredCannonRTP cannon = cannon("crossworld", fields, new Location(sourceWorld, 2, 70, 2));
        Location resolvedCenter = cannon.getResolvedSearchCenter();

        assertSame(targetWorld, cannon.getTargetWorld());
        assertSame(targetWorld, resolvedCenter.getWorld());
        assertEquals(configuredCenter.getX(), resolvedCenter.getX());
        assertEquals(configuredCenter.getY(), resolvedCenter.getY());
        assertEquals(configuredCenter.getZ(), resolvedCenter.getZ());
        assertEquals(configuredCenter.getYaw(), resolvedCenter.getYaw());
        assertEquals(configuredCenter.getPitch(), resolvedCenter.getPitch());
    }

    @Test
    void queuedDestinationLifecycleTracksReadyStateAndRequeue() {
        World world = server.addSimpleWorld("world");
        world.loadChunk(0, 0);
        ConfiguredCannonRTP cannon = cannon("default", fields("default", "Launch Pad", null, null), new Location(world, 8, 70, 8));
        Location destination = new Location(world, 25.5, 66, -31.5);

        cannon.markSearchSuccess(destination);

        assertEquals(CannonSearchState.READY, cannon.getSearchState());
        assertEquals("Ready", cannon.getStatusDisplay());
        assertSame(destination, cannon.consumeQueuedLocation());
        assertEquals("Charging", cannon.getStatusDisplay());

        cannon.returnQueuedLocation(destination);

        assertSame(destination, cannon.consumeQueuedLocation());
    }

    @Test
    void configuredLabelsCoverEveryStatusDisplayBranch() {
        World world = server.addSimpleWorld("world");
        world.loadChunk(0, 0);
        Location cannonLocation = new Location(world, 8, 70, 8);
        Location firstDestination = new Location(world, 25.5, 66, -31.5);
        Location secondDestination = new Location(world, -18.5, 72, 40.5);
        CannonRTPTestSupport.setStaticField(LandingSearchConfig.class, "preloadedLocationsPerCannon", 3);
        CannonRTPTestSupport.setStaticField(LandingSearchConfig.class, "chargedLocationsPerCannon", 2);
        setStatusLabels("Listo", "Cargando", "Manteniendo", "Agotado", "Desactivado", "Inválido");

        ConfiguredCannonRTP charging = cannon(
                "charging", fields("charging", "Charging Cannon", null, null), cannonLocation);
        assertEquals("Cargando", charging.getStatusDisplay());

        charging.markSearchSuccess(firstDestination);
        assertEquals("Manteniendo", charging.getStatusDisplay());

        charging.markSearchSuccess(secondDestination);
        assertEquals("Listo", charging.getStatusDisplay());

        ConfiguredCannonRTP exhausted = cannon(
                "exhausted", fields("exhausted", "Exhausted Cannon", null, null), cannonLocation);
        exhausted.exhaustSearch();
        assertEquals("Agotado", exhausted.getStatusDisplay());

        ConfiguredCannonRTP invalid = cannon(
                "invalid", fields("invalid", "Invalid Cannon", null, null), cannonLocation);
        invalid.markInvalidConfiguration("Missing target world.");
        assertEquals("Inválido", invalid.getStatusDisplay());

        CannonRTPConfigFields disabledFields = new CannonRTPConfigFields(
                "disabled", false, "Disabled Cannon", List.of(), null, null);
        ConfiguredCannonRTP disabled = cannon("disabled", disabledFields, cannonLocation);
        assertEquals("Desactivado", disabled.getStatusDisplay());
    }

    @Test
    void requiredPermissionControlsUse() {
        World world = server.addSimpleWorld("world");
        world.loadChunk(0, 0);
        CannonRTPConfigFields fields = fields("vip", "VIP Cannon", null, null);
        CannonRTPTestSupport.setField(fields, "requiredPermission", "cannonrtp.vip");
        ConfiguredCannonRTP cannon = cannon("vip", fields, new Location(world, 8, 70, 8));
        PlayerMock player = server.addPlayer("NoAccess");

        assertFalse(cannon.canUse(player));

        player.addAttachment(plugin, "cannonrtp.vip", true);

        assertTrue(cannon.canUse(player));
    }

    private static CannonRTPConfigFields fields(String id, String displayName, String targetWorld, Location searchCenter) {
        return new CannonRTPConfigFields(id, true, displayName, List.of(), targetWorld, searchCenter);
    }

    private static ConfiguredCannonRTP cannon(String id, CannonRTPConfigFields fields, Location location) {
        return new ConfiguredCannonRTP(
                id,
                id + "#0",
                fields,
                location,
                ConfigurationLocation.deserialize(location));
    }

    private static void setStatusLabels(String ready,
                                        String charging,
                                        String maintaining,
                                        String exhausted,
                                        String disabled,
                                        String invalid) {
        CannonRTPTestSupport.setStaticField(CannonMessagesConfig.class, "statusReadyLabel", ready);
        CannonRTPTestSupport.setStaticField(CannonMessagesConfig.class, "statusChargingLabel", charging);
        CannonRTPTestSupport.setStaticField(CannonMessagesConfig.class, "statusMaintainingLabel", maintaining);
        CannonRTPTestSupport.setStaticField(CannonMessagesConfig.class, "statusExhaustedLabel", exhausted);
        CannonRTPTestSupport.setStaticField(CannonMessagesConfig.class, "statusDisabledLabel", disabled);
        CannonRTPTestSupport.setStaticField(CannonMessagesConfig.class, "statusInvalidLabel", invalid);
    }
}
