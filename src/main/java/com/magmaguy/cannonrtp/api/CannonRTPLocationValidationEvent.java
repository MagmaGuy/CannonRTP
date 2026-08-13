package com.magmaguy.cannonrtp.api;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired for each candidate landing location as CannonRTP validates it during the
 * preload search, and fired again for the consumed destination when a launch
 * revalidates it at commit time (just before the teleport). Listeners may veto a
 * location by calling {@link #setRejected(String)} — during preload CannonRTP
 * discards the candidate as if it had failed an internal check and continues
 * searching; at commit time the launch is cancelled and the player recovered.
 * Useful for custom region checks beyond the built-in protection integrations.
 *
 * <p>Non-cancellable in the Bukkit sense — use {@code setRejected} to signal
 * rejection. Both dispatches occur on the main thread.</p>
 */
public class CannonRTPLocationValidationEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    @Getter
    private final String cannonId;
    @Getter
    private final String cannonDisplayName;
    @Getter
    private final Location candidate;
    @Getter
    private boolean rejected;
    @Getter
    private String rejectionReason;

    public CannonRTPLocationValidationEvent(String cannonId,
                                            String cannonDisplayName,
                                            Location candidate) {
        this.cannonId = cannonId;
        this.cannonDisplayName = cannonDisplayName;
        this.candidate = candidate;
    }

    public void setRejected(String reason) {
        this.rejected = true;
        this.rejectionReason = reason;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
