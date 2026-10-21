package com.mahghuuuls.jafs.client;

import com.mahghuuuls.jafs.api.client.FoodHudRenderEvent;
import com.mahghuuuls.jafs.core.DisplayFormat;
import com.mahghuuuls.jafs.core.FoodDefinition;
import com.mahghuuuls.jafs.core.Matcher;
import com.mahghuuuls.jafs.network.FoodStateSnapshotMessage;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.GuiIngameForge;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

/**
 * Draws the active foods: one cell per slot, each an item icon with the time left under it in
 * the color of {@link TimeColor} (white, yellow once the food can be eaten again, red in the last
 * minute; REQ-064, REV-015). The cells run in a row or a column from the configured anchor
 * (REQ-063, REV-012). Drawn after the whole vanilla overlay so the health rows (whose height
 * depends on maximum health) are known.
 */
@SideOnly(Side.CLIENT)
public final class HudRenderer {

    private static final int ICON = 16;
    /**
     * Shown instead of a time while the timers stand still (rate 0: spectator, depletion rate 0).
     * Two capital i letters: the font draws the bar character with a gap, which reads as four bars.
     */
    static final String PAUSED_MARK = "II";
    /** The widest compact time under ten hours; cells are at least this wide so the block keeps its size. */
    static final String WIDEST_USUAL_TIME = "0h00";

    @SubscribeEvent
    public void onPostRender(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }
        ScaledResolution resolution = event.getResolution();
        int width = resolution.getScaledWidth();
        int height = resolution.getScaledHeight();
        // Addons may cancel to draw their own HUD; posted on every frame, foods or not.
        if (MinecraftForge.EVENT_BUS.post(new FoodHudRenderEvent(width, height))) {
            return;
        }
        List<FoodStateSnapshotMessage.Entry> entries = ClientFoodState.entries();
        if (entries.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        ClientPreferenceRules.Parsed prefs = ClientPreferences.current();
        float scale = prefs.scale;
        int count = entries.size();

        // The times first: a cell is as wide as its icon, the widest usual time (so the block does
        // not jump as times shorten), or a wider time when one occurs; the text never runs past the
        // block (REV-012).
        String[] times = new String[count];
        int[] colors = new int[count];
        int cellWidth = Math.max(ICON, mc.fontRenderer.getStringWidth(WIDEST_USUAL_TIME));
        for (int i = 0; i < count; i++) {
            FoodStateSnapshotMessage.Entry entry = entries.get(i);
            int realTicks = ClientFoodState.realTicksLeft(entry);
            times[i] = realTicks < 0 ? PAUSED_MARK : DisplayFormat.compactDurationTicks(realTicks);
            colors[i] = TimeColor.of(entry.eligible, realTicks).argb;
            cellWidth = Math.max(cellWidth, mc.fontRenderer.getStringWidth(times[i]));
        }
        int cellHeight = ICON + 2 + mc.fontRenderer.FONT_HEIGHT;
        int blockWidth = HudLayout.blockWidth(prefs.growth, count, cellWidth);
        int blockHeight = HudLayout.blockHeight(prefs.growth, count, cellHeight);
        int x = ClientPreferences.hudX(prefs, width, blockWidth);
        int y = ClientPreferences.hudY(prefs, height, blockHeight, GuiIngameForge.left_height);

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0);
        GlStateManager.scale(scale, scale, 1);
        RenderHelper.enableGUIStandardItemLighting();
        for (int i = 0; i < count; i++) {
            FoodStateSnapshotMessage.Entry entry = entries.get(i);
            int along = HudLayout.slotOffset(prefs.growth, i, count, prefs.growth.horizontal ? cellWidth : cellHeight);
            int cellX = prefs.growth.horizontal ? along : 0;
            int cellY = prefs.growth.horizontal ? 0 : along;
            // The icon sits at the cell's left; the time under it, left-aligned with the icon, in its color.
            ItemStack icon = iconFor(entry.definitionId);
            if (!icon.isEmpty()) {
                mc.getRenderItem().renderItemAndEffectIntoGUI(icon, cellX, cellY);
            }
            GlStateManager.disableLighting();
            GlStateManager.disableDepth();
            mc.fontRenderer.drawStringWithShadow(times[i], cellX, cellY + ICON + 2, colors[i]);
            GlStateManager.enableDepth();
        }
        RenderHelper.disableStandardItemLighting();
        GlStateManager.popMatrix();
    }

    /**
     * The first matcher's item as the slot's icon: an item matcher's item, or a block matcher's
     * block as its item (a cake draws the cake item); an empty stack when none is known.
     */
    static ItemStack iconFor(String definitionId) {
        FoodDefinition definition = ClientDefinitions.definitions().get(definitionId);
        if (definition == null) {
            return ItemStack.EMPTY;
        }
        for (Matcher matcher : definition.matchers()) {
            Item item;
            if (matcher.kind() == Matcher.Kind.ITEM) {
                item = Item.REGISTRY.getObject(new ResourceLocation(matcher.id()));
            } else {
                // Some blocks (cake, among others) have an item registered under the block's own id
                // that getItemFromBlock does not know, so the item registry is asked first.
                ResourceLocation id = new ResourceLocation(matcher.id());
                item = Item.REGISTRY.getObject(id);
                if (item == null) {
                    Block block = Block.REGISTRY.getObject(id);
                    item = block == null ? null : Item.getItemFromBlock(block);
                }
            }
            if (item != null && item != Items.AIR) {
                int meta = matcher.kind() == Matcher.Kind.ITEM && matcher.meta() != Matcher.ANY_META ? matcher.meta() : 0;
                return new ItemStack(item, 1, meta);
            }
        }
        return ItemStack.EMPTY;
    }
}
