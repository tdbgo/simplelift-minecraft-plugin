package com.playcity.simplelift.lift;

import java.util.Objects;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

public final class BlockPosition {

    private final UUID worldId;
    private final int x;
    private final int y;
    private final int z;

    public BlockPosition(UUID worldId, int x, int y, int z) {
        this.worldId = Objects.requireNonNull(worldId, "worldId");
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public UUID worldId() {
        return worldId;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int z() {
        return z;
    }

    public static BlockPosition of(Block block) {
        return new BlockPosition(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
    }

    public static BlockPosition of(Location location) {
        World world = location.getWorld();
        if (world == null) {
            throw new IllegalArgumentException("Location has no world");
        }
        return new BlockPosition(world.getUID(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BlockPosition)) {
            return false;
        }
        BlockPosition position = (BlockPosition) other;
        return x == position.x && y == position.y && z == position.z
                && worldId.equals(position.worldId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(worldId, Integer.valueOf(x), Integer.valueOf(y), Integer.valueOf(z));
    }

    @Override
    public String toString() {
        return worldId + ":" + x + "," + y + "," + z;
    }
}
