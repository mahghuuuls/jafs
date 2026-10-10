package com.mahghuuuls.jafs.gameplay.hooks;

import com.mahghuuuls.jafs.core.RecoveryRules;
import com.mahghuuuls.jafs.diagnostic.Diag;
import com.mahghuuuls.jafs.gameplay.RecoveryService;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Damage adapters. Starvation can only reach a player if the hunger pin was bypassed; this
 * guard drops it before any hurt animation, and the diagnostic line makes the bypass visible.
 *
 * <p>For recovery (REQ-034) a hit counts when the amount after armor and resistance is above
 * zero, whether or not absorption soaked it. Vanilla fires {@code LivingHurtEvent} before armor
 * and {@code LivingDamageEvent} after absorption, so the hurt handler remembers the absorption
 * and the damage handler adds back what absorption took. A fully blocked or cancelled hit never
 * reaches the damage event; a hit reduced to zero by armor returns before it. Both handlers run
 * last so other mods' changes to the amount are seen.
 */
public final class DamageHook {

    private final RecoveryService recovery;
    /** Absorption at the hurt event, consumed by the damage event of the same hit. A hit that armor zeroes
     *  leaves its entry until the player's next hurt event overwrites it; one float per player at most. */
    private final Map<UUID, Float> absorptionAtHurt = new HashMap<UUID, Float>();

    public DamageHook(RecoveryService recovery) {
        this.recovery = recovery;
    }

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

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onHurt(LivingHurtEvent event) {
        EntityPlayerMP player = serverPlayer(event.getEntityLiving());
        if (player != null && event.getAmount() > 0) {
            absorptionAtHurt.put(player.getUniqueID(), player.getAbsorptionAmount());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDamage(LivingDamageEvent event) {
        EntityPlayerMP player = serverPlayer(event.getEntityLiving());
        if (player == null) {
            return;
        }
        Float before = absorptionAtHurt.remove(player.getUniqueID());
        float absorptionBefore = before == null ? player.getAbsorptionAmount() : before;
        if (RecoveryRules.damageCounts(event.getAmount(), absorptionBefore, player.getAbsorptionAmount())) {
            recovery.onDamage(player, event.getAmount() + Math.max(0, absorptionBefore - player.getAbsorptionAmount()));
        }
    }

    private static EntityPlayerMP serverPlayer(Object entity) {
        return entity instanceof EntityPlayerMP ? (EntityPlayerMP) entity : null;
    }
}
