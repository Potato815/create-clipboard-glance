package dev.clipboardhud.create;

import com.mojang.serialization.JsonOps;
import com.simibubi.create.content.equipment.clipboard.ClipboardEntry;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Detached value of one Create entry. Never exposes the source entry or its mutable values. */
public final class ClipboardEntrySnapshot {
    private final boolean checked;
    private final Component text;
    private final ItemStack icon;
    private final HolderLookup.Provider registries;

    public ClipboardEntrySnapshot(ClipboardEntry source, HolderLookup.Provider registries) {
        this.registries = registries;
        checked = source.checked;
        text = copyText(source.text);
        icon = copyIcon(source.icon);
    }

    public boolean checked() { return checked; }
    public Component text() { return copyText(text); }
    public ItemStack icon() { return copyIcon(icon); }

    public boolean matches(ClipboardEntry source) {
        return checked == source.checked && text.equals(source.text) && ItemStack.matches(icon, source.icon);
    }

    private Component copyText(Component value) {
        // Component.copy() shares mutable sibling/translation argument components.
        return Component.Serializer.fromJson(Component.Serializer.toJson(value, registries), registries);
    }

    private ItemStack copyIcon(ItemStack value) {
        if (value.isEmpty()) return ItemStack.EMPTY;
        // A codec round-trip also detaches mutable component values such as custom names.
        var ops = registries.createSerializationContext(JsonOps.INSTANCE);
        var encoded = ItemStack.OPTIONAL_CODEC.encodeStart(ops, value).getOrThrow();
        return ItemStack.OPTIONAL_CODEC.parse(ops, encoded).getOrThrow();
    }
}
