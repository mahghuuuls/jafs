package com.mahghuuuls.jafs.client;

import com.mahghuuuls.jafs.Tags;
import com.mahghuuuls.jafs.diagnostic.Diag;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Owner of {@code config/jafs/jafs-client.cfg} (ARC-006, REQ-063, REQ-066, REQ-067, REQ-075):
 * the Forge annotation configuration, so the file shows in the mod's config GUI and a change
 * there applies at once through {@link ConfigChangedEvent}. Readers never touch these fields;
 * they use the parsed {@link ClientPreferences}. Nothing here reaches the server; Forge reads
 * annotation configs on both sides, so a dedicated server also writes this file and ignores it.
 * Forge's range annotations guide the GUI only; the file's values are checked by
 * {@link ClientPreferenceRules}.
 */
@Config(modid = Tags.MOD_ID, name = "jafs/jafs-client", category = "client")
@Mod.EventBusSubscriber(modid = Tags.MOD_ID, value = Side.CLIENT)
public final class ClientConfig {

    @Config.Name("hudAnchor")
    @Config.LangKey("jafs.config.hudAnchor")
    @Config.Comment("Where the active-food HUD row sits: above-hotbar-left, above-hotbar-right, top-left, top-right, bottom-left, bottom-right.")
    public static String hudAnchor = ClientDefaults.HUD_ANCHOR;

    @Config.Name("hudOffsetX")
    @Config.LangKey("jafs.config.hudOffsetX")
    @Config.Comment("Pixels to move the HUD row to the right (negative: left) from its anchor.")
    @Config.RangeInt(min = ClientDefaults.HUD_OFFSET_MIN, max = ClientDefaults.HUD_OFFSET_MAX)
    public static int hudOffsetX = ClientDefaults.HUD_OFFSET_X;

    @Config.Name("hudOffsetY")
    @Config.LangKey("jafs.config.hudOffsetY")
    @Config.Comment("Pixels to move the HUD row down (negative: up) from its anchor.")
    @Config.RangeInt(min = ClientDefaults.HUD_OFFSET_MIN, max = ClientDefaults.HUD_OFFSET_MAX)
    public static int hudOffsetY = ClientDefaults.HUD_OFFSET_Y;

    @Config.Name("hudScale")
    @Config.LangKey("jafs.config.hudScale")
    @Config.Comment("Size multiplier of the HUD row.")
    @Config.RangeDouble(min = ClientDefaults.HUD_SCALE_MIN, max = ClientDefaults.HUD_SCALE_MAX)
    public static double hudScale = ClientDefaults.HUD_SCALE;

    @Config.Name("showUnconfiguredTooltip")
    @Config.LangKey("jafs.config.showUnconfiguredTooltip")
    @Config.Comment("Show the 'No food bonuses configured' tooltip line on edible items without a definition.")
    public static boolean showUnconfiguredTooltip = ClientDefaults.SHOW_UNCONFIGURED_TOOLTIP;

    @Config.Name("propertyColorOverrides")
    @Config.LangKey("jafs.config.propertyColorOverrides")
    @Config.Comment("One entry per line as <property id>=<color name>, for example jafs:max_health=blue. Color names are the vanilla text colors (black, dark_blue, dark_green, dark_aqua, dark_red, dark_purple, gold, gray, dark_gray, blue, green, aqua, red, light_purple, yellow, white).")
    public static String[] propertyColorOverrides = ClientDefaults.PROPERTY_COLOR_OVERRIDES;

    private ClientConfig() {
    }

    /** The GUI saved: write the file, re-read the fields, and refresh the parsed preferences. */
    @SubscribeEvent
    public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (!Tags.MOD_ID.equals(event.getModID())) {
            return;
        }
        ConfigManager.sync(Tags.MOD_ID, Config.Type.INSTANCE);
        ClientPreferences.reload();
        Diag.LOGGER.info("client preferences changed: {}", ClientPreferences.describe());
    }
}
