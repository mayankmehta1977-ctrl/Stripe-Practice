# Transaction ledger: Part 2

Two new rules:

1. Lines are **not** in time order. Process transactions by timestamp; if two
   have the same timestamp, keep their order in the input.
2. An account can't go negative. **Reject** any transaction that would
   overdraw, and keep processing the rest.

Return the final balances and the list of rejected transaction ids with the
reason.

Example: `t1,1,a,100` / `t2,2,a,-150` / `t3,3,a,-100` leaves `a = 0`, and
`t2` is rejected for insufficient funds.
