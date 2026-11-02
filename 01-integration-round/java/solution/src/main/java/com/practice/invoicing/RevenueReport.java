package com.practice.invoicing;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** New reporting for the "Revenue in USD" spec. Leaves Report untouched. */
public final class RevenueReport {
    private RevenueReport() {}

    /** Currencies whose amounts are already in whole units (see API.md). */
    static final Set<String> ZERO_DECIMAL_CURRENCIES = Set.of("jpy");

    public record CustomerRevenue(String name, long usdCents) {}

    static double toMajorUnits(long amount, String currency) {
        return ZERO_DECIMAL_CURRENCIES.contains(currency) ? amount : amount / 100.0;
    }

    static long toUsdCents(long amount, String currency, Map<String, Double> rates) {
        Double rate = rates.get(currency.toUpperCase(Locale.ROOT));
        if (rate == null) {
            throw new IllegalArgumentException("no exchange rate for " + currency.toUpperCase(Locale.ROOT));
        }
        return Math.round(toMajorUnits(amount, currency) / rate * 100);
    }

    static Map<String, Long> usdCentsByCustomer(List<Invoice> invoices, LedgerApi api) {
        Map<String, Double> rates = api.getRates();
        Map<String, Long> totals = new HashMap<>();
        for (Invoice inv : invoices) {
            if (inv.isPaid()) {
                totals.merge(inv.customerId(), toUsdCents(inv.total(), inv.currency(), rates), Long::sum);
            }
        }
        return totals;
    }

    public static long totalPaidUsd(List<Invoice> invoices, LedgerApi api) {
        return usdCentsByCustomer(invoices, api).values().stream().mapToLong(Long::longValue).sum();
    }

    public static List<CustomerRevenue> topCustomers(List<Invoice> invoices, LedgerApi api, int n) {
        Map<String, String> names = api.listCustomers().stream()
                .collect(Collectors.toMap(Customer::id, Customer::name, (a, b) -> a));
        return usdCentsByCustomer(invoices, api).entrySet().stream()
                .sorted(Comparator.comparing((Map.Entry<String, Long> e) -> -e.getValue())
                        .thenComparing(Map.Entry::getKey))
                .limit(n)
                .map(e -> new CustomerRevenue(names.getOrDefault(e.getKey(), e.getKey()), e.getValue()))
                .toList();
    }
}
