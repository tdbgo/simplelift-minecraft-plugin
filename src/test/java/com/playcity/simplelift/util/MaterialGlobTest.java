package com.playcity.simplelift.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MaterialGlobTest {

    @Test
    void matchesLegacySuffixPatterns() {
        assertTrue(MaterialGlob.matches("*BUTTON", "OAK_BUTTON"));
        assertTrue(MaterialGlob.matches("*_STAINED_GLASS", "LIGHT_BLUE_STAINED_GLASS"));
        assertFalse(MaterialGlob.matches("*BUTTON", "OAK_PRESSURE_PLATE"));
    }

    @Test
    void treatsRegexCharactersAsLiterals() {
        assertFalse(MaterialGlob.matches("IRON.BLOCK", "IRON_BLOCK"));
        assertTrue(MaterialGlob.matches("IRON_BLOCK", "IRON_BLOCK"));
    }

    @Test
    void matchingIsCaseInsensitive() {
        assertTrue(MaterialGlob.matches("glass", "GLASS"));
    }

    @Test
    void rejectsBlankPatterns() {
        assertThrows(IllegalArgumentException.class, () -> MaterialGlob.compile("  "));
    }
}
