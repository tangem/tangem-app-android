# Polymarket — domain

Business logic of the Predictions feature: the prediction account, onboarding of a Polymarket deposit
wallet, discovery of events, order signing, funds movement. One Android-library module
(`:domain:polymarket`), pure Kotlin apart from `libs/blockchain-sdk` types (`Blockchain`, EIP-712 hashing).

This file is the domain half of the module knowledge base. The other two halves:
`data/polymarket/CLAUDE.md` (BFF/CLOB/bridge contracts, wire-format traps, stores) and
`features/polymarket/CLAUDE.md` (screens, routes, flows). Read all three before touching anything that
signs, deploys or moves funds.

```bash
./gradlew :domain:polymarket:testDebugUnitTest
./gradlew :domain:polymarket:detekt
```

## Package map

| Package | What lives there |
|---|---|
| root | Repository and store **interfaces** (`PolymarketRepository`, `PolymarketBridgeRepository`, `PolymarketWithdrawRepository`, `PolymarketPositionsRepository`, `PolymarketCredentialsStore`, `PolymarketOnboardedStore`, `PredictionDepositRequisitesStore`, `PredictionWithdrawalStore`, `PolymarketBridgeAssetsStore`); `PolymarketDepositBlockchain`, `PredictionAccountSupport` |
| `approval` | `PolymarketContracts` — every on-chain address and constant; `PolymarketApprovalCalls` — the 13-call approvals batch |
| `derivation` | `PolymarketEoaDeriver`, `PolymarketDepositWalletDeriver`, `POLYMARKET_OWNER_DERIVATION_PATH` |
| `signing` | `PolymarketTypedDataSigner` (4 signing entry points), `PolymarketRelayerDeadline` |
| `model` | 64 value types; the ones that carry a decision are documented in their KDoc |
| `usecase` | One network or store operation each |
| `interactor` | Use cases composed of use cases (project rule: a use case that injects use cases is an Interactor, package `interactor/`) |
| `flow` | Long-lived processes: `PredictionAccountStatusSupplier`/`Producer`/`Fetcher`, `PredictionBalancePoller`, `PredictionFundingWindow`, `PredictionWithdrawalTracker` |
| `session` | `PolymarketPositionsSession` — one visit of the positions list |
| `amount` | `PredictionCollateralAmount` — decimal ↔ base-unit conversion of pUSD (6 decimals) |

Nothing in this module talks to a card, a network or a disk; those are `data/polymarket` implementations
injected behind the interfaces above. Use cases and interactors are plain classes provided in the app's
`di/domain/PolymarketDomainModule`, or in the feature's `PolymarketDomainUseCasesModule`
(`features/polymarket/impl/di/PolymarketBindsModule.kt`) when they need feature-side dependencies;
`data/polymarket/di/PolymarketDataModule` binds implementations and the long-lived flows only.

## The identities

One Tangem wallet ↔ one Polymarket account. Three identifiers travel together, and confusing them is
the classic defect:

| Identifier | Derived how | Used for |
|---|---|---|
| **Owner EOA** | secp256k1 key on `m/44'/60'/999899'/0/0` (`POLYMARKET_OWNER_DERIVATION_PATH`) → ERC-55 address | Signs everything: ClobAuth, approvals batch, orders, withdrawals. Protocol identity in every request |
| **Deposit wallet (DW)** | CREATE2 from the owner EOA — Solady ERC-1967 **beacon** proxy on Polygon, `DW_FACTORY`/`DW_BEACON`/`DW_BEACON_INIT_*` | Holds the funds; `maker`/`signer` of every order; the address funds are credited to and leave from |
| **`walletId`** | `UserWalletId.stringValue` (64 hex, no `0x`) | Correlation key on the BFF only (`/wallet/deploy` body, `GET /wallet?walletId`, the `wallet-id` header on positions, `/withdraw*` and `GET /configuration`). The bridge never sees it. **Not** an input to any derivation |

`PolymarketAddresses` binds the first two to the `UserWalletId` they were derived for, and its
constructor is `internal` so a pair cannot be assembled by hand. `DerivePolymarketAddressesUseCase`
is the only producer; its `stored()` variant never prompts.

