# Integration round practice (Java): Invoicing service

A 60-minute practice exercise in the format of Stripe's Integration round:
an existing codebase, a spec, API documentation, and libraries to use.

- `starter/`: the existing codebase she works in (Maven project)
- `solution/`: a reference solution. **Don't open until the timer ends.**
- `mock_api/`: a local mock of the Ledger API
- `INTERVIEWER_GUIDE.md`: for the person playing the interviewer

## Setup (do this before starting the timer)

Requires Java 17+ and Maven.

```bash
# Terminal 1: start the mock API (no build needed)
java mock_api/MockLedgerServer.java

# Terminal 2: build the starter and run the existing tests (3 should pass)
cd starter
mvn -q test
```

Libraries available: **Gson** (JSON) and **JUnit 5** (tests) are already in
`pom.xml`. Use the JDK's built-in `java.net.http.HttpClient` for HTTP.
Adding Mockito to the pom is fine if she prefers it.

## How to run the practice

1. Start a 60-minute timer.
2. Spend the first 5-10 minutes reading `SPEC.md`, `API.md` and the code in
   `starter/src/main/java`. Don't write code yet.
3. Work through the parts in order, talking out loud. Looking things up
   (JDK docs, Gson docs, Stack Overflow) is allowed, as in the real round.
4. Run `mvn -q test` after every change. Existing tests must keep passing.
5. When time is up, compare with `solution/`.

To run the reference solution end to end against the mock server:

```bash
cd solution
mvn -q compile
mvn -q dependency:build-classpath -Dmdep.outputFile=cp.txt
java -cp target/classes:$(cat cp.txt) com.practice.invoicing.RunLive
```

## What the real interviewers evaluate

- **Ability to help yourself**: reading docs, error messages and existing code
- **Abstractions and writing code**: clean, extensible structure
- **Correctness, testing and debugging**: tests for what you add; nothing broken
- **Interaction and collaboration**: explaining choices, asking good questions
