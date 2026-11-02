"""
Stripe-style practice: worked solutions to the problems candidates reported.

Stripe's exact prompts aren't public. Each problem below is a faithful
reconstruction from the Glassdoor descriptions, written the way Stripe runs
them: one problem, several parts, each part extending the code from the last.

How to practice: read only the docstring of a part, set a timer, write your
own solution, then compare. Aim for ~10-15 minutes per part.

Run all tests:  python3 stripe_practice.py
"""

from __future__ import annotations

import heapq
from collections import defaultdict, deque
from dataclasses import dataclass, field
from decimal import Decimal, ROUND_DOWN
from typing import Optional


# ============================================================================
# 1. CURRENCY CONVERSION  (reported: technical screen)
#    Input like "USD:CAD:DHL:5,USD:GBP:FEDX:10"  ->  from:to:provider:rate
# ============================================================================

@dataclass(frozen=True)
class Rate:
    src: str
    dst: str
    provider: str
    rate: float


def parse_rates(s: str) -> list[Rate]:
    """Part 1a: parse the string. Skip blanks; reject malformed entries."""
    rates = []
    for chunk in s.split(","):
        chunk = chunk.strip()
        if not chunk:
            continue
        parts = chunk.split(":")
        if len(parts) != 4:
            raise ValueError(f"bad entry: {chunk!r}")
        src, dst, provider, rate = parts
        rates.append(Rate(src.upper(), dst.upper(), provider, float(rate)))
    return rates


class Converter:
    def __init__(self, s: str):
        # graph[src] -> list of (dst, rate, provider); add inverse edges too
        self.graph: dict[str, list[tuple[str, float, str]]] = defaultdict(list)
        for r in parse_rates(s):
            self.graph[r.src].append((r.dst, r.rate, r.provider))
            self.graph[r.dst].append((r.src, 1 / r.rate, r.provider))

    def direct(self, amount: float, src: str, dst: str) -> Optional[float]:
        """Part 1b: convert using a single direct rate (either direction)."""
        for nxt, rate, _ in self.graph[src]:
            if nxt == dst:
                return amount * rate
        return None

    def one_hop(self, amount: float, src: str, dst: str) -> Optional[float]:
        """Part 2: allow at most one intermediate currency. Return the best."""
        best = self.direct(amount, src, dst)
        for mid, r1, _ in self.graph[src]:
            for nxt, r2, _ in self.graph[mid]:
                if nxt == dst:
                    val = amount * r1 * r2
                    best = val if best is None else max(best, val)
        return best

    def best_path(self, amount: float, src: str, dst: str):
        """Part 3: any number of hops; return (best amount, route of providers).
        BFS over simple paths; fine for interview-sized graphs."""
        best = (None, [])
        queue = deque([(src, amount, [src], [])])
        while queue:
            cur, val, seen, route = queue.popleft()
            if cur == dst:
                if best[0] is None or val > best[0]:
                    best = (val, route)
                continue
            for nxt, rate, provider in self.graph[cur]:
                if nxt not in seen:
                    queue.append((nxt, val * rate, seen + [nxt], route + [f"{cur}->{nxt} via {provider}"]))
        return best


# ============================================================================
# 2. SERVER LOAD BALANCING  (reported: OA and screen, 2025-2026)
# ============================================================================