The derivation path is hardened and belongs to no network — a wallet needs nothing in its portfolio to be
onboarded. It must match iOS and the SDK byte for byte; `PolymarketDerivationConstantsTest` pins it.

## Invariants — dear to break

1. **Opening a screen prompts for nothing.** Deriving the owner key opens a card session (Cold) or unlocks
   the seed (Hot). `PolymarketEoaDeriver.storedOwnerEoa`, `DerivePolymarketAddressesUseCase.stored`,
   `ResolvePolymarketEntryInteractor.withoutPrompting` and `DefaultPredictionAccountStatusFetcher` exist so
   that a background refresh or a screen open never asks. `null` from any of them means "not derived on this
   device", never "this wallet has no account" — a wallet onboarded on another phone answers `null` too.
2. **One signing session per onboarding run.** `RunPolymarketOnboardingInteractor` signs ClobAuth and the
   approvals batch in one signing session — a card session for Cold, a seed unlock for Hot
   (`PolymarketTypedDataSigner.signOnboarding`) — and never asks twice; a
   failure is reported retryable and the caller starts a new run.
3. **Approvals are only ever checked on a fresh wallet.** The BFF reads on-chain allowances before it
   validates the batch we send, so an already-approved deposit wallet answers `READY_TO_TRADE` to *any*
   batch. A wrong batch surfaces only on a brand-new wallet — test approvals changes on one.
4. **The batch is 13 calls and is not pinned by the backend.** `PolymarketApprovalCalls.build()`: 8 ERC-20
   `approve` + 5 ERC-1155 `setApprovalForAll`, validated by the BFF as a set of `{target, selector, spender}`.
   It already grew from 6 to 13 without notice (August 2026); when it changes again the app fails with a
   BFF `400` until a release ships. Taking the list from the backend is a proposed v2 change.
5. **Collateral ≠ top-up token.** `PolymarketContracts.COLLATERAL` (`0xC011a7E1…`) is **pUSD**, the
   settlement token every order spends and `getBalanceAllowance` reports. Deposits are made in native USDC
   on Polygon (`USDC_TOKEN_ADDRESS`, `0x3c499c…`); `PolymarketCollateralCurrencyFactory.createDepositTarget()`
   is that token, `create()` is pUSD. The bridge converts.
6. **The region is read by the flow that commits, never supplied by its entry route.** An unreadable region never
   locks a user out of an existing account (browse, withdraw stay open), but it does refuse what would
   *commit* them: `CheckNewPolymarketAccountAllowedInteractor` answers `UNDETERMINED` (no new deploy) and
   the order flow keeps its gate shut as "unknown" rather than "restricted". The one fail-closed edge: that
   interactor tells a new account from an existing one by a BFF status read, and when the read fails it asks
   the geoblock anyway — an existing account that has no local confirmation record is then gated like a new
   one until the BFF answers. The order flow reads the
   region when it is shown and re-reads on resume only while it is still unknown — there is no re-read
   right before signing; Amount passes its accepted state to Summary in an internal snapshot, so a region
   that changes while the screen is open is not caught. A new trading
   flow needs its own read.
   `PolymarketAccessMode.READ_ONLY` (browse + withdraw only) is sourced from the geoblock alone — a second
   reason to restrict access needs its own signal, or the UI explains the restriction wrongly.
7. **The prediction account never blocks the other accounts.** `PredictionAccountStatusSupplier` is combined
   with every other account's status on the wallet screen; `combine` withholds until every source has
   emitted. So the store emits on subscription even when empty, the producer starts with a pending rate,
   and a failed refresh keeps the cached value instead of writing "empty". Never show `Loading` when the
   store already holds a value.
8. **A ready backend status is not enough to trade.** L2 credentials live only on this device. Reinstall,
   new phone, deleted-and-re-added wallet → status `READY_TO_TRADE`, nothing to sign with →
   `PolymarketEntry.Reconnect`: the same onboarding run signs both digests in one session, uses only the
   ClobAuth signature to re-derive the credentials, and deploys and approves nothing.
