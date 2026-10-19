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
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The client's half of a refused meal (REQ-081a, REV-013). Two client events are cancelled when
 * the server's own rule, run on the counted-down snapshot, would refuse the meal: the right-click
 * on the item (with a pass result, so the item never reports a success and vanilla does not play
 * its re-equip motion, D-D5) and, as a second line, the use start (so the hand is never set
 * active and no eating gesture begins). The use packet has already gone to the server before
 * either event, so the server decides for real and sends the message; a stale snapshot only
 * delays the gesture by one round trip.
 */
@SideOnly(Side.CLIENT)
public final class ClientUseHook {

    /** The click itself: cancelled with a pass so the item reports no success and nothing re-equips (D-D5). */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!event.getWorld().isRemote || event.getEntityPlayer() != Minecraft.getMinecraft().player) {
            return;
        }
        Refusal refusal = predictedRefusal(event.getItemStack());
        if (refusal != null) {
            event.setCanceled(true);
            event.setCancellationResult(EnumActionResult.PASS);
            Diag.event("client predicted refusal {} for {}", refusal.kind, refusal.definitionId);
        }
    }

    /** The use start, in case something reaches it anyway: cancelled so the hand is never set active. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onStart(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer) || !event.getEntityLiving().world.isRemote
                || event.getEntityLiving() != Minecraft.getMinecraft().player) {
            return;
        }
        if (predictedRefusal(event.getItem()) != null) {
            event.setCanceled(true);
        }
    }

    /** A refusal the rule predicts for a configured food. */
    private static final class Refusal {
        final String definitionId;
        final MealRules.Kind kind;

        Refusal(String definitionId, MealRules.Kind kind) {
            this.definitionId = definitionId;
            this.kind = kind;
        }
    }

    /** The predicted refusal for the stack, or null when the stack is not a configured JAFS food or would be accepted. */
    private static Refusal predictedRefusal(ItemStack stack) {
        if (JafsItems.isBitterBerries(stack)) {
            return null; // always accepted by the server
        }
        DefinitionSet set = ClientDefinitions.definitions();
        ConsumableKey key = ItemKeys.of(stack, set);
        FoodDefinition definition = key == null ? null : set.configured(key);
        if (definition == null) {
            return null; // not a JAFS food: vanilla decides
        }
        MealRules.Outcome outcome = MealPrediction.predict(ClientFoodState.slotsNow(), definition.id(),
                ClientFoodState.activeFoodLimit(), ClientFoodState.eligibilityThresholdPercent());
        return outcome.accepted() ? null : new Refusal(definition.id(), outcome.kind());
    }
}
