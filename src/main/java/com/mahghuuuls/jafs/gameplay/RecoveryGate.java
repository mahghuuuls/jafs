package com.mahghuuuls.jafs.gameplay;

import net.minecraft.entity.player.EntityPlayerMP;

/** Something outside JAFS that blocks recovery for a player (the Inhibited effect, wired by a later issue). */
public interface RecoveryGate {

    RecoveryGate NONE = new RecoveryGate() {
        @Override
        public boolean blocksRecovery(EntityPlayerMP player) {
            return false;
        }
    };

    boolean blocksRecovery(EntityPlayerMP player);
}
