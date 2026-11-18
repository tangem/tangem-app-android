# Allure steps gate

This is the prompt for the step-by-step gate in workflow step 6. The author passes this file's content
to a fresh `general-purpose` subagent, followed by the inputs below. Never use a `fork`: a fork inherits
the author's plan and reads the test the way the author meant it, not the way it is written.

The gate answers one question: **does each automated test do every step of its Allure case, in order,
and assert every expected result?** It does not review style, waits or page objects — that is the
reviewer's job (`reviewer.md`).

## Inputs the author appends

- `ANDROID_REPO`: absolute path to the Android repository.
- `TESTS`: one line per automated test — `path/to/FooTest.kt#methodName — AllureId N`.
- `AGREED_DEVIATIONS`: deviations from the cases the user has already approved, one line each
  (`N / step 4 — Android shows X instead of Y, approved`). Empty if none.

## Rules

- Read only. Never edit, create or delete files, never run `git` commands that change state, never
  build or run tests.
- Fetch every case yourself from Allure TestOps — never take steps from the author:
  `testops_find_testcases` with `projectId: 1`, `aql: "id = N"`, `expand: ["scenario"]`. Read the
  `precondition` field and every item of `scenario.steps`.
- A step's expected result is in one of two places: its `expectedResultSteps`, or — the common Tangem
  format — the lines of `body` after the first one (first line = action, the rest = what must be seen).
  Split every expected-result line into its own **check**: "Current plan «Basic»; блок «Card related»
  (…); кнопка «Change plan»" is three checks.
- Follow the code, not the names. A `step("Assert X is displayed")` whose block asserts something else,
  or nothing, covers nothing. Open the scenario and page-object functions the test calls until you reach
  the actual Kakao/Compose call; a scenario call counts only for what its body really does.
- Report only what you can point to in the code.

## What to produce, per case

1. **Preconditions.** Each precondition maps to setup code: a WireMock scenario state, a mock content,
   a feature toggle, an imported wallet. Missing setup is `MISSING`.
2. **Step table.** One row per action and one row per check, in case order:

   | # | Case (verbatim) | Test code (`file:line`, `step("…")` name) | Verdict |
   |---|---|---|---|

   Verdicts:
   - `COVERED` — the action is performed, or the check is asserted on the value the case names (the
     text, amount, state), on the screen the case names.
   - `WEAKER` — asserted, but weaker than the case: existence instead of the value, a container instead
     of its content, a negative assertion (`assertDoesNotExist` / `assertIsNotDisplayed`) as proof that
     something happened, a substring that also matches another state, a check the case puts on screen A
     made on screen B.
   - `MISSING` — no code does or asserts it.
   - `AGREED` — listed in `AGREED_DEVIATIONS`. Quote the line.
   - `NOT_AUTOMATABLE` — needs a real card tap, a real chain, biometrics, another device. Say why in
     one clause; it is still the user's call, so it is a finding unless it is in `AGREED_DEVIATIONS`.
3. **Order.** Flag a step the test performs before an earlier case step it depends on, and a test that
   stops before the case's last step.
4. **Extra steps.** List test steps that match no case step and are not setup or synchronisation. They
   are fine when they only prepare a state; flag one that changes what the case verifies.
5. **Allure report readability.** The `step("…")` names in the test body follow the case's steps closely
   enough that a tester reading the Allure report can find each case step. Flag a case step whose code
   sits inside an unrelated or generic step name.

## Output

For each case: the step table, then the findings, most severe first, one per line:

```
N / step K — MISSING|WEAKER|NOT_AUTOMATABLE|ORDER|EXTRA|NAMING — case text — test file:line or "none" — what to add or change
```

End with one line per case: `GATE N: PASS` when every row is `COVERED` or `AGREED` and there are no
`ORDER` findings, otherwise `GATE N: FAIL (<count> findings)`. No summary beyond that.