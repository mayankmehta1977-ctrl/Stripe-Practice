package com.practice.invoicing;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests for the new code. None of them need the mock server running. */
class RevenueReportTest {
    static final Path DATA = Path.of("..", "data", "invoices.json");
    static final Map<String, Double> RATES =
            Map.of("USD", 1.0, "EUR", 0.92, "GBP", 0.79, "INR", 83.0, "JPY", 150.0);
    static final List<Customer> CUSTOMERS = List.of(
            new Customer("cus_001", "Acme Corp", "US"),
            new Customer("cus_002", "Bharat Textiles", "IN"),
            new Customer("cus_003", "Euro Parts GmbH", "DE"),
            new Customer("cus_004", "Tokyo Gadgets", "JP"),
            new Customer("cus_005", "London Books Ltd", "GB"));

    /** Stands in for ApiClient in report tests: no HTTP at all. */
    static class FakeLedgerApi implements LedgerApi {
        public Map<String, Double> getRates() { return RATES; }
        public List<Customer> listCustomers() { return CUSTOMERS; }
    }

    /** Scripted HTTP responses, recording every request made. */
    static class FakeTransport implements HttpTransport {
        final Deque<Response> responses = new ArrayDeque<>();
        final List<URI> requests = new ArrayList<>();
        final List<Map<String, String>> headers = new ArrayList<>();

        FakeTransport then(int status, String body, Map<String, String> hdrs) {
            responses.add(new Response(status, hdrs, body));
            return this;
        }

        FakeTransport then(int status, String body) {
            return then(status, body, Map.of());
        }

        public Response get(URI uri, Map<String, String> h) {
            requests.add(uri);
            headers.add(h);
            return responses.removeFirst();
        }
    }

    static final String RATES_JSON = "{\"rates\": {\"USD\": 1.0, \"EUR\": 0.92, \"GBP\": 0.79, \"INR\": 83.0, \"JPY\": 150.0}}";
    static final String BUSY = "{\"error\": {\"message\": \"slow down\"}}";

    static ApiClient client(FakeTransport t, List<Long> sleeps) {
        return new ApiClient("http://api", "sk_test", t, sleeps::add);
    }

    // ---- Part 1 -----------------------------------------------------------
    @Test
    void conversionHandlesMinorAndZeroDecimalUnits() {
        assertEquals(10000L, RevenueReport.toUsdCents(9200, "eur", RATES));   // 92.00 EUR -> $100
        assertEquals(10000L, RevenueReport.toUsdCents(15000, "jpy", RATES));  // 15,000 yen -> $100
    }

    @Test
    void unknownCurrencyIsAClearError() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> RevenueReport.toUsdCents(100, "chf", RATES));
        assertTrue(e.getMessage().contains("CHF"));
    }

    @Test
    void totalPaidUsdOnSampleData() throws Exception {
        assertEquals(32800L, RevenueReport.totalPaidUsd(InvoiceStore.load(DATA), new FakeLedgerApi()));
    }

    @Test
    void unpaidInvoicesAreIgnored() {
        List<Invoice> open = List.of(new Invoice("i", "cus_001", "usd", "open", List.of(new LineItem("x", 100, 1))));
        assertEquals(0L, RevenueReport.totalPaidUsd(open, new FakeLedgerApi()));
    }

    @Test
    void clientSendsApiKey() {
        FakeTransport t = new FakeTransport().then(200, RATES_JSON);
        client(t, new ArrayList<>()).getRates();
        assertEquals("Bearer sk_test", t.headers.get(0).get("Authorization"));
    }

    // ---- Part 2 -----------------------------------------------------------
    @Test
    void topCustomers() throws Exception {
        assertEquals(
                List.of(new RevenueReport.CustomerRevenue("Tokyo Gadgets", 15000),
                        new RevenueReport.CustomerRevenue("Acme Corp", 9800)),
                RevenueReport.topCustomers(InvoiceStore.load(DATA), new FakeLedgerApi(), 2));
    }

    @Test
    void listCustomersFollowsPagination() {
        FakeTransport t = new FakeTransport()
                .then(200, "{\"data\": [{\"id\": \"cus_001\", \"name\": \"A\"}, {\"id\": \"cus_002\", \"name\": \"B\"}], \"has_more\": true}")
                .then(200, "{\"data\": [{\"id\": \"cus_003\", \"name\": \"C\"}], \"has_more\": false}");
        List<Customer> all = client(t, new ArrayList<>()).listCustomers();
        assertEquals(3, all.size());
        assertTrue(t.requests.get(1).getQuery().contains("starting_after=cus_002"));
    }

    // ---- Part 3 -----------------------------------------------------------
    @Test
    void retries429HonoringRetryAfter() {
        FakeTransport t = new FakeTransport()
                .then(429, BUSY, Map.of("Retry-After", "2"))
                .then(200, RATES_JSON);
        List<Long> sleeps = new ArrayList<>();
        assertEquals(RATES, client(t, sleeps).getRates());
        assertEquals(List.of(2000L), sleeps);
        assertEquals(2, t.requests.size());
    }

    @Test
    void givesUpAfterThreeAttempts() {
        FakeTransport t = new FakeTransport()
                .then(429, BUSY, Map.of("Retry-After", "0"))
                .then(429, BUSY, Map.of("Retry-After", "0"))
                .then(429, BUSY, Map.of("Retry-After", "0"));
        ApiException e = assertThrows(ApiException.class, () -> client(t, new ArrayList<>()).getRates());
        assertEquals(429, e.status());
        assertEquals(3, t.requests.size());
    }

    @Test
    void otherErrorsRaiseWithServerMessage() {
        FakeTransport t = new FakeTransport().then(401, "{\"error\": {\"message\": \"Invalid API key\"}}");
        ApiException e = assertThrows(ApiException.class, () -> client(t, new ArrayList<>()).getRates());
        assertTrue(e.getMessage().contains("Invalid API key"));
    }

    @Test
    void ratesAreCached() {
        FakeTransport t = new FakeTransport().then(200, RATES_JSON);
        ApiClient c = client(t, new ArrayList<>());
        c.getRates();
        c.getRates();
        assertEquals(1, t.requests.size());
    }
}
