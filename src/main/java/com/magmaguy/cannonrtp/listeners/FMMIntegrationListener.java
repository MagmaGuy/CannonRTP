package com.magmaguy.cannonrtp.listeners;

import com.magmaguy.cannonrtp.services.CannonRTPManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;

/**
 * Invalidates cached FMM model resolutions whenever FreeMinecraftModels toggles
 * its enabled state. Without this, cannons can end up showing particle visuals
 * even after FMM becomes available (or vice versa).
 */
public class FMMIntegrationListener implements Listener {
    private static final String FMM_PLUGIN_NAME = "FreeMinecraftModels";

    private final CannonRTPManager manager;

    public FMMIntegrationListener(CannonRTPManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        if (FMM_PLUGIN_NAME.equals(event.getPlugin().getName())) {
            manager.handleFMMStateChange();
        }
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        if (FMM_PLUGIN_NAME.equals(event.getPlugin().getName())) {
            manager.handleFMMStateChange();
        }
    }
}
