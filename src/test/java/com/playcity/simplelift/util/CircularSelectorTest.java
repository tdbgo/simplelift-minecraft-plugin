package com.playcity.simplelift.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class CircularSelectorTest {

    @Test
    void wrapsForwardAndSkipsExcludedFloor() {
        assertEquals(1, CircularSelector.nextIndex(3, 2, 0, true));
    }

    @Test
    void wrapsBackwardAndSkipsExcludedFloor() {
        assertEquals(1, CircularSelector.nextIndex(3, 0, 2, false));
    }

    @Test
    void topFloorInitializationWrapsToBottomFloor() {
        assertEquals(0, CircularSelector.nextIndex(2, 1, 1, true));
    }

    @Test
    void rejectsSingleFloorLift() {
        assertThrows(IllegalArgumentException.class,
                () -> CircularSelector.nextIndex(1, 0, 0, true));
    }

    @Test
    void advancesSeveralFloorsWithoutSelectingCurrentFloor() {
        assertEquals(2, CircularSelector.advanceIndex(6, 5, 0, true, 2));
        assertEquals(4, CircularSelector.advanceIndex(6, 1, 0, false, 2));
    }

    @Test
    void rejectsZeroStepAdvance() {
        assertThrows(IllegalArgumentException.class,
                () -> CircularSelector.advanceIndex(3, 1, 0, true, 0));
    }
}
