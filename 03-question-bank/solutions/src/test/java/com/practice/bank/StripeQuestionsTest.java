package com.practice.bank;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StripeQuestionsTest {

    static BigDecimal bd(String s) { return new BigDecimal(s); }

    static void assertMoney(String expected, Optional<BigDecimal> actual) {
        assertEquals(0, bd(expected).compareTo(actual.orElseThrow().setScale(6, java.math.RoundingMode.HALF_UP)),
                "expected " + expected + " got " + actual);
    }

    // ---- 1. Currency conversion ----
    @Test
    void currencyDirectInverseOneHopAndBest() {
        CurrencyConverter c = new CurrencyConverter("USD:CAD:DHL:1.25,USD:GBP:FEDX:0.8,CAD:EUR:UPS:0.7");
        assertMoney("125", c.direct(bd("100"), "USD", "CAD"));
        assertMoney("100", c.direct(bd("125"), "CAD", "USD"));          // inverse rate
        assertTrue(c.direct(bd("100"), "USD", "EUR").isEmpty());
        assertMoney("87.5", c.withOneHop(bd("100"), "USD", "EUR"));
        CurrencyConverter.Conversion best = c.best(bd("100"), "GBP", "EUR").orElseThrow();
        assertEquals(List.of("GBP", "USD", "CAD", "EUR"), best.path());
        assertEquals(0, bd("109.375").compareTo(best.amount().setScale(3, java.math.RoundingMode.HALF_UP)));
        assertThrows(IllegalArgumentException.class, () -> new CurrencyConverter("USD:CAD:5"));
        assertThrows(IllegalArgumentException.class, () -> new CurrencyConverter("USD:CAD:X:0"));
    }

    // ---- 2. Shipping cost ----
    @Test
    void shippingFlatAndTiered() {
        List<ShippingCost.Item> order = List.of(new ShippingCost.Item("mouse", 20), new ShippingCost.Item("laptop", 5));
        Map<String, Map<String, Long>> flat = Map.of("US", Map.of("mouse", 550L, "laptop", 1000L));
        assertEquals(20 * 550 + 5 * 1000, ShippingCost.flat("US", order, flat));

        Map<String, Map<String, List<ShippingCost.Tier>>> tiered = Map.of("US", Map.of(
                "mouse", List.of(new ShippingCost.Tier(0, null, 550)),
                "laptop", List.of(new ShippingCost.Tier(0, 2, 1000), new ShippingCost.Tier(2, null, 900))));
        assertEquals(20 * 550 + 2 * 1000 + 3 * 900, ShippingCost.tiered("US", order, tiered));
        assertThrows(IllegalArgumentException.class, () -> ShippingCost.flat("FR", order, flat));
    }

    // ---- 3. Rate limiter ----
    @Test
    void fixedWindowLimiter() {
        RateLimiter.FixedWindow f = new RateLimiter.FixedWindow(2, 10);
        assertEquals(List.of(true, true, false, true),
                List.of(f.allow("c", 1), f.allow("c", 2), f.allow("c", 3), f.allow("c", 11)));
        assertTrue(f.allow("other", 3));                      // limits are per client
    }

    @Test
    void slidingWindowWithPerClientLimits() {
        RateLimiter.SlidingWindow s = new RateLimiter.SlidingWindow(10, Map.of("pro", 3), 1);
        assertEquals(List.of(true, false, true), List.of(s.allow("free", 0), s.allow("free", 5), s.allow("free", 10)));
        assertEquals(List.of(true, true, true, false),
                List.of(s.allow("pro", 0), s.allow("pro", 1), s.allow("pro", 2), s.allow("pro", 3)));
    }

    // ---- 4. Data table ----
    @Test
    void dataTableFilterAndMultiKeySort() {
        List<Map<String, Object>> rows = List.of(
                Map.of("name", "b", "city", "SF", "age", 30),
                Map.of("name", "a", "city", "SF", "age", 30),
                Map.of("name", "c", "city", "NY", "age", 25));
        assertEquals(List.of("b", "a"), DataTable.query(rows, Map.of("city", "SF"), List.of())
                .stream().map(r -> r.get("name")).toList());
        assertEquals(List.of("a", "b", "c"), DataTable.query(rows, Map.of(),
                        List.of(DataTable.Sort.desc("age"), DataTable.Sort.asc("name")))
                .stream().map(r -> r.get("name")).toList());
    }

    // ---- 5. Split note ----
    @Test
    void splitNoteAddsUpExactly() {
        Map<String, BigDecimal> shares = SplitNote.split("$100.00 dinner split alice,bob,carol");
        Map<String, BigDecimal> expected = new LinkedHashMap<>();
        expected.put("alice", bd("33.34"));
        expected.put("bob", bd("33.33"));
        expected.put("carol", bd("33.33"));
        assertEquals(expected, shares);
        assertEquals(bd("100.00"), shares.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));
        assertThrows(IllegalArgumentException.class, () -> SplitNote.split("dinner split alice"));
        assertThrows(IllegalArgumentException.class, () -> SplitNote.split("$10.001 split a,b"));
    }

    // ---- 6. Nested maps ----
    @Test
    void spendTracker() {
        SpendTracker t = new SpendTracker();
        t.record("u1", "amzn", 50);
        t.record("u1", "uber", 20);
        t.record("u1", "amzn", 10);
        t.record("u2", "uber", 100);
        assertEquals(60, t.total("u1", "amzn"));
        assertEquals(80, t.total("u1"));
        assertEquals(Optional.of("amzn"), t.topMerchant("u1"));
        assertEquals(Optional.empty(), t.topMerchant("nobody"));
        assertEquals("uber", t.topMerchants(1).get(0).getKey());
        assertEquals(120L, (long) t.topMerchants(1).get(0).getValue());
    }

    // ---- 7. Order book ----
    @Test
    void orderBookMatchesBestPriceFirst() {
        OrderBook ob = new OrderBook();
        ob.add(OrderBook.Side.BUY, 100, 5);
        ob.add(OrderBook.Side.BUY, 101, 2);
        ob.add(OrderBook.Side.SELL, 105, 4);
        assertEquals(Optional.of(101L), ob.bestBid());
        assertEquals(Optional.of(105L), ob.bestAsk());
        assertEquals(List.of(new OrderBook.Fill(101, 2), new OrderBook.Fill(100, 2)),
                ob.add(OrderBook.Side.SELL, 100, 4));
        assertEquals(Optional.of(100L), ob.bestBid());      // 3 left at 100
    }

    // ---- 8. Payments API ----
    @Test
    void paymentsIdempotencyStateMachineAndRefunds() {
        PaymentService ps = new PaymentService();
        PaymentService.Payment p = ps.create(1000, "USD", "key-1");
        assertTrue(p == ps.create(1000, "usd", "key-1"));    // safe retry, same object
        assertThrows(PaymentService.PaymentException.class, () -> ps.create(999, "usd", "key-1"));
        ps.authorize(p.id);
        ps.capture(p.id);
        ps.refund(p.id, 400);
        assertThrows(PaymentService.PaymentException.class, () -> ps.refund(p.id, 700));
        ps.refund(p.id, 600);
        assertEquals(PaymentService.Status.REFUNDED, p.status());
        PaymentService.Payment q = ps.create(500, "usd", "key-2");
        assertThrows(PaymentService.PaymentException.class, () -> ps.capture(q.id));   // must authorize first
    }
}
