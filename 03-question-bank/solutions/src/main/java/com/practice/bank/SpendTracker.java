package com.practice.bank;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Problem 6: a hash map of hash maps: user -> merchant -> total cents.
 * Part 1: record spend; total for a user (optionally one merchant).
 * Part 2: a user's top merchant (ties: alphabetical).
 * Part 3: top-k merchants across all users.
 */
public class SpendTracker {
    private final Map<String, Map<String, Long>> spend = new HashMap<>();

    public void record(String user, String merchant, long cents) {
        spend.computeIfAbsent(user, k -> new HashMap<>()).merge(merchant, cents, Long::sum);
    }

    public long total(String user) {
        return spend.getOrDefault(user, Map.of()).values().stream().mapToLong(Long::longValue).sum();
    }

    public long total(String user, String merchant) {
        return spend.getOrDefault(user, Map.of()).getOrDefault(merchant, 0L);
    }

    public Optional<String> topMerchant(String user) {
        return spend.getOrDefault(user, Map.of()).entrySet().stream()
                .min(byAmountDescThenName())
                .map(Map.Entry::getKey);
    }

    public List<Map.Entry<String, Long>> topMerchants(int k) {
        Map<String, Long> totals = new HashMap<>();
        spend.values().forEach(m -> m.forEach((merchant, c) -> totals.merge(merchant, c, Long::sum)));
        return totals.entrySet().stream().sorted(byAmountDescThenName()).limit(k).toList();
    }

    private static Comparator<Map.Entry<String, Long>> byAmountDescThenName() {
        return Comparator.<Map.Entry<String, Long>>comparingLong(e -> -e.getValue())
                .thenComparing(Map.Entry::getKey);
    }
}
