package dev.clipboardhud.client.reading;

import java.util.LinkedHashMap;

/** Bounded, session-only personal positions. Keys are observed client block-entity identities. */
public final class PersonalPagePositions<K> {
    private final LinkedHashMap<K, Integer> positions = new LinkedHashMap<>(16, .75f, true);
    private final int capacity;
    public PersonalPagePositions() { this(256); }
    PersonalPagePositions(int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("capacity must be positive");
        this.capacity = capacity;
    }
    public static int clamp(int page, int pageCount) {
        return pageCount <= 0 ? 0 : Math.max(0, Math.min(page, pageCount - 1));
    }
    public int current(K target, int pageCount) {
        // Preserve a just-closed screen's position while an older server snapshot is still visible.
        return clamp(positions.getOrDefault(target, 0), pageCount);
    }
    public boolean has(K target) { return positions.containsKey(target); }
    public void remember(K target, int page, int pageCount) {
        if (target == null || pageCount <= 0) return;
        positions.put(target, clamp(page, pageCount));
        while (positions.size() > capacity) positions.remove(positions.keySet().iterator().next());
    }
    public void move(K target, int step, int pageCount) {
        if (pageCount <= 1) return;
        long next = (long) current(target, pageCount) + step;
        remember(target, (int) Math.max(0, Math.min(next, pageCount - 1L)), pageCount);
    }
    public void clear() { positions.clear(); }
}
