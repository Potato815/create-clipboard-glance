package dev.clipboardhud.client.render;

import dev.clipboardhud.client.layout.ClipboardMarkerSlot;
import dev.clipboardhud.client.layout.EntryMarker;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** Create 6.0.8 ClipboardScreen.renderWindow checkbox: the box is always drawn one pixel below the text row,
 * faded when checked, and the check mark is drawn over it on the text row.
 */
public final class ClipboardCheckbox {
    private ClipboardCheckbox() {}

    // Create uses 0xFF8D7F6B / 0x668D7F6B on paper. The HUD keeps its lighter box color for the dark
    // background and applies Create's checked alpha (0x66) to it.
    public static int boxColor(boolean checked) { return checked ? 0x66AAAAAA : 0xFFAAAAAA; }
    public static int checkColor() { return 0xFF31B25D; }
    public static int boxYOffset() { return 1; }

    public static void draw(GuiGraphics graphics, Font font, boolean checked, int x, int lineY, float alpha) {
        int boxX = ClipboardMarkerSlot.boxX(font, x);
        graphics.drawString(font, EntryMarker.BOX_GLYPH, boxX, lineY + boxYOffset(),
                ClipboardFadeColor.apply(boxColor(checked), alpha), true);
        if (checked) {
            graphics.drawString(font, EntryMarker.CHECK_GLYPH, boxX, lineY, ClipboardFadeColor.apply(checkColor(), alpha), true);
        }
    }
}
