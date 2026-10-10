package com.mahghuuuls.jafs.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Owner of how active-food timers move: natural depletion at an effective rate (fractional
 * progress is carried between ticks so 0.5x really halves the speed), duration payments that
 * deduct whole seconds from every food at once, and the cap applied when a full duration shrinks.
 */
public final class FoodTimers {

    private FoodTimers() {
    }

    /** Per-player carry of fractional depletion between ticks; not saved. */
    public static final class Clock {
        private double carry;

        /** Whole ticks to deduct for one game tick at {@code rate}, carrying the remainder. */
        public int step(double rate) {
            if (rate <= 0) {
                carry = 0;
                return 0;
            }
            carry += rate;
            int whole = (int) carry;
            carry -= whole;
            return whole;
        }

        public void reset() {
            carry = 0;
        }
    }

    /**
     * Advances every slot by {@code ticks} and returns the slots that reached zero, in slot order.
     * The expired slots are left in the list for the caller to remove through its service.
     */
    public static List<FoodSlot> advance(List<FoodSlot> slots, int ticks) {
        if (ticks <= 0) {
            return Collections.emptyList();
        }
        List<FoodSlot> expired = null;
        for (FoodSlot slot : slots) {
            if (!slot.isExpired()) {
                slot.setRemainingTicks(slot.remainingTicks() - ticks);
            }
            if (slot.isExpired()) {
                if (expired == null) {
                    expired = new ArrayList<FoodSlot>(2);
                }
                expired.add(slot);
            }
        }
        return expired == null ? Collections.<FoodSlot>emptyList() : expired;
    }

    /**
     * The client's countdown: remaining ticks a while after a snapshot, at the snapshot's rate.
     * Never below zero.
     */
    public static int remainingAfter(int remainingTicks, double elapsedTicks, double rate) {
        if (elapsedTicks <= 0 || rate <= 0) {
            return remainingTicks < 0 ? 0 : remainingTicks;
        }
        long remaining = Math.round(remainingTicks - elapsedTicks * rate);
        return remaining < 0 ? 0 : (int) remaining;
    }

    /** True when at least one food can cover the whole cost (a zero cost is always affordable). */
    public static boolean canPay(List<FoodSlot> slots, int ticks) {
        if (ticks <= 0) {
            return true;
        }
        for (FoodSlot slot : slots) {
            if (slot.remainingTicks() >= ticks) {
                return true;
            }
        }
        return false;
    }

    /**
     * Deducts {@code ticks} from every slot when affordable and returns the slots that reached
     * zero; deducts nothing and returns null when not affordable.
     */
    public static List<FoodSlot> pay(List<FoodSlot> slots, int ticks) {
        if (!canPay(slots, ticks)) {
            return null;
        }
        return advance(slots, ticks);
    }

    /** Caps a slot whose full duration shrank: remaining never exceeds the new full duration. */
    public static FoodSlot withFullTicks(FoodSlot slot, int newFullTicks) {
        return new FoodSlot(slot.definitionId(), Math.min(slot.remainingTicks(), newFullTicks), newFullTicks);
    }
}
