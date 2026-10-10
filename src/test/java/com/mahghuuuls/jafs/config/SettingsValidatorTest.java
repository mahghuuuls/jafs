package com.mahghuuuls.jafs.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsValidatorTest {

    @Test
    void validValuesPassThroughWithoutProblems() {
        SettingsValidator.Raw raw = new SettingsValidator.Raw();
        raw.activeFoodLimit = 12;
        raw.eligibilityThreshold = 1;
        raw.depletionRate = 0.0;
        raw.recoveryDelay = 300;
        GameplaySettings settings = SettingsValidator.validate(raw, 7);
        assertEquals(12, settings.activeFoodLimit());
        assertEquals(1, settings.eligibilityThresholdPercent());
        assertEquals(0.0, settings.depletionRate(), 1e-9);
        assertEquals(300, settings.recoveryDelaySeconds());
        assertEquals(7, settings.revision());
        assertTrue(settings.problems().isEmpty());
    }

    @Test
    void outOfRangeValuesAreReportedAndReplacedByDefaults() {
        SettingsValidator.Raw raw = new SettingsValidator.Raw();
        raw.activeFoodLimit = 99;
        raw.depletionRate = -1.0;
        raw.baseRecoveryRate = 100.5;
        GameplaySettings settings = SettingsValidator.validate(raw, 1);
        assertEquals(SettingKeys.ACTIVE_FOOD_LIMIT.defaultValue, settings.activeFoodLimit());
        assertEquals(SettingKeys.DEPLETION_RATE.defaultValue, settings.depletionRate(), 1e-9);
        assertEquals(SettingKeys.BASE_RECOVERY_RATE.defaultValue, settings.baseRecoveryRatePercent(), 1e-9);
        assertEquals(3, settings.problems().size());
        String joined = String.join(" | ", settings.problems());
        assertTrue(joined.contains("activeFoodLimit = 99"), joined);
        assertTrue(joined.contains("depletionRate = -1.0"), joined);
        assertTrue(joined.contains("baseRecoveryRate = 100.5"), joined);
    }

    @Test
    void booleansAreNeverReported() {
        SettingsValidator.Raw raw = new SettingsValidator.Raw();
        raw.keepFoodsOnDeath = true;
        raw.diagnosticMode = true;
        GameplaySettings settings = SettingsValidator.validate(raw, 1);
        assertTrue(settings.keepFoodsOnDeath());
        assertTrue(settings.diagnosticMode());
        assertTrue(settings.problems().isEmpty());
    }
}
