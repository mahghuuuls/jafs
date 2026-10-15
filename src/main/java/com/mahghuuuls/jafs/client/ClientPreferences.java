package com.mahghuuuls.jafs.client;

import com.mahghuuuls.jafs.diagnostic.Diag;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The client's display preferences as the renderer and the tooltip handler read them: the
 * parsed form of {@link ClientConfig}, refreshed at start and whenever the config GUI saves.
 * Nothing here changes gameplay (REQ-075).
 */
@SideOnly(Side.CLIENT)
public final class ClientPreferences {

    private static final ClientPreferenceRules.ColorNames VANILLA_COLORS = new ClientPreferenceRules.ColorNames() {
        @Override
        public boolean isKnown(String name) {
            TextFormatting formatting = TextFormatting.getValueByName(name);
            return formatting != null && formatting.isColor();
        }
    };

    private static volatile ClientPreferenceRules.Parsed current = ClientPreferenceRules.parse(
            ClientDefaults.HUD_ANCHOR, ClientDefaults.HUD_OFFSET_X, ClientDefaults.HUD_OFFSET_Y, ClientDefaults.HUD_SCALE,
            ClientDefaults.SHOW_UNCONFIGURED_TOOLTIP, ClientDefaults.PROPERTY_COLOR_OVERRIDES, VANILLA_COLORS);

    private ClientPreferences() {
    }

    /** Re-read {@link ClientConfig}; every fallback is logged once per reload (REQ-077). */
    public static void reload() {
        ClientPreferenceRules.Parsed parsed = ClientPreferenceRules.parse(ClientConfig.hudAnchor, ClientConfig.hudOffsetX,
                ClientConfig.hudOffsetY, ClientConfig.hudScale, ClientConfig.showUnconfiguredTooltip,
                ClientConfig.propertyColorOverrides, VANILLA_COLORS);
        for (String problem : parsed.problems) {
            Diag.LOGGER.warn("jafs-client.cfg: {}", problem);
        }
        current = parsed;
    }

    /** One line for the log: the values in effect. */
    public static String describe() {
        ClientPreferenceRules.Parsed p = current;
        return "anchor=" + p.anchor.name + " offset=" + p.offsetX + "," + p.offsetY + " scale=" + p.scale
                + " unconfiguredTooltip=" + p.showUnconfiguredTooltip + " colorOverrides=" + p.colorOverrides;
    }

    public static boolean showUnconfiguredTooltip() {
        return current.showUnconfiguredTooltip;
    }

    /** The color to use for a property's lines: an override when configured, else the provider default (REQ-066). */
    public static String colorFor(String propertyId, String providerDefault) {
        String override = current.colorOverrides.get(propertyId);
        return override == null ? providerDefault : override;
    }

    /** The values in effect, as one immutable snapshot so a frame reads anchor, offsets, and scale together. */
    public static ClientPreferenceRules.Parsed current() {
        return current;
    }

    /** The row's left edge for the snapshot's anchor, offset, and scale; {@code rowWidth} is unscaled. */
    public static int hudX(ClientPreferenceRules.Parsed p, int screenWidth, int rowWidth) {
        return HudLayout.x(p.anchor, screenWidth, Math.round(rowWidth * p.scale), p.offsetX);
    }

    /** The row's top edge for the snapshot's anchor, offset, and scale; {@code rowHeight} is unscaled. */
    public static int hudY(ClientPreferenceRules.Parsed p, int screenHeight, int rowHeight, int leftHeight) {
        return HudLayout.y(p.anchor, screenHeight, Math.round(rowHeight * p.scale), leftHeight, p.offsetY);
    }
}
