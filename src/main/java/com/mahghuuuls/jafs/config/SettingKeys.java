package com.mahghuuuls.jafs.config;

/**
 * The server configuration keys with their defaults and ranges. These values mirror the
 * project's numeric authority file ({@code config-defaults.json}); a unit test asserts parity.
 */
public final class SettingKeys {

    public static final String CATEGORY_GAMEPLAY = "gameplay";
    public static final String CATEGORY_DIAGNOSTICS = "diagnostics";

    public static final IntKey ACTIVE_FOOD_LIMIT = new IntKey("activeFoodLimit", 3, 1, 12,
            "Maximum simultaneous active foods per player.");
    public static final IntKey ELIGIBILITY_THRESHOLD = new IntKey("eligibilityThreshold", 50, 1, 100,
            "An active food can be refreshed or replaced when its remaining duration is strictly below this percent of its full duration.");
    public static final DoubleKey BASE_RECOVERY_RATE = new DoubleKey("baseRecoveryRate", 5.0, 0.0, 100.0,
            "Health restored per second once recovery is active, as a percent of maximum health.");
    public static final IntKey RECOVERY_DELAY = new IntKey("recoveryDelay", 10, 0, 300,
            "Damage-free seconds before recovery starts.");
    public static final DoubleKey DEPLETION_RATE = new DoubleKey("depletionRate", 1.0, 0.0, 10.0,
            "Speed multiplier of active-food timers. 0 pauses natural depletion.");
    public static final DoubleKey HUNGER_EFFECT_INCREMENT = new DoubleKey("hungerEffectIncrement", 0.5, 0.0, 10.0,
            "Added to the depletion rate for each level of the Hunger effect.");
    public static final BoolKey KEEP_FOODS_ON_DEATH = new BoolKey("keepFoodsOnDeath", false,
            "Whether active foods survive death.");
    public static final BoolKey INHIBITED_BLOCKS_RECOVERY = new BoolKey("inhibitedBlocksRecovery", true,
            "Block recovery while the Inhibited effect is active. Does nothing when Inhibited is not installed.");
    public static final BoolKey DIAGNOSTIC_MODE = new BoolKey("diagnosticMode", false,
            "Log one line per JAFS decision (meals, timers, recovery, payments, reloads). Off by default.");

    private SettingKeys() {
    }

    public static final class IntKey {
        public final String name;
        public final int defaultValue;
        public final int min;
        public final int max;
        public final String comment;

        IntKey(String name, int defaultValue, int min, int max, String comment) {
            this.name = name;
            this.defaultValue = defaultValue;
            this.min = min;
            this.max = max;
            this.comment = comment;
        }

        public boolean inRange(int value) {
            return value >= min && value <= max;
        }
    }

    public static final class DoubleKey {
        public final String name;
        public final double defaultValue;
        public final double min;
        public final double max;
        public final String comment;

        DoubleKey(String name, double defaultValue, double min, double max, String comment) {
            this.name = name;
            this.defaultValue = defaultValue;
            this.min = min;
            this.max = max;
            this.comment = comment;
        }

        public boolean inRange(double value) {
            return value >= min && value <= max;
        }
    }

    public static final class BoolKey {
        public final String name;
        public final boolean defaultValue;
        public final String comment;

        BoolKey(String name, boolean defaultValue, String comment) {
            this.name = name;
            this.defaultValue = defaultValue;
            this.comment = comment;
        }
    }
}
