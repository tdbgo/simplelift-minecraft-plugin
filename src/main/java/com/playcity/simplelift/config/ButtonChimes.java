package com.playcity.simplelift.config;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

/** Short, original note arrangements played with the client's built-in instruments. */
public final class ButtonChimes {
    public enum Profile {
        LEGACY(new Note("BELL", 0, 0.5F, 1.0F)),
        // A descending major third, with 400 ms between attacks at 20 TPS.
        // Keep the legacy gain: profile shaping must not make arrivals inaudible.
        WARM(new Note("BELL", 0, 0.793701F, 1.0F), new Note("BELL", 8, 0.629961F, 1.0F)),
        CLEAR(new Note("BELL", 0, 1.0F, 1.0F), new Note("BELL", 8, 0.793701F, 1.0F)),
        // A low descending major triad, with a longer pause before the resolving note.
        HOTEL(new Note("CHIME", 0, 0.890899F, 1.0F),
                new Note("CHIME", 5, 0.749154F, 0.9F), new Note("CHIME", 12, 0.594604F, 0.9F)),
        CRYSTAL(new Note("CHIME", 0, 1.0F, 1.0F),
                new Note("CHIME", 3, 1.33484F, 0.9F), new Note("CHIME", 8, 1.587401F, 0.8F)),
        SILENT();

        private final java.util.List<Note> notes;
        Profile(Note... notes) {
            this.notes = Collections.unmodifiableList(java.util.Arrays.asList(notes));
        }
        public java.util.List<Note> notes() { return notes; }
        public static Profile parse(String value) {
            try {
                return valueOf(value.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException | NullPointerException exception) {
                throw new IllegalArgumentException("Unknown arrival chime '" + value
                        + "'; expected legacy, warm, clear, hotel, crystal, or silent");
            }
        }
    }

    public static final class Note {
        public final String instrument;
        public final int delayTicks;
        public final float pitch;
        public final float volume;
        private Note(String instrument, int delayTicks, float pitch, float volume) {
            this.instrument = instrument;
            this.delayTicks = delayTicks;
            this.pitch = pitch;
            this.volume = volume;
        }
    }

    private final Profile fallback;
    private final Map<String, Profile> tags;
    private final Map<String, Profile> materials;

    public ButtonChimes(Profile fallback, Map<String, Profile> tags, Map<String, Profile> materials) {
        this.fallback = fallback;
        this.tags = Collections.unmodifiableMap(new LinkedHashMap<>(tags));
        this.materials = Collections.unmodifiableMap(new LinkedHashMap<>(materials));
    }

    public Profile select(String material, Predicate<String> inTag) {
        Profile exact = materials.get(material);
        if (exact != null) { return exact; }
        for (String tag : new String[] {"minecraft:wooden_buttons", "minecraft:stone_buttons", "minecraft:buttons"}) {
            Profile profile = tags.get(tag);
            if (profile != null && inTag.test(tag)) { return profile; }
        }
        return fallback;
    }
}
