# Question bank

Problems reported by real candidates, written as multi-part questions.
Solve each in Java with your own tests, part by part, before opening
`solutions/`. Aim for 10-15 minutes per part.

Stripe's exact prompts aren't public; these are reconstructions from the
Glassdoor descriptions. Two more reported Stripe problems, **server load
balancing** and **account balances from transactions**, are full exercises
in `../02-ai-enabled-round/problems/`.

---

## Stripe

### 1. Currency conversion (technical screen)
Input: `"USD:CAD:DHL:1.25,USD:GBP:FEDX:0.8,CAD:EUR:UPS:0.7"`, meaning
1 USD buys 1.25 CAD through provider DHL.
1. Parse the input. Convert an amount using a direct rate. Rates work in
   both directions (1 CAD = 1/1.25 USD). Malformed entries are errors.
2. Allow one intermediate currency. Return the best result.
3. Allow any number of conversions. Return the best amount and the path.

Check: 100 GBP to EUR, best path `GBP -> USD -> CAD -> EUR` = 109.375.

### 2. Shipping cost (onsite)
Costs are in cents per unit, by country and product.
1. Flat rates: `US: mouse 550, laptop 1000`. Order: 20 mice + 5 laptops.
2. Tiered rates: laptops cost 1000 each for the first 2 units, 900 each after
   that. Make Part 1 a special case rather than separate code.

Check: Part 1 = 16000; Part 2 = 15700.

### 3. Rate limiter (onsite)
`allow(clientId, timestampSeconds)` returns true or false.
1. Fixed window: at most N requests per client per window.
   What's the weakness at window boundaries?
2. Sliding window.
3. Different limits per client (e.g. paid plans), with a default.

### 4. Search and sort a data table (technical screen)
Rows are maps of column to value.
1. Filter rows where all given columns equal given values.
2. Sort by several columns, each ascending or descending. Ties keep input order.

### 5. Split a transaction note (technical screen)
Input like `"$100.00 dinner split alice,bob,carol"`.
Return each person's share. Shares must add up exactly to the total.
What happens to the leftover cent? What input should be rejected?

### 6. Hash map of hash maps (technical screen)
Track spend as user -> merchant -> total cents.
1. `record(user, merchant, cents)`, `total(user)`, `total(user, merchant)`.
2. A user's top merchant (ties: alphabetical).
3. Top-k merchants across all users.

### 7. Array problem with custom classes (OA and screen)
Representative version: an order book.
1. Add buy and sell orders (price, quantity). Report the best bid (highest
   buy) and best ask (lowest sell).
2. An incoming order that crosses the book trades against the best opposite
   orders, best price first, then earliest first. Return the fills; any
   leftover quantity rests in the book.

### 8. Design an API to support transactions (OA)
1. `create(amount, currency, idempotencyKey)`. Retrying with the same key
   returns the same payment, not a second charge. Same key with different
   parameters is an error.
2. `authorize`, `capture`, `cancel`, enforced by a state machine.
   Capturing without authorizing is an error.
3. Partial refunds that can never exceed the captured amount.

---

## Asked at Amazon (her interview)

### 9. Tree serialize / deserialize (LeetCode 297, 606, 536)
1. Print a binary tree depth-first to a string with `#` for missing
   children: `1,2,#,#,3,4,#,#,5,#,#`. Then parse it back.
2. Bracket format: `4(2(3)(1))(6(5))`, with `()` for a missing left child
   when there is a right child: `1()(4)`. Print and parse.
3. Follow-up: print without recursion.

### 10. Find words in a string of letters (LeetCode 139, 140)
1. Can the string be split entirely into dictionary words?
2. List every way to split it.
3. List every dictionary word appearing anywhere in the string (use a trie).

## Asked at Visa (her interview)

### 11. Merge intervals (LeetCode 56)
Merge overlapping intervals. Key first move: sort by start.
`[8,10],[1,3],[2,6],[15,18]` gives `[1,6],[8,10],[15,18]`.
