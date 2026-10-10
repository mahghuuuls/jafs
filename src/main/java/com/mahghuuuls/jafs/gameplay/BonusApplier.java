package com.mahghuuuls.jafs.gameplay;

import com.mahghuuuls.jafs.api.IPropertyApplier;
import com.mahghuuuls.jafs.api.PropertySpec;
import com.mahghuuuls.jafs.config.FoodDefinitionFile;
import com.mahghuuuls.jafs.core.BonusMath;
import com.mahghuuuls.jafs.core.FoodDefinition;
import com.mahghuuuls.jafs.core.FoodSlot;
import com.mahghuuuls.jafs.core.Modifier;
import com.mahghuuuls.jafs.diagnostic.Diag;
import com.mahghuuuls.jafs.property.PropertyRegistry;
import com.mahghuuuls.jafs.state.FoodStateCapability;
import com.mahghuuuls.jafs.state.IFoodState;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Recomputes a player's combined modifiers from the active foods' current definitions and hands
 * each registered property its sums. Appliers are idempotent, so every registered property is
 * applied on every recompute: a property that lost its last modifier gets zero and cleans up.
 */
public final class BonusApplier implements BonusHook {

    private static final long WARNING_INTERVAL_MILLIS = 60_000L;

    private final PropertyRegistry registry;
    private final Map<String, Long> lastWarning = new java.util.HashMap<String, Long>();

    public BonusApplier(PropertyRegistry registry) {
        this.registry = registry;
    }

    /** The combined modifiers per property for the player's active foods, in property order. */
    public Map<String, BonusMath.Combined> combinedFor(EntityPlayerMP player) {
        IFoodState state = FoodStateCapability.get(player);
        if (state == null || state.slots().isEmpty()) {
            return Collections.emptyMap();
        }
        List<Modifier> modifiers = new ArrayList<Modifier>();
        for (FoodSlot slot : state.slots()) {
            FoodDefinition definition = FoodDefinitionFile.current().get(slot.definitionId());
            if (definition != null) {
                modifiers.addAll(definition.modifiers());
            }
        }
        return BonusMath.combine(modifiers);
    }

    @Override
    public void recompute(EntityPlayerMP player, String cause) {
        Map<String, BonusMath.Combined> combined = combinedFor(player);
        for (PropertySpec spec : registry.all()) {
            IPropertyApplier applier = registry.applier(spec.id());
            if (applier == null) {
                continue;
            }
            BonusMath.Combined value = combined.get(spec.id());
            if (value == null) {
                value = BonusMath.Combined.ZERO;
            }
            try {
                applier.apply(player, value.flat(), value.percent());
            } catch (RuntimeException e) {
                warnOncePerMinute(spec.id(), player, cause, e);
            }
        }
        Diag.event("bonuses recomputed for {} ({}): {}", player.getName(), cause, combined);
    }

    /** A provider's failure is logged with its stack trace, at most once per property per minute. */
    private void warnOncePerMinute(String propertyId, EntityPlayerMP player, String cause, RuntimeException e) {
        long now = System.currentTimeMillis();
        Long last = lastWarning.get(propertyId);
        if (last != null && now - last < WARNING_INTERVAL_MILLIS) {
            return;
        }
        lastWarning.put(propertyId, now);
        Diag.LOGGER.warn("Property " + propertyId + " failed to apply for " + player.getName() + " (" + cause
                + "); further failures of this property are not logged for a minute", e);
    }

    /** Inspect lines: one per property with a combined value, in property order. */
    public List<String> describe(EntityPlayerMP player) {
        List<String> lines = new ArrayList<String>();
        for (Map.Entry<String, BonusMath.Combined> entry : combinedFor(player).entrySet()) {
            lines.add(entry.getKey() + " " + entry.getValue());
        }
        return lines;
    }
}
