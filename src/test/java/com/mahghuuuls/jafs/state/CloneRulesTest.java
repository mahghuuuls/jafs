package com.mahghuuuls.jafs.state;

import com.mahghuuuls.jafs.core.FoodSlot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** REQ-088 and the delay reset of REQ-041 on a state fixture. */
class CloneRulesTest {

    private static FoodState original() {
        FoodState state = new FoodState();
        state.slots().add(new FoodSlot("steak", 12 * 60 * 20, 20 * 60 * 20));
        state.slots().add(new FoodSlot("bread", 100, 36000));
        state.setRecoveryDelayTicks(140);
        return state;
    }

    @Test
    void aDeathWithDefaultsClearsFoodsAndTheDelay() {
        FoodState clone = new FoodState();
        assertEquals(CloneRules.Result.CLEARED, CloneRules.apply(original(), clone, true, false));
        assertTrue(clone.slots().isEmpty());
        assertEquals(0, clone.recoveryDelayTicks());
    }

    @Test
    void aDeathWithKeepFoodsCopiesTheFoodsButResetsTheDelay() {
        FoodState clone = new FoodState();
        assertEquals(CloneRules.Result.KEPT, CloneRules.apply(original(), clone, true, true));
        assertEquals(2, clone.slots().size());
        assertEquals(12 * 60 * 20, clone.slots().get(0).remainingTicks());
        assertEquals(20 * 60 * 20, clone.slots().get(0).fullTicks());
        assertEquals(0, clone.recoveryDelayTicks());
    }

    @Test
    void anEndReturnCopiesEverything() {
        FoodState clone = new FoodState();
        assertEquals(CloneRules.Result.COPIED, CloneRules.apply(original(), clone, false, false));
        assertEquals(2, clone.slots().size());
        assertEquals(140, clone.recoveryDelayTicks());
    }

    @Test
    void theCopyIsIndependentOfTheOriginal() {
        FoodState source = original();
        FoodState clone = new FoodState();
        CloneRules.apply(source, clone, false, true);
        source.slots().clear();
        assertEquals(2, clone.slots().size());
    }
}
