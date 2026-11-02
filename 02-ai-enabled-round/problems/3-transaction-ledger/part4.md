# Transaction ledger: Part 4

Our clients retry requests when they time out, so the same transaction can
appear more than once.

1. **Idempotency:** apply each transaction id at most once. An exact repeat
   is ignored silently. A repeat with the same id but **different details**
   is a conflict: reject it and report it.
2. **History:** add `balanceAt(account, timestamp)`, returning the balance
   after all transactions up to and including that timestamp.

Example: `t1,10,a,100` / `t2,20,a,-30` gives `balanceAt(a, 25) = 70` and
`balanceAt(a, 5) = 0`.
