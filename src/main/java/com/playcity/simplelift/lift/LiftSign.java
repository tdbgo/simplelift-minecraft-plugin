package com.playcity.simplelift.lift;

import com.playcity.simplelift.config.LiftMessages;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;

public final class LiftSign {

    public static final int CURRENT_LEVEL_LINE = 0;
    public static final int CURRENT_NAME_LINE = 1;
    public static final int DESTINATION_LEVEL_LINE = 2;
    public static final int DESTINATION_NAME_LINE = 3;
    private static final char SEPARATOR = ':';

    private final Block block;
    private final BlockPosition position;

    public LiftSign(Block block) {
        if (!(block.getState() instanceof Sign)) {
            throw new IllegalArgumentException("Block is not a sign: " + block.getLocation());
        }
        this.block = block;
        this.position = BlockPosition.of(block);
    }

    public Block block() {
        return block;
    }

    public BlockPosition position() {
        return position;
    }

    public boolean isSameBlock(Block other) {
        return position.equals(BlockPosition.of(other));
    }

    public boolean isBlank() {
        return line(CURRENT_LEVEL_LINE).trim().isEmpty();
    }

    public boolean isInitialized() {
        if (isBlank()) {
            return false;
        }
        try {
            return destinationLevel() > 0;
        } catch (LiftUserException exception) {
            return false;
        }
    }

    public int destinationLevel() {
        String value = line(DESTINATION_LEVEL_LINE);
        int separatorIndex = value.lastIndexOf(SEPARATOR);
        if (separatorIndex < 0 || separatorIndex == value.length() - 1) {
            throw new LiftUserException("Lift sign has no destination level.");
        }
        try {
            return Integer.parseInt(value.substring(separatorIndex + 1).trim());
        } catch (NumberFormatException exception) {
            throw new LiftUserException("Lift sign has an invalid destination level.");
        }
    }

    public String currentName() {
        return line(CURRENT_NAME_LINE);
    }

    public void update(LiftMessages messages, LiftFloor current, LiftFloor destination) {
        Sign sign = currentSign();
        setLine(sign, CURRENT_LEVEL_LINE, messages.currentFloor() + ": " + current.level());
        setLine(sign, CURRENT_NAME_LINE, current.name());
        setLine(sign, DESTINATION_LEVEL_LINE, messages.destination() + ": " + destination.level());
        setLine(sign, DESTINATION_NAME_LINE, destination.name());
        if (!sign.update(true, false)) {
            throw new LiftUserException("Could not update the lift sign at " + block.getLocation());
        }
    }

    @SuppressWarnings("deprecation")
    public String line(int index) {
        return currentSign().getLine(index);
    }

    @SuppressWarnings("deprecation")
    private static void setLine(Sign sign, int index, String value) {
        sign.setLine(index, value == null ? "" : value);
    }

    private Sign currentSign() {
        BlockState state = block.getState();
        if (!(state instanceof Sign)) {
            throw new LiftUserException("Lift sign no longer exists at " + block.getLocation());
        }
        return (Sign) state;
    }
}
