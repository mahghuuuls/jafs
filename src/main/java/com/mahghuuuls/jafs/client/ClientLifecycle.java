package com.mahghuuuls.jafs.client;

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Forgets server-sent data when the client leaves a server or world. This event arrives on the
 * network thread; the fields it clears are volatile and are always resent at the next login,
 * so no scheduling is needed.
 */
@SideOnly(Side.CLIENT)
public final class ClientLifecycle {

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        ClientDefinitions.clear();
        ClientFoodState.clear();
    }
}
