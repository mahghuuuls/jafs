package com.mahghuuuls.jafs.api;

import net.minecraft.entity.player.EntityPlayerMP;

/**
 * How a property's combined value reaches a player. JAFS calls it whenever the player's
 * combined modifiers for the property may have changed (a meal, an expiration, a clear, a
 * reload, login, respawn), with the current flat sum and percent sum; a provider applies the
 * result its own way and must be idempotent: applying the same sums twice changes nothing.
 * Zero sums mean "no bonus": the provider removes whatever it applied.
 */
public interface IPropertyApplier {

    void apply(EntityPlayerMP player, double flatSum, double percentSum);
}
