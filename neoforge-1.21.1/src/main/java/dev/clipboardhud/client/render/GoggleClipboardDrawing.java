package dev.clipboardhud.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.clipboardhud.client.layout.ClipboardHudBox;
import dev.clipboardhud.client.layout.ClipboardHudLayout;
import dev.clipboardhud.client.layout.ClipboardHudSpacing;
import dev.clipboardhud.client.layout.ClipboardMarkerSlot;
import dev.clipboardhud.client.layout.ClipboardPageFooter;
import dev.clipboardhud.client.layout.EntryLine;
import dev.clipboardhud.client.layout.GoggleHudGeometry;
import dev.clipboardhud.client.layout.HudLine;
import dev.clipboardhud.client.layout.PlainLine;
import dev.clipboardhud.client.style.ClipboardHudPadding;
import dev.clipboardhud.client.style.ClipboardPixelScale;
import dev.clipboardhud.create.CreateGuiAssets;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;

/** Create 6.0.10 RemovedGuiUtils background geometry, with body-only text instead of a tooltip title.
 * Source: Creators-of-Create/Create, mc1.21.1-6.0.10, foundation/gui/RemovedGuiUtils.java.
 * That helper hard-codes a title gap and one font scale, so the clipboard draws its own text layer.
 */
public final class GoggleClipboardDrawing {
    private GoggleClipboardDrawing() {}

    /** ARGB colors of the box, already faded with Create's entrance. */
    public record BoxColors(int background, int borderTop, int borderBottom) {}

    /** previous: the page being left during a flip, or null. */
    public static void draw(GuiGraphics graphics, Font font, ClipboardHudLayout current, ClipboardHudLayout previous,
                            ClipboardHudFrame frame, int anchorX, BoxColors colors) {
        double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
        // The box keeps Create's origin and grows by the padding; text starts inside it.
        int x = anchorX + GoggleHudGeometry.TEXT_ORIGIN + ClipboardHudPadding.left();
        float boxX = anchorX + GoggleHudGeometry.TEXT_ORIGIN;
        float boxY = frame.boxAnchorY() - GoggleHudGeometry.TEXT_ORIGIN;
        float bw = frame.boxWidth(), bh = frame.boxHeight();
        var pose = graphics.pose();
        RenderSystem.disableDepthTest();
        try {
            drawBox(graphics, boxX, boxY, bw, bh, guiScale, colors);
            // Content that does not fit an easing box stays inside the border. Scissor ignores the pose,
            // so it adds Create's entrance slide itself.
            float clipShift = frame.entranceShift();
            graphics.enableScissor((int) Math.floor(boxX - 2 + clipShift), (int) Math.floor(boxY - 2),
                    (int) Math.ceil(boxX + bw + 2 + clipShift), (int) Math.ceil(boxY + bh + 2));
            pose.pushPose();
            try {
                pose.translate(0, 0, GoggleHudGeometry.Z);
                int bodyTop = ClipboardHudPadding.top() - GoggleHudGeometry.TEXT_ORIGIN;
                // Icons fade through an off-screen buffer (ClipboardIconFade); without it, each page shows its icons
                // while it is the more visible one.
                if (previous != null) {
                    drawBody(graphics, font, previous, x, frame.outgoingAnchorY() + bodyTop,
                            frame.outgoingShift(), frame.outgoingAlpha(), frame.outgoingAlpha() > .5f, guiScale);
                }
                drawBody(graphics, font, current, x, frame.bodyAnchorY() + bodyTop,
                        frame.bodyShift(), frame.bodyAlpha(), frame.bodyAlpha() >= .5f, guiScale);
                drawFooters(graphics, font, current.lines(), x, boxY, bw, bh, frame.footerShift(), guiScale);
                graphics.flush();
            } finally {
                pose.popPose();
                graphics.disableScissor();
            }
        } finally { RenderSystem.enableDepthTest(); }
    }

