package com.playcity.simplelift.lift;

import com.playcity.simplelift.compat.ServerCompatibility;
import com.playcity.simplelift.config.ConfigLoader;
import com.playcity.simplelift.config.LiftSettings;
import com.playcity.simplelift.util.BoundedFloodFill;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;

public final class LiftScanner {

    private static final List<BlockFace> HORIZONTAL_FACES =
            Collections.unmodifiableList(Arrays.asList(
                    BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST));

    private final Supplier<LiftSettings> settingsSupplier;
    private final Logger logger;

    public LiftScanner(Supplier<LiftSettings> settingsSupplier, Logger logger) {
        this.settingsSupplier = settingsSupplier;
        this.logger = logger;
    }

    public boolean isValidButtonStructure(Block button) {
        LiftSettings settings = settingsSupplier.get();
        Block floor = button.getRelative(BlockFace.DOWN, 2);
        return settings.isButton(button)
                && settings.isSign(button.getRelative(BlockFace.UP))
                && (settings.isFloor(floor) || settings.isBase(floor));
    }

    public Optional<LiftStructure> scan(Block button) {
        requireServerThread();
        LiftSettings settings = settingsSupplier.get();
        if (!isValidButtonStructure(button)) {
            return Optional.empty();
        }

        Set<Block> baseBlocks = findBase(button, settings);
        if (baseBlocks.isEmpty()) {
            return Optional.empty();
        }

        List<LiftFloor> floors = findFloors(baseBlocks, settings);
        if (floors.size() < 2) {
            throw new LiftUserException(settings.messages().oneFloor());
        }
        LiftFloor currentFloor = floors.stream()
                .filter(floor -> floor.buttonY() == button.getY())
                .findFirst()
                .orElseThrow(() -> new LiftUserException(
                        settings.messages().text("floorNotInLift")));
        LiftSign initialSign = currentFloor.signs().stream()
                .filter(sign -> sign.block().getX() == button.getX() && sign.block().getZ() == button.getZ())
                .findFirst()
                .orElseThrow(() -> new LiftUserException(settings.messages().text("signNotFound")));

        LiftStructure structure = new LiftStructure(settings, baseBlocks, floors, currentFloor, initialSign);
        structure.initializeInvalidSigns();
        ConfigLoader.debug(logger, settings,
                "Scanned " + baseBlocks.size() + " base blocks and " + floors.size() + " floors at "
                        + button.getLocation());
        return Optional.of(structure);
    }

    private Set<Block> findBase(Block start, LiftSettings settings) {
        World world = start.getWorld();
        int minimum = Math.max(settings.minHeight(), ServerCompatibility.minimumHeight(world));
        for (int y = start.getY(); y >= minimum; y--) {
            Block block = world.getBlockAt(start.getX(), y, start.getZ());
            if (settings.isBase(block)) {
                return connectedBase(block, settings);
            }
            if (!settings.isAllowedShaftBlock(block)) {
                return Collections.emptySet();
            }
        }
        return Collections.emptySet();
    }

    private Set<Block> connectedBase(Block origin, LiftSettings settings) {
        Material type = origin.getType();
        try {
            return BoundedFloodFill.collect(
                    origin,
                    settings.maxLiftArea(),
                    block -> {
                        List<Block> neighbors = new ArrayList<Block>(HORIZONTAL_FACES.size());
                        for (BlockFace face : HORIZONTAL_FACES) {
                            neighbors.add(block.getRelative(face));
                        }
                        return neighbors;
                    },
                    block -> block.getType() == type);
        } catch (BoundedFloodFill.SizeLimitExceededException exception) {
            throw new LiftUserException(
                    settings.messages().text("footprintExceeded").replace("{max}", Integer.toString(exception.maximumSize())));
        }
    }

    private List<LiftFloor> findFloors(Set<Block> baseBlocks, LiftSettings settings) {
        Block firstBase = baseBlocks.iterator().next();
        World world = firstBase.getWorld();
        int maximum = Math.min(settings.maxHeight(), world.getMaxHeight() - 2);
        List<LiftFloor> floors = new ArrayList<>();

        for (int y = firstBase.getY() + 1; y <= maximum; y++) {
            List<Block> buttons = new ArrayList<>();
            for (Block base : baseBlocks) {
                Block candidate = world.getBlockAt(base.getX(), y, base.getZ());
                if (isValidButtonStructure(candidate)) {
                    buttons.add(candidate);
                }
                if (!settings.isAllowedShaftBlock(candidate)) {
                    return floors;
                }
            }
            if (!buttons.isEmpty()) {
                floors.add(createFloor(buttons, floors.size() + 1, settings));
            }
        }
        return floors;
    }

    private LiftFloor createFloor(List<Block> buttons, int level, LiftSettings settings) {
        List<LiftSign> signs = new ArrayList<LiftSign>(buttons.size());
        for (Block button : buttons) {
            signs.add(new LiftSign(button.getRelative(BlockFace.UP)));
        }

        String fallback = signs.get(0).currentName();
        String name = fallback;
        for (Block button : buttons) {
            Block nameSign = button.getRelative(BlockFace.DOWN);
            if (settings.isSign(nameSign)) {
                name = legacyLine((Sign) nameSign.getState(), 1);
                break;
            }
        }
        return new LiftFloor(level, name, buttons.get(0).getY(), signs);
    }

    @SuppressWarnings("deprecation")
    private static String legacyLine(Sign sign, int line) {
        return sign.getLine(line);
    }

    private static void requireServerThread() {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("Lift scans must run on the server thread");
        }
    }
}
