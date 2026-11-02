package com.mahghuuuls.jafs.property;

import com.mahghuuuls.jafs.api.PropertyOperation;
import com.mahghuuuls.jafs.api.PropertySpec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The table of JAFS's attribute-backed properties (REQ-056, ARC-013): which attribute each one
 * reaches, the operations it supports with their limits, its unit and color, and the mod it needs,
 * when any. The numbers are the ones recorded in {@code config-defaults.json} (its {@code
 * properties} section); a test keeps the two in step. Registering is one call per row, and a new
 * property is one more row.
 */
public final class AttributeProperties {

    /** Whether a mod is loaded; the mod passes FML's answer, tests pass what they need. */
    public interface ModPresence {
        boolean isLoaded(String modId);
    }

    /** No optional mod present. */
    public static final ModPresence NONE = new ModPresence() {
        @Override
        public boolean isLoaded(String modId) {
            return false;
        }
    };

    /** One table row. Immutable. */
    public static final class Row {
        private final String id;
        private final String attribute;
        private final String unit;
        private final String color;
        private final PropertySpec.Range flat;
        private final PropertySpec.Range percent;
        private final String requiresMod;

        Row(String id, String attribute, String unit, String color, PropertySpec.Range flat, PropertySpec.Range percent,
                String requiresMod) {
            if (flat == null && percent == null) {
                throw new IllegalArgumentException("property " + id + " supports no operation");
            }
            this.id = id;
            this.attribute = attribute;
            this.unit = unit;
            this.color = color;
            this.flat = flat;
            this.percent = percent;
            this.requiresMod = requiresMod;
        }

        public String id() {
            return id;
        }

        public String attribute() {
            return attribute;
        }

        public String unit() {
            return unit;
        }

        public String color() {
            return color;
        }

        /** The flat limits, or null when flat is unsupported. */
        public PropertySpec.Range flat() {
            return flat;
        }

        /** The percent limits, or null when percent is unsupported. */
        public PropertySpec.Range percent() {
            return percent;
        }

        /** The mod id this row needs, or null when the attribute is vanilla's or Forge's. */
        public String requiresMod() {
            return requiresMod;
        }

        public PropertySpec spec() {
            PropertySpec.Builder builder = PropertySpec.builder(id).unit(unit).color(color);
            if (flat != null) {
                builder.operation(PropertyOperation.FLAT, flat.min(), flat.max());
            }
            if (percent != null) {
                builder.operation(PropertyOperation.PERCENT, percent.min(), percent.max());
            }
            return builder.build();
        }
    }

    /** The mod id of Potion Core, whose attributes the optional rows reach. */
    public static final String POTION_CORE = "potioncore";

    private static final List<Row> ROWS;

    static {
        List<Row> rows = new ArrayList<Row>();
        // Vanilla and Forge attributes, always present on players.
        rows.add(new Row("jafs:movement_speed", "generic.movementSpeed", "", "aqua", null, range(-90, 300), null));
        rows.add(new Row("jafs:attack_damage", "generic.attackDamage", "HP", "red", range(-20, 100), range(-100, 1000), null));
        rows.add(new Row("jafs:attack_speed", "generic.attackSpeed", "", "yellow", range(-4, 100), range(-90, 1000), null));
        rows.add(new Row("jafs:armor", "generic.armor", "", "gray", range(-30, 30), range(-100, 1000), null));
        rows.add(new Row("jafs:armor_toughness", "generic.armorToughness", "", "dark_gray", range(-20, 20), range(-100, 1000), null));
        rows.add(new Row("jafs:knockback_resistance", "generic.knockbackResistance", "", "dark_aqua", range(-1, 1), null, null));
        rows.add(new Row("jafs:luck", "generic.luck", "", "light_purple", range(-100, 100), null, null));
        rows.add(new Row("jafs:block_reach", "generic.reachDistance", "blocks", "blue", range(-5, 100), range(-100, 1000), null));
        // Potion Core's attributes (REQ-056b): registered only when that mod is loaded; found by name, no class of it referenced.
        rows.add(new Row("jafs:jump_height", "potioncore.jumpHeight", "", "dark_green", range(-1, 10), range(-100, 1000), POTION_CORE));
        rows.add(new Row("jafs:dig_speed", "potioncore.digSpeed", "", "gold", range(-1, 10), range(-100, 1000), POTION_CORE));
        rows.add(new Row("jafs:projectile_damage", "potioncore.projectileDamage", "", "dark_red", range(-1, 10), range(-100, 1000), POTION_CORE));
        rows.add(new Row("jafs:magic_damage", "potioncore.magicDamage", "", "dark_purple", range(-1, 10), range(-100, 1000), POTION_CORE));
        // Potion Core multiplies damage received by (2 - damageResistance): a positive amount is less damage taken, and the
        // attribute's ceiling of 2.0 would be immunity, so the limits stop at 90 percent of the way there.
        rows.add(new Row("jafs:damage_resistance", "potioncore.damageResistance", "", "dark_blue", range(-1, 0.9), range(-100, 90), POTION_CORE));
        rows.add(new Row("jafs:magic_shielding", "potioncore.magicShielding", "", "white", range(0, 20), null, POTION_CORE));
        rows.add(new Row("jafs:step_height", "potioncore.stepHeight", "blocks", "dark_green", range(-0.6, 5), range(-100, 1000), POTION_CORE));
        ROWS = Collections.unmodifiableList(rows);
    }

    private AttributeProperties() {
    }

    private static PropertySpec.Range range(double min, double max) {
        return new PropertySpec.Range(min, max);
    }

    /** Every row, in registration order. */
    public static List<Row> rows() {
        return ROWS;
    }

    /** The row of a property id, or null. */
    public static Row row(String propertyId) {
        for (Row row : ROWS) {
            if (row.id().equals(propertyId)) {
                return row;
            }
        }
        return null;
    }

    /**
     * Registers every row whose required mod is absent-or-loaded, each with its own
     * {@link AttributeApplier}. Returns the registered ids, for the load report.
     */
    public static List<String> registerAll(PropertyRegistry registry, ModPresence presence) {
        return registerAll(ROWS, registry, presence);
    }

    /** The same over an explicit list of rows; package-visible for tests of the gating. */
    static List<String> registerAll(List<Row> rows, PropertyRegistry registry, ModPresence presence) {
        List<String> registered = new ArrayList<String>();
        for (Row row : rows) {
            if (row.requiresMod() != null && !presence.isLoaded(row.requiresMod())) {
                continue;
            }
            registry.register(row.spec(), new AttributeApplier(row.id(), row.attribute()));
            registered.add(row.id());
        }
        return registered;
    }
}
