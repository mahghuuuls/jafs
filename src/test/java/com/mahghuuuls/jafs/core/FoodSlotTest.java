package com.mahghuuuls.jafs.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FoodSlotTest {

    @Test
    void remainingIsClampedToFullAndZero() {
        FoodSlot slot = new FoodSlot("a:b", 500, 100);
        assertEquals(100, slot.remainingTicks());
        slot.setRemainingTicks(-5);
        assertEquals(0, slot.remainingTicks());
        assertTrue(slot.isExpired());
    }

    @Test
    void invalidArgumentsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new FoodSlot("", 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new FoodSlot("a:b", 1, 0));
    }
}
