package dev.clipboardhud.client.reading;

import dev.clipboardhud.create.ClipboardDocumentSnapshot;
import dev.clipboardhud.create.ClipboardEntrySnapshot;
import java.util.List;

/** One original page; clipping and line wrapping never create extra document pages. */
public record ClipboardPageView(int index, int pageCount, List<ClipboardEntrySnapshot> entries) {
    public static ClipboardPageView select(ClipboardDocumentSnapshot document, int requestedPage) {
        int count = document == null ? 0 : document.pages().size();
        int index = PersonalPagePositions.clamp(requestedPage, count);
        return new ClipboardPageView(index, count, count == 0 ? List.of() : document.pages().get(index));
    }
}
