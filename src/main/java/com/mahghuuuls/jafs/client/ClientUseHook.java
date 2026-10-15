package com.mahghuuuls.jafs.client;

import com.mahghuuuls.jafs.core.ConsumableKey;
import com.mahghuuuls.jafs.core.DefinitionSet;
import com.mahghuuuls.jafs.core.FoodDefinition;
import com.mahghuuuls.jafs.core.MealRules;
import com.mahghuuuls.jafs.diagnostic.Diag;
import com.mahghuuuls.jafs.item.JafsItems;
import com.mahghuuuls.jafs.network.ItemKeys;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The client's half of a refused meal (REQ-081a, REV-013): the use-start event fires on the
 * client for the local player too, before the hand is set active; when the server's own rule,
 * run on the counted-down snapshot, would refuse the meal, the start is cancelled here so no
 * eating gesture begins. The use packet has already gone to the server, which decides for real
 * and sends the message; a stale snapshot can only cost the player one more click.
 */
@SideOnly(Side.CLIENT)
public final class ClientUseHook {

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onStart(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer) || !event.getEntityLiving().world.isRemote
                || event.getEntityLiving() != Minecraft.getMinecraft().player) {
            return;
        }
        if (JafsItems.isBitterBerries(event.getItem())) {
            return; // always accepted by the server
        }
        DefinitionSet set = ClientDefinitions.definitions();
        ConsumableKey key = ItemKeys.of(event.getItem(), set);
        FoodDefinition definition = key == null ? null : set.configured(key);
        if (definition == null) {
            return; // not a JAFS food: vanilla decides
        }
        MealRules.Outcome outcome = MealPrediction.predict(ClientFoodState.slotsNow(), definition.id(),
                ClientFoodState.activeFoodLimit(), ClientFoodState.eligibilityThresholdPercent());
        if (!outcome.accepted()) {
            event.setCanceled(true);
            Diag.event("client predicted refusal {} for {}", outcome.kind(), definition.id());
        }
    }
}
