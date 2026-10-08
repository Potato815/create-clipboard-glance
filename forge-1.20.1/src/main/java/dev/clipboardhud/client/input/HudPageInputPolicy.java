package dev.clipboardhud.client.input;

/** One page per dispatched wheel gesture, matching Create's direction without sensitivity jumps. */
public final class HudPageInputPolicy {
    private HudPageInputPolicy() {}
    public static boolean handles(boolean hudVisible, boolean modifierHeld, int pages, double deltaY) {
        return hudVisible && modifierHeld && pages > 1 && Double.isFinite(deltaY) && deltaY != 0;
    }
    public static int step(double deltaY) {
        if (!Double.isFinite(deltaY) || deltaY == 0) return 0;
        return deltaY < 0 ? 1 : -1;
    }
}
