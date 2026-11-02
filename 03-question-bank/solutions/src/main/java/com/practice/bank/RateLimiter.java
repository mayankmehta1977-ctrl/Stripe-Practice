package com.practice.bank;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Problem 3: rate limiter. Times are in seconds.
 * Part 1: fixed window - at most `limit` requests per client per window.
 *         Simple, but allows a burst of 2x limit across a window boundary.
 * Part 2: sliding window using a queue of timestamps per client.
 * Part 3: per-client limits (e.g. paid plans), with a default.
 */
public final class RateLimiter {
    private RateLimiter() {}

    public static class FixedWindow {
        private final int limit;
        private final long window;
        private final Map<String, long[]> state = new HashMap<>();   // client -> {windowStart, count}

        public FixedWindow(int limit, long windowSeconds) {
            this.limit = limit;
            this.window = windowSeconds;
        }

        public boolean allow(String client, long now) {
            long start = now - Math.floorMod(now, window);
            long[] s = state.computeIfAbsent(client, k -> new long[]{start, 0});
            if (s[0] != start) {
                s[0] = start;
                s[1] = 0;
            }
            if (s[1] >= limit) return false;
            s[1]++;
            return true;
        }
    }

    public static class SlidingWindow {
        private final long window;
        private final Map<String, Integer> limits;
        private final int defaultLimit;
        private final Map<String, Deque<Long>> log = new HashMap<>();

        public SlidingWindow(long windowSeconds, Map<String, Integer> limits, int defaultLimit) {
            this.window = windowSeconds;
            this.limits = Map.copyOf(limits);
            this.defaultLimit = defaultLimit;
        }

        public boolean allow(String client, long now) {
            Deque<Long> q = log.computeIfAbsent(client, k -> new ArrayDeque<>());
            while (!q.isEmpty() && q.peekFirst() <= now - window) q.pollFirst();
            if (q.size() >= limits.getOrDefault(client, defaultLimit)) return false;
            q.addLast(now);
            return true;
        }
    }
}
