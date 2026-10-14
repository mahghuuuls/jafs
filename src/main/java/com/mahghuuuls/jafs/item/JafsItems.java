package com.mahghuuuls.jafs.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** JAFS's own items, registered through the Forge registry event. */
public final class JafsItems {

    public static final ItemBitterBerries BITTER_BERRIES = new ItemBitterBerries();

    @SubscribeEvent
    public void onRegisterItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(BITTER_BERRIES);
    }

    public static boolean isBitterBerries(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() == BITTER_BERRIES;
    }
}
