package com.mahghuuuls.jafs.core;

import com.mahghuuuls.jafs.api.PropertyOperation;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Owner of how modifiers combine: per property, flat amounts add together and percent amounts
 * add together; the percent sum then applies once to the provider's base (which includes the
 * flat sum), so two percent bonuses never compound with each other.
 */
public final class BonusMath {

    private BonusMath() {
    }

    /** The combined modifiers of one property. */
    public static final class Combined {
        public static final Combined ZERO = new Combined(0, 0, 0);

        private final double flat;
        private final double percent;
        private final int contributions;

        public Combined(double flat, double percent, int contributions) {
            this.flat = flat;
            this.percent = percent;
            this.contributions = contributions;
        }

        public double flat() {
            return flat;
        }

        public double percent() {
            return percent;
        }

        public int contributions() {
            return contributions;
        }

        public boolean isZero() {
            return flat == 0 && percent == 0;
        }

        /** The property's value for a given base: {@code (base + flat) * (1 + percent / 100)}. */
        public double apply(double base) {
            return (base + flat) * (1.0 + percent / 100.0);
        }

        public Combined plus(Combined other) {
            return new Combined(flat + other.flat, percent + other.percent, contributions + other.contributions);
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Combined && ((Combined) o).flat == flat && ((Combined) o).percent == percent;
        }

        @Override
        public int hashCode() {
            return Double.hashCode(flat) * 31 + Double.hashCode(percent);
        }

        @Override
        public String toString() {
            return "flat=" + flat + " percent=" + percent + " from=" + contributions;
        }
    }

    /** Combines modifiers by property id, in first-seen order. */
    public static Map<String, Combined> combine(List<Modifier> modifiers) {
        Map<String, Combined> result = new LinkedHashMap<String, Combined>();
        for (Modifier modifier : modifiers) {
            Combined current = result.get(modifier.propertyId());
            if (current == null) {
                current = Combined.ZERO;
            }
            Combined added = modifier.operation() == PropertyOperation.FLAT
                    ? new Combined(modifier.amount(), 0, 1)
                    : new Combined(0, modifier.amount(), 1);
            result.put(modifier.propertyId(), current.plus(added));
        }
        return result;
    }

    /**
     * The base depletion rate before food modifiers: the configured rate plus the Hunger effect level
     * times the configured increment (level I adds once, level II twice; REQ-045), never below zero.
     */
    public static double baseDepletionRate(double configRate, int hungerLevel, double increment) {
        double base = configRate + Math.max(0, hungerLevel) * increment;
        return base < 0 ? 0 : base;
    }

    /** A rate multiplier from a percent sum, never below zero: {@code max(0, 1 + percent / 100)}. */
    public static double rateMultiplier(double percent) {
        double multiplier = 1.0 + percent / 100.0;
        return multiplier < 0 ? 0 : multiplier;
    }

    /** The health to set after the maximum changed: the current value capped at the new maximum. */
    public static float cappedHealth(float current, float maximum) {
        return current > maximum ? maximum : current;
    }
}
