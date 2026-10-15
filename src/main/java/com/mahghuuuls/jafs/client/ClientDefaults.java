package com.mahghuuuls.jafs.client;

/**
 * The client preference defaults and ranges, copied from {@code config-defaults.json} (REQ-078)
 * and checked against it by the parity test. Plain Java so the test needs no game classes.
 */
public final class ClientDefaults {

    public static final String HUD_ANCHOR = "bottom-left";
    public static final String HUD_GROWTH = "left-to-right";
    public static final int HUD_OFFSET_X = 0;
    public static final int HUD_OFFSET_Y = 0;
    public static final int HUD_OFFSET_MIN = -1000;
    public static final int HUD_OFFSET_MAX = 1000;
    public static final double HUD_SCALE = 1.0;
    public static final double HUD_SCALE_MIN = 0.5;
    public static final double HUD_SCALE_MAX = 3.0;
    public static final boolean SHOW_UNCONFIGURED_TOOLTIP = true;
    /** No overrides: every property uses its provider's color. */
    public static final String[] PROPERTY_COLOR_OVERRIDES = new String[0];

    private ClientDefaults() {
    }
}
