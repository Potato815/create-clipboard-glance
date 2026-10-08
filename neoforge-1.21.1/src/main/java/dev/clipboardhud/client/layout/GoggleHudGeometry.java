package dev.clipboardhud.client.layout;

/** Create 6.0.10 goggle placement and entrance formulas, with bounds for extreme player-configured offsets. */
public final class GoggleHudGeometry {
    private GoggleHudGeometry() {}

    /** Create draws tooltip text this far right of the anchor X and above the anchor Y. */
    public static final int TEXT_ORIGIN = 12;
    /** Z level of Create's tooltip background and text. */
    public static final int Z = 400;
    public static final int LINE_HEIGHT = 10;
    /** The last line only needs the glyph height, not a full line gap. */
    public static final int LAST_LINE_HEIGHT = 8;
    /** Create's goggle overlay fades in over this many rendered frames. */
    public static final int ENTRANCE_FRAMES = 24;
    public static final int ENTRANCE_SLIDE = 8;

    public static float fade(int renderedFrames, float partialTick) {
        return Math.max(0, Math.min(1, (renderedFrames + partialTick) / (float) ENTRANCE_FRAMES));
    }

    public static double slide(float fade, int offsetX) {
        return Math.pow(1 - fade, 3) * Math.signum(offsetX + .5f) * ENTRANCE_SLIDE;
    }

    public static int anchorX(int screenWidth, int textWidth, int offsetX) {
        return (int) Math.max(-8, Math.min((long) screenWidth / 2 + offsetX, (long) screenWidth - textWidth - 20));
    }

    public static int tooltipHeight(int lines) { return LAST_LINE_HEIGHT + Math.max(0, lines - 1) * LINE_HEIGHT; }
    public static int lineY(int index) { return index * LINE_HEIGHT; }
}
