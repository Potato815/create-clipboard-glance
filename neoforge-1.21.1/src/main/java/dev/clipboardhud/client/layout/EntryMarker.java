package dev.clipboardhud.client.layout;

/** What is drawn in the marker slot before an entry row. Only the first row of an entry has a marker.
 * A valid # address shows Create's address icon instead of a checkbox; its check state picks the inactive icon.
 */
public enum EntryMarker {
    /** Continuation rows of a wrapped entry. */
    NONE,
    OPEN_BOX,
    CHECKED_BOX,
    ADDRESS,
    INACTIVE_ADDRESS;

    public static final String BOX_GLYPH = "□";
    public static final String CHECK_GLYPH = "✔";

    public static EntryMarker first(boolean address, boolean checked) {
        if (address) return checked ? INACTIVE_ADDRESS : ADDRESS;
        return checked ? CHECKED_BOX : OPEN_BOX;
    }

    public boolean isCheckbox() { return this == OPEN_BOX || this == CHECKED_BOX; }
    public boolean isAddress() { return this == ADDRESS || this == INACTIVE_ADDRESS; }
    public boolean checked() { return this == CHECKED_BOX || this == INACTIVE_ADDRESS; }

    /** The glyph that identifies a checkbox's state and width, or null for other markers. */
    public String glyph() {
        return switch (this) {
            case OPEN_BOX -> BOX_GLYPH;
            case CHECKED_BOX -> CHECK_GLYPH;
            default -> null;
        };
    }
}
