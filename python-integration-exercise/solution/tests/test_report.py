from pathlib import Path

from invoicing.models import Invoice, LineItem
from invoicing.report import format_report, summarize
from invoicing.store import load_invoices

DATA = Path(__file__).parent.parent.parent / "data" / "invoices.json"


def test_line_item_and_invoice_totals():
    inv = Invoice("in_x", "cus_x", "usd", "paid", [LineItem("a", 100, 3), LineItem("b", 50)])
    assert inv.total == 350


def test_summarize_only_counts_paid():
    s = summarize(load_invoices(DATA))
    assert s["paid_count"] == 4
    assert s["total_by_currency"] == {"usd": 9800, "inr": 415000, "jpy": 22500, "gbp": 2370}


def test_format_report():
    out = format_report({"paid_count": 1, "total_by_currency": {"usd": 500}})
    assert out == "Paid invoices: 1\n  USD: 500"
