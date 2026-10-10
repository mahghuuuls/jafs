package com.mahghuuuls.jafs.gameplay;

import com.mahghuuuls.jafs.config.FoodDefinitionFile;
import com.mahghuuuls.jafs.config.GameplayConfig;
import com.mahghuuuls.jafs.diagnostic.Diag;
import com.mahghuuuls.jafs.network.JafsNetwork;
import com.mahghuuuls.jafs.property.PropertyRegistry;
import com.mahghuuuls.jafs.state.CloneRules;
import com.mahghuuuls.jafs.state.FoodState;
import com.mahghuuuls.jafs.state.FoodStateCapability;
import com.mahghuuuls.jafs.state.IFoodState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Player lifecycle on the server. Login: pin, reconcile the saved foods against the current
 * definitions (REQ-090), send definitions, recompute bonuses once (REQ-089), snapshot, operator
 * notices. Clone: {@link CloneRules} (death clears or keeps by option, the delay never survives a
 * death, an End return keeps everything). Respawn: recompute once and snapshot. Logout: forget
 * the per-session carries.
 */
public final class LifecycleHandlers {

    private final JafsGameplay gameplay;

    public LifecycleHandlers(JafsGameplay gameplay) {
        this.gameplay = gameplay;
    }

    @SubscribeEvent
    public void onLoggedIn(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.player instanceof EntityPlayerMP)) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        boolean corrected = HungerPin.apply(player);
        int reconciled = gameplay.service().reconcile(player, FoodDefinitionFile.current());
        JafsNetwork.sendDefinitions(player, FoodDefinitionFile.current(), PropertyRegistry.get().all());
        gameplay.bonuses().recompute(player, "login");
        gameplay.sync().sendNow(player, gameplay.ticker().effectiveRate(player));
        IFoodState state = FoodStateCapability.get(player);
        Diag.event("login player={} pinApplied={} foods={} reconciled={} recoveryDelayTicks={} definitionsSent={}", player.getName(),
                corrected, state == null ? -1 : state.slots().size(), reconciled, state == null ? -1 : state.recoveryDelayTicks(),
                FoodDefinitionFile.current().size());
        gameplay.notices().onLogin(player);
    }

    @SubscribeEvent
    public void onLoggedOut(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.player instanceof EntityPlayerMP)) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        gameplay.gate().cancel(player);
        gameplay.service().forget(player);
        gameplay.sync().forget(player);
        gameplay.recovery().forget(player);
        PropertyRegistry.get().rates().forget(player);
    }

    @SubscribeEvent
    public void onRespawn(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) event.player;
            gameplay.recovery().forget(player);
            gameplay.bonuses().recompute(player, "respawn");
            gameplay.sync().sendNow(player, gameplay.ticker().effectiveRate(player));
        }
    }

    @SubscribeEvent
    public void onChangedDimension(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            gameplay.sync().sendNow((EntityPlayerMP) event.player, gameplay.ticker().effectiveRate((EntityPlayerMP) event.player));
        }
    }

    @SubscribeEvent
    public void onClone(PlayerEvent.Clone event) {
        IFoodState original = FoodStateCapability.get(event.getOriginal());
        IFoodState clone = FoodStateCapability.get(event.getEntityPlayer());
        if (original == null || !(clone instanceof FoodState)) {
            return;
        }
        int before = original.slots().size();
        CloneRules.Result result = CloneRules.apply(original, (FoodState) clone, event.isWasDeath(),
                GameplayConfig.current().keepFoodsOnDeath());
        switch (result) {
            case CLEARED:
                Diag.event("clone player={} death: cleared {} foods, delay reset", event.getEntityPlayer().getName(), before);
                break;
            case KEPT:
                Diag.event("clone player={} death: kept {} foods (keepFoodsOnDeath), delay reset", event.getEntityPlayer().getName(),
                        clone.slots().size());
                break;
            default:
                // Vanilla copied the health onto a fresh entity whose maximum is the base, so health above the
                // base was clamped away. Reapply the bonuses now and restore the health; the respawn
                // recompute that follows is idempotent, so nothing applies twice (REQ-089).
                if (event.getEntityPlayer() instanceof EntityPlayerMP) {
                    EntityPlayerMP player = (EntityPlayerMP) event.getEntityPlayer();
                    gameplay.bonuses().recompute(player, "clone");
                    float health = event.getOriginal().getHealth();
                    if (health > player.getHealth()) {
                        player.setHealth(health);
                    }
                }
                Diag.event("clone player={} kept {} foods (not a death), health {}", event.getEntityPlayer().getName(),
                        clone.slots().size(), event.getEntityPlayer().getHealth());
                break;
        }
    }
}
