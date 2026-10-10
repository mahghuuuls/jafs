package com.mahghuuuls.jafs.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The numeric examples of REQ-033 to REQ-037 and REQ-026, run tick by tick through the pure rules
 * the way the service runs them: a player record with a delay, a health, and a carry.
 */
class RecoveryRulesTest {

    /** A minimal player stand-in driven by the rules. */
    private static final class Player {
        int delay;
        double health;
        double max = 20;
        boolean spectator;
        boolean blocked;
        double carry;
        RecoveryRules.Status status;

        Player(double health) {
            this.health = health;
        }

        void hit(int delaySeconds) {
            delay = RecoveryRules.delayTicks(delaySeconds);
            carry = 0;
        }

        void tick(double basePercent, double multiplier) {
            double perTick = RecoveryRules.healPerTick(basePercent, multiplier, max);
            RecoveryRules.Step step = RecoveryRules.step(delay, health, max, spectator, blocked, perTick, carry);
            delay = step.delayTicks;
            carry = step.carry;
            status = step.status;
            health = Math.min(max, health + step.heal);
        }

        void ticks(int count, double basePercent, double multiplier) {
            for (int i = 0; i < count; i++) {
                tick(basePercent, multiplier);
            }
        }
    }

    @Test
    void fivePercentOfTwentyHealsExactlyOnePerSecond() {
        // REQ-035: base 5, max 20, 10 seconds from 10 health reaches 20
        Player p = new Player(10);
        p.ticks(20, 5, 1.0);
        assertEquals(11.0, p.health, 1e-6);
        p.ticks(180, 5, 1.0);
        assertEquals(20.0, p.health, 1e-6);
        assertEquals(RecoveryRules.Status.HEALING, p.status);
        p.tick(5, 1.0);
        assertEquals(RecoveryRules.Status.FULL, p.status);
        assertEquals(20.0, p.health, 0);
    }

    @Test
    void aHitRestartsTheDelayFromTheFullValue() {
        // REQ-033: hits at second 0 and 6 with delay 10: healing starts at second 16
        Player p = new Player(10);
        p.hit(10);
        p.ticks(120, 5, 1.0);
        assertEquals(10.0, p.health, 0);
        assertEquals(RecoveryRules.Status.DELAY, p.status);
        p.hit(10);
        p.ticks(199, 5, 1.0);
        assertEquals(RecoveryRules.Status.DELAY, p.status);
        assertEquals(10.0, p.health, 0);
        p.tick(5, 1.0);
        assertEquals(RecoveryRules.Status.DELAY, p.status); // tick 200 of the delay
        p.tick(5, 1.0);
        assertEquals(RecoveryRules.Status.HEALING, p.status); // second 16
        assertEquals(10.05, p.health, 1e-6);
    }

    @Test
    void plusFiftyPercentHealsOnePointFivePerSecond() {
        Player p = new Player(10);
        p.ticks(20, 5, BonusMath.rateMultiplier(50));
        assertEquals(11.5, p.health, 1e-6);
    }

    @Test
    void minusOneHundredFiftyPercentHealsNothing() {
        // REQ-036: a total below zero is zero; recovery never deals damage
        Player p = new Player(10);
        p.ticks(600, 5, BonusMath.rateMultiplier(-150));
        assertEquals(10.0, p.health, 0);
        assertEquals(RecoveryRules.Status.ZERO_RATE, p.status);
        assertEquals(0, RecoveryRules.healPerTick(5, BonusMath.rateMultiplier(-150), 20), 0);
    }

    @Test
    void rateZeroInConfigHealsNothing() {
        Player p = new Player(10);
        p.ticks(100, 0, 1.0);
        assertEquals(10.0, p.health, 0);
        assertEquals(RecoveryRules.Status.ZERO_RATE, p.status);
    }

    @Test
    void healingNeverExceedsTheMaximum() {
        // REQ-037: 19.98 with 0.05 per tick heals 0.02 and reports full next
        Player p = new Player(19.98);
        p.tick(5, 1.0);
        assertEquals(20.0, p.health, 1e-6);
        assertEquals(RecoveryRules.Status.HEALING, p.status);
        assertEquals(0, p.carry, 0);
        p.tick(5, 1.0);
        assertEquals(RecoveryRules.Status.FULL, p.status);
    }

    @Test
    void spectatorHoldsTheDelayAndTheHealth() {
        // REQ-026: nothing advances while spectating; the delay resumes where it stopped
        Player p = new Player(10);
        p.hit(10);
        p.ticks(60, 5, 1.0);
        assertEquals(140, p.delay);
        p.spectator = true;
        p.ticks(1200, 5, 1.0);
        assertEquals(140, p.delay);
        assertEquals(10.0, p.health, 0);
        assertEquals(RecoveryRules.Status.SPECTATOR, p.status);
        p.spectator = false;
        p.ticks(140, 5, 1.0);
        assertEquals(0, p.delay);
        assertEquals(RecoveryRules.Status.DELAY, p.status);
        p.tick(5, 1.0);
        assertEquals(RecoveryRules.Status.HEALING, p.status);
    }

    @Test
    void spectatorAtZeroDelayPausesHealingAndResumesAtOnce() {
        Player p = new Player(10);
        p.ticks(20, 5, 1.0);
        p.spectator = true;
        p.ticks(100, 5, 1.0);
        assertEquals(11.0, p.health, 1e-6);
        p.spectator = false;
        p.ticks(20, 5, 1.0);
        assertEquals(12.0, p.health, 1e-6);
    }

