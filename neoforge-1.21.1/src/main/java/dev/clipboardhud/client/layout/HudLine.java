package dev.clipboardhud.client.layout;

import java.util.function.ToIntFunction;
import net.minecraft.network.chat.FormattedText;

/** One row of the HUD box. The set of row kinds is closed, so drawing (GoggleClipboardDrawing) switches over it
 * exhaustively and a new kind fails to compile until it is drawn.
 */
public sealed interface HudLine permits PlainLine, EntryLine, ClipboardPageFooter {
    /** Width in GUI pixels; fractional only for the scaled page footer. */
    float measuredWidth(ToIntFunction<FormattedText> measure);
}
