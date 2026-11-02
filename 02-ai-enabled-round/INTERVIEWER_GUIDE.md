# Interviewer guide: AI-enabled round mock

For the person playing the Stripe interviewer. Pick one problem from
`problems/`. Give the candidate **only `part1.md`** at the start, and reveal
the next part when they finish (or when ~12 minutes have passed).
These questions are modeled on Stripe's published format and evaluation
areas; they are not Stripe's actual questions.

---

## Setup (10 min)

The candidate opens `workspace/` in an editor with an AI agent (Copilot
Chat, Cursor). Ask:

- "Walk me through your environment. How do you usually use the agent?"
- "How will you run tests?"

Then share Part 1 and say:

> "You can use the agent however you like. Please narrate: tell me what
> you're asking it and why, and what you think of what it gives you."

---

## Questions to ask while they work

**System architecting**
- "Before you prompt, what's your plan for structuring this?"
- "Why that data structure?"
- "The agent added an interface and a factory. Do we need them?"
- "What do you expect the next part might ask for? Does your design allow it?"

**Critical oversight**
- "Walk me through what the agent just generated. Anything you'd change?"
- "How do you know that's right without running it?"
- "Is that what the spec asked for, or something slightly different?"
- Point at a line: "What happens here if two servers have the same load?"

**Correctness and debugging**
- "Where did the expected values in your tests come from?"
- "What edge cases are you worried about?"
- When a test fails: "What's your hypothesis?" Watch whether they debug or
  just re-prompt blindly.
- "Would you trust this test if the agent wrote it?"

**Interaction**
- Give one piece of feedback mid-way ("I'd like ties broken differently")
  and see how they adapt.
- "Is anything in this part unclear to you?"

---

## Problem-specific probes

**Load balancer**
- Part 2: "What if the same disconnect arrives twice?"
- Part 3: "The sticky server is full. Then what?"
- Part 4: "In what order do the moved connections get re-routed? Show me
  where the code guarantees that."

**Shipping routes**
- Part 1: "Can we ship UK to US with this input?" *(no, one-way)*
- Part 1: "Two carriers on the same lane. Which do you keep?"
- Part 4: "Is Dijkstra still correct with a hop limit?" *(no)* If they
  aren't sure: "Try US to DE with max 2 legs. What do you expect?"

**Transaction ledger**
- Part 1: "Why `long` and not `double`?"
- Part 2: "Two transactions at the same timestamp: which goes first?"
- Part 3: "What if the transfer fails halfway?"
- Part 4: "Same id, different amount: retry or bug?"

---

## The closing question (always ask)

> **"How do you know this is correct?"**

Strong answer: tests derived from the spec with hand-computed values,
covering each rule and the edge cases; the key invariants and where the code
enforces them; and an honest list of what isn't tested yet.

Weak answer: "The tests pass" (when the agent wrote the tests), or "It looks
right."

Follow-ups: "What test would you add next?" "What would break first in
production?"

---

## Scoring sheet

Rate 1-4 with one sentence of evidence each.

| Area | Strong (4) | Weak (1) |
|---|---|---|
| **System architecting** | Plans before prompting; gives clear constraints; keeps the design simple and extensible; rejects over-engineering | Prompts cold ("write a load balancer"); accepts whatever structure appears |
| **Critical oversight** | Reads every diff; catches spec mismatches and logic errors; takes over when the agent struggles | Accepts output unread; re-prompts in circles |
| **Correctness & debugging** | Writes or dictates tests with hand-computed values; covers edge cases; debugs systematically | No tests, or agent-written tests that mirror the code; guesses at fixes |
| **Interaction** | Narrates decisions; asks clarifying questions; adapts to feedback | Silent; defensive; doesn't explain choices |

**Rough bar for an intern:** Parts 1-2 solid with tests and clear reasoning,
Part 3 in progress, and a convincing answer to the closing question.
