package com.mahghuuuls.jafs.client;

import com.mahghuuuls.jafs.core.FoodSlot;
import com.mahghuuuls.jafs.core.FoodTimers;
import com.mahghuuuls.jafs.core.MealRules;
import com.mahghuuuls.jafs.network.FoodStateSnapshotMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * Whether the server would refuse a meal, predicted from the client's snapshot counted down to
 * now with the server's own rule (REQ-081a, REV-013). The prediction only decides whether to
 * cancel the client's use start; the server still decides for real and sends the message.
 * Plain Java, unit-tested.
 */
public final class MealPrediction {

    private MealPrediction() {
    }

    /** The snapshot's entries as the rule sees them after {@code elapsedTicks} at {@code rate}. */
    public static List<FoodSlot> slotsAfter(List<FoodStateSnapshotMessage.Entry> entries, double elapsedTicks, double rate) {
        List<FoodSlot> slots = new ArrayList<FoodSlot>(entries.size());
        for (FoodStateSnapshotMessage.Entry entry : entries) {
            slots.add(new FoodSlot(entry.definitionId, FoodTimers.remainingAfter(entry.remainingTicks, elapsedTicks, rate), entry.fullTicks));
        }
        return slots;
    }

    /** The rule's outcome for the definition on these slots; a refused outcome is what the client cancels on. */
    public static MealRules.Outcome predict(List<FoodSlot> slotsNow, String definitionId, int activeFoodLimit, int thresholdPercent) {
        return MealRules.decide(slotsNow, definitionId, activeFoodLimit, thresholdPercent);
    }

    public static boolean wouldRefuse(List<FoodSlot> slotsNow, String definitionId, int activeFoodLimit, int thresholdPercent) {
        return !predict(slotsNow, definitionId, activeFoodLimit, thresholdPercent).accepted();
    }
}
