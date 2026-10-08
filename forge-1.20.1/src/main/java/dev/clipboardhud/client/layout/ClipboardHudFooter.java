package dev.clipboardhud.client.layout;

import dev.clipboardhud.client.style.ClipboardPageStyle;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

/** Page count is unnecessary for a single page; omitted content still needs its own signal. */
public final class ClipboardHudFooter {
    private ClipboardHudFooter() {}

    public static Component text(int currentPage, int totalPages, boolean clipped) {
        if (totalPages <= 1 && !clipped) return null;
        var text = totalPages > 1
                ? Component.translatable("create_clipboard_hud.preview.page", currentPage, totalPages)
                : Component.literal("\u2026");
        if (totalPages > 1 && clipped) text.append(" \u2026");
        return text.withStyle(Style.EMPTY.withColor(ClipboardPageStyle.color()));
    }
}
