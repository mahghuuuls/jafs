package com.mahghuuuls.jafs.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Eligibility, new slot, refresh, ineligible duplicate, replacement, tie-break (REQ-015 to REQ-019). */
class MealRulesTest {

    private static final int LIMIT = 3;
    private static final int THRESHOLD = 50;

    private static FoodSlot slot(String id, int remainingSeconds, int fullSeconds) {
        return new FoodSlot(id, remainingSeconds * 20, fullSeconds * 20);
    }

    private static List<FoodSlot> slots(FoodSlot... slots) {
        return new ArrayList<FoodSlot>(Arrays.asList(slots));
    }

    @Test
    void eligibilityIsStrictlyBelowTheThreshold() {
        assertFalse(MealRules.isEligible(slot("a", 50, 100), THRESHOLD));
        assertTrue(MealRules.isEligible(slot("a", 49, 100), THRESHOLD));
        assertFalse(MealRules.isEligible(new FoodSlot("a", 1000, 2000), THRESHOLD));
        assertTrue(MealRules.isEligible(new FoodSlot("a", 999, 2000), THRESHOLD));
        assertTrue(MealRules.isEligible(slot("a", 99, 100), 100));
        assertFalse(MealRules.isEligible(slot("a", 100, 100), 100));
        assertFalse(MealRules.isEligible(slot("a", 1, 100), 1));
        assertTrue(MealRules.isEligible(slot("a", 0, 100), 1));
    }

    @Test
    void newFoodTakesAnEmptySlot() {
        MealRules.Outcome outcome = MealRules.decide(slots(slot("steak", 1200, 1200)), "bread", LIMIT, THRESHOLD);
        assertEquals(MealRules.Kind.NEW_SLOT, outcome.kind());
        assertEquals(1, outcome.slot());
        assertTrue(outcome.accepted());
    }

    @Test
    void ineligibleDuplicateIsRefusedEvenWithAnEmptySlot() {
        MealRules.Outcome outcome = MealRules.decide(slots(slot("steak", 1080, 1200)), "steak", LIMIT, THRESHOLD);
        assertEquals(MealRules.Kind.REFUSE_INELIGIBLE, outcome.kind());
        assertFalse(outcome.accepted());
    }

    @Test
    void eligibleDuplicateRefreshesItsOwnSlot() {
        MealRules.Outcome outcome = MealRules.decide(slots(slot("bread", 1800, 1800), slot("steak", 599, 1200)), "steak", LIMIT, THRESHOLD);
        assertEquals(MealRules.Kind.REFRESH, outcome.kind());
        assertEquals(1, outcome.slot());
    }

    @Test
    void fullSlotsWithNothingEligibleRefuse() {
        List<FoodSlot> full = slots(slot("steak", 1000, 1200), slot("bread", 1700, 1800), slot("carrot", 800, 900));
        MealRules.Outcome outcome = MealRules.decide(full, "stew", LIMIT, THRESHOLD);
        assertEquals(MealRules.Kind.REFUSE_NO_SLOT, outcome.kind());
        assertEquals(-1, outcome.slot());
    }

    @Test
    void fullSlotsReplaceTheEligibleFoodWithTheLeastTime() {
        // REQ-019: remaining 2:00, 8:00, 9:00 of long full durations; all eligible; the least time wins
        List<FoodSlot> full = slots(new FoodSlot("steak", 120 * 20, 1200 * 20), new FoodSlot("bread", 480 * 20, 1800 * 20),
                new FoodSlot("carrot", 540 * 20, 1800 * 20));
        MealRules.Outcome outcome = MealRules.decide(full, "stew", LIMIT, THRESHOLD);
        assertEquals(MealRules.Kind.REPLACE, outcome.kind());
        assertEquals(0, outcome.slot());
    }

    @Test
    void replacementIgnoresIneligibleFoodsEvenWhenTheyHaveLessTime() {
        // slot 0 has the least time but is above its threshold (short full duration); slot 2 is eligible
        List<FoodSlot> full = slots(new FoodSlot("a", 50 * 20, 60 * 20), new FoodSlot("b", 400 * 20, 500 * 20),
                new FoodSlot("c", 100 * 20, 1000 * 20));
        MealRules.Outcome outcome = MealRules.decide(full, "d", LIMIT, THRESHOLD);
        assertEquals(MealRules.Kind.REPLACE, outcome.kind());
        assertEquals(2, outcome.slot());
    }

    @Test
    void equalRemainingTimesReplaceTheLowestSlot() {
        List<FoodSlot> full = slots(new FoodSlot("a", 100, 1000), new FoodSlot("b", 100, 1000), new FoodSlot("c", 100, 1000));
        assertEquals(0, MealRules.decide(full, "d", LIMIT, THRESHOLD).slot());
        List<FoodSlot> full2 = slots(new FoodSlot("a", 900, 1000), new FoodSlot("b", 100, 1000), new FoodSlot("c", 100, 1000));
        assertEquals(1, MealRules.decide(full2, "d", LIMIT, THRESHOLD).slot());
    }

    @Test
    void requirementExampleTenFortyFortyFiveOfOneHundred() {
        // REQ-019: remaining 10, 40, 45 seconds of 100 at 50 percent: the 10-second food is replaced
        List<FoodSlot> full = slots(slot("a", 10, 100), slot("b", 40, 100), slot("c", 45, 100));
        MealRules.Outcome outcome = MealRules.decide(full, "d", LIMIT, THRESHOLD);
        assertEquals(MealRules.Kind.REPLACE, outcome.kind());
        assertEquals(0, outcome.slot());
        // all three above the threshold: refused
        List<FoodSlot> none = slots(slot("a", 60, 100), slot("b", 70, 100), slot("c", 80, 100));
        assertEquals(MealRules.Kind.REFUSE_NO_SLOT, MealRules.decide(none, "d", LIMIT, THRESHOLD).kind());
    }

    @Test
    void limitOneReplacesInsteadOfFillingASecondSlot() {
        assertEquals(MealRules.Kind.REPLACE, MealRules.decide(slots(new FoodSlot("a", 10, 1000)), "b", 1, THRESHOLD).kind());
        assertEquals(MealRules.Kind.REFUSE_NO_SLOT, MealRules.decide(slots(new FoodSlot("a", 900, 1000)), "b", 1, THRESHOLD).kind());
    }
}
