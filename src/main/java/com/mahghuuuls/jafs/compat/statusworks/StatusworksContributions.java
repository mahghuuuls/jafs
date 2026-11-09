package com.mahghuuuls.jafs.compat.statusworks;

import com.mahghuuuls.jafs.api.IRateModifierProvider;
import com.mahghuuuls.jafs.api.PropertyOperation;
import com.mahghuuuls.jafs.api.PropertySpec;
import com.mahghuuuls.jafs.property.DescribableRateModifier;
import com.mahghuuuls.jafs.property.PropertyRegistry;
import com.mahghuuuls.jafs.property.RateText;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The ledger of what Statusworks contributions add to a player's two rates: per player, the
 * contribution key Statusworks reports (for example {@code status:mypack:well_fed#0}) with the rate
 * and its percent. Statusworks reports a value again at every join, so a repeated value changes
 * nothing. A value past the rate's percent limits is used at the nearer limit, and that is logged
 * once per player and key until the key ends. Statusworks sends no end at logout and reports the
 * values again at the next join, so a player's entries are dropped when the player logs out. That
 * happens at the highest event priority, like the Setworks ledger, so both outside sources are gone
 * before JAFS's own logout handler forgets the player's rate and sync trackers.
 */
public final class StatusworksContributions implements IRateModifierProvider, DescribableRateModifier {

    /** Where the over-limit warning goes; the logger in the game, a list in tests. */
    public interface Warnings {
        void warn(String message);
    }

    static final class Entry {
        final String rateId;
        final double percent;

        Entry(String rateId, double percent) {
            this.rateId = rateId;
            this.percent = percent;
        }
    }

    private final Map<UUID, Map<String, Entry>> players = new LinkedHashMap<UUID, Map<String, Entry>>();
    private final Map<UUID, Set<String>> warned = new LinkedHashMap<UUID, Set<String>>();
    private final PropertyRegistry registry;
    private final Warnings warnings;

    public StatusworksContributions(PropertyRegistry registry, Warnings warnings) {
        this.registry = registry;
        this.warnings = warnings;
    }

    /**
     * Records the value Statusworks reports for a key, held inside the rate's percent limits.
     * Returns whether the player's recorded entries changed, so the caller recomputes only then.
     */
    public synchronized boolean put(UUID playerId, String playerName, String key, String rateId, double value) {
        double percent = withinLimits(playerId, playerName, key, rateId, value);
        Map<String, Entry> entries = players.get(playerId);
        if (entries == null) {
            entries = new LinkedHashMap<String, Entry>();
            players.put(playerId, entries);
        }
        Entry existing = entries.get(key);
        if (existing != null && existing.rateId.equals(rateId) && Double.compare(existing.percent, percent) == 0) {
            return false;
        }
        entries.put(key, new Entry(rateId, percent));
        return true;
    }

    /** Drops a key's entry when its contribution ends; returns whether there was one. */
    public synchronized boolean remove(UUID playerId, String key) {
        Set<String> keys = warned.get(playerId);
        if (keys != null) {
            keys.remove(key);
            if (keys.isEmpty()) {
                warned.remove(playerId);
            }
        }
        Map<String, Entry> entries = players.get(playerId);
        if (entries == null || entries.remove(key) == null) {
            return false;
        }
        if (entries.isEmpty()) {
            players.remove(playerId);
        }
        return true;
    }

    /** Drops every entry of a player; returns whether there were any. */
    public synchronized boolean clear(UUID playerId) {
        warned.remove(playerId);
        return players.remove(playerId) != null;
    }

    public synchronized List<String> keys(UUID playerId) {
        Map<String, Entry> entries = players.get(playerId);
        return entries == null ? Collections.<String>emptyList() : new ArrayList<String>(entries.keySet());
    }

    public synchronized double percent(UUID playerId, String rateId) {
        Map<String, Entry> entries = players.get(playerId);
        if (entries == null) {
            return 0;
        }
        double sum = 0;
        for (Entry entry : entries.values()) {
            if (entry.rateId.equals(rateId)) {
                sum += entry.percent;
            }
        }
        return sum;
    }

    /** One text per recorded key, {@code <key>=<rate> +N%}, in the order the keys arrived. */
    public synchronized List<String> describe(UUID playerId) {
        Map<String, Entry> entries = players.get(playerId);
        List<String> lines = new ArrayList<String>();
        if (entries != null) {
            for (Map.Entry<String, Entry> entry : entries.entrySet()) {
                lines.add(entry.getKey() + "=" + entry.getValue().rateId + " " + RateText.signedPercent(entry.getValue().percent));
            }
        }
        return lines;
    }

    private double withinLimits(UUID playerId, String playerName, String key, String rateId, double value) {
        PropertySpec spec = registry.find(rateId);
        PropertySpec.Range range = spec == null ? null : spec.range(PropertyOperation.PERCENT);
        if (range == null) {
            return value;
        }
        boolean notANumber = Double.isNaN(value);
        double used = notANumber ? 0 : Math.max(range.min(), Math.min(range.max(), value));
        if (Double.compare(used, value) != 0) {
            Set<String> keys = warned.get(playerId);
            if (keys == null) {
                keys = new HashSet<String>();
                warned.put(playerId, keys);
            }
            if (keys.add(key)) {
                String sent = notANumber ? "a value that is not a number" : RateText.signedPercent(value);
                warnings.warn("Statusworks contribution " + key + " on " + playerName + " sent " + sent + " for " + rateId + ", limits "
                        + RateText.signedPercent(range.min()) + " to " + RateText.signedPercent(range.max()) + "; JAFS uses "
                        + RateText.signedPercent(used));
            }
        }
        return used;
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