    /** Create's tooltip background and border at fractional GUI coordinates, drawn on whole screen pixels so an
     * easing box moves one screen pixel at a time instead of one GUI pixel. */
    private static void drawBox(GuiGraphics graphics, float boxX, float boxY, float boxWidth, float boxHeight,
                                double guiScale, BoxColors colors) {
        float s = (float) guiScale;
        int left = Math.round(boxX * s), top = Math.round(boxY * s);
        int right = Math.round((boxX + boxWidth) * s), bottom = Math.round((boxY + boxHeight) * s);
        int p = Math.max(1, Math.round(s));
        int z = GoggleHudGeometry.Z;
        int background = colors.background(), borderTop = colors.borderTop(), borderBottom = colors.borderBottom();
        var pose = graphics.pose();
        pose.pushPose();
        try {
            pose.scale(1 / s, 1 / s, 1);
            // Same padding, gradient border and z level as Create's goggle tooltip (offsets in GUI pixels × p).
            graphics.fillGradient(left - 3 * p, top - 4 * p, right + 3 * p, top - 3 * p, z, background, background);
            graphics.fillGradient(left - 3 * p, bottom + 3 * p, right + 3 * p, bottom + 4 * p, z, background, background);
            graphics.fillGradient(left - 3 * p, top - 3 * p, right + 3 * p, bottom + 3 * p, z, background, background);
            graphics.fillGradient(left - 4 * p, top - 3 * p, left - 3 * p, bottom + 3 * p, z, background, background);
            graphics.fillGradient(right + 3 * p, top - 3 * p, right + 4 * p, bottom + 3 * p, z, background, background);
            graphics.fillGradient(left - 3 * p, top - 2 * p, left - 2 * p, bottom + 2 * p, z, borderTop, borderBottom);
            graphics.fillGradient(right + 2 * p, top - 2 * p, right + 3 * p, bottom + 2 * p, z, borderTop, borderBottom);
            graphics.fillGradient(left - 3 * p, top - 3 * p, right + 3 * p, top - 2 * p, z, borderTop, borderTop);
            graphics.fillGradient(left - 3 * p, bottom + 2 * p, right + 3 * p, bottom + 3 * p, z, borderBottom, borderBottom);
        } finally { pose.popPose(); }
    }

    /** Body rows and icons of one page; the footer is drawn separately because it follows the box edges. */
    private static void drawBody(GuiGraphics graphics, Font font, ClipboardHudLayout layout,
                                 int x, int y, float shift, float alpha, boolean showIcons, double guiScale) {
        var lines = layout.lines();
        var icons = layout.icons();
        if (lines.isEmpty() || !ClipboardFadeColor.visible(alpha)) return;
        var pose = graphics.pose();
        pose.pushPose();
        try {
            pose.translate(ClipboardPixelScale.snap(shift, guiScale), 0, 0);
            for (int i = 0; i < lines.size(); i++) {
                int lineY = y + ClipboardHudSpacing.lineY(lines, i);
                switch (lines.get(i)) {
                    case PlainLine plain -> graphics.drawString(font, Language.getInstance().getVisualOrder(plain.text()),
                            x, lineY, ClipboardFadeColor.apply(0xffffffff, alpha), true);
                    case EntryLine entry -> drawEntry(graphics, font, entry, x, lineY, alpha);
                    case ClipboardPageFooter footer -> { }
                }
            }
            graphics.flush();
            if (icons.isEmpty()) return;
            if (alpha >= .999f) ClipboardIconFade.drawIcons(graphics, icons, x, y);
            else if (ClipboardIconFade.available()) ClipboardIconFade.drawFaded(graphics, icons, x, y, alpha);
            else if (showIcons) ClipboardIconFade.drawIcons(graphics, icons, x, y);
        } finally { pose.popPose(); }
    }

