---
name: write-ui-test
description: Write a Kaspresso/Compose instrumentation UI test for the Tangem Android app following project conventions — test class shape, page-object locations, Allure step naming, WireMock scenario setup, synchronization, and meaningful assertions. Covers known Compose traps (PullToRefreshBox swipe, TangemHoldToConfirmButton, Decompose lifecycle, hot-wallet access code) and the build/run/debug flow. Use when the user asks to write, add, port, or fix an instrumentation / androidTest / UI test, a page object, or a test scenario ("напиши UI-тест", "добавь инструментальный тест", "напиши тест в androidTest", "page object", "автотест на экран").
allowed-tools: Read, Grep, Glob, Bash, Edit, Write, Agent
argument-hint: [screen/flow or TC# to cover, e.g. "TangemPay freeze card"]
---

Write an instrumentation (androidTest) UI test for the Tangem Android app. These conventions are
enforced by reviewers (tnagmetulla, dpodoynikov) — applying them up front skips a review round.

This is an **interactive** skill: if scope is ambiguous (which screen, which flow, what the final
assertion should verify), ask before writing. Do not invent UI text or test tags — read the real
production source and reuse existing patterns.

## When to use

Use for instrumented UI tests under `app/src/androidTest/` (Kaspresso + Kakao-Compose), page objects,
and test scenarios. **Not** for JVM/Robolectric unit tests (`testDebugUnitTest`) — those follow a
different setup.

## Workflow

1. **Clarify scope.** Which screen/flow, which Allure TC#, and what the *final assertion* verifies.
   Ask if any of these is unclear.
2. **Find an existing sibling test to mirror.** Grep `app/src/androidTest/` for a test on a similar
   screen (e.g. `SendViaSwapTest`). Match its structure rather than inventing one. Read the real
   production composable to get the actual `testTag`s and string resources — never guess UI text.
3. **Locate / extend page objects** in `com/tangem/screens/` (see Locations). Add new ones there,
   never inside the scenario or test file.
4. **Set up WireMock scenarios** in the *test body* if the flow depends on backend state
   (see `reference/running-and-debugging.md`).
5. **Write the test** per Conventions below.
6. **Build BOTH APKs, install, run, and classify the result** correctly — Allure post-run hook
   failures are not test failures (see `reference/running-and-debugging.md`).
7. **Final cleanup pass — remove what you no longer use.** Before declaring done, review every file you
   touched for leftovers from iteration (see "Final cleanup" below). This is a required step, not optional.

## Final cleanup (required before done)

Iterating on a test typically leaves dead code behind — an import for a helper you swapped out, an
`@OptIn` for an API you stopped calling directly, a matcher you replaced. Reviewers flag these, and a
stray `@OptIn` reads as "this code needs an experimental API" when it doesn't. For **every file you
added or edited** (production `testTag` files included), check and remove:

- **Unused imports.** Any leftover after swapping an approach (e.g. `performTextReplacement` →
  `performTextInputInChunks`, `onDialog` after extracting a scenario). Diff-check each import against
  the body: `for f in <changed .kt>; do grep '^import' "$f" | while read -r i; do n=${i##*.}; n=${n%% *};
  grep -q "\b$n\b" <(grep -v '^import' "$f") || echo "$f: unused? $i"; done; done`
  (heuristic — `foundation.layout.*` wildcards and `getValue`/`setValue` used only by `by` delegates
  are false positives; verify before deleting).
- **Redundant `@OptIn(ExperimentalTestApi::class)`.** Needed **only** where you call an experimental
  API *directly* (`performScrollToNode`, `waitUntilAtLeastOneExists`, `waitUntilDoesNotExist`, …).
  It is **not** needed just to call your own helper that is already annotated (e.g. a page-object
  `scrollToNetwork` that wraps `performScrollToNode`), nor for the stable `composeTestRule.waitUntil(timeoutMillis, condition)`.
  A `BaseTestCase`-extension scenario that only calls annotated page-object methods + stable `waitUntil`
  needs no `@OptIn` — drop it (and its `import androidx.compose.ui.test.ExperimentalTestApi`).
- **Dead vals / matchers / page-object members** you introduced and then stopped referencing.

Then recompile the changed module(s) to confirm the removals are valid — the androidTest APK
(`:app:assembleGoogleMockedAndroidTest`) for test-side edits, or the touched production module
(e.g. `:core:ui:compileDebugKotlin`) for `testTag` edits. A clean compile with no opt-in / unused-symbol
warnings is the pass criterion. Behavior-only-neutral cleanups don't need a re-run of the suite.

## Porting a test from iOS

When the user asks to **port** an iOS test to Android:

- **Default to the sibling iOS repo `../tangem-app-ios/`** (next to `tangem-app-android`). If that path
  doesn't exist, **ask the user** where the iOS repo is — don't guess.
- iOS UI tests live under `TangemUITests/`; look there for the source test, its page objects
  (`*Screen`), and accessibility identifiers (`*AccessibilityIdentifiers`).
- Port the *intent and steps*, not the API. Map the iOS stack to the Android one:
  XCUITest/accessibility identifiers → Compose `testTag`; iOS `*Screen` page objects → Kotlin page
  objects in `com/tangem/screens/`; XCTest assertions → Kaspresso/Truth assertions. Re-derive the real
  Android `testTag`s and string resources from production source — never reuse iOS identifier strings.
- **Card/wallet mock mapping — where iOS uses `wallet2`, Android uses the default `Wallet`**
  (`openMainScreen()` with no `productType` → `ProductType.Wallet`). Do NOT port iOS `.wallet2` to
  `ProductType.Wallet2`. Other cards map directly: iOS `.twin` → `ProductType.Twins`, `.xrpNote` →
  `ProductType.Note`, `.four12` → `Firmware412MockContent` (via the `mockContent` param). `ProductType.Wallet2`
  exists but is a distinct Wallet-2.0-card case, not the iOS-`wallet2` analog.
- **A wallet has no balance until you sync.** The default `Wallet` mock starts with missing derivations
  ("Some addresses are missing"); the fiat balance shows `—` until you call `synchronizeAddresses()` after
  `openMainScreen()` (mirror `TotalBalanceUpdateTest`). Any test asserting a balance/fiat-equivalent must
  sync first, then `waitUntil` the value loads — balances re-load asynchronously (e.g. after an app-currency
  change the equivalent repaints with a delay).
- The WireMock scenarios are usually shared across platforms, but the branch may differ
  (see `reference/running-and-debugging.md`).

## Conventions (must-follow)

### Test class shape

- **Test method names are camelCase and always end with `Test`** (e.g. `groupTokensTest()`,
  `renameWalletTest()`). Never use `snake_case` and never the unit-test `GIVEN … WHEN … THEN …`
  backtick phrasing — that GWT convention is for JVM unit tests only, not instrumentation tests.
- **Scenario state setup goes in the test body**, not inside the open-the-feature helper. Each test
  starts with explicit `step("Set WireMock scenario '$name' to '$state'") { setWireMockScenarioState(name, state) }`
  calls, then calls a thin helper (e.g. `openTangemPay()`) that only opens the screen. Mirror the
  `SendViaSwapTest` pattern.
- **Open-the-feature helpers stay thin** — no scenarios-as-parameters, no scenario juggling inside.
- **Every scenario name + state is a `val`** at the top of the test method. Reviewers reject magic
  strings inside `step(...)`.
- **Each click is its own** `step("Click on '$x' button")`. Combining clicks into one step hides which
  click failed in the Allure report.
- **No reusable step-helpers as private functions in the test class.** A sequence reused across tests
  (e.g. `enterAmount`, `assertReady`) goes in a `scenarios/` file as a `BaseTestCase` extension, not as a
  private method on the test class — reviewers reject the latter. The test body then calls it wrapped in a
  `step(...)` like any scenario.
- **Every scenario call in the test body is wrapped in its own `step("…")`**, even though the scenario
  itself contains inner `step(...)`s — the outer step names the flow in the Allure tree, the inner ones
  detail it (nested steps are expected). `step(...)` (Allure) is callable anywhere, including inside
  scenario extension functions; only `flakySafely` is restricted to the `TestCase` body. Caveat: don't
  wrap a *mutating* scenario (e.g. one that long-clicks to sign+send) in `flakySafely` — a retry would
  re-fire the action; rely on the assertion's own built-in retry instead.
- **Step naming**: `Click on 'X' button` (not "Tap X"); `Assert <thing> is displayed` / `is not displayed`.
  Reviewers reject `is visible`, `does not exist`, `Check X visible` — the convention is **`is displayed` /
  `is not displayed`** even though older tests in the file may still use the old phrasing (don't copy it).
- **No conditional `if (foo.isDisplayedSafely()) foo.performClick()`** for elements that are
  deterministically present after `pm clear` — the `if` is dead code. Use a straight `performClick()`.

### Locations

| What | Where |
|------|-------|
| Page objects | `app/src/androidTest/kotlin/com/tangem/screens/…` — **always** |
| Common test helpers | `app/src/androidTest/kotlin/com/tangem/common/utils/` |
| Feature scenarios | `app/src/androidTest/kotlin/com/tangem/scenarios/` |
| Cross-feature helper (e.g. `confirmSwapByHolding`) | the **feature-of-origin** scenarios file (e.g. `SwapScenarios.kt`), not the consumer's |

Scenario files orchestrate flows; they must not define page objects or duplicate generic helpers.

### Strings

- **No hardcoded UI text** in matchers. Use `getResourceString(R.string.foo)` from
  `com.tangem.core.res.R` or `com.tangem.core.ui.R`. The Detekt rule `UnsafeStringResourceUsage`
  enforces this for production code; reviewers extend it to test code informally.

### Allure IDs

- **Every test method gets its own unique `@AllureId`.** Never reuse the same id across two test methods —
  not even for two variants of one manual case. If a manual case is split into multiple automated tests
  (e.g. a positive and a negative variant), each test must be linked to its own distinct Allure case/id.
  (Note: iOS sometimes shares one id across methods — do NOT mirror that here.)

### Assertions

- **Never** use Kotlin's built-in `assert(...)` — Android instrumentation runs don't enable JVM
  assertions, so `assert(false)` is a silent no-op. Use Truth / JUnit / Kaspresso / Kakao assertions.
- **Clipboard checks**: `assertClipboardTextEquals(expected, context)` from `common/utils/ClipboardUtils.kt`.
  Read displayed text via `KNode.extractText()` first if you need to compare against UI state.
- **Every test ends with a meaningful assertion**, not just an action. A test whose last step is
  "Click Submit" without verifying the result gets rejected.

### Waits and synchronization

- **Manual polls are banned** (`onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()` in a loop) — even
  if a bot reviewer suggests one.
- **Default in the test body: `flakySafely(TIMEOUT) { assertion }`** — the codebase idiom (hundreds of
  uses); reviewers prefer it over `composeTestRule.waitUntil { runCatching { … }.isSuccess }`.
- **`ComposeNotIdleException` / `AppNotIdleException` ("busy for ~60s") is usually a sick emulator, not
  your test.** After many back-to-back local runs the emulator degrades (you may even see a "System UI
  isn't responding" ANR), and idle-synced ops (`flakySafely`, `waitForIdle()`, Kakao actions) start
  timing out *anywhere* data is loading — different test each run. Before concluding a test is flaky or
  that a screen "never idles", **cold-boot a fresh emulator** (`emulator -avd … -no-snapshot -wipe-data
  -memory 4096 -cores 2`) and re-run. A suite that flaked across runs on a tired emulator can be a clean
  10/10 on a fresh one (verified on this exact suite). Don't rewrite waits to work around emulator rot.
- **In scenario / `BaseTestCase`-extension code, `flakySafely` is NOT available** regardless — use the
  same `composeTestRule.waitUntil` fallback (or `waitUntilAtLeastOneExists(matcher, timeout)` to wait for
  appearance, `{ a exists || b exists }` for either/or).
- **Don't repeat the `composeTestRule.waitUntil(timeoutMillis = …) { runCatching { … }.isSuccess }`
  block across steps — extract a one-line private helper** in the scenario file and call that instead.
  A multi-step scenario that gates every async step this way turns into copy-paste noise (and reviewers
  flag it). Add once, near the top of the file:
  ```kotlin
  // flakySafely is unavailable in BaseTestCase extensions — wait until the assertion/action stops throwing.
  private fun BaseTestCase.awaitSuccess(block: () -> Unit) {
      composeTestRule.waitUntil(timeoutMillis = WAIT_UNTIL_TIMEOUT) { runCatching(block).isSuccess }
  }
  ```
  then each step reads `awaitSuccess { onXxxScreen { field.assertExists() } }` before the action.
- **Right-size the timeout — don't stamp `WAIT_UNTIL_TIMEOUT_LONG` on every step.** The timeout is a
  *ceiling*, not a sleep (`waitUntil` returns the moment the condition holds), but the default
  `WAIT_UNTIL_TIMEOUT` (20 s) already dwarfs a normal async transition. Reserve `…_LONG` / `…_VERY_LONG`
  for steps that are genuinely slow (a real network round-trip + debounce that can approach 20 s);
  using LONG uniformly is lazy and hides which step is actually the slow one.

### Comment hygiene

This repo enforces "no comments unless WHY is non-obvious", in test code too. One line max, WHY-only —
encode a hidden constraint, not what the code does. Example that earns its keep:
`// Create+confirm screens share ACCESS_CODE_INPUT — gate on confirm-screen title.`
Delete anything explaining WHAT a step does.

## Reference docs

- **`reference/compose-traps.md`** — read when the screen uses `PullToRefreshBox`,
  `TangemHoldToConfirmButton`, a Decompose model that fetches in `init {}`, a hot-wallet import with
  an access code, or a target inside a **LazyColumn/LazyRow that may be below the fold** (use a
  `KLazyListNode` matcher that auto-scrolls — never a manual swipe). These have silent failure modes that
  look like passing tests.
- **`reference/running-and-debugging.md`** — read when building, installing, running tests (orchestrator
  vs. raw `am instrument`), running against a local WireMock, interpreting CLI/Allure output, using
  `@Ignore`, or driving WireMock scenarios. Includes how to find app-side root causes when the UI fails
  silently (the app log in `files/log.txt`, and the WireMock journal).