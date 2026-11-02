package com.mahghuuuls.jafs.compat.setworks;

import com.mahghuuuls.jafs.api.IRateModifierProvider;
import com.mahghuuuls.jafs.property.DescribableRateModifier;
import com.mahghuuuls.jafs.property.PropertyRegistry;
import com.mahghuuuls.jafs.property.RateText;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The ledger of what Setworks bonuses contribute to a player's two rates (REQ-056c): per player
 * and per Setworks effect instance, one percent for one rate property. Registered with JAFS as the
 * rate-modifier provider {@code jafs:setworks}, so the sums join the food modifiers through the
 * ordinary external-modifier path (REQ-060). Setworks owns when an instance is active and calls
 * the effect kind, which writes here; applying an instance that is already recorded changes
 * nothing, and removing an absent one changes nothing.
 *
 * <p>Logout: this ledger clears the player's entries at the highest event priority, before JAFS's
 * own logout handler forgets the player's rate and sync trackers and before Setworks' tracker
 * withdraws the instances. Setworks' withdrawals then find nothing here and mark nothing dirty,
 * so no tracker is re-created for a player who is gone. That order is the reason for the priority.
 */
public final class SetworksContributions implements IRateModifierProvider, DescribableRateModifier {

    /** One instance's contribution. */
    static final class Entry {
        final String propertyId;
        final double percent;

        Entry(String propertyId, double percent) {
            this.propertyId = propertyId;
            this.percent = percent;
        }
    }

    private final Map<UUID, Map<String, Entry>> players = new LinkedHashMap<UUID, Map<String, Entry>>();

    /**
     * Records an instance's contribution and returns whether the player's sums changed (a new
     * instance, or an instance whose property or percent differs from the recorded one).
     */
    public synchronized boolean put(UUID playerId, String instanceId, String propertyId, double percent) {
        Map<String, Entry> entries = players.get(playerId);
        if (entries == null) {
            entries = new LinkedHashMap<String, Entry>();
            players.put(playerId, entries);
        }
        Entry existing = entries.get(instanceId);
        if (existing != null && existing.propertyId.equals(propertyId) && Double.compare(existing.percent, percent) == 0) {
            return false;
        }
        entries.put(instanceId, new Entry(propertyId, percent));
        return true;
    }

    /** Forgets an instance's contribution and returns whether anything was recorded for it. */
    public synchronized boolean remove(UUID playerId, String instanceId) {
        Map<String, Entry> entries = players.get(playerId);
        if (entries == null || entries.remove(instanceId) == null) {
            return false;
        }
        if (entries.isEmpty()) {
            players.remove(playerId);
        }
        return true;
    }

    /** Forgets every contribution of a player (logout). Returns whether any was recorded. */
    public synchronized boolean clear(UUID playerId) {
        return players.remove(playerId) != null;
    }

    /** The recorded instance ids of a player, in registration order; empty when none. */
    public synchronized List<String> instances(UUID playerId) {
        Map<String, Entry> entries = players.get(playerId);
        return entries == null ? Collections.<String>emptyList() : new ArrayList<String>(entries.keySet());
    }

    /** The sum of the recorded percents for one property of a player. */
    public synchronized double percent(UUID playerId, String propertyId) {
        Map<String, Entry> entries = players.get(playerId);
        if (entries == null) {
            return 0;
        }
        double sum = 0;
        for (Entry entry : entries.values()) {
            if (entry.propertyId.equals(propertyId)) {
                sum += entry.percent;
            }
        }
        return sum;
    }

    /** One text per recorded instance, {@code <instance id>=<property> +N%}, in registration order. */
    public synchronized List<String> describe(UUID playerId) {
        Map<String, Entry> entries = players.get(playerId);
        List<String> lines = new ArrayList<String>();
        if (entries != null) {
            for (Map.Entry<String, Entry> entry : entries.entrySet()) {
                lines.add(entry.getKey() + "=" + entry.getValue().propertyId + " " + RateText.signedPercent(entry.getValue().percent));
            }
        }
        return lines;
    }

    @Override
    public List<String> describeContributions(EntityPlayerMP player) {
        return describe(player.getUniqueID());
    }

    @Override
    public double recoveryRatePercent(EntityPlayerMP player) {
        return percent(player.getUniqueID(), PropertyRegistry.RECOVERY_RATE);
    }

    @Override
    public double depletionRatePercent(EntityPlayerMP player) {
        return percent(player.getUniqueID(), PropertyRegistry.DEPLETION_RATE);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.player != null) {
            clear(event.player.getUniqueID());
        }
    }
}
