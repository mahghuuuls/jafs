package com.mahghuuuls.jafs.core;

/**
 * The recovery formula and pause reasons (REQ-033 to REQ-037, REQ-026). Pure values in, pure
 * values out: the gameplay service owns the player, the delay in the capability, and the
 * fractional carry between ticks.
 *
 * <p>Order of the reasons: a spectator's clock stands still (nothing advances); otherwise the
 * delay counts down first; then an external block (the Inhibited effect, added by a later issue);
 * then full health; then a rate of zero; otherwise the player heals.
 */
public final class RecoveryRules {

    public static final int TICKS_PER_SECOND = 20;

    /** Health this close to the maximum counts as full: float healing can leave a sliver no heart can show. */
    public static final double FULL_EPSILON = 1e-3;

    public enum Status {
        HEALING("healing"),
        DELAY("delay"),
        FULL("full"),
        SPECTATOR("spectator"),
        INHIBITED("Inhibited"),
        ZERO_RATE("rate zero");

        private final String label;

        Status(String label) {
            this.label = label;
        }

        /** The word used in inspect replies and diagnostic lines. */
        public String label() {
            return label;
        }
    }

    /** One tick's outcome: the delay left, what to heal now, why healing did or did not happen, and the carry. */
    public static final class Step {
        public final int delayTicks;
        public final float heal;
        public final Status status;
        public final double carry;

        Step(int delayTicks, float heal, Status status, double carry) {
            this.delayTicks = delayTicks;
            this.heal = heal;
            this.status = status;
            this.carry = carry;
        }
    }

    private RecoveryRules() {
    }

    /** The full delay a hit restarts, in ticks (REQ-033). */
    public static int delayTicks(int delaySeconds) {
        return Math.max(0, delaySeconds) * TICKS_PER_SECOND;
    }

    /**
     * Health per tick: the base percent of the current maximum per second, times the food
     * multiplier (already floored at zero by {@link BonusMath#rateMultiplier}), spread over the
     * second (REQ-035, REQ-036).
     */
    public static double healPerTick(double basePercent, double multiplier, double maxHealth) {
        if (basePercent <= 0 || multiplier <= 0 || maxHealth <= 0) {
            return 0;
        }
        return basePercent / 100.0 * multiplier * maxHealth / TICKS_PER_SECOND;
    }

    /**
     * A hit counts when the amount after armor and resistance is above zero, whether or not
     * absorption soaked it (REQ-034): the amount that reached health plus the absorption the hit
     * took away. Which game events supply the two numbers is the damage hook's knowledge.
     */
    public static boolean damageCounts(float amountAfterAbsorption, float absorptionBefore, float absorptionAfter) {
        float soaked = Math.max(0, absorptionBefore - absorptionAfter);
        return amountAfterAbsorption + soaked > 0;
    }

    /**
     * One server tick for one player.
     *
     * @param delayTicks  the delay left before healing may start
     * @param health      current health
     * @param maxHealth   current maximum health
     * @param spectator   whether the player is a spectator (nothing advances, REQ-026)
     * @param blocked     whether something external blocks recovery (Inhibited)
     * @param perTickHeal the amount per tick from {@link #healPerTick}
     * @param carry       the fraction carried from the previous tick, so the per-second sum is exact
     */
    public static Step step(int delayTicks, double health, double maxHealth, boolean spectator, boolean blocked,
            double perTickHeal, double carry) {
        if (spectator) {
            return new Step(delayTicks, 0, Status.SPECTATOR, carry);
        }
        if (delayTicks > 0) {
            return new Step(delayTicks - 1, 0, Status.DELAY, 0);
        }
        if (blocked) {
            return new Step(0, 0, Status.INHIBITED, 0);
        }
        double room = maxHealth - health;
        if (room <= FULL_EPSILON) {
            return new Step(0, 0, Status.FULL, 0);
        }
        if (perTickHeal <= 0) {
            return new Step(0, 0, Status.ZERO_RATE, 0);
        }
        double pending = carry + perTickHeal;
        float heal = (float) Math.min(pending, room);
        // What the float could not carry stays as carry; healing to full drops it (REQ-037).
        double rest = pending >= room ? 0 : pending - heal;
        return new Step(0, heal, Status.HEALING, rest);
    }
}
