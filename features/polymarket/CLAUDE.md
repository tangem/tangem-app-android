# Polymarket — feature

UI rules of "Predictions" that the code does not make evident. Read the domain invariants
(`domain/polymarket/CLAUDE.md`) before adding a screen that can sign, prompt or move funds.

## Gating and wiring

- `TWI_1700_POLYMARKET_PHASE_TWO_ENABLED` gates all D2 behavior through
  `PolymarketFeatureToggles.isPolymarketPhaseTwoEnabled`: sports cards and scores, abbreviated outcome
  labels, live quotes, crypto screens and charts as they are connected. Gate both UI mapping/navigation
  and data subscriptions; hiding a component alone must not leave its D2 socket or polling active.
  With D2 off, preserve the D1 REST presentation and outcome click targets. Test both flag values at
  each integration boundary. Reusable data/socket implementations can remain flag-independent and cold.
- The feed's Predictions block is gated by Amplitude `twi_1576_polymarket = on`, read once per model.
  Accounts, balances and the wallet-screen row are not gated.
- Add funds (swap) and Withdraw are not screens of this module: they are `AppRoute.Swap` with
  `AccountFlow.PredictionTopUp` / `PredictionWithdraw`, and the Polymarket branches live in
  `features/swap/impl`. Withdraw ends on swap's success screen with the tracker polling in the background —
  there is no status screen.
- The wallet-screen row's balance arrives formatted because formatting needs the app currency.
- Analytics names are the analysts' contract — never invent one; wait for the spec.

## Navigation

- Routes carry `userWalletId` on purpose: the entry step may settle a *different* wallet than the one the
  feature was opened with, and `PolymarketComponent.Params.userWalletId` is nullable and may be a wallet the
  gate rejected. Children read the wallet from their route, never from the feature params.
- Opened from the feed without a wallet, the entry step asks which one.

## Entry gate

- **Opening the gate prompts for nothing** — no NFC, no biometrics (domain invariant 1).
- `Undetermined` is not "no account": a wallet onboarded on another device lands on Welcome too, and
  pressing Start derives the address and finds the account. Don't add copy that says "create".
- The button always runs the new-account check first; `REGION_BLOCKED` shows the restrictions screen,
  `UNDETERMINED` refuses with a snackbar. A run failure leaves the screen idle; the button is the only retry.
- Region-restricted users (`READ_ONLY`) still reach Main, browse and withdraw. `READ_ONLY` explains itself
  as a region restriction — reuse it for another cause and the UI lies.
- After the gate the region lives in the process-wide `PredictionRegionAccess`, refreshed by Main on open and
  on pull-to-refresh. While it is restricted, Main shows the banner, Add funds is dimmed, and every order
  entry (feed, search, event, Buy more, Close) shows the restrictions modal instead of opening. A failed check
  keeps the last answer and counts as allowed only when there is none; the Amount and Order steps still check
  on their own.
- Main returns here (`replaceAll(Onboarding)`) the moment the stored credentials are gone, even from under a
  child: nothing above Main can sign without them. So the submitting step (Summary or Order) answers an
  `Unauthorized` placement with no dialog and leaves the gate to explain it.

## Main screen

- **Available vs Total** (tap the header label): Available is the CLOB collateral (default); Total adds the
  positions value and is offered only once that value has been read — until then the toggle is disabled but
  the chosen mode is kept and returns with the value. The wallet-screen row and the wallet total count
  Total, as on iOS ([REDACTED_TASK_KEY]).
- **Add funds** needs a confirmed deposit address; without one, report an unknown error rather than offer
  an address that may be wrong. The buy amount step offers the same sheet, shut until the region check
  answers, and shows the account-status balance only once `ACTUAL`.
- **Withdraw** waits on the same funding requisites as Add funds (QA, [REDACTED_TASK_KEY]): disabled while they load,
  dimmed and answering with Add funds' error when they failed. Unlike Add funds it ignores the region.
- Failed requisites are re-requested when that error is dismissed (Main and the amount step) and on resume.
  A failed refresh fails them even after a success, as on iOS: the spec wants every refresh to end in Ready
  or Failed.
- **Awaiting-funds banner** opens on a completed top-up swap or a sent order, never on Receive.
- Pull-to-refresh refreshes region, positions preview and account status (with the positions value), then the
  requisites. Feed pages are not re-requested — they are kept per visited category and refreshed by
  `PolymarketFeedPoller`.
- A position row always opens something: `PositionResult` when resolved with a known outcome,
  `PositionDetails` otherwise (awaiting result, or market lookup failed).

## Order flow

- **Two flows**, chosen once per opening by the Phase 2 toggle (`isPolymarketPhaseTwoEnabled`, read in
  `DefaultPolymarketComponent`): single-step `Route.Order` when on, legacy `Amount → Summary` otherwise. Both
  share `PlacePredictionQuoteEngine`, `PlacePredictionOrderSubmitter` and the fee sheet; Slippage and Status
  routes are the same.
