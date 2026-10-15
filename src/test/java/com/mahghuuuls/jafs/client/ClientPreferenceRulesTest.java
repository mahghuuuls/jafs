package com.mahghuuuls.jafs.client;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Raw client values become preferences; every fallback is reported and never silent. */
class ClientPreferenceRulesTest {

    private static final Set<String> COLORS = new HashSet<String>(Arrays.asList("red", "blue", "green", "gold", "aqua"));
    private static final ClientPreferenceRules.ColorNames NAMES = new ClientPreferenceRules.ColorNames() {
        @Override
        public boolean isKnown(String name) {
            return COLORS.contains(name);
        }
    };

    @Test
    void defaultsParseWithoutProblems() {
        ClientPreferenceRules.Parsed p = ClientPreferenceRules.parse(ClientDefaults.HUD_ANCHOR, ClientDefaults.HUD_OFFSET_X,
                ClientDefaults.HUD_OFFSET_Y, ClientDefaults.HUD_SCALE, ClientDefaults.SHOW_UNCONFIGURED_TOOLTIP,
                ClientDefaults.PROPERTY_COLOR_OVERRIDES, NAMES);
        assertTrue(p.problems.isEmpty(), p.problems.toString());
        assertEquals(HudLayout.Anchor.ABOVE_HOTBAR_LEFT, p.anchor);
        assertEquals(1.0f, p.scale);
        assertTrue(p.showUnconfiguredTooltip);
        assertTrue(p.colorOverrides.isEmpty());
    }

    @Test
    void configuredValuesAreKept() {
        ClientPreferenceRules.Parsed p = ClientPreferenceRules.parse("top-right", 12, -7, 2.0, false,
                new String[] {"jafs:max_health=blue", "jafsfixture:stamina = Gold "}, NAMES);
        assertTrue(p.problems.isEmpty(), p.problems.toString());
        assertEquals(HudLayout.Anchor.TOP_RIGHT, p.anchor);
        assertEquals(12, p.offsetX);
        assertEquals(-7, p.offsetY);
        assertEquals(2.0f, p.scale);
        assertFalse(p.showUnconfiguredTooltip);
        assertEquals("blue", p.colorOverrides.get("jafs:max_health"));
        assertEquals("gold", p.colorOverrides.get("jafsfixture:stamina"));
    }

    @Test
    void unknownAnchorFallsBackToTheDefaultAndSaysSo() {
        ClientPreferenceRules.Parsed p = ClientPreferenceRules.parse("middle", 0, 0, 1.0, true, null, NAMES);
        assertEquals(HudLayout.Anchor.ABOVE_HOTBAR_LEFT, p.anchor);
        assertEquals(1, p.problems.size());
        assertTrue(p.problems.get(0).contains("hudAnchor 'middle'"));
        assertTrue(p.problems.get(0).contains("above-hotbar-left"));
    }

    @Test
    void outOfRangeNumbersFallBackToTheirDefaults() {
        ClientPreferenceRules.Parsed p = ClientPreferenceRules.parse("top-left", 5000, -5000, 9.0, true, null, NAMES);
        assertEquals(ClientDefaults.HUD_OFFSET_X, p.offsetX);
        assertEquals(ClientDefaults.HUD_OFFSET_Y, p.offsetY);
        assertEquals((float) ClientDefaults.HUD_SCALE, p.scale);
        assertEquals(3, p.problems.size());
        assertTrue(p.problems.get(0).startsWith("hudOffsetX 5000"));
        assertTrue(p.problems.get(1).startsWith("hudOffsetY -5000"));
        assertTrue(p.problems.get(2).startsWith("hudScale 9.0"));
    }

    @Test
    void badOverrideLinesAreIgnoredOneByOne() {
        ClientPreferenceRules.Parsed p = ClientPreferenceRules.parse("top-left", 0, 0, 1.0, true,
                new String[] {"jafs:max_health=blue", "no-equals", "=blue", "jafs:recovery_rate=", "jafs:depletion_rate=purple", "", null}, NAMES);
        assertEquals(1, p.colorOverrides.size());
        assertEquals("blue", p.colorOverrides.get("jafs:max_health"));
        assertEquals(4, p.problems.size());
        assertTrue(p.problems.get(0).contains("'no-equals'"));
        assertTrue(p.problems.get(1).contains("'=blue'"));
        assertTrue(p.problems.get(2).contains("'jafs:recovery_rate='"));
        assertTrue(p.problems.get(3).contains("names no known color"));
    }
}
