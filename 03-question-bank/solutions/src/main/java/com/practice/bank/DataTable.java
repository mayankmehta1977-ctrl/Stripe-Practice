package com.practice.bank;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Problem 4: search and sort a data table of rows (column -> value).
 * Part 1: filter rows where every given column equals the given value.
 * Part 2: sort by several columns, each ascending or descending.
 */
public final class DataTable {
    private DataTable() {}

    public record Sort(String column, boolean descending) {
        public static Sort asc(String c) { return new Sort(c, false); }
        public static Sort desc(String c) { return new Sort(c, true); }
    }

    public static List<Map<String, Object>> query(List<Map<String, Object>> rows,
                                                  Map<String, Object> where, List<Sort> orderBy) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            boolean match = where.entrySet().stream().allMatch(e -> Objects.equals(r.get(e.getKey()), e.getValue()));
            if (match) out.add(r);
        }
        Comparator<Map<String, Object>> cmp = null;
        for (Sort s : orderBy) {
            Comparator<Map<String, Object>> c = Comparator.comparing(
                    r -> cast(r.get(s.column())),
                    Comparator.nullsLast(Comparator.naturalOrder()));
            if (s.descending()) c = c.reversed();
            cmp = cmp == null ? c : cmp.thenComparing(c);
        }
        if (cmp != null) out.sort(cmp);       // List.sort is stable: ties keep input order
        return out;
    }

    @SuppressWarnings("unchecked")
    private static Comparable<Object> cast(Object v) {
        return (Comparable<Object>) v;
    }
}
