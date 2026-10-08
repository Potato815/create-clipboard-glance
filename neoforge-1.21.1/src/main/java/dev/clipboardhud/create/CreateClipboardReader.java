package dev.clipboardhud.create;

import com.simibubi.create.AllDataComponents;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;

/** Reads only data Create has already synchronized; no screen, packet, setter, or world write. */
public final class CreateClipboardReader {
    public enum Status { DATA_UNAVAILABLE, EMPTY, READY, ERROR }
    public record Result(Status status, Optional<ClipboardDocumentSnapshot> document) {
        public static Result unavailable() { return new Result(Status.DATA_UNAVAILABLE, Optional.empty()); }
        public static Result error() { return new Result(Status.ERROR, Optional.empty()); }

        /** Like Create's own screen, the HUD shows only entries: a blank, not yet received or unreadable
         * clipboard shows no HUD at all. */
        public boolean hasContent() { return status == Status.READY; }
    }

    private Result cached = Result.unavailable();

    public Result read(DataComponentMap components, HolderLookup.Provider registries) {
        var content = components.get(AllDataComponents.CLIPBOARD_CONTENT);
        if (content == null) {
            // Absence alone cannot distinguish a blank clipboard from data not received yet.
            clear();
            return cached;
        }
        var oldDocument = cached.document();
        if (oldDocument.isPresent() && oldDocument.get().matches(content.pages())) return cached;
        var document = new ClipboardDocumentSnapshot(content.pages(), registries);
        cached = new Result(document.isEmpty() ? Status.EMPTY : Status.READY, Optional.of(document));
        return cached;
    }

    public void clear() { cached = Result.unavailable(); }
}
