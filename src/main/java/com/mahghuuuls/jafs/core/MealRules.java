package com.mahghuuuls.jafs.core;

import java.util.List;

/**
 * Owner of the slot rules: whether a meal is accepted and what it does. A food is eligible when
 * its remaining time is strictly below the threshold fraction of its full time. An active food
 * that is eligible is refreshed; one that is not is refused. A new food takes an empty slot, or
 * replaces the eligible food with the least remaining time (lowest slot on a tie), or is refused.
 */
public final class MealRules {

    private MealRules() {
    }

    public enum Kind {
        NEW_SLOT,
        REFRESH,
        REPLACE,
        REFUSE_INELIGIBLE,
        REFUSE_NO_SLOT
    }

    /** The decision for one meal. {@code slot} is the affected slot index, or -1 for a refusal. */
    public static final class Outcome {
        private final Kind kind;
        private final int slot;

        private Outcome(Kind kind, int slot) {
            this.kind = kind;
            this.slot = slot;
        }

        public Kind kind() {
            return kind;
        }

        public int slot() {
            return slot;
        }

        public boolean accepted() {
            return kind == Kind.NEW_SLOT || kind == Kind.REFRESH || kind == Kind.REPLACE;
        }

        @Override
        public String toString() {
            return kind + (slot >= 0 ? "[" + slot + "]" : "");
        }
    }

    public static boolean isEligible(FoodSlot slot, int thresholdPercent) {
        // remaining / full < threshold / 100, in integers to avoid rounding at the boundary
        return (long) slot.remainingTicks() * 100L < (long) thresholdPercent * (long) slot.fullTicks();
    }

    public static Outcome decide(List<FoodSlot> slots, String definitionId, int activeFoodLimit, int thresholdPercent) {
        for (int i = 0; i < slots.size(); i++) {
            FoodSlot slot = slots.get(i);
            if (slot.definitionId().equals(definitionId)) {
                return isEligible(slot, thresholdPercent) ? new Outcome(Kind.REFRESH, i) : new Outcome(Kind.REFUSE_INELIGIBLE, i);
            }
        }
        if (slots.size() < activeFoodLimit) {
            return new Outcome(Kind.NEW_SLOT, slots.size());
        }
        int best = -1;
        for (int i = 0; i < slots.size(); i++) {
            FoodSlot slot = slots.get(i);
            if (!isEligible(slot, thresholdPercent)) {
                continue;
            }
            if (best < 0 || slot.remainingTicks() < slots.get(best).remainingTicks()) {
                best = i;
            }
        }
        return best < 0 ? new Outcome(Kind.REFUSE_NO_SLOT, -1) : new Outcome(Kind.REPLACE, best);
    }
}
