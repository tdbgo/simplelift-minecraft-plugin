package com.playcity.simplelift.compat;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

public final class ActionBars {

    private ActionBars() {
    }

    public static boolean send(Player player, String message) {
        try {
            player.spigot().sendMessage(
                    ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
            return true;
        } catch (LinkageError | RuntimeException ignored) {
            return false;
        }
    }

    public static void clear(Player player) {
        send(player, "");
    }
}
