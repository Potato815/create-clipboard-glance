package dev.clipboardhud.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.clipboardhud.client.layout.ClipboardHudIcon;
import dev.clipboardhud.client.layout.GoggleHudGeometry;
import dev.clipboardhud.create.CreateGuiAssets;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;

/** Material icons that fade with the page cross-fade.
 * Item models cannot all be drawn translucent directly (solid and cutout block items ignore alpha), so during a
 * flip the icons are drawn into Catnip's shared off-screen buffer, the one Ponder uses for its screen transitions,
 * and the buffer is composited with the page alpha. The HUD only renders without an open screen, so it never overlaps Ponder's
 * use. Without the buffer the caller falls back to swapping icons halfway.
 */
public final class ClipboardIconFade {
    private ClipboardIconFade() {}

    public static boolean available() { return CreateGuiAssets.sharedFramebuffer() != null; }

    public static void drawIcons(GuiGraphics graphics, List<ClipboardHudIcon> icons, int x, int y) {
        // Block item models need the depth test for their faces.
        RenderSystem.enableDepthTest();
        try {
            for (var icon : icons) {
                graphics.renderItem(icon.stack(), x + icon.xOffset(), y + GoggleHudGeometry.lineY(icon.lineIndex()));
            }
            graphics.flush();
        } finally { RenderSystem.disableDepthTest(); }
    }

    /** Draws the icons at the given alpha. The current scissor limits both the off-screen drawing and the result. */
    public static void drawFaded(GuiGraphics graphics, List<ClipboardHudIcon> icons, int x, int y, float alpha) {
        var buffer = CreateGuiAssets.sharedFramebuffer();
        var minecraft = Minecraft.getInstance();
        graphics.flush();
        buffer.clear(Minecraft.ON_OSX);
        buffer.bindWrite(true);
        try {
            drawIcons(graphics, icons, x, y);
        } finally {
            minecraft.getMainRenderTarget().bindWrite(true);
        }
        // Catnip's drawFramebuffer uses Minecraft's blit_screen shader, which ignores the vertex alpha,
        // so the buffer is composited here with position_tex, which multiplies the shader color alpha.
        // Exact GUI size, as in the GUI projection. getGuiScaledHeight() rounds up when the window size is not a
        // multiple of the GUI scale, which would stretch the buffer and move the fading icons by a pixel or two.
        double guiScale = minecraft.getWindow().getGuiScale();
        float width = (float) (buffer.viewWidth / guiScale), height = (float) (buffer.viewHeight / guiScale);
        float u = (float) buffer.viewWidth / buffer.width, v = (float) buffer.viewHeight / buffer.height;
        var matrix = new Matrix4f();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, buffer.getColorTextureId());
        RenderSystem.setShaderColor(1, 1, 1, alpha);
        try {
            // The icons already carry the HUD pose, so the quad covers the screen without it.
            // Render targets store rows bottom-up.
            var quad = Tesselator.getInstance().getBuilder();
            quad.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            quad.vertex(matrix, 0, height, GoggleHudGeometry.Z).uv(0, 0).endVertex();
            quad.vertex(matrix, width, height, GoggleHudGeometry.Z).uv(u, 0).endVertex();
            quad.vertex(matrix, width, 0, GoggleHudGeometry.Z).uv(u, v).endVertex();
            quad.vertex(matrix, 0, 0, GoggleHudGeometry.Z).uv(0, v).endVertex();
            BufferUploader.drawWithShader(quad.end());
        } finally { RenderSystem.setShaderColor(1, 1, 1, 1); }
    }
}
