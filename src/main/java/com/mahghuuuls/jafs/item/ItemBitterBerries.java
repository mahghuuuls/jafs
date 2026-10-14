package com.mahghuuuls.jafs.item;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemFood;

/**
 * Bitter Berries (REQ-052): an edible that clears every active food. It gives no hunger and no
 * saturation and applies no effect; the hunger pin makes it always edible in survival, and
 * vanilla consumes it in {@code onItemUseFinish} as any food. The clearing itself happens in the
 * meal gate's clear path, so this class carries no gameplay rule.
 */
public final class ItemBitterBerries extends ItemFood {

    public static final String NAME = "bitter_berries";

    public ItemBitterBerries() {
        super(0, 0.0F, false);
        setAlwaysEdible();
        setRegistryName(NAME);
        setTranslationKey("jafs." + NAME);
        setCreativeTab(CreativeTabs.FOOD);
    }
}
