# Transaction ledger: Part 1

You receive transactions as lines of text:

```
t1,1,bob,500
t2,2,alice,1000
t3,3,bob,-200
t4,4,alice,-1500
```

Each line is `id,timestamp,account,amount`. Amounts are integers in **cents**:
positive is a deposit, negative a withdrawal.

Compute the **final balance of every account**, sorted by account id.
For now, balances may go negative.

Expected: `alice: -500, bob: 300`.

Ignore blank lines. A malformed line should produce an error that says
which line was bad.
