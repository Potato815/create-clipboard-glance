package dev.clipboardhud.client.render;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ClipboardPageMotionTest {
    private static final long MS = 1_000_000L;

    @Test
    void firstSizeAppearsImmediatelyAndLaterSizesEaseToTheTarget() {
        var motion = new ClipboardPageMotion();
        motion.target(100, 50, 0);
        assertEquals(100, motion.width(0));
        assertEquals(50, motion.height(0));
        motion.target(160, 80, 10 * MS);
        assertEquals(100, motion.width(10 * MS), 1e-3);
        // Gentle in-out curve: a 60 fps first frame moves only a little, the midpoint is halfway.
        float firstFrame = motion.width(10 * MS + 16 * MS);
        assertTrue(firstFrame - 100 < 60 * .05f, "no visible jump in the first frame: " + firstFrame);
        assertEquals(130, motion.width(10 * MS + ClipboardPageMotion.resizeNanos() / 2), 1e-3);
        assertEquals(160, motion.width(10 * MS + ClipboardPageMotion.resizeNanos()), 1e-3);
        assertEquals(80, motion.height(10 * MS + ClipboardPageMotion.resizeNanos()), 1e-3);
        // Repeating the same target does not restart the animation.
        motion.target(160, 80, 500 * MS);
        assertEquals(160, motion.width(500 * MS), 1e-3);
    }

    @Test
    void fastFlipsContinueFromTheCurrentSizeWithoutJumping() {
        var motion = new ClipboardPageMotion();
        motion.target(100, 40, 0);
        motion.target(200, 40, 0);
        long halfway = ClipboardPageMotion.resizeNanos() / 2;
        float current = motion.width(halfway);
        motion.target(120, 40, halfway);
        assertEquals(current, motion.width(halfway), 1e-3, "an interrupted resize starts where it was");
        assertEquals(120, motion.width(halfway + ClipboardPageMotion.resizeNanos()), 1e-3);
    }

    @Test
    void pagesCrossFadeAndMoveInTheFlipDirection() {
        var motion = new ClipboardPageMotion();
        assertEquals(0, motion.incomingShift(0));
        assertEquals(1, motion.incomingAlpha(0));
        assertEquals(0, motion.outgoingAlpha(0));
        long end = ClipboardPageMotion.slideNanos();
        float distance = ClipboardPageMotion.slideDistance();
        motion.target(100, 40, 0);
        // Same box size: a simultaneous cross-fade.
        motion.flip(1, 100, 40, 1, 0);
        // Next page: the new body enters from the right, the old one leaves to the left.
        assertEquals(distance, motion.incomingShift(0), 1e-3);
        assertEquals(0, motion.outgoingShift(0), 1e-3);
        assertEquals(0, motion.incomingAlpha(0), 1e-3);
        assertEquals(1, motion.outgoingAlpha(0), 1e-3);
        float mid = motion.outgoingShift(end / 2);
        assertTrue(mid < 0 && mid > -distance);
        assertEquals(1, motion.incomingAlpha(end / 3) + motion.outgoingAlpha(end / 3), 1e-5);
        assertEquals(0, motion.incomingShift(end), 1e-3);
        assertEquals(-distance, motion.outgoingShift(end), 1e-3);
        assertEquals(1, motion.incomingAlpha(end), 1e-3);
        assertEquals(0, motion.outgoingAlpha(end), 1e-3);
        // Previous page: mirrored.
        motion.flip(-3, 100, 40, 1, 0);
        assertEquals(-distance, motion.incomingShift(0), 1e-3);
        assertEquals(distance, motion.outgoingShift(end), 1e-3);
    }

    @Test
    void flipWithASizeChangeDelaysTheNewBodyWithoutAnEmptyGap() {
        var motion = new ClipboardPageMotion();
        motion.target(100, 40, 0);
        motion.flip(1, 160, 70, 1, 0);
        long out = ClipboardPageMotion.outgoingNanos();
        long inStart = ClipboardPageMotion.incomingDelayNanos();
        long inEnd = inStart + ClipboardPageMotion.incomingNanos();
        // Some text is always visible: the new body starts before the old one is gone.
        assertTrue(inStart < out, "no frame without text");
        for (long t = 0; t <= inEnd; t += 10 * MS) {
            assertTrue(motion.incomingAlpha(t) + motion.outgoingAlpha(t) > .3f, "text visible at " + t / MS + "ms");
        }
        assertEquals(0, motion.incomingAlpha(inStart - 1), 1e-3);
        assertEquals(ClipboardPageMotion.slideDistance(), motion.incomingShift(inStart - 1), 1e-3);
        assertEquals(0, motion.outgoingAlpha(out), 1e-3);
        assertEquals(160, motion.width(ClipboardPageMotion.resizeNanos()), 1e-3);
        assertEquals(70, motion.height(ClipboardPageMotion.resizeNanos()), 1e-3);
        assertEquals(1, motion.incomingAlpha(inEnd), 1e-3);
        assertEquals(0, motion.incomingShift(inEnd), 1e-3);
    }

    @Test
    void textCurvesAvoidLargeFirstFrameJumps() {
        float frame = 16f / 180;
        assertTrue(ClipboardPageMotion.easeOut(frame) < .2f);
        assertTrue(ClipboardPageMotion.easeIn(frame) < .01f);
        assertTrue(ClipboardPageMotion.easeInOut(frame) < .01f);
        assertEquals(.5f, ClipboardPageMotion.easeInOut(.5f), 1e-6);
        for (float t = 0; t <= 1; t += .05f) {
            assertTrue(ClipboardPageMotion.easeInOut(t) >= 0 && ClipboardPageMotion.easeInOut(t) <= 1.0001f);
        }
    }

    @Test
    void interruptedFlipFadesFromTheCurrentVisibility() {
        var motion = new ClipboardPageMotion();
        motion.target(100, 40, 0);
        motion.flip(1, 100, 40, .3f, 0);
        assertEquals(.3f, motion.outgoingAlpha(0), 1e-3);
        assertEquals(0, motion.outgoingAlpha(ClipboardPageMotion.slideNanos()), 1e-3);
        motion.flip(1, 100, 40, 2, 0);
        assertEquals(1, motion.outgoingAlpha(0), 1e-3, "visibility is clamped");
    }

    @Test
    void fadeColorKeepsRgbAndScalesAlphaWithoutBecomingOpaque() {
        assertEquals(0xFF31B25D, ClipboardFadeColor.apply(0xFF31B25D, 1));
        assertEquals(0x8031B25D, ClipboardFadeColor.apply(0xFF31B25D, .5f));
        // Create's inactive address alpha 0x66 is scaled, not replaced.
        assertEquals(0x33 << 24 | 0x8D7F6B, ClipboardFadeColor.apply(0x668D7F6B, .5f));
        // Font draws alpha 0 as opaque; the lowest value stays a faint 4/255.
        assertEquals(4, ClipboardFadeColor.apply(0xFFFFFFFF, 0) >>> 24);
        assertFalse(ClipboardFadeColor.visible(.01f));
        assertTrue(ClipboardFadeColor.visible(.5f));
    }

    @Test
    void boundaryNudgeGoesOutAndBackInTheWheelDirection() {
        var motion = new ClipboardPageMotion();
        assertEquals(0, motion.nudgeShift(0));
        motion.nudge(1, 0);
        assertEquals(0, motion.nudgeShift(0), 1e-3);
        assertEquals(ClipboardPageMotion.nudgeDistance(), motion.nudgeShift(ClipboardPageMotion.nudgeNanos() / 2), 1e-3);
        assertEquals(0, motion.nudgeShift(ClipboardPageMotion.nudgeNanos()), 1e-3);
        assertEquals(0, motion.nudgeShift(10 * ClipboardPageMotion.nudgeNanos()), 1e-3);
        motion.nudge(-1, 0);
        assertTrue(motion.nudgeShift(ClipboardPageMotion.nudgeNanos() / 3) < 0);
    }

    @Test
    void progressAndEasingStayInRange() {
        assertEquals(0, ClipboardPageMotion.progress(-5, 0, 10));
        assertEquals(1, ClipboardPageMotion.progress(50, 0, 10));
        assertEquals(1, ClipboardPageMotion.progress(0, 0, 0));
        assertEquals(0, ClipboardPageMotion.easeOut(0), 1e-6);
        assertEquals(1, ClipboardPageMotion.easeOut(1), 1e-6);
        assertEquals(1, ClipboardPageMotion.easeInOut(1), 1e-6);
    }
}
