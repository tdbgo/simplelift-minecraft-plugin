package com.playcity.simplelift.compat;

import com.playcity.simplelift.config.ButtonChimes;
import com.playcity.simplelift.util.ChimeQueue;
import java.util.UUID;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/** One bounded chime per player; a later arrival replaces any remaining notes. */
public final class ArrivalChimes {
    private final JavaPlugin plugin;
    private final ChimeQueue<UUID> queue;

    public ArrivalChimes(JavaPlugin plugin) {
        this.plugin = plugin;
        this.queue = new ChimeQueue<>((action, ticks) ->
                plugin.getServer().getScheduler().runTaskLater(plugin, action, ticks)::cancel);
    }

    public void play(Player player, ButtonChimes.Profile profile, float volume) {
        UUID id = player.getUniqueId();
        cancel(id);
        if (volume <= 0 || profile.notes().isEmpty()) { return; }
        UUID world = player.getWorld().getUID();
        queue.play(id, profile, note -> {
            Player target = plugin.getServer().getPlayer(id);
            if (target != null && target.isOnline() && !target.isDead()
                    && target.getWorld().getUID().equals(world)) {
                Sound sound = SoundEffects.resolve("BLOCK_NOTE_BLOCK_" + note.instrument,
                        "BLOCK_NOTE_" + note.instrument, "BLOCK_NOTE_BLOCK_BELL", "NOTE_PLING");
                if (sound != null) { target.playSound(target.getLocation(), sound, volume * note.volume, note.pitch); }
            }
        });
    }

    public void cancelAll() {
        queue.cancelAll();
    }

    public void cancel(UUID id) {
        queue.cancel(id);
    }
}
