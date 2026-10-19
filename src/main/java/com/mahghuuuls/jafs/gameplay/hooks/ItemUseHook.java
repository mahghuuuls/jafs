package com.mahghuuuls.jafs.gameplay.hooks;

import com.mahghuuuls.jafs.config.FoodDefinitionFile;
import com.mahghuuuls.jafs.core.ConsumableKey;
import com.mahghuuuls.jafs.gameplay.MealGate;
import com.mahghuuuls.jafs.item.JafsItems;
import com.mahghuuuls.jafs.network.ItemKeys;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.SPacketEntityMetadata;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.EnumActionResult;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Adapts item use to the meal gate. A refusal is decided at the right-click itself (D-D5): the
 * click is cancelled with a pass result before vanilla's item path runs, so nothing starts, the
 * inventory is not resent, and the client shows only the message. Start (a second line, for a
 * use that reaches it some other way) is cancelable and fires before anything is consumed; Finish
 * fires after the item applied itself (hunger, effects, container), which is when the food is
 * activated. Tick is never cancelled: cancelling it finishes the item at once.
 */
public final class ItemUseHook {

    private final MealGate gate;

    public ItemUseHook(MealGate gate) {
        this.gate = gate;
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getWorld().isRemote || !(event.getEntityPlayer() instanceof EntityPlayerMP)) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) event.getEntityPlayer();
        if (JafsItems.isBitterBerries(event.getItemStack())) {
            return; // always accepted; the clear path runs from Start and Finish
        }
        ConsumableKey key = ItemKeys.of(event.getItemStack(), FoodDefinitionFile.current());
        if (key == null) {
            return;
        }
        MealGate.Decision decision = gate.preview(player, key, new TextComponentString(event.getItemStack().getDisplayName()),
                event.getItemStack().getItemUseAction() == net.minecraft.item.EnumAction.DRINK);
        if (!decision.accepted) {
            event.setCanceled(true);
            event.setCancellationResult(EnumActionResult.PASS);
            player.sendStatusMessage(decision.message, true);
            // A client whose own prediction accepted has set its hand active; the (unchanged) metadata resets it.
            player.connection.sendPacket(new SPacketEntityMetadata(player.getEntityId(), player.getDataManager(), true));
        }
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
