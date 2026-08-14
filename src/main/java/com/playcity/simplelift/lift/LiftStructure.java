package com.playcity.simplelift.lift;

import com.playcity.simplelift.config.LiftSettings;
import com.playcity.simplelift.util.CircularSelector;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

public final class LiftStructure {

    private final LiftSettings settings;
    private final Set<Block> baseBlocks;
    private final List<LiftFloor> floors;
    private final LiftFloor currentFloor;
    private final LiftSign initialSign;
    private final double speed;
    private final ShaftBounds shaftBounds;

    public LiftStructure(LiftSettings settings, Set<Block> baseBlocks, List<LiftFloor> floors,
            LiftFloor currentFloor, LiftSign initialSign) {
        this.settings = settings;
        this.baseBlocks = Collections.unmodifiableSet(new LinkedHashSet<Block>(baseBlocks));
        this.floors = Collections.unmodifiableList(new ArrayList<LiftFloor>(floors));
        this.currentFloor = currentFloor;
        this.initialSign = initialSign;
        this.speed = settings.speedOf(this.baseBlocks.iterator().next());
        this.shaftBounds = calculateBounds();
    }

    public LiftSettings settings() {
        return settings;
    }

    public Set<Block> baseBlocks() {
        return baseBlocks;
    }

    public List<LiftFloor> floors() {
        return floors;
    }

    public LiftFloor currentFloor() {
        return currentFloor;
    }

    public LiftSign initialSign() {
        return initialSign;
    }

    public double speed() {
        return speed;
    }

    public ShaftBounds shaftBounds() {
        return shaftBounds;
    }

    public World world() {
        return baseBlocks.iterator().next().getWorld();
    }

    public LiftFloor resolveDestination() {
        int level = initialSign.destinationLevel();
        LiftFloor destination = floorByLevel(level).orElseGet(() -> nextFloor(currentFloor, true));
        if (destination.level() == currentFloor.level()) {
            throw new LiftUserException(settings.messages().floorNotExists());
        }
        return destination;
    }

    public LiftFloor cycleDestination(LiftSign sign, boolean forward) {
        return cycleDestination(sign, forward, 1);
    }

    public LiftFloor cycleDestination(LiftSign sign, boolean forward, int steps) {
        LiftFloor signFloor = floorContaining(sign)
                .orElseThrow(() -> new LiftUserException("This sign no longer belongs to the scanned lift."));
        LiftFloor selected = floorByLevelSafely(sign.destinationLevel()).orElse(signFloor);
        LiftFloor next = advanceFloor(selected, signFloor, forward, steps);
        sign.update(settings.messages(), signFloor, next);
        return next;
    }

    public LiftFloor selectedDestination(LiftSign sign) {
        LiftFloor signFloor = floorContaining(sign)
                .orElseThrow(() -> new LiftUserException("This sign no longer belongs to the scanned lift."));
        return floorByLevelSafely(sign.destinationLevel())
                .filter(floor -> floor.level() != signFloor.level())
                .orElseGet(() -> nextFloor(signFloor, signFloor, true));
    }

    public void initializeInvalidSigns() {
        for (LiftFloor floor : floors) {
            boolean invalid = floor.signs().stream().anyMatch(sign -> !sign.isInitialized());
            if (!invalid) {
                continue;
            }
            LiftFloor destination = nextFloor(floor, floor, true);
            for (LiftSign sign : floor.signs()) {
                sign.update(settings.messages(), floor, destination);
            }
        }
    }

    public Location centerAt(LiftFloor floor) {
        return new Location(world(),
                (shaftBounds.minX() + shaftBounds.maxX()) / 2.0D,
                floor.floorY() + 1.5D,
                (shaftBounds.minZ() + shaftBounds.maxZ()) / 2.0D);
    }

    public List<LiftFloor> traversedFloors(LiftFloor destination) {
        List<LiftFloor> result = new ArrayList<>();
        boolean up = destination.level() > currentFloor.level();
        for (LiftFloor floor : floors) {
            if (up && floor.level() > currentFloor.level() && floor.level() <= destination.level()) {
                result.add(floor);
            } else if (!up && floor.level() <= currentFloor.level() && floor.level() > destination.level()) {
                result.add(floor);
            }
        }
        return result;
    }

    private Optional<LiftFloor> floorContaining(LiftSign sign) {
        return floors.stream()
                .filter(floor -> floor.signs().stream().anyMatch(candidate -> candidate.position().equals(sign.position())))
                .findFirst();
    }

    private Optional<LiftFloor> floorByLevelSafely(int level) {
        return floorByLevel(level);
    }

    private Optional<LiftFloor> floorByLevel(int level) {
        return floors.stream().filter(floor -> floor.level() == level).findFirst();
    }

    private LiftFloor nextFloor(LiftFloor selected, boolean forward) {
        return nextFloor(selected, currentFloor, forward);
    }

    private LiftFloor nextFloor(LiftFloor selected, LiftFloor excluded, boolean forward) {
        return advanceFloor(selected, excluded, forward, 1);
    }

    private LiftFloor advanceFloor(LiftFloor selected, LiftFloor excluded, boolean forward, int steps) {
        if (floors.size() < 2) {
            throw new LiftUserException(settings.messages().oneFloor());
        }
        int index = floors.indexOf(selected);
        if (index < 0) {
            index = floors.indexOf(excluded);
        }
        int excludedIndex = floors.indexOf(excluded);
        int nextIndex = CircularSelector.advanceIndex(
                floors.size(), index, excludedIndex, forward, steps);
        return floors.get(nextIndex);
    }

    private ShaftBounds calculateBounds() {
        Block first = baseBlocks.iterator().next();
        int minX = first.getX();
        int maxX = first.getX();
        int minZ = first.getZ();
        int maxZ = first.getZ();
        for (Block block : baseBlocks) {
            minX = Math.min(minX, block.getX());
            maxX = Math.max(maxX, block.getX());
            minZ = Math.min(minZ, block.getZ());
            maxZ = Math.max(maxZ, block.getZ());
        }
        int baseY = baseBlocks.iterator().next().getY();
        int topY = baseY;
        for (LiftFloor floor : floors) {
            topY = Math.max(topY, floor.buttonY());
        }
        return new ShaftBounds(minX, baseY, minZ, maxX + 1.0D, topY + 2.0D, maxZ + 1.0D);
    }
}
