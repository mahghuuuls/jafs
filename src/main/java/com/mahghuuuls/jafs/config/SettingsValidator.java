package com.mahghuuuls.jafs.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Turns raw configuration values into a settings snapshot. A value that is not a number or
 * boolean at all is malformed (the caller treats that as a file-level failure); an out-of-range
 * number is reported and replaced by its default; it is never clamped silently, so a typo stays
 * visible. Pure Java so the rules are unit-tested without a configuration file.
 */
public final class SettingsValidator {

    private SettingsValidator() {
    }

    /** The raw values parsed from their strings, plus the ones that could not be parsed at all. */
    public static final class Parsed {
        public final Raw raw;
        public final List<String> malformed;

        Parsed(Raw raw, List<String> malformed) {
            this.raw = raw;
            this.malformed = malformed;
        }
    }

    /**
     * Parses the file's strings, keyed by setting name; a missing key keeps the default, an
     * unparseable value keeps the default and is listed as malformed.
     */
    public static Parsed parse(Map<String, String> values) {
        List<String> malformed = new ArrayList<String>();
        Raw raw = new Raw();
        raw.activeFoodLimit = parseInt(values, SettingKeys.ACTIVE_FOOD_LIMIT, malformed);
        raw.eligibilityThreshold = parseInt(values, SettingKeys.ELIGIBILITY_THRESHOLD, malformed);
        raw.baseRecoveryRate = parseDouble(values, SettingKeys.BASE_RECOVERY_RATE, malformed);
        raw.recoveryDelay = parseInt(values, SettingKeys.RECOVERY_DELAY, malformed);
        raw.depletionRate = parseDouble(values, SettingKeys.DEPLETION_RATE, malformed);
        raw.hungerEffectIncrement = parseDouble(values, SettingKeys.HUNGER_EFFECT_INCREMENT, malformed);
        raw.keepFoodsOnDeath = parseBoolean(values, SettingKeys.KEEP_FOODS_ON_DEATH, malformed);
        raw.inhibitedBlocksRecovery = parseBoolean(values, SettingKeys.INHIBITED_BLOCKS_RECOVERY, malformed);
        raw.diagnosticMode = parseBoolean(values, SettingKeys.DIAGNOSTIC_MODE, malformed);
        return new Parsed(raw, malformed);
    }

    private static int parseInt(Map<String, String> values, SettingKeys.IntKey key, List<String> malformed) {
        String text = values.get(key.name);
        if (text == null) {
            return key.defaultValue;
        }
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            malformed.add(key.name + " = '" + text + "' is not a whole number");
            return key.defaultValue;
        }
    }

    private static double parseDouble(Map<String, String> values, SettingKeys.DoubleKey key, List<String> malformed) {
        String text = values.get(key.name);
        if (text == null) {
            return key.defaultValue;
        }
        try {
            double value = Double.parseDouble(text.trim());
            if (Double.isNaN(value) || Double.isInfinite(value)) {
                throw new NumberFormatException();
            }
            return value;
        } catch (NumberFormatException e) {
            malformed.add(key.name + " = '" + text + "' is not a number");
            return key.defaultValue;
        }
    }

    private static boolean parseBoolean(Map<String, String> values, SettingKeys.BoolKey key, List<String> malformed) {
        String text = values.get(key.name);
        if (text == null) {
            return key.defaultValue;
        }
        String lower = text.trim().toLowerCase(Locale.ROOT);
        if ("true".equals(lower)) {
            return true;
        }
        if ("false".equals(lower)) {
            return false;
        }
        malformed.add(key.name + " = '" + text + "' is not true or false");
        return key.defaultValue;
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
