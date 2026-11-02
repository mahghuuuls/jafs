package com.mahghuuuls.jafs.compat.setworks;

import com.mahghuuuls.jafs.api.JafsApi;
import com.mahghuuuls.jafs.api.PropertyOperation;
import com.mahghuuuls.jafs.api.PropertySpec;
import com.mahghuuuls.jafs.diagnostic.Diag;
import com.mahghuuuls.jafs.property.PropertyRegistry;
import com.mahghuuuls.jafs.property.RateText;
import com.mahghuuuls.setworks.api.CombinationRule;
import com.mahghuuuls.setworks.api.IEffectKind;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Setworks effect kind {@code jafs:property} (REQ-056c): a set bonus grants a percent of one
 * of JAFS's two rate properties while it is active. Parameters: {@code property} (one of
 * {@code jafs:recovery_rate}, {@code jafs:depletion_rate}) and {@code amount} (a percent inside
 * the property's limits). Attribute-backed properties and max health are not accepted: Setworks'
 * own {@code setworks:attribute} grants those. Stateless, as Setworks requires; the ledger is
 * {@link SetworksContributions}. JAFS recomputes the player's rates only when the ledger changed.
 */
public final class JafsPropertyEffectKind implements IEffectKind {

    public static final String ID = "jafs:property";
    public static final String PARAM_PROPERTY = "property";
    public static final String PARAM_AMOUNT = "amount";
    static final List<String> ALLOWED = Arrays.asList(PropertyRegistry.RECOVERY_RATE, PropertyRegistry.DEPLETION_RATE);

    private final SetworksContributions contributions;
    private final PropertyRegistry registry;

    public JafsPropertyEffectKind(SetworksContributions contributions, PropertyRegistry registry) {
        this.contributions = contributions;
        this.registry = registry;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public CombinationRule combinationRule() {
        return CombinationRule.SUM;
    }

    @Override
    public String validate(Map<String, String> params) {
        String property = params.get(PARAM_PROPERTY);
        if (property == null || !ALLOWED.contains(property)) {
            return "property must be one of " + ALLOWED + " (attributes and max health use setworks:attribute)";
        }
        String amountText = params.get(PARAM_AMOUNT);
        double amount;
        try {
            amount = Double.parseDouble(amountText == null ? "" : amountText.trim());
        } catch (NumberFormatException e) {
            return "amount must be a number of percent, not '" + amountText + "'";
        }
        if (Double.isNaN(amount) || Double.isInfinite(amount)) {
            return "amount must be a finite number of percent";
        }
        PropertySpec spec = registry.find(property);
        if (spec == null) {
            return "property " + property + " is not registered with JAFS";
        }
        PropertySpec.Range range = spec.range(PropertyOperation.PERCENT);
        if (range == null || !range.contains(amount)) {
            return "amount " + amount + " is outside the percent limits " + range + " of " + property;
        }
        return null;
    }

    @Override
    public String describe(Map<String, String> params) {
        return params.get(PARAM_PROPERTY) + " " + RateText.signedPercent(parse(params));
    }

    @Override
    public void apply(EntityPlayerMP player, String instanceId, Map<String, String> params) {
        if (record(player.getUniqueID(), player.getName(), instanceId, params)) {
            markDirty(player, instanceId);
        }
    }

    @Override
    public void withdraw(EntityPlayerMP player, String instanceId, Map<String, String> params) {
        if (forget(player.getUniqueID(), player.getName(), instanceId)) {
            JafsApi.markDirty(player);
        }
    }

    /**
     * Records the instance in the ledger and returns whether the player's sums changed; the
     * recompute follows only then. Package-visible for tests without a player entity.
     */
    boolean record(UUID playerId, String playerName, String instanceId, Map<String, String> params) {
        boolean changed = contributions.put(playerId, instanceId, params.get(PARAM_PROPERTY), parse(params));
        if (changed) {
            Diag.event("setworks bonus {} applied {} for {}", instanceId, describe(params), playerName);
        }
        return changed;
    }

    /** Forgets the instance and returns whether anything was recorded for it. */
    boolean forget(UUID playerId, String playerName, String instanceId) {
        boolean changed = contributions.remove(playerId, instanceId);
        if (changed) {
            Diag.event("setworks bonus {} withdrawn for {}", instanceId, playerName);
        }
        return changed;
    }

    /**
     * The recompute after a recorded apply. Should it throw, the entry is taken back first so
     * that an instance Setworks marks as failed (and never withdraws) leaves nothing behind.
     */
    private void markDirty(EntityPlayerMP player, String instanceId) {
        try {
            JafsApi.markDirty(player);
        } catch (RuntimeException e) {
            contributions.remove(player.getUniqueID(), instanceId);
            throw e;
        }
    }

    /** The validated amount; Setworks calls apply only for validated parameters, so a bad text reads as 0. */
    static double parse(Map<String, String> params) {
        try {
            String text = params.get(PARAM_AMOUNT);
            double value = Double.parseDouble(text == null ? "" : text.trim());
            return Double.isNaN(value) || Double.isInfinite(value) ? 0 : value;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
