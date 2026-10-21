package com.mahghuuuls.jafs.client;

import com.mahghuuuls.jafs.core.FoodDefinition;

/**
 * The color of a food's time text in the HUD (REQ-064, REV-015): white while the food cannot be
 * eaten again, yellow once it is eligible, red when less than a minute of real time is left.
 * Red wins over yellow. A paused food (no real time left to count) keeps the eligibility color.
 * Plain Java, unit-tested.
 */
public enum TimeColor {
    NORMAL(0xFFFFFFFF),
    ELIGIBLE(0xFFFFFF55),
    LOW(0xFFFF5555);

    /** Below this many real ticks left the time is red: one minute. */
    public static final int LOW_TICKS = 60 * FoodDefinition.TICKS_PER_SECOND;

    public final int argb;

    TimeColor(int argb) {
        this.argb = argb;
    }

    /**
     * @param eligible      whether the food can be eaten again now
     * @param realTicksLeft the real ticks until expiry at the current rate, or a negative value while paused
     */
    public static TimeColor of(boolean eligible, int realTicksLeft) {
        if (realTicksLeft >= 0 && realTicksLeft < LOW_TICKS) {
            return LOW;
        }
        return eligible ? ELIGIBLE : NORMAL;
    }
}
