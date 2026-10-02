# Polymarket — feature

UI rules of "Predictions" that the code does not make evident. Read the domain invariants
(`domain/polymarket/CLAUDE.md`) before adding a screen that can sign, prompt or move funds.

## Gating and wiring

- `AND_16204_POLYMARKET_ENABLED` gates everything (dev builds only until release). The feed's Predictions
  block additionally needs Amplitude `twi_1576_polymarket = on`, read once per model and only while the
  toggle is on; accounts, balances and the wallet-screen row follow the toggle alone.
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
- Main returns here (`replaceAll(Onboarding)`) the moment the stored credentials are gone, even from under a
  child: nothing above Main can sign without them. So the order summary answers an `Unauthorized`
  placement with no dialog and leaves the gate to explain it.

## Main screen

- **Available vs Total** (tap the header label): Available is the CLOB collateral (default); Total adds the
  positions value and is offered only once that value has been read — until then the toggle is disabled but
  the chosen mode is kept and returns with the value. The wallet screen and wallet total count Available
  only, as on iOS.
- **Add funds** needs a confirmed deposit address; without one, report an unknown error rather than offer
  an address that may be wrong. The buy amount step offers the same sheet, shut until the region check
  answers, and shows the account-status balance only once `ACTUAL`.
- **Awaiting-funds banner** opens on a completed top-up swap or a sent order, never on Receive.
- Pull-to-refresh refreshes region, positions preview, account status and positions value, then the
  requisites. Feed pages are not re-requested — they are kept per visited category and refreshed by
  `PolymarketFeedPoller`.
- A position row always opens something: `PositionResult` when resolved with a known outcome,
  `PositionDetails` otherwise (awaiting result, or market lookup failed).

## Order flow

- **Confirmation boundary.** Amount freezes a `PreparedPrediction` snapshot on Next (displayed state, quote
  receipt time, signing params, region decision). Summary displays and signs that snapshot and has no price
  or quote use cases; Back returns to the retained Amount model, which re-reads immediately.
- **Quote loop** runs only while Amount is on top and in the foreground (bound to its resume/pause). The
  quote is never cached; a stale one (>60 s at signing) is refused before submission.
- **Placeable** = `quote.isPlaceable` (a SELL also places on `BELOW_MIN_ORDER_SIZE`) and, for BUY only,
  the price-weighted minimum. A SELL has no floor beyond one whole share or the whole position.
- **Shares.** The sellable size is the whole position at on-chain precision (6 decimals), not floored to
  the 2-decimal grid — the BFF quotes a whole-position sale at the position's own precision. Display cuts
  to 2 decimals, never more than held; a count below 0.01 keeps its precision so a remainder is not shown
  as `0`. Steps move in whole shares; 100 % is the position itself, so a full close reaches the last fraction.
- **Outcomes.** `Ambiguous` (transport failure after send) is shown as **Submitted**, never as failed — the
  order may be live. `NoLiquidity` (fill-and-kill found nothing) is NotFilled. Filled / PartiallyFilled /
  Submitted open the funding window.
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
- Close and Buy more share one rule, `resolutionStatus == OPEN && isAcceptingOrders`, as on iOS. Close
  needs a known `marketId` — a row whose market lookup failed must not build `ClosePosition`. Otherwise
  both buttons stay enabled, and a tap on an untradable market opens the market-closed sheet.
- Status: a settled `result` (WON / LOST / SPLIT) wins; otherwise `resolutionStatus` decides, so an
  unrecognised `result` never hides Pending or Disputed. The BFF sends `status` only once `RESOLVED`, and
  a resolved market can still arrive without one while the payout settles — no badge, opens details.
- `PositionResult` requires `RESOLVED` and a known outcome, not `isRedeemable`: a won position whose payout
  is not on-chain yet opens the result with a payout-pending footer.

## History

BUY / WITHDRAW are outgoing, SELL / DEPOSIT / REDEEM incoming, a lost redemption shows a dash. Search and
CSV are inert placeholders.