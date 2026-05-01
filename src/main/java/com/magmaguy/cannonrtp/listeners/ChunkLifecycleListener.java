package com.magmaguy.cannonrtp.listeners;

import com.magmaguy.cannonrtp.services.CannonRTPManager;
import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

public class ChunkLifecycleListener implements Listener {
    private final CannonRTPManager manager;

    public ChunkLifecycleListener(CannonRTPManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();
        manager.handleChunkLoad(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        Chunk chunk = event.getChunk();
        manager.handleChunkUnload(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
    }
}
