package com.mahghuuuls.jafs.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns raw configuration values into a settings snapshot. An out-of-range number is reported
 * and replaced by its default; it is never clamped silently, so a typo stays visible.
 * Pure Java so the rules are unit-tested without a configuration file.
 */
public final class SettingsValidator {

    private SettingsValidator() {
    }

    /** Raw values as read from the file, before validation. */
    public static final class Raw {
        public int activeFoodLimit = SettingKeys.ACTIVE_FOOD_LIMIT.defaultValue;
        public int eligibilityThreshold = SettingKeys.ELIGIBILITY_THRESHOLD.defaultValue;
        public double baseRecoveryRate = SettingKeys.BASE_RECOVERY_RATE.defaultValue;
        public int recoveryDelay = SettingKeys.RECOVERY_DELAY.defaultValue;
        public double depletionRate = SettingKeys.DEPLETION_RATE.defaultValue;
        public double hungerEffectIncrement = SettingKeys.HUNGER_EFFECT_INCREMENT.defaultValue;
        public boolean keepFoodsOnDeath = SettingKeys.KEEP_FOODS_ON_DEATH.defaultValue;
        public boolean inhibitedBlocksRecovery = SettingKeys.INHIBITED_BLOCKS_RECOVERY.defaultValue;
        public boolean diagnosticMode = SettingKeys.DIAGNOSTIC_MODE.defaultValue;
    }

    public static GameplaySettings validate(Raw raw, long revision) {
        List<String> problems = new ArrayList<String>();
        int limit = checkInt(SettingKeys.ACTIVE_FOOD_LIMIT, raw.activeFoodLimit, problems);
        int threshold = checkInt(SettingKeys.ELIGIBILITY_THRESHOLD, raw.eligibilityThreshold, problems);
        double recoveryRate = checkDouble(SettingKeys.BASE_RECOVERY_RATE, raw.baseRecoveryRate, problems);
        int delay = checkInt(SettingKeys.RECOVERY_DELAY, raw.recoveryDelay, problems);
        double depletion = checkDouble(SettingKeys.DEPLETION_RATE, raw.depletionRate, problems);
        double increment = checkDouble(SettingKeys.HUNGER_EFFECT_INCREMENT, raw.hungerEffectIncrement, problems);
        return new GameplaySettings(limit, threshold, recoveryRate, delay, depletion, increment, raw.keepFoodsOnDeath,
                raw.inhibitedBlocksRecovery, raw.diagnosticMode, problems, revision);
    }

    private static int checkInt(SettingKeys.IntKey key, int value, List<String> problems) {
        if (key.inRange(value)) {
            return value;
        }
        problems.add(key.name + " = " + value + " is outside " + key.min + ".." + key.max
                + "; using the default " + key.defaultValue);
        return key.defaultValue;
    }

    private static double checkDouble(SettingKeys.DoubleKey key, double value, List<String> problems) {
        if (!Double.isNaN(value) && key.inRange(value)) {
            return value;
        }
        problems.add(key.name + " = " + value + " is outside " + key.min + ".." + key.max
                + "; using the default " + key.defaultValue);
        return key.defaultValue;
    }
}
