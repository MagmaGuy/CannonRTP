package com.magmaguy.cannonrtp.config;

import com.magmaguy.magmacore.config.ConfigurationEngine;
import com.magmaguy.magmacore.config.ConfigurationFile;
import lombok.Getter;

import java.util.List;

public class ProtectionSettingsConfig extends ConfigurationFile {
    @Getter
    private static boolean worldGuardEnabled;
    @Getter
    private static boolean worldGuardAllowGlobalRegionOnly;
    @Getter
    private static boolean worldGuardAllowBuildAllowedRegions;
    @Getter
    private static boolean worldGuardAllowPassthroughRegions;
    @Getter
    private static boolean townyEnabled;
    @Getter
    private static boolean townyAllowWilderness;
    @Getter
    private static boolean townyAllowNationZones;
    @Getter
    private static boolean townyAllowClaimedTownBlocks;
    @Getter
    private static boolean landsEnabled;
    @Getter
    private static boolean landsAllowUnclaimedAreas;
    @Getter
    private static boolean landsAllowClaimedAreas;
    @Getter
    private static boolean griefPreventionEnabled;
    @Getter
    private static boolean griefPreventionAllowWilderness;
    @Getter
    private static boolean griefPreventionAllowAdminClaims;
    @Getter
    private static boolean griefPreventionAllowPlayerClaims;
    @Getter
    private static boolean huskTownsEnabled;
    @Getter
    private static boolean huskTownsAllowWilderness;
    @Getter
    private static boolean huskTownsAllowAdminClaims;
    @Getter
    private static boolean huskTownsAllowRegularClaims;
    @Getter
    private static boolean huskTownsAllowFarmClaims;
    @Getter
    private static boolean huskTownsAllowPlotClaims;
    @Getter
    private static boolean huskClaimsEnabled;
    @Getter
    private static boolean huskClaimsAllowWilderness;
    @Getter
    private static boolean huskClaimsAllowAdminClaims;
    @Getter
    private static boolean huskClaimsAllowPlayerClaims;

    public ProtectionSettingsConfig() {
        super("protection.yml");
    }

    @Override
    public void initializeValues() {
        worldGuardEnabled = ConfigurationEngine.setBoolean(
                List.of("Enables WorldGuard landing checks."),
                fileConfiguration, "worldGuard.enabled", true);
        worldGuardAllowGlobalRegionOnly = ConfigurationEngine.setBoolean(
                List.of("If true, a location that is only inside WorldGuard's __global__ region is considered safe."),
                fileConfiguration, "worldGuard.allowGlobalRegionOnly", true);
        worldGuardAllowBuildAllowedRegions = ConfigurationEngine.setBoolean(
                List.of("If true, regions with an explicit build=ALLOW flag are treated as public landings."),
                fileConfiguration, "worldGuard.allowBuildAllowedRegions", true);
        worldGuardAllowPassthroughRegions = ConfigurationEngine.setBoolean(
                List.of("If true, regions with passthrough=ALLOW are treated as public landings."),
                fileConfiguration, "worldGuard.allowPassthroughRegions", true);

        townyEnabled = ConfigurationEngine.setBoolean(
                List.of("Enables Towny landing checks."),
                fileConfiguration, "towny.enabled", true);
        townyAllowWilderness = ConfigurationEngine.setBoolean(
                List.of("If true, Towny wilderness is considered safe."),
                fileConfiguration, "towny.allowWilderness", true);
        townyAllowNationZones = ConfigurationEngine.setBoolean(
                List.of("If true, Towny nation zones are considered safe."),
                fileConfiguration, "towny.allowNationZones", false);
        townyAllowClaimedTownBlocks = ConfigurationEngine.setBoolean(
                List.of("If true, CannonRTP may land players inside claimed Towny town blocks."),
                fileConfiguration, "towny.allowClaimedTownBlocks", false);

        landsEnabled = ConfigurationEngine.setBoolean(
                List.of("Enables Lands landing checks."),
                fileConfiguration, "lands.enabled", true);
        landsAllowUnclaimedAreas = ConfigurationEngine.setBoolean(
                List.of("If true, unclaimed Lands areas are considered safe."),
                fileConfiguration, "lands.allowUnclaimedAreas", true);
        landsAllowClaimedAreas = ConfigurationEngine.setBoolean(
                List.of("If true, CannonRTP may land players inside claimed Lands areas."),
                fileConfiguration, "lands.allowClaimedAreas", false);

        griefPreventionEnabled = ConfigurationEngine.setBoolean(
                List.of("Enables GriefPrevention landing checks."),
                fileConfiguration, "griefPrevention.enabled", true);
        griefPreventionAllowWilderness = ConfigurationEngine.setBoolean(
                List.of("If true, GriefPrevention wilderness is considered safe."),
                fileConfiguration, "griefPrevention.allowWilderness", true);
        griefPreventionAllowAdminClaims = ConfigurationEngine.setBoolean(
                List.of("If true, CannonRTP may land players inside GriefPrevention admin claims."),
                fileConfiguration, "griefPrevention.allowAdminClaims", false);
        griefPreventionAllowPlayerClaims = ConfigurationEngine.setBoolean(
                List.of("If true, CannonRTP may land players inside regular GriefPrevention claims."),
                fileConfiguration, "griefPrevention.allowPlayerClaims", false);

        huskTownsEnabled = ConfigurationEngine.setBoolean(
                List.of("Enables HuskTowns landing checks."),
                fileConfiguration, "huskTowns.enabled", true);
        huskTownsAllowWilderness = ConfigurationEngine.setBoolean(
                List.of("If true, HuskTowns wilderness is considered safe."),
                fileConfiguration, "huskTowns.allowWilderness", true);
        huskTownsAllowAdminClaims = ConfigurationEngine.setBoolean(
                List.of("If true, CannonRTP may land players inside HuskTowns admin claims."),
                fileConfiguration, "huskTowns.allowAdminClaims", false);
        huskTownsAllowRegularClaims = ConfigurationEngine.setBoolean(
                List.of("If true, CannonRTP may land players inside standard HuskTowns claims."),
                fileConfiguration, "huskTowns.allowRegularClaims", false);
        huskTownsAllowFarmClaims = ConfigurationEngine.setBoolean(
                List.of("If true, CannonRTP may land players inside HuskTowns farm claims."),
                fileConfiguration, "huskTowns.allowFarmClaims", false);
        huskTownsAllowPlotClaims = ConfigurationEngine.setBoolean(
                List.of("If true, CannonRTP may land players inside HuskTowns plot claims."),
                fileConfiguration, "huskTowns.allowPlotClaims", false);

        huskClaimsEnabled = ConfigurationEngine.setBoolean(
                List.of("Enables HuskClaims landing checks."),
                fileConfiguration, "huskClaims.enabled", true);
        huskClaimsAllowWilderness = ConfigurationEngine.setBoolean(
                List.of("If true, HuskClaims wilderness is considered safe."),
                fileConfiguration, "huskClaims.allowWilderness", true);
        huskClaimsAllowAdminClaims = ConfigurationEngine.setBoolean(
                List.of("If true, CannonRTP may land players inside HuskClaims admin claims."),
                fileConfiguration, "huskClaims.allowAdminClaims", false);
        huskClaimsAllowPlayerClaims = ConfigurationEngine.setBoolean(
                List.of("If true, CannonRTP may land players inside regular HuskClaims claims."),
                fileConfiguration, "huskClaims.allowPlayerClaims", false);
    }
}
