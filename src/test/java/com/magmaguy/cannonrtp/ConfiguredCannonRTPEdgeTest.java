package com.magmaguy.cannonrtp;

import com.magmaguy.cannonrtp.config.CannonRTPConfigFields;
import com.magmaguy.cannonrtp.config.LandingSearchConfig;
import com.magmaguy.cannonrtp.services.CannonSearchState;
import com.magmaguy.cannonrtp.services.ConfiguredCannonRTP;
import com.magmaguy.cannonrtp.services.RemovalReason;
import com.magmaguy.cannonrtp.services.SearchFailureReason;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfiguredCannonRTPEdgeTest {
    private ServerMock server;
    private MockCannonRTPPlugin plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = CannonRTPTestSupport.loadPlugin();
        CannonRTPTestSupport.setStaticField(LandingSearchConfig.class, "preloadedLocationsPerCannon", 2);
        CannonRTPTestSupport.setStaticField(LandingSearchConfig.class, "chargedLocationsPerCannon", 1);
        CannonRTPTestSupport.setStaticField(LandingSearchConfig.class, "searchTimeoutAttempts", 2);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void removalReasonsTrackChunkAndWorldActiveState() {
        World world = server.addSimpleWorld("world");
        world.loadChunk(0, 0);
        ConfiguredCannonRTP cannon = cannon("stateful", world, new Location(world, 8, 70, 8));

        assertTrue(cannon.isActive());

        cannon.remove(RemovalReason.CHUNK_UNLOAD);

        assertFalse(cannon.isChunkLoaded());
        assertFalse(cannon.isActive());

        cannon.setChunkLoaded(true);
        cannon.handleChunkLoad();

        assertTrue(cannon.isActive());
        assertEquals(CannonSearchState.SEARCHING, cannon.getSearchState());

        cannon.remove(RemovalReason.WORLD_UNLOAD);

        assertFalse(cannon.isCannonWorldLoaded());
        assertFalse(cannon.isChunkLoaded());
        assertFalse(cannon.isActive());
    }

    @Test
    void searchFailuresProduceOperatorSummaryAndExhaustedStatus() {
        World world = server.addSimpleWorld("world");
        world.loadChunk(0, 0);
        ConfiguredCannonRTP cannon = cannon("landing", world, new Location(world, 8, 70, 8));

        cannon.markSearchFailure(SearchFailureReason.HAZARDOUS_TERRAIN);
        cannon.markSearchFailure(SearchFailureReason.PROTECTED_LAND);
        cannon.exhaustSearch();

        assertEquals(CannonSearchState.EXHAUSTED, cannon.getSearchState());
        assertEquals("Exhausted", cannon.getStatusDisplay());
        assertTrue(cannon.buildFailureSummary().contains("hazardous"));
        assertTrue(cannon.buildFailureSummary().contains("protected land"));
        assertFalse(cannon.needsMoreLocations() && cannon.isCharged());
    }

    @Test
    void notifyThrottleCanBeClearedWhenPlayerLeaves() {
        World world = server.addSimpleWorld("world");
        world.loadChunk(0, 0);
        ConfiguredCannonRTP cannon = cannon("notify", world, new Location(world, 8, 70, 8));
        PlayerMock player = server.addPlayer("Traveler");

        assertTrue(cannon.shouldNotify(player, 60_000));
        assertFalse(cannon.shouldNotify(player, 60_000));

        cannon.clearNotifyThrottle(player.getUniqueId());

        assertTrue(cannon.shouldNotify(player, 60_000));
    }

    private static ConfiguredCannonRTP cannon(String id, World world, Location location) {
        CannonRTPConfigFields fields = new CannonRTPConfigFields(id, true, "Launch Pad", List.of(), world.getName(), null);
        return new ConfiguredCannonRTP(id, id + "#0", fields, location, ConfigurationLocation.deserialize(location));
    }
}