9. **Withdrawal is persisted before it is submitted.** `SubmitPredictionWithdrawalInteractor` stores the
   signed batch, then posts it; the relayer keys submissions on `(deposit wallet, nonce)`, so a resend of the
   same batch returns the same receipt. `prepare()` refuses while a record is in flight (`AlreadyInFlight`).
   A double withdrawal is worse than a stuck record — do not clear a record on an ambiguous error.

## Onboarding state machine

`PolymarketWalletStatus` mirrors BFF `GET /wallet`:
`NOT_CREATED → DEPLOYMENT_IN_PROGRESS → DEPLOYED → APPROVALS_IN_PROGRESS → READY_TO_TRADE`, with
`DEPLOYMENT_FAILED` / `APPROVALS_FAILED` as retry points and `UNKNOWN` for anything the BFF adds later
(treated as "keep polling", never as a breaking change).

`RunPolymarketOnboardingInteractor` resumes from whatever status the backend reports, emits
`PolymarketOnboardingProgress` (`Deriving → AwaitingSignature → Working → Ready | Failed`), and has no
deadline: it polls while the status is non-terminal. Steps, each its own use case:
`DerivePolymarketAddresses → GetPolymarketWalletStatus` (a wallet that owes no approvals and already
holds credentials skips straight to waiting for `READY_TO_TRADE`) `→ GetPolymarketRelayerNonce →
SignOnboardingDigests → DeployDepositWallet` (when needed) `→ DeriveApiCredentials` (skipped when
credentials exist; derive-api-key first, create only on "no key yet") `→` wait for `DEPLOYED` `→
SubmitApprovals →` wait for `READY_TO_TRADE`. Finishing marks the wallet in `PolymarketOnboardedStore`,
primes the CLOB balance cache and refreshes the account status. `RecordPolymarketConfirmationUseCase`
keeps that store in step with a backend read elsewhere (`ResolvePolymarketEntryInteractor`): `READY_TO_TRADE`
records the wallet, `NOT_CREATED` withdraws it, every other status leaves the record alone.

`ResolvePolymarketEntryInteractor` decides where the user lands (`PolymarketEntry`): `Onboarded` from local
state alone when the confirmation record and credentials exist; `Onboard(status)` / `Reconnect` after a
backend read when the addresses are stored. Without stored addresses and without prompting, no backend is
asked: the confirmation record alone answers `Reconnect`, its absence `Undetermined`.

## Signing

`PolymarketTypedDataSigner` has four entry points, all EIP-712 over the owner key:

| Call | Domain | Notes |
|---|---|---|
| `signOnboarding` | `ClobAuth` (no `verifyingContract`) + `DepositWallet` batch | Two digests, one session |
| `signClobAuth` | `ClobAuth` | ClobAuth alone; no production caller — `Reconnect` goes through `signOnboarding` |
| `signWithdrawal` | `DepositWallet` batch | Same `Batch` envelope as approvals, one `transfer` call |
| `signOrder` | CTF Exchange (`name "Polymarket CTF Exchange"`, `version "2"`, chain 137) wrapped in **ERC-7739** for the deposit wallet | `signatureType = 3` (`SIGNATURE_TYPE_DEPOSIT_WALLET`) |

ERC-7739 is not in the Blockchain SDK; it is ported in `data/polymarket/builder/PolymarketErc7739SignatureBuilder`.
The exchange domain is the same name/version for CTF and neg-risk — only `verifyingContract` differs
(`CTF_EXCHANGE` vs `NEG_RISK_CTF_EXCHANGE`), chosen by `isNegRisk`. Known-answer vectors read off the chain
pin `PolymarketOrderDigestTest`, `PolymarketErc7739SignatureBuilderTest`, `PolymarketTypedDataDigestTest`;
a byte-level mistake shows up only as a CLOB rejection, so change these builders vector-first.

Relayer batches (approvals, withdrawal) carry a `deadline` of now + 600 s (`PolymarketRelayerDeadline`).
`SubmitPredictionWithdrawalInteractor.confirm` refuses an expired batch (`isExpired`); `resend`, which the
tracker uses to recover a persisted batch, submits it without that check — known gap. On either path a
`RelayerRejected` (the 422s, `DeadlineTooSoon` included) or `InvalidRequest` answer to the submit clears the
record, because the relayer recorded nothing; every other failure keeps it.

