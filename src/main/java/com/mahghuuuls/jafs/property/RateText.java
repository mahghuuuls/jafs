package com.mahghuuuls.jafs.property;

/** The one way a rate percent is written for operators: a sign for a positive value, no decimals when whole, a percent sign. */
public final class RateText {

    private RateText() {
    }

    /** {@code +50%}, {@code -30%}, {@code 0%}, {@code +12.5%}. */
    public static String signedPercent(double percent) {
        String text = percent == Math.rint(percent) ? Long.toString((long) percent) : Double.toString(percent);
        return (percent > 0 ? "+" : "") + text + "%";
    }
}
