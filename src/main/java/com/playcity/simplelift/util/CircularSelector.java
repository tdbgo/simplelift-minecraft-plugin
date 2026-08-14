package com.playcity.simplelift.util;

public final class CircularSelector {

    private CircularSelector() {
    }

    public static int nextIndex(int size, int selectedIndex, int excludedIndex, boolean forward) {
        if (size < 2) {
            throw new IllegalArgumentException("At least two entries are required");
        }
        if (selectedIndex < 0 || selectedIndex >= size || excludedIndex < 0 || excludedIndex >= size) {
            throw new IndexOutOfBoundsException("Selected and excluded indices must be within the list");
        }

        int index = selectedIndex;
        for (int attempts = 0; attempts < size; attempts++) {
            index = Math.floorMod(index + (forward ? 1 : -1), size);
            if (index != excludedIndex) {
                return index;
            }
        }
        throw new IllegalStateException("No selectable entry exists");
    }

    public static int advanceIndex(int size, int selectedIndex, int excludedIndex,
            boolean forward, int steps) {
        if (steps < 1) {
            throw new IllegalArgumentException("Steps must be at least one");
        }
        int index = selectedIndex;
        for (int step = 0; step < steps; step++) {
            index = nextIndex(size, index, excludedIndex, forward);
        }
        return index;
    }
}
