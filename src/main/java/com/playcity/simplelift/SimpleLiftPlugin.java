package com.playcity.simplelift;

import com.playcity.simplelift.compat.ServerCompatibility;
import com.playcity.simplelift.command.LiftCommand;
import com.playcity.simplelift.config.ConfigLoader;
import com.playcity.simplelift.config.LiftConfigurationException;
import com.playcity.simplelift.config.LiftSettings;
import com.playcity.simplelift.listener.LiftListener;
import com.playcity.simplelift.lift.LiftScanner;
import com.playcity.simplelift.ride.RideManager;
import java.util.Objects;
import java.util.logging.Level;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class SimpleLiftPlugin extends JavaPlugin {

    private LiftSettings settings;
    private LiftScanner scanner;
    private RideManager rideManager;
    private LiftListener liftListener;

    @Override
    public void onEnable() {
        try {
            if (ServerCompatibility.isFolia()) {
                getLogger().severe("Folia is not supported: a moving lift can cross region ownership boundaries.");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            ConfigLoader.prepareFirstRun(this);
            settings = ConfigLoader.load(this);
            scanner = new LiftScanner(this::settings, getLogger());
            rideManager = new RideManager(this);

            liftListener = new LiftListener(this, scanner, rideManager);
            getServer().getPluginManager().registerEvents(liftListener, this);
            Objects.requireNonNull(getCommand("lift"), "lift command missing from plugin.yml")
                    .setExecutor(new LiftCommand(this));

            getLogger().info("SimpleLift " + pluginVersion()
                    + " enabled on " + getServer().getVersion());
        } catch (RuntimeException exception) {
            getLogger().log(Level.SEVERE, "SimpleLift could not be enabled", exception);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (liftListener != null) {
            liftListener.shutdown();
        }
        if (rideManager != null) {
            rideManager.shutdown();
        }
    }

    public LiftSettings settings() {
        return Objects.requireNonNull(settings, "Settings are not loaded");
    }

    @SuppressWarnings("deprecation")
    public String pluginVersion() {
        return getDescription().getVersion();
    }

    public boolean reloadSettings(CommandSender sender) {
        try {
            LiftSettings candidate = ConfigLoader.load(this);
            settings = candidate;
            if (liftListener != null) {
                liftListener.settingsReloaded();
            }
            sender.sendMessage("SimpleLift configuration reloaded.");
            getLogger().info("Configuration reloaded successfully");
            return true;
        } catch (LiftConfigurationException exception) {
            sender.sendMessage("SimpleLift configuration was not reloaded: " + exception.getMessage());
            getLogger().log(Level.WARNING,
                    "Configuration reload failed; keeping the last known-good settings", exception);
            return false;
        }
    }
}
