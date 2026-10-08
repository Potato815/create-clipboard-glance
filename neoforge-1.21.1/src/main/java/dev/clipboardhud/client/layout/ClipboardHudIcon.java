package dev.clipboardhud.client.layout;

import net.minecraft.world.item.ItemStack;

/** A material icon on a body row, drawn with the body so it follows the page slide and the box clip. */
public record ClipboardHudIcon(ItemStack stack, int lineIndex, int xOffset) {}
