package com.mahghuuuls.jafs.core;

import com.mahghuuuls.jafs.api.PropertyOperation;

import java.util.Locale;

/**
 * Owner of how amounts and durations are written for players: a percent change as {@code +20%},
 * a flat change as {@code +4 HP} (the unit follows the number), durations as {@code m:ss} or
 * {@code h:mm:ss} (tooltips, inspect), and the HUD's compact time left (REV-012): {@code 45s}
 * under a minute, {@code 19m} under an hour (whole minutes, rounded down), {@code 1h05} from an
 * hour on.
 */
public final class DisplayFormat {

    private DisplayFormat() {
    }

    public static String amount(double amount, PropertyOperation operation, String unit) {
        String number = amount == Math.rint(amount) ? String.valueOf((long) amount) : String.format(Locale.ROOT, "%.1f", amount);
        String signed = amount >= 0 ? "+" + number : number;
        if (operation == PropertyOperation.PERCENT) {
            return signed + "%";
        }
        return unit == null || unit.isEmpty() ? signed : signed + " " + unit;
    }

    public static String duration(int seconds) {
        if (seconds < 0) {
            seconds = 0;
        }
        int h = seconds / 3600;
        int m = (seconds % 3600) / 60;
        int s = seconds % 60;
        return h > 0 ? String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s) : String.format(Locale.ROOT, "%d:%02d", m, s);
    }

    public static String durationTicks(int ticks) {
        return duration((ticks + FoodDefinition.TICKS_PER_SECOND - 1) / FoodDefinition.TICKS_PER_SECOND);
    }

    /** The HUD's short form: seconds under a minute, whole minutes under an hour, hours and minutes after. */
    public static String compactDuration(int seconds) {
        if (seconds < 0) {
            seconds = 0;
        }
        if (seconds < 60) {
            return seconds + "s";
        }
        if (seconds < 3600) {
            return (seconds / 60) + "m";
        }
        return String.format(Locale.ROOT, "%dh%02d", seconds / 3600, (seconds % 3600) / 60);
    }

    public static String compactDurationTicks(int ticks) {
        return compactDuration((ticks + FoodDefinition.TICKS_PER_SECOND - 1) / FoodDefinition.TICKS_PER_SECOND);
    }
}
