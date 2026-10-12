---
name: write-ui-test
description: Write a Kaspresso/Compose instrumentation UI test for the Tangem Android app following project conventions — test class shape, page-object locations, Allure step naming, WireMock scenario setup, synchronization, and meaningful assertions. Covers known Compose traps (PullToRefreshBox swipe, TangemHoldToConfirmButton, Decompose lifecycle, hot-wallet access code), a catalogue of known pitfalls, a mechanical self-review script plus a fresh-context reviewer subagent, and the build/run/debug flow. Use when the user asks to write, add, port, or fix an instrumentation / androidTest / UI test, a page object, or a test scenario ("напиши UI-тест", "добавь инструментальный тест", "напиши тест в androidTest", "page object", "автотест на экран").
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
   Ask if any of these is unclear. Pull the raw steps and expected results of each case from Allure
   TestOps (allure MCP) and keep them verbatim — the reviewer in step 6 needs them, not your paraphrase.
   Every deviation from a case (a step Android can't do, a different expected text) is the user's call:
   agree it before writing, and keep the agreed list for step 6.
2. **Start the build in the background, then find an existing sibling test to mirror.** A cold build of
   both APKs takes ~25 min; run `git submodule update --init --recursive` first, then start it with
   `run_in_background` before reading code (see `reference/running-and-debugging.md`). Grep
   `app/src/androidTest/` for a test on a similar screen (e.g. `SendViaSwapTest`). Match its structure
   rather than inventing one. Read the real production composable to get the actual `testTag`s and
   string resources — never guess UI text.
3. **Read `reference/pitfalls.md`**, then locate / extend page objects in `com/tangem/screens/` (see
   Locations). Add new ones there, never inside the scenario or test file. Check existing tags before
   adding new ones; a new tag goes on the production composable with only `.testTag(...)` — no other
   change to layout or behaviour.
4. **Set up WireMock scenarios** in the *test body* if the flow depends on backend state
   (see `reference/running-and-debugging.md`). If you add or change mocks in `../tangem-api-mocks`,
   list the other tests that use the same endpoint or scenario — the user re-runs them.
5. **Write the test** per Conventions below.
6. **Self-review — before the first run.** A review round is cheaper than a build-and-run cycle.
   1. Run `.claude/skills/write-ui-test/scripts/check.sh [<android-base>] [<mocks-base>]` from the repo
      root (defaults: `origin/develop`, `origin/main`; pass `origin/releases/x.y` when the branch is
      based on a release; set `MOCKS_ROOT` if the mocks repo is not at `../tangem-api-mocks`). It
      checks the working tree, untracked files included. Fix every `error`. Keep the printed
      `report.txt` and `review.diff` paths.
   2. Spawn a fresh `general-purpose` subagent — never a `fork` — with the content of
      `reference/reviewer.md` as the prompt, followed by its inputs: the `review.diff` and `report.txt`
      paths, both repo paths, the raw case steps from Allure, and the deviations the user approved. If
      subagents are unavailable, go through `reference/reviewer.md` yourself as a separate pass over
      `review.diff`, ignoring your plan.
   3. Verify each finding against the code. Fix the confirmed ones.
   4. Re-run only `scripts/check.sh`. No second reviewer round.
7. **Build BOTH APKs, install, run only the methods under test, and classify the result** correctly —
   Allure post-run hook failures are not test failures (see `reference/running-and-debugging.md`).
   Before blaming a test, check your local WireMock is started the way CI starts it — a misconfigured
   local instance fails tests that are fine on CI. Don't run the whole package or feature "for
   regression" — CI does that.
8. **Final cleanup, then `check.sh` once more.** Iterating on a failing run leaves dead code behind (see
   "Final cleanup" below). Re-run the script after the last edit and recompile the touched modules.
9. **Report.** List every reviewer finding with its outcome — fixed, or rejected with a one-line
   reason. Never drop a finding silently. Name the tests the user should re-run for any shared mock
   you changed.

## Final cleanup (required before done)

Iterating on a test typically leaves dead code behind — an import for a helper you swapped out, an
`@OptIn` for an API you stopped calling directly, a matcher you replaced. Reviewers flag these, and a
stray `@OptIn` reads as "this code needs an experimental API" when it doesn't. For **every file you
added or edited** (production `testTag` files included), check and remove:

- **Unused imports.** Any leftover after swapping an approach (e.g. `performTextReplacement` →
  `performTextInputInChunks`, `onDialog` after extracting a scenario). `check.sh` lists them as
  `unused-import` (a heuristic — operator and extension imports can be false positives; verify
  before deleting).
- **Redundant `@OptIn(ExperimentalTestApi::class)`.** Needed **only** where you call an experimental
  API *directly* (`performScrollToNode`, `waitUntilAtLeastOneExists`, `waitUntilDoesNotExist`, …).
  It is **not** needed just to call your own helper that is already annotated (e.g. a page-object
  `scrollToNetwork` that wraps `performScrollToNode`), nor for the stable `composeTestRule.waitUntil(timeoutMillis, condition)`.
  A `BaseTestCase`-extension scenario that only calls annotated page-object methods + stable `waitUntil`
  needs no `@OptIn` — drop it (and its `import androidx.compose.ui.test.ExperimentalTestApi`).
- **Dead vals / matchers / page-object members** you introduced and then stopped referencing.
- **Comments left over from debugging or from explaining a choice to the user** — re-read every added
  comment against "Comment hygiene" and delete what a name or a step already says.

### Mechanical gates live in `scripts/check.sh` — run it, do not eyeball

The script encodes the conventions reviewers keep flagging: sleeps, Kotlin `assert()`, Truth and
`com.tangem.common.R` imports, the UiAutomator `pullToRefresh()`, JSON by string interpolation, `@Ignore`
without a ticket, step wording, raw Compose in a test body, scenario-name literals, test-method names,
missing / duplicate `@AllureId`, waits of 180 s and more, test tags not applied in production, and mock
JSON / body files. `error` lines must be fixed. `warn` lines need a decision, not silence:

- `manual-wait` — `awaitSuccess(timeoutMillis, block)` **is** the codebase's wrapper for
  `composeTestRule.waitUntil { runCatching(block).isSuccess }`. A hand-rolled loop stays only when it
  retries a *mutating* action (a click, a hold) and carries a one-line WHY.
- `enabled-semantics` — read the target in production before choosing the assertion: see
  `reference/pitfalls.md` ("Before asserting enabled state…").
- `comment` / `kdoc` — delete by default (see "Comment hygiene"); keep only a one-line comment whose
  removal would get the code broken or reverted.
- `production-non-tag`, `long-timeout`, `private-helper-in-test`, `mock-*` — the reviewer
  in workflow step 6 triages them; fix the ones it confirms.

The script only matches text. Walk the **whole** Waits/sync section against the diff by meaning too.

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
  objects in `com/tangem/screens/`; XCTest assertions → Kakao / JUnit `Assert` assertions. Re-derive the real
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
  (see `reference/running-and-debugging.md`). Shared fixtures are often built for iOS and miss fields
  Android's Moshi requires, or serve iOS-specific behaviour (page sizes, `User-Agent`-guarded variants)
  — see the Mocks section of `reference/pitfalls.md`.
- **Android names and data differ from iOS.** TON's coin is `Gram` (iOS: Toncoin); the Note mock is a
  Dogecoin Note; Tangem Pay history pages by 50, not 20. Re-derive every expected text from Android
  production code and string resources.
- **When Android behaves differently from the iOS case, don't "stabilize" it.** A longer wait never
  fixes a divergence (e.g. Android dismisses the Tangem Pay PIN sheet on pause; the Main tile shows a
  card count, not the `*XXXX` mask). Confirm in production code which behaviour Android implements,
  then ask the user: assert what Android renders and get the case split or updated, or file a bug and
  `@Ignore` the test with the ticket URL.

## Conventions (must-follow)

### Test class shape

- **Test method names are camelCase and always end with `Test`** (e.g. `groupTokensTest()`,
  `renameWalletTest()`). Never use `snake_case` and never the unit-test `GIVEN … WHEN … THEN …`
  backtick phrasing — that GWT convention is for JVM unit tests only, not instrumentation tests.
- **Scenario state setup goes in the test body**, not inside the open-the-feature helper. Each test
  starts with explicit `step("Set WireMock scenario '$name' to '$state'") { setWireMockScenarioState(name, state) }`
  calls, then calls a thin helper (e.g. `openTangemPay()`) that only opens the screen. Mirror the
  `SendViaSwapTest` pattern. **Exception — endpoints read at app start** (`/v1/networks/providers`,
  `/v1/stories/*`): a state set in the body races the startup requests, so set it in
  `setupHooks(additionalBeforeAppLaunchSection = { … })` (see `BalanceCachingTest`, `AppTransfersTest`).
- **Don't roll scenarios back.** `BaseTestCase.setupHooks` resets *all* WireMock scenario states before
  every test, in the same window as `additionalBeforeAppLaunchSection` (i.e. before
  `ActivityScenario.launch`), so scenarios read at app start — `/v1/networks/providers`, stories — are
  covered. So no `resetWireMockScenarioState(...)` / `resetWireMockScenarios()` in
  `additionalAfterSection` / `additionalBeforeSection`, and no defensive reset as a first step;
  `additionalAfterSection` is for **non-WireMock** cleanup only (system properties, clipboard, network
  toggles). A reset *inside* a test body is legitimate only when it is part of the scenario under test
  (e.g. "error state → reset → pull-to-refresh → content loads"); `MainScreenActionButtonsTest` and
  `SwapStoriesTest` are the only such cases today.
- **The reset covers scenario state, not the request journal.** `/__admin/scenarios/reset` leaves
  `/__admin/requests` untouched, so `getWireMockRequestCount(...)` still sees calls made by earlier tests
  in the run. Always assert on a **delta** (`countBefore` → act → `countAfter`), never on an absolute
  count — mirror `TangemPayTransactionsTest` / `TangemPayBalanceSyncTest`.
- **The reset is global for the WireMock instance it hits.** Harmless on CI, where each emulator gets its
  own container, and locally when you run your own instance. But without a `wiremockBaseUrl` arg tests hit
  the **shared remote** WireMock, and the run then resets scenarios out from under everyone else using it
  — so pass `wiremockBaseUrl` when running locally (see `reference/running-and-debugging.md`).
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

### Page-object matchers: exhaust the native Kakao API before dropping to raw Compose

**Reviewers reject raw `composeTestRule` / `semanticsProvider.onNode(...)` / `onAllNodes(...)[i]` and
deep nested matchers when a native Kakao-Compose mechanism does the same thing.** Before writing any
such construct, look for the built-in KNode / `ViewBuilder` / `KLazyListNode` API — it almost always
exists. The raw form is a last resort, and even then it stays **inside the page object**, never in the
test body (the test only calls page-object members and scenarios — no `composeTestRule`, no test tags,
no `onNode`/`onAllNodes`, no bare matchers leak into it).

Native first, by need:
- **N-th of several identical nodes** → `child { … ; hasPosition(index) }` (Kakao maps
  `NodeMatcher.position` → `onAllNodes(matcher)[index]` for you). Do **not** hand-roll
  `semanticsProvider.onAllNodes(matcher)[index]`.
- **Scroll to index / matcher / key** → inside a KNode block: `knode { performScrollToIndex(index) }`,
  `knode { performScrollToNode(matcher) }`, `knode { performScrollToKey(key) }` (mirror
  `MarketsPageObject.scrollToListedOnBlock`). These wrappers are `@ExperimentalTestApi`, so annotate the
  page-object method `@OptIn(ExperimentalTestApi::class)` — that opt-in is expected, not a smell.
- **Relationship filters** → `ViewBuilder` DSL inside `child { }`: `hasAnyChild`, `hasAnySibling`,
  `hasAnyAncestor`, `hasAnyDescendant`, `addSemanticsMatcher(matcher)`, `useUnmergedTree = true`.
- **Lazy list / pager item (esp. below the fold)** → `KLazyListNode` + `childWith { … }` / `childAt(index)`
  (see `AddFundsBottomSheetPageObject`, `BuyTokenPageObject`), not a manual scroll + `onAllNodes`.
- **A raw Compose-Test op with no KNode wrapper** (e.g. `captureToImage()`, and any other
  `SemanticsNodeInteraction` extension Kakao doesn't surface) → do **not** fall back to
  `composeTestRule.onNode(hasTestTag(...))` in the scenario. Every KNode exposes a public `delegate`, and
  the built-in actions/assertions are all just `delegate.perform(type) { <this: SemanticsNodeInteraction> }`
  / `delegate.check(type) { … }`. `ComposeOperationType` is an open interface, so declare a tiny private
  `enum class Xxx : ComposeOperationType { … }` and reach the underlying `SemanticsNodeInteraction` from a
  **page-object method** — reusing an existing KNode (its testTag + `useUnmergedTree`). Capture a return
  value via a `lateinit var` written inside the lambda. Example (`TokenReceiveQrCodeBottomSheetPageObject.captureQrCodeBitmap`):
  ```kotlin
  fun captureQrCodeBitmap(): Bitmap {
      lateinit var bitmap: Bitmap
      qrCode.delegate.perform(QrCodeAction.CAPTURE) { bitmap = captureToImage().asAndroidBitmap() }
      return bitmap
  }
  private enum class QrCodeAction : ComposeOperationType { CAPTURE }
  ```
  This keeps `composeTestRule` / test tags out of the scenario — the scenario just calls the page-object method.
- **Only if truly nothing fits** → `semanticsProvider.onNode(...)` / `onAllNodes(...)` (the escape hatch
  used by `MainScreenPageObject`), wrapped in a named page-object method with a one-line WHY comment.

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
  assertions, so `assert(false)` is a silent no-op. Use Kakao / Kaspresso assertions or
  `org.junit.Assert` — Truth is **not** on the androidTest classpath (it is for unit tests only).
- **Clipboard checks**: `assertClipboardTextEquals(expected, context)` from `common/utils/ClipboardUtils.kt`.
  Read displayed text via `KNode.extractText()` first if you need to compare against UI state.
- **Every test ends with a meaningful assertion**, not just an action. A test whose last step is
  "Click Submit" without verifying the result gets rejected.

### Waits and synchronization

- **Never use the shared `pullToRefresh()` from `common/extensions/UiDeviceExt.kt` on a Compose screen.**
  It is UiAutomator-based and is a **silent no-op** on Material3 `PullToRefreshBox` — `onRefresh` never
  fires, no request is made, and the screen keeps serving stale data while the test happily continues.
  Use a Compose gesture instead: `pullToRefreshTokenDetails()` / `pullToRefreshTangemPay()`, or write the
  same shape for your screen (`onNode(hasTestTag(CONTAINER)).performTouchInput { swipeDown(...) }`).
  Details in `reference/compose-traps.md`. The trigger for this rule is **calling the helper**, not
  recognising the widget — you usually cannot tell from the test which refresh container the screen uses.
- **A negative assertion never proves an action landed.** `assertDoesNotExist()` / `assertIsNotDisplayed()`
  pass just as happily when the element is missing for an unrelated reason (not rendered yet, scrolled
  away, wrong screen). Using one as the "did the refresh happen?" check silently validates nothing. Prove
  an action took effect with a **positive** change (the new value/state appears), or with the WireMock
  journal (`/__admin/requests`) showing the request the action was supposed to trigger.
- **Manual polls are banned** (`onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()` in a loop) — even
  if a bot reviewer suggests one.
- **`flakySafely` only retries its allowed exception types — throw `AssertionError`, never `require()`/
  `error()`.** A `require()` raises `IllegalArgumentException` and `error()` an `IllegalStateException`;
  neither is in the retry set, so the block runs **once** and the test fails on the first miss while
  looking like it retried for the full timeout. Custom wait conditions must be written as
  `if (notReadyYet) throw AssertionError("…")`.
  **Diagnostic:** a step wrapped in `flakySafely(LONG)` that fails in a few seconds did not retry —
  compare the failure time against the timeout before believing "it retried and never succeeded".
- **Wait on a signal from the same data path as the assertion.** Refresh handlers fan out into parallel
  coroutines (`TokenDetailsModel.onRefreshSwipe` re-reads balance, transaction history and staking in
  three `async` blocks), so "the history updated" does not mean "the balance finished loading". Waiting
  on the wrong one lets the test proceed mid-refresh and assert against stale state. Pick the value the
  assertion actually depends on — e.g. wait for the *balance* to change before asserting a button whose
  enabled state is derived from the balance.
- **Default in the test body: `flakySafely(TIMEOUT) { assertion }`** — the codebase idiom (hundreds of
  uses); reviewers prefer it over `composeTestRule.waitUntil { runCatching { … }.isSuccess }`.
- **`ComposeNotIdleException` / `AppNotIdleException` ("busy for ~60s") is usually a sick emulator, not
  your test.** After many back-to-back local runs the emulator degrades (you may even see a "System UI
  isn't responding" ANR), and idle-synced ops (`flakySafely`, `waitForIdle()`, Kakao actions) start
  timing out *anywhere* data is loading — different test each run. Before concluding a test is flaky or
  that a screen "never idles", **cold-boot a fresh emulator** (`emulator -avd … -no-snapshot -wipe-data
  -memory 4096 -cores 2`) and re-run. A suite that flaked across runs on a tired emulator can be a clean
  10/10 on a fresh one (verified on this exact suite). Don't rewrite waits to work around emulator rot.
- **In scenario / `BaseTestCase`-extension code, `flakySafely` is NOT available** regardless — use
  `BaseTestCase.awaitSuccess(timeoutMillis = WAIT_UNTIL_TIMEOUT) { … }` (a shared member on `BaseTestCase`,
  no import needed) which wraps `composeTestRule.waitUntil { runCatching(block).isSuccess }`. Each async step
  reads `awaitSuccess { onXxxScreen { field.assertExists() } }` before the action. For appearance-only waits
  `composeTestRule.waitUntilAtLeastOneExists(matcher, timeout)` (or `{ a exists || b exists }` for either/or)
  is also fine.
  **Do not re-declare a private `awaitSuccess` in a scenario file** — the shared `BaseTestCase.awaitSuccess`
  already exists; older files may still have a private copy, don't copy that pattern.
- **Right-size the timeout — don't stamp `WAIT_UNTIL_TIMEOUT_LONG` on every step.** The timeout is a
  *ceiling*, not a sleep (`waitUntil` returns the moment the condition holds), but the default
  `WAIT_UNTIL_TIMEOUT` (20 s) already dwarfs a normal async transition. Reserve `…_LONG` / `…_VERY_LONG`
  for steps that are genuinely slow (a real network round-trip + debounce that can approach 20 s);
  using LONG uniformly is lazy and hides which step is actually the slow one.

### Comment hygiene

**Default: write no comments.** Reviewers read a comment-heavy diff as generated ("looks like Claude
just generated it" — the review on PR #6434, where 11 of 12 added comment lines had to go). This
covers every file you touch: tests, scenarios, page objects, and production / `src/mocked` code.

Before keeping a comment, try to delete it:

1. Can a name say it? Rename the val / function / matcher (`buttonOutsideDialog` instead of a
   comment explaining the dialog filter).
2. Does a `step("…")` name, the function name, or the constant name already say it? Then the comment
   is a duplicate.
3. Would someone break or revert the code without it? Only then keep it, in **one line**.

Never write:

- KDoc that paraphrases the function or constant name (`/** Makes the token expired */` on
  `expireTangemPayAccessToken()`, `/** Mirrors KEY in Foo */` on a constant of the same name).
- A comment in production or `src/mocked` code that explains how a UI test uses it.
- The same comment in two places, or a comment block of two lines or more.
- A comment that restates the next `step(...)` text or narrates the change.
- An explanation of a deviation from the manual case — that goes into the report and Allure, not code.

What earns its keep (one line, a hidden constraint the code cannot show):
`// Raw Compose on purpose: a Kakao check here waits out Kaspresso's 10 s flaky-safety retry.`

`scripts/check.sh` now flags `comment-block`, `comment-duplicate` and `comment-restates-code` as
errors in **every** changed `.kt` file. A `comment` / `kdoc` warning is resolved by deleting the comment;
keeping one needs the step-3 reason, not just "it states a WHY".

## Reference docs

- **`reference/pitfalls.md`** — read before writing every test (workflow step 3). Locator, timing and
  mock traps that already broke Android UI tests: merged vs. unmerged trees, disabled-button labels,
  screen-wide tags, swallowed clicks and holds, version-gated toggles, `MockAware*` repositories,
  iOS-built fixtures, traffic that bypasses WireMock.
- **`reference/reviewer.md`** — the prompt for the fresh-context reviewer subagent in workflow step 6.
  Pass it verbatim plus the inputs it lists; don't paraphrase it.
- **`scripts/check.sh`** — the mechanical self-review (workflow steps 6 and 8). Prints `report.txt` and
  a `review.diff` covering the Android and mocks repos; exits non-zero on any `error`.
- **`reference/compose-traps.md`** — read when the screen uses `PullToRefreshBox`,
  `TangemHoldToConfirmButton`, a Decompose model that fetches in `init {}`, a hot-wallet import with
  an access code, or a target inside a **LazyColumn/LazyRow that may be below the fold** (use a
  `KLazyListNode` matcher that auto-scrolls — never a manual swipe). These have silent failure modes that
  look like passing tests.
- **`reference/running-and-debugging.md`** — read when building, installing, running tests (orchestrator
  vs. raw `am instrument`), running against a local WireMock, interpreting CLI/Allure output, using
  `@Ignore`, or driving WireMock scenarios. Includes how to find app-side root causes when the UI fails
  silently (the app log in `files/log.txt`, and the WireMock journal).
- **`reference/yield-mode.md`** — read before writing any **Yield Mode (yield-supply / "Earning")** test.
  Covers the hot-wallet activation flow, why Ethereum (not Polygon — gasless), the full mock set +
  scenarios, the `isActive`-semantics / lowercase-address / hold-timing / Pill-testTag gotchas, and the
  production testTags already added. Mirror `tests/yield/YieldModeTest.kt`.