Orders: `SignPredictionOrderInteractor` builds the draft from the quote (`BuildPredictionOrderDraftUseCase`),
signs with a 52-bit random salt, then `ValidatePredictionOrderUseCase` re-checks the signed order against the
draft, the deposit wallet, the market and the quote age before anything is sent. SELL side:
`makerAmount = shares`, `takerAmount = collateral`; BUY the reverse.

## Funds movement

- **Requisites.** `RefreshPredictionDepositRequisitesInteractor` reads bridge `/supported-assets` and
  `POST /deposit` together, groups assets into `PolymarketDepositNetwork`s by `PolymarketChainFamily`, and
  writes the requisites store **after** the deposit address (`PredictionDepositAddressWriter`) — callers may
  treat requisites becoming available as proof the status carries a confirmed address.
- **Balance.** `GetPolymarketBalanceInteractor` calls `syncBalanceAllowance` before `getBalanceAllowance`:
  the CLOB serves a cached figure it only recomputes when asked, so without the sync a wallet funded after
  onboarding reports zero forever. The sync is best-effort.
- **Polling.** `PredictionBalancePoller` — one loop per wallet, reference-counted subscribers, runs on the
  app scope. `PredictionBalancePollingConfig`: 30 s base, 5 s while a `PredictionFundingWindow` is open
  (2 min window, 10 min max age). The window opens on a send this app made (`PredictionFundingWindow.open`)
  and closes when the balance settles or the window elapses. `PredictionAccountStatusFetcher.Params.mode`
  says how much a tick re-reads: `FULL` (main screen, pull-to-refresh, base poll ticks) also reads the
  positions value from the data API (`PolymarketPositionsRepository.getPositionsValue`) into
  `Active.positionsValue`; `AVAILABLE_ONLY` (wallet screen, onboarding, ticks inside the funding window)
  carries the cached one over. A failed `/value` read keeps the cached value too and never fails the balance.
- **Withdraw destinations.** `GetPredictionWithdrawDestinationsUseCase` matches bridge assets to the
  wallet's currencies but is filtered to USDC · Polygon (phase 1, owner decision 15.09.2026). Removing the
  filter is the phase-2 change.
- **Withdrawal tracking.** `PredictionWithdrawalTracker` resends a record without `transactionId`, then
  polls `getState` (5 s × 360). A terminal state clears the record; a `NonceTaken` / `RelayerRejected` /
  `InvalidRequest` / `Unauthorized` / `NotFound` answer to `getState` stops polling and leaves the record in
  place (see invariant 9) — only the submit itself clears it on a rejection, as above. Known gap: the
  tracker is started from the withdraw flow only, not on cold start.

## Prediction account in the rest of the app

The account type itself is `Account.Prediction` / `AccountId.forPredictionAccount` /
`PredictionAccountStatusValue` in `domain/models`, and `domain/account/status` combines
`PredictionAccountStatusSupplier` into the account list. Support is `UserWallet.isPredictionAccountSupported`
(= `isMultiCurrency`) and ignores the lock — the account is part of the wallet's structure.
`GetPolymarketEligibleWalletsUseCase` is the one that respects the lock, for the wallet picker.

## What is not here (yet)

- Claiming a resolved position (redeem) — [REDACTED_TASK_KEY] / [REDACTED_TASK_KEY]; iOS has a claim transaction service.
- Dropping L2 credentials on a CLOB `401` — [REDACTED_TASK_KEY], design done, not implemented.
- Tradezone PnL (`/v1/leaderboard`).
- Withdraw minimum — deliberately none (BFF client guide §7.1); `minCheckoutUsd` is deposit-only.

## Where the contract comes from

The external contract is a hypothesis until a real response confirms it: every documented status code and
field shape on this epic has been wrong at least once (`404` that was a `400`, UUPS that was a beacon, a
6-call batch that became 13, a `walletId` whose meaning reversed). Notion, `Tangem Wallet / Features /
Polymarket Integration`, is the source; the pages that matter are listed in `data/polymarket/CLAUDE.md`.
When a contract changes, update that list and this file in the same PR as the code.