package dev.clipboardhud.client.style;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;

/** Page label size and shade. */
public final class ClipboardPageStyle {
    private ClipboardPageStyle() {}

    public static float scale() {
        // About 70% of the body, snapped to whole screen pixels per glyph pixel (ClipboardPixelScale).
        float requested = .70f;
        var minecraft = Minecraft.getInstance();
        return minecraft == null || minecraft.getWindow() == null ? requested
                : ClipboardPixelScale.of(requested, minecraft.getWindow().getGuiScale());
    }

    public static int color() {
        // The same faint shade Create goggle HUDs use for secondary notes (DARK_GRAY with shadow).
        return ChatFormatting.DARK_GRAY.getColor();
    }
}
