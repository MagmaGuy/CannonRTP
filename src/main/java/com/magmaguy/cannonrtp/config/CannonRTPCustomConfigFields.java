package com.magmaguy.cannonrtp.config;

import com.magmaguy.magmacore.util.ChatColorConverter;

/**
 * CannonRTP wrapper over MagmaCore's CustomConfigFields that adds a translation hook.
 *
 * <p>Release 1 ships English-only — the hook is a passthrough that applies
 * ChatColorConverter so gradient/hex tags resolve. A real translation backend
 * (CSV-backed, à la EliteMobs) can be wired in behind this method later without
 * changing callers.</p>
 */
public abstract class CannonRTPCustomConfigFields extends com.magmaguy.magmacore.config.CustomConfigFields {

    public CannonRTPCustomConfigFields(String filename, boolean isEnabled) {
        super(filename, isEnabled);
    }

    /**
     * Marks a string as translatable and returns the display-ready (color-converted) value.
     * Translators see the raw template; players see expanded gradients/hex.
     * The filename and key parameters are currently unused; they are reserved for a
     * future translation backend.
     */
    protected String translatable(String filename, String key, String value) {
        if (value == null) return null;
        return ChatColorConverter.convert(value);
    }
}
