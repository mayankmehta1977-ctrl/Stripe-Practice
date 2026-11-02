package com.practice.invoicing;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReportTest {
    static final Path DATA = Path.of("..", "data", "invoices.json");

    @Test
    void lineItemAndInvoiceTotals() {
        Invoice inv = new Invoice("in_x", "cus_x", "usd", "paid",
                List.of(new LineItem("a", 100, 3), new LineItem("b", 50, 1)));
        assertEquals(350L, inv.total());
    }

    @Test
    void summarizeOnlyCountsPaid() throws Exception {
        Report.Summary s = Report.summarize(InvoiceStore.load(DATA));
        assertEquals(4, s.paidCount());
        assertEquals(Map.of("usd", 9800L, "inr", 415000L, "jpy", 22500L, "gbp", 2370L), s.totalByCurrency());
    }

    @Test
    void formatReport() {
        assertEquals("Paid invoices: 1\n  USD: 500", Report.formatReport(new Report.Summary(1, Map.of("usd", 500L))));
    }
}
