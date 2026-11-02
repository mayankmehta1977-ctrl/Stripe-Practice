package com.practice.ai.shipping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Reference solution for the progressive shipping-routes problem.
 * Input: "US:UK:UPS:5,US:CA:FedEx:3,CA:UK:DHL:7"  (from:to:carrier:cost)
 *
 * Part 1: cost of a direct shipment.
 * Part 2: cheapest route with at most one intermediate stop.
 * Part 3: cheapest route with any number of stops.
 * Part 4: cheapest route avoiding some carriers, with at most K legs.
 */
public class ShippingNetwork {

    public record Leg(String from, String to, String carrier, int cost) {}

    public record Route(List<Leg> legs) {
        public int cost() {
            return legs.stream().mapToInt(Leg::cost).sum();
        }

        /** e.g. "US -(FedEx)-> CA -(DHL)-> UK" */
        @Override
        public String toString() {
            if (legs.isEmpty()) return "(empty)";
            StringBuilder sb = new StringBuilder(legs.get(0).from());
            legs.forEach(l -> sb.append(" -(").append(l.carrier()).append(")-> ").append(l.to()));
            return sb + " = " + cost();
        }
    }

    /** Adjacency list: country -> outgoing legs. Shipping lanes are one-directional. */
    private final Map<String, List<Leg>> lanes = new LinkedHashMap<>();

    public ShippingNetwork(String input) {
        for (String raw : input.split(",")) {
            String entry = raw.trim();
            if (entry.isEmpty()) continue;
            String[] p = entry.split(":");
            if (p.length != 4) throw new IllegalArgumentException("bad entry: '" + entry + "'");
            int cost;
            try {
                cost = Integer.parseInt(p[3].trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("bad cost in '" + entry + "'");
            }
            if (cost < 0) throw new IllegalArgumentException("negative cost in '" + entry + "'");
            lanes.computeIfAbsent(p[0].trim(), k -> new ArrayList<>())
                 .add(new Leg(p[0].trim(), p[1].trim(), p[2].trim(), cost));
        }
    }

    // ---- Part 1 ----------------------------------------------------------
    /** Cheapest direct leg (there may be several carriers on one lane). */
    public Optional<Route> direct(String from, String to) {
        return lanes.getOrDefault(from, List.of()).stream()
                .filter(l -> l.to().equals(to))
                .min((a, b) -> Integer.compare(a.cost(), b.cost()))
                .map(l -> new Route(List.of(l)));
    }

    // ---- Part 2 ----------------------------------------------------------
    public Optional<Route> withAtMostOneStop(String from, String to) {
        Optional<Route> best = direct(from, to);
        for (Leg first : lanes.getOrDefault(from, List.of())) {
            if (first.to().equals(to) || first.to().equals(from)) continue;
            Optional<Route> second = direct(first.to(), to);
            if (second.isPresent()) {
                Route candidate = new Route(List.of(first, second.get().legs().get(0)));
                if (best.isEmpty() || candidate.cost() < best.get().cost()) best = Optional.of(candidate);
            }
        }
        return best;
    }

    // ---- Part 3 ----------------------------------------------------------
    /** Dijkstra: valid because costs are non-negative (checked when parsing). */
    public Optional<Route> cheapest(String from, String to) {
        if (from.equals(to)) return Optional.of(new Route(List.of()));
        Map<String, Integer> dist = new HashMap<>();
        Map<String, Leg> via = new HashMap<>();
        PriorityQueue<Map.Entry<String, Integer>> pq =
                new PriorityQueue<>(Map.Entry.comparingByValue());
        dist.put(from, 0);
        pq.add(Map.entry(from, 0));
        while (!pq.isEmpty()) {
            Map.Entry<String, Integer> top = pq.poll();
            String node = top.getKey();
            if (top.getValue() > dist.get(node)) continue;       // stale queue entry
            if (node.equals(to)) break;
            for (Leg leg : lanes.getOrDefault(node, List.of())) {
                int next = top.getValue() + leg.cost();
                if (next < dist.getOrDefault(leg.to(), Integer.MAX_VALUE)) {
                    dist.put(leg.to(), next);
                    via.put(leg.to(), leg);
                    pq.add(Map.entry(leg.to(), next));
                }
            }
        }
        if (!via.containsKey(to)) return Optional.empty();
        List<Leg> path = new ArrayList<>();
        for (String at = to; !at.equals(from); at = via.get(at).from()) path.add(via.get(at));
        Collections.reverse(path);
        return Optional.of(new Route(path));
    }

    // ---- Part 4 ----------------------------------------------------------
    /**
     * Cheapest route using at most maxLegs legs and none of the excluded carriers.
     * Bellman-Ford limited to maxLegs rounds. Plain Dijkstra with a visited set
     * is WRONG here: the cheapest way to reach a midpoint may use too many legs.
     */
    public Optional<Route> cheapest(String from, String to, int maxLegs, Set<String> excludedCarriers) {
        if (maxLegs < 0) throw new IllegalArgumentException("maxLegs must be >= 0");
        if (from.equals(to)) return Optional.of(new Route(List.of()));
        // best.get(node) = cheapest route to node found so far, using <= round legs
        Map<String, Route> best = new HashMap<>();
        best.put(from, new Route(List.of()));
        for (int round = 0; round < maxLegs; round++) {
            Map<String, Route> next = new HashMap<>(best);     // copy: each round adds at most one leg
            for (Map.Entry<String, Route> e : best.entrySet()) {
                for (Leg leg : lanes.getOrDefault(e.getKey(), List.of())) {
                    if (excludedCarriers.contains(leg.carrier())) continue;
                    int cost = e.getValue().cost() + leg.cost();
                    Route current = next.get(leg.to());
                    if (current == null || cost < current.cost()) {
                        List<Leg> legs = new ArrayList<>(e.getValue().legs());
                        legs.add(leg);
                        next.put(leg.to(), new Route(legs));
                    }
                }
            }
            best = next;
        }
        return Optional.ofNullable(best.get(to));
    }

    public static Set<String> carriers(String... names) {
        return Set.copyOf(Arrays.asList(names));
    }
}
