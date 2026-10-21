package com.mahghuuuls.jafs.gameplay;

import com.mahghuuuls.jafs.core.FoodDefinition;
import com.mahghuuuls.jafs.core.Matcher;
import net.minecraft.block.Block;
import net.minecraft.init.Items;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

/**
 * How a definition is named to the player when no item stack is at hand (the expiry notice,
 * REQ-068a): the first matcher's item, resolved like the HUD icon (a block matcher through the
 * item registered under the block's id, else the block's item), gives the display name and
 * whether the food is drunk. Falls back to the definition id when nothing resolves.
 */
final class FoodNames {

    private FoodNames() {
    }

    /** The player-facing name of a resolved stack, or the definition id when nothing resolved. */
    static ITextComponent displayName(FoodDefinition definition, ItemStack stack) {
        return new TextComponentString(stack.isEmpty() ? definition.id() : stack.getDisplayName());
    }

    /** Whether the resolved stack is drunk (milk, potions), for the verb. */
    static boolean isDrink(ItemStack stack) {
        return !stack.isEmpty() && stack.getItemUseAction() == EnumAction.DRINK;
    }

    static ItemStack firstStack(FoodDefinition definition) {
        for (Matcher matcher : definition.matchers()) {
            ResourceLocation id = new ResourceLocation(matcher.id());
            Item item = Item.REGISTRY.getObject(id);
            if ((item == null || item == Items.AIR) && matcher.kind() == Matcher.Kind.BLOCK) {
                Block block = Block.REGISTRY.getObject(id);
                item = block == null ? null : Item.getItemFromBlock(block);
            }
            if (item != null && item != Items.AIR) {
                int meta = matcher.kind() == Matcher.Kind.ITEM && matcher.meta() != Matcher.ANY_META ? matcher.meta() : 0;
                return new ItemStack(item, 1, meta);
            }
        }
        return ItemStack.EMPTY;
    }
}
