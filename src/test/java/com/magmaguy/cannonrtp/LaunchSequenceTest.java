package com.magmaguy.cannonrtp;

import com.magmaguy.cannonrtp.api.CannonRTPLandingEvent;
import com.magmaguy.cannonrtp.config.CannonMessagesConfig;
import com.magmaguy.cannonrtp.config.CannonRTPConfigFields;
import com.magmaguy.cannonrtp.config.LandingSearchConfig;
import com.magmaguy.cannonrtp.services.ConfiguredCannonRTP;
import com.magmaguy.cannonrtp.services.LaunchPhase;
import com.magmaguy.cannonrtp.services.LaunchSequence;
import com.magmaguy.magmacore.util.ConfigurationLocation;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LaunchSequenceTest {
    private ServerMock server;
    private MockCannonRTPPlugin plugin;
    private World world;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = CannonRTPTestSupport.loadPlugin();
        world = server.addSimpleWorld("world");
        CannonRTPTestSupport.setStaticField(LandingSearchConfig.class, "slowFallingSeconds", 1);
        CannonRTPTestSupport.setStaticField(CannonMessagesConfig.class, "prefix", "");
        CannonRTPTestSupport.setStaticField(
                CannonMessagesConfig.class,
                "destinationPreviewSubtitle",
                "$x $y $z");
        CannonRTPTestSupport.setStaticField(
                CannonMessagesConfig.class,
                "destinationConfirmedSubtitle",
                "$x $y $z $world");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void launchSequenceTeleportsPlayerAndFiresLandingEvent() {
        AtomicReference<CannonRTPLandingEvent> landingEvent = new AtomicReference<>();
        server.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onLanding(CannonRTPLandingEvent event) {
                landingEvent.set(event);
            }
        }, plugin);

        PlayerMock player = server.addPlayer("Tiago");
        Location cannonLocation = new Location(world, 4, 70, 4);
        player.teleport(cannonLocation);
        ConfiguredCannonRTP cannon = cannon(cannonLocation);
        Location destination = new Location(world, 20.5, 65, -12.5);
        world.setType(destination.getBlockX(), destination.getBlockY() - 1, destination.getBlockZ(), Material.STONE);
        world.setType(destination.getBlockX(), destination.getBlockY(), destination.getBlockZ(), Material.AIR);
        LaunchSequence sequence = new LaunchSequence(player, cannon, destination);

        assertTrue(sequence.tick());
        assertEquals(LaunchPhase.FIRING, sequence.getPhase());
        assertTrue(player.hasPotionEffect(PotionEffectType.LEVITATION));

        assertTrue(sequence.tick());
        assertEquals(LaunchPhase.TELEPORTING, sequence.getPhase());
        assertFalse(player.hasPotionEffect(PotionEffectType.LEVITATION));

        assertTrue(sequence.tick());
        assertEquals(LaunchPhase.DROPPING, sequence.getPhase());
        assertEquals(destination.getX(), player.getLocation().getX());
        assertEquals(destination.getY() + 50, player.getLocation().getY());
        assertEquals(destination.getZ(), player.getLocation().getZ());
        assertTrue(player.hasPotionEffect(PotionEffectType.SLOW_FALLING));

        // MockBukkit does not implement Block#isPassable(), so move directly from
        // the verified drop state into landing instead of exercising hasLanded().
        player.teleport(destination);
        player.setVelocity(new Vector(0, 0, 0));
        CannonRTPTestSupport.setField(sequence, "phase", LaunchPhase.LANDING);
        CannonRTPTestSupport.setField(sequence, "phaseTick", 0);

        assertFalse(sequence.tick());
        assertTrue(sequence.isFinished());
        assertFalse(player.hasPotionEffect(PotionEffectType.SLOW_FALLING));
        assertNotNull(landingEvent.get());
        assertEquals("fast", landingEvent.get().getCannonId());
        assertEquals(destination, landingEvent.get().getDestination());
    }

    private static ConfiguredCannonRTP cannon(Location cannonLocation) {
        CannonRTPConfigFields fields = new CannonRTPConfigFields(
                "fast",
                true,
                "Fast Cannon",
                List.of(),
                cannonLocation.getWorld().getName(),
                null);
        CannonRTPTestSupport.setField(fields, "launchWarmupTicks", 1);
        CannonRTPTestSupport.setField(fields, "verticalBoostTicks", 1);
        CannonRTPTestSupport.setField(fields, "verticalBoostVelocity", 1.25);
        return new ConfiguredCannonRTP(
                "fast",
                "fast#0",
                fields,
                cannonLocation,
                ConfigurationLocation.deserialize(cannonLocation));
    }
}
