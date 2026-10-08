package dev.clipboardhud.create;

import com.simibubi.create.infrastructure.config.AllConfigs;
import net.createmod.catnip.gui.element.BoxElement;
import net.createmod.catnip.theme.Color;

/** Create 6.0.8 goggle overlay settings the HUD follows: the player's offsets, optional custom colors and the
 * default goggle background. Every color is a fresh copy, so applying alpha never changes the shared theme that
 * other Create HUDs draw with.
 */
public final class CreateOverlayConfig {
    private CreateOverlayConfig() {}

    public static int offsetX() { return AllConfigs.client().overlayOffsetX.get(); }
    public static int offsetY() { return AllConfigs.client().overlayOffsetY.get(); }
    public static boolean customColors() { return AllConfigs.client().overlayCustomColor.get(); }

    /** The goggle background (or the player's custom one), multiplied by the given alpha. */
    public static int background(float alpha) {
        var config = AllConfigs.client();
        Color background = config.overlayCustomColor.get() ? new Color(config.overlayBackgroundColor.get())
                : BoxElement.COLOR_VANILLA_BACKGROUND.copy().scaleAlpha(.75f);
        return background.scaleAlpha(alpha).getRGB();
    }

    public static int customBorderTop(float alpha) {
        return new Color(AllConfigs.client().overlayBorderColorTop.get()).scaleAlpha(alpha).getRGB();
    }

    public static int customBorderBottom(float alpha) {
        return new Color(AllConfigs.client().overlayBorderColorBot.get()).scaleAlpha(alpha).getRGB();
    }

    /** An ARGB color multiplied by the given alpha, with the same rounding as Create's own overlay colors. */
    public static int withAlpha(int argb, float alpha) { return new Color(argb, true).scaleAlpha(alpha).getRGB(); }
}
