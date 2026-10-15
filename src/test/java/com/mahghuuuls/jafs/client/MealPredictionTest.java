package com.mahghuuuls.jafs.client;

import com.mahghuuuls.jafs.core.FoodSlot;
import com.mahghuuuls.jafs.core.MealRules;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The client predicts with the server's rule on its counted-down slots (REQ-081a). */
class MealPredictionTest {

    private static final int LIMIT = 3;
    private static final int THRESHOLD = 50;

    private static FoodSlot slot(String id, int remaining, int full) {
        return new FoodSlot(id, remaining, full);
    }

    @Test
    void anActiveFoodAboveTheThresholdIsRefused() {
        List<FoodSlot> slots = Collections.singletonList(slot("steak", 20000, 24000));
        assertTrue(MealPrediction.wouldRefuse(slots, "steak", LIMIT, THRESHOLD));
        assertEquals(MealRules.Kind.REFUSE_INELIGIBLE, MealPrediction.predict(slots, "steak", LIMIT, THRESHOLD).kind());
    }

    @Test
    void anActiveFoodBelowTheThresholdRefreshes() {
        List<FoodSlot> slots = Collections.singletonList(slot("steak", 11000, 24000));
        assertFalse(MealPrediction.wouldRefuse(slots, "steak", LIMIT, THRESHOLD));
        assertEquals(MealRules.Kind.REFRESH, MealPrediction.predict(slots, "steak", LIMIT, THRESHOLD).kind());
    }

    @Test
    void aFreeSlotAcceptsANewFood() {
        List<FoodSlot> slots = Arrays.asList(slot("steak", 20000, 24000), slot("bread", 30000, 36000));
        assertFalse(MealPrediction.wouldRefuse(slots, "golden_carrot", LIMIT, THRESHOLD));
    }

    @Test
    void aFullSetWithNothingEligibleIsRefused() {
        List<FoodSlot> slots = Arrays.asList(slot("steak", 20000, 24000), slot("bread", 30000, 36000), slot("stew", 10000, 12000));
        assertTrue(MealPrediction.wouldRefuse(slots, "golden_carrot", LIMIT, THRESHOLD));
        assertEquals(MealRules.Kind.REFUSE_NO_SLOT, MealPrediction.predict(slots, "golden_carrot", LIMIT, THRESHOLD).kind());
    }

    @Test
    void aFullSetWithAnEligibleFoodReplaces() {
        List<FoodSlot> slots = Arrays.asList(slot("steak", 20000, 24000), slot("bread", 5000, 36000), slot("stew", 10000, 12000));
        assertFalse(MealPrediction.wouldRefuse(slots, "golden_carrot", LIMIT, THRESHOLD));
        assertEquals(MealRules.Kind.REPLACE, MealPrediction.predict(slots, "golden_carrot", LIMIT, THRESHOLD).kind());
    }

    @Test
    void slotsAreRebuiltFromTheSnapshotCountedDownAtTheRate() {
        List<com.mahghuuuls.jafs.network.FoodStateSnapshotMessage.Entry> entries = Arrays.asList(
                new com.mahghuuuls.jafs.network.FoodStateSnapshotMessage.Entry("steak", 12100, 24000, false),
                new com.mahghuuuls.jafs.network.FoodStateSnapshotMessage.Entry("bread", 50, 36000, true));
        List<FoodSlot> slots = MealPrediction.slotsAfter(entries, 200, 1.0);
        assertEquals("steak", slots.get(0).definitionId());
        assertEquals(11900, slots.get(0).remainingTicks());
        assertEquals(24000, slots.get(0).fullTicks());
        assertEquals(0, slots.get(1).remainingTicks());
        // the snapshot said not eligible; counted down past the threshold it refreshes
        assertFalse(MealPrediction.wouldRefuse(slots, "steak", LIMIT, THRESHOLD));
        assertEquals(12000, MealPrediction.slotsAfter(entries, 200, 0.5).get(0).remainingTicks());
        assertEquals(12100, MealPrediction.slotsAfter(entries, 200, 0.0).get(0).remainingTicks());
    }

    @Test
    void theCountedDownRemainingDecidesNotTheSnapshotFlag() {
        // 12000 of 24000 is exactly the threshold: not yet eligible; one tick less is.
        assertTrue(MealPrediction.wouldRefuse(Collections.singletonList(slot("steak", 12000, 24000)), "steak", LIMIT, THRESHOLD));
        assertFalse(MealPrediction.wouldRefuse(Collections.singletonList(slot("steak", 11999, 24000)), "steak", LIMIT, THRESHOLD));
    }
}
