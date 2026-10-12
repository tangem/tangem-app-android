# Known UI-test pitfalls

Traps that have already broken Tangem Android UI tests. Read this before writing a test (workflow
step 3). The reviewer (`reference/reviewer.md`) checks the diff against the same list and against
`compose-traps.md`. Each item cost at least one full build-and-run cycle to find.

## Locating elements

- **Merged vs. unmerged tree decides where the text is.** On a clickable `Row` with
  `mergeDescendants`, the text is aggregated onto the tagged node only in the **merged** tree. With
  `useUnmergedTree = true`, `performClick()` still works but `hasText` / `assertTextContains` on that
  node never match, so the wait runs to its ceiling. Rows built from a plain `Layout` (Organize tokens,
  `TangemRowContainer`) are the opposite: the item node carries only the tag, and the title is a
  descendant. Do not copy a matcher from another screen. Dump both trees first
  (`onAllNodes(isRoot(), useUnmergedTree = …)[i].printToLog(tag, Int.MAX_VALUE)`).
- **A disabled button does not merge its label.** `child { hasTestTag(BaseButtonTestTags.BUTTON); hasText("Transfer") }`
  finds the button only while it is enabled. The click action merges the label into `BASE_BUTTON`; a
  disabled button keeps it in a separate `BASE_BUTTON_TEXT` node. Kakao then reports
  `… is not displayed!` for a button that is on screen. When the button can be disabled at that
  moment, match `BaseButtonTestTags.TEXT` with `useUnmergedTree = true`.
- **Before asserting enabled state, read the target's semantics in production.**
  `clickable(enabled = false)` keeps the `OnClick` action and adds `Disabled`, so
  `assertIsNotEnabled()` works and `assertHasNoClickAction()` fails. `ACTION_BUTTON` has three
  implementations, and the tag means something different on each:
  - `components/buttons/actions/Actions.kt`: the tagged node is the clickable. It exposes `Disabled`.
  - `ds/button/action/ActionButtons.kt`: the tagged `Column` gets `disabled()` but no click action.
  - `TangemPayActionButton` (Payment account): the tagged `Column` carries neither. The clickable is
    the inner `TangemButton`: match it with `withdrawButtonControl` / `addFundsButtonControl`.

  On a node with no click action, `assertHasNoClickAction()` passes in both states and proves
  nothing. `TangemHoldToConfirmButton` exposes neither (see `compose-traps.md`).
- **A screen-wide tag matches the previous screen too.** `TopAppBarTestTags.TITLE` alone matches every
  app bar, so "Assert confirm screen is displayed" passes while the test is still on the Address
  screen. Gate a transition on a tag or text unique to the target screen.
- **Tag names lie about where they are used.** `WalletConnectBottomSheetTestTags.WALLET_NAME` is emitted
  by the shared `PortfolioSelectRow` (also on Referral). The Add-to-wallet guide reuses the card's
  `CARD_DETAILS_*` tags. Grep a tag's production usages before relying on it, and scope with
  `hasAnyAncestor(hasTestTag(<screen tag>))`.
- **Not every sheet carries `BaseBottomSheetTestTags.CONTAINER`.** `TangemBottomSheet`, `TangemModal`
  and `BsContent` set it; `TangemModalBottomSheetWithFooter` (e.g. the WalletConnect connection sheet)
  does not. Check which sheet composable the screen uses before scoping by the container.
- **There are two back-button tags.** `TangemTopAppBar` sets `TopAppBarTestTags.CLOSE_BUTTON`, the DS2
  `TangemTopNavigation` sets `TopNavigationTestTags.BACK_BUTTON`, and screens migrate one by one. Use
  `tapBackButton()` (`common/extensions/NavigationExt.kt`), which matches either.
- **Hidden does not mean absent.** A `SubcomposeLayout` or a pager keeps hidden content composed: both
  card faces, both wallet pages on Main. Assert hidden with `assertIsNotDisplayed()`, never
  `assertDoesNotExist()`. A singular matcher on a two-page pager can resolve to the off-screen page and
  fail with "in scrollable container": match the displayed copy.
