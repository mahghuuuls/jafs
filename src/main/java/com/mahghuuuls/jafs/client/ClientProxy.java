package com.mahghuuuls.jafs.client;

import com.mahghuuuls.jafs.CommonProxy;
import com.mahghuuuls.jafs.network.JafsNetwork;
import net.minecraftforge.common.MinecraftForge;

/** Client-only registrations. Never loaded on a dedicated server. */
public final class ClientProxy extends CommonProxy {

    @Override
    public void preInit() {
        MinecraftForge.EVENT_BUS.register(new HungerBarHider());
        MinecraftForge.EVENT_BUS.register(new TooltipHandler());
        MinecraftForge.EVENT_BUS.register(new HudRenderer());
        MinecraftForge.EVENT_BUS.register(new ClientLifecycle());
        JafsNetwork.setDefinitionsSink(new ClientDefinitions());
        JafsNetwork.setSnapshotSink(new ClientFoodState());
    }
}