- **Confirmation boundary.** The tap that commits freezes a `PreparedPrediction` snapshot (displayed state,
  quote receipt time, signing params, region decision) — Next on Amount, Place on Order. Summary signs that
  snapshot and has no price or quote use cases; Back returns to the retained Amount model, which re-reads
  immediately. Order signs it in place and holds the engine's sources (`holdDuring`) until submission ends,
  so the NFC pause/resume cannot restart the quote loop or reload the position under the signature.
- **Back while submitting** is swallowed: the active step implements `SubmittingStep`.
- **Tap while quoting** (Order only, as iOS): `PlacePredictionQuoteEngine` keeps a fresh amount pending from
  entry (typed or Sell prefill) until its first quote result or error; a pause, `holdDuring`,
  `abandonFreshQuote()`, a loop that stops without re-requesting it or a throwing request settle it instead.
  `QuoteUM.Loading(isAnswerExpected)` is that plus a request actually running. A tap waits in
  `SubmitUM.AwaitingQuote` on `awaitFreshQuote()` (true only for a result of that amount) for at most 10 s: true
  places through `prepare()`, timeout abandons the fresh amount, false drops at once. The gate derives one
  `PlaceActionUM` (`Ready`, `ReadyOnceQuoted`, `AwaitingQuote`, `Unavailable`); the button reads only that and
  spins while a tap waits.
- **Quote loop** runs only while Amount or Order is on top and in the foreground (bound to its resume/pause).
  The quote is never cached. A quote older than 30 s at the tap is refused, as on iOS: the price-changed
  dialog shows, Order drops it and requotes, Summary closes back to Amount.
- **Placeable** = `quote.isPlaceable` (a SELL also places on `BELOW_MIN_ORDER_SIZE`) and, for BUY only,
  the price-weighted minimum. A SELL has no floor beyond one whole share or the whole position.
- **Shares.** The sellable size is the whole position at on-chain precision (6 decimals), not floored to
  the 2-decimal grid — the BFF quotes a whole-position sale at the position's own precision. Display cuts
  to 2 decimals, never more than held; a count below 0.01 keeps its precision so a remainder is not shown
  as `0`. Steps move in whole shares; 100 % is the position itself, so a full close reaches the last fraction.
- **Ambiguous orders.** A transport or inconclusive HTTP error does not prove rejection. The submitting
  step (Summary or Order) shows the existing error dialog with Support; keep balance/position refreshes and
  never resubmit automatically, because the order may already be live. A `delayed` order is read back up to
  3 times, 10 s apart; `LIVE`, unknown statuses and unanswered reads keep waiting, CANCELED / INVALID fail. If
  it never settles, the flow ends on the "still being processed" result screen, not a dialog over the form.
- **Position indexing.** `/positions/outcome` answers `null` until the position is indexed — for several
  seconds even for an outcome the wallet already held. Details shows Unavailable at once on a first `null`
  and retries only for a shown position whose size lags the last order. The result screen waits longer
  (`CREATED_POSITION_READS`) and treats a failed read like a missing one; a market without `conditionId`
  offers no View position. View position replaces the stack with Main + details, so Back lands on the feed,
  not on the result of an order already placed.
- **Sell** re-validates at signing that the position exists, the market accepts orders and enough shares
  remain after grid rounding.
- Quotes and prices are decimal strings: `BigDecimal`, `compareTo`, never `==`. Market end dates are
  Eastern time.

## Positions

- The BFF distinguishes only ACTIVE and FINISHED endpoints; the status filters are applied locally.
- Close follows the position, `resolutionStatus == OPEN && isAcceptingOrders`, and needs a known `marketId` —
  a row whose market lookup failed must not build `ClosePosition`. Buy more reads `isAcceptingOrders` off the
  event's market (found by `conditionId`, outcome by `outcomeIndex`, else by current or legacy asset id) and
  trades the outcome's current asset id: a position still held in a market's legacy token reports no orders
  while the market trades on under its new token. The resolution status stays the position's. Both buttons
  always stay enabled and open the market-closed sheet instead of the flow when untradable, when the position
  has no `marketId` or no `eventId`, and — Buy more only — when the event read failed or its market holds no
  outcome of the position. While the event is loading Buy more does nothing.
- Status: a settled `result` (WON / LOST / SPLIT) wins; otherwise `resolutionStatus` decides, so an
  unrecognised `result` never hides Pending or Disputed. The BFF sends `status` only once `RESOLVED`, and
  a resolved market can still arrive without one while the payout settles — no badge, opens details.
- `PositionResult` requires `RESOLVED` and a known outcome, not `isRedeemable`: a won position whose payout
  is not on-chain yet opens the result with a payout-pending footer.

## History

BUY / WITHDRAW are outgoing, SELL / DEPOSIT / REDEEM incoming, a lost redemption shows a dash.
CSV exports every page of the selected filter independently of list pagination, deduplicating IDs.
Search is an inert placeholder. Activity details use `TangemModal`; LOST hides its amount until the
activity API supplies the lost stake, and outcome price appears only when an order provides it.