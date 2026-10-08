package dev.clipboardhud.client.layout;

import net.minecraft.network.chat.Component;

/** Presentation-only Create address notation. Raw source and snapshot text retain the # prefix. */
public final class ClipboardAddress {
    private ClipboardAddress() {}

    public static boolean isAddress(Component text) {
        String raw = text.getString();
        return raw.startsWith("#") && !raw.substring(1).isBlank();
    }

    public static Component displayText(Component detachedText) {
        // Match ClipboardScreen: only remove a leading # and leading whitespace for a valid address.
        return isAddress(detachedText)
                ? Component.literal(detachedText.getString().substring(1).stripLeading()) : detachedText;
    }
}
