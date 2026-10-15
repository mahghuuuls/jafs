package com.mahghuuuls.jafs.gameplay.hooks;

import com.mahghuuuls.jafs.config.FoodDefinitionFile;
import com.mahghuuuls.jafs.core.ConsumableKey;
import com.mahghuuuls.jafs.gameplay.MealGate;
import com.mahghuuuls.jafs.item.JafsItems;
import com.mahghuuuls.jafs.network.ItemKeys;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.SPacketEntityMetadata;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Adapts item use to the meal gate. Start is cancelable and fires before anything is consumed
 * (the client is told to drop its hand, see below);
 * Finish fires after the item applied itself (hunger, effects, container), which is when the
 * food is activated. Tick is never cancelled: cancelling it finishes the item at once.
 */
public final class ItemUseHook {

    private final MealGate gate;

    public ItemUseHook(MealGate gate) {
        this.gate = gate;
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onStart(LivingEntityUseItemEvent.Start event) {
        EntityPlayerMP player = serverPlayer(event);
        if (player == null) {
            return;
        }
        MealGate.Decision decision;
        if (JafsItems.isBitterBerries(event.getItem())) {
            decision = gate.beginClear(player);
        } else {
            ConsumableKey key = ItemKeys.of(event.getItem(), FoodDefinitionFile.current());
            if (key == null) {
                return;
            }
            decision = gate.begin(player, key, new TextComponentString(event.getItem().getDisplayName()),
                    event.getItem().getItemUseAction() == net.minecraft.item.EnumAction.DRINK);
        }
        if (!decision.accepted) {
            event.setCanceled(true);
            player.sendStatusMessage(decision.message, true);
            // The client set its own hand active before asking; vanilla never tells it the server
            // refused. Resending the (unchanged) entity metadata makes the client reset its hand,
            // so no eating animation or crunch plays for a refused meal. Building this packet marks
            // the data manager clean, so metadata this player dirtied earlier in the same tick
            // (a sneak toggle in the same packet burst) reaches other watchers only at its next
            // change; rare and self-healing, accepted over a reflective dirty-mark of HAND_STATES.
            player.connection.sendPacket(new SPacketEntityMetadata(player.getEntityId(), player.getDataManager(), true));
        }
    }

    @SubscribeEvent
    public void onFinish(LivingEntityUseItemEvent.Finish event) {
        EntityPlayerMP player = serverPlayer(event);
        if (player == null) {
            return;
        }
        if (JafsItems.isBitterBerries(event.getItem())) {
            gate.finishClear(player);
            return;
        }
        gate.finish(player, ItemKeys.of(event.getItem(), FoodDefinitionFile.current()));
    }

    @SubscribeEvent
    public void onStop(LivingEntityUseItemEvent.Stop event) {
        EntityPlayerMP player = serverPlayer(event);
        if (player != null) {
            gate.cancel(player);
        }
    }

    private static EntityPlayerMP serverPlayer(LivingEntityUseItemEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayerMP)) {
            return null;
        }
        return (EntityPlayerMP) event.getEntityLiving();
    }
}
