package com.mahghuuuls.jafs.state;

import com.mahghuuuls.jafs.core.FoodSlot;

import java.util.ArrayList;
import java.util.List;

public final class FoodState implements IFoodState {

    private final List<FoodSlot> slots = new ArrayList<FoodSlot>();
    private int recoveryDelayTicks;
    private long revision;

    @Override
    public List<FoodSlot> slots() {
        return slots;
    }

    @Override
    public int recoveryDelayTicks() {
        return recoveryDelayTicks;
    }

    @Override
    public void setRecoveryDelayTicks(int ticks) {
        this.recoveryDelayTicks = ticks < 0 ? 0 : ticks;
    }

    @Override
    public long revision() {
        return revision;
    }

    @Override
    public void bumpRevision() {
        revision++;
    }

    /** Replaces this state's contents with another's (used when a player entity is recreated). */
    public void copyFrom(IFoodState other) {
        slots.clear();
        for (FoodSlot slot : other.slots()) {
            slots.add(new FoodSlot(slot.definitionId(), slot.remainingTicks(), slot.fullTicks()));
        }
        recoveryDelayTicks = other.recoveryDelayTicks();
        bumpRevision();
    }
}
