package com.mahghuuuls.jafs.core;

/**
 * Owner of how NBT and definition-file values are written as comparable text. Both the file
 * reader and the stack reader use it, so a matcher value and an item's field compare the same
 * way: integral numbers as plain integers ({@code 1} for a byte flag, {@code 3} for a short or
 * an int), fractional numbers in Java's shortest form ({@code 2.5}), strings as they are.
 */
public final class ValueText {

    private ValueText() {
    }

    public static String ofNumber(double value) {
        if (value == Math.rint(value) && !Double.isInfinite(value) && Math.abs(value) < 1e15) {
            return Long.toString((long) value);
        }
        return Double.toString(value);
    }

    public static String ofLong(long value) {
        return Long.toString(value);
    }

    public static String ofBoolean(boolean value) {
        return value ? "1" : "0";
    }
}
