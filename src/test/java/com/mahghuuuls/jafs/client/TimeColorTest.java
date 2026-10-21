package com.mahghuuuls.jafs.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** White, yellow once eligible, red in the last minute; red wins; paused keeps the eligibility color (REQ-064). */
class TimeColorTest {

    @Test
    void freshFoodIsWhite() {
        assertEquals(TimeColor.NORMAL, TimeColor.of(false, 20 * 60 * 20));
        assertEquals(TimeColor.NORMAL, TimeColor.of(false, TimeColor.LOW_TICKS));
    }

    @Test
    void eligibleFoodIsYellow() {
        assertEquals(TimeColor.ELIGIBLE, TimeColor.of(true, 5 * 60 * 20));
        assertEquals(TimeColor.ELIGIBLE, TimeColor.of(true, TimeColor.LOW_TICKS));
    }

    @Test
    void lastMinuteIsRedWhateverTheEligibility() {
        assertEquals(TimeColor.LOW, TimeColor.of(true, TimeColor.LOW_TICKS - 1));
        assertEquals(TimeColor.LOW, TimeColor.of(false, 30 * 20));
        assertEquals(TimeColor.LOW, TimeColor.of(true, 0));
    }

    @Test
    void pausedKeepsTheEligibilityColor() {
        assertEquals(TimeColor.NORMAL, TimeColor.of(false, -1));
        assertEquals(TimeColor.ELIGIBLE, TimeColor.of(true, -1));
    }

    @Test
    void colorsAreTheVanillaChatColors() {
        assertEquals(0xFFFFFFFF, TimeColor.NORMAL.argb);
        assertEquals(0xFFFFFF55, TimeColor.ELIGIBLE.argb);
        assertEquals(0xFFFF5555, TimeColor.LOW.argb);
        assertEquals(1200, TimeColor.LOW_TICKS);
    }
}
