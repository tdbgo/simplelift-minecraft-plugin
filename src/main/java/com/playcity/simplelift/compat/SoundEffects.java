package com.playcity.simplelift.compat;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

/** Resolves renamed Bukkit sounds without linking the plugin to one Minecraft generation. */
public final class SoundEffects {

    private static final Sound BUTTON = resolve("UI_BUTTON_CLICK", "CLICK");
    private static final Sound ARRIVAL = resolve(
            "BLOCK_NOTE_BLOCK_BELL", "BLOCK_NOTE_BELL", "NOTE_PLING");
    private static final Sound MOVEMENT_PRIMARY = resolve(
            "ENTITY_BOAT_PADDLE_LAND", "ENTITY_MINECART_RIDING", "MINECART_BASE");
    private static final Sound MOVEMENT_SECONDARY = resolve(
            "BLOCK_BEEHIVE_WORK", "BLOCK_PISTON_EXTEND", "PISTON_EXTEND");

    private SoundEffects() {
    }

    public static void button(Player player, float volume, float pitch) {
        play(player, BUTTON, volume, pitch);
    }

    public static void arrival(Player player, float volume, float pitch) {
        play(player, ARRIVAL, volume, pitch);
    }

    public static void movement(Player player, float primaryVolume, float secondaryVolume, float pitch) {
        play(player, MOVEMENT_PRIMARY, primaryVolume, pitch);
        if (MOVEMENT_SECONDARY != MOVEMENT_PRIMARY) {
            play(player, MOVEMENT_SECONDARY, secondaryVolume, pitch);
        }
    }

    static Sound resolve(String... names) {
        for (String name : names) {
            try {
                return Sound.valueOf(name);
            } catch (IllegalArgumentException ignored) {
                // Try the name used by the next server generation.
            }
        }
        return null;
    }

    private static void play(Player player, Sound sound, float volume, float pitch) {
        if (sound != null) {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }
}
