package dev.clipboardhud.create;

import com.simibubi.create.content.equipment.clipboard.ClipboardEntry;
import java.util.ArrayList;
import java.util.List;

/** Preserves original page and entry order, including empty pages. No shared reading position. */
public final class ClipboardDocumentSnapshot {
    private final List<List<ClipboardEntrySnapshot>> pages;

    public ClipboardDocumentSnapshot(List<List<ClipboardEntry>> source) {
        var copiedPages = new ArrayList<List<ClipboardEntrySnapshot>>(source.size());
        for (var page : source) {
            var entries = new ArrayList<ClipboardEntrySnapshot>(page.size());
            for (var entry : page) entries.add(new ClipboardEntrySnapshot(entry));
            copiedPages.add(List.copyOf(entries));
        }
        pages = List.copyOf(copiedPages);
    }

    public List<List<ClipboardEntrySnapshot>> pages() { return pages; }
    public boolean isEmpty() { return pages.stream().allMatch(List::isEmpty); }
}
