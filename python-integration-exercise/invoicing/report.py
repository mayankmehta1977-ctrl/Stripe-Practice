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
