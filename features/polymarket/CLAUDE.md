# Polymarket — feature

"Predictions" in the app: a prediction account on the wallet screen, an entry gate that onboards a
Polymarket deposit wallet, a discovery feed of events, event details, order placement (buy) and position
close (sell), positions, funds in and out. API/impl split: `:features:polymarket:api` holds
`PolymarketComponent`, `PolymarketWalletBlockComponent` and `PolymarketFeatureToggles`; everything else is
`:features:polymarket:impl`.

The other halves of the module knowledge base: `domain/polymarket/CLAUDE.md` (identities, invariants,
signing) and `data/polymarket/CLAUDE.md` (contracts, wire traps, stores). Read the domain invariants before
adding any screen that can sign, prompt or move funds.

```bash
./gradlew :features:polymarket:impl:testDebugUnitTest
./gradlew :features:polymarket:impl:detekt
```

**Toggle:** `AND_16204_POLYMARKET_ENABLED` (`PolymarketFeatureToggles.isPolymarketEnabled`), `"version":
"undefined"` in `feature_toggles_config.json` — dev builds only until release.

## How it plugs in

- **Entry points** — `AppRoute.Polymarket(userWalletId?)` built in `app/.../ChildFactory` from
  `PolymarketComponent.Factory`. Pushed from the wallet screen's prediction account row
  (`DefaultWalletRouter`) with a wallet, and from the feed's Predictions block (`PredictionsBlockComponent`)
  without one — then the entry step asks which wallet.
- **Wallet screen row** — `PolymarketWalletBlockComponent` renders one lazy-list item
  (`PREDICTION_ACCOUNT_ROW_ID`); `features/wallet/impl` converts `PredictionAccountStatusValue` into
  `PolymarketWalletBlockUM` (`PolymarketWalletBlockConverter`) and owns the click. The balance arrives
  formatted because formatting needs the app currency.
- **Swap flow** — Add funds (swap) and Withdraw are `AppRoute.Swap` with
  `accountFlow = AccountFlow.PredictionTopUp` / `PredictionWithdraw`; the Polymarket-specific branches
  live in `features/swap/impl` (`SwapModel`, `InitialCurrenciesResolver`, `AccountFlowPresentation`,
  `SwapTransferStateBuilder`). Withdraw is signed there through `SubmitPredictionWithdrawalInteractor` and
  ends on `SwapRoute.Success` with `PredictionWithdrawalTracker` polling in the background — there is no
  status screen. A top-up sent from swap opens `PredictionFundingWindow`.
- **Receive** — `TokenReceiveComponent` (`features/token-recieve`, package `tokenreceive`) in a slot of `PolymarketMainComponent`,
  configured by `PolymarketReceiveConfigFactory` with the bridge deposit address and the `minCheckoutUsd`
  limits notification.
- **DI** — `di/PolymarketBindsModule.kt` holds four modules: `PolymarketBindsModule` (component
  factories), `PolymarketModelModule` (every model `@IntoMap @ClassKey`), `PolymarketFeatureTogglesModule`,
  `PolymarketDomainUseCasesModule` (use cases / interactors that need feature-side dependencies).
- **Analytics** — `AnalyticsParam.ScreensSources.Predictions` as the screen source of the swap routes
  (top-up, withdraw) and of the add-funds method event; the receive sheet carries no screen source;
  `PredictionAddFundsAnalyticsEvent` is the only event hierarchy in the module. Events are an analysts'
  contract — do not invent names, leave a `TODO` with the ticket instead.

## Navigation

`DefaultPolymarketComponent` owns a `childStack<PolymarketRoute>` (`serializer = null`) behind an
`InnerRouter`; unknown routes fall through to the app router. Every child is created directly (no assisted
factory) with `childByContext(componentContext, router = innerRouter)`.

