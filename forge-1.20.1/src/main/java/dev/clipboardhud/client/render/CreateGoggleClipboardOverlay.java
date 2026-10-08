package dev.clipboardhud.client.render;

import dev.clipboardhud.client.layout.ClipboardHudBox;
import dev.clipboardhud.client.layout.ClipboardHudLayout;
import dev.clipboardhud.client.layout.ClipboardHudPlacement;
import dev.clipboardhud.client.layout.GoggleHudGeometry;
import dev.clipboardhud.client.reading.ClipboardPageView;
import dev.clipboardhud.client.style.ClipboardHudPadding;
import dev.clipboardhud.client.style.ClipboardHudTheme;
import dev.clipboardhud.create.ClipboardDocumentSnapshot;
import dev.clipboardhud.create.CreateClipboardReader;
import dev.clipboardhud.create.CreateOverlayConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** Shows one clipboard in Create's goggle overlay style: Create's background, placement, config and entrance,
 * with the clipboard's own text columns. Holds presentation state only (layout cache, entrance, page-flip motion);
 * document data and personal reading positions live elsewhere. Use a new instance for a new target.
 */
public final class CreateGoggleClipboardOverlay {
    /** Width kept outside the text column; only narrow windows wrap earlier than Create's clipboard screen. */
    private static final int SCREEN_MARGIN_X = 32;
    /** Below these the window is too small for a useful page, and the HUD is not drawn. */
    private static final int MIN_TEXT_WIDTH = 60;
    private static final int MIN_LINES = 3;

    /** Everything the cached layout depends on. The document is compared by identity: snapshots are replaced,
     * never changed. */
    private record LayoutKey(ClipboardDocumentSnapshot document, Font font,
                             String language, int maxWidth, int maxLines, int page) {}

    private LayoutKey cachedKey;
    private ClipboardHudLayout cachedLayout;
    private int hoverFrames;
    private final ClipboardPageMotion motion = new ClipboardPageMotion();
    private int displayedPage = -1;
    private ClipboardHudLayout previousLayout;
    private int bodyAnchorY = Integer.MIN_VALUE;
    private int previousAnchorY;

    /** False when nothing was drawn because the window is too small. partialTick: the GUI overlay's partial tick,
     * which Create's goggle overlay also uses for its entrance. */
    public boolean render(GuiGraphics graphics, Minecraft minecraft, float partialTick, CreateClipboardReader.Result result,
                          int page) {
        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();
        int maxWidth = screenWidth - SCREEN_MARGIN_X - ClipboardHudPadding.horizontal();
        int maxLines = ClipboardHudPlacement.maxLines(screenHeight - ClipboardHudPadding.top() - ClipboardHudBox.bottomWithoutFooter());
        if (maxWidth < MIN_TEXT_WIDTH || maxLines < MIN_LINES) return false;
        var shown = cachedLayout;
        var layout = layout(minecraft, result, maxWidth, maxLines, page);
        long now = System.nanoTime();
        float targetWidth = layout.width() + ClipboardHudPadding.horizontal();
        int targetHeight = ClipboardHudBox.height(layout.lines());
        int offsetX = CreateOverlayConfig.offsetX();
        int offsetY = CreateOverlayConfig.offsetY();
        // Each body stays at the position of its own final box, so only the box edges move during a flip.
        int targetAnchorY = ClipboardHudPlacement.centeredAnchorY(screenHeight, targetHeight, offsetY);
        followPage(shown, targetWidth, targetHeight, targetAnchorY, now);
        float boxWidth = motion.width(now);
        float boxHeight = motion.height(now);
        // Placement and screen-edge bounds use the padded box at its current (easing) size.
        int posX = GoggleHudGeometry.anchorX(screenWidth, Math.round(boxWidth), offsetX);
        // Offset from the integer resting position, so the box and the body line up exactly once the resize ends.
        float boxAnchorY = targetAnchorY + ClipboardHudPlacement.centeredAnchorY(screenHeight, boxHeight, offsetY)
                - ClipboardHudPlacement.centeredAnchorY(screenHeight, (float) targetHeight, offsetY);
        hoverFrames = Math.min(GoggleHudGeometry.ENTRANCE_FRAMES, hoverFrames + 1);
        float fade = GoggleHudGeometry.fade(hoverFrames, partialTick);
        var pose = graphics.pose();
        pose.pushPose();
        try {
            float entrance = (float) GoggleHudGeometry.slide(fade, offsetX);
            pose.translate(entrance, 0, 0);
            var frame = new ClipboardHudFrame(boxWidth, boxHeight, boxAnchorY,
                    bodyAnchorY, motion.incomingShift(now), motion.incomingAlpha(now),
                    previousAnchorY, motion.outgoingShift(now), motion.outgoingAlpha(now), motion.nudgeShift(now), entrance);
            GoggleClipboardDrawing.draw(graphics, minecraft.font, layout, previousLayout, frame, posX, colors(fade));
        } finally { pose.popPose(); }
        return true;
    }

    /** The wheel asked for a page beyond the first/last one of the shown clipboard. */
    public void nudge(int direction) { motion.nudge(direction, System.nanoTime()); }

    /** Starts a flip when the shown page changed, and eases the box toward the current page's size. */
    private void followPage(ClipboardHudLayout shown, float targetWidth, int targetHeight, int targetAnchorY, long now) {
        int page = cachedKey.page();
        if (displayedPage >= 0 && page != displayedPage) {
            // An interrupted flip continues from whichever page is more visible right now.
            float incoming = motion.incomingAlpha(now), outgoing = motion.outgoingAlpha(now);
            boolean keepOutgoing = previousLayout != null && outgoing > incoming;
            if (!keepOutgoing) {
                previousLayout = shown;
                previousAnchorY = bodyAnchorY;
            }
            motion.flip(page - displayedPage, targetWidth, targetHeight, keepOutgoing ? outgoing : incoming, now);
        }
        displayedPage = page;
        bodyAnchorY = targetAnchorY;
        motion.target(targetWidth, targetHeight, now);
        if (!ClipboardFadeColor.visible(motion.outgoingAlpha(now))) previousLayout = null;
    }

    /** Create's background; the border uses the clipboard accent instead of the goggle purple, unless the player
     * set custom overlay colors. */
    private static GoggleClipboardDrawing.BoxColors colors(float fade) {
        boolean custom = CreateOverlayConfig.customColors();
        int background = CreateOverlayConfig.background(fade);
        int borderTop = custom ? CreateOverlayConfig.customBorderTop(fade)
                : CreateOverlayConfig.withAlpha(ClipboardHudTheme.borderTop(), fade);
        int borderBottom = custom ? CreateOverlayConfig.customBorderBottom(fade)
                : CreateOverlayConfig.withAlpha(ClipboardHudTheme.borderBottom(), fade);
        return new GoggleClipboardDrawing.BoxColors(background, borderTop, borderBottom);
    }

    private ClipboardHudLayout layout(Minecraft minecraft, CreateClipboardReader.Result result,
                                      int maxWidth, int maxLines, int page) {
        var document = result.document().orElse(null);
        var view = ClipboardPageView.select(document, page);
        var key = new LayoutKey(document, minecraft.font, minecraft.getLanguageManager().getSelected(),
                maxWidth, maxLines, view.index());
        if (cachedLayout == null || !key.equals(cachedKey)) {
            cachedLayout = ClipboardHudLayout.build(minecraft.font, view, maxWidth, maxLines);
            cachedKey = key;
        }
        return cachedLayout;
    }
}
