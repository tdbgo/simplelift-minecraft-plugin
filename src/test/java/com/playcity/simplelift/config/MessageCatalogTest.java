package com.playcity.simplelift.config;

import com.playcity.simplelift.lift.LiftSign;
import java.util.ArrayList;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MessageCatalogTest {
    private YamlConfiguration config(String language) {
        YamlConfiguration config = new YamlConfiguration();
        if (language != null) { config.set("language", language); }
        return config;
    }

    @Test void englishOverridesExistingKoreanForScreenshots() {
        YamlConfiguration config = config("en");
        config.set("messages.currentFloor", "현재 층");
        config.set("messages.scrollSelection", "{floor}층");
        LiftMessages messages = ConfigLoader.parseMessages(config);
        assertEquals("\u00a77Current Floor", messages.currentFloor());
        assertTrue(messages.scrollSelection().contains("Floor {floor}"));
        assertTrue(messages.text("reloadSucceeded").contains("reloaded"));
    }

    @Test void koreanOverridesBundledEnglishDefaults() {
        YamlConfiguration config = config("ko");
        YamlConfiguration defaults = config(null);
        defaults.set("messages.currentFloor", "Current Floor");
        config.setDefaults(defaults);
        assertEquals("\u00a77현재 층", ConfigLoader.parseMessages(config).currentFloor());
    }

    @Test void localeAliasesResolveConsistently() {
        for (String alias : Arrays.asList("EN", "en_US", "en-us", " en ")) {
            assertEquals(MessageCatalog.load(config("en")), MessageCatalog.load(config(alias)));
        }
        assertEquals(MessageCatalog.load(config("ko")), MessageCatalog.load(config("ko_KR")));
        assertEquals(MessageCatalog.load(config("de")), MessageCatalog.load(config("de-DE")));
        assertEquals(MessageCatalog.load(config("ja")), MessageCatalog.load(config("ja_JP")));
        assertEquals(MessageCatalog.load(config("pt")), MessageCatalog.load(config("pt-PT")));
        assertEquals(MessageCatalog.load(config("pt_BR")), MessageCatalog.load(config("pt-BR")));
        assertEquals(MessageCatalog.load(config("zh")), MessageCatalog.load(config("zh_CN")));
        assertEquals(MessageCatalog.load(config("zh_HK")), MessageCatalog.load(config("zh-MO")));
        assertEquals(MessageCatalog.load(config("zh_TW")), MessageCatalog.load(config("zh-Hant")));
    }

    @Test void missingLanguagePreservesLegacyMessages() {
        YamlConfiguration config = config(null);
        config.set("messages.currentFloor", "&e현재 위치");
        config.set("messages.scrollSelection", "");
        LiftMessages messages = ConfigLoader.parseMessages(config);
        assertEquals("\u00a7e현재 위치", messages.currentFloor());
        assertEquals("", messages.scrollSelection());
        assertNotNull(messages.text("commandInfo"));
    }

    @Test void customAcceptsAnyLanguageAndFallsBackPerMissingKey() {
        YamlConfiguration config = config("custom");
        config.set("messages.destination", "&7Ziel");
        Map<String, String> messages = MessageCatalog.load(config);
        assertEquals("&7Ziel", messages.get("destination"));
        assertEquals("&7Current Floor", messages.get("currentFloor"));
    }

    @Test void invalidLanguageOrCustomTextRejectsCandidate() {
        assertThrows(LiftConfigurationException.class, () -> MessageCatalog.load(config("typo")));
        YamlConfiguration config = config("custom");
        config.set("messages.currentFloor", Arrays.asList("invalid"));
        assertThrows(LiftConfigurationException.class, () -> MessageCatalog.load(config));
        config.set("language", 5);
        assertThrows(LiftConfigurationException.class, () -> MessageCatalog.load(config));
    }

    @Test void bundledLanguagesHaveSameKeysAndPlaceholders() {
        Map<String, String> en = MessageCatalog.bundled("en");
        List<String> languages = new ArrayList<String>();
        for (String language : MessageCatalog.bundledLanguages()) {
            languages.add(language);
            Map<String, String> localized = MessageCatalog.bundled(language);
            assertEquals(en.keySet(), localized.keySet(), language);
            for (String key : en.keySet()) {
                assertFalse(localized.get(key).trim().isEmpty(), language + ": " + key);
                for (String placeholder : Arrays.asList(
                        "{floor}", "{name}", "{index}", "{total}", "{version}", "{error}", "{max}")) {
                    assertEquals(en.get(key).contains(placeholder), localized.get(key).contains(placeholder),
                            language + ": " + key + placeholder);
                }
            }
        }
        assertEquals(30, languages.size());
    }

    @Test void signLanguageRefreshPreservesNamesAndDestinationAndSkipsUnchangedWrites() {
        String[] lines = {"현재 층: 2", "로비", "목적지: 7", "옥상"};
        AtomicInteger writes = new AtomicInteger();
        World world = (World) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {World.class},
                (proxy, method, args) -> method.getName().equals("getUID") ? new UUID(0, 1) : null);
        Sign sign = (Sign) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {Sign.class}, (proxy, method, args) -> {
            if (method.getName().equals("getLine")) { return lines[(Integer) args[0]]; }
            if (method.getName().equals("setLine")) { lines[(Integer) args[0]] = (String) args[1]; return null; }
            if (method.getName().equals("update")) { writes.incrementAndGet(); return true; }
            return null;
        });
        Block block = (Block) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {Block.class}, (proxy, method, args) -> {
            if (method.getName().equals("getState")) { return sign; }
            if (method.getName().equals("getWorld")) { return world; }
            return 0;
        });
        LiftSign lift = new LiftSign(block);
        LiftMessages english = ConfigLoader.parseMessages(config("en"));
        lift.refreshLabels(english, 2);
        assertArrayEquals(new String[] {"\u00a77Current Floor: 2", "로비", "\u00a77Destination: 7", "옥상"}, lines);
        assertEquals(7, lift.destinationLevel());
        lift.refreshLabels(english, 2);
        assertEquals(1, writes.get());
        lift.refreshLabels(ConfigLoader.parseMessages(config("ko")), 2);
        assertEquals("\u00a77목적지: 7", lines[2]);
        assertEquals("로비", lines[1]);
    }
}
