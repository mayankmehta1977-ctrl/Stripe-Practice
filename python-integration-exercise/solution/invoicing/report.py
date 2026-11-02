from collections import defaultdict

from .models import Invoice


def summarize(invoices: list[Invoice]) -> dict:
    """Paid revenue grouped by currency, in minor units.
    Used by the finance dashboard - do not change its output format."""
    totals: dict[str, int] = defaultdict(int)
    for inv in invoices:
        if inv.is_paid:
            totals[inv.currency] += inv.total
    return {
        "paid_count": sum(1 for i in invoices if i.is_paid),
        "total_by_currency": dict(totals),
    }


def format_report(summary: dict) -> str:
    lines = [f"Paid invoices: {summary['paid_count']}"]
    for currency, amount in sorted(summary["total_by_currency"].items()):
        lines.append(f"  {currency.upper()}: {amount}")
    return "\n".join(lines)


# --- Added for the USD revenue spec -------------------------------------------

ZERO_DECIMAL_CURRENCIES = {"jpy"}


def to_major_units(amount: int, currency: str) -> float:
    """Minor units -> whole currency units (JPY has no minor unit)."""
    return amount if currency in ZERO_DECIMAL_CURRENCIES else amount / 100


def to_usd_cents(amount: int, currency: str, rates: dict[str, float]) -> int:
    rate = rates.get(currency.upper())
    if rate is None:
        raise ValueError(f"no exchange rate for {currency.upper()}")
    return round(to_major_units(amount, currency) / rate * 100)


def usd_cents_by_customer(invoices: list[Invoice], client) -> dict[str, int]:
    rates = client.get_rates()
    totals: dict[str, int] = defaultdict(int)
    for inv in invoices:
        if inv.is_paid:
            totals[inv.customer_id] += to_usd_cents(inv.total, inv.currency, rates)
    return dict(totals)


def total_paid_usd(invoices: list[Invoice], client) -> int:
    return sum(usd_cents_by_customer(invoices, client).values())


def top_customers(invoices: list[Invoice], client, n: int) -> list[tuple[str, int]]:
    by_customer = usd_cents_by_customer(invoices, client)
    names = {c["id"]: c["name"] for c in client.list_customers()}
    ranked = sorted(by_customer.items(), key=lambda kv: (-kv[1], kv[0]))[:n]
    return [(names.get(cid, cid), cents) for cid, cents in ranked]
