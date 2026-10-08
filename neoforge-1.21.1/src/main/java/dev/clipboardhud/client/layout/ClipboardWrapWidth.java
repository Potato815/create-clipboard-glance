package dev.clipboardhud.client.layout;

/** Wraps the HUD body exactly where Create's own clipboard screen wraps it.
 * Source: Create 6.0.10 ClipboardScreen.renderWindow, font.split(text, 150 - iconOffset), iconOffset 16 with an item.
 */
public final class ClipboardWrapWidth {
    private ClipboardWrapWidth() {}
    static final int CREATE_TEXT_WIDTH = 150;
    static final int CREATE_ICON_OFFSET = 16;

    public static int of(boolean hasIcon, int availableWidth) {
        int create = CREATE_TEXT_WIDTH - (hasIcon ? CREATE_ICON_OFFSET : 0);
        // Only a window too narrow for the Create paper width wraps earlier than the original screen.
        return Math.max(1, Math.min(create, availableWidth));
    }
}
