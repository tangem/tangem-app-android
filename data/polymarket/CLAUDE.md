# Polymarket — data

What the code does not tell you about the BFF / CLOB / bridge / relayer contracts. Identities and
invariants: `domain/polymarket/CLAUDE.md`.

## The contract is a hypothesis

Every documented status code and field shape on this epic has been wrong at least once (a `404` that was a
`400`, UUPS that was a beacon, a 6-call approvals batch that became 13, a `walletId` whose meaning
reversed). Read the contract from Notion (`Tangem Wallet / Features / Polymarket Integration`), never from
memory; pages change without notice. Check a page's last-edited date against the date below and update it
in the same PR as the code.

- [BFF onboarding](https://app.notion.com/p/39e5d34eb6788042b112e10747b02827) — `/wallet*`, approvals, `walletId` · 10.09.2026
- [BFF discovery](https://app.notion.com/p/3965d34eb6788015af10eed189d2f6e8) — `/categories`, `/events`, `/search` · 10.09.2026
- [Order flow](https://app.notion.com/p/3ab5d34eb678803e8462c1b95681bdb6) — `/orders/quote`, CLOB order, positions · 26.08.2026
- [Mobile order placement](https://app.notion.com/p/3c35d34eb6788044b0eed0781c44ef18) — order screen rules · 01.09.2026
- [Deposit & withdraw guide](https://app.notion.com/p/3cd5d34eb6788087b7f2c748c6b24a46) — bridge, `/withdraw`, minimums · 17.09.2026
- [Deposit use cases](https://app.notion.com/p/3d55d34eb6788088b536d7d22b5143cc) — add-funds cases · 08.09.2026
- [Tradezone balance](https://app.notion.com/p/3dc5d34eb6788099b050dd757a787996) — polling, `/value`, PnL · 21.09.2026 (PnL not implemented)
- [Positions & rewards](https://app.notion.com/p/3c15d34eb67880859ea2f883ac086d02) — positions, claim · 17.09.2026
- [Close position](https://app.notion.com/p/3dd5d34eb6788087a944c0d8a60af953) — sell rules · 17.09.2026 (cases 5/7/8 not implemented)

Activity history (`/activity`, `/activity/deposits/reconcile`) was checked against an OpenAPI export on
28.09.2026.

## Wire-format traps (each one cost a device session)

- **The CLOB answers `400 "Could not derive api key!"`, not `404`, for an address with no key yet.** That
  body, and the `404` the spec documents, are the only answers that send `DeriveApiCredentialsUseCase` to
  `POST auth/api-key`.
- **The deposit wallet is a beacon proxy, not UUPS.** The CREATE2 init code was verified against 60 real
  factory deployments; a wrong one shows up only as `400 "depositWalletAddress does not match the derived
  deposit wallet"` from `/wallet/deploy`.
- **Express answers with lower-cased addresses and matches pairs by string equality.** A checksum-cased
  `COLLATERAL` / USDC address shows "Unsupported swap pair" for a pair Express supports.
- **`chainId` in bridge `/supported-assets` is a string**; `minCheckoutUsd` arrives as either string or number.
- **L2 HMAC** signs `timestamp + method + requestPath [+ body]` with the base64url-decoded secret
  (mirrors `py-clob-client`). `requestPath` is verbatim — leading slash, no query string. The passphrase
  travels in the clear and is not part of the HMAC. `GET price` is public: send no auth headers.
- **`ClobAuth` EIP-712 domain has no `verifyingContract`; the `DepositWallet` batch domain does.** The CTF
  and neg-risk exchanges share name and version and differ only in `verifyingContract`.
- **ERC-7739 is not in the Blockchain SDK** — it is ported here. The order signature is the inner `R‖S‖V`
  followed by the exchange domain separator, the order struct hash, the type description and its 2-byte
  length. A byte-level mistake surfaces only as a CLOB rejection, so change the builders vector-first: the
  known-answer tests are read off the chain.
- **Errors are RFC-7807 problem details with no machine-readable code.** `422` sub-cases (relayer
  rejections) are matched on the `detail` text; unknown text must fall back to `Other`, never fail.
- **Activity amounts and prices are decimal strings**; market / order / redemption metadata may be absent.
  `reconcile` answers `202 {state: SCANNING, retryAfterSeconds}` — it starts or joins a scan, it does not
  report completion. Cursors are opaque: keep them across retries, reset on filter change, reject cycles.

## Error-mapping decisions

Every `Either`-returning call is total (`.claude/rules/codestyle/either-error-handling.md`), with two
deliberate exceptions:

- The pagination fetchers (events, positions, activity) throw into the pagination layer — that layer's
  contract.
- CLOB `POST order` maps network / timeout to `Ambiguous`, not `Network`: the order may have been placed,
  so the feature reports it as submitted, never as failed.

BFF `/wallet*` `409` means approvals were sent before the deploy finished; `/withdraw*` `409` means the same
nonce was already submitted — neither is a generic conflict.

## Stores

- **Everything per wallet is keyed by `UserWalletId` and wiped by `PolymarketUserWalletDataCleaner`.**
  `UserWalletId` is derived from the wallet key, so a deleted-and-re-added wallet reproduces every storage
  key and would otherwise inherit the old CLOB credentials without a card tap. Credentials are keyed by
  `UserWalletId`, not by owner EOA, because deletion removes the wallet before per-wallet cleanup runs and
  the EOA can only be derived *from* the wallet. A new per-wallet store must join the cleaner.
- **`PredictionAccountStatusStore` writes wait for the disk preload** so a deletion during startup is not
  resurrected by the snapshot; reads never wait.
- **`DefaultPolymarketOnboardedStore` is only an optimisation** over asking the BFF; every read failure
  degrades to "not recorded".

## Account status

- The fetcher runs for every wallet on the wallet screen, including ones that never opened the feature, so
  it never derives. Without stored addresses it reads `GET /wallet?walletId` — that serves a wallet
  onboarded on another device but yields no balance (no credentials here).
- On failure it keeps the cached value and marks it un-refreshed; it never writes "empty" on the strength
  of a failed question. Exception: a balance `KeyNotFound` / `InvalidSignature` commits `Onboarded` — after
  a `401` the credentials are gone, so that is a fact, not a failed read.
- `Active` has two writers — the fetcher owns the balance, `DefaultPredictionDepositAddressWriter` the
  deposit address — so both commit through `PredictionAccountStatusStore.update` with a transform over the
  value current at commit time, never a whole value read earlier. Accepted race: the slower of two
  overlapping refreshes may commit the older balance, or flag a fresh value `ONLY_CACHE`; the next poll
  heals it. Revisit if a user-visible write that does not self-heal lands here (an optimistic debit after
  an order, say).

## Signing

The signer uses only an owner key that is **already derived and stored** (`NotDerived` otherwise);
`DefaultPolymarketEoaDeriver` is the only place a card session or hot-wallet unlock is opened for
derivation. Card and hot signers return raw `r‖s`; `v` is recovered afterwards.