package com.playcity.simplelift.config;

import com.playcity.simplelift.util.MaterialGlob;
import com.playcity.simplelift.compat.ButtonTags;
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
                messages,
                parseArrivalChimes(config, buttonMaterials)
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

    private static Map<Material, ButtonChimes.Profile> parseArrivalChimes(
            FileConfiguration config, Set<Material> buttons) {
        ButtonChimes policy = parseChimePolicy(config, buttons);
        Map<Material, ButtonChimes.Profile> result = new EnumMap<>(Material.class);
        for (Material button : buttons) {
            result.put(button, policy.select(button.name(), tag -> ButtonTags.contains(tag, button)));
        }
        return result;
    }

    static ButtonChimes parseChimePolicy(FileConfiguration config, Set<Material> buttons) {
        try {
            if (config.contains("arrivalChimes") && !config.isConfigurationSection("arrivalChimes")) {
                throw new IllegalArgumentException("arrivalChimes must be a mapping");
            }
            if (config.contains("arrivalChimes.default") && !config.isString("arrivalChimes.default")) {
                throw new IllegalArgumentException("arrivalChimes.default must be a profile name");
            }
            ButtonChimes.Profile fallback = ButtonChimes.Profile.parse(config.getString("arrivalChimes.default", "legacy"));
            Map<String, ButtonChimes.Profile> tags = chimeMappings(config, "arrivalChimes.tags");
            for (String tag : tags.keySet()) {
                if (!Arrays.asList("minecraft:wooden_buttons", "minecraft:stone_buttons", "minecraft:buttons").contains(tag)) {
                    throw new IllegalArgumentException("Unknown button tag: " + tag);
                }
            }
            Map<String, ButtonChimes.Profile> materials = chimeMappings(config, "arrivalChimes.materials");
            for (String name : materials.keySet()) {
                Material material = Material.getMaterial(name);
                if (material == null || !buttons.contains(material)) {
                    throw new IllegalArgumentException("Material must match buttonBlocks on this server: " + name);
                }
            }
            return new ButtonChimes(fallback, tags, materials);
        } catch (IllegalArgumentException exception) {
            throw new LiftConfigurationException("arrivalChimes: " + exception.getMessage(), exception);
        }
    }

    private static Map<String, ButtonChimes.Profile> chimeMappings(FileConfiguration config, String path) {
        Object raw = config.get(path);
        if (raw != null && !(raw instanceof ConfigurationSection)) {
            throw new IllegalArgumentException(path + " must be a mapping");
        }
        ConfigurationSection section;
        if (!config.isSet(path) && config.getDefaults() != null) {
            section = config.getDefaults().getConfigurationSection(path);
        } else {
            section = config.getConfigurationSection(path);
        }
        Map<String, ButtonChimes.Profile> result = new LinkedHashMap<>();
        if (section != null) {
            for (String key : section.getKeys(false)) {
                Object value = section.get(key);
                if (!(value instanceof String)) { throw new IllegalArgumentException(path + "." + key + " must be a profile name"); }
                result.put(key, ButtonChimes.Profile.parse((String) value));
            }
        }
        return result;
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

    static LiftMessages parseMessages(FileConfiguration config) {
        Map<String, String> values = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : MessageCatalog.load(config).entrySet()) {
            values.put(entry.getKey(), color(entry.getValue()));
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
                values.get("internalError"), values
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
