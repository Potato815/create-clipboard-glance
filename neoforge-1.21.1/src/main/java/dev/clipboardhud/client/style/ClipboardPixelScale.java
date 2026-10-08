package dev.clipboardhud.client.style;

/** Shrinks the bitmap font without damaging its pixels.
 * A fractional size such as 0.7 at GUI scale 4 maps one glyph pixel to 2.8 screen pixels, so strokes alternate
 * between 2 and 3 pixels. Use the size nearest to the request at which one glyph pixel covers whole screen pixels.
 */
public final class ClipboardPixelScale {
    private ClipboardPixelScale() {}

    public static float of(float requested, double guiScale) {
        if (!(guiScale > 0) || Double.isInfinite(guiScale)) return requested;
        // The epsilon keeps exact halves (0.7f * 5 = 3.4999998) rounding up consistently.
        long pixels = Math.max(1, Math.round(requested * guiScale + 1e-4));
        // GUI scale 1 cannot go below one screen pixel per glyph pixel, so the label is then full size.
        return (float) Math.min(1, pixels / guiScale);
    }

    public static float snap(float guiValue, double guiScale) {
        if (!(guiScale > 0) || Double.isInfinite(guiScale)) return guiValue;
        return (float) (Math.round(guiValue * guiScale) / guiScale);
    }
}
