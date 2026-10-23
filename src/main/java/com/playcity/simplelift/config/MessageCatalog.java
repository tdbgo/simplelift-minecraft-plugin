package com.playcity.simplelift.config;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

/** Explicit presets override legacy text; custom mode preserves operator messages. */
public final class MessageCatalog {
    private static final Map<String, String> LANGUAGE_ALIASES = languageAliases();

    private MessageCatalog() { }

    public static Map<String, String> load(FileConfiguration config) {
        if (config.contains("language") && !config.isString("language")) {
            throw new LiftConfigurationException("language must be a bundled locale or custom");
        }
        String language = normalize(config.getString("language", "custom"));
        if (language.equals("custom")) {
            return customMessages(config);
        }

        String bundledLanguage = LANGUAGE_ALIASES.get(language);
        if (bundledLanguage == null) {
            throw new LiftConfigurationException("Unsupported language '" + language
                    + "'; use a bundled locale or custom (see config.yml)");
        }

        Map<String, String> messages = bundled("en");
        if (!bundledLanguage.equals("en")) {
            messages.putAll(bundled(bundledLanguage));
        }
        return messages;
    }

    static Iterable<String> bundledLanguages() {
        return Collections.unmodifiableSet(new java.util.LinkedHashSet<String>(LANGUAGE_ALIASES.values()));
    }

    private static Map<String, String> customMessages(FileConfiguration config) {
        Map<String, String> messages = bundled("en");
        for (String key : messages.keySet()) {
            String path = "messages." + key;
            if (config.contains(path)) {
                if (!config.isString(path)) { throw new LiftConfigurationException(path + " must be text"); }
                messages.put(key, config.getString(path));
            }
        }
        return messages;
    }

    private static String normalize(String language) {
        return language.trim().toLowerCase(Locale.ROOT).replace('-', '_');
    }

    private static Map<String, String> languageAliases() {
        Map<String, String> aliases = new LinkedHashMap<String, String>();
        addAliases(aliases, "en", "en", "en_us");
        addAliases(aliases, "en_gb", "en_gb", "en_uk");
        addAliases(aliases, "bg", "bg", "bg_bg");
        addAliases(aliases, "cs", "cs", "cs_cz");
        addAliases(aliases, "da", "da", "da_dk");
        addAliases(aliases, "de", "de", "de_de", "de_at", "de_ch");
        addAliases(aliases, "el", "el", "el_gr");
        addAliases(aliases, "es", "es", "es_es", "es_mx", "es_ar", "es_cl");
        addAliases(aliases, "et", "et", "et_ee");
        addAliases(aliases, "fi", "fi", "fi_fi");
        addAliases(aliases, "fr", "fr", "fr_fr", "fr_ca", "fr_be");
        addAliases(aliases, "hu", "hu", "hu_hu");
        addAliases(aliases, "it", "it", "it_it");
        addAliases(aliases, "ja", "ja", "ja_jp");
        addAliases(aliases, "ko", "ko", "ko_kr");
        addAliases(aliases, "lt", "lt", "lt_lt");
        addAliases(aliases, "nl", "nl", "nl_nl", "nl_be");
        addAliases(aliases, "no", "no", "no_no", "nb_no");
        addAliases(aliases, "pl", "pl", "pl_pl");
        addAliases(aliases, "pt", "pt", "pt_pt");
        addAliases(aliases, "pt_br", "pt_br");
        addAliases(aliases, "ro", "ro", "ro_ro");
        addAliases(aliases, "ru", "ru", "ru_ru");
        addAliases(aliases, "sv", "sv", "sv_se");
        addAliases(aliases, "tr", "tr", "tr_tr");
        addAliases(aliases, "uk", "uk", "uk_ua");
        addAliases(aliases, "vi", "vi", "vi_vn");
        addAliases(aliases, "zh", "zh", "zh_cn", "zh_sg", "zh_hans");
        addAliases(aliases, "zh_hk", "zh_hk", "zh_mo");
        addAliases(aliases, "zh_tw", "zh_tw", "zh_hant");
        return Collections.unmodifiableMap(aliases);
    }

    private static void addAliases(Map<String, String> aliases, String bundledLanguage, String... values) {
        for (String value : values) {
            aliases.put(value, bundledLanguage);
        }
    }

    static Map<String, String> bundled(String language) {
        try (InputStream input = MessageCatalog.class.getResourceAsStream("/languages/" + language + ".yml")) {
            if (input == null) { throw new LiftConfigurationException("Missing bundled language: " + language); }
            try (InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(reader);
                Map<String, String> result = new LinkedHashMap<>();
                for (String key : yaml.getKeys(false)) {
                    if (!yaml.isString(key)) { throw new LiftConfigurationException("Invalid bundled message: " + key); }
                    result.put(key, yaml.getString(key));
                }
                return result;
            }
        } catch (IOException exception) {
            throw new LiftConfigurationException("Could not read bundled language", exception);
        }
    }
}
