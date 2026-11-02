"""Local mock of the "Ledger API" used in this exercise.

Run it in a separate terminal before you start:
    python3 mock_api/server.py
It listens on http://localhost:8765. See API.md for the endpoints.
"""
import json
from http.server import BaseHTTPRequestHandler, HTTPServer
from urllib.parse import urlparse, parse_qs

API_KEY = "sk_test_practice"

# 1 USD = <rate> units of the currency
RATES = {"USD": 1.0, "EUR": 0.92, "GBP": 0.79, "INR": 83.0, "JPY": 150.0}

CUSTOMERS = [
    {"id": "cus_001", "name": "Acme Corp", "country": "US"},
    {"id": "cus_002", "name": "Bharat Textiles", "country": "IN"},
    {"id": "cus_003", "name": "Euro Parts GmbH", "country": "DE"},
    {"id": "cus_004", "name": "Tokyo Gadgets", "country": "JP"},
    {"id": "cus_005", "name": "London Books Ltd", "country": "GB"},
]

REQUEST_COUNT = {"n": 0}


class Handler(BaseHTTPRequestHandler):
    def _send(self, status, body, headers=None):
        payload = json.dumps(body).encode()
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        for k, v in (headers or {}).items():
            self.send_header(k, v)
        self.end_headers()
        self.wfile.write(payload)

    def log_message(self, *args):  # keep the terminal quiet
        pass

    def do_GET(self):
        REQUEST_COUNT["n"] += 1
        if self.headers.get("Authorization") != f"Bearer {API_KEY}":
            return self._send(401, {"error": {"type": "authentication_error", "message": "Invalid API key"}})
        # Every 4th request is rate limited, to exercise retry logic (Part 3).
        if REQUEST_COUNT["n"] % 4 == 0:
            return self._send(429, {"error": {"type": "rate_limit_error", "message": "Too many requests"}},
                              {"Retry-After": "1"})

        url = urlparse(self.path)
        q = {k: v[0] for k, v in parse_qs(url.query).items()}

        if url.path == "/v1/rates":
            base = q.get("base", "USD").upper()
            if base != "USD":
                return self._send(400, {"error": {"type": "invalid_request_error", "message": "Only base=USD is supported"}})
            return self._send(200, {"object": "rates", "base": "USD", "rates": RATES})

        if url.path == "/v1/customers":
            limit = min(int(q.get("limit", 2)), 2)  # server caps page size at 2
            start = 0
            if "starting_after" in q:
                ids = [c["id"] for c in CUSTOMERS]
                if q["starting_after"] not in ids:
                    return self._send(400, {"error": {"type": "invalid_request_error", "message": "Unknown cursor"}})
                start = ids.index(q["starting_after"]) + 1
            page = CUSTOMERS[start:start + limit]
            return self._send(200, {"object": "list", "data": page, "has_more": start + limit < len(CUSTOMERS)})

        return self._send(404, {"error": {"type": "invalid_request_error", "message": "Not found"}})


if __name__ == "__main__":
    print("Ledger API mock running on http://localhost:8765  (Ctrl+C to stop)")
    HTTPServer(("localhost", 8765), Handler).serve_forever()
