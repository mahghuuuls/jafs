package com.mahghuuuls.jafs.core;

import com.mahghuuuls.jafs.api.PropertyOperation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Percent versus flat amounts are told apart, and durations read as clock times (REQ-062, REQ-065). */
class DisplayFormatTest {

    @Test
    void amounts() {
        assertEquals("+4 HP", DisplayFormat.amount(4, PropertyOperation.FLAT, "HP"));
        assertEquals("-2 HP", DisplayFormat.amount(-2, PropertyOperation.FLAT, "HP"));
        assertEquals("+20%", DisplayFormat.amount(20, PropertyOperation.PERCENT, "HP"));
        assertEquals("+5 pp", DisplayFormat.amount(5, PropertyOperation.FLAT, "pp"));
        assertEquals("+2.5", DisplayFormat.amount(2.5, PropertyOperation.FLAT, ""));
        assertEquals("-12.5%", DisplayFormat.amount(-12.5, PropertyOperation.PERCENT, ""));
    }

    @Test
    void compactDurationsReadAsSecondsMinutesOrHours() {
        assertEquals("45s", DisplayFormat.compactDuration(45));
        assertEquals("59s", DisplayFormat.compactDuration(59));
        assertEquals("1m", DisplayFormat.compactDuration(60));
        assertEquals("1m", DisplayFormat.compactDuration(119));
        assertEquals("19m", DisplayFormat.compactDuration(19 * 60 + 45));
        assertEquals("59m", DisplayFormat.compactDuration(3599));
        assertEquals("1h00", DisplayFormat.compactDuration(3600));
        assertEquals("1h05", DisplayFormat.compactDuration(3600 + 5 * 60 + 30));
        assertEquals("12h30", DisplayFormat.compactDuration(12 * 3600 + 30 * 60));
        assertEquals("0s", DisplayFormat.compactDuration(-3));
        assertEquals("1s", DisplayFormat.compactDurationTicks(1));
        assertEquals("20m", DisplayFormat.compactDurationTicks(24000));
    }

    @Test
    void valueTextIsSharedBetweenFileAndItems() {
        assertEquals("1", ValueText.ofNumber(1.0));
        assertEquals("3", ValueText.ofNumber(3));
        assertEquals("2.5", ValueText.ofNumber(2.5));
        assertEquals("-7", ValueText.ofLong(-7));
        assertEquals("1", ValueText.ofBoolean(true));
        assertEquals("0", ValueText.ofBoolean(false));
    }

    @Test
    void durations() {
        assertEquals("20:00", DisplayFormat.duration(1200));
        assertEquals("0:03", DisplayFormat.duration(3));
        assertEquals("1:00:00", DisplayFormat.duration(3600));
        assertEquals("24:00:00", DisplayFormat.duration(86400));
        assertEquals("0:01", DisplayFormat.durationTicks(1));
        assertEquals("0:00", DisplayFormat.durationTicks(0));
    }
}
