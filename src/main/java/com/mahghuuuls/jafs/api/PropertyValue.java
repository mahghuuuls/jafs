package com.mahghuuuls.jafs.api;

/**
 * The combined value of one property for one player (REQ-059): the flat sum and the percent
 * sum of every active food's modifiers. For the two rate properties the applier also receives
 * the addon contributions of {@link IRateModifierProvider}s, which are not part of this value.
 */
public final class PropertyValue {

    public static final PropertyValue ZERO = new PropertyValue(0, 0, 0);

    private final double flat;
    private final double percent;
    private final int contributions;

    public PropertyValue(double flat, double percent, int contributions) {
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

    /** How many modifiers contributed; zero means no active food modifies the property. */
    public int contributions() {
        return contributions;
    }

    @Override
    public String toString() {
        return "flat=" + flat + " percent=" + percent + " from=" + contributions;
    }
}
