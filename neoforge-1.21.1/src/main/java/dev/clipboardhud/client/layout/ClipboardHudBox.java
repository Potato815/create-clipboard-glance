package dev.clipboardhud.client.layout;

import dev.clipboardhud.client.style.ClipboardHudPadding;
import java.util.List;

/** Padded box height shared by drawing and placement. A page footer already leaves room below the body;
 * without one (single page) the box gets its own bottom padding.
 */
public final class ClipboardHudBox {
    private ClipboardHudBox() {}

    /** Same as the top padding, so the body sits evenly inside the box. */
    public static int bottomWithoutFooter() { return ClipboardHudPadding.top(); }

    public static boolean hasFooter(List<? extends HudLine> lines) {
        return !lines.isEmpty() && ClipboardPageFooter.isFooter(lines.getLast());
    }

    public static int height(List<? extends HudLine> lines) {
        return ClipboardHudSpacing.height(lines) + ClipboardHudPadding.top() + (hasFooter(lines) ? 0 : bottomWithoutFooter());
    }
}
