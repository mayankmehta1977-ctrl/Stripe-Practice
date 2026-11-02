# Interviewer guide: Integration round mock

For the person playing the Stripe interviewer. The real round is 60 minutes
with one Stripe engineer, who gives a spec and docs, watches the candidate
work inside an existing codebase, and asks questions throughout.
The questions below are modeled on that format and on what Stripe says it
evaluates. They are not Stripe's actual questions.

---

## 0. Opening (2-3 min)

Say, roughly:

> "Thanks for joining. Today you'll be adding a feature to an existing
> invoicing service. There's a spec and API docs in the repo. Take a few
> minutes to read them and the existing code before you start, and feel free
> to look anything up online. Please talk me through what you're thinking as
> you go. Any questions before you start?"

Then stay quiet while they read. Note whether they read the docs fully or
jump straight to coding.

---

## 1. While reading the spec (5-10 min)

Ask one or two of these if they don't raise them on their own:

- "Before you write anything, walk me through how you understand the task."
- "What do you see in the existing code that you'll reuse?"
- "Is there anything in the spec or the docs that seems ambiguous?"
- "How will you know you haven't broken the finance dashboard?"

**Strong signal:** they run the existing tests first, notice the
"do not change `summarize()`" constraint, and ask about rounding or
unknown currencies.

---

## 2. Part 1: API client and USD total (~20 min)

Questions to ask while they work:

- "Where are you putting the HTTP code, and why there?"
- "How does the API know who you are?" *(Authorization header)*
- "What does a rate of 0.92 for EUR mean? Which way do you divide?"
- "Why are amounts stored as integers rather than doubles?"
- "Where exactly do you round, and why there and not earlier?"
- "What happens if an invoice is in a currency the API doesn't return?"
- "How would you test this without the server running?"

**The trap:** JPY is a zero-decimal currency (last section of `API.md`).
If they get 17950 instead of 32800:
1. First, just say: "Your total doesn't match the expected value. How would
   you investigate?" (tests their debugging)
2. If still stuck after a few minutes: "Have a look at how amounts are
   described in the docs."
3. Last resort: "Check how yen amounts are represented."

---

## 3. Part 2: Top customers (~20 min)

Questions to ask:

- "How many customers did you get back? Does that match what you expected?"
  *(the server caps pages at 2, so a single request misses customers)*
- "How does the pagination cursor work, and how do you know when to stop?"
- "What if a customer has an invoice but isn't in the customer list?"
- "Two customers have the same revenue. What order should they come in?"
- "How are you testing the pagination logic without the network?"

**Hint ladder if they only get the first page:**
1. "Print how many customers you received."
2. "Look at the fields in the response besides `data`."

---

## 4. Part 3: Reliability (~15 min)

Questions to ask:

- "You just got a 429. What does that mean, and what should the client do?"
- "Which errors should be retried, and which shouldn't? Why not retry a 401?"
- "How do you test the retry without your test actually sleeping?"
  *(injectable sleeper / fake clock)*
- "Why cap the number of attempts?"
- "If you cache the rates, what's the risk? When would the cache be wrong?"
- "Would you retry on a network timeout? On a 500?"

---

## 5. Wrap-up questions (last 5-10 min)

Pick a few, depending on time:

- "If you had another hour, what would you improve?"
- "What tests are you still missing?"
- "This report now runs nightly over a million invoices. What changes?"
  *(batch currency conversion, one rates fetch, streaming the file)*
- "Rates change during the day. How would you make the report reproducible?"
  *(store the rates snapshot / timestamp used)*
- "How would you handle a customer list of 10 million records?"
- "If the API went down for an hour, what should this job do?"
- "Is a double the right type for currency conversion? What would you use
  in production?" *(BigDecimal, or integer minor units with explicit rounding)*

Then: "What questions do you have for me?" Have the candidate prepare two.

---

## Scoring sheet

Rate each 1-4 and write one sentence of evidence.

| Area | Strong (4) | Weak (1) |
|---|---|---|
| **Help yourself** | Reads docs and errors, finds the zero-decimal rule and pagination unaided | Guesses, asks the interviewer for things that are in the docs |
| **Abstractions & code** | Separate API client from conversion/report logic; small methods; clear names | HTTP, JSON and math all in one method; copy-paste |
| **Correctness & testing** | Runs existing tests first; adds tests per part; no network in tests; existing output unchanged | No tests, or only manual runs against the server |
| **Interaction** | Thinks aloud, states trade-offs, asks clarifying questions, takes hints well | Silent for long stretches, defensive about hints |

**Rough bar:** finishing Parts 1 and 2 with tests and good communication is
a solid performance. Part 3 is a stretch goal for an intern.
