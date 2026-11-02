"""Client for the Ledger API (reference solution)."""
import time

import requests

from . import config


class ApiError(Exception):
    """Raised for any non-retryable API failure, with the server's message."""

    def __init__(self, status: int, message: str):
        super().__init__(f"Ledger API error {status}: {message}")
        self.status = status


class ApiClient:
    MAX_ATTEMPTS = 3

    def __init__(self, base_url: str = config.API_BASE, api_key: str = config.API_KEY,
                 session: requests.Session | None = None, sleep=time.sleep):
        self.base_url = base_url.rstrip("/")
        self.session = session or requests.Session()
        self.session.headers["Authorization"] = f"Bearer {api_key}"
        self._sleep = sleep            # injectable so tests don't actually wait
        self._rates: dict[str, float] | None = None

    # -- low-level ----------------------------------------------------------
    def _get(self, path: str, params: dict | None = None) -> dict:
        for attempt in range(1, self.MAX_ATTEMPTS + 1):
            resp = self.session.get(f"{self.base_url}{path}", params=params, timeout=10)
            if resp.status_code == 429 and attempt < self.MAX_ATTEMPTS:
                self._sleep(float(resp.headers.get("Retry-After", 1)))
                continue
            if not resp.ok:
                try:
                    message = resp.json()["error"]["message"]
                except (ValueError, KeyError):
                    message = resp.text
                raise ApiError(resp.status_code, message)
            return resp.json()
        raise AssertionError("unreachable")

    # -- endpoints ----------------------------------------------------------
    def get_rates(self) -> dict[str, float]:
        """USD-based rates, fetched once per client and cached."""
        if self._rates is None:
            self._rates = self._get("/v1/rates", {"base": "USD"})["rates"]
        return self._rates

    def list_customers(self) -> list[dict]:
        """Follow cursor pagination until has_more is false."""
        customers: list[dict] = []
        params: dict = {"limit": 100}
        while True:
            page = self._get("/v1/customers", params)
            customers.extend(page["data"])
            if not page["has_more"] or not page["data"]:
                return customers
            params["starting_after"] = page["data"][-1]["id"]
