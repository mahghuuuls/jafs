package com.mahghuuuls.jafs.gameplay;

import net.minecraft.entity.player.EntityPlayerMP;

/**
 * What runs after a player's active foods changed: recompute and apply bonuses. The property
 * applier installs itself here; until then nothing is applied.
 */
public interface BonusHook {

    BonusHook NONE = new BonusHook() {
        @Override
        public void recompute(EntityPlayerMP player, String cause) {
        }
    };

    void recompute(EntityPlayerMP player, String cause);
}
