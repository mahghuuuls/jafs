package com.mahghuuuls.jafs.state;

import com.mahghuuuls.jafs.core.FoodSlot;

import java.util.List;

/**
 * The per-player JAFS state saved with the player: the active foods in slot order and the
 * remaining recovery delay. Mutated only by the gameplay services.
 */
public interface IFoodState {

    /** Active foods in slot order; the list is live and ordered by slot index. */
    List<FoodSlot> slots();

    int recoveryDelayTicks();

    void setRecoveryDelayTicks(int ticks);

    /** Bumped on every mutation so caches and sync can tell whether anything changed. */
    long revision();

    void bumpRevision();
}
