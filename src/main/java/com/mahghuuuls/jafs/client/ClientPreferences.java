package com.mahghuuuls.jafs.client;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The client's display preferences. Until the client configuration file arrives, these are the
 * documented defaults; nothing here changes gameplay.
 */
@SideOnly(Side.CLIENT)
public final class ClientPreferences {

    private ClientPreferences() {
    }

    public static boolean showUnconfiguredTooltip() {
        return true;
    }

    /** The color to use for a property's lines: an override when configured, else the provider default. */
    public static String colorFor(String propertyId, String providerDefault) {
        return providerDefault;
    }

    public static float hudScale() {
        return 1.0f;
    }

    /** Default anchor: above the hotbar, left-aligned with it, above the health rows. */
    public static int hudX(int screenWidth, int rowWidth, float scale) {
        return screenWidth / 2 - 91;
    }

    public static int hudY(int screenHeight, int rowHeight, float scale, int leftHeight) {
        return screenHeight - leftHeight - 2 - Math.round(rowHeight * scale);
    }
}
