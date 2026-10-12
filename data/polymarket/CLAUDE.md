# Polymarket — data

Implementations behind the `domain/polymarket` interfaces: six remote services, the signer that talks to
the card / hot wallet, the stores, and the error resolvers that turn HTTP into typed domain errors. One
Android-library module (`:data:polymarket`), Hilt, Moshi via `grow/datasource`.

The other halves of the module knowledge base: `domain/polymarket/CLAUDE.md` (identities, invariants,
signing) and `features/polymarket/CLAUDE.md` (screens, routes, flows).

```bash
./gradlew :data:polymarket:testDebugUnitTest
./gradlew :data:polymarket:detekt
```

Retrofit interfaces and DTOs are **not** here — they live in `grow/datasource`
(`com.tangem.grow.datasource.polymarket.*`), base URLs in `grow/datasource/.../config/Polymarket*.kt`.

## The six remote services

| Service | Retrofit interface | Base URL | Auth | Repository |
|---|---|---|---|---|
| **BFF** (Tangem backend, `api/predictions/v1/…`) | `PolymarketApi` | Tangem API | wallet identity per endpoint: `wallet-id` header on `/positions*`, `/withdraw*` and `GET /configuration`, `walletId` query on `GET /wallet`, `walletId` in the `/wallet/deploy` body, `ownerAddress` / `depositWalletAddress` elsewhere | `DefaultPolymarketRepository`, `DefaultPolymarketPositionsRepository`, `DefaultPolymarketWithdrawRepository` |
| **CLOB** (`clob.polymarket.com`) | `PolymarketClobApi` | `PolymarketClob` | L1 (`POLY_ADDRESS/SIGNATURE/TIMESTAMP/NONCE`) for `auth/*`; L2 HMAC (`POLY_*` ×5) for balance and orders; `GET price` is public, no headers | `DefaultPolymarketRepository` |
| **Bridge** (`bridge.polymarket.com`) | `PolymarketBridgeApi` | `PolymarketBridge` | none | `DefaultPolymarketBridgeRepository` |
| **Relayer** (`relayer-v2.polymarket.com`) | `PolymarketRelayerApi` | `PolymarketRelayer` | none | `DefaultPolymarketRepository.getRelayerNonce` |
| **Web** (`polymarket.com/api/geoblock`) | `PolymarketGeoApi` | `PolymarketWeb` | none | `DefaultPolymarketRepository.checkGeoblock` |
| **Data API** (`data-api.polymarket.com/value`) | `PolymarketDataApi` | `PolymarketData` | none | `DefaultPolymarketPositionsRepository.getPositionsValue` |

Who owns what: the BFF owns onboarding (`/wallet`, `/wallet/deploy`, `/wallet/approvals`), discovery
(`/categories`, `/events`, `/search`, `/events/{id}`), quoting (`POST /orders/quote`), positions
(`/positions`, `/positions/closed`, `/positions/outcome`) and withdrawal (`POST /withdraw`,
`GET /withdraw/{transactionId}`). The CLOB owns credentials (`auth/derive-api-key`, `auth/api-key`), balance
(`balance-allowance`, `balance-allowance/update`), price (`price`), and the order itself (`POST order`,
`GET data/order/{id}`). The bridge owns funding requisites (`supported-assets`, `deposit`, `withdraw`).

### Contract sources (Notion, `Tangem Wallet / Features / Polymarket Integration`)

The contract is read from Notion, never from memory of it — pages change without notice and the code has
been wrong about every documented status code at least once. Check the page's last-edited date against the
date here; when you ingest a change, update this table in the same PR.

