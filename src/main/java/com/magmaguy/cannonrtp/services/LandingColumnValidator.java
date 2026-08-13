package com.magmaguy.cannonrtp.services;

import com.magmaguy.cannonrtp.config.LandingSearchConfig;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

/**
 * Validates both the landing space and every block the player crosses during
 * the vertical airdrop. The latter matters in roofed dimensions and custom
 * worlds where checking only feet/head can place the player inside terrain.
 */
final class LandingColumnValidator {
    static final int AIRDROP_HEIGHT_BLOCKS = 50;

    private LandingColumnValidator() {
    }

    static Result validate(Location landingLocation) {
        World world = landingLocation == null ? null : landingLocation.getWorld();
        if (world == null) {
            return Result.NO_SAFE_SURFACE;
        }

        int blockX = landingLocation.getBlockX();
        int blockZ = landingLocation.getBlockZ();
        int landingY = landingLocation.getBlockY();
        Block ground = world.getBlockAt(blockX, landingY - 1, blockZ);

        if (!ground.getType().isSolid() || ground.isLiquid()) {
            return Result.NO_SAFE_SURFACE;
        }
        if (LandingSearchConfig.isUnsafeGroundMaterial(ground.getType())) {
            return Result.HAZARDOUS_TERRAIN;
        }
        if (isNetherCeiling(world, ground)) {
            return Result.HAZARDOUS_TERRAIN;
        }

        long arrivalFeetY = (long) landingY + AIRDROP_HEIGHT_BLOCKS;
        long arrivalHeadY = arrivalFeetY + 1;
        if (landingY < world.getMinHeight() || arrivalHeadY >= world.getMaxHeight()) {
            return Result.NO_SAFE_SURFACE;
        }

        for (int y = landingY; y <= (int) arrivalHeadY; y++) {
            Block block = world.getBlockAt(blockX, y, blockZ);
            Material material = block.getType();
            if (!(material.isAir() || block.isPassable())) {
                return Result.NO_SAFE_SURFACE;
            }
            if (LandingSearchConfig.isUnsafeBodyMaterial(material)) {
                return Result.HAZARDOUS_TERRAIN;
            }
        }
        return Result.SAFE;
    }

    private static boolean isNetherCeiling(World world, Block ground) {
        return world.getEnvironment() == World.Environment.NETHER
                && ground.getType() == Material.BEDROCK
                && ground.getY() >= world.getLogicalHeight() - 8;
    }

    enum Result {
        SAFE,
        NO_SAFE_SURFACE,
        HAZARDOUS_TERRAIN
    }
}
