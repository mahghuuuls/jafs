package com.mahghuuuls.jafs.core;

import com.mahghuuuls.jafs.api.PropertyOperation;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Flat sums, additive percent, the rate multiplier floor, and the health cap (REQ-028, REQ-029, REQ-031, REQ-032, REQ-036). */
class BonusMathTest {

    @Test
    void theHungerEffectAddsItsIncrementPerLevel() {
        // REQ-045: rate 1.0, increment 0.5: Hunger I 1.5, Hunger II 2.0; no effect leaves the rate
        assertEquals(1.0, BonusMath.baseDepletionRate(1.0, 0, 0.5), 1e-12);
        assertEquals(1.5, BonusMath.baseDepletionRate(1.0, 1, 0.5), 1e-12);
        assertEquals(2.0, BonusMath.baseDepletionRate(1.0, 2, 0.5), 1e-12);
        // REQ-044: a configured rate of 0 stays 0 without the effect
        assertEquals(0.0, BonusMath.baseDepletionRate(0.0, 0, 0.5), 0);
        assertEquals(1.0, BonusMath.baseDepletionRate(0.0, 2, 0.5), 1e-12);
    }

    @Test
    void flatAmountsAdd() {
        Map<String, BonusMath.Combined> combined = BonusMath.combine(Arrays.asList(
                new Modifier("jafs:max_health", PropertyOperation.FLAT, 4),
                new Modifier("jafs:max_health", PropertyOperation.FLAT, 6)));
        assertEquals(10, combined.get("jafs:max_health").flat(), 1e-9);
        assertEquals(30, combined.get("jafs:max_health").apply(20), 1e-9);
    }

    @Test
    void percentAmountsAddAndApplyOnceToTheBase() {
        // REQ-029: +20 and +30 percent recovery rate: base times 1.5, not 1.56
        Map<String, BonusMath.Combined> combined = BonusMath.combine(Arrays.asList(
                new Modifier("jafs:recovery_rate", PropertyOperation.PERCENT, 20),
                new Modifier("jafs:recovery_rate", PropertyOperation.PERCENT, 30)));
        assertEquals(50, combined.get("jafs:recovery_rate").percent(), 1e-9);
        assertEquals(7.5, combined.get("jafs:recovery_rate").apply(5), 1e-9);
        assertEquals(1.5, BonusMath.rateMultiplier(50), 1e-9);
    }

    @Test
    void percentAppliesAfterFlat() {
        // IMP-004 example: +4 and +6 flat and +50 percent on a base of 20: (20 + 10) * 1.5 = 45
        Map<String, BonusMath.Combined> combined = BonusMath.combine(Arrays.asList(
                new Modifier("jafs:max_health", PropertyOperation.FLAT, 4),
                new Modifier("jafs:max_health", PropertyOperation.FLAT, 6),
                new Modifier("jafs:max_health", PropertyOperation.PERCENT, 50)));
        assertEquals(45, combined.get("jafs:max_health").apply(20), 1e-9);
        assertEquals(3, combined.get("jafs:max_health").contributions());
    }

    @Test
    void propertiesAreCombinedSeparatelyInFirstSeenOrder() {
        Map<String, BonusMath.Combined> combined = BonusMath.combine(Arrays.asList(
                new Modifier("jafs:depletion_rate", PropertyOperation.PERCENT, 100),
                new Modifier("jafs:max_health", PropertyOperation.FLAT, -25),
                new Modifier("jafs:depletion_rate", PropertyOperation.PERCENT, -20)));
        assertEquals(Arrays.asList("jafs:depletion_rate", "jafs:max_health"), new java.util.ArrayList<String>(combined.keySet()));
        assertEquals(80, combined.get("jafs:depletion_rate").percent(), 1e-9);
        assertEquals(-25, combined.get("jafs:max_health").flat(), 1e-9);
        assertEquals(2.0, BonusMath.rateMultiplier(100), 1e-9);
    }

    @Test
    void rateMultiplierNeverGoesBelowZero() {
        // REQ-036: -150 percent recovery is zero, never damage
        assertEquals(0, BonusMath.rateMultiplier(-150), 1e-9);
        assertEquals(0, BonusMath.rateMultiplier(-100), 1e-9);
        assertEquals(0.5, BonusMath.rateMultiplier(-50), 1e-9);
    }

    @Test
    void healthIsCappedOnlyWhenAboveTheMaximum() {
        assertEquals(20, BonusMath.cappedHealth(24, 20), 1e-9);
        assertEquals(15, BonusMath.cappedHealth(15, 24), 1e-9);
        assertEquals(15, BonusMath.cappedHealth(15, 15), 1e-9);
        assertTrue(BonusMath.Combined.ZERO.isZero());
    }
}
