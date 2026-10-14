package com.mahghuuuls.jafs.api;

import net.minecraft.entity.player.EntityPlayerMP;

/**
 * An addon's percent contributions to a player's recovery rate and depletion rate (REQ-060),
 * for example from equipment or effects. JAFS reads the provider when it recomputes a player's
 * bonuses; an addon whose contribution changed calls {@link JafsApi#markDirty} so the new value
 * applies at once. Return 0 for no contribution. Positive speeds a rate up, negative slows it;
 * the sum with the food modifiers is floored so a rate never goes below zero.
 */
public interface IRateModifierProvider {

    double recoveryRatePercent(EntityPlayerMP player);

    double depletionRatePercent(EntityPlayerMP player);
}
