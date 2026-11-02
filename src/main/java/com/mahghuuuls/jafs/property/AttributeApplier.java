package com.mahghuuuls.jafs.property;

import com.mahghuuuls.jafs.api.IPropertyApplier;
import com.mahghuuuls.jafs.diagnostic.Diag;
import net.minecraft.entity.ai.attributes.AbstractAttributeMap;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayerMP;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * One applier for every property backed by a player attribute (REQ-056a, ARC-013): the flat sum
 * as an operation-0 modifier and the percent sum, divided by 100, as an operation-1 modifier,
 * each under an id derived from the property id (so a saved modifier is found again on any
 * server), saved with the player, removed by id before it is reapplied, absent at a zero sum.
 * The attribute is found by name on the player's map, so attributes that another mod registers
 * (Potion Core's) work the same way as vanilla's. A player without the attribute is skipped and
 * reported once per property.
 */
public final class AttributeApplier implements IPropertyApplier {

    private static final Set<String> MISSING_REPORTED = Collections.synchronizedSet(new HashSet<String>());

    private final String propertyId;
    private final String attributeName;
    private final UUID flatId;
    private final UUID percentId;
    private final String flatName;
    private final String percentName;

    public AttributeApplier(String propertyId, String attributeName) {
        if (propertyId == null || propertyId.isEmpty() || attributeName == null || attributeName.isEmpty()) {
            throw new IllegalArgumentException("a property id and an attribute name are required");
        }
        this.propertyId = propertyId;
        this.attributeName = attributeName;
        this.flatId = modifierId(propertyId, "flat");
        this.percentId = modifierId(propertyId, "percent");
        this.flatName = "JAFS food " + propertyId + " (flat)";
        this.percentName = "JAFS food " + propertyId + " (percent)";
    }

    /**
     * The id of one of a property's two modifiers: {@code kind} is {@code "flat"} (operation 0)
     * or {@code "percent"} (operation 1). Derived from the text {@code jafs:modifier:<property
     * id>:<kind>} with {@link UUID#nameUUIDFromBytes}, so it is the same on every server and in
     * every version; a saved modifier on a player is a persistent format, and the text must not
     * change.
     */
    public static UUID modifierId(String propertyId, String kind) {
        return UUID.nameUUIDFromBytes(("jafs:modifier:" + propertyId + ":" + kind).getBytes(StandardCharsets.UTF_8));
    }

    public String propertyId() {
        return propertyId;
    }

    public String attributeName() {
        return attributeName;
    }

    UUID flatId() {
        return flatId;
    }

    UUID percentId() {
        return percentId;
    }

    @Override
    public void apply(EntityPlayerMP player, double flatSum, double percentSum) {
        applyTo(player.getAttributeMap(), player.getName(), flatSum, percentSum);
    }

    /**
     * Finds the attribute on the map and puts the two modifiers in the state the sums describe.
     * Returns whether anything changed; a map without the attribute changes nothing and is
     * reported once per property. Package-visible for tests on a plain attribute map.
     */
    boolean applyTo(AbstractAttributeMap map, String playerName, double flatSum, double percentSum) {
        IAttributeInstance attribute = map.getAttributeInstanceByName(attributeName);
        if (attribute == null) {
            if (MISSING_REPORTED.add(propertyId)) {
                Diag.LOGGER.warn("property {} cannot apply: attribute {} is missing from {} (reported once)", propertyId,
                        attributeName, playerName);
            }
            return false;
        }
        double before = attribute.getAttributeValue();
        boolean changed = applyTo(attribute, flatSum, percentSum);
        if (changed) {
            Diag.event("applied {} flat={} percent={} for {} ({} {} -> {})", propertyId, flatSum, percentSum,
                    playerName, attributeName, before, attribute.getAttributeValue());
        }
        return changed;
    }

    /**
     * Puts the two modifiers in the state the sums describe and returns whether anything changed.
     * Package-visible for tests on a plain attribute instance.
     */
    boolean applyTo(IAttributeInstance attribute, double flatSum, double percentSum) {
        boolean changed = AttributeModifiers.put(attribute, flatId, flatName, 0, flatSum);
        changed |= AttributeModifiers.put(attribute, percentId, percentName, 1, percentSum / 100.0);
        return changed;
    }

    /** Whether the missing-attribute warning was already given for a property; for tests. */
    static boolean wasReportedMissing(String propertyId) {
        return MISSING_REPORTED.contains(propertyId);
    }

    /** Forgets which properties were reported missing; for tests. */
    static void resetMissingReports() {
        MISSING_REPORTED.clear();
    }
}