    private static void drawEntry(GuiGraphics graphics, Font font, EntryLine entry, int x, int lineY, float alpha) {
        var marker = entry.marker();
        if (marker.isAddress()) {
            RenderSystem.enableBlend();
            graphics.setColor(1, 1, 1, alpha);
            try {
                CreateGuiAssets.drawAddressIcon(graphics, marker.checked(), ClipboardMarkerSlot.iconX(font, x), lineY);
            } finally { graphics.setColor(1, 1, 1, 1); }
        } else if (marker.isCheckbox()) {
            ClipboardCheckbox.draw(graphics, font, marker.checked(), x, lineY, alpha);
        }
        var pose = graphics.pose();
        pose.pushPose();
        try {
            pose.translate(x + entry.textOffset(), lineY, 0);
            graphics.drawString(font, entry.visualOrder(), 0, 0, ClipboardFadeColor.apply(entry.color(), alpha), true);
        } finally { pose.popPose(); }
    }

    /** The footer follows the current right and bottom edges of the easing box. */
    private static void drawFooters(GuiGraphics graphics, Font font, List<HudLine> lines, int x, float boxY,
                                    float boxWidth, float boxHeight, float groupShift, double guiScale) {
        var pose = graphics.pose();
        float footerY = boxY + ClipboardHudPadding.top() + boxHeight - ClipboardHudBox.height(lines);
        for (int i = 0; i < lines.size(); i++) {
            if (!(lines.get(i) instanceof ClipboardPageFooter footer)) continue;
            float rowY = ClipboardPixelScale.snap(footerY + ClipboardHudSpacing.lineY(lines, i), guiScale);
            pose.pushPose();
            try {
                pose.translate(0, rowY, 0);
                // The fractional width moves only the right-aligned group, never the left clipped mark.
                float contentWidth = ClipboardPixelScale.snap(boxWidth - ClipboardHudPadding.horizontal(), guiScale);
                int wholeWidth = (int) Math.floor(contentWidth);
                drawFooter(graphics, font, footer, x, wholeWidth,
                        ClipboardPixelScale.snap(groupShift, guiScale) + contentWidth - wholeWidth, guiScale);
            } finally { pose.popPose(); }
        }
    }

    /** groupShift: the page-flip nudge of the arrow/number group (ClipboardPageMotion); the clipped mark stays. */
    private static void drawFooter(GuiGraphics graphics, Font font, ClipboardPageFooter footer, int x, int panelWidth,
                                   float groupShift, double guiScale) {
        float scale = footer.scale();
        if (footer.clipped() != null) drawScaledText(graphics, font, footer.clipped(), x, scale);
        if (footer.label() == null) return;
        int arrow = ClipboardPageFooter.arrowWidth();
        int labelSlot = footer.labelSlot();
        float groupX = ClipboardPixelScale.snap(x + panelWidth - ClipboardPageFooter.groupWidth(arrow, labelSlot) * scale,
                guiScale) + groupShift;
        var pose = graphics.pose();
        pose.pushPose();
        try {
            pose.translate(groupX, 0, 0);
            pose.scale(scale, scale, 1);
            // Both arrows are always drawn; one that cannot be used is faint (ClipboardPageArrow).
            ClipboardPageArrow.draw(graphics, false, 0, footer.previous());
            ClipboardPageArrow.draw(graphics, true, ClipboardPageFooter.nextArrowX(arrow, labelSlot), footer.next());
            graphics.drawString(font, Language.getInstance().getVisualOrder(footer.label()),
                    ClipboardPageFooter.labelX(arrow, labelSlot, font.width(footer.label())), 0, 0xffffffff, true);
        } finally { pose.popPose(); }
    }

    private static void drawScaledText(GuiGraphics graphics, Font font, Component text, float x, float scale) {
        var pose = graphics.pose();
        pose.pushPose();
        try {
            pose.translate(x, 0, 0);
            pose.scale(scale, scale, 1);
            graphics.drawString(font, Language.getInstance().getVisualOrder(text), 0, 0, 0xffffffff, true);
        } finally { pose.popPose(); }
    }
}
