package com.playcity.simplelift.config;

import com.playcity.simplelift.util.MaterialGlob;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.IntPredicate;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class ConfigLoader {

    private static final List<String> LEGACY_DATA_FOLDERS = Collections.unmodifiableList(
            Arrays.asList("LiftReloaded", "Lift"));

    private ConfigLoader() {
    }

    public static void prepareFirstRun(JavaPlugin plugin) {
        File target = new File(plugin.getDataFolder(), "config.yml");
        if (target.isFile()) {
            return;
        }

        File pluginsDirectory = plugin.getDataFolder().getParentFile();
        for (String legacyFolder : LEGACY_DATA_FOLDERS) {
            File candidate = new File(new File(pluginsDirectory, legacyFolder), "config.yml");
            if (!candidate.isFile()) {
                continue;
            }
            try {
                Files.createDirectories(target.toPath().getParent());
                Files.copy(candidate.toPath(), target.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
                plugin.getLogger().info("Imported legacy configuration from " + candidate.getPath());
                return;
            } catch (IOException exception) {
                throw new LiftConfigurationException("Could not import " + candidate, exception);
            }
        }

        plugin.saveDefaultConfig();
    }

    public static LiftSettings load(JavaPlugin plugin) {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();
        applyBundledDefaults(plugin, config);

        int maxLiftArea = boundedInt(config, "maxLiftArea", 49, value -> value >= 1 && value <= 4096,
                "must be between 1 and 4096");
        int minHeight = config.getInt("minHeight", -64);
        int maxHeight = config.getInt("maxHeight", 1023);
        if (maxHeight <= minHeight) {
            throw new LiftConfigurationException("maxHeight must be greater than minHeight");
        }

        int timeout = boundedInt(config, "secondsUntilTimeout", 2, value -> value >= 0 && value <= 60,
                "must be between 0 and 60");
        int soundVolume = boundedInt(config, "soundVolume", 100, value -> value >= 0 && value <= 100,
                "must be between 0 and 100");
        int selectionTimeout = boundedInt(config, "selection.timeoutSeconds", 8,
                value -> value >= 0 && value <= 300, "must be between 0 and 300");
        int selectionFastStep = boundedInt(config, "selection.fastStep", 5,
                value -> value >= 1 && value <= 20, "must be between 1 and 20");

        Map<Material, Double> baseSpeeds = parseBaseBlocks(config);
        Set<Material> floorMaterials = parseMaterials(config, "floorBlocks");
        Set<Material> buttonMaterials = parseMaterials(config, "buttonBlocks");
        Set<Material> signMaterials = parseMaterials(config, "signBlocks");
        Set<Material> shaftMaterials = parseMaterials(config, "shaftBlocks");
        Set<Material> musicMaterials = parseMusicBlocks(config);

        if (baseSpeeds.isEmpty() || floorMaterials.isEmpty() || buttonMaterials.isEmpty() || signMaterials.isEmpty()) {
            throw new LiftConfigurationException(
                    "baseBlocks, floorBlocks, buttonBlocks, and signBlocks must each match at least one material");
        }
        if (!musicMaterials.isEmpty()) {
            plugin.getLogger().warning(
                    "Legacy music:true base blocks are accepted, but NoteBlockAPI music playback is not yet implemented; "
                            + "SimpleLift will use its normal movement sounds.");
        }

        LiftMessages messages = parseMessages(config);
        return new LiftSettings(
                config.getBoolean("debug", false),
                maxLiftArea,
                maxHeight,
                minHeight,
                config.getBoolean("autoPlace", false),
                config.getBoolean("preventEntry", false),
                config.getBoolean("preventLeave", true),
                config.getBoolean("liftMobs", true),
                config.getBoolean("mouseScroll", true),
                config.getBoolean("selection.preserveHotbar", true),
                config.getBoolean("selection.actionBar", true),
                selectionTimeout,
                selectionFastStep,
                config.getBoolean("selection.confirmWithSwapKey", true),
                timeout,
                config.getBoolean("redstone", false),
                config.getBoolean("soundEnabled", true),
                soundVolume,
                baseSpeeds,
                floorMaterials,
                buttonMaterials,
                signMaterials,
                shaftMaterials,
                musicMaterials,
                messages
        );
    }

    private static void applyBundledDefaults(JavaPlugin plugin, FileConfiguration config) {
        try (InputStream stream = plugin.getResource("config.yml")) {
            if (stream == null) {
                throw new LiftConfigurationException("Bundled config.yml is missing");
            }
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                config.setDefaults(YamlConfiguration.loadConfiguration(reader));
            }
        } catch (IOException exception) {
            throw new LiftConfigurationException("Could not read bundled config.yml", exception);
        }
    }

    private static int boundedInt(FileConfiguration config, String key, int fallback, IntPredicate predicate,
            String requirement) {
        int value = config.getInt(key, fallback);
        if (!predicate.test(value)) {
            throw new LiftConfigurationException(key + " " + requirement + " (was " + value + ")");
        }
        return value;
    }

    private static Map<Material, Double> parseBaseBlocks(FileConfiguration config) {
        Map<Material, Double> result = new EnumMap<>(Material.class);
        List<Map<?, ?>> list = config.getMapList("baseBlocks");
        if (!list.isEmpty()) {
            for (Map<?, ?> entry : list) {
                addBaseBlock(result, entry.get("type"), entry.get("speed"));
            }
            return result;
        }

        // Lift config version 1 represented base blocks as a map keyed by material.
        ConfigurationSection legacy = config.getConfigurationSection("baseBlocks");
        if (legacy != null) {
            for (String material : legacy.getKeys(false)) {
                ConfigurationSection values = legacy.getConfigurationSection(material);
                Object speed = values == null ? legacy.get(material) : values.get("speed");
                addBaseBlock(result, material, speed);
            }
        }
        return result;
    }

    private static Set<Material> parseMusicBlocks(FileConfiguration config) {
        Set<Material> result = EnumSet.noneOf(Material.class);
        for (Map<?, ?> entry : config.getMapList("baseBlocks")) {
            if (!Boolean.TRUE.equals(entry.get("music"))) {
                continue;
            }
            Material material = parseMaterial(entry.get("type"));
            result.add(material);
        }
        return result;
    }

    private static void addBaseBlock(Map<Material, Double> result, Object rawType, Object rawSpeed) {
        Material material = parseMaterial(rawType);
        if (!(rawSpeed instanceof Number)) {
            throw new LiftConfigurationException("baseBlocks speed for " + material + " must be a number");
        }
        Number number = (Number) rawSpeed;
        double speed = number.doubleValue();
        if (!Double.isFinite(speed) || speed <= 0.0D || speed > 10.0D) {
            throw new LiftConfigurationException(
                    "baseBlocks speed for " + material + " must be greater than 0 and at most 10");
        }
        if (result.putIfAbsent(material, speed) != null) {
            throw new LiftConfigurationException("Duplicate baseBlocks material: " + material);
        }
    }

    private static Material parseMaterial(Object rawType) {
        if (!(rawType instanceof String) || ((String) rawType).trim().isEmpty()) {
            throw new LiftConfigurationException("baseBlocks type must be a material name");
        }
        String name = (String) rawType;
        Material material = Material.matchMaterial(name.trim().toUpperCase(Locale.ROOT));
        if (material == null || !material.isBlock()) {
            throw new LiftConfigurationException("Unknown block material: " + name);
        }
        return material;
    }

    private static Set<Material> parseMaterials(FileConfiguration config, String key) {
        List<String> globs = config.getStringList(key);
        if (globs.isEmpty()) {
            return Collections.emptySet();
        }
        List<Pattern> patterns = new ArrayList<>(globs.size());
        try {
            for (String glob : globs) {
                patterns.add(MaterialGlob.compile(glob));
            }
        } catch (IllegalArgumentException exception) {
            throw new LiftConfigurationException("Invalid material pattern in " + key, exception);
        }

        Set<Material> result = EnumSet.noneOf(Material.class);
        for (Material material : Material.values()) {
            if (!material.isBlock()) {
                continue;
            }
            for (Pattern pattern : patterns) {
                if (pattern.matcher(material.name()).matches()) {
                    result.add(material);
                    break;
                }
            }
        }
        return result;
    }

    private static LiftMessages parseMessages(FileConfiguration config) {
        Map<String, String> fallbacks = new LinkedHashMap<>();
        fallbacks.put("destination", "&7Destination");
        fallbacks.put("currentFloor", "&7Current Floor");
        fallbacks.put("cantEnter", "&7Can't enter elevator in use");
        fallbacks.put("cantLeave", "&7Can't leave elevator in use");
        fallbacks.put("oneFloor", "&7There is only one floor.");
        fallbacks.put("unsafe", "&cIt is unsafe to leave a vehicle in a lift!");
        fallbacks.put("floorNotExists", "&cFloor does not exist. Check shaft for blockage");
        fallbacks.put("scrollSelectEnabled", "&7Scrollable floor selection enabled.");
        fallbacks.put("scrollSelectDisabled", "&7Scrollable floor selection disabled");
        fallbacks.put("scrollSelection", "&e▲ &fFloor {floor} {name} &7({index}/{total}) &e▼");
        fallbacks.put("scrollSelectConfirmed", "&aSelected floor {floor} {name}.");
        fallbacks.put("scrollSelectTimedOut", "&7Floor selection timed out.");
        fallbacks.put("scrollSelectBusy", "&cAnother player is selecting a floor on this sign.");
        fallbacks.put("timeout", "&7You have been teleported to the destination due to timeout");
        fallbacks.put("busy", "&cThis lift is already moving.");
        fallbacks.put("noPermission", "&cYou do not have permission.");
        fallbacks.put("internalError", "&cAn internal error occurred while processing this lift.");

        Map<String, String> values = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : fallbacks.entrySet()) {
            values.put(entry.getKey(), color(config.getString("messages." + entry.getKey(), entry.getValue())));
        }
        return new LiftMessages(
                values.get("destination"),
                values.get("currentFloor"),
                values.get("cantEnter"),
                values.get("cantLeave"),
                values.get("oneFloor"),
                values.get("unsafe"),
                values.get("floorNotExists"),
                values.get("scrollSelectEnabled"),
                values.get("scrollSelectDisabled"),
                values.get("scrollSelection"),
                values.get("scrollSelectConfirmed"),
                values.get("scrollSelectTimedOut"),
                values.get("scrollSelectBusy"),
                values.get("timeout"),
                values.get("busy"),
                values.get("noPermission"),
                values.get("internalError")
        );
    }

    @SuppressWarnings("deprecation")
    private static String color(String value) {
        return ChatColor.translateAlternateColorCodes('&', value == null ? "" : value);
    }

    public static void debug(Logger logger, LiftSettings settings, String message) {
        if (settings.debug()) {
            logger.info("[DEBUG] " + message);
        }
    }
}
