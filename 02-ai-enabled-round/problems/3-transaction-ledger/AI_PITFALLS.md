# Transaction ledger: what AI agents tend to get wrong

- **Money as `double`.** Amounts are integer cents; keep them in `long`.
- **`Collections.sort` vs input order.** Agents sort by timestamp but forget
  the tie-break on input position, or use an unstable approach.
- **Non-atomic transfers:** debiting the source, then discovering the problem,
  and leaving a half-applied transfer. Check every posting first, then apply.
- **Checking the overdraft after applying** instead of before.
- **Parsing by splitting and indexing without validating field counts**,
  so a 6-field transfer is misread as a 4-field line (or crashes).
- **Idempotency that ignores conflicts**: treating any repeated id as a
  harmless retry, even when the amount differs.
- **Rejected-id keys colliding**: storing a conflict under the original id
  overwrites information. Make the key or record distinct.
- **History that records only deposits**, or misses the source account of a
  transfer.
- **Tests that only check the happy path.** Add a test for each rule above.
