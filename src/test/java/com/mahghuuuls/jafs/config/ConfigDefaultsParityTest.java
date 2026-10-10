package com.mahghuuuls.jafs.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every server key the mod defines must carry the default and range recorded in
 * {@code config-defaults.json}, the project's single source of tuning numbers. The copy in test
 * resources is refreshed from the workspace file whenever that file changes.
 */
class ConfigDefaultsParityTest {

    @Test
    void serverKeysMatchTheAuthorityFile() throws Exception {
        JsonObject root = load();
        JsonObject gameplay = root.getAsJsonObject("gameplay");
        JsonObject diagnostics = root.getAsJsonObject("diagnostics");

        assertInt(gameplay, SettingKeys.ACTIVE_FOOD_LIMIT);
        assertInt(gameplay, SettingKeys.ELIGIBILITY_THRESHOLD);
        assertDouble(gameplay, SettingKeys.BASE_RECOVERY_RATE);
        assertInt(gameplay, SettingKeys.RECOVERY_DELAY);
        assertDouble(gameplay, SettingKeys.DEPLETION_RATE);
        assertDouble(gameplay, SettingKeys.HUNGER_EFFECT_INCREMENT);
        assertBool(gameplay, SettingKeys.KEEP_FOODS_ON_DEATH);
        assertBool(gameplay, SettingKeys.INHIBITED_BLOCKS_RECOVERY);
        assertBool(diagnostics, SettingKeys.DIAGNOSTIC_MODE);
    }

    @Test
    void everyServerScopeEntryInTheAuthorityFileHasAKey() throws Exception {
        JsonObject root = load();
        java.util.Set<String> known = new java.util.HashSet<String>(java.util.Arrays.asList(
                SettingKeys.ACTIVE_FOOD_LIMIT.name, SettingKeys.ELIGIBILITY_THRESHOLD.name,
                SettingKeys.BASE_RECOVERY_RATE.name, SettingKeys.RECOVERY_DELAY.name, SettingKeys.DEPLETION_RATE.name,
                SettingKeys.HUNGER_EFFECT_INCREMENT.name, SettingKeys.KEEP_FOODS_ON_DEATH.name,
                SettingKeys.INHIBITED_BLOCKS_RECOVERY.name, SettingKeys.DIAGNOSTIC_MODE.name));
        int serverEntries = 0;
        for (String section : new String[] {"gameplay", "diagnostics", "client", "syncing"}) {
            for (java.util.Map.Entry<String, JsonElement> e : root.getAsJsonObject(section).entrySet()) {
                JsonObject entry = e.getValue().getAsJsonObject();
                if (!"server".equals(entry.get("scope").getAsString())) {
                    continue;
                }
                serverEntries++;
                assertTrue(known.contains(e.getKey()), "config-defaults.json server entry " + e.getKey()
                        + " has no SettingKeys entry");
            }
        }
        assertEquals(known.size(), serverEntries, "SettingKeys and the server-scope entries differ in count");
    }

    @Test
    void definitionDurationRangeMatchesTheAuthorityFile() throws Exception {
        JsonObject entry = load().getAsJsonObject("gameplay").getAsJsonObject("fullDuration");
        assertEquals(entry.get("min").getAsInt(), com.mahghuuuls.jafs.core.DefinitionValidator.MIN_DURATION_SECONDS);
        assertEquals(entry.get("max").getAsInt(), com.mahghuuuls.jafs.core.DefinitionValidator.MAX_DURATION_SECONDS);
    }

    @Test
    void pinnedFoodLevelMatchesTheAuthorityFile() throws Exception {
        JsonObject gameplay = load().getAsJsonObject("gameplay");
        assertEquals(gameplay.getAsJsonObject("pinnedFoodLevel").get("default").getAsInt(),
                com.mahghuuuls.jafs.gameplay.HungerPin.PINNED_FOOD_LEVEL);
    }

    private static JsonObject load() throws Exception {
        InputStream stream = ConfigDefaultsParityTest.class.getResourceAsStream("/config-defaults.json");
        assertNotNull(stream, "config-defaults.json is missing from test resources");
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return new JsonParser().parse(reader).getAsJsonObject();
        }
    }

    private static JsonObject entry(JsonObject section, String key) {
        JsonElement element = section.get(key);
        assertNotNull(element, "config-defaults.json has no entry for " + key);
        return element.getAsJsonObject();
    }

    private static void assertInt(JsonObject section, SettingKeys.IntKey key) {
        JsonObject entry = entry(section, key.name);
        assertEquals(entry.get("default").getAsInt(), key.defaultValue, key.name + " default");
        assertEquals(entry.get("min").getAsInt(), key.min, key.name + " min");
        assertEquals(entry.get("max").getAsInt(), key.max, key.name + " max");
    }

    private static void assertDouble(JsonObject section, SettingKeys.DoubleKey key) {
        JsonObject entry = entry(section, key.name);
        assertEquals(entry.get("default").getAsDouble(), key.defaultValue, 1e-9, key.name + " default");
        assertEquals(entry.get("min").getAsDouble(), key.min, 1e-9, key.name + " min");
        assertEquals(entry.get("max").getAsDouble(), key.max, 1e-9, key.name + " max");
    }

    private static void assertBool(JsonObject section, SettingKeys.BoolKey key) {
        JsonObject entry = entry(section, key.name);
        assertEquals(entry.get("default").getAsBoolean(), key.defaultValue, key.name + " default");
    }
}
