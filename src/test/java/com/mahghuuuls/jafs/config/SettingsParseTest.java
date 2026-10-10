package com.mahghuuuls.jafs.config;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The file's strings into raw values: a value of the wrong kind is malformed (a reload then
 * keeps the previous snapshot), an out-of-range number falls back with a report (REQ-077),
 * a missing key keeps its default, and good values are read as written.
 */
class SettingsParseTest {

    private static Map<String, String> values(String... pairs) {
        Map<String, String> map = new HashMap<String, String>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put(pairs[i], pairs[i + 1]);
        }
        return map;
    }

    @Test
    void aWordWhereANumberBelongsIsMalformed() {
        SettingsValidator.Parsed parsed = SettingsValidator.parse(values("activeFoodLimit", "abc", "depletionRate", "1.0"));
        assertEquals(1, parsed.malformed.size());
        assertTrue(parsed.malformed.get(0).startsWith("activeFoodLimit = 'abc'"), parsed.malformed.get(0));
        // the bad value keeps its default so the first load can still use the rest
        assertEquals(SettingKeys.ACTIVE_FOOD_LIMIT.defaultValue, parsed.raw.activeFoodLimit);
        assertEquals(1.0, parsed.raw.depletionRate, 0);
    }

    @Test
    void wrongKindsAreMalformedForEveryType() {
        SettingsValidator.Parsed parsed = SettingsValidator.parse(values(
                "depletionRate", "fast", "keepFoodsOnDeath", "yes", "recoveryDelay", "1.5", "baseRecoveryRate", "NaN"));
        assertEquals(4, parsed.malformed.size());
    }

    @Test
    void anOutOfRangeNumberIsNotMalformedButFallsBackWithAReport() {
        SettingsValidator.Parsed parsed = SettingsValidator.parse(values("depletionRate", "-1.0", "activeFoodLimit", "99"));
        assertTrue(parsed.malformed.isEmpty());
        GameplaySettings settings = SettingsValidator.validate(parsed.raw, 5);
        assertEquals(2, settings.problems().size());
        assertEquals(SettingKeys.DEPLETION_RATE.defaultValue, settings.depletionRate(), 1e-9);
        assertEquals(SettingKeys.ACTIVE_FOOD_LIMIT.defaultValue, settings.activeFoodLimit());
        assertEquals(5, settings.revision());
    }

    @Test
    void goodValuesAreReadAsWrittenAndMissingKeysKeepDefaults() {
        SettingsValidator.Parsed parsed = SettingsValidator.parse(values(
                "activeFoodLimit", "1", "depletionRate", " 0.0 ", "hungerEffectIncrement", "0.25", "keepFoodsOnDeath", "TRUE",
                "diagnosticMode", "true"));
        assertTrue(parsed.malformed.isEmpty());
        GameplaySettings settings = SettingsValidator.validate(parsed.raw, 7);
        assertTrue(settings.problems().isEmpty());
        assertEquals(1, settings.activeFoodLimit());
        assertEquals(0.0, settings.depletionRate(), 0);
        assertEquals(0.25, settings.hungerEffectIncrement(), 1e-12);
        assertTrue(settings.keepFoodsOnDeath());
        assertTrue(settings.diagnosticMode());
        assertEquals(SettingKeys.RECOVERY_DELAY.defaultValue, settings.recoveryDelaySeconds());
        assertEquals(SettingKeys.INHIBITED_BLOCKS_RECOVERY.defaultValue, settings.inhibitedBlocksRecovery());
    }
}
