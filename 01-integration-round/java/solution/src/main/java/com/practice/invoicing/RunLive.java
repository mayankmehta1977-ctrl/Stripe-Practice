package com.practice.invoicing;

import java.nio.file.Path;
import java.util.List;

/** End-to-end check against the running mock server. */
public class RunLive {
    public static void main(String[] args) throws Exception {
        List<Invoice> invoices = InvoiceStore.load(Path.of("..", "data", "invoices.json"));
        ApiClient client = new ApiClient();
        System.out.println("Total paid (USD cents): " + RevenueReport.totalPaidUsd(invoices, client));
        System.out.println("Top 2 customers: " + RevenueReport.topCustomers(invoices, client, 2));
    }
}
