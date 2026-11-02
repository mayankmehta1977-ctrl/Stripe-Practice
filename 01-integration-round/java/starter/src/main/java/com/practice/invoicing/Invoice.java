package com.practice.invoicing;

import java.util.List;

/** currency is a lowercase ISO code, e.g. "usd". status is draft | open | paid | void. */
public record Invoice(String id, String customerId, String currency, String status, List<LineItem> lineItems) {
    public long total() {
        return lineItems.stream().mapToLong(LineItem::amount).sum();
    }

    public boolean isPaid() {
        return "paid".equals(status);
    }
}
