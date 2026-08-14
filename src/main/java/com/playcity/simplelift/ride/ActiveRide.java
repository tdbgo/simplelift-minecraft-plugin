package com.playcity.simplelift.ride;

import com.playcity.simplelift.compat.SoundEffects;
import com.playcity.simplelift.compat.ServerCompatibility;
import com.playcity.simplelift.config.ConfigLoader;
import com.playcity.simplelift.config.LiftSettings;
import com.playcity.simplelift.lift.BlockPosition;
import com.playcity.simplelift.lift.LiftFloor;
import com.playcity.simplelift.lift.ShaftBounds;
import com.playcity.simplelift.lift.LiftStructure;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

final class ActiveRide {

    private final JavaPlugin plugin;
    private final LiftStructure structure;
    private final LiftFloor destination;
    private final LiftSettings settings;
    private final Consumer<ActiveRide> onFinished;
    private final Map<UUID, Entity> passengers = new LinkedHashMap<>();
    private final Map<UUID, Entity> freezers = new LinkedHashMap<>();
    private final Map<UUID, EntityPhysics> physics = new LinkedHashMap<>();
    private final Map<BlockPosition, BlockState> removedBlocks = new LinkedHashMap<>();
    private final Set<Chunk> retainedChunks = new LinkedHashSet<Chunk>();
    private BukkitTask task;
    private long maxEndTime;
    private int taskRuns;
    private boolean finishing;

    ActiveRide(JavaPlugin plugin, LiftStructure structure, LiftFloor destination, Consumer<ActiveRide> onFinished) {
        this.plugin = plugin;
        this.structure = structure;
        this.destination = destination;
        this.settings = structure.settings();
        this.onFinished = onFinished;
    }

