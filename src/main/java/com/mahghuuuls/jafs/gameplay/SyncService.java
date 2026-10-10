package com.mahghuuuls.jafs.gameplay;

import com.mahghuuuls.jafs.config.FoodDefinitionFile;
import com.mahghuuuls.jafs.core.FoodSlot;
import com.mahghuuuls.jafs.network.FoodStateSnapshotMessage;
import com.mahghuuuls.jafs.network.JafsNetwork;
import com.mahghuuuls.jafs.state.FoodStateCapability;
import com.mahghuuuls.jafs.state.IFoodState;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Owner of the client contract: one snapshot per player when something changed, and a
 * correction every {@link #CORRECTION_TICKS} otherwise, so the client's own countdown never
 * drifts more than the tolerance.
 */
public class SyncService {

    public static final int CORRECTION_TICKS = 100;

    public static final SyncService NONE = new SyncService() {
        @Override
        public void markDirty(EntityPlayerMP player) {
        }

        @Override
        public void tick(EntityPlayerMP player, double effectiveRate) {
        }
    };

    /** Per-player send bookkeeping; mutable so a tick allocates nothing. */
    private static final class Tracker {
        int ticksSinceSend = CORRECTION_TICKS;
        boolean dirty;
        double lastRate = Double.NaN;
    }

    private final Map<UUID, Tracker> trackers = new HashMap<UUID, Tracker>();

    private Tracker tracker(EntityPlayerMP player) {
        Tracker tracker = trackers.get(player.getUniqueID());
        if (tracker == null) {
            tracker = new Tracker();
            trackers.put(player.getUniqueID(), tracker);
        }
        return tracker;
    }

    public void markDirty(EntityPlayerMP player) {
        tracker(player).dirty = true;
    }

    /** Sends now, regardless of dirtiness (login, respawn, dimension change). */
    public void sendNow(EntityPlayerMP player, double effectiveRate) {
        IFoodState state = FoodStateCapability.get(player);
        if (state == null) {
            return;
        }
        List<FoodStateSnapshotMessage.Entry> entries = new ArrayList<FoodStateSnapshotMessage.Entry>();
        for (FoodSlot slot : state.slots()) {
            entries.add(new FoodStateSnapshotMessage.Entry(slot.definitionId(), slot.remainingTicks(), slot.fullTicks(),
                    ActiveFoodService.isEligible(slot)));
        }
        JafsNetwork.sendSnapshot(player, new FoodStateSnapshotMessage(entries, effectiveRate, FoodDefinitionFile.current().revision()));
        Tracker tracker = tracker(player);
        tracker.dirty = false;
        tracker.ticksSinceSend = 0;
        tracker.lastRate = effectiveRate;
    }

    /**
     * Called every server tick for every online player. Sends when something changed, when the
     * effective rate changed (the client counts down at that rate), or as a periodic correction
     * while foods are active.
     */
    public void tick(EntityPlayerMP player, double effectiveRate) {
        Tracker tracker = tracker(player);
        IFoodState state = FoodStateCapability.get(player);
        boolean hasFoods = state != null && !state.slots().isEmpty();
        boolean rateChanged = hasFoods && Double.compare(effectiveRate, tracker.lastRate) != 0;
        if (tracker.dirty || rateChanged || (hasFoods && tracker.ticksSinceSend >= CORRECTION_TICKS)) {
            sendNow(player, effectiveRate);
        } else if (tracker.ticksSinceSend < CORRECTION_TICKS) {
            tracker.ticksSinceSend++;
        }
    }

    public void forget(EntityPlayerMP player) {
        trackers.remove(player.getUniqueID());
    }
}
