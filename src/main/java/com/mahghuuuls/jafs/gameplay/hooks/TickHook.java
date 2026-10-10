package com.mahghuuuls.jafs.gameplay.hooks;

import com.mahghuuuls.jafs.gameplay.FoodTicker;
import com.mahghuuuls.jafs.gameplay.HungerPin;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Server player ticks: the hunger pin at START (before vanilla's food update, so starvation and
 * regen are evaluated at the pinned value) and again at END (so commands, packets, and other
 * mods that run between world ticks never see the one-tick drift vanilla's exhaustion or
 * Peaceful healing can cause); then, at END, the food ticker.
 */
public final class TickHook {

    private final FoodTicker ticker;

    public TickHook(FoodTicker ticker) {
        this.ticker = ticker;
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (!(event.player instanceof EntityPlayerMP)) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        HungerPin.apply(player);
        if (event.phase == TickEvent.Phase.END) {
            ticker.tick(player);
        }
    }
}
