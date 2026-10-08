package dev.clipboardhud.client.layout;

import java.util.function.ToIntFunction;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;

/** One wrapped row of a clipboard entry. The text starts at a fixed column after the marker slot (and the material
 * icon), so a checkbox state or glyph width never moves the body. All rows of an entry share its ARGB color.
 */
public record EntryLine(FormattedText text, EntryMarker marker, int textOffset, int color,
                        FormattedCharSequence visualOrder) implements HudLine {
    public EntryLine(FormattedText text, EntryMarker marker, int textOffset, int color) {
        this(text, marker, textOffset, color, Language.getInstance().getVisualOrder(text));
    }

    public static EntryLine row(FormattedText text, EntryMarker marker, int checkboxColumn, boolean hasIcon, int color) {
        return new EntryLine(text, marker, ClipboardEntryText.indent(checkboxColumn, hasIcon), color);
    }

    @Override public float measuredWidth(ToIntFunction<FormattedText> measure) {
        int textWidth = textOffset + measure.applyAsInt(text);
        String glyph = marker.glyph();
        return Math.max(textWidth, glyph == null ? 0 : measure.applyAsInt(Component.literal(glyph)));
    }
}