```
Entry                       PolymarketEntryComponent      settles the wallet (PortfolioSelector sheet when none given)
└─ Onboarding(wallet)       PolymarketOnboardingComponent the gate: Welcome screen, onboarding run, region restrictions
   └─ Main(wallet)          PolymarketMainComponent       feed + balance header + positions preview; slots: AddFunds, Receive
      ├─ Search             PolymarketSearchComponent     search-as-you-type, 500 ms debounce, min length
      ├─ EventDetails       PolymarketEventDetailsComponent all markets of one event
      │  └─ PlacePrediction PlacePredictionComponent      Params.Buy — its own childStack: Amount → Slippage? → Summary → Status
      ├─ Positions(tab)     PolymarketPositionsComponent  ACTIVE | FINISHED, PolymarketPositionsSession per visit
      │  ├─ PositionDetails PolymarketPositionDetailsComponent open position, close gate, buy-more gate
      │  │  ├─ ClosePosition PlacePredictionComponent     Params.Sell — same flow, side fixed to SELL
      │  │  └─ PlacePrediction PlacePredictionComponent   Params.Buy — buy more of the same outcome
      │  └─ PositionResult  PolymarketPositionResultComponent resolved position (won/lost)
```

Routes carry `userWalletId` on purpose: the entry gate may settle a *different* wallet than the one the
feature was opened with, so children never read it from the feature params.

## The entry gate

`PolymarketEntryModel` settles the wallet, `PolymarketOnboardingModel` decides what to show:

1. `ResolvePolymarketEntryInteractor.withoutPrompting` on open → `Onboarded` goes straight to `Main`;
   `Onboard`/`Undetermined` show Welcome with an idle button ("Start" vs "Continue" from the backend
   status); `Reconnect` shows its own screen (`PolymarketOnboardingUM.Reconnect`, "Reconnect" button) for a
   ready account whose credentials are not on this device. **Opening the gate prompts for nothing** — no
   NFC, no biometrics.
2. The button always runs `CheckNewPolymarketAccountAllowedInteractor`. It reads the status by wallet id and
   lets a deployed account through without a region check; only a wallet that still needs a deploy asks the
   geoblock: `REGION_BLOCKED` shows the region-restrictions screen, `UNDETERMINED` (geoblock unreadable)
   refuses with a snackbar. When the status read itself fails the interactor falls back to the geoblock
   anyway, so during a BFF outage an existing account without a local confirmation record is gated like a
   new one — fail-closed by design.
3. Then `RunPolymarketOnboardingInteractor` — one signing session (card for Cold, unlock for Hot), progress
   collapsed into the button spinner; a failure leaves the screen idle and reports through a snackbar. The
   screen's button is the only retry.

Region-restricted users (`PolymarketAccessMode.READ_ONLY`) still reach `Main`, browse and withdraw; the
place-prediction flow re-checks the region itself before an order.

## Main screen

`PolymarketMainModel` composes: categories + paginated events (`GetPolymarketEventsBatchFlowUseCase`,
a failed or empty first page is retried once silently), the balance header from
`PredictionAccountStatusSupplier` (`PredictionBalanceUMConverter`), the deposit requisites
(`PredictionFundingRequisites` wrapper over `RefreshPredictionDepositRequisitesInteractor`), the
positions preview (`PolymarketPositionsPreviewLoader`, polled every 20 s), and
`PredictionBalancePoller.subscribe` for as long as the screen is shown. Pull-to-refresh refreshes status and requisites together.

The header shows one of two figures, toggled by tapping its label (`PredictionBalanceMode`): **Available**
(the CLOB collateral, `Active.balance`, the default) and **Total** (`Active.totalBalance` = collateral + the
data-API value of the open positions). Total is offered only once a full refresh has read `positionsValue`;
until then the header shows Available and the toggle is disabled, but the chosen mode is kept and comes back
as soon as the value does. The wallet screen and the wallet total keep counting the available
balance only, as on iOS.

- **Add funds** is gated: `RequisitesState.Loading` ignores the tap, `Failed` shows a dialog, `Ready`
  opens the sheet — swap (`AccountFlow.PredictionTopUp`) or receive. Both need a confirmed address
  (`activeStatus.cryptoCurrencyStatus`); without it a snackbar reports an unknown error rather than
  offering an address that may be wrong.
- **Withdraw** pushes swap with the collateral as FROM (`AccountFlow.PredictionWithdraw`), available in
  read-only mode too.
