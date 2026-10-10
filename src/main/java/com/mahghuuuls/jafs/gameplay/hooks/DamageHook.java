package com.mahghuuuls.jafs.gameplay.hooks;

import com.mahghuuuls.jafs.diagnostic.Diag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Damage adapters. Starvation can only reach a player if the hunger pin was bypassed; this
 * guard drops it before any hurt animation, and the diagnostic line makes the bypass visible.
 */
public final class DamageHook {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onAttack(LivingAttackEvent event) {
        if (event.getSource() != DamageSource.STARVE || !(event.getEntityLiving() instanceof EntityPlayer)) {
            return;
        }
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if (player.world.isRemote) {
            return;
        }
        event.setCanceled(true);
        Diag.event("starvation cancelled player={} foodLevel={}", player.getName(), player.getFoodStats().getFoodLevel());
    }
}
