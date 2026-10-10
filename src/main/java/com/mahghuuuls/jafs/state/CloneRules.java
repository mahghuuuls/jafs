package com.mahghuuuls.jafs.state;

/**
 * What a recreated player entity inherits (REQ-088, REQ-041): not a death (End return) copies
 * everything; a death with {@code keepFoodsOnDeath} copies the foods and their remaining times;
 * a death without it copies nothing. The recovery delay never survives a death.
 */
public final class CloneRules {

    public enum Result {
        /** Not a death: foods and the recovery delay copied. */
        COPIED,
        /** Death with the option on: foods copied, the recovery delay reset. */
        KEPT,
        /** Death with the option off: nothing copied. */
        CLEARED
    }

    private CloneRules() {
    }

    public static Result apply(IFoodState original, FoodState clone, boolean death, boolean keepFoodsOnDeath) {
        if (!death) {
            clone.copyFrom(original);
            return Result.COPIED;
        }
        if (keepFoodsOnDeath) {
            clone.copyFrom(original);
            clone.setRecoveryDelayTicks(0);
            return Result.KEPT;
        }
        clone.slots().clear();
        clone.setRecoveryDelayTicks(0);
        clone.bumpRevision();
        return Result.CLEARED;
    }
}
