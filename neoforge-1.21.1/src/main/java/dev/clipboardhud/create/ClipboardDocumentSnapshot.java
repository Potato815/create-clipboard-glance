package dev.clipboardhud.create;

import com.simibubi.create.content.equipment.clipboard.ClipboardEntry;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;

/** Preserves original page and entry order, including empty pages. No shared reading position. */
public final class ClipboardDocumentSnapshot {
    private final List<List<ClipboardEntrySnapshot>> pages;

    public ClipboardDocumentSnapshot(List<List<ClipboardEntry>> source, HolderLookup.Provider registries) {
        var copiedPages = new ArrayList<List<ClipboardEntrySnapshot>>(source.size());
        for (var page : source) {
            var entries = new ArrayList<ClipboardEntrySnapshot>(page.size());
            for (var entry : page) entries.add(new ClipboardEntrySnapshot(entry, registries));
            copiedPages.add(List.copyOf(entries));
        }
        pages = List.copyOf(copiedPages);
    }

    public List<List<ClipboardEntrySnapshot>> pages() { return pages; }
    public boolean isEmpty() { return pages.stream().allMatch(List::isEmpty); }

    public boolean matches(List<List<ClipboardEntry>> source) {
        if (source.size() != pages.size()) return false;
        for (int page = 0; page < pages.size(); page++) {
            var saved = pages.get(page);
            var current = source.get(page);
            if (saved.size() != current.size()) return false;
            for (int entry = 0; entry < saved.size(); entry++) {
                if (!saved.get(entry).matches(current.get(entry))) return false;
            }
        }
        return true;
    }
}
