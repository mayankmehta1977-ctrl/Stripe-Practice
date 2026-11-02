package com.practice.bank;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Problem 5: split a transaction note such as "$100.00 dinner split alice,bob,carol".
 * Amounts are split in cents; leftover cents go one each to the first people,
 * so the shares always add up exactly to the total. Never use double for money.
 */
public final class SplitNote {
    private SplitNote() {}

    public static Map<String, BigDecimal> split(String note) {
        String[] tokens = note.trim().split("\\s+");
        BigDecimal total = null;
        List<String> people = new ArrayList<>();
        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i].startsWith("$") && total == null) {
                total = new BigDecimal(tokens[i].substring(1).replace(",", ""));
            }
            if (tokens[i].equalsIgnoreCase("split") && i + 1 < tokens.length) {
                for (String p : tokens[i + 1].split(",")) {
                    if (!p.isBlank()) people.add(p.trim());
                }
            }
        }
        if (total == null) throw new IllegalArgumentException("no $amount in note");
        if (people.isEmpty()) throw new IllegalArgumentException("no people to split between");
        if (total.scale() > 2) throw new IllegalArgumentException("amount has fractions of a cent");

        long cents = total.movePointRight(2).longValueExact();
        long base = cents / people.size(), extra = cents % people.size();
        Map<String, BigDecimal> shares = new LinkedHashMap<>();
        for (int i = 0; i < people.size(); i++) {
            long share = base + (i < extra ? 1 : 0);
            shares.merge(people.get(i), BigDecimal.valueOf(share, 2), BigDecimal::add);
        }
        return shares;
    }
}
