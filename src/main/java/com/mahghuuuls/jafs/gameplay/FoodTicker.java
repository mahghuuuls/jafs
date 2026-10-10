package com.mahghuuuls.jafs.gameplay;

import com.mahghuuuls.jafs.config.GameplayConfig;
import com.mahghuuuls.jafs.config.GameplaySettings;
import com.mahghuuuls.jafs.core.BonusMath;
import com.mahghuuuls.jafs.property.EffectiveRates;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;

/**
 * Per-player server tick: depletion through the service, then recovery, then the sync service. A spectator's
 * effective rate is zero, so their timers stand still and the client stops counting. The
 * effective depletion rate is the configured base times the food multiplier held by the rate
 * properties.
 */
public final class FoodTicker {

    private final ActiveFoodService service;
    private final SyncService sync;
    private final EffectiveRates rates;
    private final RecoveryService recovery;

    public FoodTicker(ActiveFoodService service, SyncService sync, EffectiveRates rates, RecoveryService recovery) {
        this.service = service;
        this.sync = sync;
        this.rates = rates;
        this.recovery = recovery;
    }

    /** The base the depletion percent applies to: the configured rate plus the Hunger effect level times the increment (REQ-045). */
    public static double baseDepletionRate(EntityPlayerMP player) {
        GameplaySettings settings = GameplayConfig.current();
        return BonusMath.baseDepletionRate(settings.depletionRate(), hungerLevel(player), settings.hungerEffectIncrement());
    }

    /** The Hunger effect level: 0 without the effect, 1 for Hunger I, 2 for Hunger II. */
    public static int hungerLevel(EntityPlayerMP player) {
        PotionEffect effect = player.getActivePotionEffect(MobEffects.HUNGER);
        return effect == null ? 0 : effect.getAmplifier() + 1;
    }

    public double effectiveRate(EntityPlayerMP player) {
        return rates.depletionRate(player, baseDepletionRate(player));
    }

    public void tick(EntityPlayerMP player) {
        // A spectator's timers stand still: rate 0 pauses depletion and tells the client to stop counting.
        double rate = player.isSpectator() ? 0.0 : effectiveRate(player);
        service.tick(player, rate);
        recovery.tick(player);
        sync.tick(player, rate);
    }
}
