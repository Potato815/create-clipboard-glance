package dev.clipboardhud.client.layout;

import dev.clipboardhud.create.CreateGuiAssets;
import net.minecraft.client.gui.Font;

/** The checkbox and the wider address icon share one centered marker slot, followed by the text column.
 * Create's own screen only shifts the icon 1px left of the box; the HUD centers both instead.
 * The check mark keeps Create's placement on the box.
 */
public final class ClipboardMarkerSlot {
    private ClipboardMarkerSlot() {}

    // Bitmap glyph advances include 1px of spacing after the visible pixels.
    static int glyphWidth(int advance) { return Math.max(0, advance - 1); }
    public static int width(int boxWidth, int iconWidth) { return Math.max(boxWidth, iconWidth); }
    public static int offset(int slotWidth, int itemWidth) { return Math.max(0, (slotWidth - itemWidth) / 2); }

    /** Where the text (or the material icon) starts: the wider marker plus a space, for every row of a page. */
    public static int checkboxColumn(Font font) {
        return checkboxColumn(Math.max(font.width(EntryMarker.BOX_GLYPH), CreateGuiAssets.addressIconWidth()),
                font.width(EntryMarker.CHECK_GLYPH), font.width(" "));
    }

    public static int checkboxColumn(int emptyWidth, int checkedWidth, int gap) {
        return Math.max(emptyWidth, checkedWidth) + gap;
    }

    public static int boxX(Font font, int x) {
        int box = glyphWidth(font.width(EntryMarker.BOX_GLYPH));
        return x + offset(width(box, CreateGuiAssets.addressIconWidth()), box);
    }

    public static int iconX(Font font, int x) {
        int icon = CreateGuiAssets.addressIconWidth();
        return x + offset(width(glyphWidth(font.width(EntryMarker.BOX_GLYPH)), icon), icon);
    }
}
