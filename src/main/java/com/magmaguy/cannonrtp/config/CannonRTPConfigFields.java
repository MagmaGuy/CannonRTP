package com.magmaguy.cannonrtp.config;

import com.magmaguy.magmacore.config.CustomConfigFields;
import com.magmaguy.magmacore.util.ConfigurationLocation;
import lombok.Getter;
import org.bukkit.Location;

public class CannonRTPConfigFields extends CustomConfigFields {
    @Getter
    private String displayName;
    @Getter
    private Location cannonLocation;
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
    private int launchWarmupSeconds = 6;
    @Getter
    private int verticalBoostTicks = 45;
    @Getter
    private double verticalBoostVelocity = 1.35;
    @Getter
    private boolean enableParticles = true;
    @Getter
    private String requiredPermission = "";
    @Getter
    private String customModel = "";

    public CannonRTPConfigFields(String filename,
                                    boolean isEnabled,
                                    String displayName,
                                    Location cannonLocation,
                                    String targetWorldName,
                                    Location searchCenter) {
        super(filename, isEnabled);
        this.displayName = displayName;
        this.cannonLocation = cannonLocation;
        this.targetWorldName = targetWorldName;
        this.searchCenter = searchCenter;
    }

    public CannonRTPConfigFields(String filename, boolean isEnabled) {
        super(filename, isEnabled);
    }

    @Override
    public void processConfigFields() {
        isEnabled = processBoolean("isEnabled", isEnabled, true, true);
        displayName = processString("displayName", displayName, "CannonRTP", true);
        cannonLocation = processLocation("cannonLocation", cannonLocation, null, true);
        targetWorldName = processString("targetWorld", targetWorldName, targetWorldName, true);

        String serializedSearchCenter = processString("searchCenter",
                searchCenter == null ? null : ConfigurationLocation.deserialize(searchCenter), null, false);
        searchCenter = ConfigurationLocation.serialize(serializedSearchCenter, true);

        triggerRadius = Math.max(0.5, processDouble("triggerRadius", triggerRadius, 1.75, true));
        minSearchRadius = Math.max(0, processInt("minSearchRadius", minSearchRadius, 500, true));
        maxSearchRadius = Math.max(minSearchRadius + 1, processInt("maxSearchRadius", maxSearchRadius, 5000, true));
        launchWarmupSeconds = Math.max(0, processInt("launchWarmupSeconds", launchWarmupSeconds, 6, true));
        verticalBoostTicks = Math.max(0, processInt("verticalBoostTicks", verticalBoostTicks, 45, true));
        verticalBoostVelocity = Math.max(0, processDouble("verticalBoostVelocity", verticalBoostVelocity, 1.35, true));
        enableParticles = processBoolean("enableParticles", enableParticles, true, true);
        requiredPermission = processString("requiredPermission", requiredPermission, "", false);
        customModel = processString("customModel", customModel, "", false);
    }
}

