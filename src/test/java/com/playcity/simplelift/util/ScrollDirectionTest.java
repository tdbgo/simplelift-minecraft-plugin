package com.playcity.simplelift.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ScrollDirectionTest {

    @Test
    void normalSlotIncreaseMovesForward() {
        assertTrue(ScrollDirection.isForward(4, 3));
    }

    @Test
    void normalSlotDecreaseMovesBackward() {
        assertFalse(ScrollDirection.isForward(3, 4));
    }

    @Test
    void hotbarWrapFromEightToZeroMovesForward() {
        assertTrue(ScrollDirection.isForward(0, 8));
    }

    @Test
    void hotbarWrapFromZeroToEightMovesBackward() {
        assertFalse(ScrollDirection.isForward(8, 0));
    }
}
