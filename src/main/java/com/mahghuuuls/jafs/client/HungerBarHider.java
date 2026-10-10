package com.mahghuuuls.jafs.client;

import net.minecraftforge.client.GuiIngameForge;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Hides the vanilla hunger bar. Forge skips {@code right_height += 10} when the FOOD element is
 * cancelled, which would move the air bubbles down, so the same advance is applied here.
 */
@SideOnly(Side.CLIENT)
public final class HungerBarHider {

    private static final int FOOD_ROW_HEIGHT = 10;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPreRender(RenderGameOverlayEvent.Pre event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.FOOD) {
            return;
        }
        event.setCanceled(true);
        GuiIngameForge.right_height += FOOD_ROW_HEIGHT;
    }
}
