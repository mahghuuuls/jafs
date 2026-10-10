package com.mahghuuuls.jafs.core;

/**
 * One active food record: which definition is active, how much time is left, and the full
 * duration it was activated with. Pure data; the rules that change it live in the core rule
 * classes, and the only runtime writer is the active-food service.
 */
public final class FoodSlot {

    private final String definitionId;
    private int remainingTicks;
    private final int fullTicks;

    public FoodSlot(String definitionId, int remainingTicks, int fullTicks) {
        if (definitionId == null || definitionId.isEmpty()) {
            throw new IllegalArgumentException("definitionId is required");
        }
        if (fullTicks <= 0) {
            throw new IllegalArgumentException("fullTicks must be positive");
        }
        this.definitionId = definitionId;
        this.fullTicks = fullTicks;
        this.remainingTicks = clamp(remainingTicks, fullTicks);
    }

    public String definitionId() {
        return definitionId;
    }

    public int remainingTicks() {
        return remainingTicks;
    }

    public int fullTicks() {
        return fullTicks;
    }

    /** Sets the remaining time, clamped to {@code 0..fullTicks}. */
    public void setRemainingTicks(int ticks) {
        this.remainingTicks = clamp(ticks, fullTicks);
    }

    public boolean isExpired() {
        return remainingTicks <= 0;
    }

    private static int clamp(int value, int max) {
        if (value < 0) {
            return 0;
        }
        return value > max ? max : value;
    }

    @Override
    public String toString() {
        return definitionId + " " + remainingTicks + "/" + fullTicks;
    }
}
