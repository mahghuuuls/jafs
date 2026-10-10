package com.mahghuuuls.jafs.core;

import java.util.HashMap;
import java.util.Map;

/**
 * The decisions made when eating started, waiting for the finish. One per player: a new start
 * replaces the old decision, a stop or a logout drops it, and a finish takes it exactly once.
 */
public final class PendingMeals<K> {

    private final Map<K, String> pending = new HashMap<K, String>();

    /** Records the definition accepted at start, replacing any earlier one for the player. */
    public void begin(K player, String definitionId) {
        pending.put(player, definitionId);
    }

    /** Removes and returns the pending definition id, or null when nothing was pending. */
    public String take(K player) {
        return pending.remove(player);
    }

    public void cancel(K player) {
        pending.remove(player);
    }

    public boolean has(K player) {
        return pending.containsKey(player);
    }

    public int size() {
        return pending.size();
    }
}
