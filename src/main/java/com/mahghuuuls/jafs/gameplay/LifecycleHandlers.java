package com.mahghuuuls.jafs.gameplay;

import com.mahghuuuls.jafs.config.FoodDefinitionFile;
import com.mahghuuuls.jafs.diagnostic.Diag;
import com.mahghuuuls.jafs.network.JafsNetwork;
import com.mahghuuuls.jafs.property.PropertyRegistry;
import com.mahghuuuls.jafs.state.FoodState;
import com.mahghuuuls.jafs.state.FoodStateCapability;
import com.mahghuuuls.jafs.state.IFoodState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Player lifecycle on the server. Death handling and reconciliation are added by later work;
 * for now a recreated player entity that did not die (End return) keeps its state, and login
 * applies the hunger pin at once and writes the one diagnostic line for it.
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
        JafsNetwork.sendDefinitions(player, FoodDefinitionFile.current(), PropertyRegistry.get().all());
        gameplay.bonuses().recompute(player, "login");
        gameplay.sync().sendNow(player, gameplay.ticker().effectiveRate(player));
        IFoodState state = FoodStateCapability.get(player);
        Diag.event("login player={} pinApplied={} foods={} recoveryDelayTicks={} definitionsSent={}", player.getName(), corrected,
                state == null ? -1 : state.slots().size(), state == null ? -1 : state.recoveryDelayTicks(),
                FoodDefinitionFile.current().size());
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
        PropertyRegistry.get().rates().forget(player);
    }

    @SubscribeEvent
    public void onRespawn(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) event.player;
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
        if (event.isWasDeath()) {
            return;
        }
        IFoodState original = FoodStateCapability.get(event.getOriginal());
        IFoodState clone = FoodStateCapability.get(event.getEntityPlayer());
        if (original != null && clone instanceof FoodState) {
            ((FoodState) clone).copyFrom(original);
            Diag.event("clone player={} kept foods={} (not a death)", event.getEntityPlayer().getName(),
                    clone.slots().size());
        }
    }
}