class LoadBalancer:
    """Part 1: round robin.
    Part 2: each server has a capacity; pick the least-loaded server with room.
    Part 3: sticky sessions - the same client key goes to the same server
            while it has room.
    Part 4: a server can go down; its requests are reassigned."""

    def __init__(self, capacities: dict[str, int]):
        self.capacity = dict(capacities)
        self.load = {s: 0 for s in capacities}
        self.order = list(capacities)
        self.rr = 0
        self.sticky: dict[str, str] = {}
        self.assignments: dict[str, str] = {}   # request_id -> server

    def round_robin(self) -> str:                         # Part 1
        server = self.order[self.rr % len(self.order)]
        self.rr += 1
        return server

    def _least_loaded(self) -> Optional[str]:             # Part 2
        candidates = [s for s in self.order if self.load[s] < self.capacity[s]]
        if not candidates:
            return None
        return min(candidates, key=lambda s: (self.load[s], self.order.index(s)))

    def assign(self, request_id: str, client: Optional[str] = None) -> Optional[str]:
        server = None
        if client and client in self.sticky:              # Part 3
            s = self.sticky[client]
            if s in self.load and self.load[s] < self.capacity[s]:
                server = s
        if server is None:
            server = self._least_loaded()
        if server is None:
            return None                                   # everything full
        self.load[server] += 1
        self.assignments[request_id] = server
        if client:
            self.sticky[client] = server
        return server

    def finish(self, request_id: str) -> None:
        server = self.assignments.pop(request_id)
        self.load[server] -= 1

    def server_down(self, server: str) -> dict[str, Optional[str]]:  # Part 4
        orphaned = [r for r, s in self.assignments.items() if s == server]
        for r in orphaned:
            del self.assignments[r]
        self.order.remove(server)
        del self.load[server], self.capacity[server]
        self.sticky = {c: s for c, s in self.sticky.items() if s != server}
        return {r: self.assign(r) for r in orphaned}


# ============================================================================
# 3. SHIPPING COST  (reported: onsite, two parts)
# ============================================================================

# Part 1 config: flat cost per unit, by country and product
FLAT = {
    "US": {"mouse": 550, "laptop": 1000},   # cents per unit
    "CA": {"mouse": 750, "laptop": 1100},
}

# Part 2 config: tiered cost per unit by quantity range [min, max)
TIERED = {
    "US": {
        "mouse":  [(0, None, 550)],
        "laptop": [(0, 2, 1000), (2, None, 900)],   # first 2 at $10, rest at $9
    },
    "CA": {
        "mouse":  [(0, None, 750)],
        "laptop": [(0, 2, 1100), (2, None, 1000)],
    },
}


def shipping_flat(order: dict) -> int:
    """Part 1: order = {"country": "US", "items": [{"product":..., "quantity":...}]}"""
    rates = FLAT[order["country"]]
    return sum(rates[i["product"]] * i["quantity"] for i in order["items"])


def tiered_cost(tiers, qty: int) -> int:
    total = 0
    for lo, hi, per_unit in tiers:
        if qty <= lo:
            break
        upper = qty if hi is None else min(qty, hi)
        total += (upper - lo) * per_unit
    return total


def shipping_tiered(order: dict) -> int:
    """Part 2: per-unit price depends on how many units are in the tier.
    Note how part 1 becomes a special case (one tier with no upper bound)."""
    rates = TIERED[order["country"]]
    return sum(tiered_cost(rates[i["product"]], i["quantity"]) for i in order["items"])


# ============================================================================
# 4. ACCOUNT BALANCE FROM TRANSACTIONS  (reported: onsite, two parts)
# ============================================================================

@dataclass
class Txn:
    id: str
    account: str
    amount: int          # cents; negative = debit
    ts: int


def balances(txns: list[Txn]) -> dict[str, int]:
    """Part 1: replay transactions into final balances."""
    out: dict[str, int] = defaultdict(int)
    for t in txns:
        out[t.account] += t.amount
    return dict(out)


def balances_no_overdraft(txns: list[Txn]) -> tuple[dict[str, int], list[str]]:
    """Part 2: process in timestamp order; reject any debit that would make the
    balance negative. Return balances and the rejected transaction ids."""
    out: dict[str, int] = defaultdict(int)
    rejected = []
    for t in sorted(txns, key=lambda t: (t.ts, t.id)):
        if out[t.account] + t.amount < 0:
            rejected.append(t.id)
        else:
            out[t.account] += t.amount
    return dict(out), rejected


def balance_at(txns: list[Txn], account: str, ts: int) -> int:
    """Part 3 (common follow-up): balance of one account at a point in time."""
    return sum(t.amount for t in txns if t.account == account and t.ts <= ts)


# ============================================================================
# 5. RATE LIMITER  (reported: onsite)
# ============================================================================

