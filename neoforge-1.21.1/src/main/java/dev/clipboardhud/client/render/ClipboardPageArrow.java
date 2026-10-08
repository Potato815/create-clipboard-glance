package dev.clipboardhud.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.clipboardhud.client.style.ClipboardPageStyle;
import dev.clipboardhud.create.CreateGuiAssets;
import net.minecraft.client.gui.GuiGraphics;

/** Page arrows: Create's schedule scroll texture tinted to the page label shade, with the label's 1px shadow.
 * An arrow that cannot be used stays visible but faint, so the reserved slot does not read as extra margin on
 * the first/last page and still marks the end of the document.
 */
public final class ClipboardPageArrow {
    private ClipboardPageArrow() {}

    /** Alpha of an arrow that cannot be used. */
    public static float disabledAlpha() { return .35f; }

    public static float alpha(boolean enabled) { return enabled ? 1 : disabledAlpha(); }

    /** next: the right-pointing arrow; otherwise the left-pointing one. */
    public static void draw(GuiGraphics graphics, boolean next, int x, boolean enabled) {
        // Create's arrow gray is 0xA3A3A3; scale it to the label shade.
        float shade = (ClipboardPageStyle.color() & 0xFF) / (float) 0xA3;
        float alpha = alpha(enabled);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        try {
            graphics.setColor(shade / 4, shade / 4, shade / 4, alpha);
            CreateGuiAssets.drawPageArrow(graphics, next, x + 1, 1);
            graphics.setColor(shade, shade, shade, alpha);
            CreateGuiAssets.drawPageArrow(graphics, next, x, 0);
        } finally { graphics.setColor(1, 1, 1, 1); }
    }
}
