package com.mahghuuuls.jafsfixture;

import com.mahghuuuls.jafs.api.client.FoodHudRenderEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Proves the HUD event contract (REQ-063 render event): while the fixture's switch is on, the
 * event is cancelled and JAFS draws nothing. An addon drawing its own HUD would read
 * {@code ClientFoodApi.getSnapshot()} here. Client side only; registered from the mod class
 * only on a client.
 */
public final class FixtureHudCanceller {

    @SubscribeEvent
    public void onHud(FoodHudRenderEvent event) {
        if (JafsFixtureMod.cancelHud) {
            event.setCanceled(true);
        }
    }
}
