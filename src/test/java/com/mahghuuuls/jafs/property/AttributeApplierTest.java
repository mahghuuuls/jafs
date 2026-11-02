package com.mahghuuuls.jafs.property;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeMap;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The generic applier on real vanilla attribute instances: the flat sum is an operation-0
 * modifier and the percent sum an operation-1 modifier (vanilla's additive-percent rule), the ids
 * are derived from the property id (pinned as literals, because they are saved on players) and
 * distinct from every other property's and from max health's, reapplying unchanged sums changes
 * nothing, zero removes, the modifiers are saved, and a map without the attribute is skipped and
 * reported once (REQ-056a, ARC-013).
 */
class AttributeApplierTest {

    private static IAttributeInstance armor() {
        return new AttributeMap().registerAttribute(SharedMonsterAttributes.ARMOR);
    }

    @Test
    void flatIsOperationZeroAndPercentOperationOne() {
        AttributeApplier applier = new AttributeApplier("jafs:armor", "generic.armor");
        IAttributeInstance attribute = armor();
        assertTrue(applier.applyTo(attribute, 10, 50));
        assertEquals(15, attribute.getAttributeValue(), 1e-9); // (0 + 10) * (1 + 0.5)
        AttributeModifier flat = attribute.getModifier(applier.flatId());
        AttributeModifier percent = attribute.getModifier(applier.percentId());
        assertNotNull(flat);
        assertNotNull(percent);
        assertEquals(0, flat.getOperation());
        assertEquals(1, percent.getOperation());
        assertEquals(0.5, percent.getAmount(), 1e-9);
        assertTrue(flat.isSaved());
        assertTrue(percent.isSaved());
        assertTrue(flat.getName().startsWith("JAFS food "));
    }

    @Test
    void reapplyingTheSameSumsIsANoOpAndZeroRemoves() {
        AttributeApplier applier = new AttributeApplier("jafs:armor", "generic.armor");
        IAttributeInstance attribute = armor();
        assertTrue(applier.applyTo(attribute, 4, 0));
        assertFalse(applier.applyTo(attribute, 4, 0));
        assertNull(attribute.getModifier(applier.percentId()));
        assertTrue(applier.applyTo(attribute, 6, 0));
        assertEquals(6, attribute.getAttributeValue(), 1e-9);
        assertTrue(applier.applyTo(attribute, 0, 0));
        assertNull(attribute.getModifier(applier.flatId()));
        assertEquals(0, attribute.getAttributeValue(), 1e-9);
        assertFalse(applier.applyTo(attribute, 0, 0));
    }

    @Test
    void idsArePinnedLiteralsDistinctAcrossProperties() {
        // The derivation text is a persistent format: these literals are what players' saved modifiers carry.
        assertEquals(UUID.fromString("dbaad998-bc6a-39ac-b56e-b0c5aef0de26"), AttributeApplier.modifierId("jafs:armor", "flat"));
        assertEquals(UUID.fromString("1c8e606e-c3ef-353f-b2dc-b617d9ecf3bf"), AttributeApplier.modifierId("jafs:armor", "percent"));
        assertEquals(UUID.fromString("e90e9899-7e7b-341e-a617-f9e37912d38b"), AttributeApplier.modifierId("jafs:luck", "flat"));
        assertEquals(new AttributeApplier("jafs:luck", "generic.luck").flatId(), new AttributeApplier("jafs:luck", "generic.luck").flatId());
        Set<UUID> ids = new HashSet<UUID>();
        ids.add(MaxHealthApplier.FLAT_ID);
        ids.add(MaxHealthApplier.PERCENT_ID);
        for (AttributeProperties.Row row : AttributeProperties.rows()) {
            AttributeApplier applier = new AttributeApplier(row.id(), row.attribute());
            assertTrue(ids.add(applier.flatId()), "flat id of " + row.id() + " collides");
            assertTrue(ids.add(applier.percentId()), "percent id of " + row.id() + " collides");
        }
        assertNotEquals(AttributeApplier.modifierId("jafs:armor", "flat"), AttributeApplier.modifierId("jafs:armor", "percent"));
    }

    @Test
    void percentScalesWhatEquipmentAdded() {
        AttributeApplier applier = new AttributeApplier("jafs:armor", "generic.armor");
        IAttributeInstance attribute = armor();
        attribute.applyModifier(new AttributeModifier(UUID.randomUUID(), "iron chestplate", 6, 0));
        assertTrue(applier.applyTo(attribute, 0, 50));
        assertEquals(9, attribute.getAttributeValue(), 1e-9);
    }

    @Test
    void aValueBelowTheFloorClampsAndRemovalRestoresTheBase() {
        AttributeApplier applier = new AttributeApplier("jafs:knockback_resistance", "generic.knockbackResistance");
        IAttributeInstance attribute = new AttributeMap().registerAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE);
        assertTrue(applier.applyTo(attribute, -1, 0));
        assertEquals(0, attribute.getAttributeValue(), 1e-9); // clamped to the attribute's own floor, no exception
        assertEquals(-1, attribute.getModifier(applier.flatId()).getAmount(), 1e-9);
        assertTrue(applier.applyTo(attribute, 0, 0));
        assertNull(attribute.getModifier(applier.flatId()));
        assertEquals(0, attribute.getAttributeValue(), 1e-9);
    }

    @Test
    void aMapWithoutTheAttributeIsSkippedAndReportedOnce() {
        AttributeApplier.resetMissingReports();
        AttributeApplier applier = new AttributeApplier("jafs:block_reach", "generic.reachDistance");
        AttributeMap map = new AttributeMap(); // no reach attribute registered, as another mod might leave it
        assertFalse(AttributeApplier.wasReportedMissing("jafs:block_reach"));
        assertFalse(applier.applyTo(map, "tester", 3, 0));
        assertTrue(AttributeApplier.wasReportedMissing("jafs:block_reach"));
        assertFalse(applier.applyTo(map, "tester", 3, 0));
        assertTrue(AttributeApplier.wasReportedMissing("jafs:block_reach"));
        assertFalse(AttributeApplier.wasReportedMissing("jafs:armor"));
        map.registerAttribute(SharedMonsterAttributes.ARMOR);
        assertTrue(new AttributeApplier("jafs:armor", "generic.armor").applyTo(map, "tester", 2, 0));
        assertEquals(2, map.getAttributeInstanceByName("generic.armor").getAttributeValue(), 1e-9);
        AttributeApplier.resetMissingReports();
        assertFalse(AttributeApplier.wasReportedMissing("jafs:block_reach"));
    }
}