    @Test
    void anExternalBlockPausesAfterTheDelay() {
        Player p = new Player(10);
        p.blocked = true;
        p.hit(1);
        p.ticks(20, 5, 1.0);
        assertEquals(RecoveryRules.Status.DELAY, p.status);
        p.tick(5, 1.0);
        assertEquals(RecoveryRules.Status.INHIBITED, p.status);
        assertEquals(10.0, p.health, 0);
        p.blocked = false;
        p.ticks(20, 5, 1.0);
        assertEquals(11.0, p.health, 1e-6);
    }

    @Test
    void theBlockComesAfterSpectatorAndDelayAndBeforeFullAndRate() {
        // REQ-040 precedence: a spectator stands still; a running delay counts first; then the block; a blocked
        // player at full health reports the block (so inspect names the cause); the rate is not consulted while blocked
        assertEquals(RecoveryRules.Status.SPECTATOR, RecoveryRules.step(0, 10, 20, true, true, 0.05, 0).status);
        assertEquals(RecoveryRules.Status.DELAY, RecoveryRules.step(5, 10, 20, false, true, 0.05, 0).status);
        assertEquals(RecoveryRules.Status.INHIBITED, RecoveryRules.step(0, 10, 20, false, true, 0.05, 0).status);
        assertEquals(RecoveryRules.Status.INHIBITED, RecoveryRules.step(0, 20, 20, false, true, 0.05, 0).status);
        assertEquals(RecoveryRules.Status.INHIBITED, RecoveryRules.step(0, 10, 20, false, true, 0, 0).status);
        // the block ending with the delay elapsed heals on the very next tick (REQ-040)
        RecoveryRules.Step step = RecoveryRules.step(0, 10, 20, false, false, 0.05, 0);
        assertEquals(RecoveryRules.Status.HEALING, step.status);
        assertEquals(0.05f, step.heal, 1e-6f);
    }

    @Test
    void theDelayCountsDownWhileFull() {
        // A full player's delay still runs out, so a later maximum increase heals at once
        Player p = new Player(20);
        p.hit(2);
        p.ticks(40, 5, 1.0);
        assertEquals(0, p.delay);
        p.tick(5, 1.0);
        assertEquals(RecoveryRules.Status.FULL, p.status);
        p.max = 24;
        p.ticks(20, 5, 1.0);
        assertEquals(21.2, p.health, 1e-6); // 5 percent of 24 per second
    }

    @Test
    void theCarryKeepsAFloatHealthOnTheCurve() {
        // The game holds health as a float; 0.1 per tick is not a float. The carry keeps the sum of the
        // heal amounts exact; the float additions in health add at most one ulp per tick (about 1e-6 here),
        // so a second of healing lands on the curve within float precision and never drifts a heart.
        float health = 10;
        double carry = 0;
        double perTick = RecoveryRules.healPerTick(10, 1.0, 20);
        for (int i = 0; i < 20; i++) {
            RecoveryRules.Step step = RecoveryRules.step(0, health, 20, false, false, perTick, carry);
            health += step.heal;
            carry = step.carry;
        }
        assertEquals(12.0f, health, 1e-4f);
        double sumOfHeals = 0;
        carry = 0;
        for (int i = 0; i < 20; i++) {
            RecoveryRules.Step step = RecoveryRules.step(0, 10, 20, false, false, perTick, carry);
            sumOfHeals += step.heal;
            carry = step.carry;
        }
        assertEquals(2.0, sumOfHeals + carry, 1e-12);
        // and over a long run the float sum does not drift: 100 seconds at 0.05 per tick from 0 of 1000
        health = 0;
        carry = 0;
        perTick = RecoveryRules.healPerTick(5, 1.0, 20);
        for (int i = 0; i < 2000; i++) {
            RecoveryRules.Step step = RecoveryRules.step(0, health, 1000, false, false, perTick, carry);
            health += step.heal;
            carry = step.carry;
        }
        assertEquals(100.0f, health, 1e-2f);
    }

    @Test
    void thePerSecondAmountScalesWithTheMaximum() {
        assertEquals(0.05, RecoveryRules.healPerTick(5, 1.0, 20), 1e-12);
        assertEquals(0.075, RecoveryRules.healPerTick(5, 1.0, 30), 1e-12);
        assertEquals(0.075, RecoveryRules.healPerTick(5, 1.5, 20), 1e-12);
        assertEquals(0, RecoveryRules.healPerTick(5, 1.0, 0), 0);
    }

    @Test
    void delayTicksComeFromSeconds() {
        assertEquals(200, RecoveryRules.delayTicks(10));
        assertEquals(0, RecoveryRules.delayTicks(0));
        assertEquals(0, RecoveryRules.delayTicks(-3));
    }

    @Test
    void damageCountsAfterArmorWhetherOrNotAbsorptionSoaksIt() {
        // REQ-034: 2 damage fully soaked by 4 absorption hearts still counts
        assertTrue(RecoveryRules.damageCounts(0, 4, 2));
        // a plain hit counts
        assertTrue(RecoveryRules.damageCounts(2, 0, 0));
        // reduced to nothing by armor or resistance, no absorption change: does not count
        assertFalse(RecoveryRules.damageCounts(0, 0, 0));
        assertFalse(RecoveryRules.damageCounts(0, 4, 4));
        // absorption that grew during the hit (another mod) never makes a negative soak
        assertFalse(RecoveryRules.damageCounts(0, 2, 4));
    }
}
