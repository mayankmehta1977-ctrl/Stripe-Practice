# Spec: Revenue in USD

Finance wants one number: total **paid** revenue converted to US dollars, plus
a list of the top customers by that revenue. Exchange rates and customer names
come from the Ledger API (see `API.md`).

Constraints:
- Do not change the behavior or output of the existing `Report.summarize()`
  and `Report.formatReport()`; the finance dashboard depends on them.
- Use `java.net.http.HttpClient` for HTTP and Gson for JSON.
- Unit tests must not need the mock server to be running.

## Part 1: API client and USD total (~20 min)

1. Add an `ApiClient` class with a method `Map<String, Double> getRates()`.
   It must send the API key as described in `API.md`.
2. Add `long totalPaidUsd(List<Invoice> invoices, ...)` returning paid revenue
   converted to **US cents**, rounded to the nearest cent.
3. Add tests.

## Part 2: Top customers (~20 min)

1. Add `ApiClient.listCustomers()` returning **all** customers.
2. Add `topCustomers(invoices, ..., int n)` returning customer name and USD
   cents for the `n` highest-revenue customers, highest first.
3. Add tests.

## Part 3: Reliability (~15 min)

1. The API sometimes returns HTTP 429. Retry those requests, honoring the
   `Retry-After` header, up to 3 attempts in total. Other errors should
   throw a clear exception.
2. Calling `getRates()` repeatedly on the same client should only hit the
   API once.
3. Add tests.

## Expected results against the sample data

- total paid in USD = **32800** cents ($328.00)
- top 2 customers = **Tokyo Gadgets** (15000), **Acme Corp** (9800)

If your total comes out as 17950, re-read the last section of `API.md`.
