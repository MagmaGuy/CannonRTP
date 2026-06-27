package com.magmaguy.cannonrtp;

import com.magmaguy.cannonrtp.listeners.FMMIntegrationListener;
import com.magmaguy.cannonrtp.services.CannonRTPManager;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FMMIntegrationListenerTest {
    private MockCannonRTPPlugin cannonPlugin;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        cannonPlugin = CannonRTPTestSupport.loadPlugin();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void onlyFreeMinecraftModelsEnableAndDisableEventsInvalidateModelCaches() {
        RecordingManager manager = new RecordingManager();
        FMMIntegrationListener listener = new FMMIntegrationListener(manager);
        Plugin fmm = MockBukkit.createMockPlugin("FreeMinecraftModels");
        Plugin other = MockBukkit.createMockPlugin("OtherPlugin");

        listener.onPluginEnable(new PluginEnableEvent(other));
        listener.onPluginDisable(new PluginDisableEvent(other));

        assertEquals(0, manager.fmmInvalidations);

        listener.onPluginEnable(new PluginEnableEvent(fmm));
        listener.onPluginDisable(new PluginDisableEvent(fmm));

        assertEquals(2, manager.fmmInvalidations);
    }

    private final class RecordingManager extends CannonRTPManager {
        private int fmmInvalidations;

        private RecordingManager() {
            super(null);
        }

        @Override
        public void handleFMMStateChange() {
            fmmInvalidations++;
        }
    }
}
