package com.magmaguy.cannonrtp.listeners;

import com.magmaguy.cannonrtp.services.CannonRTPManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

public class WorldLifecycleListener implements Listener {
    private final CannonRTPManager manager;

    public WorldLifecycleListener(CannonRTPManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        manager.handleWorldLoad(event.getWorld().getName());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldUnload(WorldUnloadEvent event) {
        manager.handleWorldUnload(event.getWorld().getName());
    }
}
