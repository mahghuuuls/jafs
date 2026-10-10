package com.mahghuuuls.jafs.network;

import com.mahghuuuls.jafs.core.ConsumableKey;
import com.mahghuuuls.jafs.core.DefinitionSet;
import com.mahghuuuls.jafs.core.Matcher;
import com.mahghuuuls.jafs.core.ValueText;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTPrimitive;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagFloat;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Turns an item stack into the consumable key matchers look at. Shared by the server (meal
 * decisions) and the client (tooltips), so both sides read the same fields the same way: only
 * the NBT fields some matcher for that item asked for, as plain strings.
 */
public final class ItemKeys {

    private ItemKeys() {
    }

    /** The registry id of the stack's item, or null for an empty stack or an unregistered item. */
    public static String itemId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        ResourceLocation name = stack.getItem().getRegistryName();
        return name == null ? null : name.toString();
    }

    /** The key for a stack, reading the NBT fields the set's matchers for this item need. */
    public static ConsumableKey of(ItemStack stack, DefinitionSet set) {
        String id = itemId(stack);
        if (id == null) {
            return null;
        }
        Set<String> fields = set.nbtFieldsFor(id);
        Map<String, String> nbt = new LinkedHashMap<String, String>();
        if (!fields.isEmpty() && stack.hasTagCompound()) {
            NBTTagCompound tag = stack.getTagCompound();
            for (String field : fields) {
                NBTBase value = tag.getTag(field);
                if (value != null) {
                    nbt.put(field, text(value));
                }
            }
        }
        return new ConsumableKey(Matcher.Kind.ITEM, id, stack.getMetadata(), nbt);
    }

    /** The comparable text of a tag: strings as they are, numbers through {@link ValueText}, others as NBT text. */
    static String text(NBTBase value) {
        if (value instanceof NBTTagString) {
            return ((NBTTagString) value).getString();
        }
        if (value instanceof NBTTagFloat) {
            // Through the float's own shortest text, so 0.1f compares as "0.1", not as its double expansion.
            return ValueText.ofNumber(Double.parseDouble(Float.toString(((NBTTagFloat) value).getFloat())));
        }
        if (value instanceof NBTTagDouble) {
            return ValueText.ofNumber(((NBTPrimitive) value).getDouble());
        }
        if (value instanceof NBTPrimitive) {
            return ValueText.ofLong(((NBTPrimitive) value).getLong());
        }
        return value.toString();
    }
}
