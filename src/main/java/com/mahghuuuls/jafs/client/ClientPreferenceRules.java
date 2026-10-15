package com.mahghuuuls.jafs.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * How raw client configuration values become usable preferences (REQ-063, REQ-066): an unknown
 * anchor or an out-of-range number falls back to its default (the same rule as the server file,
 * REQ-077; Forge's range annotations only guide the GUI and never check the file), and the color
 * override list is parsed. Every fallback is reported so the file's author can see it (REQ-077). Plain
 * Java, unit-tested; the game-facing cache is {@link ClientPreferences}.
 */
public final class ClientPreferenceRules {

    /** One parse of every raw value. */
    public static final class Parsed {
        public final HudLayout.Anchor anchor;
        public final int offsetX;
        public final int offsetY;
        public final float scale;
        public final boolean showUnconfiguredTooltip;
        /** Property id to color name, in file order. */
        public final Map<String, String> colorOverrides;
        /** Human-readable fallbacks, empty when every value was usable. */
        public final List<String> problems;

        Parsed(HudLayout.Anchor anchor, int offsetX, int offsetY, float scale, boolean showUnconfiguredTooltip,
               Map<String, String> colorOverrides, List<String> problems) {
            this.anchor = anchor;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.scale = scale;
            this.showUnconfiguredTooltip = showUnconfiguredTooltip;
            this.colorOverrides = Collections.unmodifiableMap(colorOverrides);
            this.problems = Collections.unmodifiableList(problems);
        }
    }

    /** What counts as a color name; the game's own list, so this class stays free of game classes. */
    public interface ColorNames {
        boolean isKnown(String name);
    }

    private ClientPreferenceRules() {
    }

    public static Parsed parse(String anchorText, int offsetX, int offsetY, double scale, boolean showUnconfiguredTooltip,
                               String[] colorOverrideLines, ColorNames colors) {
        List<String> problems = new ArrayList<String>();
        HudLayout.Anchor anchor = HudLayout.Anchor.parse(anchorText);
        if (anchor == null) {
            anchor = HudLayout.Anchor.parse(ClientDefaults.HUD_ANCHOR);
            problems.add("hudAnchor '" + anchorText + "' is not one of " + String.join(", ", HudLayout.Anchor.names())
                    + "; using " + ClientDefaults.HUD_ANCHOR);
        }
        int x = inRangeOrDefault("hudOffsetX", offsetX, ClientDefaults.HUD_OFFSET_X, problems);
        int y = inRangeOrDefault("hudOffsetY", offsetY, ClientDefaults.HUD_OFFSET_Y, problems);
        double s = scale;
        if (Double.isNaN(s) || s < ClientDefaults.HUD_SCALE_MIN || s > ClientDefaults.HUD_SCALE_MAX) {
            problems.add("hudScale " + scale + " is outside " + ClientDefaults.HUD_SCALE_MIN + ".." + ClientDefaults.HUD_SCALE_MAX
                    + "; using " + ClientDefaults.HUD_SCALE);
            s = ClientDefaults.HUD_SCALE;
        }
        Map<String, String> overrides = new LinkedHashMap<String, String>();
        if (colorOverrideLines != null) {
            for (String line : colorOverrideLines) {
                parseOverride(line, colors, overrides, problems);
            }
        }
        return new Parsed(anchor, x, y, (float) s, showUnconfiguredTooltip, overrides, problems);
    }

    private static int inRangeOrDefault(String name, int value, int fallback, List<String> problems) {
        if (value < ClientDefaults.HUD_OFFSET_MIN || value > ClientDefaults.HUD_OFFSET_MAX) {
            problems.add(name + " " + value + " is outside " + ClientDefaults.HUD_OFFSET_MIN + ".." + ClientDefaults.HUD_OFFSET_MAX
                    + "; using " + fallback);
            return fallback;
        }
        return value;
    }

    /** One override line: {@code <property id>=<color name>}; blank lines are skipped. */
    private static void parseOverride(String line, ColorNames colors, Map<String, String> into, List<String> problems) {
        if (line == null || line.trim().isEmpty()) {
            return;
        }
        int eq = line.indexOf('=');
        if (eq <= 0 || eq == line.length() - 1) {
            problems.add("propertyColorOverrides entry '" + line + "' is not <property id>=<color>; ignored");
            return;
        }
        String id = line.substring(0, eq).trim();
        String color = line.substring(eq + 1).trim().toLowerCase(Locale.ROOT);
        if (id.isEmpty() || color.isEmpty()) {
            problems.add("propertyColorOverrides entry '" + line + "' is not <property id>=<color>; ignored");
            return;
        }
        if (!colors.isKnown(color)) {
            problems.add("propertyColorOverrides entry '" + line + "' names no known color; ignored");
            return;
        }
        into.put(id, color);
    }
}
