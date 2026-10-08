package dev.clipboardhud.client.render;

/** Per-frame presentation of the padded box in GUI pixels.
 * The box size and its anchor Y can be fractional while easing; drawing snaps them to screen pixels.
 * bodyAnchorY / outgoingAnchorY keep each page body still at its own final position during a flip, so only the
 * box edges move. Also carries the body shifts/alphas, the page-group nudge, and
 * Create's entrance slide, which the scissor clip must follow.
 */
public record ClipboardHudFrame(float boxWidth, float boxHeight, float boxAnchorY,
                                int bodyAnchorY, float bodyShift, float bodyAlpha,
                                int outgoingAnchorY, float outgoingShift, float outgoingAlpha,
                                float footerShift, float entranceShift) {}