- **Awaiting-funds banner** follows `PredictionFundingWindow.isOpen`; the poller drops to 5 s while open.
  Opened by `SwapRoute.Success` and a placed order, never by Receive (an external transfer's timing is unknown).
- A position row opens `PositionResult` when resolved (`canOpenPositionResult`), `PositionDetails` when
  still open (`canOpenPositionDetails`), nothing otherwise.

## Order flow (`placeprediction/`)

`PlacePredictionComponent` owns Amount → Slippage → Summary → Status. Each step has its own retained model.
Status receives the result, market header and position id as route data and owns the position indexing wait.

- **Confirmation boundary.** Amount creates `PreparedPrediction` synchronously on Next and cancels its
  current quote request. The snapshot carries the displayed state, quote receipt time and signing
  parameters. Summary displays and signs that snapshot; it has no price or quote use cases. Balance and
  position validation can still update its eligibility to submit. Back returns to the retained Amount
  model and immediately starts fresh spending-power and price reads.

- **Quote loop.** Amount entry is debounced 500 ms, then one loop runs every 3 s
  (`QUOTE_POLL_INTERVAL_MILLIS`) and decides on each turn whether to ask `GetPredictionOrderQuoteUseCase`
  for a quote or `GetPredictionPriceUseCase` for a price — a quote while the amount is quotable, a price
  while it is empty or below the minimum. The quote is never cached. The loop is bound to the Amount step
  (`PlacePredictionAmountComponent` `doOnResume` / `doOnPause`), so it runs only while that screen is on top
  and in the foreground. Slippage returns an explicit result to Amount and invalidates its old quote
  when the value changes; returning to Amount starts the next request immediately.
  `ValidatePredictionOrderUseCase` refuses a quote older than 60 s after signing, before submission.
  Summary shows the generic place-error dialog and stays on the same screen. Two independent gates decide whether
  it is placeable: `quote.isPlaceable` (market state; a SELL also places on `BELOW_MIN_ORDER_SIZE`) and
  `isBelowMinAmount` (BUY only, price-weighted — a SELL has no floor beyond one whole share or the whole
  position).
- **Shares.** `SharesCalculator`: the sellable size is the position floored to the 2-decimal exchange grid
  (`PolymarketConst.SHARES_DECIMALS`); presets and ± steps move in whole shares with a floor of one share
  (or the whole position when smaller), and 100 % is the position itself, so a full close reaches the
  last fraction.
- **Slippage.** Default 3 %, range 0.1–30 % (`SlippagePickerUM`); a fill at the worst-case price counts as filled.
- **Region.** Read by Amount (`TradingPermissionUM`), re-read on its resume only while `Unknown`.
  A permitted order carries that decision in its internal confirmation snapshot; external entry routes
  do not supply it, and it is not re-read before signing. An unreadable region keeps the
  gate shut as `Unknown` — the order is refused, the user is not told they are restricted.
- **Placing.** `SignPredictionOrderInteractor` (one signing session: card for Cold, unlock for Hot) →
  `PlacePredictionOrderUseCase`.
  Outcomes: filled (with the actual filled amounts); `Delayed` → `AwaitDelayedPredictionOrderUseCase` polls
  `GET data/order/{id}`; `Ambiguous` (transport failure after send) is shown as **Submitted**, never as
  failed — the order may be live; `NoLiquidity` (fill-and-kill found no match) is shown as NotFilled.
  Any result that moves the balance (`Filled` / `PartiallyFilled` / `Submitted`) opens
  `PredictionFundingWindow`, so the feed shows the awaiting-funds banner and polls faster until the balance
  settles.
- **Position reads.** `/positions/outcome` may answer `null` until the position is indexed.
  `PolymarketPositionDetailsModel` shows Unavailable at once when the first read is `null`; the retry loop
  (`POSITION_INDEXING_READS` reads, `POSITION_INDEXING_RETRY_MILLIS` apart) serves a position already shown
  whose size has not caught up with the last order, and pops the screen if it disappears. The sell flow
  reads the position on open and again at signing.
- **View position.** After a buy fills (`Filled` / `PartiallyFilled`), the result screen reads the created
  position back itself — `PolymarketPositionId(market.conditionId, assetId)`, so a market the BFF gave no
  `conditionId` for offers no button. `ViewPositionUM` drives it: `Awaiting` shows the button with a loader
  and keeps it disabled, `Available` enables it, `Hidden` leaves the single To main button — including when
  the read comes back a position the details screen would refuse (`canOpenPositionDetails`). The wait is `CREATED_POSITION_READS`
  (15, ~30 s) rather than the details screen's 5, because the index answers `null` for several seconds even
  for an outcome the wallet already held — a read that fails is retried like a missing one. The button
  replaces the whole stack with Main + the position details, so back from the position lands on the feed
  rather than on the result of an order already placed.
- **Sell** (`Params.Sell`) validates at signing time that the position still exists, the market accepts
  orders and enough shares remain after grid rounding. SELL legs: `makerAmount = shares`,
  `takerAmount = collateral`.

## Positions

`PolymarketPositionsSession` (domain) is one visit: pages are retained across pause/resume and refreshed
in place on resume (`refreshLoadedPages`); a filter change keeps the retained pages, re-filters them and
requests more only to fill a page (`selectFilter`); disposal ends it. `PolymarketPositionsLoader` renders
it. The BFF only distinguishes the ACTIVE and FINISHED endpoints (`PolymarketPositionsTab`); no status
parameter is sent, and `PolymarketPositionsFilter` (`ALL/ACTIVE/WON/LOST/DISPUTED/PENDING/SPLIT`) is applied
locally to the fetched pages through `PolymarketPositionStatusConverter`. A position is identified by
`PolymarketPositionId(conditionId, assetId)`.

The close gate (`PolymarketPositionDetailsConverter.canClosePosition`): not resolved, no result, not
redeemable, `marketId` known, `isAcceptingOrders`, `size > 0`. A row whose market lookup failed has no
`marketId` and must not build `ClosePosition`. iOS gates on `isAcceptingOrders` + market ids only.
Buy more (`PolymarketPositionDetailsModel.buyableMarket`) needs the position `OPEN`, its event loaded and
the market holding that outcome still `isAcceptingOrders`; it pushes the ordinary `PlacePrediction` route.

`PolymarketPositionResultComponent` renders a resolved position (WON / LOST / SPLIT); `canOpenPositionResult`
requires `RESOLVED` and, for WON / SPLIT, `isRedeemable`, so a position already redeemed elsewhere opens
nothing. **Claiming (redeeming) a won position is not implemented** — [REDACTED_TASK_KEY] (the redeem transaction)
and [REDACTED_TASK_KEY] (the claim action on the result screen).

## Gotchas

- **Never pass `this` as a child context.** Every child gets `child(key)` or `childByContext(...)`; models
  are keyed by class in the `InstanceKeeper`, and two children on one context collide.
- **The feed's `userWalletId` is the route's, not the params'.** `PolymarketComponent.Params.userWalletId`
  is nullable and may be a wallet the gate rejected.
- **`Undetermined` is not "no account".** A wallet onboarded on another device lands on Welcome too;
  pressing Start derives the address and finds the account. Don't add copy that says "create".
- **`READ_ONLY` explains itself as a region restriction.** Reuse it for another cause and the UI lies.
- **Quotes (BFF `POST /orders/quote`) and prices (CLOB `GET price`) arrive as decimal strings.** Parse
  with `BigDecimal`, compare with `compareTo`, never `==`.
- **Times are Eastern** (`PolymarketConst.EASTERN_TIME_ZONE`) for market end dates.
- **`PolymarketMainModelTest` and `PlacePredictionModelTestBase` are the reference tests** for a model in this
  module: `ParamsContainer` fed by hand, Turbine over `uiState`, relaxed mocks only for router, message
  sender and the flows the test is not about.
- Pull-to-refresh on the feed refreshes the region, the account status and the requisites together; the
  feed pages are not re-requested — they are kept per visited category and updated by `PolymarketFeedPoller`.

## Not implemented / open

| Item | Ticket |
|---|---|
| Claim (redeem) of a won position: transaction and claim action | [REDACTED_TASK_KEY], [REDACTED_TASK_KEY] |
| Drop L2 credentials on CLOB `401` and route to the gate | [REDACTED_TASK_KEY] |
| Tradezone PnL (`/v1/leaderboard`) | not ticketed |
| Withdraw beyond USDC · Polygon (phase 2), withdrawal tracker on cold start, explorer link on withdraw | not ticketed |
| Prediction-withdraw analytics event | waiting for analysts |