class FixedWindowLimiter:
    """Part 1: at most `limit` requests per client per window of `window` secs.
    Simple, but allows bursts at window edges (say this in the interview)."""

    def __init__(self, limit: int, window: int):
        self.limit, self.window = limit, window
        self.counts: dict[tuple[str, int], int] = defaultdict(int)

    def allow(self, client: str, now: int) -> bool:
        key = (client, now // self.window)
        if self.counts[key] >= self.limit:
            return False
        self.counts[key] += 1
        return True


class SlidingWindowLimiter:
    """Part 2: true sliding window using a queue of timestamps per client.
    Part 3: different limits per client tier."""

    def __init__(self, window: int, limits: dict[str, int], default_limit: int):
        self.window = window
        self.limits = limits
        self.default = default_limit
        self.log: dict[str, deque] = defaultdict(deque)

    def allow(self, client: str, now: int) -> bool:
        q = self.log[client]
        while q and q[0] <= now - self.window:
            q.popleft()
        if len(q) >= self.limits.get(client, self.default):
            return False
        q.append(now)
        return True


# ============================================================================
# 6. SEARCH AND SORT A DATA TABLE  (reported: screen, one question + follow-up)
# ============================================================================

def query(rows: list[dict], where: dict | None = None, order_by: list[tuple[str, str]] | None = None) -> list[dict]:
    """Part 1: filter rows where every column equals the given value.
    Part 2: sort by several columns, each 'asc' or 'desc'.
    Stable sorts applied from the last key to the first handle mixed directions
    without needing to negate strings."""
    result = [r for r in rows if all(r.get(k) == v for k, v in (where or {}).items())]
    for col, direction in reversed(order_by or []):
        result.sort(key=lambda r: r[col], reverse=(direction == "desc"))
    return result


# ============================================================================
# 7. SPLIT A TRANSACTION NOTE  (reported: screen, string parsing in Python)
#    e.g. "$100.00 dinner split alice,bob,carol"
# ============================================================================

def split_note(note: str) -> dict[str, Decimal]:
    """Parse the amount and people, then split evenly in cents.
    Leftover cents go one each to the first people, so the parts always sum
    exactly to the total. Never use floats for money - interviewers notice."""
    tokens = note.split()
    amount_tok = next(t for t in tokens if t.startswith("$"))
    total = Decimal(amount_tok[1:].replace(",", ""))
    people = [p.strip() for p in tokens[tokens.index("split") + 1].split(",") if p.strip()]
    cents = int(total * 100)
    base, extra = divmod(cents, len(people))
    return {p: Decimal(base + (1 if i < extra else 0)) / 100 for i, p in enumerate(people)}


# ============================================================================
# 8. HASH MAP OF HASH MAPS  (reported: screen)
# ============================================================================

class SpendTracker:
    """Part 1: record(user, merchant, amount); total(user, merchant).
    Part 2: top merchant for a user.
    Part 3: top-k merchants across all users."""

    def __init__(self):
        self.spend: dict[str, dict[str, int]] = defaultdict(lambda: defaultdict(int))

    def record(self, user: str, merchant: str, amount: int) -> None:
        self.spend[user][merchant] += amount

    def total(self, user: str, merchant: str | None = None) -> int:
        if merchant is None:
            return sum(self.spend[user].values())
        return self.spend[user].get(merchant, 0)

    def top_merchant(self, user: str) -> Optional[str]:
        m = self.spend.get(user)
        if not m:
            return None
        # highest spend wins; ties broken alphabetically so output is deterministic
        return min(m.items(), key=lambda kv: (-kv[1], kv[0]))[0]

    def top_k_merchants(self, k: int) -> list[tuple[str, int]]:
        totals: dict[str, int] = defaultdict(int)
        for merchants in self.spend.values():
            for m, amt in merchants.items():
                totals[m] += amt
        return heapq.nlargest(k, totals.items(), key=lambda kv: (kv[1], kv[0]))


# ============================================================================
# 9. ARRAY PROBLEM WITH CUSTOM CLASSES  (reported: OA and screen, 2 parts / 45 min)
#    Representative version: an order book of buy/sell orders.
# ============================================================================

@dataclass(order=True)
class Order:
    sort_key: tuple = field(init=False, repr=False)
    price: int
    qty: int
    side: str        # "buy" or "sell"
    seq: int

    def __post_init__(self):
        # buys: highest price first; sells: lowest price first; then time order
        self.sort_key = (-self.price if self.side == "buy" else self.price, self.seq)


class OrderBook:
    """Part 1: add orders and report best bid / best ask.
    Part 2: when a new order crosses the book, match it and return the fills."""

    def __init__(self):
        self.buys: list[Order] = []
        self.sells: list[Order] = []
        self.seq = 0

    def best(self, side: str) -> Optional[int]:
        book = self.buys if side == "buy" else self.sells
        return book[0].price if book else None

    def add(self, side: str, price: int, qty: int) -> list[tuple[int, int]]:
        self.seq += 1
        order = Order(price, qty, side, self.seq)
        opposite = self.sells if side == "buy" else self.buys
        fills = []
        while order.qty and opposite:
            top = opposite[0]
            crosses = price >= top.price if side == "buy" else price <= top.price
            if not crosses:
                break
            traded = min(order.qty, top.qty)
            fills.append((top.price, traded))
            order.qty -= traded
            top.qty -= traded
            if top.qty == 0:
                heapq.heappop(opposite)
        if order.qty:
            heapq.heappush(self.buys if side == "buy" else self.sells, order)
        return fills


# ============================================================================
# 10. DESIGN AN API TO SUPPORT TRANSACTIONS  (reported: OA, "harder than expected")
#     The ideas Stripe cares about: idempotency, state machines, money in cents.
# ============================================================================

class PaymentError(Exception):
    pass


VALID = {
    "created":    {"authorized", "failed"},
    "authorized": {"captured", "canceled"},
    "captured":   {"refunded"},
}


@dataclass
class Payment:
    id: str
    amount: int
    currency: str
    status: str = "created"
    refunded: int = 0


class PaymentService:
    """Part 1: create a payment; the same idempotency key returns the same
           payment instead of charging twice (retries are safe).
    Part 2: authorize / capture / cancel with a state machine that rejects
           invalid transitions.
    Part 3: partial refunds that can never exceed the captured amount."""

    def __init__(self):
        self.payments: dict[str, Payment] = {}
        self.idempotency: dict[str, str] = {}
        self.next_id = 1

    def create(self, amount: int, currency: str, idempotency_key: str) -> Payment:
        if idempotency_key in self.idempotency:
            return self.payments[self.idempotency[idempotency_key]]
        if amount <= 0:
            raise PaymentError("amount must be positive")
        pid = f"pay_{self.next_id}"
        self.next_id += 1
        self.payments[pid] = Payment(pid, amount, currency.lower())
        self.idempotency[idempotency_key] = pid
        return self.payments[pid]

    def _move(self, pid: str, new_status: str) -> Payment:
        p = self.payments.get(pid)
        if p is None:
            raise PaymentError("no such payment")
        if new_status not in VALID.get(p.status, set()):
            raise PaymentError(f"cannot go from {p.status} to {new_status}")
        p.status = new_status
        return p

    def authorize(self, pid: str) -> Payment:
        return self._move(pid, "authorized")

    def capture(self, pid: str) -> Payment:
        return self._move(pid, "captured")

    def cancel(self, pid: str) -> Payment:
        return self._move(pid, "canceled")

    def refund(self, pid: str, amount: int) -> Payment:
        p = self.payments.get(pid)
        if p is None or p.status not in ("captured", "refunded"):
            raise PaymentError("only captured payments can be refunded")
        if amount <= 0 or p.refunded + amount > p.amount:
            raise PaymentError("refund exceeds captured amount")
        p.refunded += amount
        if p.refunded == p.amount:
            p.status = "refunded"
        return p


# ============================================================================
# Tests
# ============================================================================

def run_tests() -> None:
    # 1. currency
    c = Converter("USD:CAD:DHL:1.25,USD:GBP:FEDX:0.8,CAD:EUR:UPS:0.7")
    assert abs(c.direct(100, "USD", "CAD") - 125) < 1e-9
    assert abs(c.direct(125, "CAD", "USD") - 100) < 1e-9          # inverse rate
    assert c.direct(100, "USD", "EUR") is None
    assert abs(c.one_hop(100, "USD", "EUR") - 87.5) < 1e-9
    amt, route = c.best_path(100, "GBP", "EUR")                    # GBP->USD->CAD->EUR
    assert abs(amt - 109.375) < 1e-9 and len(route) == 3
    try:
        parse_rates("USD:CAD:5")
        raise AssertionError
    except ValueError:
        pass

    # 2. load balancer
    lb = LoadBalancer({"a": 2, "b": 2})
    assert [lb.round_robin() for _ in range(3)] == ["a", "b", "a"]
    assert lb.assign("r1", "alice") == "a"
    assert lb.assign("r2", "bob") == "b"
    assert lb.assign("r3", "alice") == "a"          # sticky
    assert lb.assign("r4", "alice") == "b"          # a is full, falls back
    assert lb.assign("r5") is None                  # everything full
    lb.finish("r1")
    moved = lb.server_down("a")
    assert moved == {"r3": None}                    # b is full too
    lb2 = LoadBalancer({"a": 2, "b": 2})
    lb2.assign("x", "u")
    assert lb2.server_down("a") == {"x": "b"}

    # 3. shipping
    order = {"country": "US", "items": [{"product": "mouse", "quantity": 20}, {"product": "laptop", "quantity": 5}]}
    assert shipping_flat(order) == 20 * 550 + 5 * 1000
    assert shipping_tiered(order) == 20 * 550 + (2 * 1000 + 3 * 900)

    # 4. balances
    tx = [Txn("t1", "A", 500, 1), Txn("t2", "A", -700, 2), Txn("t3", "B", 300, 1), Txn("t4", "A", -200, 3)]
    assert balances(tx) == {"A": -400, "B": 300}
    final, rejected = balances_no_overdraft(tx)
    assert final == {"A": 300, "B": 300} and rejected == ["t2"]
    assert balance_at(tx, "A", 2) == -200

    # 5. rate limiter
    f = FixedWindowLimiter(limit=2, window=10)
    assert [f.allow("c", t) for t in (1, 2, 3, 11)] == [True, True, False, True]
    s = SlidingWindowLimiter(window=10, limits={"pro": 3}, default_limit=1)
    assert [s.allow("free", t) for t in (0, 5, 10)] == [True, False, True]
    assert [s.allow("pro", t) for t in (0, 1, 2, 3)] == [True, True, True, False]

    # 6. table
    rows = [{"name": "b", "city": "SF", "age": 30}, {"name": "a", "city": "SF", "age": 30}, {"name": "c", "city": "NY", "age": 25}]
    assert [r["name"] for r in query(rows, {"city": "SF"})] == ["b", "a"]
    assert [r["name"] for r in query(rows, order_by=[("age", "desc"), ("name", "asc")])] == ["a", "b", "c"]

    # 7. split note
    parts = split_note("$100.00 dinner split alice,bob,carol")
    assert parts == {"alice": Decimal("33.34"), "bob": Decimal("33.33"), "carol": Decimal("33.33")}
    assert sum(parts.values()) == Decimal("100.00")

    # 8. nested maps
    st = SpendTracker()
    for u, m, a in [("u1", "amzn", 50), ("u1", "uber", 20), ("u1", "amzn", 10), ("u2", "uber", 100)]:
        st.record(u, m, a)
    assert st.total("u1", "amzn") == 60 and st.total("u1") == 80
    assert st.top_merchant("u1") == "amzn" and st.top_merchant("nobody") is None
    assert st.top_k_merchants(1) == [("uber", 120)]

    # 9. order book
    ob = OrderBook()
    ob.add("buy", 100, 5)
    ob.add("buy", 101, 2)
    ob.add("sell", 105, 4)
    assert ob.best("buy") == 101 and ob.best("sell") == 105
    assert ob.add("sell", 100, 4) == [(101, 2), (100, 2)]   # best bid fills first
    assert ob.best("buy") == 100

    # 10. payments API
    ps = PaymentService()
    p1 = ps.create(1000, "USD", "key-1")
    assert ps.create(1000, "USD", "key-1") is p1                # safe retry
    ps.authorize(p1.id)
    ps.capture(p1.id)
    ps.refund(p1.id, 400)
    try:
        ps.refund(p1.id, 700)
        raise AssertionError
    except PaymentError:
        pass
    ps.refund(p1.id, 600)
    assert p1.status == "refunded"
    try:
        ps.capture(ps.create(500, "usd", "key-2").id)            # skip authorize
        raise AssertionError
    except PaymentError:
        pass

    print("All 10 problems pass their tests.")


if __name__ == "__main__":
    run_tests()
