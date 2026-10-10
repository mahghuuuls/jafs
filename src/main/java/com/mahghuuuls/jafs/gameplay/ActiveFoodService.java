package com.mahghuuuls.jafs.gameplay;

import com.mahghuuuls.jafs.config.GameplayConfig;
import com.mahghuuuls.jafs.core.FoodDefinition;
import com.mahghuuuls.jafs.core.FoodSlot;
import com.mahghuuuls.jafs.core.FoodTimers;
import com.mahghuuuls.jafs.core.MealRules;
import com.mahghuuuls.jafs.diagnostic.Diag;
import com.mahghuuuls.jafs.state.FoodStateCapability;
import com.mahghuuuls.jafs.state.IFoodState;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The only writer of a player's active foods. Every change (a meal, a tick's expirations, a
 * clear, later payments and reload consequences) goes through here, so the bonus recompute,
 * the diagnostic line, and the sync mark can never be forgotten.
 */
public final class ActiveFoodService {

    private final Map<UUID, FoodTimers.Clock> clocks = new HashMap<UUID, FoodTimers.Clock>();
    private volatile BonusHook bonuses = BonusHook.NONE;
    private volatile SyncService sync = SyncService.NONE;

    ActiveFoodService() {
    }

    public void setBonusHook(BonusHook hook) {
        this.bonuses = hook == null ? BonusHook.NONE : hook;
    }

    public void setSyncService(SyncService service) {
        this.sync = service == null ? SyncService.NONE : service;
    }

    /** Applies an accepted meal outcome: a new slot, a refresh, or a replacement. */
    public void applyMeal(EntityPlayerMP player, FoodDefinition definition, MealRules.Outcome outcome) {
        IFoodState state = FoodStateCapability.get(player);
        if (state == null || !outcome.accepted()) {
            return;
        }
        List<FoodSlot> slots = state.slots();
        FoodSlot fresh = new FoodSlot(definition.id(), definition.fullTicks(), definition.fullTicks());
        switch (outcome.kind()) {
            case NEW_SLOT:
                slots.add(fresh);
                Diag.event("activated {} for {} in slot {}", definition.id(), player.getName(), slots.size() - 1);
                break;
            case REFRESH:
                slots.set(outcome.slot(), fresh);
                Diag.event("refreshed {} for {} in slot {}", definition.id(), player.getName(), outcome.slot());
                break;
            case REPLACE:
                FoodSlot removed = slots.set(outcome.slot(), fresh);
                Diag.event("replaced {} with {} for {} in slot {}", removed.definitionId(), definition.id(), player.getName(),
                        outcome.slot());
                break;
            default:
                return;
        }
        changed(player, state, "meal");
    }

    /** One server tick of depletion; expirations are removed and reported. */
    public void tick(EntityPlayerMP player, double effectiveRate) {
        IFoodState state = FoodStateCapability.get(player);
        if (state == null || state.slots().isEmpty()) {
            return;
        }
        FoodTimers.Clock clock = clocks.get(player.getUniqueID());
        if (clock == null) {
            clock = new FoodTimers.Clock();
            clocks.put(player.getUniqueID(), clock);
        }
        int ticks = clock.step(effectiveRate);
        if (ticks == 0) {
            return;
        }
        int threshold = GameplayConfig.current().eligibilityThresholdPercent();
        int eligibleBefore = countEligible(state.slots(), threshold);
        List<FoodSlot> expired = FoodTimers.advance(state.slots(), ticks);
        if (expired.isEmpty()) {
            // Crossing the threshold is not a mutation, but the client shows it, so tell it now.
            if (countEligible(state.slots(), threshold) != eligibleBefore) {
                sync.markDirty(player);
            }
            return;
        }
        for (FoodSlot slot : expired) {
            state.slots().remove(slot);
            Diag.event("expired {} for {}", slot.definitionId(), player.getName());
        }
        changed(player, state, "expiration");
    }

    /** Removes every active food; returns how many were removed. */
    public int clearAll(EntityPlayerMP player, String cause) {
        IFoodState state = FoodStateCapability.get(player);
        if (state == null || state.slots().isEmpty()) {
            return 0;
        }
        int count = state.slots().size();
        List<String> ids = new ArrayList<String>();
        for (FoodSlot slot : state.slots()) {
            ids.add(slot.definitionId());
        }
        state.slots().clear();
        Diag.event("cleared {} foods for {} ({}): {}", count, player.getName(), cause, ids);
        changed(player, state, cause);
        return count;
    }

    public static boolean isEligible(FoodSlot slot) {
        return MealRules.isEligible(slot, GameplayConfig.current().eligibilityThresholdPercent());
    }

    private static int countEligible(List<FoodSlot> slots, int threshold) {
        int count = 0;
        for (FoodSlot slot : slots) {
            if (MealRules.isEligible(slot, threshold)) {
                count++;
            }
        }
        return count;
    }

    /** Forgets the per-player runtime carry (logout). */
    public void forget(EntityPlayerMP player) {
        clocks.remove(player.getUniqueID());
    }

    private void changed(EntityPlayerMP player, IFoodState state, String cause) {
        state.bumpRevision();
        bonuses.recompute(player, cause);
        sync.markDirty(player);
    }
}
