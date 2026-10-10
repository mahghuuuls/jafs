package com.mahghuuuls.jafs.property;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeMap;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The two modifiers on the real vanilla attribute instance: vanilla's operation math gives the
 * additive percent rule, reapplying the same sums changes nothing, zero removes, the floor of 1
 * holds, and the modifiers are saved with the player (REQ-027 to REQ-029, REQ-056, REQ-089, REV-005).
 */
class MaxHealthApplierTest {

    private static IAttributeInstance maxHealth() {
        return new AttributeMap().registerAttribute(SharedMonsterAttributes.MAX_HEALTH);
    }

    @Test
    void flatAndPercentCombineAsVanillaOperations() {
        IAttributeInstance attribute = maxHealth();
        assertTrue(MaxHealthApplier.applyTo(attribute, 10, 50));
        assertEquals(45, attribute.getAttributeValue(), 1e-9);
        assertTrue(MaxHealthApplier.applyTo(attribute, 4, 0));
        assertEquals(24, attribute.getAttributeValue(), 1e-9);
        assertNull(attribute.getModifier(MaxHealthApplier.PERCENT_ID));
    }

    @Test
    void reapplyingTheSameSumsIsANoOp() {
        IAttributeInstance attribute = maxHealth();
        assertTrue(MaxHealthApplier.applyTo(attribute, 4, 0));
        assertFalse(MaxHealthApplier.applyTo(attribute, 4, 0));
        assertFalse(MaxHealthApplier.applyTo(attribute, 4, 0));
        assertEquals(24, attribute.getAttributeValue(), 1e-9);
        assertEquals(1, attribute.getModifiers().size());
    }

    @Test
    void zeroRemovesAndNegativeFlatLowersTheMaximum() {
        IAttributeInstance attribute = maxHealth();
        MaxHealthApplier.applyTo(attribute, 4, 0);
        assertTrue(MaxHealthApplier.applyTo(attribute, 0, 0));
        assertEquals(20, attribute.getAttributeValue(), 1e-9);
        assertTrue(attribute.getModifiers().isEmpty());
        assertFalse(MaxHealthApplier.applyTo(attribute, 0, 0));
        MaxHealthApplier.applyTo(attribute, -5, 0);
        assertEquals(15, attribute.getAttributeValue(), 1e-9);
    }

    @Test
    void theMaximumNeverGoesBelowOne() {
        // REQ-056: -25 flat on a base of 20 leaves 1, not Float.MIN_VALUE
        IAttributeInstance attribute = maxHealth();
        assertTrue(MaxHealthApplier.applyTo(attribute, -25, 0));
        assertEquals(1, attribute.getAttributeValue(), 1e-6);
        // -100 percent would be zero: percent dropped, floor kept
        assertTrue(MaxHealthApplier.applyTo(attribute, 0, -100));
        assertEquals(1, attribute.getAttributeValue(), 1e-6);
        assertNull(attribute.getModifier(MaxHealthApplier.PERCENT_ID));
        // back to normal
        assertTrue(MaxHealthApplier.applyTo(attribute, 0, 0));
        assertEquals(20, attribute.getAttributeValue(), 1e-9);
    }

    @Test
    void theFloorRespectsOtherModsModifiers() {
        IAttributeInstance attribute = maxHealth();
        attribute.applyModifier(new AttributeModifier(UUID.randomUUID(), "other mod", 10, 0));
        assertEquals(30, attribute.getAttributeValue(), 1e-9);
        MaxHealthApplier.applyTo(attribute, -40, 0);
        assertEquals(1, attribute.getAttributeValue(), 1e-6);
        MaxHealthApplier.applyTo(attribute, -5, 0);
        assertEquals(25, attribute.getAttributeValue(), 1e-9);
    }

    @Test
    void modifiersAreSavedWithThePlayer() {
        // So vanilla restores the maximum before it clamps the saved health on login (REV-005)
        IAttributeInstance attribute = maxHealth();
        MaxHealthApplier.applyTo(attribute, 4, 10);
        assertNotNull(attribute.getModifier(MaxHealthApplier.FLAT_ID));
        assertTrue(attribute.getModifier(MaxHealthApplier.FLAT_ID).isSaved());
        assertTrue(attribute.getModifier(MaxHealthApplier.PERCENT_ID).isSaved());
    }
}
