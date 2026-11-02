# Transaction ledger: Part 3

Add **transfers** between accounts, in a new line format:

```
id,timestamp,TRANSFER,from,to,amount
```

`amount` is positive. A transfer is **all or nothing**: if the source account
would go negative, reject the whole transfer and neither balance changes.
Transfers to the same account, or of non-positive amounts, are invalid.

The old 4-field format must keep working.
