package com.practice.bank;

import java.util.List;
import java.util.Map;

/**
 * Problem 2: shipping cost. Costs are in cents per unit.
 * Part 1: flat cost per unit by country and product.
 * Part 2: tiered cost: the per-unit price depends on how many units fall in
 *         each quantity band. Part 1 is the special case of a single band.
 */
public final class ShippingCost {
    private ShippingCost() {}

    public record Item(String product, int quantity) {}

    /** Units in [minQty, maxQty) cost unitCents each; maxQty null = no upper bound. */
    public record Tier(int minQty, Integer maxQty, long unitCents) {}

    public static long flat(String country, List<Item> items, Map<String, Map<String, Long>> rates) {
        Map<String, Long> byProduct = lookup(rates, country, "country");
        long total = 0;
        for (Item i : items) {
            total += lookup(byProduct, i.product(), "product") * i.quantity();
        }
        return total;
    }

    public static long tiered(String country, List<Item> items, Map<String, Map<String, List<Tier>>> rates) {
        Map<String, List<Tier>> byProduct = lookup(rates, country, "country");
        long total = 0;
        for (Item i : items) {
            total += tierCost(lookup(byProduct, i.product(), "product"), i.quantity());
        }
        return total;
    }

    static long tierCost(List<Tier> tiers, int qty) {
        if (qty < 0) throw new IllegalArgumentException("negative quantity");
        long total = 0;
        for (Tier t : tiers) {
            if (qty <= t.minQty()) break;
            int upper = t.maxQty() == null ? qty : Math.min(qty, t.maxQty());
            total += (long) (upper - t.minQty()) * t.unitCents();
        }
        return total;
    }

    private static <V> V lookup(Map<String, V> map, String key, String what) {
        V v = map.get(key);
        if (v == null) throw new IllegalArgumentException("unknown " + what + ": " + key);
        return v;
    }
}
