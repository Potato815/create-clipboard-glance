package dev.clipboardhud.client.layout;

import dev.clipboardhud.create.CreateGuiAssets;
import java.util.function.ToIntFunction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;

/** Bottom row: a clipped mark at the left and a [previous, page, next] group at the right.
 * Both arrow slots are always reserved and drawn; an arrow that cannot be used is faint, so it never moves the label.
 * The label (labelSlot = its own width) keeps the same gap to both arrows, and the group keeps the box's right edge;
 * a page number gaining a digit therefore moves only the left arrow and the label.
 * Arrows reuse Create's schedule scroll textures. Gaps are in unscaled pixels.
 * label is null for a single page; clipped is null when nothing was cut off.
 */
public record ClipboardPageFooter(Component label, Component clipped, boolean previous, boolean next,
                                  int labelSlot, float scale) implements HudLine {
    public static int arrowGap() { return 3; }
    public static int clippedGap() { return 6; }
    public static int arrowWidth() { return CreateGuiAssets.pageArrowWidth(); }
    public static boolean isFooter(HudLine line) { return line instanceof ClipboardPageFooter; }
    public static int groupWidth(int arrowWidth, int labelSlot) { return 2 * arrowWidth + 2 * arrowGap() + labelSlot; }
    public static int labelX(int arrowWidth, int labelSlot, int labelWidth) {
        return arrowWidth + arrowGap() + Math.max(0, (labelSlot - labelWidth) / 2);
    }
    public static int nextArrowX(int arrowWidth, int labelSlot) { return arrowWidth + 2 * arrowGap() + labelSlot; }

    @Override public float measuredWidth(ToIntFunction<FormattedText> measure) { return measuredWidth(measure, arrowWidth()); }

    /** Exact panel width needed by this row, in GUI pixels. Not rounded up: with a pixel-exact scale it falls on
     * whole screen pixels, and rounding to whole GUI pixels would put a varying remainder before the left arrow
     * (the left arrow would move when the label gains a digit). */
    public float measuredWidth(ToIntFunction<FormattedText> measure, int arrowWidth) {
        float group = label == null ? 0 : groupWidth(arrowWidth, labelSlot) * scale;
        float mark = clipped == null ? 0 : measure.applyAsInt(clipped) * scale;
        float gap = group > 0 && mark > 0 ? clippedGap() * scale : 0;
        return mark + gap + group;
    }
}
