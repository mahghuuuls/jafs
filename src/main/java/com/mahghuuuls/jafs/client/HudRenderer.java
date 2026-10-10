package com.mahghuuuls.jafs.client;

import com.mahghuuuls.jafs.core.DisplayFormat;
import com.mahghuuuls.jafs.core.FoodDefinition;
import com.mahghuuuls.jafs.core.Matcher;
import com.mahghuuuls.jafs.network.FoodStateSnapshotMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.GuiIngameForge;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

/**
 * Draws the active foods: one item icon per slot in slot order with the remaining time under it,
 * and a steady border around a food that can be refreshed or replaced. Drawn after the whole
 * vanilla overlay so the health rows (whose height depends on maximum health) are known.
 */
@SideOnly(Side.CLIENT)
public final class HudRenderer {

    private static final int SLOT_WIDTH = 28;
    private static final int ICON = 16;
    private static final int BORDER_COLOR = 0xFFF0E68C;
    private static final int TEXT_COLOR = 0xFFFFFFFF;

    @SubscribeEvent
    public void onPostRender(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }
        List<FoodStateSnapshotMessage.Entry> entries = ClientFoodState.entries();
        if (entries.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution resolution = event.getResolution();
        int width = resolution.getScaledWidth();
        int height = resolution.getScaledHeight();
        float scale = ClientPreferences.hudScale();
        int rowWidth = entries.size() * SLOT_WIDTH;
        int rowHeight = ICON + 2 + mc.fontRenderer.FONT_HEIGHT;
        int x = ClientPreferences.hudX(width, rowWidth, scale);
        int y = ClientPreferences.hudY(height, rowHeight, scale, GuiIngameForge.left_height);

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0);
        GlStateManager.scale(scale, scale, 1);
        RenderHelper.enableGUIStandardItemLighting();
        for (int i = 0; i < entries.size(); i++) {
            FoodStateSnapshotMessage.Entry entry = entries.get(i);
            int slotX = i * SLOT_WIDTH;
            ItemStack icon = iconFor(entry.definitionId);
            if (entry.eligible) {
                Gui.drawRect(slotX - 1, -1, slotX + ICON + 1, ICON + 1, BORDER_COLOR);
                Gui.drawRect(slotX, 0, slotX + ICON, ICON, 0xFF000000);
            }
            if (!icon.isEmpty()) {
                mc.getRenderItem().renderItemAndEffectIntoGUI(icon, slotX, 0);
            }
            String time = DisplayFormat.durationTicks(ClientFoodState.remainingNow(entry));
            GlStateManager.disableLighting();
            GlStateManager.disableDepth();
            mc.fontRenderer.drawStringWithShadow(time, slotX, ICON + 2, TEXT_COLOR);
            GlStateManager.enableDepth();
        }
        RenderHelper.disableStandardItemLighting();
        GlStateManager.popMatrix();
    }

    /** The first item matcher's item, as the slot's icon; an empty stack when none is known. */
    static ItemStack iconFor(String definitionId) {
        FoodDefinition definition = ClientDefinitions.definitions().get(definitionId);
        if (definition == null) {
            return ItemStack.EMPTY;
        }
        for (Matcher matcher : definition.matchers()) {
            if (matcher.kind() != Matcher.Kind.ITEM) {
                continue;
            }
            Item item = Item.REGISTRY.getObject(new ResourceLocation(matcher.id()));
            if (item != null) {
                return new ItemStack(item, 1, matcher.meta() == Matcher.ANY_META ? 0 : matcher.meta());
            }
        }
        return ItemStack.EMPTY;
    }
}
