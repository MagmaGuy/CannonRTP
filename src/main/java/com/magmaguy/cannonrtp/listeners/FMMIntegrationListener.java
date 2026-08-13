package com.magmaguy.cannonrtp.listeners;

import com.magmaguy.cannonrtp.CannonRTP;
import com.magmaguy.cannonrtp.services.CannonRTPManager;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;

/**
 * Invalidates cached FMM model resolutions whenever FreeMinecraftModels toggles
 * its enabled state. Without this, cannons can end up showing particle visuals
 * even after FMM becomes available (or vice versa).
 */
public class FMMIntegrationListener implements Listener {
    private static final String FMM_PLUGIN_NAME = "FreeMinecraftModels";
    private static final String FMM_RELOADED_EVENT_CLASS =
            "com.magmaguy.freeminecraftmodels.api.FmmReloadedEvent";

    private final CannonRTPManager manager;
    private boolean reloadEventRegistered;

    public FMMIntegrationListener(CannonRTPManager manager) {
        this.manager = manager;
        registerReloadEventIfAvailable();
    }

    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        if (FMM_PLUGIN_NAME.equals(event.getPlugin().getName())) {
            registerReloadEventIfAvailable();
            manager.handleFMMStateChange();
        }
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        if (FMM_PLUGIN_NAME.equals(event.getPlugin().getName())) {
            manager.handleFMMStateChange();
        }
    }

    /**
     * FmmReloadedEvent was added after CannonRTP's minimum supported FMM API.
     * Register it dynamically when present so current FMM reloads repair models
     * without making older optional FMM installations fail class loading.
     */
    @SuppressWarnings("unchecked")
    private void registerReloadEventIfAvailable() {
        if (reloadEventRegistered) return;
        Plugin fmm = Bukkit.getPluginManager().getPlugin(FMM_PLUGIN_NAME);
        if (fmm == null) return;
        try {
            Class<?> rawEventClass = Class.forName(
                    FMM_RELOADED_EVENT_CLASS,
                    false,
                    fmm.getClass().getClassLoader());
            if (!Event.class.isAssignableFrom(rawEventClass)) return;

            Bukkit.getPluginManager().registerEvent(
                    (Class<? extends Event>) rawEventClass,
                    this,
                    EventPriority.MONITOR,
                    (listener, event) -> manager.handleFMMReloaded(),
                    CannonRTP.getInstance(),
                    true);
            reloadEventRegistered = true;
        } catch (ClassNotFoundException | LinkageError ignored) {
            // Older FMM versions have no reload-complete event. Plugin
            // enable/disable invalidation above remains the compatible fallback.
        }
    }
}
