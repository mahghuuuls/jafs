package com.mahghuuuls.jafs.property;

import com.mahghuuuls.jafs.api.IPropertyApplier;
import com.mahghuuuls.jafs.core.BonusMath;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The per-player percent sums of the two rate properties, written by their appliers and read
 * by the ticker and the recovery service. A rate property has no vanilla home, so its combined
 * value lives here; the multiplier is {@code max(0, 1 + percent / 100)} on the provider's base.
 */
public final class EffectiveRates {

    private static final class Rates {
        double recoveryPercent;
        double depletionPercent;
    }

    private final Map<UUID, Rates> byPlayer = new HashMap<UUID, Rates>();

    private Rates rates(EntityPlayerMP player) {
        Rates rates = byPlayer.get(player.getUniqueID());
        if (rates == null) {
            rates = new Rates();
            byPlayer.put(player.getUniqueID(), rates);
        }
        return rates;
    }

    public double recoveryPercent(EntityPlayerMP player) {
        Rates rates = byPlayer.get(player.getUniqueID());
        return rates == null ? 0 : rates.recoveryPercent;
    }

    public double depletionPercent(EntityPlayerMP player) {
        Rates rates = byPlayer.get(player.getUniqueID());
        return rates == null ? 0 : rates.depletionPercent;
    }

    public double recoveryMultiplier(EntityPlayerMP player) {
        return BonusMath.rateMultiplier(recoveryPercent(player));
    }

    public double depletionMultiplier(EntityPlayerMP player) {
        return BonusMath.rateMultiplier(depletionPercent(player));
    }

    /** The effective depletion rate: the base (config, plus later the Hunger effect) times the food multiplier. */
    public double depletionRate(EntityPlayerMP player, double base) {
        return base * depletionMultiplier(player);
    }

    /** The effective recovery rate in percent of maximum health per second: the base times the food multiplier. */
    public double recoveryRatePercentPerSecond(EntityPlayerMP player, double basePercent) {
        return basePercent * recoveryMultiplier(player);
    }

    public void forget(EntityPlayerMP player) {
        byPlayer.remove(player.getUniqueID());
    }

    public IPropertyApplier recoveryApplier() {
        return new IPropertyApplier() {
            @Override
            public void apply(EntityPlayerMP player, double flatSum, double percentSum) {
                rates(player).recoveryPercent = percentSum;
            }
        };
    }

    public IPropertyApplier depletionApplier() {
        return new IPropertyApplier() {
            @Override
            public void apply(EntityPlayerMP player, double flatSum, double percentSum) {
                rates(player).depletionPercent = percentSum;
            }
        };
    }
}
