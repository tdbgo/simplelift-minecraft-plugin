package com.playcity.simplelift.lift;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;

/** Version-neutral axis-aligned bounds kept stable across Bukkit API revisions. */
public final class ShaftBounds {

    private final double minX;
    private final double minY;
    private final double minZ;
    private final double maxX;
    private final double maxY;
    private final double maxZ;

    public ShaftBounds(double minX, double minY, double minZ,
            double maxX, double maxY, double maxZ) {
        if (maxX <= minX || maxY <= minY || maxZ <= minZ) {
            throw new IllegalArgumentException("Maximum bounds must be greater than minimum bounds");
        }
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    public double minX() {
        return minX;
    }

    public double minY() {
        return minY;
    }

    public double minZ() {
        return minZ;
    }

    public double maxX() {
        return maxX;
    }

    public double maxY() {
        return maxY;
    }

    public double maxZ() {
        return maxZ;
    }

    public boolean contains(Location location) {
        return location.getX() >= minX && location.getX() < maxX
                && location.getY() >= minY && location.getY() < maxY
                && location.getZ() >= minZ && location.getZ() < maxZ;
    }

    public boolean overlaps(ShaftBounds other) {
        return maxX > other.minX && minX < other.maxX
                && maxY > other.minY && minY < other.maxY
                && maxZ > other.minZ && minZ < other.maxZ;
    }

    public boolean overlaps(Entity entity) {
        Location location = entity.getLocation();
        double halfWidth = Math.max(0.0D, entity.getWidth()) / 2.0D;
        double entityHeight = Math.max(0.0D, entity.getHeight());
        return maxX > location.getX() - halfWidth && minX < location.getX() + halfWidth
                && maxY > location.getY() && minY < location.getY() + entityHeight
                && maxZ > location.getZ() - halfWidth && minZ < location.getZ() + halfWidth;
    }

    public boolean overlapsChunk(int chunkX, int chunkZ) {
        double chunkMinX = (double) chunkX * 16.0D;
        double chunkMinZ = (double) chunkZ * 16.0D;
        return maxX > chunkMinX && minX < chunkMinX + 16.0D
                && maxZ > chunkMinZ && minZ < chunkMinZ + 16.0D;
    }

    public Location center(World world) {
        return new Location(world,
                (minX + maxX) / 2.0D,
                (minY + maxY) / 2.0D,
                (minZ + maxZ) / 2.0D);
    }

    public double halfWidth() {
        return (maxX - minX) / 2.0D;
    }

    public double halfHeight() {
        return (maxY - minY) / 2.0D;
    }

    public double halfDepth() {
        return (maxZ - minZ) / 2.0D;
    }
}
