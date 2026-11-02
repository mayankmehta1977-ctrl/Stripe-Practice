"""Tests for the new code. None of them need the mock server running."""
from pathlib import Path
from unittest.mock import MagicMock

import pytest

from invoicing.api_client import ApiClient, ApiError
from invoicing.models import Invoice, LineItem
from invoicing.report import top_customers, total_paid_usd, to_usd_cents
from invoicing.store import load_invoices

DATA = Path(__file__).parent.parent.parent / "data" / "invoices.json"
RATES = {"USD": 1.0, "EUR": 0.92, "GBP": 0.79, "INR": 83.0, "JPY": 150.0}
CUSTOMERS = [{"id": f"cus_00{i}", "name": n} for i, n in
             enumerate(["Acme Corp", "Bharat Textiles", "Euro Parts GmbH", "Tokyo Gadgets", "London Books Ltd"], 1)]


class FakeClient:
    """Stands in for ApiClient in report tests: no HTTP at all."""
    def get_rates(self):
        return RATES

    def list_customers(self):
        return CUSTOMERS


def response(status, body, headers=None):
    r = MagicMock()
    r.status_code, r.ok, r.headers, r.text = status, status < 400, headers or {}, str(body)
    r.json.return_value = body
    return r


def client_with(*responses):
    session = MagicMock()
    session.headers = {}
    session.get.side_effect = list(responses)
    sleeps = []
    return ApiClient("http://api", "sk_test", session=session, sleep=sleeps.append), session, sleeps


# ---- Part 1 ------------------------------------------------------------------
def test_conversion_handles_minor_and_zero_decimal_units():
    assert to_usd_cents(9200, "eur", RATES) == 10000      # 92.00 EUR -> $100
    assert to_usd_cents(15000, "jpy", RATES) == 10000     # 15,000 yen -> $100


def test_unknown_currency_is_a_clear_error():
    with pytest.raises(ValueError, match="CHF"):
        to_usd_cents(100, "chf", RATES)


def test_total_paid_usd_on_sample_data():
    assert total_paid_usd(load_invoices(DATA), FakeClient()) == 32800


def test_client_sends_api_key():
    client, session, _ = client_with(response(200, {"rates": RATES}))
    client.get_rates()
    assert session.headers["Authorization"] == "Bearer sk_test"


# ---- Part 2 ------------------------------------------------------------------
def test_top_customers():
    assert top_customers(load_invoices(DATA), FakeClient(), 2) == [("Tokyo Gadgets", 15000), ("Acme Corp", 9800)]


def test_list_customers_follows_pagination():
    client, session, _ = client_with(
        response(200, {"data": CUSTOMERS[:2], "has_more": True}),
        response(200, {"data": CUSTOMERS[2:], "has_more": False}),
    )
    assert [c["id"] for c in client.list_customers()] == [c["id"] for c in CUSTOMERS]
    assert session.get.call_args_list[1].kwargs["params"]["starting_after"] == "cus_002"


def test_unpaid_invoices_are_ignored():
    inv = [Invoice("i", "cus_001", "usd", "open", [LineItem("x", 100)])]
    assert total_paid_usd(inv, FakeClient()) == 0


# ---- Part 3 ------------------------------------------------------------------
def test_retries_429_honoring_retry_after():
    client, session, sleeps = client_with(
        response(429, {"error": {"message": "slow down"}}, {"Retry-After": "2"}),
        response(200, {"rates": RATES}),
    )
    assert client.get_rates() == RATES
    assert sleeps == [2.0] and session.get.call_count == 2


def test_gives_up_after_three_attempts():
    busy = lambda: response(429, {"error": {"message": "slow down"}}, {"Retry-After": "0"})
    client, session, _ = client_with(busy(), busy(), busy())
    with pytest.raises(ApiError, match="429"):
        client.get_rates()
    assert session.get.call_count == 3


def test_other_errors_raise_with_server_message():
    client, _, _ = client_with(response(401, {"error": {"message": "Invalid API key"}}))
    with pytest.raises(ApiError, match="Invalid API key"):
        client.get_rates()


def test_rates_are_cached():
    client, session, _ = client_with(response(200, {"rates": RATES}))
    client.get_rates()
    client.get_rates()
    assert session.get.call_count == 1
