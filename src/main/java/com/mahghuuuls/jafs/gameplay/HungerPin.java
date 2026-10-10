package com.mahghuuuls.jafs.gameplay;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.FoodStats;

/**
 * Holds every player's hunger stat at one fixed value. At this value vanilla neither starves
 * (needs 0), regenerates (needs 18 or more), blocks sprinting (needs 6 or less), nor refuses
 * food (needs 20). Applied at the start of every server player tick, before vanilla's own
 * food update runs, so the pin is in place when vanilla evaluates it.
 */
public final class HungerPin {

    /** The pinned food level (owner decision: fixed, not configurable). */
    public static final int PINNED_FOOD_LEVEL = 10;

    private HungerPin() {
    }

    /** Returns true when the value had to be corrected. Saturation is never written. */
    public static boolean apply(EntityPlayerMP player) {
        FoodStats stats = player.getFoodStats();
        if (stats.getFoodLevel() == PINNED_FOOD_LEVEL) {
            return false;
        }
        stats.setFoodLevel(PINNED_FOOD_LEVEL);
        return true;
    }
}
