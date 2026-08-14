package com.playcity.simplelift.command;

import com.playcity.simplelift.SimpleLiftPlugin;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

public final class LiftCommand implements TabExecutor {

    private static final List<String> SUBCOMMANDS = Collections.unmodifiableList(
            Arrays.asList("info", "reload"));
    private final SimpleLiftPlugin plugin;

    public LiftCommand(SimpleLiftPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("info")) {
            sender.sendMessage("SimpleLift " + plugin.pluginVersion()
                    + " - Minecraft 1.13-26.2, Lift/LiftReloaded compatible");
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("lift.reload")) {
                sender.sendMessage(plugin.settings().messages().noPermission());
                return true;
            }
            plugin.reloadSettings(sender);
            return true;
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return Collections.emptyList();
        }
        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<String>();
        for (String value : SUBCOMMANDS) {
            if (value.startsWith(prefix)
                    && (!value.equals("reload") || sender.hasPermission("lift.reload"))) {
                result.add(value);
            }
        }
        return result;
    }
}
