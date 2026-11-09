package com.mahghuuuls.jafs.compat.statusworks;

import com.mahghuuuls.jafs.api.JafsApi;
import com.mahghuuuls.statusworks.api.contribution.ContributionType;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.common.util.FakePlayer;

import java.util.UUID;

/**
 * A Statusworks contribution type for one JAFS rate. Its id is the rate's property id, so a
 * Statusworks definition writes {@code contributeType("jafs:recovery_rate", "fixed", 20)}. JAFS rates
 * exist on real players only: a mod's fake player never logs out, so entries recorded for it would
 * never be dropped. Statusworks reports each value and each end of a contribution; the ledger keeps
 * them, and the player's rates are recomputed only when the ledger changed, so the values
 * Statusworks reports again at every join cost nothing.
 */
public final class RateContributionType implements ContributionType {

    private final String rateId;
    private final StatusworksContributions contributions;

    public RateContributionType(String rateId, StatusworksContributions contributions) {
        this.rateId = rateId;
        this.contributions = contributions;
    }

    @Override
    public String id() {
        return rateId;
    }

    @Override
    public boolean appliesTo(EntityLivingBase entity) {
        return entity instanceof EntityPlayerMP && !(entity instanceof FakePlayer);
    }

    @Override
    public void onValue(EntityLivingBase entity, String key, double value) {
        if (appliesTo(entity)) {
            final EntityPlayerMP player = (EntityPlayerMP) entity;
            value(player.getUniqueID(), player.getName(), key, value, () -> JafsApi.markDirty(player));
        }
    }

    @Override
    public void onEnd(EntityLivingBase entity, String key) {
        if (appliesTo(entity)) {
            final EntityPlayerMP player = (EntityPlayerMP) entity;
            end(player.getUniqueID(), key, () -> JafsApi.markDirty(player));
        }
    }

    /** Records a value and runs {@code markDirty} only when the player's entries changed. */
    void value(UUID playerId, String playerName, String key, double value, Runnable markDirty) {
        if (contributions.put(playerId, playerName, key, rateId, value)) {
            markDirty.run();
        }
    }

    /** Drops a key and runs {@code markDirty} only when there was an entry to drop. */
    void end(UUID playerId, String key, Runnable markDirty) {
        if (contributions.remove(playerId, key)) {
            markDirty.run();
        }
    }
}
