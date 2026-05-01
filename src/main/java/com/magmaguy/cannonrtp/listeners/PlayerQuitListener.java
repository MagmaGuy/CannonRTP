package com.magmaguy.cannonrtp.listeners;

import com.magmaguy.cannonrtp.services.CannonRTPManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {
    private final CannonRTPManager manager;

    public PlayerQuitListener(CannonRTPManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        manager.handlePlayerQuit(event.getPlayer().getUniqueId());
    }
}
