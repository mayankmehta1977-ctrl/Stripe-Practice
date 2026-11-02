package com.practice.bank;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Problem 1: currency conversion. Input "USD:CAD:DHL:1.25,USD:GBP:FEDX:0.8"
 * means 1 USD buys 1.25 CAD via provider DHL.
 * Part 1: parse; convert with a direct rate (either direction).
 * Part 2: allow one intermediate currency; return the best result.
 * Part 3: any number of hops; return the best amount and the path.
 */
public class CurrencyConverter {

    public record Edge(String to, BigDecimal rate, String provider) {}

    public record Conversion(BigDecimal amount, List<String> path) {}

    private static final MathContext MC = MathContext.DECIMAL64;
    private final Map<String, List<Edge>> graph = new LinkedHashMap<>();

    public CurrencyConverter(String input) {
        for (String raw : input.split(",")) {
            String entry = raw.trim();
            if (entry.isEmpty()) continue;
            String[] p = entry.split(":");
            if (p.length != 4) throw new IllegalArgumentException("bad entry: '" + entry + "'");
            BigDecimal rate;
            try {
                rate = new BigDecimal(p[3].trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("bad rate in '" + entry + "'");
            }
            if (rate.signum() <= 0) throw new IllegalArgumentException("rate must be positive: '" + entry + "'");
            String from = p[0].trim().toUpperCase(), to = p[1].trim().toUpperCase();
            add(from, new Edge(to, rate, p[2].trim()));
            add(to, new Edge(from, BigDecimal.ONE.divide(rate, MC), p[2].trim()));   // inverse
        }
    }

    private void add(String from, Edge e) {
        graph.computeIfAbsent(from, k -> new ArrayList<>()).add(e);
    }

    /** Part 1 */
    public Optional<BigDecimal> direct(BigDecimal amount, String from, String to) {
        return graph.getOrDefault(from, List.of()).stream()
                .filter(e -> e.to().equals(to))
                .map(e -> amount.multiply(e.rate(), MC))
                .max(BigDecimal::compareTo);
    }

    /** Part 2 */
    public Optional<BigDecimal> withOneHop(BigDecimal amount, String from, String to) {
        Optional<BigDecimal> best = direct(amount, from, to);
        for (Edge first : graph.getOrDefault(from, List.of())) {
            if (first.to().equals(to)) continue;
            Optional<BigDecimal> viaMid = direct(amount.multiply(first.rate(), MC), first.to(), to);
            if (viaMid.isPresent() && (best.isEmpty() || viaMid.get().compareTo(best.get()) > 0)) {
                best = viaMid;
            }
        }
        return best;
    }

    /**
     * Part 3: explore simple paths (no repeated currency) and keep the best.
     * Exponential in the worst case, fine for interview-sized inputs; say so.
     */
    public Optional<Conversion> best(BigDecimal amount, String from, String to) {
        Conversion best = null;
        Deque<Object[]> stack = new ArrayDeque<>();
        stack.push(new Object[]{from, amount, List.of(from)});
        while (!stack.isEmpty()) {
            Object[] s = stack.pop();
            String cur = (String) s[0];
            BigDecimal val = (BigDecimal) s[1];
            @SuppressWarnings("unchecked") List<String> path = (List<String>) s[2];
            if (cur.equals(to) && path.size() > 1) {
                if (best == null || val.compareTo(best.amount()) > 0) best = new Conversion(val, path);
                continue;
            }
            Set<String> seen = new HashSet<>(path);
            for (Edge e : graph.getOrDefault(cur, List.of())) {
                if (seen.contains(e.to())) continue;
                List<String> next = new ArrayList<>(path);
                next.add(e.to());
                stack.push(new Object[]{e.to(), val.multiply(e.rate(), MC), next});
            }
        }
        return Optional.ofNullable(best);
    }
}
