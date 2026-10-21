package com.mahghuuuls.jafs.client;

/**
 * Where the HUD goes and how its slots are laid out (REQ-063, REV-012): a fixed set of anchors
 * places the block's top-left corner from the screen size, the block size (already multiplied by
 * the scale), and the pixel offsets; the growth direction says whether slots run in a row or a
 * column and from which end slot 0 starts. The result is kept on screen: the block never starts
 * left of or above the margin, and it never ends past the right or bottom margin unless it is
 * larger than the screen, in which case its left or top edge wins. Plain Java so the placement
 * rules are unit-tested; nothing here touches the game.
 */
public final class HudLayout {

    /** Half the hotbar's width: the vanilla hotbar is 182 pixels wide, centered. */
    static final int HOTBAR_HALF_WIDTH = 91;
    /** The gap kept between the block and the health rows or the screen edge. */
    static final int MARGIN = 2;
    /** The gap between two slots along the growth axis. */
    public static final int GAP = 4;

    /** A setting value with the name the file and the GUI use. */
    public interface Named {
        String configName();
    }

    public enum Anchor implements Named {
        ABOVE_HOTBAR_LEFT("above-hotbar-left"),
        ABOVE_HOTBAR_RIGHT("above-hotbar-right"),
        TOP_LEFT("top-left"),
        TOP_RIGHT("top-right"),
        MIDDLE_LEFT("middle-left"),
        MIDDLE_RIGHT("middle-right"),
        BOTTOM_LEFT("bottom-left"),
        BOTTOM_RIGHT("bottom-right");

        public final String name;

        Anchor(String name) {
            this.name = name;
        }

        @Override
        public String configName() {
            return name;
        }

        /** The configured name, or null when it is none of the anchors (case and surrounding spaces ignored). */
        public static Anchor parse(String text) {
            return (Anchor) find(values(), text);
        }

        /** The names in declaration order, for comments, messages, and the GUI's value cycler. */
        public static String[] names() {
            return namesOf(values());
        }
    }

    /** How the slots are laid out: a row or a column, and from which end slot 0 starts. */
    public enum Growth implements Named {
        LEFT_TO_RIGHT("left-to-right", true),
        RIGHT_TO_LEFT("right-to-left", true),
        TOP_TO_BOTTOM("top-to-bottom", false),
        BOTTOM_TO_TOP("bottom-to-top", false);

        public final String name;
        public final boolean horizontal;

        Growth(String name, boolean horizontal) {
            this.name = name;
            this.horizontal = horizontal;
        }

        @Override
        public String configName() {
            return name;
        }

        /** Whether slot 0 sits at the far end (right or bottom) and later slots run back toward the start. */
        public boolean reversed() {
            return this == RIGHT_TO_LEFT || this == BOTTOM_TO_TOP;
        }

        public static Growth parse(String text) {
            return (Growth) find(values(), text);
        }

        public static String[] names() {
            return namesOf(values());
        }
    }

    private HudLayout() {
    }

    private static Named find(Named[] values, String text) {
        if (text == null) {
            return null;
        }
        String wanted = text.trim().toLowerCase(java.util.Locale.ROOT);
        for (Named value : values) {
            if (value.configName().equals(wanted)) {
                return value;
            }
        }
        return null;
    }

    private static String[] namesOf(Named[] values) {
        String[] names = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            names[i] = values[i].configName();
        }
        return names;
    }

    /** The block's width (unscaled) for {@code count} slots whose cells are {@code cellWidth} wide. */
    public static int blockWidth(Growth growth, int count, int cellWidth) {
        return growth.horizontal ? count * cellWidth + (count - 1) * GAP : cellWidth;
    }

    /** The block's height (unscaled) for {@code count} slots whose cells are {@code cellHeight} tall. */
    public static int blockHeight(Growth growth, int count, int cellHeight) {
        return growth.horizontal ? cellHeight : count * cellHeight + (count - 1) * GAP;
    }

    /**
     * Where slot {@code index} of {@code count} starts along the growth axis, from the block's
     * start edge (unscaled): slot 0 at the start, or at the far end when the growth is reversed.
     */
    public static int slotOffset(Growth growth, int index, int count, int cellSize) {
        int position = growth.reversed() ? count - 1 - index : index;
        return position * (cellSize + GAP);
    }

    /**
     * The block's left edge. {@code scaledWidth} is the block width after scaling. A positive
     * offset moves the block to the right for every anchor.
     */
    public static int x(Anchor anchor, int screenWidth, int scaledWidth, int offsetX) {
        int base;
        switch (anchor) {
            case ABOVE_HOTBAR_LEFT:
                base = screenWidth / 2 - HOTBAR_HALF_WIDTH;
                break;
            case ABOVE_HOTBAR_RIGHT:
                base = screenWidth / 2 + HOTBAR_HALF_WIDTH - scaledWidth;
                break;
            case TOP_LEFT:
            case MIDDLE_LEFT:
            case BOTTOM_LEFT:
                base = MARGIN;
                break;
            default:
                base = screenWidth - MARGIN - scaledWidth;
                break;
        }
        return keepOnScreen(base + offsetX, screenWidth, scaledWidth);
    }

    /**
     * The block's top edge. {@code scaledHeight} is the block height after scaling;
     * {@code leftHeight} is how much of the bottom the vanilla overlay already uses on the left
     * (health rows, hotbar), so the hotbar anchors sit above it. A positive offset moves the block
     * down for every anchor.
     */
    public static int y(Anchor anchor, int screenHeight, int scaledHeight, int leftHeight, int offsetY) {
        int base;
        switch (anchor) {
            case ABOVE_HOTBAR_LEFT:
            case ABOVE_HOTBAR_RIGHT:
                base = screenHeight - leftHeight - MARGIN - scaledHeight;
                break;
            case TOP_LEFT:
            case TOP_RIGHT:
                base = MARGIN;
                break;
            case MIDDLE_LEFT:
            case MIDDLE_RIGHT:
                base = (screenHeight - scaledHeight) / 2;
                break;
            default:
                base = screenHeight - MARGIN - scaledHeight;
                break;
        }
        return keepOnScreen(base + offsetY, screenHeight, scaledHeight);
    }

    /** The left edge of content {@code contentWidth} wide centered in a cell {@code cellWidth} wide (never negative). */
    public static int centered(int cellWidth, int contentWidth) {
        return Math.max(0, (cellWidth - contentWidth) / 2);
    }

    /** The position moved back inside the margins; the near edge wins when the block does not fit. */
    static int keepOnScreen(int position, int screenSize, int blockSize) {
        int max = screenSize - MARGIN - blockSize;
        return Math.max(MARGIN, Math.min(position, max));
    }
}
