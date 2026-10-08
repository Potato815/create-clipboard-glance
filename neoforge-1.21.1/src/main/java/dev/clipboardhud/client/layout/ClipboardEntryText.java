package dev.clipboardhud.client.layout;

import dev.clipboardhud.client.style.ClipboardEntryAppearance;
import dev.clipboardhud.create.ClipboardEntrySnapshot;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;

/** Display text, indentation and wrapping of one entry. */
public final class ClipboardEntryText {
    private ClipboardEntryText() {}

    /** A 16px material icon plus a 4px gap before the text. */
    public static final int MATERIAL_ICON_SLOT = 20;

    public static Component display(ClipboardEntrySnapshot entry) {
        Component raw = entry.text();
        boolean address = ClipboardAddress.isAddress(raw);
        return ClipboardEntryAppearance.prepare(ClipboardAddress.displayText(raw), entry.checked(), address);
    }

    public static int indent(int checkboxColumn, boolean hasIcon) {
        return checkboxColumn + (hasIcon ? MATERIAL_ICON_SLOT : 0);
    }

    public static List<FormattedText> split(Font font, Component text, boolean hasIcon, int availableWidth) {
        var parts = font.getSplitter().splitLines(text, ClipboardWrapWidth.of(hasIcon, availableWidth), Style.EMPTY);
        return parts.isEmpty() ? List.of(Component.empty()) : parts;
    }
}
