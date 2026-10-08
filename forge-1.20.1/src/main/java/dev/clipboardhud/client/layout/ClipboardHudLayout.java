package dev.clipboardhud.client.layout;

import dev.clipboardhud.client.reading.ClipboardPageView;
import dev.clipboardhud.client.style.ClipboardEntryAppearance;
import dev.clipboardhud.client.style.ClipboardPageStyle;
import dev.clipboardhud.create.ClipboardEntrySnapshot;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;

/** Rows, material icons and content width of one HUD page, built from a detached document snapshot.
 * width is fractional because the scaled page footer is measured exactly (ClipboardPageFooter.measuredWidth).
 */
public record ClipboardHudLayout(List<HudLine> lines, List<ClipboardHudIcon> icons, float width) {
    /** maxWidth: text width the window allows; maxLines: rows that fit, including the page footer. */
    public static ClipboardHudLayout build(Font font, ClipboardPageView view, int maxWidth, int maxLines) {
        var builder = new Builder(font, maxWidth, maxLines - (view.pageCount() == 0 ? 0 : 1));
        for (var entry : view.entries()) {
            if (!builder.addEntry(entry)) break;
        }
        builder.addFooter(view);
        if (builder.lines.isEmpty()) builder.lines.add(PlainLine.BLANK);
        // Each page fits its own width; the box eases between sizes (ClipboardPageMotion).
        float width = 0;
        for (var line : builder.lines) width = Math.max(width, line.measuredWidth(font::width));
        return new ClipboardHudLayout(List.copyOf(builder.lines), List.copyOf(builder.icons), width);
    }

    private static final class Builder {
        private final Font font;
        private final int maxWidth;
        private final int bodyLimit;
        private final int checkboxColumn;
        private final List<HudLine> lines = new ArrayList<>();
        private final List<ClipboardHudIcon> icons = new ArrayList<>();
        private boolean clipped;

        private Builder(Font font, int maxWidth, int bodyLimit) {
            this.font = font;
            this.maxWidth = maxWidth;
            this.bodyLimit = bodyLimit;
            this.checkboxColumn = ClipboardMarkerSlot.checkboxColumn(font);
        }

        /** Adds the wrapped rows of one entry; false once the page is full and the rest is clipped. */
        private boolean addEntry(ClipboardEntrySnapshot entry) {
            ItemStack icon = entry.icon();
            boolean hasIcon = !icon.isEmpty();
            // A material entry needs two rows for its 16px icon.
            if (hasIcon && bodyLimit - lines.size() < 2) {
                clipped = true;
                return false;
            }
            boolean address = ClipboardAddress.isAddress(entry.text());
            int color = ClipboardEntryAppearance.color(entry.checked(), address);
            var parts = ClipboardEntryText.split(font, ClipboardEntryText.display(entry), hasIcon,
                    maxWidth - ClipboardEntryText.indent(checkboxColumn, hasIcon));
            int firstLine = lines.size();
            for (int part = 0; part < parts.size(); part++) {
                if (lines.size() >= bodyLimit) {
                    clipped = true;
                    break;
                }
                var marker = part == 0 ? EntryMarker.first(address, entry.checked()) : EntryMarker.NONE;
                lines.add(EntryLine.row(parts.get(part), marker, checkboxColumn, hasIcon, color));
            }
            if (hasIcon && lines.size() > firstLine) {
                if (parts.size() == 1 && lines.size() < bodyLimit) lines.add(PlainLine.BLANK);
                // Never let a 16px icon extend into the page footer when only one text row remains.
                if (lines.size() - firstLine >= 2) icons.add(new ClipboardHudIcon(icon, firstLine, checkboxColumn));
            }
            return !clipped;
        }

        private void addFooter(ClipboardPageView view) {
            int pages = view.pageCount();
            if (pages <= 1 && !clipped) return;
            int page = view.index();
            Component label = pages > 1 ? ClipboardHudFooter.text(page + 1, pages, false) : null;
            // The label hugs both arrows with a constant gap; the group is right-aligned (ClipboardPageFooter).
            int labelSlot = pages > 1 ? font.width(label) : 0;
            Component mark = clipped ? Component.literal("…").withStyle(Style.EMPTY.withColor(ClipboardPageStyle.color())) : null;
            lines.add(new ClipboardPageFooter(label, mark, page > 0, page + 1 < pages, labelSlot, ClipboardPageStyle.scale()));
        }
    }
}
