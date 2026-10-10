package com.mahghuuuls.jafs.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Which consumables a food definition covers: one item id (optional metadata, optional NBT
 * fields with required values) or one cake-like block id (optional metadata).
 */
public final class Matcher {

    public enum Kind {
        ITEM,
        BLOCK
    }

    /** Metadata value meaning "any". */
    public static final int ANY_META = -1;

    private final Kind kind;
    private final String id;
    private final int meta;
    private final Map<String, String> nbt;

    public Matcher(Kind kind, String id, int meta, Map<String, String> nbt) {
        if (kind == null || id == null || id.isEmpty()) {
            throw new IllegalArgumentException("kind and id are required");
        }
        this.kind = kind;
        this.id = id;
        this.meta = meta < 0 ? ANY_META : meta;
        this.nbt = nbt == null || nbt.isEmpty() ? Collections.<String, String>emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<String, String>(nbt));
    }

    public static Matcher item(String id, int meta, Map<String, String> nbt) {
        return new Matcher(Kind.ITEM, id, meta, nbt);
    }

    public static Matcher block(String id, int meta) {
        return new Matcher(Kind.BLOCK, id, meta, null);
    }

    public Kind kind() {
        return kind;
    }

    public String id() {
        return id;
    }

    public int meta() {
        return meta;
    }

    /** Required NBT field values; empty for a block matcher or an item matcher without NBT. */
    public Map<String, String> nbt() {
        return nbt;
    }

    /** True when this matcher covers the key. Unrelated NBT on the key is ignored. */
    public boolean matches(ConsumableKey key) {
        if (key.kind() != kind || !key.id().equals(id)) {
            return false;
        }
        if (meta != ANY_META && key.meta() != meta) {
            return false;
        }
        for (Map.Entry<String, String> required : nbt.entrySet()) {
            String actual = key.nbt().get(required.getKey());
            if (actual == null || !actual.equals(required.getValue())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Matcher)) {
            return false;
        }
        Matcher that = (Matcher) other;
        return kind == that.kind && meta == that.meta && id.equals(that.id) && nbt.equals(that.nbt);
    }

    @Override
    public int hashCode() {
        return ((kind.hashCode() * 31 + id.hashCode()) * 31 + meta) * 31 + nbt.hashCode();
    }

    @Override
    public String toString() {
        return kind.name().toLowerCase() + " " + id + (meta == ANY_META ? "" : "@" + meta) + (nbt.isEmpty() ? "" : " " + nbt);
    }
}
