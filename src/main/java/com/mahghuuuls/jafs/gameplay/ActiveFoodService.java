package com.mahghuuuls.jafs.gameplay;

import com.mahghuuuls.jafs.api.PaymentResult;
import com.mahghuuuls.jafs.config.FoodDefinitionFile;
import com.mahghuuuls.jafs.config.GameplayConfig;
import com.mahghuuuls.jafs.core.FoodDefinition;
import com.mahghuuuls.jafs.core.FoodSlot;
import com.mahghuuuls.jafs.core.FoodTimers;
import com.mahghuuuls.jafs.core.MealRules;
import com.mahghuuuls.jafs.core.ReloadRules;
import com.mahghuuuls.jafs.core.DefinitionSet;
import com.mahghuuuls.jafs.core.DisplayFormat;
import com.mahghuuuls.jafs.diagnostic.Diag;
import com.mahghuuuls.jafs.state.FoodStateCapability;
import com.mahghuuuls.jafs.state.IFoodState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.TextComponentTranslation;

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
            notifyExpiry(player, slot.definitionId());
        }
        changed(player, state, "expiration");
    }

    /** Tells the player a food ran out on its own (not by a payment), naming it as the HUD does. */
    private static void notifyExpiry(EntityPlayerMP player, String definitionId) {
        FoodDefinition definition = FoodDefinitionFile.current().get(definitionId);
        if (definition == null) {
            return; // the definition vanished with a reload; nothing to name
        }
        net.minecraft.item.ItemStack stack = FoodNames.firstStack(definition);
        String key = FoodNames.isDrink(stack) ? MESSAGE_EXPIRED_DRINK : MESSAGE_EXPIRED;
        player.sendStatusMessage(new TextComponentTranslation(key, FoodNames.displayName(definition, stack)), true);
    }

    /** The largest cost in seconds that fits the tick arithmetic; anything above is never affordable. */
    public static final int MAX_PAYMENT_SECONDS = Integer.MAX_VALUE / FoodDefinition.TICKS_PER_SECOND;
    /** The notice keys for a food that ran out (REQ-068a): "You could eat more <name>." or "... drink more ...". */
    public static final String MESSAGE_EXPIRED = "message.jafs.expired";
    public static final String MESSAGE_EXPIRED_DRINK = "message.jafs.expired_drink";

    /** Whether a payment of the seconds would succeed now (REQ-048). */
    public boolean canPay(EntityPlayerMP player, int seconds) {
        IFoodState state = FoodStateCapability.get(player);
        if (state == null || seconds > MAX_PAYMENT_SECONDS) {
            return false;
        }
        return FoodTimers.canPay(state.slots(), Math.max(0, seconds) * FoodDefinition.TICKS_PER_SECOND);
    }

    /**
     * A duration payment (REQ-047 to REQ-050): one check and deduction on the server thread;
     * foods that reach zero expire with the usual consequences; a failed payment changes nothing.
     */
    public PaymentResult pay(EntityPlayerMP player, int seconds, String cause) {
        if (player.getServer() == null || !player.getServer().isCallingFromMinecraftThread()) {
            Diag.event("payment {}s by {} for {} refused: not on the server thread", seconds, cause, player.getName());
            return PaymentResult.failed(PaymentResult.Status.NOT_ON_SERVER, seconds);
        }
        IFoodState state = FoodStateCapability.get(player);
        if (state == null) {
            return PaymentResult.failed(PaymentResult.Status.NOT_ON_SERVER, seconds);
        }
        if (seconds > MAX_PAYMENT_SECONDS) {
            Diag.event("payment {}s by {} for {}: failed, cost beyond the supported range", seconds, cause, player.getName());
            return PaymentResult.failed(PaymentResult.Status.NOT_AFFORDABLE, seconds);
        }
        if (seconds > 0 && state.slots().isEmpty()) {
            Diag.event("payment {}s by {} for {}: failed, no active food", seconds, cause, player.getName());
            return PaymentResult.failed(PaymentResult.Status.NO_ACTIVE_FOOD, seconds);
        }
        int ticks = Math.max(0, seconds) * FoodDefinition.TICKS_PER_SECOND;
        List<FoodSlot> expired = FoodTimers.pay(state.slots(), ticks);
        if (expired == null) {
            Diag.event("payment {}s by {} for {}: failed, no food covers the cost", seconds, cause, player.getName());
            return PaymentResult.failed(PaymentResult.Status.NOT_AFFORDABLE, seconds);
        }
        List<String> expiredIds = new ArrayList<String>();
        for (FoodSlot slot : expired) {
            state.slots().remove(slot);
            expiredIds.add(slot.definitionId());
            Diag.event("expired {} for {} (payment)", slot.definitionId(), player.getName());
        }
        Diag.event("payment {}s by {} for {}: ok, expired={}", seconds, cause, player.getName(), expiredIds);
        if (ticks > 0) {
            changed(player, state, "payment");
        }
        return new PaymentResult(PaymentResult.Status.SUCCESS, seconds, expiredIds);
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

    /**
     * A successful reload: removed, disabled, or inert definitions clear their instances; a changed
     * full duration caps the remaining time; modifiers are recomputed from the new definitions
     * either way (REQ-071). Returns the number of slot changes.
     */
    public int applyReloadChanges(EntityPlayerMP player, DefinitionSet definitions) {
        return applyDefinitions(player, definitions, "reload", false);
    }

    /**
     * Login reconciliation (REQ-090): the same consequences for a saved state against the current
     * definitions, with one warning per player that names the dropped foods. A change recomputes
     * the bonuses as every change does; the login recompute that follows is idempotent.
     */
    public int reconcile(EntityPlayerMP player, DefinitionSet definitions) {
        return applyDefinitions(player, definitions, "reconcile", true);
    }

    private int applyDefinitions(EntityPlayerMP player, DefinitionSet definitions, String cause, boolean warnDropped) {
        IFoodState state = FoodStateCapability.get(player);
        if (state == null || state.slots().isEmpty()) {
            return 0;
        }
        List<ReloadRules.Change> changes = ReloadRules.apply(state.slots(), definitions);
        List<String> dropped = new ArrayList<String>();
        for (ReloadRules.Change change : changes) {
            switch (change.kind) {
                case REMOVED:
                    dropped.add(change.definitionId);
                    Diag.event("{}: removed {} from {}", cause, change.definitionId, player.getName());
                    break;
                case CAPPED:
                    Diag.event("{}: capped {} remaining to {} for {}", cause, change.definitionId,
                            DisplayFormat.durationTicks(change.remainingTicks), player.getName());
                    break;
                default:
                    Diag.event("{}: {} full duration is now {} for {}", cause, change.definitionId,
                            DisplayFormat.durationTicks(change.fullTicks), player.getName());
                    break;
            }
        }
        if (warnDropped && !dropped.isEmpty()) {
            Diag.LOGGER.warn("dropped saved foods of {} whose definition no longer exists: {}", player.getName(), dropped);
        }
        if (!changes.isEmpty() || !"reconcile".equals(cause)) {
            changed(player, state, cause);
        }
        return changes.size();
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
