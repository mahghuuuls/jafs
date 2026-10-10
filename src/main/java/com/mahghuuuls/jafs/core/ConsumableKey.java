package com.mahghuuuls.jafs.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * What is about to be consumed, reduced to the fields matchers can look at: kind, registry id,
 * metadata, and the string values of the NBT fields some matcher for this id asked for.
 */
public final class ConsumableKey {

    private final Matcher.Kind kind;
    private final String id;
    private final int meta;
    private final Map<String, String> nbt;

    public ConsumableKey(Matcher.Kind kind, String id, int meta, Map<String, String> nbt) {
        if (kind == null || id == null) {
            throw new IllegalArgumentException("kind and id are required");
        }
        this.kind = kind;
        this.id = id;
        this.meta = meta;
        this.nbt = nbt == null || nbt.isEmpty() ? Collections.<String, String>emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<String, String>(nbt));
    }

    public Matcher.Kind kind() {
        return kind;
    }

    public String id() {
        return id;
    }

    public int meta() {
        return meta;
    }

    public Map<String, String> nbt() {
        return nbt;
    }

    @Override
    public String toString() {
        return kind.name().toLowerCase() + " " + id + "@" + meta + (nbt.isEmpty() ? "" : " " + nbt);
    }
}
