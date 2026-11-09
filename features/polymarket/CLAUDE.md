# Polymarket — feature

Rules of "Predictions" that the code does not make evident. Read the domain invariants
(`domain/polymarket/CLAUDE.md`) before adding a screen that can sign, prompt or move funds.

## Across the feature

- `TWI_1700_POLYMARKET_PHASE_TWO_ENABLED` gates all D2 behavior through
  `PolymarketFeatureToggles.isPolymarketPhaseTwoEnabled`: sports cards and scores, abbreviated outcome
  labels, live quotes, crypto screens and charts as they are connected. Gate both UI mapping/navigation
  and data subscriptions; hiding a component alone must not leave its D2 socket or polling active.
  With D2 off, preserve the D1 REST presentation and outcome click targets. Test both flag values at
  each integration boundary. Reusable data/socket implementations can remain flag-independent and cold.
- The feed's Predictions block is gated by Amplitude `twi_1576_polymarket = on`. Accounts, balances and the
  wallet-screen row are not gated.
- Add funds and Withdraw are not screens of this module: they are `AppRoute.Swap` with
  `AccountFlow.PredictionTopUp` / `PredictionWithdraw`, implemented in `features/swap/impl`.
- Routes carry `userWalletId` on purpose: the entry step may settle a different wallet than the one the feature
  was opened with. Children read the wallet from their route, never from the feature params.
- Quotes and prices are decimal strings: `BigDecimal`, `compareTo`, never `==`. Market end dates are Eastern time.
- Analytics names are the analysts' contract — never invent one; wait for the spec.

## Entry gate

- Opening the gate prompts for nothing — no NFC, no biometrics (domain invariant 1).
- `Undetermined` is not "no account": a wallet onboarded on another device lands on Welcome too, and Start finds
  its account. Don't add copy that says "create".
- `READ_ONLY` means a region restriction; such users still reach Main, browse and withdraw. Reuse it for another
  cause and the UI lies.
- Main returns to the gate the moment the stored credentials are gone, so an `Unauthorized` placement shows no
  dialog and leaves the gate to explain it.

## Main screen

- Total adds the positions value to the CLOB collateral. The wallet-screen row and the wallet total count Total,
  as on iOS ([REDACTED_TASK_KEY]).
- Add funds needs a confirmed deposit address; without one, report an unknown error rather than offer an address
  that may be wrong.

## Order flow

- Two flows, chosen once per opening by `isPolymarketPhaseTwoEnabled`: single-step Order, or legacy
  Amount → Summary. The quote engine, the submitter and the result screen are shared, so a change there reaches
  both flows.
- Signing uses the `PreparedPrediction` frozen at the tap, never the live screen: nothing may requote or reload
  the position under the signature, NFC pause/resume included.
- A quote older than 30 s at the tap is refused, as on iOS.
- A sell's size is the whole position at on-chain precision (6 decimals), not the 2-decimal grid: the BFF quotes a
  whole-position sale at the position's own precision.
- A transport or inconclusive error does not prove rejection: never resubmit automatically, the order may already
  be live.
- `/positions/outcome` answers `null` for several seconds after a fill, even for an outcome already held.

## Positions

- The BFF has only ACTIVE and FINISHED endpoints; status filters are applied locally.
- A position held in a market's legacy token reports no orders while the market trades on under its new token, so
  Buy more reads `isAcceptingOrders` off the event's market and trades the outcome's current asset id.
- A settled `result` (WON / LOST / SPLIT) wins over `resolutionStatus`. A resolved market can arrive without
  `status` while the payout settles.