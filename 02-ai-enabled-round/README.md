# Round 2: AI-enabled programming exercise

Stripe's format: 60 minutes in HackerRank's AI Coding Environment with a
built-in agent; a multi-part problem revealed one part at a time.

| Path | What it is |
|---|---|
| `CANDIDATE_GUIDE.md` | How to plan, prompt, review and prove correctness. **Read first.** |
| `INTERVIEWER_GUIDE.md` | For whoever runs the mock: questions, probes, scoring |
| `problems/<name>/part1.md ... part4.md` | Statements, revealed one at a time |
| `problems/<name>/AI_PITFALLS.md` | What agents get wrong on that problem. Read after practicing. |
| `workspace/` | Empty Maven project to practice in |
| `solutions/` | Reference solutions with JUnit tests. Open after practicing. |

## Problems

| Problem | Based on reported Glassdoor question |
|---|---|
| `1-load-balancer` | "Server load balancing" (OA and technical screen, 2025-2026) |
| `2-shipping-routes` | "Currency conversion with shipping" (`USD:CAD:DHL:5,...`) and "calculate shipping cost" |
| `3-transaction-ledger` | "Account balance from transactions" (onsite) and "Design an API to support transactions" |

Each takes about 45 minutes. Do them on different days.

## Running the reference solutions

```bash
cd solutions
mvn -q test        # 30 tests
```
