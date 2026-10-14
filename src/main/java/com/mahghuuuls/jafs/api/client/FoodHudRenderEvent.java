package com.mahghuuuls.jafs.api.client;

import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

/**
 * Fired on the Forge event bus right before JAFS draws its active-food HUD. Cancel it to draw
 * your own HUD instead; {@link ClientFoodApi#getSnapshot()} gives the foods to draw.
 */
@Cancelable
public final class FoodHudRenderEvent extends Event {

    private final int scaledWidth;
    private final int scaledHeight;

    public FoodHudRenderEvent(int scaledWidth, int scaledHeight) {
        this.scaledWidth = scaledWidth;
        this.scaledHeight = scaledHeight;
    }

    public int scaledWidth() {
        return scaledWidth;
    }

    public int scaledHeight() {
        return scaledHeight;
    }
}
