package com.playcity.simplelift.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BoundedFloodFillTest {

    @Test
    void collectsOnlyConnectedIncludedNodes() {
        Set<Integer> result = BoundedFloodFill.collect(
                0,
                4,
                value -> Arrays.asList(value - 1, value + 1),
                value -> value >= 0 && value <= 2);

        assertEquals(new LinkedHashSet<Integer>(Arrays.asList(0, 1, 2)), result);
    }

    @Test
    void acceptsAreaAtExactLimit() {
        Set<Integer> result = BoundedFloodFill.collect(
                0,
                3,
                value -> Arrays.asList(value + 1),
                value -> value <= 2);

        assertEquals(3, result.size());
    }

    @Test
    void rejectsConnectedAreaBeyondLimit() {
        BoundedFloodFill.SizeLimitExceededException exception = assertThrows(
                BoundedFloodFill.SizeLimitExceededException.class,
                () -> BoundedFloodFill.collect(
                        0,
                        3,
                        value -> Arrays.asList(value + 1),
                        value -> value <= 3));

        assertEquals(3, exception.maximumSize());
    }
}
