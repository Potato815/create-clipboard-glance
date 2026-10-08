package dev.clipboardhud.client.render;

/** Page-flip motion: the box eases to each page's size, the previous body fades out while sliding away and the
 * new body fades in from the flip direction, and the page group nudges when the first/last page cannot go further.
 * Text moving with the box looks worse, and short ease-out curves look like dropped frames, so the drawing keeps
 * both bodies still vertically (only the box edges move), the resize uses a gentle in-out curve, and when the size
 * changes the new body starts a little later so it overlaps less with the resize.
 * Every change restarts from the current state, so fast wheel input never queues animations.
 */
public final class ClipboardPageMotion {
    public static long resizeNanos() { return 180_000_000L; }
    /** Cross-fade length of both bodies when the box size stays the same. */
    public static long slideNanos() { return 180_000_000L; }
    /** Flip with a size change: the old body fades sooner and the new one waits briefly for the box. */
    public static long outgoingNanos() { return 140_000_000L; }
    public static long incomingDelayNanos() { return 60_000_000L; }
    public static long incomingNanos() { return 180_000_000L; }
    public static long nudgeNanos() { return 160_000_000L; }
    // Create's goggle entrance also slides 8px.
    public static float slideDistance() { return 8; }
    public static float nudgeDistance() { return 2; }

    private boolean sized;
    private float fromWidth, fromHeight;
    private float toWidth;
    private int toHeight;
    private long resizeStart;
    private int direction;
    private long outStart, outLength = 1, inStart, inLength = 1;
    private float outFrom;
    private boolean flipping;
    private int nudgeDirection;
    private long nudgeStart;

    /** Size change without a page flip (first display, edits, window changes). */
    public void target(float width, int height, long now) {
        if (!sized) {
            // The first frame of a target uses Create's entrance instead of a resize.
            sized = true;
            fromWidth = toWidth = width;
            fromHeight = toHeight = height;
            resizeStart = now - resizeNanos();
            return;
        }
        if (width == toWidth && height == toHeight) return;
        fromWidth = width(now);
        fromHeight = height(now);
        toWidth = width;
        toHeight = height;
        resizeStart = now;
    }

    /** direction: +1 for the next page, -1 for the previous page. outgoingFrom: how visible the page being left
     * currently is (1 unless a previous flip was interrupted). */
    public void flip(int direction, float width, int height, float outgoingFrom, long now) {
        this.direction = Integer.signum(direction);
        flipping = true;
        outFrom = Math.max(0, Math.min(1, outgoingFrom));
        outStart = now;
        boolean resized = sized && (width != toWidth || height != toHeight);
        outLength = resized ? outgoingNanos() : slideNanos();
        inStart = resized ? now + incomingDelayNanos() : now;
        inLength = resized ? incomingNanos() : slideNanos();
        target(width, height, now);
    }

    public float width(long now) { return lerp(fromWidth, toWidth, easeInOut(progress(now, resizeStart, resizeNanos()))); }
    public float height(long now) { return lerp(fromHeight, toHeight, easeInOut(progress(now, resizeStart, resizeNanos()))); }

    /** The next page enters from the right, the previous page from the left, decelerating into place. */
    public float incomingShift(long now) {
        if (!flipping) return 0;
        return direction * slideDistance() * (1 - easeOut(progress(now, inStart, inLength)));
    }

    /** The page being left accelerates away in the opposite direction. */
    public float outgoingShift(long now) {
        if (!flipping) return 0;
        return -direction * slideDistance() * easeIn(progress(now, outStart, outLength));
    }

    public float incomingAlpha(long now) { return flipping ? progress(now, inStart, inLength) : 1; }
    public float outgoingAlpha(long now) { return flipping ? outFrom * (1 - progress(now, outStart, outLength)) : 0; }

    /** direction: the wheel direction that could not move further (+1 past the last page, -1 before the first). */
    public void nudge(int direction, long now) {
        nudgeDirection = Integer.signum(direction);
        nudgeStart = now;
    }

    public float nudgeShift(long now) {
        if (nudgeDirection == 0) return 0;
        float t = progress(now, nudgeStart, nudgeNanos());
        // Out and back once; zero at both ends.
        return nudgeDirection * nudgeDistance() * (float) Math.sin(Math.PI * t);
    }

    static float progress(long now, long start, long duration) {
        if (duration <= 0) return 1;
        return Math.max(0, Math.min(1, (now - start) / (float) duration));
    }

    // Quadratic ends move less in the first and last frames than the cubic ease-out used for Create's long entrance.
    static float easeOut(float t) { return 1 - (1 - t) * (1 - t); }
    static float easeIn(float t) { return t * t; }
    static float easeInOut(float t) { return t < .5f ? 4 * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 3) / 2; }

    private static float lerp(float from, float to, float t) { return from + (to - from) * t; }
}
