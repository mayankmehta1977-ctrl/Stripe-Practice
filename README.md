# Stripe SWE intern interview prep (Java)

Practice material for Stripe's virtual onsite, organized by round type.
Everything is Java 17+ with Maven and JUnit 5.

| Folder | Round | What's inside |
|---|---|---|
| `01-integration-round/` | **Integration** | 60-min exercise: add a feature to an existing codebase using a spec, API docs and a local mock API. Interviewer guide and reference solution. |
| `02-ai-enabled-round/` | **AI-enabled programming** | How to work with the AI agent, an interviewer guide, and 3 progressive problems revealed part by part, with solutions and AI-pitfall notes. |
| `03-question-bank/` | Practice | 11 reported problems (8 Stripe, plus her Amazon and Visa questions) with Java solutions and tests. |
| `REPORTED_QUESTIONS.md` | Index | Every Glassdoor-reported question and where it's covered. |

## Suggested order

1. Read `02-ai-enabled-round/CANDIDATE_GUIDE.md`.
2. Day 1: Integration exercise (60 min), then compare with the solution.
3. Days 2-4: one AI-round problem per day, 45 minutes, parts revealed one at a time.
4. In between: question-bank problems, 2-3 per day, timed.
5. Before the onsite: redo the problem that felt weakest, from scratch.

Don't open any `solution/` or `solutions/` folder until the timer runs out.

## Running tests

```bash
cd 01-integration-round/java/solution && mvn -q test   # 14 tests
cd 02-ai-enabled-round/solutions && mvn -q test        # 30 tests
cd 03-question-bank/solutions && mvn -q test           # 14 tests
```

These exercises are modeled on Stripe's published candidate guides and on
candidate reports. They are not Stripe's actual interview questions.
