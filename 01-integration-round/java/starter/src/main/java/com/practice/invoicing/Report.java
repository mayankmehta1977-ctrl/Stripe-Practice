package com.practice.invoicing;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Used by the finance dashboard. Do not change the output of these methods. */
public final class Report {
    private Report() {}

    public record Summary(int paidCount, Map<String, Long> totalByCurrency) {}

    /** Paid revenue grouped by currency, in minor units. */
    public static Summary summarize(List<Invoice> invoices) {
        Map<String, Long> totals = new TreeMap<>();
        int paid = 0;
        for (Invoice inv : invoices) {
            if (inv.isPaid()) {
                paid++;
                totals.merge(inv.currency(), inv.total(), Long::sum);
            }
        }
        return new Summary(paid, totals);
    }

    public static String formatReport(Summary summary) {
        StringBuilder sb = new StringBuilder("Paid invoices: " + summary.paidCount());
        new TreeMap<>(summary.totalByCurrency())
                .forEach((currency, amount) -> sb.append("\n  ").append(currency.toUpperCase()).append(": ").append(amount));
        return sb.toString();
    }
}
