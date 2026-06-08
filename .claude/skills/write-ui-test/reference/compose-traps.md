# Compose UI test traps

Each of these has a **silent** failure mode: the gesture/action appears to run, the test stays green
(or fails for the wrong reason), but the intended behavior never fired. Diagnose with logcat network
traces or a semantics-tree snapshot, not by visually watching the swipe.

## Material3 `PullToRefreshBox` + UiAutomator swipe = silent no-op

`androidx.compose.material3.pulltorefresh.PullToRefreshBox` reacts to overscroll deltas via Compose's
`NestedScrollConnection` from the inner `LazyColumn`. UiAutomator's `device.swipe(x1,y1,x2,y2,steps)`
dispatches platform `MotionEvent`s; the `LazyColumn` receives them as an ordinary scroll, never
produces overscroll, and `onRefresh` never fires — regardless of `steps=30` (fling) or `steps=1000`
(slow drag). Confirmed by `NetworkLogs`: zero refresh calls after the UiAutomator swipe, vs. one
immediate call via the Compose Test API.

**Use the Compose Test API:**

```kotlin
composeTestRule.onNode(hasTestTag(SOME_TAG_INSIDE_THE_BOX))
    .performTouchInput {
        swipeDown(startY = 0f, endY = visibleSize.height.toFloat() * 6f, durationMillis = 800)
    }
```

The shared `pullToRefresh()` in `common/extensions/UiDeviceExt.kt` is UiAutomator-based and works for
*some* screens (a different refresh container), but **not** for Material3 `PullToRefreshBox`. When
porting a test, verify with a logcat network trace, not visual inspection.

## `TangemHoldToConfirmButton` semantics are minimal

The component exposes ONLY `TestTag`, `IsContainer`, `Shape` in Compose semantics — no `Disabled`,
`Role`, or `OnClick`. `assertIsEnabled()` / `assertHasClickAction()` are useless on it.

`Modifier.holdToConfirmGestures(enabled, ...)` early-returns from `pointerInput` when `enabled=false`,
so the hold gesture is silently swallowed: the button looks fine, the user holds, nothing happens,
`onConfirm` never fires.

**Diagnose "silently disabled" from a test:**
1. Snapshot the Compose semantics tree before the hold.
2. Perform the hold: `performTouchInput { longClick(durationMillis = HOLD_DURATION_MS) }`.
3. Snapshot again — byte-identical trees mean `onConfirm` didn't run.
4. Or check WireMock request stats for the downstream API call expected after `onConfirm`.

## Asserting enabled/disabled on a `Modifier.clickable` row

When a settings/list row puts `Modifier.clickable(enabled = isClickable, ...)` on the row **container**
(not the title `Text`), the enabled/disabled state lives on that container; the child Texts only carry
`testTag`/text. So `assertIsEnabled()` / `assertIsNotEnabled()` must target the container, matched by a
descendant text — not the title node itself.

Match the container in BOTH states with **click-action OR disabled-semantics**. Do NOT rely on
`hasClickAction()` alone: depending on the Compose version a `clickable(enabled = false)` row may not
expose an onClick action, so a `hasClickAction()`-only matcher finds no node and `assertIsNotEnabled()`
fails with "No node found".

```kotlin
import androidx.compose.ui.test.hasClickAction as withClickAction
import androidx.compose.ui.test.isNotEnabled as withDisabled

val row: KNode = child {
    addSemanticsMatcher(withClickAction() or withDisabled())          // matches enabled AND disabled rows
    hasAnyDescendant(withText(getResourceString(R.string.row_title)))  // narrows to the specific row
    useUnmergedTree = true
}
// enabled card:  row.assertIsEnabled() ; disabled card:  row.assertIsNotEnabled()
```

## `assertTextContains(x)` defaults to exact-segment match, not substring

`SemanticsNodeInteraction.assertTextContains(value, substring = false, ignoreCase = false)` defaults to
`substring = false` — it asserts that some text **segment of the node equals `value` exactly**. Matching
a symbol or fragment inside a larger string (e.g. `"€"` against a balance `"€108,474.21"`) silently
never matches and times out inside a `waitUntil`. Pass `substring = true`:

```kotlin
totalBalanceText.assertTextContains("€", substring = true)
```

Reference tests that pass the *full* string (`assertTextContains("€108,474.21")`) work with the default,
which is why a copy-pasted matcher can mislead.

## Kakao-Compose `child { }`: use DSL matchers, not raw Compose matcher aliases

