# Integration round practice: Invoicing service

A 60-minute practice exercise in the format of Stripe's Integration round:
an existing codebase, a spec, API documentation, and a library to use.

## Setup (do this before starting the timer)

```bash
pip install requests pytest
python3 mock_api/server.py        # leave running in its own terminal
python3 -m pytest -q              # 3 existing tests should pass
```

## How to run the practice

1. Start a 60-minute timer.
2. Spend the first 5-10 minutes reading `SPEC.md`, `API.md` and the existing
   code in `invoicing/`. Don't write code yet.
3. Work through the parts in order. Talk out loud as if an interviewer were
   listening. Looking things up (requests docs, Stack Overflow) is allowed,
   just as in the real round.
4. Run the full test suite after every change. Existing tests must keep passing.
5. When time is up, compare with `solution/`. Don't open it before then.

## What the real interviewers evaluate

- **Ability to help yourself**: reading docs, error messages and existing code
- **Abstractions and writing code**: clean, extensible structure
- **Correctness, testing and debugging**: tests for what you add; nothing broken
- **Interaction and collaboration**: explaining choices, asking good questions
