package operation;

import java.util.List;

/** A bounded slice of a list and its one-based position. */
record Page<T>(List<T> items, int currentPage, int totalPages) {
    private static final int SIZE = 10;

    /** Returns an immutable page of up to ten items. */
    static <T> Page<T> slice(List<T> items, int requestedPage) {
        int totalPages = items.isEmpty() ? 0 : (items.size() - 1) / SIZE + 1;
        int currentPage = Math.max(1, Math.min(requestedPage, totalPages));
        int from = (currentPage - 1) * SIZE;
        int to = from + Math.min(SIZE, items.size() - from);
        return new Page<>(List.copyOf(items.subList(from, to)), currentPage, totalPages);
    }
}
