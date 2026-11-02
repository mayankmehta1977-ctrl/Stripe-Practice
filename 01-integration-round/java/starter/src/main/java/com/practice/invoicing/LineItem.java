package com.practice.invoicing;

/** A line on an invoice. unitAmount is in the currency's minor unit (see API.md). */
public record LineItem(String description, long unitAmount, int quantity) {
    public long amount() {
        return unitAmount * quantity;
    }
}
