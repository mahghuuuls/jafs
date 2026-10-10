package com.mahghuuuls.jafs.api;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * Metadata of a property that food can modify: its id, how it is shown, which operations it
 * supports, and the valid amount range per operation. JAFS registers its own three properties
 * through this type; addons register theirs the same way.
 *
 * <p>Display names are language keys so every side translates them. Colors are vanilla text
 * formatting names ({@code red}, {@code green}, {@code aqua}, ...), which is what item tooltips
 * can show.
 */
public final class PropertySpec {

    private final String id;
    private final String displayNameKey;
    private final String unit;
    private final String color;
    private final Map<PropertyOperation, Range> operations;

    private PropertySpec(Builder builder) {
        this.id = builder.id;
        this.displayNameKey = builder.displayNameKey;
        this.unit = builder.unit;
        this.color = builder.color;
        this.operations = Collections.unmodifiableMap(new EnumMap<PropertyOperation, Range>(builder.operations));
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    /** Namespaced id, for example {@code jafs:max_health}. */
    public String id() {
        return id;
    }

    public String displayNameKey() {
        return displayNameKey;
    }

    /** Unit shown after a flat amount, such as {@code HP}; may be empty. */
    public String unit() {
        return unit;
    }

    /** Default tooltip color as a vanilla text formatting name. */
    public String color() {
        return color;
    }

    public Set<PropertyOperation> operations() {
        return operations.keySet();
    }

    public boolean supports(PropertyOperation operation) {
        return operations.containsKey(operation);
    }

    /** The valid amount range for an operation, or null when the operation is unsupported. */
    public Range range(PropertyOperation operation) {
        return operations.get(operation);
    }

    /** Inclusive amount range. */
    public static final class Range {
        private final double min;
        private final double max;

        public Range(double min, double max) {
            if (max < min) {
                throw new IllegalArgumentException("max below min");
            }
            this.min = min;
            this.max = max;
        }

        public double min() {
            return min;
        }

        public double max() {
            return max;
        }

        public boolean contains(double value) {
            return !Double.isNaN(value) && value >= min && value <= max;
        }

        @Override
        public String toString() {
            return min + ".." + max;
        }
    }

    public static final class Builder {
        private final String id;
        private String displayNameKey;
        private String unit = "";
        private String color = "white";
        private final Map<PropertyOperation, Range> operations = new EnumMap<PropertyOperation, Range>(PropertyOperation.class);

        private Builder(String id) {
            if (id == null || !id.contains(":")) {
                throw new IllegalArgumentException("property id must be namespaced: " + id);
            }
            this.id = id;
            this.displayNameKey = "property." + id.replace(':', '.');
        }

        public Builder displayNameKey(String key) {
            this.displayNameKey = key;
            return this;
        }

        public Builder unit(String unit) {
            this.unit = unit == null ? "" : unit;
            return this;
        }

        public Builder color(String formattingName) {
            this.color = formattingName;
            return this;
        }

        public Builder operation(PropertyOperation operation, double min, double max) {
            operations.put(operation, new Range(min, max));
            return this;
        }

        public PropertySpec build() {
            if (operations.isEmpty()) {
                throw new IllegalStateException("property " + id + " supports no operation");
            }
            return new PropertySpec(this);
        }
    }
}
