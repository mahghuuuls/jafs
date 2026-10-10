package com.mahghuuuls.jafs.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A validated food definition: matchers, full duration, priority, and its valid modifiers. */
public final class FoodDefinition {

    public static final int TICKS_PER_SECOND = 20;

    private final String id;
    private final List<Matcher> matchers;
    private final int fullTicks;
    private final int priority;
    private final List<Modifier> modifiers;

    public FoodDefinition(String id, List<Matcher> matchers, int fullDurationSeconds, int priority, List<Modifier> modifiers) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("id is required");
        }
        if (fullDurationSeconds <= 0) {
            throw new IllegalArgumentException("fullDuration must be positive");
        }
        this.id = id;
        this.matchers = Collections.unmodifiableList(new ArrayList<Matcher>(matchers));
        this.fullTicks = fullDurationSeconds * TICKS_PER_SECOND;
        this.priority = priority;
        this.modifiers = Collections.unmodifiableList(new ArrayList<Modifier>(modifiers));
    }

    public String id() {
        return id;
    }

    public List<Matcher> matchers() {
        return matchers;
    }

    public int fullTicks() {
        return fullTicks;
    }

    public int fullDurationSeconds() {
        return fullTicks / TICKS_PER_SECOND;
    }

    public int priority() {
        return priority;
    }

    /** Valid modifiers only; a definition with none grants no active food. */
    public List<Modifier> modifiers() {
        return modifiers;
    }

    public boolean hasModifiers() {
        return !modifiers.isEmpty();
    }

    @Override
    public String toString() {
        return id;
    }
}
