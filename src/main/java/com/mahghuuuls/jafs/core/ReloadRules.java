package com.mahghuuuls.jafs.core;

import java.util.ArrayList;
import java.util.List;

/**
 * What a successful reload does to a player's active foods (REQ-071): a definition that is gone,
 * or no longer grants an active food, clears its instances; a changed full duration caps the
 * remaining time at the new full duration (eligibility follows, since it is derived from the two);
 * a changed modifier set needs no slot change, only a bonus recompute, which the caller does.
 * Timers are never refilled or rescaled (REQ-046).
 */
public final class ReloadRules {

    public enum Kind {
        /** The definition is gone, disabled, or inert: the slot was removed. */
        REMOVED,
        /** The full duration changed and the remaining time was cut to it. */
        CAPPED,
        /** The full duration changed; the remaining time still fit under it. */
        RESIZED
    }

    public static final class Change {
        public final Kind kind;
        public final String definitionId;
        public final int remainingTicks;
        public final int fullTicks;

        Change(Kind kind, String definitionId, int remainingTicks, int fullTicks) {
            this.kind = kind;
            this.definitionId = definitionId;
            this.remainingTicks = remainingTicks;
            this.fullTicks = fullTicks;
        }
    }

    private ReloadRules() {
    }

    /** Applies the new set to the slots in place and returns what changed, in slot order. */
    public static List<Change> apply(List<FoodSlot> slots, DefinitionSet set) {
        List<Change> changes = new ArrayList<Change>();
        for (int i = 0; i < slots.size(); i++) {
            FoodSlot slot = slots.get(i);
            FoodDefinition definition = set.get(slot.definitionId());
            if (definition == null || !definition.hasModifiers()) {
                slots.remove(i--);
                changes.add(new Change(Kind.REMOVED, slot.definitionId(), 0, slot.fullTicks()));
                continue;
            }
            if (definition.fullTicks() != slot.fullTicks()) {
                FoodSlot resized = FoodTimers.withFullTicks(slot, definition.fullTicks());
                slots.set(i, resized);
                Kind kind = resized.remainingTicks() != slot.remainingTicks() ? Kind.CAPPED : Kind.RESIZED;
                changes.add(new Change(kind, slot.definitionId(), resized.remainingTicks(), resized.fullTicks()));
            }
        }
        return changes;
    }
}
