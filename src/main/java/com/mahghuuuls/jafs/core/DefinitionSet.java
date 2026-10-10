package com.mahghuuuls.jafs.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The loaded food definitions, immutable, with a revision number and the index that resolves a
 * consumable to its definition. Owner of the priority and conflict rules.
 */
public final class DefinitionSet {

    private final long revision;
    private final Map<String, FoodDefinition> byId;
    private final Map<String, List<FoodDefinition>> byItemId;
    private final Map<String, List<FoodDefinition>> byBlockId;
    private final Map<String, Set<String>> nbtFieldsByItemId;

    public DefinitionSet(long revision, List<FoodDefinition> definitions) {
        this.revision = revision;
        Map<String, FoodDefinition> ids = new LinkedHashMap<String, FoodDefinition>();
        Map<String, List<FoodDefinition>> items = new HashMap<String, List<FoodDefinition>>();
        Map<String, List<FoodDefinition>> blocks = new HashMap<String, List<FoodDefinition>>();
        Map<String, Set<String>> fields = new HashMap<String, Set<String>>();
        for (FoodDefinition definition : definitions) {
            if (ids.put(definition.id(), definition) != null) {
                throw new IllegalArgumentException("duplicate definition id " + definition.id());
            }
            for (Matcher matcher : definition.matchers()) {
                Map<String, List<FoodDefinition>> index = matcher.kind() == Matcher.Kind.ITEM ? items : blocks;
                List<FoodDefinition> list = index.get(matcher.id());
                if (list == null) {
                    list = new ArrayList<FoodDefinition>();
                    index.put(matcher.id(), list);
                }
                if (!list.contains(definition)) {
                    list.add(definition);
                }
                if (matcher.kind() == Matcher.Kind.ITEM && !matcher.nbt().isEmpty()) {
                    Set<String> keys = fields.get(matcher.id());
                    if (keys == null) {
                        keys = new HashSet<String>();
                        fields.put(matcher.id(), keys);
                    }
                    keys.addAll(matcher.nbt().keySet());
                }
            }
        }
        this.byId = Collections.unmodifiableMap(ids);
        this.byItemId = items;
        this.byBlockId = blocks;
        this.nbtFieldsByItemId = fields;
    }

    public static DefinitionSet empty() {
        return new DefinitionSet(0L, Collections.<FoodDefinition>emptyList());
    }

    public long revision() {
        return revision;
    }

    public FoodDefinition get(String id) {
        return byId.get(id);
    }

    public Map<String, FoodDefinition> all() {
        return byId;
    }

    public int size() {
        return byId.size();
    }

    /** NBT field names any matcher for this item id reads; empty when none does. */
    public Set<String> nbtFieldsFor(String itemId) {
        Set<String> keys = nbtFieldsByItemId.get(itemId);
        return keys == null ? Collections.<String>emptySet() : keys;
    }

    /** True when any item matcher names this id (so the tooltip knows the item is configurable). */
    public boolean hasItem(String itemId) {
        return byItemId.containsKey(itemId);
    }

    /**
     * Resolves a consumable: the highest-priority matching definition wins; several at the same
     * highest priority is a conflict and nothing matches.
     */
    public Resolution resolve(ConsumableKey key) {
        Map<String, List<FoodDefinition>> index = key.kind() == Matcher.Kind.ITEM ? byItemId : byBlockId;
        List<FoodDefinition> candidates = index.get(key.id());
        if (candidates == null) {
            return Resolution.NONE;
        }
        List<FoodDefinition> best = new ArrayList<FoodDefinition>();
        int bestPriority = Integer.MIN_VALUE;
        for (FoodDefinition definition : candidates) {
            boolean matched = false;
            for (Matcher matcher : definition.matchers()) {
                if (matcher.matches(key)) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                continue;
            }
            if (definition.priority() > bestPriority) {
                best.clear();
                bestPriority = definition.priority();
            }
            if (definition.priority() == bestPriority) {
                best.add(definition);
            }
        }
        if (best.isEmpty()) {
            return Resolution.NONE;
        }
        if (best.size() == 1) {
            return new Resolution(best.get(0), Collections.<FoodDefinition>emptyList());
        }
        return new Resolution(null, Collections.unmodifiableList(best));
    }

    /**
     * Load-time conflicts: two definitions at the same priority whose matchers can both cover
     * one consumable (same kind and id, metadata equal or either "any", one NBT requirement a
     * subset of the other). Each pair is reported once with both ids.
     */
    public List<Conflict> conflicts() {
        List<Conflict> conflicts = new ArrayList<Conflict>();
        List<FoodDefinition> definitions = new ArrayList<FoodDefinition>(byId.values());
        for (int i = 0; i < definitions.size(); i++) {
            for (int j = i + 1; j < definitions.size(); j++) {
                FoodDefinition a = definitions.get(i);
                FoodDefinition b = definitions.get(j);
                if (a.priority() != b.priority()) {
                    continue;
                }
                for (Matcher ma : a.matchers()) {
                    for (Matcher mb : b.matchers()) {
                        if (overlap(ma, mb)) {
                            conflicts.add(new Conflict(a, ma, b, mb));
                        }
                    }
                }
            }
        }
        return conflicts;
    }

    static boolean overlap(Matcher a, Matcher b) {
        if (a.kind() != b.kind() || !a.id().equals(b.id())) {
            return false;
        }
        if (a.meta() != Matcher.ANY_META && b.meta() != Matcher.ANY_META && a.meta() != b.meta()) {
            return false;
        }
        return subset(a.nbt(), b.nbt()) || subset(b.nbt(), a.nbt());
    }

    private static boolean subset(Map<String, String> small, Map<String, String> large) {
        for (Map.Entry<String, String> e : small.entrySet()) {
            if (!e.getValue().equals(large.get(e.getKey()))) {
                return false;
            }
        }
        return true;
    }

    /** Two matchers of equal-priority definitions that can cover the same consumable. */
    public static final class Conflict {
        public final FoodDefinition first;
        public final Matcher firstMatcher;
        public final FoodDefinition second;
        public final Matcher secondMatcher;

        Conflict(FoodDefinition first, Matcher firstMatcher, FoodDefinition second, Matcher secondMatcher) {
            this.first = first;
            this.firstMatcher = firstMatcher;
            this.second = second;
            this.secondMatcher = secondMatcher;
        }
    }

    /** The outcome of resolving a consumable. */
    public static final class Resolution {
        public static final Resolution NONE = new Resolution(null, Collections.<FoodDefinition>emptyList());

        private final FoodDefinition definition;
        private final List<FoodDefinition> conflicting;

        private Resolution(FoodDefinition definition, List<FoodDefinition> conflicting) {
            this.definition = definition;
            this.conflicting = conflicting;
        }

        /** The matching definition, or null when none matches or the match is a conflict. */
        public FoodDefinition definition() {
            return definition;
        }

        /** The equal-priority definitions that collided; empty unless this is a conflict. */
        public List<FoodDefinition> conflicting() {
            return conflicting;
        }

        public boolean isConflict() {
            return !conflicting.isEmpty();
        }

        public boolean isNone() {
            return definition == null && conflicting.isEmpty();
        }
    }
}
