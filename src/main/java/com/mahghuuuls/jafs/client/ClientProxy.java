package com.mahghuuuls.jafs.client;

import com.mahghuuuls.jafs.CommonProxy;
import com.mahghuuuls.jafs.api.client.ClientFoodApi;
import com.mahghuuuls.jafs.core.DefinitionSet;
import net.minecraft.world.World;
import com.mahghuuuls.jafs.network.JafsNetwork;
import net.minecraftforge.common.MinecraftForge;

/** Client-only registrations. Never loaded on a dedicated server. */
public final class ClientProxy extends CommonProxy {

    @Override
    public DefinitionSet definitionsFor(World world) {
        return world.isRemote ? ClientDefinitions.definitions() : super.definitionsFor(world);
    }

    @Override
    public void preInit() {
        // The annotation config was read before preInit; parse it once, then on every GUI save.
        ClientPreferences.reload();
        MinecraftForge.EVENT_BUS.register(new ClientModels());
        MinecraftForge.EVENT_BUS.register(new HungerBarHider());
        MinecraftForge.EVENT_BUS.register(new TooltipHandler());
        MinecraftForge.EVENT_BUS.register(new HudRenderer());
        MinecraftForge.EVENT_BUS.register(new ClientLifecycle());
        JafsNetwork.setDefinitionsSink(new ClientDefinitions());
        JafsNetwork.setSnapshotSink(new ClientFoodState());
        ClientFoodApi.bind(new ClientFoodState.ApiSource());
    }
}
