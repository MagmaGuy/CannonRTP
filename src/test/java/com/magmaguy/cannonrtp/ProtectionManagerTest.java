package com.magmaguy.cannonrtp;

import com.magmaguy.cannonrtp.config.LandingSearchConfig;
import com.magmaguy.cannonrtp.protection.ProtectionAdapter;
import com.magmaguy.cannonrtp.protection.ProtectionManager;
import com.magmaguy.cannonrtp.protection.ProtectionQueryResult;
import com.magmaguy.magmacore.MagmaCore;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProtectionManagerTest {
    private ServerMock server;
    private MockCannonRTPPlugin plugin;
    private Location location;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = CannonRTPTestSupport.loadPlugin();
        MagmaCore.createInstance(plugin);
        World world = server.addSimpleWorld("world");
        location = new Location(world, 0, 64, 0);
        activeAdapters().clear();
    }

    @AfterEach
    void tearDown() {
        ProtectionManager.shutdown();
        MagmaCore.shutdown(plugin);
        CannonRTPTestSupport.setStaticField(MagmaCore.class, "instance", null);
        MockBukkit.unmock();
    }

    @Test
    void adapterFailureBlocksWhenFailClosed() {
        CannonRTPTestSupport.setStaticField(LandingSearchConfig.class, "failOpenOnProtectionErrors", false);
        activeAdapters().add(new ThrowingAdapter("BrokenGuard"));

        ProtectionQueryResult result = ProtectionManager.inspect(location);

        assertFalse(result.allowed());
        assertEquals("BrokenGuard", result.pluginName());
        assertEquals("its API could not be queried safely", result.reason());
    }

    @Test
    void adapterFailureAllowsWhenFailOpen() {
        CannonRTPTestSupport.setStaticField(LandingSearchConfig.class, "failOpenOnProtectionErrors", true);
        activeAdapters().add(new ThrowingAdapter("BrokenGuard"));

        ProtectionQueryResult result = ProtectionManager.inspect(location);

        assertTrue(result.allowed());
    }

    @Test
    void firstBlockingAdapterShortCircuitsLandingValidation() {
        CannonRTPTestSupport.setStaticField(LandingSearchConfig.class, "failOpenOnProtectionErrors", true);
        CountingAdapter first = new CountingAdapter(ProtectionQueryResult.blocked("Claims", "inside a claim"));
        CountingAdapter second = new CountingAdapter(ProtectionQueryResult.pass());
        activeAdapters().add(first);
        activeAdapters().add(second);

        ProtectionQueryResult result = ProtectionManager.inspect(location);

        assertFalse(result.allowed());
        assertEquals("Claims", result.pluginName());
        assertEquals(1, first.calls);
        assertEquals(0, second.calls);
    }

    @SuppressWarnings("unchecked")
    private static List<ProtectionAdapter> activeAdapters() {
        try {
            Field field = ProtectionManager.class.getDeclaredField("activeAdapters");
            field.setAccessible(true);
            return (List<ProtectionAdapter>) field.get(null);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Failed to access active protection adapters", exception);
        }
    }

    private record ThrowingAdapter(String pluginName) implements ProtectionAdapter {
        @Override
        public ProtectionQueryResult query(Location location) throws Exception {
            throw new Exception("test failure");
        }

        @Override
        public String getPluginName() {
            return pluginName;
        }
    }

    private static final class CountingAdapter implements ProtectionAdapter {
        private final ProtectionQueryResult result;
        private int calls;

        private CountingAdapter(ProtectionQueryResult result) {
            this.result = result;
        }

        @Override
        public String getPluginName() {
            return result.pluginName();
        }

        @Override
        public ProtectionQueryResult query(Location location) {
            calls++;
            return result;
        }
    }
}
