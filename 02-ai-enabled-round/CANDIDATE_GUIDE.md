# AI-enabled programming round: how to work

Stripe's round: 60 minutes in HackerRank's AI Coding Environment, about
10 minutes of setup and 45 minutes of coding, with a built-in agent (similar to
Copilot or Cursor). The problem has several parts, revealed one at a time.
Stripe grades **System Architecting, Critical Oversight, Correctness &
Debugging, and Interaction & Collaboration**. Only the built-in assistant is
allowed; no ChatGPT, Gemini or other external tools.

The core idea: **you are the engineer, the AI is a fast junior teammate.**
You decide the design, give it precise tasks, and check everything it writes.

---

## 1. The loop for every part

1. **Read and restate** (1-2 min). Say the requirement back in your own words.
   Ask about anything unclear: ordering, ties, errors, empty input.
2. **Sketch the design yourself** (1-2 min). Name the class, the data
   structures, the method signatures. Say why.
3. **Write the tests first, or at least the examples.** Hand-compute expected
   values from the spec. Never let the AI decide what "correct" is.
4. **Prompt with intent and constraints** (see templates below).
5. **Review the diff line by line**, out loud. Check it against the spec and
   the pitfalls list.
6. **Run the tests.** When something fails, debug it yourself first. Read the
   failure, form a hypothesis, check it.
7. **Commit mentally to the design** before moving on: what will the next
   part probably change?

---

## 2. Prompt templates

**Bad:** "Write a load balancer."

**Good (design-led):**

> In `LoadBalancer.java`, add round-robin routing. Constructor takes a
> `List<String>` of server ids in order. Add `String connect(String
> connectionId, String userId)` that returns the next server, wrapping around.
> Throw `IllegalArgumentException` for a repeated connection id. Keep it in
> one class, no interfaces or factories. Don't change any other files.

**Extending for the next part:**

> Change routing to least-connections with per-server capacity. Keep server
> order for tie-breaks (use LinkedHashMap). Return "REJECTED" when all are
> full. Add `disconnect(id)`; a second disconnect for the same id must be a
> no-op. Keep the existing round-robin constructor working.

**Tests:**

> Write JUnit 5 tests for exactly these cases, with these expected values:
> (1) capacities a=1,b=1: conn1->a, conn2->b, conn3->REJECTED.
> (2) after disconnect(conn1), conn4->a. (3) duplicate disconnect is harmless.

**Asking it to explain, not write:**

> Before changing anything: is Dijkstra still correct if the route may use at
> most K legs? Explain briefly.

**Pushing back:**

> This is more abstraction than we need. Remove the strategy interface and
> factory; put the logic in LoadBalancer with a simple mode field.

Rules of thumb:
- Name the file, class and method signatures.
- State constraints: data types (`long` cents), ordering, error handling,
  "don't touch other files," "no new dependencies."
- Ask for **small diffs**, one part at a time.
- Give it the expected values; don't ask it to invent them.

---

## 3. Review checklist ("AI slop" to catch)

Say these out loud when you check them:

- [ ] Does it do what *this part* asks, and nothing extra?
- [ ] Did it break or quietly change an earlier part's behavior?
- [ ] Ordering: any `HashMap`/`HashSet` where order matters?
- [ ] Money and numbers: `double` for money? overflow? rounding?
- [ ] Edge cases: empty input, duplicates, ties, unknown ids, zero/negative.
- [ ] Error handling: clear exceptions vs silent wrong answers.
- [ ] Invented APIs or methods that don't exist.
- [ ] Over-engineering: interfaces, factories, generics no one needs.
- [ ] Dead code, unused imports, misleading comments.
- [ ] Tests that only mirror the implementation, or only test the happy path.

**Know when to take over.** If the agent fails twice on the same bug, stop
prompting and fix it yourself. Saying "I'll just do this one by hand, it's
faster" shows exactly the independence they grade.

---

## 4. "How do you know this is correct?"

The interviewer will ask this at the end. Have an answer ready:

1. **Tests:** "I wrote tests from the spec examples with hand-computed values,
   one per rule, plus edge cases: X, Y, Z. They all pass."
2. **Reasoning:** "The key invariant is ___ (e.g. load never exceeds
   capacity, every transfer is all-or-nothing). Here's where the code
   enforces it."
3. **Known gaps:** "I haven't tested ___; with more time I'd add ___."

Naming a gap honestly scores better than claiming it's perfect.

---

## 5. Time plan for 45 minutes

| Minutes | Activity |
|---|---|
| 0-3 | Read Part 1, restate, ask questions, sketch the design |
| 3-15 | Part 1: tests, prompt, review, run |
| 15-27 | Part 2 |
| 27-39 | Part 3 |
| 39-45 | Part 4 if revealed, or polish tests and answer "how do you know?" |

Finishing every part is not required. Clean, correct, well-tested earlier
parts beat a rushed, broken final part.

---

## 6. Phrases that show your thinking

- "Before I prompt, here's the shape I want: ..."
- "I'm giving it the constraint about ordering because HashMap would break ties."
- "Let me read this diff before running it."
- "This part looks off: it re-routes before removing the server. I'll fix that."
- "It's over-engineered; I'm asking it to simplify."
- "The test it wrote just asserts what the code returns. I'll write the expected value myself."
- "That's failed twice; I'll take this one by hand."

---

## 7. Practice setup before the interview

- Install **GitHub Copilot** or **Cursor** (free tiers work) and use its
  sidebar agent, not just autocomplete.
- Open `workspace/` in the editor and do each problem in `problems/` with a
  45-minute timer, revealing one part at a time (or have someone run the
  interviewer guide).
- Afterwards, compare with `solutions/` and read that problem's
  `AI_PITFALLS.md`. Did you catch those issues?
