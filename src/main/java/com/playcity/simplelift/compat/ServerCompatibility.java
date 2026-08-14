package com.playcity.simplelift.compat;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Switch;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.Plugin;

/** Small reflective bridge for API points that changed across the supported releases. */
public final class ServerCompatibility {

    private static final Method WORLD_MIN_HEIGHT = findMethod(World.class, "getMinHeight");
    private static final Method ADD_CHUNK_TICKET =
            findMethod(Chunk.class, "addPluginChunkTicket", Plugin.class);
    private static final Method REMOVE_CHUNK_TICKET =
            findMethod(Chunk.class, "removePluginChunkTicket", Plugin.class);
    private static final Method CANCEL_CHUNK_UNLOAD =
            findMethod(ChunkUnloadEvent.class, "setCancelled", boolean.class);

    private ServerCompatibility() {
    }

    public static int minimumHeight(World world) {
        if (WORLD_MIN_HEIGHT == null) {
            return 0;
        }
        try {
            return ((Integer) WORLD_MIN_HEIGHT.invoke(world)).intValue();
        } catch (IllegalAccessException exception) {
            return 0;
        } catch (InvocationTargetException exception) {
            return 0;
        }
    }

    public static Block supportBlock(Block button) {
        BlockData data = button.getBlockData();
        if (!(data instanceof Switch)) {
            return button;
        }
        Switch buttonData = (Switch) data;
        switch (buttonData.getFace()) {
            case FLOOR:
                return button.getRelative(BlockFace.DOWN);
            case CEILING:
                return button.getRelative(BlockFace.UP);
            case WALL:
                return button.getRelative(buttonData.getFacing().getOppositeFace());
            default:
                return button;
        }
    }

    public static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer", false,
                    ServerCompatibility.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException exception) {
            return false;
        }
    }

    public static boolean addChunkTicket(Chunk chunk, Plugin plugin) {
        return invokeCompatibilityMethod(ADD_CHUNK_TICKET, chunk, plugin);
    }

    public static void removeChunkTicket(Chunk chunk, Plugin plugin) {
        invokeCompatibilityMethod(REMOVE_CHUNK_TICKET, chunk, plugin);
    }

    public static boolean cancelChunkUnload(ChunkUnloadEvent event) {
        return invokeCompatibilityMethod(CANCEL_CHUNK_UNLOAD, event, Boolean.TRUE);
    }

    private static Method findMethod(Class<?> owner, String name, Class<?>... parameterTypes) {
        try {
            return owner.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException exception) {
            return null;
        }
    }

    private static boolean invokeCompatibilityMethod(Method method, Object receiver, Object... arguments) {
        if (method == null) {
            return false;
        }
        try {
            Object result = method.invoke(receiver, arguments);
            return !(result instanceof Boolean) || ((Boolean) result).booleanValue();
        } catch (IllegalAccessException exception) {
            return false;
        } catch (InvocationTargetException exception) {
            return false;
        }
    }
}
