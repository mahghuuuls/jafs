package com.mahghuuuls.jafs.config;

import java.util.Collections;
import java.util.List;

/** Immutable snapshot of the server configuration, with the problems found while reading it. */
public final class GameplaySettings {

    private final int activeFoodLimit;
    private final int eligibilityThresholdPercent;
    private final double baseRecoveryRatePercent;
    private final int recoveryDelaySeconds;
    private final double depletionRate;
    private final double hungerEffectIncrement;
    private final boolean keepFoodsOnDeath;
    private final boolean inhibitedBlocksRecovery;
    private final boolean diagnosticMode;
    private final List<String> problems;
    private final long revision;

    GameplaySettings(int activeFoodLimit, int eligibilityThresholdPercent, double baseRecoveryRatePercent,
            int recoveryDelaySeconds, double depletionRate, double hungerEffectIncrement, boolean keepFoodsOnDeath,
            boolean inhibitedBlocksRecovery, boolean diagnosticMode, List<String> problems, long revision) {
        this.activeFoodLimit = activeFoodLimit;
        this.eligibilityThresholdPercent = eligibilityThresholdPercent;
        this.baseRecoveryRatePercent = baseRecoveryRatePercent;
        this.recoveryDelaySeconds = recoveryDelaySeconds;
        this.depletionRate = depletionRate;
        this.hungerEffectIncrement = hungerEffectIncrement;
        this.keepFoodsOnDeath = keepFoodsOnDeath;
        this.inhibitedBlocksRecovery = inhibitedBlocksRecovery;
        this.diagnosticMode = diagnosticMode;
        this.problems = Collections.unmodifiableList(problems);
        this.revision = revision;
    }

    public static GameplaySettings defaults() {
        return new GameplaySettings(SettingKeys.ACTIVE_FOOD_LIMIT.defaultValue,
                SettingKeys.ELIGIBILITY_THRESHOLD.defaultValue, SettingKeys.BASE_RECOVERY_RATE.defaultValue,
                SettingKeys.RECOVERY_DELAY.defaultValue, SettingKeys.DEPLETION_RATE.defaultValue,
                SettingKeys.HUNGER_EFFECT_INCREMENT.defaultValue, SettingKeys.KEEP_FOODS_ON_DEATH.defaultValue,
                SettingKeys.INHIBITED_BLOCKS_RECOVERY.defaultValue, SettingKeys.DIAGNOSTIC_MODE.defaultValue,
                Collections.<String>emptyList(), 0L);
    }

    public int activeFoodLimit() {
        return activeFoodLimit;
    }

    public int eligibilityThresholdPercent() {
        return eligibilityThresholdPercent;
    }

    public double baseRecoveryRatePercent() {
        return baseRecoveryRatePercent;
    }

    public int recoveryDelaySeconds() {
        return recoveryDelaySeconds;
    }

    public double depletionRate() {
        return depletionRate;
    }

    public double hungerEffectIncrement() {
        return hungerEffectIncrement;
    }

    public boolean keepFoodsOnDeath() {
        return keepFoodsOnDeath;
    }

    public boolean inhibitedBlocksRecovery() {
        return inhibitedBlocksRecovery;
    }

    public boolean diagnosticMode() {
        return diagnosticMode;
    }

    /** Human-readable problems found while reading; empty when every value was valid. */
    public List<String> problems() {
        return problems;
    }

    public long revision() {
        return revision;
    }
}
