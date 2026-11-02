package com.practice.invoicing;

import java.util.List;
import java.util.Map;

/**
 * What the reporting code needs from the Ledger API. Depending on this
 * interface (not on ApiClient directly) lets tests pass in a fake.
 */
public interface LedgerApi {
    /** USD-based rates: how many units of each currency one US dollar buys. */
    Map<String, Double> getRates();

    /** Every customer, across all pages. */
    List<Customer> listCustomers();
}
