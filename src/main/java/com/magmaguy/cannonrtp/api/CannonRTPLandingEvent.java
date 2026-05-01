package com.magmaguy.cannonrtp.api;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired after a player has been teleported to the chosen destination and the launch
 * sequence has fully completed. Non-cancellable; use CannonRTPLaunchEvent to veto
 * a launch before it happens.
 */
public class CannonRTPLandingEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    @Getter
    private final Player player;
    @Getter
    private final String cannonId;
    @Getter
    private final String cannonDisplayName;
    @Getter
    private final Location destination;

    public CannonRTPLandingEvent(Player player,
                                 String cannonId,
                                 String cannonDisplayName,
                                 Location destination) {
        this.player = player;
        this.cannonId = cannonId;
        this.cannonDisplayName = cannonDisplayName;
        this.destination = destination;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
