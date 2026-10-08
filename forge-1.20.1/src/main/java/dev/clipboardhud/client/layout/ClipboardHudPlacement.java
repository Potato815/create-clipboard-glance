package dev.clipboardhud.client.layout;

/** The box is centered vertically on the screen; horizontal Create placement is retained. */
public final class ClipboardHudPlacement {
    private ClipboardHudPlacement() {}

    /** Screen height kept outside the text lines, so the centered box stays inside the window. */
    private static final int RESERVED_HEIGHT = 40;
    /** Lowest anchor Y: the text starts 12px and its background 8px below the top of the window. */
    private static final int MIN_ANCHOR_Y = 24;

    public static int maxLines(int screenHeight) {
        // The whole available height is used; there is no fixed line cap.
        return Math.max(0, (screenHeight - RESERVED_HEIGHT - ClipboardHudSpacing.footerGap()) / GoggleHudGeometry.LINE_HEIGHT);
    }

    /** Fractional variant for a box whose height is easing; the same bounds as the integer version. */
    public static float centeredAnchorY(int screenHeight, float boxHeight, int offsetY) {
        double desired = screenHeight / 2.0 + offsetY + GoggleHudGeometry.TEXT_ORIGIN - boxHeight / 2.0;
        double maximum = Math.max(MIN_ANCHOR_Y, screenHeight - (double) boxHeight);
        return (float) Math.max(MIN_ANCHOR_Y, Math.min(desired, maximum));
    }

    public static int centeredAnchorY(int screenHeight, int textHeight, int offsetY) {
        // Create text origin: anchor - 12. Background adds 4px padding on each side.
        long desired = (long) screenHeight / 2 + offsetY + GoggleHudGeometry.TEXT_ORIGIN - textHeight / 2;
        long maximum = Math.max(MIN_ANCHOR_Y, (long) screenHeight - textHeight);
        return (int) Math.max(MIN_ANCHOR_Y, Math.min(desired, maximum));
    }
}
