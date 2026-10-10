package com.mahghuuuls.jafs.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Depletion at a rate with carry, expiration, payments, and the reload cap (REQ-043 to REQ-048, REQ-071). */
class FoodTimersTest {

    private static List<FoodSlot> slots(FoodSlot... slots) {
        return new ArrayList<FoodSlot>(Arrays.asList(slots));
    }

    private static int ticksOver(FoodTimers.Clock clock, double rate, int gameTicks) {
        int total = 0;
        for (int i = 0; i < gameTicks; i++) {
            total += clock.step(rate);
        }
        return total;
    }

    @Test
    void rateOneDepletesOneTickPerTick() {
        assertEquals(200, ticksOver(new FoodTimers.Clock(), 1.0, 200));
    }

    @Test
    void rateTwoDepletesTwiceAsFast() {
        // REQ-043: rate 2.0 over 10 seconds loses 20 seconds
        assertEquals(400, ticksOver(new FoodTimers.Clock(), 2.0, 200));
    }

    @Test
    void fractionalRatesCarryExactly() {
        assertEquals(100, ticksOver(new FoodTimers.Clock(), 0.5, 200));
        assertEquals(160, ticksOver(new FoodTimers.Clock(), 0.8, 200));
        assertEquals(300, ticksOver(new FoodTimers.Clock(), 1.5, 200));
    }

    @Test
    void rateZeroPausesAndDropsTheCarry() {
        FoodTimers.Clock clock = new FoodTimers.Clock();
        clock.step(0.9);
        assertEquals(0, clock.step(0));
        assertEquals(0, clock.step(0.9));
        assertEquals(1, clock.step(0.9));
    }

    @Test
    void advanceReportsExpirationsInSlotOrderAndClampsAtZero() {
        List<FoodSlot> slots = slots(new FoodSlot("a", 3, 100), new FoodSlot("b", 50, 100), new FoodSlot("c", 2, 100));
        List<FoodSlot> expired = FoodTimers.advance(slots, 3);
        assertEquals(2, expired.size());
        assertEquals("a", expired.get(0).definitionId());
        assertEquals("c", expired.get(1).definitionId());
        assertEquals(47, slots.get(1).remainingTicks());
        assertEquals(0, slots.get(0).remainingTicks());
    }

    @Test
    void paymentSucceedsWhenOneFoodCoversTheCost() {
        // REQ-048: foods at 20 s and 5 s, request 10 s: succeed; 10 and 0 (expired)
        List<FoodSlot> slots = slots(new FoodSlot("a", 400, 1000), new FoodSlot("b", 100, 1000));
        List<FoodSlot> expired = FoodTimers.pay(slots, 200);
        assertEquals(1, expired.size());
        assertEquals("b", expired.get(0).definitionId());
        assertEquals(200, slots.get(0).remainingTicks());
        assertEquals(0, slots.get(1).remainingTicks());
    }

    @Test
    void paymentFailsWhenNoFoodCoversTheCostAndChangesNothing() {
        List<FoodSlot> slots = slots(new FoodSlot("a", 160, 1000), new FoodSlot("b", 100, 1000));
        assertFalse(FoodTimers.canPay(slots, 200));
        assertNull(FoodTimers.pay(slots, 200));
        assertEquals(160, slots.get(0).remainingTicks());
        assertEquals(100, slots.get(1).remainingTicks());
    }

    @Test
    void paymentWithNoFoodsFailsAndZeroCostAlwaysSucceeds() {
        assertFalse(FoodTimers.canPay(slots(), 1));
        assertTrue(FoodTimers.canPay(slots(), 0));
        assertTrue(FoodTimers.pay(slots(new FoodSlot("a", 1, 10)), 0).isEmpty());
    }

    @Test
    void clientCountdownFollowsTheSnapshotRateAndNeverGoesNegative() {
        assertEquals(1000, FoodTimers.remainingAfter(1200, 200, 1.0));
        assertEquals(800, FoodTimers.remainingAfter(1200, 200, 2.0));
        assertEquals(1100, FoodTimers.remainingAfter(1200, 200, 0.5));
        assertEquals(1200, FoodTimers.remainingAfter(1200, 200, 0.0));
        assertEquals(0, FoodTimers.remainingAfter(100, 200, 1.0));
        assertEquals(1200, FoodTimers.remainingAfter(1200, -5, 1.0));
    }

    @Test
    void pendingMealsExpireOnCancelAndAreTakenOnce() {
        PendingMeals<String> pending = new PendingMeals<String>();
        pending.begin("steve", "steak");
        pending.begin("steve", "bread");
        assertEquals("bread", pending.take("steve"));
        assertNull(pending.take("steve"));
        pending.begin("steve", "steak");
        pending.cancel("steve");
        assertFalse(pending.has("steve"));
        pending.begin("alex", "stew");
        assertEquals(1, pending.size());
        assertNull(pending.take("steve"));
        assertEquals("stew", pending.take("alex"));
    }

    @Test
    void shrinkingTheFullDurationCapsRemaining() {
        FoodSlot capped = FoodTimers.withFullTicks(new FoodSlot("a", 12 * 60 * 20, 20 * 60 * 20), 5 * 60 * 20);
        assertEquals(5 * 60 * 20, capped.remainingTicks());
        assertEquals(5 * 60 * 20, capped.fullTicks());
        assertFalse(MealRules.isEligible(capped, 50));
        FoodSlot untouched = FoodTimers.withFullTicks(new FoodSlot("a", 100, 1000), 2000);
        assertEquals(100, untouched.remainingTicks());
    }
}
