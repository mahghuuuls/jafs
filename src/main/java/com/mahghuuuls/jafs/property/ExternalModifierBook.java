package com.mahghuuuls.jafs.property;

import com.mahghuuuls.jafs.api.IRateModifierProvider;
import com.mahghuuuls.jafs.diagnostic.Diag;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Addon contributions to the two rate properties (REQ-060), keyed by contributor id. The sums
 * join the food modifiers' percent sums when the bonuses are recomputed; a provider that throws
 * contributes nothing and is reported once per minute.
 */
public final class ExternalModifierBook {

    private static final long WARNING_INTERVAL_MILLIS = 60_000L;

    private final Map<String, IRateModifierProvider> providers = new LinkedHashMap<String, IRateModifierProvider>();
    private final Map<String, Long> lastWarning = new LinkedHashMap<String, Long>();

    public synchronized void register(String providerId, IRateModifierProvider provider) {
        if (providerId == null || providerId.isEmpty() || provider == null) {
            throw new IllegalArgumentException("a provider id and a provider are required");
        }
        providers.put(providerId, provider);
        Diag.event("external modifier provider {} registered", providerId);
    }

    public synchronized void unregister(String providerId) {
        if (providers.remove(providerId) != null) {
            Diag.event("external modifier provider {} withdrawn", providerId);
        }
    }

    public synchronized int size() {
        return providers.size();
    }

    public synchronized double recoveryPercent(EntityPlayerMP player) {
        double sum = 0;
        for (Map.Entry<String, IRateModifierProvider> entry : providers.entrySet()) {
            sum += read(entry.getKey(), entry.getValue(), player, true);
        }
        return sum;
    }

    public synchronized double depletionPercent(EntityPlayerMP player) {
        double sum = 0;
        for (Map.Entry<String, IRateModifierProvider> entry : providers.entrySet()) {
            sum += read(entry.getKey(), entry.getValue(), player, false);
        }
        return sum;
    }

    private double read(String id, IRateModifierProvider provider, EntityPlayerMP player, boolean recovery) {
        try {
            double value = recovery ? provider.recoveryRatePercent(player) : provider.depletionRatePercent(player);
            return Double.isNaN(value) || Double.isInfinite(value) ? 0 : value;
        } catch (RuntimeException e) {
            long now = System.currentTimeMillis();
            Long last = lastWarning.get(id);
            if (last == null || now - last >= WARNING_INTERVAL_MILLIS) {
                lastWarning.put(id, now);
                Diag.LOGGER.warn("rate modifier provider {} failed for {}; contributing nothing", id, player.getName(), e);
            }
            return 0;
        }
    }
}
