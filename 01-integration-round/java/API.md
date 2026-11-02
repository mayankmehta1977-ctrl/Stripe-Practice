# Ledger API reference

Base URL: `http://localhost:8765` (see `Config.java`)

## Authentication

Every request must include the header:

    Authorization: Bearer <API_KEY>

Missing or wrong keys return `401` with an `authentication_error`.

## Errors

Errors return JSON: `{"error": {"type": "...", "message": "..."}}`.

| Status | Meaning |
|---|---|
| 400 | Invalid request (bad parameter or cursor) |
| 401 | Authentication failed |
| 404 | Unknown endpoint |
| 429 | Rate limited. The `Retry-After` header gives seconds to wait. |

## GET /v1/rates

Query parameters: `base` (only `USD` is supported).

```json
{"object": "rates", "base": "USD",
 "rates": {"USD": 1.0, "EUR": 0.92, "GBP": 0.79, "INR": 83.0, "JPY": 150.0}}
```

A rate is how many units of that currency one US dollar buys.
So 92 EUR = 92 / 0.92 = 100 USD.

## GET /v1/customers

Cursor-based pagination.

Query parameters:
- `limit`: page size (the server caps this at 2)
- `starting_after`: the `id` of the last customer on the previous page

```json
{"object": "list",
 "data": [{"id": "cus_001", "name": "Acme Corp", "country": "US"}],
 "has_more": true}
```

Keep requesting with `starting_after` set to the last `id` until
`has_more` is `false`.

## Amounts and minor units

All invoice amounts are integers in the currency's **minor unit**:
cents for USD, EUR and GBP, paise for INR.

**Zero-decimal currencies** have no minor unit, so amounts are already in
whole units. In this system the only zero-decimal currency is **JPY**:
`7500` means 7,500 yen, not 75.00 yen.
