package com.magmaguy.cannonrtp.config;

import com.magmaguy.magmacore.util.ConfigurationLocation;
import lombok.Getter;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

public class CannonRTPConfigFields extends CannonRTPCustomConfigFields {
    @Getter
    private String displayName;
    /**
     * The world this config is bound to. Runtime cannon instances are spawned from
     * the {@link #cannonLocations} list below; this list is empty on first install so
     * nothing spawns until an admin runs {@code /wc create} or {@code /wc place}.
     */
    @Getter
    private List<String> cannonLocations = new ArrayList<>();
    @Getter
    private String targetWorldName;
    @Getter
    private Location searchCenter;
    @Getter
    private double triggerRadius = 1.75;
    @Getter
    private int minSearchRadius = 500;
    @Getter
    private int maxSearchRadius = 5000;
    @Getter
    private int launchWarmupTicks = 42;
    @Getter
    private int verticalBoostTicks = 45;
    @Getter
    private double verticalBoostVelocity = 1.35;
    @Getter
    private boolean enableParticles = true;
    @Getter
    private String requiredPermission = "";
    @Getter
    protected String customModel = "";

    public CannonRTPConfigFields(String filename,
                                 boolean isEnabled,
                                 String displayName,
                                 List<String> cannonLocations,
                                 String targetWorldName,
                                 Location searchCenter) {
        super(filename, isEnabled);
        this.displayName = displayName;
        this.cannonLocations = cannonLocations != null ? new ArrayList<>(cannonLocations) : new ArrayList<>();
        this.targetWorldName = targetWorldName;
        this.searchCenter = searchCenter;
    }

    public CannonRTPConfigFields(String filename, boolean isEnabled) {
        super(filename, isEnabled);
    }

    @Override
    public void processConfigFields() {
        isEnabled = processBoolean("isEnabled", isEnabled, true, true);
        displayName = translatable(filename, "displayName",
                processString("displayName", displayName, "CannonRTP", true));
        cannonLocations = processStringList("cannonLocations", cannonLocations, new ArrayList<>(), true);

        targetWorldName = processString("targetWorld", targetWorldName, targetWorldName, true);

        String serializedSearchCenter = processString("searchCenter",
                searchCenter == null ? null : ConfigurationLocation.deserialize(searchCenter), null, false);
        searchCenter = ConfigurationLocation.serialize(serializedSearchCenter, true);

        triggerRadius = Math.max(0.5, processDouble("triggerRadius", triggerRadius, 1.75, true));
        minSearchRadius = Math.max(0, processInt("minSearchRadius", minSearchRadius, 500, true));
        maxSearchRadius = Math.max(minSearchRadius + 1, processInt("maxSearchRadius", maxSearchRadius, 5000, true));
        launchWarmupTicks = Math.max(1, processInt("launchWarmupTicks", launchWarmupTicks, 42, true));
        verticalBoostTicks = Math.max(0, processInt("verticalBoostTicks", verticalBoostTicks, 45, true));
        verticalBoostVelocity = Math.max(0, processDouble("verticalBoostVelocity", verticalBoostVelocity, 1.35, true));
        enableParticles = processBoolean("enableParticles", enableParticles, true, true);
        requiredPermission = processString("requiredPermission", requiredPermission, "", false);
        customModel = processString("customModel", customModel, "", false);
    }

    /**
     * Appends a new location entry to the list and returns the new list. Callers are
     * responsible for persisting the change via ConfigurationEngine.writeValue.
     */
    public List<String> addCannonLocation(Location location) {
        if (cannonLocations == null) cannonLocations = new ArrayList<>();
        cannonLocations.add(ConfigurationLocation.deserialize(location));
        return cannonLocations;
    }

    /**
     * Removes a location entry by its serialized form. Returns true if removed.
     */
    public boolean removeCannonLocation(String serialized) {
        if (cannonLocations == null) return false;
        return cannonLocations.remove(serialized);
    }
}
