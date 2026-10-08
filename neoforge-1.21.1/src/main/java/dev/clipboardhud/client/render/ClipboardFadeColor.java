package dev.clipboardhud.client.render;

/** Multiplies the alpha of an ARGB text color for the page cross-fade. */
public final class ClipboardFadeColor {
    private ClipboardFadeColor() {}

    // Font treats an alpha below 4/255 as opaque, so nearly invisible text is skipped instead of drawn.
    public static boolean visible(float alpha) { return alpha >= .05f; }

    public static int apply(int argb, float alpha) {
        int a = Math.round((argb >>> 24) * Math.max(0, Math.min(1, alpha)));
        return Math.max(4, a) << 24 | argb & 0xFFFFFF;
    }
}