    boolean begin() {
        ShaftBounds bounds = structure.shaftBounds();
        Collection<Entity> candidates = structure.world().getNearbyEntities(
                bounds.center(structure.world()),
                bounds.halfWidth() + 1.0D,
                bounds.halfHeight() + 1.0D,
                bounds.halfDepth() + 1.0D);
        List<Entity> nearby = new ArrayList<Entity>();
        for (Entity entity : candidates) {
            if (entity.isValid() && bounds.overlaps(entity)
                    && (settings.liftMobs() || entity instanceof Player)) {
                nearby.add(entity);
            }
        }
        for (Entity entity : nearby) {
            if (isOnCurrentFloor(entity)) {
                passengers.put(entity.getUniqueId(), entity);
            } else if (isOnTraversedFloor(entity)) {
                // Occupants waiting on a floor that will be removed temporarily must
                // not fall. Floors outside this route are left completely untouched.
                freezers.put(entity.getUniqueId(), entity);
            }
        }
        if (passengers.isEmpty()) {
            return false;
        }

        try {
            if (settings.autoPlace()) {
                teleportRoots(passengers.values(), structure.centerAt(structure.currentFloor()));
            }
            retainShaftChunks();
            // Prepare entities before removing a source/intermediate floor. On
            // legacy servers, an armor stand can otherwise take support-loss damage in
            // the same tick and become invalid before the first ride task.
            involved().forEach(entity -> physics.put(
                    entity.getUniqueId(), EntityPhysics.captureAndPrepare(entity)));
            removeTraversedFloors();
            removeRailsUnderMinecarts();

            long travelMillis = (long) (Math.abs(structure.currentFloor().floorY() - destination.floorY())
                    * 50.0D / structure.speed());
            maxEndTime = System.currentTimeMillis() + travelMillis + settings.secondsUntilTimeout() * 1000L;
            task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 0L, 2L);
            ConfigLoader.debug(plugin.getLogger(), settings,
                    "Started lift with " + passengers.size() + " passengers and "
                            + freezers.size() + " protected waiting occupants toward floor "
                            + destination.level());
            return true;
        } catch (RuntimeException exception) {
            finish(false);
            throw exception;
        }
    }

    LiftStructure structure() {
        return structure;
    }

    boolean contains(Entity entity) {
        UUID uuid = entity.getUniqueId();
        return passengers.containsKey(uuid) || freezers.containsKey(uuid);
    }

    void detach(Entity entity) {
        UUID uuid = entity.getUniqueId();
        passengers.remove(uuid);
        freezers.remove(uuid);
        EntityPhysics state = physics.remove(uuid);
        if (state != null) {
            state.restore();
        }
        if (passengers.isEmpty()) {
            finish(true);
        }
    }

    void finish(boolean arrivalSound) {
        if (finishing) {
            return;
        }
        finishing = true;
        if (task != null) {
            task.cancel();
        }

        if (!arrivalSound && !passengers.isEmpty() && (!physics.isEmpty() || !removedBlocks.isEmpty())) {
            teleportRoots(passengers.values(), structure.centerAt(destination));
        }
        restoreBlocks();
        physics.values().forEach(EntityPhysics::restore);
        if (arrivalSound && settings.soundEnabled()) {
            involved().filter(Player.class::isInstance).map(Player.class::cast).forEach(player ->
                    SoundEffects.arrival(player, settings.relativeVolume(1.0D), 0.5F));
        }
        passengers.clear();
        freezers.clear();
        physics.clear();
        releaseShaftChunks();
        onFinished.accept(this);
        ConfigLoader.debug(plugin.getLogger(), settings, "Lift finished and restored its temporary state");
    }

    private void tick() {
        if (finishing) {
            return;
        }
        try {
            dropInvalidEntities();
            if (passengers.isEmpty()) {
                finish(true);
                return;
            }
            if (System.currentTimeMillis() > maxEndTime) {
                timeout();
                finish(true);
                return;
            }

            List<Entity> arrived = new ArrayList<Entity>();
            for (Entity entity : passengers.values()) {
                if (reachedDestination(entity)) {
                    arrived.add(entity);
                }
            }
            for (Entity entity : arrived) {
                passengers.remove(entity.getUniqueId());
                freezers.put(entity.getUniqueId(), entity);
            }
            handleLeavers();
            if (passengers.isEmpty()) {
                finish(true);
                return;
            }

            double velocity = destination.level() > structure.currentFloor().level()
                    ? structure.speed() : -structure.speed();
            for (Entity entity : passengers.values()) {
                if (!entity.isInsideVehicle()) {
                    entity.setVelocity(new Vector(0.0D, velocity, 0.0D));
                    entity.setFallDistance(0.0F);
                }
            }
            for (Entity entity : freezers.values()) {
                if (!entity.isInsideVehicle()) {
                    entity.setVelocity(new Vector());
                    entity.setFallDistance(0.0F);
                }
            }
            playMovementSound();
            taskRuns++;
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.SEVERE, "Lift task failed; restoring temporary state", exception);
            finish(false);
        }
    }

    private void timeout() {
        teleportRoots(passengers.values(), structure.centerAt(destination));
        for (Entity entity : passengers.values()) {
            if (entity instanceof Player) {
                ((Player) entity).sendMessage(settings.messages().timeout());
            }
            freezers.put(entity.getUniqueId(), entity);
        }
        passengers.clear();
    }

    private void handleLeavers() {
        List<Entity> leavers = new ArrayList<Entity>();
        for (Entity entity : passengers.values()) {
            if (!structure.shaftBounds().overlaps(entity)) {
                leavers.add(entity);
            }
        }
        if (leavers.isEmpty()) {
            return;
        }
        if (!settings.preventLeave()) {
            leavers.forEach(this::detach);
            return;
        }

        ShaftBounds bounds = structure.shaftBounds();
        double centerX = (bounds.minX() + bounds.maxX()) / 2.0D;
        double centerZ = (bounds.minZ() + bounds.maxZ()) / 2.0D;
        for (Entity entity : leavers) {
            if (entity.isInsideVehicle()) {
                continue;
            }
            Location location = entity.getLocation();
            location.setX(centerX);
            location.setZ(centerZ);
            entity.teleport(location, PlayerTeleportEvent.TeleportCause.PLUGIN);
            if (entity instanceof Player) {
                ((Player) entity).sendMessage(settings.messages().cantLeave());
            }
        }
    }

    private boolean reachedDestination(Entity entity) {
        if (destination.level() > structure.currentFloor().level()) {
            return entity.getLocation().getY() > destination.floorY() + 1.0D;
        }
        return entity.getLocation().getY() < destination.buttonY();
    }

    private boolean isOnCurrentFloor(Entity entity) {
        return isOnFloor(entity, structure.currentFloor());
    }

    private boolean isOnTraversedFloor(Entity entity) {
        for (LiftFloor floor : structure.traversedFloors(destination)) {
            if (isOnFloor(entity, floor)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isOnFloor(Entity entity, LiftFloor floor) {
        double y = entity.getLocation().getY();
        if (entity.isInsideVehicle() && entity.getVehicle() != null) {
            y -= entity.getVehicle().getHeight();
        }
        return y >= floor.floorY() && y <= floor.floorY() + 2.0D;
    }

    private void removeTraversedFloors() {
        for (Block base : structure.baseBlocks()) {
            for (LiftFloor floor : structure.traversedFloors(destination)) {
                Block floorBlock = structure.world().getBlockAt(base.getX(), floor.floorY(), base.getZ());
                if (!settings.isFloor(floorBlock)) {
                    continue;
                }
                Block above = floorBlock.getRelative(BlockFace.UP);
                if (!above.isEmpty() && !settings.isSign(above)) {
                    snapshotAndClear(above);
                }
                snapshotAndClear(floorBlock);
            }
        }
    }

    private void removeRailsUnderMinecarts() {
        passengers.values().stream()
                .filter(Minecart.class::isInstance)
                .map(Entity::getLocation)
                .map(Location::getBlock)
                .filter(ActiveRide::isRail)
                .forEach(this::snapshotAndClear);
    }

    private void snapshotAndClear(Block block) {
        removedBlocks.putIfAbsent(BlockPosition.of(block), block.getState());
        block.setType(Material.AIR, false);
    }

    private void restoreBlocks() {
        removedBlocks.values().stream()
                .sorted(Comparator.comparingInt(BlockState::getY))
                .forEach(state -> {
                    try {
                        if (!state.update(true, false)) {
                            plugin.getLogger().warning("Could not restore lift block at " + state.getLocation());
                        }
                    } catch (RuntimeException exception) {
                        plugin.getLogger().log(Level.SEVERE,
                                "Could not restore lift block at " + state.getLocation(), exception);
                    }
                });
        removedBlocks.clear();
    }

    private void retainShaftChunks() {
        for (Block base : structure.baseBlocks()) {
            Chunk chunk = base.getChunk();
            if (retainedChunks.add(chunk)) {
                chunk.load();
                ServerCompatibility.addChunkTicket(chunk, plugin);
            }
        }
    }

    private void releaseShaftChunks() {
        for (Chunk chunk : retainedChunks) {
            ServerCompatibility.removeChunkTicket(chunk, plugin);
        }
        retainedChunks.clear();
    }

    private void dropInvalidEntities() {
        List<Entity> invalid = new ArrayList<Entity>();
        involved().filter(entity -> !entity.isValid() || !Objects.equals(entity.getWorld(), structure.world()))
                .forEach(invalid::add);
        for (Entity entity : invalid) {
            ConfigLoader.debug(plugin.getLogger(), settings,
                    "Detached invalid lift entity " + entity.getType() + " at " + entity.getLocation()
                            + " (valid=" + entity.isValid() + ", dead=" + entity.isDead() + ")");
            passengers.remove(entity.getUniqueId());
            freezers.remove(entity.getUniqueId());
            EntityPhysics state = physics.remove(entity.getUniqueId());
            if (state != null) {
                state.restore();
            }
        }
    }

    private void playMovementSound() {
        if (!settings.soundEnabled() || taskRuns % 3 != 0) {
            return;
        }
        passengers.values().stream().filter(Player.class::isInstance).map(Player.class::cast).forEach(player -> {
            SoundEffects.movement(player,
                    settings.relativeVolume(0.8D), settings.relativeVolume(0.5D), 0.5F);
        });
    }

    private static boolean isRail(Block block) {
        String name = block.getType().name();
        return name.equals("RAILS") || name.endsWith("_RAIL") || name.equals("RAIL");
    }

    private java.util.stream.Stream<Entity> involved() {
        Set<Entity> result = new LinkedHashSet<>(passengers.values());
        result.addAll(freezers.values());
        return result.stream();
    }

    private static void teleportRoots(Collection<Entity> entities, Location destination) {
        Set<UUID> included = new LinkedHashSet<>();
        entities.forEach(entity -> included.add(entity.getUniqueId()));
        List<Entity> roots = new ArrayList<>();
        for (Entity entity : entities) {
            Entity vehicle = entity.getVehicle();
            if (vehicle == null || !included.contains(vehicle.getUniqueId())) {
                roots.add(entity);
            }
        }
        for (Entity root : roots) {
            Location target = destination.clone();
            target.setYaw(root.getLocation().getYaw());
            target.setPitch(root.getLocation().getPitch());
            root.teleport(target, PlayerTeleportEvent.TeleportCause.PLUGIN);
        }
    }
}
