package com.mahghuuuls.jafs.client;

import com.mahghuuuls.jafs.item.JafsItems;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Item models for JAFS's own items (client only). */
public final class ClientModels {

    @SubscribeEvent
    public void onModelRegistry(ModelRegistryEvent event) {
        ModelLoader.setCustomModelResourceLocation(JafsItems.BITTER_BERRIES, 0,
                new ModelResourceLocation(JafsItems.BITTER_BERRIES.getRegistryName(), "inventory"));
    }
}
