package dev.clipboardhud.create;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import net.createmod.catnip.gui.UIRenderHelper;
import net.minecraft.client.gui.GuiGraphics;

/** Create 6.0.10 GUI textures and the Catnip off-screen buffer reused by the HUD. No new art is shipped. */
public final class CreateGuiAssets {
    private CreateGuiAssets() {}

    /** The address icon of Create's clipboard screen; the inactive one marks a checked (disabled) address. */
    public static int addressIconWidth() { return AllGuiTextures.CLIPBOARD_ADDRESS.getWidth(); }

    public static void drawAddressIcon(GuiGraphics graphics, boolean inactive, int x, int y) {
        (inactive ? AllGuiTextures.CLIPBOARD_ADDRESS_INACTIVE : AllGuiTextures.CLIPBOARD_ADDRESS).render(graphics, x, y);
    }

    /** Create's schedule scroll arrows, used as page arrows. */
    public static int pageArrowWidth() { return AllGuiTextures.SCHEDULE_SCROLL_LEFT.getWidth(); }

    public static void drawPageArrow(GuiGraphics graphics, boolean next, int x, int y) {
        (next ? AllGuiTextures.SCHEDULE_SCROLL_RIGHT : AllGuiTextures.SCHEDULE_SCROLL_LEFT).render(graphics, x, y);
    }

    /** Catnip's shared off-screen buffer (the one Ponder uses for screen transitions), or null before it exists. */
    public static RenderTarget sharedFramebuffer() { return UIRenderHelper.framebuffer; }
}
