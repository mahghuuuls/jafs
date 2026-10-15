package com.mahghuuuls.jafs.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** The six anchors on a 427x240 screen (the usual GUI scale 2 at 854x480) with a 56x27 row. */
class HudLayoutTest {

    private static final int W = 427;
    private static final int H = 240;
    private static final int ROW_W = 56;
    private static final int ROW_H = 27;
    private static final int LEFT_HEIGHT = 59; // hotbar 22 + health row 10 + gaps, as the vanilla overlay reports

    @Test
    void aboveHotbarLeftAlignsWithTheHotbarAndSitsAboveTheHealthRows() {
        assertEquals(213 - 91, HudLayout.x(HudLayout.Anchor.ABOVE_HOTBAR_LEFT, W, ROW_W, 0));
        assertEquals(H - LEFT_HEIGHT - 2 - ROW_H, HudLayout.y(HudLayout.Anchor.ABOVE_HOTBAR_LEFT, H, ROW_H, LEFT_HEIGHT, 0));
    }

    @Test
    void aboveHotbarRightEndsAtTheHotbarsRightEdge() {
        assertEquals(213 + 91 - ROW_W, HudLayout.x(HudLayout.Anchor.ABOVE_HOTBAR_RIGHT, W, ROW_W, 0));
        assertEquals(H - LEFT_HEIGHT - 2 - ROW_H, HudLayout.y(HudLayout.Anchor.ABOVE_HOTBAR_RIGHT, H, ROW_H, LEFT_HEIGHT, 0));
    }

    @Test
    void cornersKeepTheMargin() {
        assertEquals(2, HudLayout.x(HudLayout.Anchor.TOP_LEFT, W, ROW_W, 0));
        assertEquals(2, HudLayout.y(HudLayout.Anchor.TOP_LEFT, H, ROW_H, LEFT_HEIGHT, 0));
        assertEquals(W - 2 - ROW_W, HudLayout.x(HudLayout.Anchor.TOP_RIGHT, W, ROW_W, 0));
        assertEquals(2, HudLayout.y(HudLayout.Anchor.TOP_RIGHT, H, ROW_H, LEFT_HEIGHT, 0));
        assertEquals(2, HudLayout.x(HudLayout.Anchor.BOTTOM_LEFT, W, ROW_W, 0));
        assertEquals(H - 2 - ROW_H, HudLayout.y(HudLayout.Anchor.BOTTOM_LEFT, H, ROW_H, LEFT_HEIGHT, 0));
        assertEquals(W - 2 - ROW_W, HudLayout.x(HudLayout.Anchor.BOTTOM_RIGHT, W, ROW_W, 0));
        assertEquals(H - 2 - ROW_H, HudLayout.y(HudLayout.Anchor.BOTTOM_RIGHT, H, ROW_H, LEFT_HEIGHT, 0));
    }

    @Test
    void aRowWiderThanTheScreenKeepsItsLeftEdgeAndOffsetsCannotPushItOff() {
        int wide = W + 100;
        assertEquals(2, HudLayout.x(HudLayout.Anchor.TOP_RIGHT, W, wide, 0));
        assertEquals(2, HudLayout.x(HudLayout.Anchor.TOP_LEFT, W, ROW_W, -999));
        assertEquals(W - 2 - ROW_W, HudLayout.x(HudLayout.Anchor.TOP_LEFT, W, ROW_W, 999));
        assertEquals(2, HudLayout.y(HudLayout.Anchor.BOTTOM_LEFT, H, ROW_H, LEFT_HEIGHT, -999));
        assertEquals(H - 2 - ROW_H, HudLayout.y(HudLayout.Anchor.TOP_LEFT, H, ROW_H, LEFT_HEIGHT, 999));
    }

    @Test
    void offsetsMoveRightAndDownForEveryAnchor() {
        // Toward the screen center, so the move is not stopped by the on-screen clamp.
        for (HudLayout.Anchor anchor : HudLayout.Anchor.values()) {
            boolean left = anchor == HudLayout.Anchor.ABOVE_HOTBAR_LEFT || anchor == HudLayout.Anchor.TOP_LEFT
                    || anchor == HudLayout.Anchor.BOTTOM_LEFT;
            boolean top = anchor == HudLayout.Anchor.TOP_LEFT || anchor == HudLayout.Anchor.TOP_RIGHT;
            int dx = left ? 10 : -10;
            int dy = top ? 5 : -5;
            assertEquals(HudLayout.x(anchor, W, ROW_W, 0) + dx, HudLayout.x(anchor, W, ROW_W, dx), anchor.name);
            assertEquals(HudLayout.y(anchor, H, ROW_H, LEFT_HEIGHT, 0) + dy, HudLayout.y(anchor, H, ROW_H, LEFT_HEIGHT, dy), anchor.name);
        }
    }

    @Test
    void aScaledRowStaysInsideTheRightAndBottomEdges() {
        int scaledW = ROW_W * 2;
        int scaledH = ROW_H * 2;
        assertEquals(W - 2, HudLayout.x(HudLayout.Anchor.BOTTOM_RIGHT, W, scaledW, 0) + scaledW);
        assertEquals(H - 2, HudLayout.y(HudLayout.Anchor.BOTTOM_RIGHT, H, scaledH, LEFT_HEIGHT, 0) + scaledH);
    }

    @Test
    void anchorNamesParseIgnoringCaseAndSpaces() {
        assertEquals(HudLayout.Anchor.TOP_RIGHT, HudLayout.Anchor.parse(" Top-Right "));
        assertNull(HudLayout.Anchor.parse("middle"));
        assertNull(HudLayout.Anchor.parse(null));
        assertArrayEquals(new String[] {"above-hotbar-left", "above-hotbar-right", "top-left", "top-right", "bottom-left", "bottom-right"},
                HudLayout.Anchor.names());
        assertEquals(ClientDefaults.HUD_ANCHOR, HudLayout.Anchor.ABOVE_HOTBAR_LEFT.name);
    }
}
