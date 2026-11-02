package com.practice.bank;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Asked at Visa: merge overlapping intervals (LeetCode 56).
 * Key idea: sort by start. Then any overlap is with the last merged interval.
 */
public final class MergeIntervals {
    private MergeIntervals() {}

    public static List<int[]> merge(List<int[]> intervals) {
        List<int[]> sorted = new ArrayList<>(intervals);
        sorted.sort(Comparator.comparingInt(a -> a[0]));
        List<int[]> merged = new ArrayList<>();
        for (int[] cur : sorted) {
            if (!merged.isEmpty() && cur[0] <= merged.get(merged.size() - 1)[1]) {
                int[] last = merged.get(merged.size() - 1);
                last[1] = Math.max(last[1], cur[1]);
            } else {
                merged.add(new int[]{cur[0], cur[1]});        // copy: don't mutate the caller's arrays
            }
        }
        return merged;
    }
}
