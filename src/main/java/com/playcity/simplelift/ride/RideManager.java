package com.playcity.simplelift.ride;

import com.playcity.simplelift.lift.LiftFloor;
import com.playcity.simplelift.compat.ArrivalChimes;
import com.playcity.simplelift.lift.ShaftBounds;
import com.playcity.simplelift.lift.LiftStructure;
import com.playcity.simplelift.lift.LiftUserException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class RideManager {

    private final JavaPlugin plugin;
    private final ArrivalChimes chimes;
    private final Set<ActiveRide> activeRides = new LinkedHashSet<>();
    private final Map<UUID, Location> pendingSafeTeleports = new LinkedHashMap<>();

    public RideManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.chimes = new ArrivalChimes(plugin);
    }

    public boolean start(LiftStructure structure) {
        LiftFloor destination = structure.resolveDestination();
        ShaftBounds requested = structure.shaftBounds();
        boolean overlaps = activeRides.stream()
                .anyMatch(ride -> ride.structure().world().equals(structure.world())
                        && ride.structure().shaftBounds().overlaps(requested));
        if (overlaps) {
            throw new LiftUserException(structure.settings().messages().busy());
        }

        ActiveRide ride = new ActiveRide(plugin, structure, destination, activeRides::remove, chimes);
        if (!ride.begin()) {
            return false;
        }
        activeRides.add(ride);
        return true;
    }

    public boolean hasActiveRides() {
        return !activeRides.isEmpty();
    }

    public boolean isParticipant(Entity entity) {
        return activeRides.stream().anyMatch(ride -> ride.contains(entity));
    }

    public boolean isInsideActive(Location location) {
        if (location.getWorld() == null) {
            return false;
        }
        return activeRides.stream().anyMatch(ride -> ride.structure().world().equals(location.getWorld())
                && ride.structure().shaftBounds().contains(location));
    }

    public boolean isActiveChunk(Chunk chunk) {
        return activeRides.stream().anyMatch(ride -> ride.structure().world().equals(chunk.getWorld())
                && ride.structure().shaftBounds().overlapsChunk(chunk.getX(), chunk.getZ()));
    }

    public void handleQuit(Player player) {
        chimes.cancel(player.getUniqueId());
        for (ActiveRide ride : new ArrayList<ActiveRide>(activeRides)) {
            if (!ride.contains(player)) {
                continue;
            }
            Location safe = ride.structure().centerAt(ride.structure().floors().get(0));
            pendingSafeTeleports.put(player.getUniqueId(), safe);
            player.teleport(safe, PlayerTeleportEvent.TeleportCause.PLUGIN);
            player.setFallDistance(0.0F);
            ride.detach(player);
        }
    }

    public void handleJoin(Player player) {
        Location destination = pendingSafeTeleports.remove(player.getUniqueId());
        if (destination == null || destination.getWorld() == null) {
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                player.teleport(destination, PlayerTeleportEvent.TeleportCause.PLUGIN);
                player.setFallDistance(0.0F);
            }
        });
    }

    public void detach(Entity entity) {
        for (ActiveRide ride : new ArrayList<ActiveRide>(activeRides)) {
            if (ride.contains(entity)) {
                ride.detach(entity);
            }
        }
    }

    public void stopWorld(World world) {
        for (ActiveRide ride : new ArrayList<ActiveRide>(activeRides)) {
            if (ride.structure().world().equals(world)) {
                ride.finish(false);
            }
        }
    }

    public void shutdown() {
        chimes.cancelAll();
        for (ActiveRide ride : new ArrayList<>(activeRides)) {
            ride.finish(false);
        }
        activeRides.clear();
        pendingSafeTeleports.clear();
    }

    public void cancelChimes() { chimes.cancelAll(); }
}
