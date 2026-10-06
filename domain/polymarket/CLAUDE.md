# Polymarket — domain

Rules of the prediction account that the code does not make evident. Read before touching anything that
signs, deploys or moves funds. Wire contract and its Notion sources: `data/polymarket/CLAUDE.md`.

## Identities

One Tangem wallet ↔ one Polymarket account; confusing these three is the classic defect.

- **Owner EOA** — key on `m/44'/60'/999899'/0/0`. Signs everything (ClobAuth, approvals, orders,
  withdrawals, claims). The path is hardened and belongs to no network, so a wallet needs nothing in its
  portfolio to onboard. It must match iOS and the SDK byte for byte.
- **Deposit wallet** — CREATE2 beacon proxy on Polygon derived from the owner EOA. Holds the funds; the
  `maker`/`signer` of every order.
- **`walletId`** = `UserWalletId.stringValue` (64 hex, no `0x`). A BFF correlation key only — never a
  derivation input, never sent to the bridge (the bridge gets addresses plus the builder code that
  `GET /configuration` returns for the wallet id).

## Invariants — expensive to break

1. **Opening a screen prompts for nothing.** Deriving the owner key opens a card session (Cold) or unlocks
   the seed (Hot); background refreshes and screen opens use only the `stored` / `withoutPrompting`
   variants. Their `null` means "not derived on this device", never "no account" — a wallet onboarded on
   another phone answers `null` too.
2. **One signing session per onboarding run.** ClobAuth and the approvals batch are signed together and the
   user is never asked twice; a failure is retryable and the caller starts a new run.
3. **Approvals are only really checked on a fresh wallet.** The BFF reads on-chain allowances before it
   validates our batch, so an already-approved deposit wallet answers `READY_TO_TRADE` to *any* batch.
   Test approvals changes on a brand-new wallet.
4. **The 13-call approvals batch is not pinned by the backend.** It is validated as a set of
   `{target, selector, spender}` and grew from 6 to 13 without notice (August 2026); the next change breaks
   the app with a BFF `400` until a release ships.
5. **Collateral ≠ top-up token.** `COLLATERAL` is pUSD — what orders spend and the CLOB balance reports.
   Deposits are native USDC on Polygon (`USDC_TOKEN_ADDRESS`); the bridge converts.
6. **The region is read by the flow that commits, never supplied by its entry route.** An unreadable region
   never locks a user out of an existing account (browse, withdraw stay open) but refuses what would commit
   them: no new deploy (`UNDETERMINED`), order gate shut as "unknown", not "restricted". Fail-closed edge:
   when the BFF status read fails, the new-account check asks the geoblock anyway, so an existing account
   without a local confirmation record is gated like a new one until the BFF answers. The order flow does
   not re-read the region right before signing — a new trading flow needs its own read.
   `PolymarketAccessMode.READ_ONLY` comes from the geoblock alone; a second restriction cause needs its own
   signal, or the UI explains it as a region restriction.
7. **The prediction account never blocks the other accounts.** Its status supplier is `combine`d with every
   other account on the wallet screen, and `combine` waits for all sources. So the store emits on
   subscription even when empty, the producer starts with a pending rate, and a failed refresh keeps the
   cached value. Never show `Loading` when the store already holds a value.
8. **A ready backend status is not enough to trade.** L2 credentials live only on this device. Reinstall,
   new phone or a re-added wallet → `READY_TO_TRADE` with nothing to sign with → `PolymarketEntry.Reconnect`:
   the same onboarding run signs both digests, uses only ClobAuth to re-derive credentials, deploys and
   approves nothing. A CLOB `401` on balance or order drops only the credentials, never
   `PolymarketOnboardedStore`, so the gate offers Reconnect, not a full onboarding. Reading, clearing and
   re-deriving one wallet's credentials share one non-reentrant `PolymarketCredentialsLock`, so a late `401`
   cannot wipe a set a Reconnect has just stored.
9. **A withdrawal is persisted before it is submitted.** The relayer keys submissions on
   `(deposit wallet, nonce)`, so resending the same batch returns the same receipt. Only a rejection of the
   submit itself (`RelayerRejected`, `InvalidRequest` — the relayer recorded nothing) clears the record;
   never clear it on an ambiguous error. A double withdrawal is worse than a stuck record.

## Onboarding

- `PolymarketWalletStatus.UNKNOWN` (anything the BFF adds later) means "keep polling", not a breaking
  change. The run has no deadline and resumes from whatever status the backend reports.
- Credentials: derive-api-key first, create only on "no key yet".
- `PolymarketOnboardedStore` mirrors backend reads: `READY_TO_TRADE` records the wallet, `NOT_CREATED`
  withdraws it, any other status leaves it alone. Without stored addresses nothing is asked: the record
  alone answers `Reconnect`, its absence `Undetermined`.

## Signing and orders

- Relayer batches (approvals, withdrawal, claim) carry `deadline = now + 600 s`. Known gap: the tracker's
  `resend` of a persisted withdrawal skips the expiry check that `confirm` does.
- Orders use `signatureType = 3` (deposit wallet) wrapped in ERC-7739; the exchange `verifyingContract`
  is chosen by `isNegRisk`. The signed order is re-validated against the draft, the deposit wallet, the
  market and the quote age (60 s) before it is sent.
- SELL: `makerAmount = shares`, `takerAmount = collateral`; BUY the reverse.

## Funds

- **Requisites** are written **after** the deposit address, so callers may treat requisites becoming
  available as proof the status carries a confirmed address.
- **Balance**: `syncBalanceAllowance` must precede `getBalanceAllowance` — the CLOB serves a cached figure it
  recomputes only when asked, so a wallet funded after onboarding would report zero forever. The sync is
  best-effort.
- **Funding window** (5 s polling instead of 30 s) opens only on a send this app made; an external
  transfer's timing is unknown.
- **Positions value** is kept apart from the account status: it needs only the deposit-wallet address
  (public data API), and a failed read must not touch the balance. `PredictionTotalBalance.of` is the one
  place collateral and positions are added. Runtime only — persist it when the wallet total starts counting
  it.
- **Withdraw destinations** are filtered to USDC · Polygon (phase 1, owner decision 15.09.2026).
- **Withdraw has no minimum** by design (guide §7.1); `minCheckoutUsd` is deposit-only.
- **Withdrawal tracker** is started from the swap withdraw flow only, not on cold start (known gap).

## Account support

`UserWallet.isPredictionAccountSupported` (= `isMultiCurrency`) ignores the lock — the account is part of
the wallet's structure. Only `GetPolymarketEligibleWalletsUseCase` (the wallet picker) respects the lock.