package com.mahghuuuls.jafs.gameplay;

import com.mahghuuuls.jafs.config.FoodDefinitionFile;
import com.mahghuuuls.jafs.config.GameplayConfig;
import com.mahghuuuls.jafs.config.GameplaySettings;
import com.mahghuuuls.jafs.core.ConsumableKey;
import com.mahghuuuls.jafs.core.DefinitionSet;
import com.mahghuuuls.jafs.core.FoodDefinition;
import com.mahghuuuls.jafs.core.MealRules;
import com.mahghuuuls.jafs.core.PendingMeals;
import com.mahghuuuls.jafs.diagnostic.Diag;
import com.mahghuuuls.jafs.state.FoodStateCapability;
import com.mahghuuuls.jafs.state.IFoodState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

import java.util.UUID;

/**
 * Turns a consumption attempt into a decision. {@link #begin} runs when eating starts and may
 * refuse (nothing has been consumed yet); {@link #finish} runs when vanilla has finished the
 * item and applies the meal after re-checking the rules against the current state.
 */
public final class MealGate {

    public static final String MESSAGE_INELIGIBLE = "message.jafs.ineligible";
    public static final String MESSAGE_NO_SLOT = "message.jafs.no_slot";
    public static final String MESSAGE_NOTHING_TO_CLEAR = "message.jafs.nothing_to_clear";
    /** The pending marker for a Bitter Berries use. A definition could carry this id in theory; the two paths then fail closed (nothing activates, nothing clears), and a player uses one hand at a time. */
    private static final String CLEAR_PENDING = "<clear>";

    /** The result of {@link #begin}: refused with a message to show, or accepted. */
    public static final class Decision {
        public final boolean accepted;
        public final ITextComponent message;

        private Decision(boolean accepted, ITextComponent message) {
            this.accepted = accepted;
            this.message = message;
        }

        /** Accepted, or not a configured food at all: vanilla proceeds. */
        public static final Decision ACCEPTED = new Decision(true, null);
    }

    private final ActiveFoodService service;
    private final PendingMeals<UUID> pending = new PendingMeals<UUID>();

    public MealGate(ActiveFoodService service) {
        this.service = service;
    }

    /**
     * Decides a meal that is about to start. {@code foodName} is the consumable's name component
     * for the refusal message.
     */
    public Decision begin(EntityPlayerMP player, ConsumableKey key, ITextComponent foodName) {
        pending.cancel(player.getUniqueID());
        FoodDefinition definition = resolve(player, key);
        if (definition == null) {
            return Decision.ACCEPTED;
        }
        IFoodState state = FoodStateCapability.get(player);
        if (state == null) {
            return Decision.ACCEPTED;
        }
        GameplaySettings settings = GameplayConfig.current();
        MealRules.Outcome outcome = MealRules.decide(state.slots(), definition.id(), settings.activeFoodLimit(),
                settings.eligibilityThresholdPercent());
        if (!outcome.accepted()) {
            Diag.event("meal refused {} for {}: {}", definition.id(), player.getName(), outcome.kind());
            ITextComponent message = outcome.kind() == MealRules.Kind.REFUSE_INELIGIBLE
                    ? new TextComponentTranslation(MESSAGE_INELIGIBLE, foodName)
                    : new TextComponentTranslation(MESSAGE_NO_SLOT);
            return new Decision(false, message);
        }
        pending.begin(player.getUniqueID(), definition.id());
        Diag.event("meal accepted {} for {}: {}", definition.id(), player.getName(), outcome);
        return Decision.ACCEPTED;
    }

    /**
     * Bitter Berries about to be eaten (REQ-054, REQ-055): refused when no food is active, so
     * nothing is consumed; otherwise accepted outside the slot rules.
     */
    public Decision beginClear(EntityPlayerMP player) {
        pending.cancel(player.getUniqueID());
        IFoodState state = FoodStateCapability.get(player);
        if (state == null || state.slots().isEmpty()) {
            Diag.event("clear refused for {}: nothing to clear", player.getName());
            return new Decision(false, new TextComponentTranslation(MESSAGE_NOTHING_TO_CLEAR));
        }
        pending.begin(player.getUniqueID(), CLEAR_PENDING);
        Diag.event("clear accepted for {}: {} foods active", player.getName(), state.slots().size());
        return Decision.ACCEPTED;
    }

    /** Bitter Berries eaten: every active food goes, with the cap and no refund. */
    public void finishClear(EntityPlayerMP player) {
        String expected = pending.take(player.getUniqueID());
        if (!CLEAR_PENDING.equals(expected)) {
            return;
        }
        service.clearAll(player, "bitter berries");
    }

    /** Applies the meal after vanilla consumed the item, re-deciding against the current state. */
    public void finish(EntityPlayerMP player, ConsumableKey key) {
        String expected = pending.take(player.getUniqueID());
        if (expected == null) {
            return;
        }
        FoodDefinition definition = resolve(player, key);
        if (definition == null || !definition.id().equals(expected)) {
            Diag.event("meal finish for {} resolved to {} instead of {}; nothing activated", player.getName(),
                    definition == null ? "nothing" : definition.id(), expected);
            return;
        }
        IFoodState state = FoodStateCapability.get(player);
        if (state == null) {
            return;
        }
        GameplaySettings settings = GameplayConfig.current();
        MealRules.Outcome outcome = MealRules.decide(state.slots(), definition.id(), settings.activeFoodLimit(),
                settings.eligibilityThresholdPercent());
        if (!outcome.accepted()) {
            Diag.event("meal finish for {} no longer allowed ({}); nothing activated", player.getName(), outcome.kind());
            return;
        }
        service.applyMeal(player, definition, outcome);
    }

    /** Drops a pending decision (use stopped, player left). */
    public void cancel(EntityPlayerMP player) {
        pending.cancel(player.getUniqueID());
    }

    private static FoodDefinition resolve(EntityPlayerMP player, ConsumableKey key) {
        if (key == null) {
            return null;
        }
        DefinitionSet.Resolution resolution = FoodDefinitionFile.current().resolve(key);
        if (resolution.isConflict()) {
            Diag.LOGGER.warn("Food definitions {} conflict for {} (same priority); none applies", resolution.conflicting(), key);
            return null;
        }
        return FoodDefinitionFile.current().configured(key);
    }
}
