package com.mahghuuuls.jafs.core;

import com.mahghuuuls.jafs.api.PropertyOperation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** REQ-071 on a slot fixture: removal, modifier change, shorter and longer full durations, inert definitions. */
class ReloadRulesTest {

    private static FoodDefinition def(String id, int seconds, double flat) {
        List<Modifier> modifiers = flat == 0 ? Collections.<Modifier>emptyList()
                : Collections.singletonList(new Modifier("jafs:max_health", PropertyOperation.FLAT, flat));
        return new FoodDefinition(id, Collections.singletonList(Matcher.item("minecraft:" + id, Matcher.ANY_META,
                Collections.<String, String>emptyMap())), seconds, 0, modifiers);
    }

    private static List<FoodSlot> slots() {
        return new ArrayList<FoodSlot>(Arrays.asList(
                new FoodSlot("steak", 12 * 60 * 20, 20 * 60 * 20),
                new FoodSlot("bread", 100 * 20, 1800 * 20)));
    }

    @Test
    void aRemovedDefinitionClearsItsSlot() {
        List<FoodSlot> slots = slots();
        DefinitionSet set = new DefinitionSet(2, Collections.singletonList(def("bread", 1800, 1)));
        List<ReloadRules.Change> changes = ReloadRules.apply(slots, set);
        assertEquals(1, slots.size());
        assertEquals("bread", slots.get(0).definitionId());
        assertEquals(1, changes.size());
        assertEquals(ReloadRules.Kind.REMOVED, changes.get(0).kind);
        assertEquals("steak", changes.get(0).definitionId);
    }

    @Test
    void anInertDefinitionClearsItsSlotToo() {
        // A definition left without a valid modifier grants no active food, so its instance goes
        List<FoodSlot> slots = slots();
        DefinitionSet set = new DefinitionSet(2, Arrays.asList(def("steak", 1200, 0), def("bread", 1800, 1)));
        List<ReloadRules.Change> changes = ReloadRules.apply(slots, set);
        assertEquals(1, slots.size());
        assertEquals(ReloadRules.Kind.REMOVED, changes.get(0).kind);
    }

    @Test
    void aChangedModifierLeavesTheTimerAlone() {
        // +4 became +6: the recompute is the caller's job; remaining and full stay (REQ-046)
        List<FoodSlot> slots = slots();
        DefinitionSet set = new DefinitionSet(2, Arrays.asList(def("steak", 1200, 6), def("bread", 1800, 1)));
        List<ReloadRules.Change> changes = ReloadRules.apply(slots, set);
        assertTrue(changes.isEmpty());
        assertEquals(12 * 60 * 20, slots.get(0).remainingTicks());
        assertEquals(20 * 60 * 20, slots.get(0).fullTicks());
    }

    @Test
    void aShorterFullDurationCapsTheRemainingTimeAndEligibility() {
        // 20:00 -> 5:00 with 12:00 left: remaining becomes 5:00, which is 100 percent, so not eligible
        List<FoodSlot> slots = slots();
        DefinitionSet set = new DefinitionSet(2, Arrays.asList(def("steak", 300, 4), def("bread", 1800, 1)));
        List<ReloadRules.Change> changes = ReloadRules.apply(slots, set);
        assertEquals(1, changes.size());
        assertEquals(ReloadRules.Kind.CAPPED, changes.get(0).kind);
        assertEquals(300 * 20, slots.get(0).remainingTicks());
        assertEquals(300 * 20, slots.get(0).fullTicks());
        assertFalse(MealRules.isEligible(slots.get(0), 50));
    }

    @Test
    void aLongerFullDurationKeepsTheRemainingTimeAndMovesEligibility() {
        // bread with 100 s left of 30:00 is eligible (5 percent left); its full duration becomes 2:30 and 100 s of
        // 150 s (67 percent left) is no longer eligible: the remaining time is kept, only the ratio moved (REQ-046)
        List<FoodSlot> slots = slots();
        assertTrue(MealRules.isEligible(slots.get(1), 50));
        DefinitionSet set = new DefinitionSet(2, Arrays.asList(def("steak", 1200, 4), def("bread", 150, 1)));
        List<ReloadRules.Change> changes = ReloadRules.apply(slots, set);
        assertEquals(1, changes.size());
        assertEquals(ReloadRules.Kind.RESIZED, changes.get(0).kind);
        assertEquals(100 * 20, slots.get(1).remainingTicks());
        assertEquals(150 * 20, slots.get(1).fullTicks());
        assertFalse(MealRules.isEligible(slots.get(1), 50));
        // the steak at 12:00 of 20:00 (60 percent left) stays ineligible
        assertFalse(MealRules.isEligible(slots.get(0), 50));
    }

    @Test
    void anUnchangedSetChangesNothing() {
        List<FoodSlot> slots = slots();
        DefinitionSet set = new DefinitionSet(2, Arrays.asList(def("steak", 1200, 4), def("bread", 1800, 1)));
        assertTrue(ReloadRules.apply(slots, set).isEmpty());
        assertEquals(2, slots.size());
    }

    @Test
    void anEmptySetClearsEverything() {
        List<FoodSlot> slots = slots();
        List<ReloadRules.Change> changes = ReloadRules.apply(slots, DefinitionSet.empty());
        assertTrue(slots.isEmpty());
        assertEquals(2, changes.size());
    }
}
