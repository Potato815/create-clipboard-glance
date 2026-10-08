package dev.clipboardhud.client.style;

/** Border accent of the clipboard HUD. The HUD is visible without goggles, so it uses the clipboard's own colors
 * instead of the goggle tooltip purple: Create 6.0.10's clipboard board texture (light grain 0xCEAC6D, dark wood
 * 0x714F0F) as a top-to-bottom gradient like the vanilla tooltip border. Create's goggle border uses alpha 0x50;
 * the less saturated wood needs 0x80 to read as clearly. Create's background and its overlayCustomColor config
 * are unchanged.
 */
public final class ClipboardHudTheme {
    private ClipboardHudTheme() {}
    public static int borderTop() { return 0x80CEAC6D; }
    public static int borderBottom() { return 0x80714F0F; }
}
