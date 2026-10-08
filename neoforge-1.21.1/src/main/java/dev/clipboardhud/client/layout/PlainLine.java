package dev.clipboardhud.client.layout;

import java.util.function.ToIntFunction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;

/** A plain text row, such as the blank row that gives a single-row material entry room for its 16px icon. */
public record PlainLine(Component text) implements HudLine {
    public static final PlainLine BLANK = new PlainLine(Component.empty());

    @Override public float measuredWidth(ToIntFunction<FormattedText> measure) { return measure.applyAsInt(text); }
}