| Page | Covers | Checked against code |
|---|---|---|
| [BFF Contract — onboarding](https://app.notion.com/p/39e5d34eb6788042b112e10747b02827) | `/wallet*`, approvals set, `walletId` | 10.09.2026 |
| [BFF-Client-Discovery-API-Contract](https://app.notion.com/p/3965d34eb6788015af10eed189d2f6e8) | `/categories`, `/events`, `/search` | 10.09.2026 |
| [Polymarket order flow](https://app.notion.com/p/3ab5d34eb678803e8462c1b95681bdb6) | `/orders/quote`, CLOB order, positions | 26.08.2026 |
| [Mobile Order Placement Flow](https://app.notion.com/p/3c35d34eb6788044b0eed0781c44ef18) | Order screen rules | 01.09.2026 |
| [Deposit & Withdraw client guide](https://app.notion.com/p/3cd5d34eb6788087b7f2c748c6b24a46) | Bridge, `/withdraw`, minimums | 17.09.2026 (§6.1/§7.1) |
| [Deposit flow Use Cases](https://app.notion.com/p/3d55d34eb6788088b536d7d22b5143cc) | Add-funds cases | 08.09.2026 |
| [Tradezone Balance — API & Display Rules](https://app.notion.com/p/3dc5d34eb6788099b050dd757a787996) | Polling, `/value`, PnL | 21.09.2026 — `/value` implemented ([REDACTED_TASK_KEY]), PnL not |
| [Positions & Rewards client guide](https://app.notion.com/p/3c15d34eb67880859ea2f883ac086d02) | Positions, claim | 17.09.2026 — claim not implemented ([REDACTED_TASK_KEY]) |
| [Close Position Flow](https://app.notion.com/p/3dd5d34eb6788087a944c0d8a60af953) | Sell rules | 17.09.2026 — cases 5/7/8 not implemented ([REDACTED_TASK_KEY] partial) |

## Wire-format traps (each one cost a device session)

- **`chainId` in bridge `/supported-assets` is a string**, not a number. `minCheckoutUsd` arrives as either;
  `PolymarketBridgeWireFormatTest` pins both readings.
- **Express answers with lower-cased addresses and matches pairs by string equality.**
  `PolymarketContracts.USDC_TOKEN_ADDRESS` is stored lower-cased and `DefaultPolymarketCollateralCurrencyFactory`
  lower-cases `COLLATERAL` for the same reason. A checksum-cased currency shows "Unsupported swap pair" for a
  pair Express actually supports.
- **The CLOB answers `400 "Could not derive api key!"`, not `404`, for an address with no key yet.**
  `PolymarketAuthErrorResolver` maps that body to `KeyNotFound`, which is the only thing that sends
  `DeriveApiCredentialsUseCase` to `POST auth/api-key`. A plain `404` also maps to `KeyNotFound`.
- **The deposit wallet is a beacon proxy, not UUPS.** `DefaultPolymarketDepositWalletDeriver` uses
  `DW_BEACON_INIT_PREFIX + beacon + DW_BEACON_INIT_SUFFIX` as the CREATE2 init code; verified against 60
  consecutive real factory deployments. `POST /wallet/deploy` answers `400 "depositWalletAddress does not
  match the derived deposit wallet"` when this is wrong.
- **`walletId` is `UserWalletId.stringValue`** (64 hex, no `0x`) wherever the BFF takes it: the `/wallet/deploy`
  body, `GET /wallet?walletId`, the `wallet-id` header on `/positions*`, `/withdraw*` and `GET /configuration`.
  It is a correlation key, not a derivation input. The bridge never receives it: `deposit` / `withdraw`
  carry addresses plus the `X-Builder-Code` header, and that builder code is what `GET /configuration`
  returns for the wallet id.
- **L2 HMAC signs `timestamp + method + requestPath [+ body]`** with the base64url-decoded secret
  (`PolymarketHmacSigner`, mirrors `py-clob-client`). `requestPath` is signed verbatim — leading slash, no
  query string for the endpoints checked so far (`PolymarketL2HeaderBuilder`). The passphrase travels in
  the clear and is not part of the HMAC.
- **`ClobAuth` EIP-712 domain has no `verifyingContract`; the `DepositWallet` batch domain does**
  (`PolymarketTypedDataBuilder`). Same builder, two different `EIP712Domain` type arrays.
- **Signatures are `0x` + R‖S‖V, 130 hex chars** (`PolymarketSignatureFormatter`, `SIGNATURE_RSV_HEX_LENGTH`);
  the ERC-7739 order signature is the inner signature followed by the exchange domain separator, the
  order struct hash, the type description and its 2-byte length.
- **Errors are RFC-7807 problem details**; `ProblemDetailParser` extracts `detail`, and the BFF exposes no
  other machine-readable hint, so `422` sub-cases are matched on the message text
  (`PolymarketWalletErrorResolver.resolveRelayerRejection`). Unknown text falls back to `RelayerRejected.Other`.
- **`/positions/outcome` may answer with a null position** right after an order — the position is not
  indexed yet. `getPosition` returns `Right(null)`; the feature polls it (`PolymarketConst.POSITION_INDEXING_READS`).

## HTTP → domain error map

| Source | Code | Domain error | Meaning |
|---|---|---|---|
| CLOB `auth/*` | `401` | `PolymarketAuthError.InvalidSignature` | L1 signature rejected |
| | `404`, `400` + "Could not derive api key!" | `KeyNotFound` | create a key |
| | `429` | `RateLimited` | |
| BFF `/wallet*` | `409` | `PolymarketWalletError.WalletNotDeployed` | approvals sent before deploy finished |
| | `422` | `RelayerRejected.{WalletNotRegistered, DeadlineTooSoon, InvalidSignature, NonceReused, Other}` | by `detail` text |
| | `502` | `RelayerUnavailable` | relayer down, retry later |
| CLOB `POST order` | `400` + "no match" marker | `PredictionPlaceOrderError.NoLiquidity` | fill-and-kill found nothing |
| | `400` other | `Rejected` | |
| | `401` | `Unauthorized` | credentials stale — [REDACTED_TASK_KEY] will clear them |
| | network/timeout, other | `Ambiguous` | the order may have been placed; the feature reports it as submitted, never as failed |
| BFF `/withdraw*` | `409` | `PredictionWithdrawError.NonceTaken` | same nonce already submitted |
| | `422` | `RelayerRejected` | |
| | `404` | `NotFound` | tracker stops, record kept |

Network and timeout exceptions map to the `Network` variant of each hierarchy; anything unmapped becomes an
`Unknown`/`Unexpected` carrying the numeric code and body. Every `Either`-returning call is total. Two
deliberate exceptions: the pagination fetchers (events, positions) throw into the pagination layer, which is
that layer's contract; and `POST order` maps network / timeout to `Ambiguous`, not `Network`, because the
order may have been placed.

## Stores and what survives a restart

| Store | Backing | Survives restart | Cleared on wallet deletion |
|---|---|---|---|
| `DefaultPolymarketCredentialsStore` | `SecureStorage` (AES-256-GCM, Keystore), JSON per wallet | yes | yes |
| `PredictionAccountStatusStore` | `RuntimeSharedStore` + `DataStore<…DTO>` | value only, restored as `StatusSource.CACHE`; no fiat rate, no loading/error states | yes |
| `DefaultPolymarketOnboardedStore` | `DataStore<Set<String>>` | yes — an optimisation over asking the BFF; every read degrades to "not recorded" | yes |
| `DefaultPredictionWithdrawalStore` | `RuntimeSharedStore` + `DataStore` | yes — the signed batch awaiting the relayer | yes |
| `DefaultPredictionDepositRequisitesStore` | runtime | no | yes |
| `DefaultPolymarketBridgeAssetsStore` | runtime | no | n/a |

`PolymarketUserWalletDataCleaner` is why the last column exists: `UserWalletId` is derived from the wallet
key, so re-adding a deleted wallet reproduces every storage key and would inherit the old credentials
without a card tap. `PredictionAccountStatusStore` writes wait for its disk preload so a deletion during
startup cannot be resurrected by the snapshot; reads never wait.

The credentials store is keyed by `UserWalletId`, not by owner EOA, so the entry stays reachable after the
wallet is gone (deletion removes the wallet before per-wallet cleanup runs, and the address can only be
derived *from* the wallet).

## Signing implementation

`DefaultPolymarketTypedDataSigner` resolves a `SigningContext` from `DerivationsRepository.getExistingDerivedKeys`
— it signs only with an owner key that is **already derived and stored** (`NotDerived` otherwise); deriving is
`DefaultPolymarketEoaDeriver`'s job and the only place a card session or hot-wallet unlock is opened. Cold
wallets sign through the card SDK, Hot through `TangemHotWalletSigner`; both return raw `r‖s` that
`PolymarketSignatureFormatter` recovers `v` for. `Either.catchOn(dispatchers.io)` keeps every path total.

Builders are pure objects with known-answer tests: `PolymarketTypedDataBuilder` (ClobAuth, approvals batch),
`PolymarketOrderTypedDataBuilder` (CTF `Order` struct, domain separators — the two exchanges share name and
version and differ only in `verifyingContract`), `PolymarketErc7739SignatureBuilder` (digest + wrap),
`PolymarketPlaceOrderBodyBuilder` (CLOB body; amounts and price formatted per `PolymarketClobOrderFormat`),
`PolymarketTransferCallBuilder` / `DefaultPredictionTransferCallCodec` (the ERC-20 `transfer` call of a
withdrawal, encoded and then decoded back so the signed calldata is proven to match the amount and recipient).

## Account status refresh

`DefaultPredictionAccountStatusFetcher` is driven by the wallet screen for every wallet, including ones
that never opened the feature. It never derives (invariant 1 in the domain file): with stored addresses it
reads by owner address, without them it falls back to the `byWalletId` read (`GET /wallet?walletId`,
`getWalletStatusByWalletId`), which serves a wallet onboarded on another device — that path knows the
status but has no credentials, so it yields no balance. It maps `PolymarketWalletStatus` to
`PredictionAccountStatusValue` and on any failure leaves the cached value in place and marks it
un-refreshed — it never writes "empty" on the strength of a failed question.
`DefaultPredictionAccountStatusProducer` mixes the fiat rate in on every emission, starting with
`Pending` so the combined account list is never stalled by this account.

`PredictionAccountStatusValue.Active` is written by two parties — the fetcher owns the balance and the
positions value, `DefaultPredictionDepositAddressWriter` owns the deposit address — so neither writes a
whole value it read earlier. Both commit through `PredictionAccountStatusStore.update`, whose
non-suspending transform receives the value current at that moment; `store` is the same call with a
transform that ignores it, for the states that are facts about the whole account (`NotOnboarded`,
`Onboarding`, `Onboarded`, `Error`). One lock spans the runtime commit and the disk write, so the disk
cannot end up holding a value the runtime already superseded. A refresh therefore carries over
what it did not re-read instead of snapshotting it before the network, and two refreshes of one wallet run
in parallel. What this accepts: the slower of two overlapping refreshes commits the older balance, and a
failing one flags a value a concurrent success has just written as `ONLY_CACHE`. Both correct themselves
on the next poll (30 s, 5 s in the funding window) and neither can touch a field its refresh did not read.
Revisit if a write lands here that is visible to the user and does not self-heal — an optimistic debit
after an order, say.

## DI

`PolymarketDataModule` binds every repository and deriver, the two runtime stores (bridge assets, deposit
requisites) and provides the long-lived flows (`PredictionAccountStatusSupplier`, `PredictionBalancePoller`,
`PredictionWithdrawalTracker`, `PredictionFundingWindow`) with `PredictionBalancePollingConfig` and
`PredictionWithdrawalTrackingConfig` defaults; `PolymarketStorageModule` provides the four persisted stores
(credentials, account status, onboarded, withdrawal) with their `DataStore`s and `SecureStorage`;
`PolymarketSignerModule` binds the signer and `Base64UrlCodec`; use cases and interactors are provided in
`app/.../di/domain/PolymarketDomainModule`.
Anything that also needs feature-layer dependencies is provided in `features/polymarket/impl/di`.

## Logging

`TangemLogger.i` / `.e` only — those are the two levels that reach the exported log file. Never mask values
by hand; the sanitizer does it, and hand-rolled masking has already once hidden the very field a bug report
needed. One tag per flow so a support file can be grepped by run.