- **Assert the text as Android renders it.**
  - Crypto amounts carry a non-breaking space and at least two decimals (`KAS 0.20`). Copy the literal
    from a passing sibling test or check it with `hexdump -C` (`c2 a0`).
  - Organize-tokens titles read `Bitcoin BTC` and need `substring = true`.
  - Case matters: the yield history pill reads `Yield mode Enabled`.
  - Android names differ from iOS: TON is `Gram`, the Note mock is Dogecoin.
- **`extractText()` reads `Text`, not `EditableText`.** On a `BasicTextField` it throws. Use
  `assertTextEquals(...)` for text fields.
- **Some custom clickable rows reject touch injection** ("autoscroll did not help", "Failed to inject
  touch input"). Fire the action from the page object with
  `performSemanticsAction(SemanticsActions.OnClick)` on a node matched by tag and descendant text.

## Timing and state

- **Debounced input drops a single instant write.** The custom-token contract field uses
  `distinctUntilChanged().sample(1000).drop(1)`, so a whole `performTextInput` is the one sampled emission
  and gets dropped. Type with `performTextInputInChunks` (`common/extensions/KNode.kt`).
- **`clickWithAssertion()` checks *displayed*, not *enabled*, and never retries.** A click right after
  an input whose validation still has the button disabled is swallowed, and the test dies later on an
  unrelated assertion. Use `clickWhenEnabled()`, or the scenario that already waits
  (`openSendConfirmScreenViaNextButton()` / `…ViaContinueButton()`).
- **The hold-to-confirm button re-disables while the fee or quote reloads.** On EVM sends the fee is
  fetched twice. Call `waitUntilNetworkFeeIsStable { readNetworkFeeAmount() }` before
  `openSendSuccessScreenViaLongClickOnSendButton()`. The Tangem Pay withdraw quote refreshes every ~10 s,
  so the hold itself is retried (`confirmTangemPayWithdrawByHolding`).
- **A tap on a row that is still re-laying out is swallowed.** Main token rows re-lay out while the
  APY promo banner resolves. Open token details with `openTokenDetailsFromTokenList(title)`, which
  retries the tap. Do not add a wait before the click.
- **The Main balance is slow on CI:** 7–10 s median, up to ~30 s, because some RPC goes to public nodes.
  Balance-dependent assertions use `awaitSuccess(WAIT_UNTIL_TIMEOUT)`, as `synchronizeAddresses` does.
- **Every wait stays well under 180 s.** Marathon's `testOutputTimeoutMillis` is 180 000 ms. A test
  killed there is reported `incomplete`, not `failed`, and is re-queued twice. Mass `incomplete` in a
  nightly run means a shared precondition hangs (e.g. an optional hot-wallet onboarding screen that
  `openMainScreenWithExistingHotWallet` does not dismiss). Find the last `ComposeInteraction` in the
  Marathon logs before looking at individual tests.
- **Version-gated feature toggles follow the APK version, not the branch you think of.** On
  `releases/x.y` the version is the branch name. On develop it is the latest release plus one minor,
  and a failed resolution gives `1.0.0-SNAPSHOT`, which turns off every gated toggle. If a test is green
  on a release branch and red on develop, compare `Resolved versionName` in the *Build APKs* CI step.
  As of 6.4:
  - Shtorka 2.0 (`TWI_1608`) replaced the markets sheet. Open it through `MainScreenTestTags.MARKETS_SHEET_EXPAND_AREA`.
  - The new transaction history (`AND_15767`) shows swaps in the history, not in the express-status block.
- **The token selector remembers expanded accounts for the whole app session**
  (`ChooseTokenExpandedAccountsHolder`). A test that opens the selector twice finds the account already
  expanded the second time, so a second "expand the account" click fails or collapses it. Expand it
  only on the first opening.
- **A step that "took 10 minutes" may be an emulator clock jump.** `SystemClockTime: Setting time of day`
  in logcat means the device clock was re-synced mid-run, and Kaspresso step durations and log
  timestamps jump with it. Look for the real failure in the attempts after the jump.
- **Joda dates in a test-class field initializer crash the instrumentation process** (`Starting 0 tests`,
  no stack trace). The class is constructed before the Application initialises Joda's timezone
  provider. Use a `get()` property or a local `val`.

## Mocks

- **Launch-time endpoints need `additionalBeforeAppLaunchSection`.** `/v1/networks/providers` and
  `/v1/stories/*` are read while the app starts. A state set in the test body races that startup,
  and it wins often enough to look fixed. Set such states in
  `setupHooks(additionalBeforeAppLaunchSection = { … })` and confirm the served body in the journal.
- **`setWireMockScenarioState` fails silently.** WireMock returns 422 for a state that no mapping of that
  scenario names as `requiredScenarioState` / `newScenarioState` (including `Started`). The helper
  returns `false`, and the step still passes. A new state needs a mapping that declares it.
- **The mocked build can bypass WireMock entirely.** `MockAware*` repositories in `*/src/mocked/` serve
  canned data. Examples: Tangem Pay tx history, card reveal, VA order id `mock-va-order-id`, cashback.
  Grep `src/mocked` before debugging a screen that shows data the mocks do not contain. Opt out with
  the repository's system property (`uitest.tangempay.*`) instead of deleting it.
- **iOS-built fixtures often break Android parsing.** Android Moshi is stricter. Examples of required
  fields: `product_specification_data_type`, `result.balance` in `customer/me`, and cashback
  `currency` / `period` / `cashback_display_mode`. A shared mapping without a `User-Agent` guard can
  serve Android the iOS body. Add an Android variant matched on `User-Agent: okhttp/.*` with a lower
  `priority` number than the shared one. An equal-priority tie-break is not deterministic.
- **A renamed DTO field silently degrades the whole screen.** `cards[].card_id` → `id` emptied the cards
  list, and every Tangem Pay test failed at its first step as "issuing card". Run a known-good sibling
  test as a baseline, and read the status line in the app log (`files/log.txt`), before touching the test.
- **Response contracts the app validates:**
  - `/v1/exchange-data` must echo `{{request.query.requestId}}` and `{{request.query.toAddress}}`
    (`response-template`). Otherwise every DEX provider silently disappears from the swap sheet.
  - `/v1/quotes` needs `priceUsd`. Without it, incoming transactions are filtered out as dust.
  - An express history row renders only if `fromAddress` + `fromNetwork` + `fromContractAddress`
    match the open wallet and token.
- **Some traffic never reaches WireMock.**
  - The providers mock also lists live RPC nodes, so fees and balances drift with the real network.
    Pin them with a mock-only providers state (`networks_providers=BitcoinMockOnly`).
  - Third-party hosts missing from `WireMockRedirectInterceptor.REDIRECTABLE_THIRD_PARTY_HOSTS` hit the
    real service. SurveySparrow kept a rating across runs this way.

  If a request is absent from `/__admin/requests`, fix the routing, not the test.
- **Switching Wi-Fi and data off does not make the app offline.** WireMock runs over `adb reverse`
  (loopback). Drop the one endpoint the case is about with a `fault` scenario state.
- **Count journal hits by query parameter with `getWireMockRequestCountByQueryParam`,** not a
  `urlPattern` regex, which returned 0 from the test while working through curl.
- **Page sizes differ from iOS.** Tangem Pay history pages with `limit=50` on Android (20 on iOS). A
  20-item first page ends pagination at once.
- **A scenario step that looks redundant can be load-bearing.** Example: `USER_TOKENS='Wallet2'`
  before `openSendScreen` in `SendViaSwapTest`. Prove a removal is safe by re-running the test, not by
  reading the helper.