Inside a `child { … }` / `ComposeScreen` element builder, call the DSL methods (`hasText(...)`,
`hasTestTag(...)`, `hasAnyDescendant(...)`). A common alias is `import androidx.compose.ui.test.hasText
as withText` — but `withText(x)` as a **bare statement** inside the builder just creates a
`SemanticsMatcher` and discards it, registering nothing → `ViewBuilderException: Please set matchers for
your Element!` at run time. `withText`/raw matchers are only valid as *arguments* to a DSL method
(`hasAnyDescendant(withText(name))`), never as a standalone line.

```kotlin
// WRONG — no matcher registered
fun walletNameValue(name: String) = child { withText(name); useUnmergedTree = true }
// RIGHT
fun walletNameValue(name: String) = child { hasText(name); useUnmergedTree = true }
```

## Decompose model lifecycle vs. data refresh

Models (e.g. `TangemPayDetailsModel`) call data fetches from `init {}`, NOT on `ON_RESUME`. Returning
to a screen via `router::pop` does NOT re-fetch. A test that switches WireMock scenarios between an
action and the assertion MUST explicitly trigger a refresh on the now-frontmost screen — otherwise the
stale in-memory data wins.

## Terminal screen never reaches Compose-idle: self-feeding `StateFlow` loop

A screen whose model writes a fresh state back into the same `StateFlow` it observes will recompose
forever, so **any** Compose/Espresso assertion on it times out with `ComposeNotIdleException`
(`autoAdvance=true`) or `AppNotIdleException` "last message = DispatchedContinuation target=Handler"
(`autoAdvance=false`). The classic shape (hit on the send-v2 `ConfirmSuccess` screen, [REDACTED_TASK_KEY]):

```kotlin
combine(uiState, currentRoute)
    .onEach { (state, _) -> callback.onResult(state.copy(navigationUM = NavigationUM.Content(onClick = { … }))) }
    // callback writes back into uiState → emits again → onEach again → ∞
```

`NavigationUM.Content` is a `data class` whose fields are **lambdas**, recreated every pass → `equals`
is always false → `StateFlow` never dedups → unthrottled loop. No test-side workaround helps (it's an
app loop): not `flakySafely`, not longer timeouts, not mocking external sources, not UiAutomator
(touching the window mid-async-signing aborts the send). **Fix is app-side** — emit once (guard the
`filter`/`distinctUntilChanged` so the self-induced field is ignored). If you see `ComposeNotIdle` on a
*static-looking* success/result screen, suspect this before blaming background polling.

## Animation-gated content via `delay()` never appears under the test clock

Compose UI tests run inside `runTest` — **virtual time**. A `LaunchedEffect { delay(600); visible = true }`
that gates the screen body behind `AnimatedVisibility(visible)` will *never* reveal it once the
composition is otherwise idle: `waitForIdle` sees no pending frame-clock awaiters, so it stops without
advancing the virtual clock to the delay's deadline. The body stays empty (you see only the parent
chrome, e.g. a top-bar close icon), the `testTag` is absent, and `assertIsDisplayed` fails as
"not displayed" — **after** burning the full wall-clock timeout. `flakySafely(LONG)` does NOT help:
it retries in wall-clock time while virtual time stays frozen.

Distinguish from the loop trap above: a `delay`-gate gives a clean `AssertionError: … not displayed`
(idle is reached, node just isn't there); the loop gives a `ComposeNotIdle`/`AppNotIdle` timeout.

Fixes: (a) app-side — drop the pre-`delay`, let the enter transition (`slideIn`/`fadeIn`) play on the
frame clock (which `autoAdvance` *does* pump); or (b) put the asserted `testTag` on a node **outside**
the `AnimatedVisibility` so the container exists from frame 0. A plain coroutine `delay` is not a
frame-clock awaiter, so advancing frames won't fire it — only `advanceTimeBy` (with `autoAdvance=false`)
would, which is fragile. Prefer the app-side fix.

## Hot wallet imports with access code

- `openMainScreenWithExistingHotWallet(seedPhrase, accessCode: String = "")` in `BaseScenarios.kt`
  handles both flows via the optional param — DO NOT introduce a parallel `importHotWalletWithAccessCode`.
- Access-code **create** and **confirm** screens share the same `ACCESS_CODE_INPUT` testTag. Gate the
  confirm-screen action on the confirm-screen's unique title:

  ```kotlin
  composeTestRule.waitUntilAtLeastOneExists(
      hasText(getResourceString(CoreUiR.string.access_code_confirm_title)),
      timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG,
  )
  ```

- Tangem Pay eligibility (`PaeraCustomer`) rejects hot wallets with `authType=NoPassword` — those tests
  must use the access-code path.