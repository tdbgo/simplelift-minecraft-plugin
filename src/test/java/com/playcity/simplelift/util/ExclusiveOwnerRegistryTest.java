package com.playcity.simplelift.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class ExclusiveOwnerRegistryTest {

    @Test
    void preventsTwoPlayersFromEditingTheSameSign() {
        ExclusiveOwnerRegistry<String> registry = new ExclusiveOwnerRegistry<>();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        assertTrue(registry.claim(first, "floor-1-sign"));
        assertFalse(registry.claim(second, "floor-1-sign"));
        assertEquals(first, registry.ownerOf("floor-1-sign"));
    }

    @Test
    void permitsPlayersToEditDifferentFloorSignsAndReleasesOwnership() {
        ExclusiveOwnerRegistry<String> registry = new ExclusiveOwnerRegistry<>();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        assertTrue(registry.claim(first, "floor-1-sign"));
        assertTrue(registry.claim(second, "floor-3-sign"));
        registry.release(first);

        assertNull(registry.ownerOf("floor-1-sign"));
        assertTrue(registry.claim(second, "floor-1-sign"));
        assertNull(registry.ownerOf("floor-3-sign"));
    }
}
