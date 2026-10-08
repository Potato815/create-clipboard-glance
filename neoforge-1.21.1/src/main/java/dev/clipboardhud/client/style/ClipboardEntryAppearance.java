package dev.clipboardhud.client.style;

import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

/** Display-only entry colors from Create 6.0.10 ClipboardScreen, adapted for the dark HUD. */
public final class ClipboardEntryAppearance {
    private ClipboardEntryAppearance() {}

    public static int color(boolean checked, boolean address) {
        // Unchecked text stays readable on the goggle background. Checked colors match Create.
        return checked ? address ? 0x668D7F6B : 0xFF31B25D : 0xFFFFFFFF;
    }

    public static Component prepare(Component detachedText, boolean checked, boolean address) {
        int defaultColor = color(checked, address) & 0xFFFFFF;
        var display = Component.empty();
        // Resolve inherited styles without mutating the source; explicit nonblack colors still win,
        // just as formatted text colors override ClipboardScreen's drawString base color.
        detachedText.visit((Style style, String text) -> {
            var explicit = style.getColor();
            Style displayed = explicit == null || explicit.getValue() == 0
                    ? style.withColor(defaultColor) : style;
            display.append(Component.literal(text).setStyle(displayed));
            return Optional.empty();
        }, Style.EMPTY);
        return display;
    }
}
