import json
from pathlib import Path

from .models import Invoice, LineItem


def load_invoices(path: str | Path) -> list[Invoice]:
    with open(path) as f:
        raw = json.load(f)
    return [
        Invoice(
            id=r["id"],
            customer_id=r["customer_id"],
            currency=r["currency"].lower(),
            status=r["status"],
            line_items=[LineItem(**li) for li in r["line_items"]],
        )
        for r in raw
    ]
