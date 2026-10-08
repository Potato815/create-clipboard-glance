package dev.clipboardhud.create;

import com.simibubi.create.content.equipment.clipboard.ClipboardEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Detached value of one Create entry. Never exposes the source entry or its mutable values. */
public final class ClipboardEntrySnapshot {
    private final boolean checked;
    private final Component text;
    private final ItemStack icon;

    public ClipboardEntrySnapshot(ClipboardEntry source) {
        checked = source.checked;
        text = copyText(source.text);
        icon = copyIcon(source.icon);
    }

    public boolean checked() { return checked; }
    public Component text() { return copyText(text); }
    public ItemStack icon() { return copyIcon(icon); }

    private static Component copyText(Component value) {
        // Component.copy() shares mutable sibling/translation argument components.
        return Component.Serializer.fromJson(Component.Serializer.toJson(value));
    }

    private static ItemStack copyIcon(ItemStack value) {
        // ItemStack.copy() also copies the NBT tag (custom names and other item data).
        return value.isEmpty() ? ItemStack.EMPTY : value.copy();
    }
}
