package com.playcity.simplelift.lift;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class LiftFloor {

    private final int level;
    private final String name;
    private final int buttonY;
    private final List<LiftSign> signs;

    public LiftFloor(int level, String name, int buttonY, List<LiftSign> signs) {
        this.level = level;
        this.name = name == null ? "" : name;
        this.buttonY = buttonY;
        this.signs = Collections.unmodifiableList(new ArrayList<LiftSign>(signs));
    }

    public int level() {
        return level;
    }

    public String name() {
        return name;
    }

    public int buttonY() {
        return buttonY;
    }

    public List<LiftSign> signs() {
        return signs;
    }

    public int floorY() {
        return buttonY - 2;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof LiftFloor)) {
            return false;
        }
        LiftFloor floor = (LiftFloor) other;
        return level == floor.level && buttonY == floor.buttonY
                && name.equals(floor.name) && signs.equals(floor.signs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Integer.valueOf(level), name, Integer.valueOf(buttonY), signs);
    }
}
