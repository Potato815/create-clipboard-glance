package dev.clipboardhud.client.layout;

import java.util.function.ToIntFunction;
import net.minecraft.network.chat.FormattedText;

/** One row of the HUD box. The set of row kinds is closed; Java 17 has no exhaustive pattern switch, so a new kind
 * must also be added to the instanceof chain in GoggleClipboardDrawing.drawBody.
 */
public sealed interface HudLine permits PlainLine, EntryLine, ClipboardPageFooter {
    /** Width in GUI pixels; fractional only for the scaled page footer. */
    float measuredWidth(ToIntFunction<FormattedText> measure);
}
