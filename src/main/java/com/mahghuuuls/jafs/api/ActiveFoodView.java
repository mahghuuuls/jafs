package com.mahghuuuls.jafs.api;

import java.util.Collections;
import java.util.List;

/**
 * A read-only view of one active food (REQ-059): the definition id, the remaining and full
 * durations, whether it can be refreshed or replaced, and its modifiers. Durations are in
 * ticks; the seconds accessors round down.
 */
public final class ActiveFoodView {

    public static final int TICKS_PER_SECOND = 20;

    private final String definitionId;
    private final int remainingTicks;
    private final int fullTicks;
    private final boolean eligible;
    private final List<ModifierView> modifiers;

    public ActiveFoodView(String definitionId, int remainingTicks, int fullTicks, boolean eligible, List<ModifierView> modifiers) {
        this.definitionId = definitionId;
        this.remainingTicks = remainingTicks;
        this.fullTicks = fullTicks;
        this.eligible = eligible;
        this.modifiers = Collections.unmodifiableList(modifiers);
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

    public int remainingSeconds() {
        return remainingTicks / TICKS_PER_SECOND;
    }

    public int fullSeconds() {
        return fullTicks / TICKS_PER_SECOND;
    }

    /** Whether eating the same food again would refresh it, and whether another food may replace it. */
    public boolean eligible() {
        return eligible;
    }

    public List<ModifierView> modifiers() {
        return modifiers;
    }

    @Override
    public String toString() {
        return definitionId + " " + remainingTicks + "/" + fullTicks + (eligible ? " eligible" : "");
    }
}
