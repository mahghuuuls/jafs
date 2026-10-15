package com.mahghuuuls.jafs.client;

/**
 * Where the HUD row goes (REQ-063): a fixed set of anchors, each placing the row's top-left
 * corner from the screen size, the row size (already multiplied by the scale), and the pixel
 * offsets. The result is then kept on screen: the row never starts left of or above the
 * margin, and it never ends past the right or bottom margin unless it is wider or taller than
 * the screen, in which case its left or top edge wins. Plain Java so the placement rule is
 * unit-tested; nothing here touches the game.
 */
public final class HudLayout {

    /** Half the hotbar's width: the vanilla hotbar is 182 pixels wide, centered. */
    static final int HOTBAR_HALF_WIDTH = 91;
    /** The gap kept between the row and the health rows or the screen edge. */
    static final int MARGIN = 2;

    public enum Anchor {
        ABOVE_HOTBAR_LEFT("above-hotbar-left"),
        ABOVE_HOTBAR_RIGHT("above-hotbar-right"),
        TOP_LEFT("top-left"),
        TOP_RIGHT("top-right"),
        BOTTOM_LEFT("bottom-left"),
        BOTTOM_RIGHT("bottom-right");

        public final String name;

        Anchor(String name) {
            this.name = name;
        }

        /** The configured name, or null when it is none of the anchors (case and surrounding spaces ignored). */
        public static Anchor parse(String text) {
            if (text == null) {
                return null;
            }
            String wanted = text.trim().toLowerCase(java.util.Locale.ROOT);
            for (Anchor anchor : values()) {
                if (anchor.name.equals(wanted)) {
                    return anchor;
                }
            }
            return null;
        }

        /** The names in declaration order, for comments and messages. */
        public static String[] names() {
            Anchor[] all = values();
            String[] names = new String[all.length];
            for (int i = 0; i < all.length; i++) {
                names[i] = all[i].name;
            }
            return names;
        }
    }

    private HudLayout() {
    }

    /**
     * The row's left edge. {@code scaledRowWidth} is the row width after scaling. A positive
     * offset moves the row to the right for every anchor.
     */
    public static int x(Anchor anchor, int screenWidth, int scaledRowWidth, int offsetX) {
        int base;
        switch (anchor) {
            case ABOVE_HOTBAR_LEFT:
                base = screenWidth / 2 - HOTBAR_HALF_WIDTH;
                break;
            case ABOVE_HOTBAR_RIGHT:
                base = screenWidth / 2 + HOTBAR_HALF_WIDTH - scaledRowWidth;
                break;
            case TOP_LEFT:
            case BOTTOM_LEFT:
                base = MARGIN;
                break;
            default:
                base = screenWidth - MARGIN - scaledRowWidth;
                break;
        }
        return keepOnScreen(base + offsetX, screenWidth, scaledRowWidth);
    }

    /**
     * The row's top edge. {@code scaledRowHeight} is the row height after scaling;
     * {@code leftHeight} is how much of the bottom the vanilla overlay already uses on the left
     * (health rows, hotbar), so the hotbar anchors sit above it. A positive offset moves the row
     * down for every anchor.
     */
    public static int y(Anchor anchor, int screenHeight, int scaledRowHeight, int leftHeight, int offsetY) {
        int base;
        switch (anchor) {
            case ABOVE_HOTBAR_LEFT:
            case ABOVE_HOTBAR_RIGHT:
                base = screenHeight - leftHeight - MARGIN - scaledRowHeight;
                break;
            case TOP_LEFT:
            case TOP_RIGHT:
                base = MARGIN;
                break;
            default:
                base = screenHeight - MARGIN - scaledRowHeight;
                break;
        }
        return keepOnScreen(base + offsetY, screenHeight, scaledRowHeight);
    }

    /** The position moved back inside the margins; the near edge wins when the row does not fit. */
    static int keepOnScreen(int position, int screenSize, int rowSize) {
        int max = screenSize - MARGIN - rowSize;
        return Math.max(MARGIN, Math.min(position, max));
    }
}
