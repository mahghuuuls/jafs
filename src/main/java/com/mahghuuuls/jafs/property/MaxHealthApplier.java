package com.mahghuuuls.jafs.property;

import com.mahghuuuls.jafs.api.IPropertyApplier;
import com.mahghuuuls.jafs.core.BonusMath;
import com.mahghuuuls.jafs.diagnostic.Diag;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.UUID;

/**
 * Maximum health through two attribute modifiers with fixed ids: operation 0 carries the flat
 * sum, operation 1 the percent sum (vanilla adds all operation-1 amounts before multiplying,
 * which is exactly the additive-percent rule). The resulting maximum never goes below 1: when
 * the sums would take it lower, the percent modifier is dropped and the flat one raised to the
 * floor. After a decrease the current health is capped, which is not damage.
 */
public final class MaxHealthApplier implements IPropertyApplier {

    static final UUID FLAT_ID = UUID.fromString("5f3d1b2a-8c4e-4a6b-9d0e-1a2b3c4d5e6f");
    static final UUID PERCENT_ID = UUID.fromString("6a4e2c3b-9d5f-4b7c-8e1f-2b3c4d5e6f70");
    static final String FLAT_NAME = "JAFS food max health (flat)";
    static final String PERCENT_NAME = "JAFS food max health (percent)";
    /** The lowest maximum health food can leave a player with (REQ-056). */
    static final double FLOOR = 1.0;

    @Override
    public void apply(EntityPlayerMP player, double flatSum, double percentSum) {
        IAttributeInstance attribute = player.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH);
        float before = player.getMaxHealth();
        boolean changed = applyTo(attribute, flatSum, percentSum);
        if (!changed) {
            return;
        }
        float after = player.getMaxHealth();
        float health = player.getHealth();
        float capped = BonusMath.cappedHealth(health, after);
        if (after == before && capped == health) {
            // A floored state is rewritten to the same values on every recompute; nothing to report.
            return;
        }
        if (capped != health) {
            player.setHealth(capped);
            Diag.event("capped health {} -> {} for {} (max health {} -> {})", health, capped, player.getName(), before, after);
        } else {
            Diag.event("applied max health flat={} percent={} for {} (max health {} -> {})", flatSum, percentSum, player.getName(),
                    before, after);
        }
    }

    /**
     * Puts the two modifiers in the state the sums describe, floored so the attribute's value is
     * at least {@link #FLOOR}, and returns whether anything changed. A zero sum means no
     * modifier. Package-visible for tests on a plain attribute instance.
     */
    static boolean applyTo(IAttributeInstance attribute, double flatSum, double percentSum) {
        boolean changed = false;
        changed |= put(attribute, FLAT_ID, FLAT_NAME, 0, flatSum);
        changed |= put(attribute, PERCENT_ID, PERCENT_NAME, 1, percentSum / 100.0);
        if (attribute.getAttributeValue() < FLOOR) {
            changed |= floor(attribute);
        }
        return changed;
    }

    /**
     * Raises the maximum to the floor with JAFS's own modifiers only. The percent modifier is
     * dropped (it is what can push the value to zero), and the flat modifier is set from the
     * value the attribute has without JAFS, so other mods' modifiers are respected.
     */
    private static boolean floor(IAttributeInstance attribute) {
        put(attribute, PERCENT_ID, PERCENT_NAME, 1, 0);
        put(attribute, FLAT_ID, FLAT_NAME, 0, 0);
        double without = attribute.getAttributeValue();
        if (without >= FLOOR) {
            // Only JAFS pulled it below the floor: our flat must bring it exactly to the floor.
            put(attribute, FLAT_ID, FLAT_NAME, 0, 1.0);
            double slope = attribute.getAttributeValue() - without;
            double flat = slope > 0 ? (FLOOR - without) / slope : 0;
            put(attribute, FLAT_ID, FLAT_NAME, 0, flat);
        }
        // Rounding or another mod's multiplier can still leave it a hair under the floor.
        for (int i = 0; i < 4 && attribute.getAttributeValue() < FLOOR; i++) {
            AttributeModifier current = attribute.getModifier(FLAT_ID);
            double flat = current == null ? 0 : current.getAmount();
            put(attribute, FLAT_ID, FLAT_NAME, 0, flat + (FLOOR - attribute.getAttributeValue()) + 1e-6);
        }
        return true;
    }

    private static boolean put(IAttributeInstance attribute, UUID id, String name, int operation, double amount) {
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
        modifier.setSaved(false);
        attribute.applyModifier(modifier);
        return true;
    }
}
