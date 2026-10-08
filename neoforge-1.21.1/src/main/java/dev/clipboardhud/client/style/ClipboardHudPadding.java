package dev.clipboardhud.client.style;

/** A little space above and at both sides of the body, added inside Create's goggle box (whose own inner gap is
 * 2px). The bottom is handled by ClipboardHudBox. Values in GUI pixels.
 */
public final class ClipboardHudPadding {
    private ClipboardHudPadding() {}
    public static int top() { return 3; }
    public static int left() { return 3; }
    public static int right() { return 3; }
    public static int horizontal() { return left() + right(); }
}
