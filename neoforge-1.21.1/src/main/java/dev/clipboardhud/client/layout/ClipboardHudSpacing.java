package dev.clipboardhud.client.layout;

import java.util.List;

/** Body/footer separation shared by drawing and the screen-edge placement calculation. */
public final class ClipboardHudSpacing {
    private ClipboardHudSpacing() {}

    /** Extra space between the body and the page footer, in GUI pixels. */
    public static int footerGap() { return 4; }

    public static int lineY(List<? extends HudLine> lines, int index) {
        int y = GoggleHudGeometry.lineY(index);
        return index > 0 && ClipboardPageFooter.isFooter(lines.get(index))
                ? y + footerGap() : y;
    }

    public static int height(List<? extends HudLine> lines) {
        int height = GoggleHudGeometry.tooltipHeight(lines.size());
        return lines.size() > 1 && ClipboardPageFooter.isFooter(lines.getLast())
                ? height + footerGap() : height;
    }
}
