"""End-to-end check against the running mock server:  python3 solution/run_live.py"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from invoicing.api_client import ApiClient
from invoicing.report import top_customers, total_paid_usd
from invoicing.store import load_invoices

invoices = load_invoices(Path(__file__).parent.parent / "data" / "invoices.json")
client = ApiClient()
print("Total paid (USD cents):", total_paid_usd(invoices, client))
print("Top 2 customers:", top_customers(invoices, client, 2))
