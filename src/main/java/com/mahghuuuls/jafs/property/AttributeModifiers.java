package com.mahghuuuls.jafs.property;

import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;

import java.util.UUID;

/**
 * The one way JAFS puts a modifier on an attribute: by a fixed id, saved with the player, removed
 * before it is reapplied so nothing duplicates, absent when its amount is zero, and left alone
 * when the amount did not change. Shared by {@link MaxHealthApplier} and {@link AttributeApplier}.
 */
final class AttributeModifiers {

    private AttributeModifiers() {
    }

    /**
     * Puts the modifier {@code id} in the state {@code amount} describes and returns whether the
     * attribute changed: removed when the amount is zero, replaced when it differs, untouched when
     * it is the same.
     */
    static boolean put(IAttributeInstance attribute, UUID id, String name, int operation, double amount) {
        AttributeModifier existing = attribute.getModifier(id);
        if (amount == 0) {
            if (existing == null) {
                return false;
            }
            attribute.removeModifier(id);
            return true;
        }
        if (existing != null) {
            if (Double.compare(existing.getAmount(), amount) == 0) {
                return false;
            }
            attribute.removeModifier(id);
        }
        AttributeModifier modifier = new AttributeModifier(id, name, amount, operation);
        modifier.setSaved(true);
        attribute.applyModifier(modifier);
        return true;
    }
}
