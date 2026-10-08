package dev.clipboardhud.create;

import com.simibubi.create.content.equipment.clipboard.ClipboardEntry;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

/** Reads only data Create has already synchronized; no screen, packet, setter, or world write.
 * Create 6.0.8 keeps the document as NBT on ClipboardBlockEntity.dataContainer, and its own screen writes into
 * that stack in place, so changes are detected by comparing the page tag content with a private copy.
 * The block entity's stack and tag are never modified.
 */
public final class CreateClipboardReader {
    /** Create's page list tag on the clipboard item. */
    private static final String PAGES = "Pages";

    public enum Status { DATA_UNAVAILABLE, EMPTY, READY, ERROR }
    public record Result(Status status, Optional<ClipboardDocumentSnapshot> document) {
        public static Result unavailable() { return new Result(Status.DATA_UNAVAILABLE, Optional.empty()); }
        public static Result error() { return new Result(Status.ERROR, Optional.empty()); }

        /** Like Create's own screen, the HUD shows only entries: a blank, not yet received or unreadable
         * clipboard shows no HUD at all. */
        public boolean hasContent() { return status == Status.READY; }
    }

    private Result cached = Result.unavailable();
    /** Copy of the page tag the cached document was built from (null: the tag had no pages). */
    private Tag cachedPages;

    public Result read(ItemStack dataContainer) {
        var tag = dataContainer.getTag();
        if (tag == null) {
            // Absence alone cannot distinguish a blank clipboard from data not received yet.
            clear();
            return cached;
        }
        Tag pages = tag.get(PAGES);
        if (cached.document().isPresent() && Objects.equals(cachedPages, pages)) return cached;
        // readAll builds new entries from the tag; the stack itself is only read.
        var document = new ClipboardDocumentSnapshot(ClipboardEntry.readAll(dataContainer));
        cachedPages = pages == null ? null : pages.copy();
        cached = new Result(document.isEmpty() ? Status.EMPTY : Status.READY, Optional.of(document));
        return cached;
    }

    public void clear() {
        cached = Result.unavailable();
        cachedPages = null;
    }
}
