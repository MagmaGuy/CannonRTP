package com.magmaguy.cannonrtp.api;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired the moment a player is about to begin a cannon launch sequence, before any
 * effects are applied. Cancelling this event aborts the launch entirely — the player
 * stays where they are and the destination is returned to the cannon's queue so it
 * can be reused on the next launch.
 */
public class CannonRTPLaunchEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    @Getter
    private final Player player;
    @Getter
    private final String cannonId;
    @Getter
    private final String cannonDisplayName;
    @Getter
    private final Location cannonLocation;
    @Getter
    private final Location destination;
    private boolean cancelled;

    public CannonRTPLaunchEvent(Player player,
                                String cannonId,
                                String cannonDisplayName,
                                Location cannonLocation,
                                Location destination) {
        this.player = player;
        this.cannonId = cannonId;
        this.cannonDisplayName = cannonDisplayName;
        this.cannonLocation = cannonLocation;
        this.destination = destination;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
