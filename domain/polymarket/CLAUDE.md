# Polymarket — domain

Rules of the prediction account the code does not make evident. Read before touching anything that signs,
deploys or moves funds. Wire contract: `data/polymarket/CLAUDE.md`.

## Identities

One Tangem wallet ↔ one Polymarket account; confusing these three is the classic defect.

- **Owner EOA** — `m/44'/60'/999899'/0/0`, signs everything. Hardened, no network: onboarding needs nothing in the
  portfolio. Must match iOS and the SDK byte for byte.
- **Deposit wallet** — CREATE2 proxy on Polygon from the owner EOA. Holds the funds; `maker`/`signer` of orders.
- **`walletId`** = `UserWalletId.stringValue`. BFF correlation key only (`wallet-id` header on every call), never a
  derivation input, never sent to the bridge. Only `GET /wallet` by `walletId` returns `sessionKeys`.
- Onboarding falls back to one read by `ownerAddress` only when the `walletId` read is `NOT_CREATED` or fails:
  wallets deployed before the BFF bound them to a wallet id are found only so.

## Invariants — expensive to break

1. **Opening a screen prompts for nothing.** Background reads use `stored` / `withoutPrompting`; their `null` means
   "not derived here", never "no account".
2. **One tap per onboarding run** (`PolymarketSigningSession`): derivation, reads and every signature share one
   card session / unlock. The derived key is stored at once, so a retry costs one tap too.
3. **Approvals are really checked only on a fresh wallet** — an approved one answers `READY_TO_TRADE` to any batch.
4. **The approvals batch is not pinned by the backend** (6 → 13 → 17 calls without notice); a change breaks the
   app with a BFF `400` until a release ships.
5. **Collateral (pUSD) ≠ top-up token (USDC).** Only `PolymarketDepositAsset.CATALOG` assets the bridge lists with
   the same contract and decimals are offered: a wrong token or chain is lost.
6. **The region is read by the flow that commits**, never passed by the route. Unreadable → existing account
   stays browsable and withdrawable, nothing new commits. `READ_ONLY` means geoblock only.
7. **The prediction account never blocks other accounts**: its supplier is `combine`d on the wallet screen, so it
   emits on subscription even when empty and never shows `Loading` over a stored value.
8. **Ready backend status ≠ able to trade.** Credentials live only on this device → `Reconnect` signs ClobAuth
   only. A CLOB `401` drops the credentials, never `PolymarketOnboardedStore`. Session key credentials are derived
   in memory and never touch the owner's (`PolymarketCredentialsLock` is the owner's only).
9. **A withdrawal is persisted before it is submitted**; cleared only on a terminal receipt or a rejected submit.
   A double withdrawal is worse than a stuck record.
10. **The local confirmation is a fallback.** The entry gate re-reads `GET /wallet`; `DEPLOYED` sends the user to
    the approvals step (not a new account), `NOT_CREATED` clears the record.

## Onboarding

- `PolymarketWalletStatus.UNKNOWN` = keep polling. The run has no deadline and resumes from the reported status.
- Credentials: derive first, create only on "no key yet".
- `PolymarketOnboardedStore` follows backend reads: `READY_TO_TRADE` records, `NOT_CREATED` clears, others keep.

## Orders

- Relayer batches expire in 600 s; a follow-up session key batch in 1200 s. Known gap: withdrawal `resend` skips
  the expiry check.
- `signatureType = 3` (deposit wallet) in ERC-7739. Exchange and domain only via `PolymarketExchangeDomain.of`;
  anything else is refused before the card is asked. The signed order is re-validated before it is sent.
- SELL: `makerAmount = shares`, `takerAmount = collateral`; BUY the reverse.
- Session route: `maker = signer = deposit wallet`, signature in `PolymarketSessionKeyEnvelope`; an envelope on the
  owner route is refused. The route comes from `PolymarketOrderSigningRouter` (a BFF record is required, unlike iOS)
  unless the preference is `OWNER_ONLY`.
- A session key refused by the CLOB (`401`, a signer/authorization `4xx` message, `NotSent`) stays out of trading
  until a new authorization or restart; the order is re-signed with the owner key. A bare `403` is a geoblock, not
  a refusal.

## Session key lifecycle

- Every relayer batch takes the next deposit-wallet nonce → claim, withdrawal and lifecycle share
  `PolymarketDepositWalletLock`. A withdrawal reads its nonce outside it (a lifecycle op in between →
  `RelayerRejected`).
- One lifecycle operation per wallet; the UI starts them only through `PolymarketSessionKeyOperationRunner` (app
  scope — leaving the screen must not drop a pre-signed authorization). `Activate` picks enable / renew / replace.
- Nothing is signed twice without the user: a refused or expired signature → `StaleSignature`. Only an unanswered or
  `502` send is repeated. Exception: the authorization after a revocation is resent on `RELAYER_REJECTED` (BFF says
  `REVOKED` while the revocation is in flight; iOS does not resend).
- A key the BFF stops listing while `REVOKING` counts as revoked only once Polymarket no longer lists the signer;
  otherwise the outcome is unsettled and the next refresh shows the truth.
- The outcome stays in the runner until the host acknowledges it; no Activate banner while an operation runs.

## Claims

- Only BFF-served redeem calls that `PredictionRedeemCallValidator` accepted are signed; a position without a known
  `version` is never claimed.

## Funds

- Requisites are written after the deposit address: requisites available ⇒ the address is confirmed.
- `syncBalanceAllowance` before `getBalanceAllowance` — the CLOB caches the figure; a SELL syncs its shares first
  (best-effort).
- Funding window (5 s polling, up to 90 s) opens only on a send this app made.
- Positions value is read beside the collateral and persisted with the status; one failed read never hides the
  other's answer (deviation from iOS). `PredictionTotalBalance.of` is the only place they are added.
- Withdraw: USDC · Polygon only, no minimum; the tracker starts from the withdraw flow only (known gap).

## Account support

`isPredictionAccountSupported` ignores the lock; only `GetPolymarketEligibleWalletsUseCase` (wallet picker) respects
it.