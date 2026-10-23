package com.playcity.simplelift.config;

import static org.junit.jupiter.api.Assertions.*;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class ButtonChimesTest {
    private static final java.util.Set<Material> BUTTONS = EnumSet.of(Material.OAK_BUTTON, Material.STONE_BUTTON);

    private YamlConfiguration config(String yaml) throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(yaml);
        return config;
    }

    @Test void materialOverridesTagsEvenWhenSilent() throws Exception {
        ButtonChimes policy = ConfigLoader.parseChimePolicy(config("arrivalChimes:\n  default: legacy\n  tags:\n    'minecraft:wooden_buttons': warm\n  materials:\n    OAK_BUTTON: silent\n"), BUTTONS);
        assertEquals(ButtonChimes.Profile.SILENT, policy.select("OAK_BUTTON", tag -> true));
    }

    @Test void specificTagPrecedesAllButtons() throws Exception {
        ButtonChimes policy = ConfigLoader.parseChimePolicy(config("arrivalChimes:\n  tags:\n    'minecraft:buttons': crystal\n    'minecraft:stone_buttons': clear\n"), BUTTONS);
        assertEquals(ButtonChimes.Profile.CLEAR, policy.select("STONE_BUTTON", tag -> true));
        assertEquals(ButtonChimes.Profile.CRYSTAL, policy.select("OTHER_BUTTON", tag -> tag.equals("minecraft:buttons")));
        assertEquals(ButtonChimes.Profile.LEGACY, policy.select("CUSTOM", tag -> false));
    }

    @Test void hotelMaterialOverridesGroupWithoutChangingOtherMaterials() throws Exception {
        ButtonChimes policy = ConfigLoader.parseChimePolicy(config("arrivalChimes:\n  default: legacy\n  tags:\n    'minecraft:buttons': crystal\n    'minecraft:wooden_buttons': warm\n  materials:\n    OAK_BUTTON: hotel\n"), BUTTONS);
        assertEquals(ButtonChimes.Profile.HOTEL, policy.select("OAK_BUTTON", tag -> true));
        assertEquals(ButtonChimes.Profile.WARM, policy.select("BIRCH_BUTTON", tag -> true));
        assertEquals(ButtonChimes.Profile.CRYSTAL, policy.select("STONE_BUTTON", tag -> tag.equals("minecraft:buttons")));
        assertEquals(ButtonChimes.Profile.LEGACY, policy.select("CUSTOM", tag -> false));
    }

    @Test void hotelCanBeAssignedToGroupOrDefault() throws Exception {
        ButtonChimes policy = ConfigLoader.parseChimePolicy(config("arrivalChimes:\n  default: hotel\n  tags:\n    'minecraft:wooden_buttons': hotel\n  materials: {}\n"), BUTTONS);
        assertEquals(ButtonChimes.Profile.HOTEL, policy.select("OAK_BUTTON", tag -> true));
        assertEquals(ButtonChimes.Profile.HOTEL, policy.select("STONE_BUTTON", tag -> false));
    }

    @Test void absentSectionKeepsLegacyWithoutBundledDefaults() throws Exception {
        ButtonChimes policy = ConfigLoader.parseChimePolicy(config("soundEnabled: true\n"), BUTTONS);
        assertEquals(ButtonChimes.Profile.LEGACY, policy.select("OAK_BUTTON", tag -> true));
    }

    @Test void importedConfigReceivesBundledPilotDefaults() throws Exception {
        YamlConfiguration existing = config("soundVolume: 35\n");
        try (InputStreamReader reader = new InputStreamReader(
                getClass().getResourceAsStream("/config.yml"), StandardCharsets.UTF_8)) {
            existing.setDefaults(YamlConfiguration.loadConfiguration(reader));
        }
        ButtonChimes policy = ConfigLoader.parseChimePolicy(existing, BUTTONS);
        assertEquals(ButtonChimes.Profile.WARM, policy.select("OAK_BUTTON", tag -> tag.equals("minecraft:wooden_buttons")));
        assertEquals(ButtonChimes.Profile.CRYSTAL, policy.select("STONE_BUTTON", tag -> tag.equals("minecraft:stone_buttons")));
        assertEquals(ButtonChimes.Profile.LEGACY, policy.select("CUSTOM", tag -> false));
        assertEquals(35, existing.getInt("soundVolume"));
    }

    @Test void explicitStoneProfileOverridesBundledDefault() throws Exception {
        YamlConfiguration existing = config("arrivalChimes:\n  tags:\n    'minecraft:stone_buttons': clear\n");
        try (InputStreamReader reader = new InputStreamReader(
                getClass().getResourceAsStream("/config.yml"), StandardCharsets.UTF_8)) {
            existing.setDefaults(YamlConfiguration.loadConfiguration(reader));
        }
        ButtonChimes policy = ConfigLoader.parseChimePolicy(existing, BUTTONS);
        assertEquals(ButtonChimes.Profile.CLEAR, policy.select("STONE_BUTTON", tag -> tag.equals("minecraft:stone_buttons")));
    }

    @Test void invalidConfigurationRejectsCandidate() throws Exception {
        for (String yaml : Arrays.asList("arrivalChimes: false", "arrivalChimes:\n  default: 5",
                "arrivalChimes:\n  default: typo", "arrivalChimes:\n  tags: []",
                "arrivalChimes:\n  tags:\n    'minecraft:wood': warm",
                "arrivalChimes:\n  materials:\n    OAK_BUTTON: 5",
                "arrivalChimes:\n  materials:\n    DIAMOND_BLOCK: clear",
                "arrivalChimes:\n  materials:\n    UNKNOWN_BUTTON: clear")) {
            YamlConfiguration candidate = config(yaml);
            assertThrows(LiftConfigurationException.class, () -> ConfigLoader.parseChimePolicy(candidate, BUTTONS), yaml);
        }
    }

    @Test void explicitEmptyTagsDisablePilotDefaults() throws Exception {
        YamlConfiguration existing = config("arrivalChimes:\n  tags: {}\n");
        try (InputStreamReader reader = new InputStreamReader(
                getClass().getResourceAsStream("/config.yml"), StandardCharsets.UTF_8)) {
            existing.setDefaults(YamlConfiguration.loadConfiguration(reader));
        }
        ButtonChimes policy = ConfigLoader.parseChimePolicy(existing, BUTTONS);
        assertEquals(ButtonChimes.Profile.LEGACY, policy.select("OAK_BUTTON", tag -> true));
    }

    @Test void allLegacyMappingRestoresOriginalSound() throws Exception {
        ButtonChimes policy = ConfigLoader.parseChimePolicy(config("arrivalChimes:\n  default: legacy\n  tags:\n    'minecraft:wooden_buttons': legacy\n    'minecraft:stone_buttons': legacy\n"), BUTTONS);
        assertEquals(ButtonChimes.Profile.LEGACY, policy.select("OAK_BUTTON", tag -> true));
        ButtonChimes.Note note = ButtonChimes.Profile.LEGACY.notes().get(0);
        assertEquals("BELL", note.instrument);
        assertEquals(0.5F, note.pitch);
        assertEquals(1.0F, note.volume);
    }

    @Test void arrangementsAreBoundedAndSilentHasNoNotes() {
        for (ButtonChimes.Profile profile : ButtonChimes.Profile.values()) {
            assertTrue(profile.notes().size() <= 3);
            int previous = -1;
            for (ButtonChimes.Note note : profile.notes()) {
                assertTrue(note.delayTicks > previous && note.delayTicks <= 12);
                assertTrue(note.pitch >= 0.5F && note.pitch <= 2.0F);
                assertTrue(note.volume > 0 && note.volume <= 1);
                previous = note.delayTicks;
            }
        }
        assertTrue(ButtonChimes.Profile.SILENT.notes().isEmpty());
    }

    @Test void pilotChimesAreFullVolumeDescendingBells() {
        for (ButtonChimes.Profile profile : Arrays.asList(ButtonChimes.Profile.WARM, ButtonChimes.Profile.CLEAR)) {
            assertEquals(2, profile.notes().size());
            ButtonChimes.Note first = profile.notes().get(0);
            ButtonChimes.Note second = profile.notes().get(1);
            assertEquals("BELL", first.instrument);
            assertEquals("BELL", second.instrument);
            assertEquals(0, first.delayTicks);
            assertEquals(8, second.delayTicks);
            assertTrue(first.pitch > second.pitch);
            assertEquals(Math.pow(2, -4.0 / 12), second.pitch / first.pitch, 0.00001);
            assertEquals(ButtonChimes.Profile.LEGACY.notes().get(0).volume, first.volume);
            assertEquals(first.volume, second.volume);
        }
    }

    @Test void crystalKeepsAudibleGainWhileRetainingItsThreeNotes() {
        assertEquals(3, ButtonChimes.Profile.CRYSTAL.notes().size());
        for (ButtonChimes.Note note : ButtonChimes.Profile.CRYSTAL.notes()) {
            assertEquals("CHIME", note.instrument);
            assertTrue(note.volume >= 0.8F);
        }
    }

    @Test void configurationSnapshotDoesNotChangeDuringRide() {
        Map<String, ButtonChimes.Profile> overrides = new HashMap<>();
        overrides.put("OAK_BUTTON", ButtonChimes.Profile.WARM);
        ButtonChimes policy = new ButtonChimes(ButtonChimes.Profile.LEGACY, Collections.emptyMap(), overrides);
        overrides.put("OAK_BUTTON", ButtonChimes.Profile.CRYSTAL);
        assertEquals(ButtonChimes.Profile.WARM, policy.select("OAK_BUTTON", tag -> false));
    }
}
