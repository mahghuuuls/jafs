package com.mahghuuuls.jafs.gameplay;

import com.mahghuuuls.jafs.config.GameplayConfig;
import com.mahghuuuls.jafs.property.EffectiveRates;
import net.minecraft.entity.player.EntityPlayerMP;

/**
 * Per-player server tick: depletion through the service, then the sync service. A spectator's
 * effective rate is zero, so their timers stand still and the client stops counting. The
 * effective depletion rate is the configured base times the food multiplier held by the rate
 * properties.
 */
public final class FoodTicker {

    private final ActiveFoodService service;
    private final SyncService sync;
    private final EffectiveRates rates;

    public FoodTicker(ActiveFoodService service, SyncService sync, EffectiveRates rates) {
        this.service = service;
        this.sync = sync;
        this.rates = rates;
    }

    /** The base the depletion percent applies to: the configured rate (the Hunger effect increment joins it later). */
    public static double baseDepletionRate(EntityPlayerMP player) {
        return GameplayConfig.current().depletionRate();
    }

    public double effectiveRate(EntityPlayerMP player) {
        return rates.depletionRate(player, baseDepletionRate(player));
    }

    public void tick(EntityPlayerMP player) {
        // A spectator's timers stand still: rate 0 pauses depletion and tells the client to stop counting.
        double rate = player.isSpectator() ? 0.0 : effectiveRate(player);
        service.tick(player, rate);
        sync.tick(player, rate);
    }
}
