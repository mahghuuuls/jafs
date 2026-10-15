package com.mahghuuuls.jafs.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** The eight anchors and the four growths on a 427x240 screen (the usual GUI scale 2 at 854x480) with a 56x27 row. */
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
                    || anchor == HudLayout.Anchor.MIDDLE_LEFT || anchor == HudLayout.Anchor.BOTTOM_LEFT;
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
    void middleAnchorsCenterTheBlockVertically() {
        assertEquals(2, HudLayout.x(HudLayout.Anchor.MIDDLE_LEFT, W, ROW_W, 0));
        assertEquals((H - ROW_H) / 2, HudLayout.y(HudLayout.Anchor.MIDDLE_LEFT, H, ROW_H, LEFT_HEIGHT, 0));
        assertEquals(W - 2 - ROW_W, HudLayout.x(HudLayout.Anchor.MIDDLE_RIGHT, W, ROW_W, 0));
        assertEquals((H - ROW_H) / 2, HudLayout.y(HudLayout.Anchor.MIDDLE_RIGHT, H, ROW_H, LEFT_HEIGHT, 0));
    }

    @Test
    void blockSizesAndSlotOffsetsFollowTheGrowth() {
        // three cells of 20x27: a row is 20+4+20+4+20 wide and one cell tall; a column the other way round
        assertEquals(68, HudLayout.blockWidth(HudLayout.Growth.LEFT_TO_RIGHT, 3, 20));
        assertEquals(27, HudLayout.blockHeight(HudLayout.Growth.RIGHT_TO_LEFT, 3, 27));
        assertEquals(20, HudLayout.blockWidth(HudLayout.Growth.TOP_TO_BOTTOM, 3, 20));
        assertEquals(89, HudLayout.blockHeight(HudLayout.Growth.BOTTOM_TO_TOP, 3, 27));
        assertEquals(20, HudLayout.blockWidth(HudLayout.Growth.LEFT_TO_RIGHT, 1, 20));
        // slot 0 starts at the named end; the other end for the reversed growths
        assertEquals(0, HudLayout.slotOffset(HudLayout.Growth.LEFT_TO_RIGHT, 0, 3, 20));
        assertEquals(48, HudLayout.slotOffset(HudLayout.Growth.LEFT_TO_RIGHT, 2, 3, 20));
        assertEquals(48, HudLayout.slotOffset(HudLayout.Growth.RIGHT_TO_LEFT, 0, 3, 20));
        assertEquals(0, HudLayout.slotOffset(HudLayout.Growth.RIGHT_TO_LEFT, 2, 3, 20));
        assertEquals(62, HudLayout.slotOffset(HudLayout.Growth.BOTTOM_TO_TOP, 0, 3, 27));
        assertEquals(31, HudLayout.slotOffset(HudLayout.Growth.TOP_TO_BOTTOM, 1, 3, 27));
        assertEquals(HudLayout.Growth.LEFT_TO_RIGHT, HudLayout.Growth.parse(ClientDefaults.HUD_GROWTH));
        assertNull(HudLayout.Growth.parse("sideways"));
    }

    @Test
    void anchorNamesParseIgnoringCaseAndSpaces() {
        assertEquals(HudLayout.Anchor.TOP_RIGHT, HudLayout.Anchor.parse(" Top-Right "));
        assertNull(HudLayout.Anchor.parse("middle"));
        assertNull(HudLayout.Anchor.parse(null));
        assertArrayEquals(new String[] {"above-hotbar-left", "above-hotbar-right", "top-left", "top-right", "middle-left", "middle-right",
                "bottom-left", "bottom-right"}, HudLayout.Anchor.names());
        assertArrayEquals(new String[] {"left-to-right", "right-to-left", "top-to-bottom", "bottom-to-top"}, HudLayout.Growth.names());
        assertEquals(ClientDefaults.HUD_ANCHOR, HudLayout.Anchor.ABOVE_HOTBAR_LEFT.name);
    }
}
