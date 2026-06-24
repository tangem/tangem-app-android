# Swap / transfer-mode / accounts-mode test setup

Hard-won prerequisites for swap, transfer-mode (same-token swap), and multi-account ("accounts mode")
tests. Each missing item below produces a state that *looks* like a locator/test bug but is actually
missing mock data — you'll burn hours on the UI before realizing the data never arrived.

> **Read this as symptom → where-to-look, not as a recipe.** The durable part of each item is the
> *symptom* and the *source-of-truth file/class* it names. The concrete mock-state names
> (`USDTHotWalletSvS`, `BitcoinExchangeEnabled`, …), mock classes (`Wallet2WithDerivationsMockContent`),
> string resources, and even "a token needs its native coin for the fee" are **a snapshot that will
> drift** — verify each against the cited source before trusting it. The change-proof skills are the
> *symptom → category* mapping here plus the two diagnostics in `running-and-debugging.md` (the app's
> `files/log.txt` and the WireMock request journal), which surface the *current* cause regardless of
> renames. If the specifics below stop matching, don't patch around them — re-derive from source and
> update this doc.

## Accounts mode: the main screen shows ACCOUNT cards, not a token list

With a two-accounts mock (`user_tokens_api=TwoAccountsSame…`, served via the `/v1/wallets/{id}/accounts`
endpoint), the main screen renders `MAIN_SCREEN_ACCOUNT_LIST_ITEM` cards ("Account 1", "Account 2"), NOT a
flat token list. `tokenWithTitleAndAddress("Bitcoin")` finds nothing. To reach a token: expand the
account, then click the token inside it.

```kotlin
onMainScreen { scrollToAccount("Account 1") }                                  // semantics scroll (see traps)
onMainScreen { findAccountSectionByName("Account 1").clickWithAssertion() }    // expand the account
onMainScreen { findTokenInAnyAccountByName("Bitcoin").clickWithAssertion() }   // token inside the account
```

The Swap **receive** selector also groups assets by account and renders the *other* account **collapsed** —
expand its group header before tapping the identical token (`tokenWithName("Account 2")` then
`tokenWithName("Bitcoin")`). Reuse `openSwapInTransferMode(token, fromAccountName, toAccountName)` in
`SwapScenarios.kt`, which encapsulates this.

## Four mock prerequisites for a token to be transferable / swappable

A transfer or swap depends on ALL of these. Each missing one fails differently:

1. **Native coin present — else the fee never computes.** To compute an ERC20 token's transfer/send fee
   the wallet must contain the token's NATIVE coin (e.g. Ethereum for USDT-on-ethereum). If the
   user-tokens mock lists only the token, `GetFeeUseCase` → `getFee` raises
   `IllegalStateException: No native currency found` → fee stays "—" → the fee-warning banner never shows
   and the action button stays disabled. **Fix: add the native coin to each account** in the mock
   response (`{"name":"Ethereum","symbol":"ETH","networkId":"ethereum","decimals":18,"id":"ethereum",
   "derivationPath":"<account's EVM path>"}`).
2. **A price quote — else fiat = $0 → empty-wallet banner.** A token with a balance but no price → total
   fiat $0 → the "Get your first crypto" banner appears and pushes the account list down **under the
   Markets sheet** (then account navigation can't reach the cards). **Fix: set the quotes scenario that
   prices the token** (`quotes_api=Solana`, `quotes_api=USDTHotWalletSvS`, …).
3. **Exchange-enabled — else the token-details Swap button is disabled.** Set `express_api_assets` to a
   state marking the token `exchangeAvailable=true` (e.g. `BitcoinExchangeEnabled`).
4. **Matching derivation style — else `isCustom` → Swap disabled.** The default `Wallet` mock is
   derivation-style **V2**; for a segwit/BIP-84 coin the user-tokens stub sends `m/84'/…`, which V2
   resolves as a *custom* path → `CryptoCurrency.isCustom == true` → `CommonActionsFactory.createSwapAction`
   returns `CustomToken` → Swap button disabled. **Fix: scan a V3 card** —
   `openSwapInTransferMode(token, mockContent = Wallet2WithDerivationsMockContent)` — so `m/84'` is the
   card's default path and the coin isn't custom.

Account-2 derivations: `Wallet2WithDerivationsMockContent.derivationTaskResponse` re-keys
`WalletMockContent`'s entries, so a missing per-account path (e.g. Bitcoin account-2 `m/84'/0'/1'/0/0`)
must be added to `WalletMockContent`'s `derivationTaskResponse` for the receive account to resolve.

## "Insufficient funds" vs "insufficient fee" are DIFFERENT banners

Don't confuse them when porting (iOS often asserts "any notification", which masks which one fired):

- **amount > token balance** → `swapping_insufficient_funds` ("Insufficient funds … Reduce the amount") —
  shows immediately, no fee needed.
- **native coin < fee** → `warning_send_blocked_funds_for_fee_title` ("Insufficient `<Coin>` to cover
  network fee", from `NotificationUM.Error.TokenExceedsBalance`). This fires only once a fee is **computed**
  (`fee > 0`, via `GetBalanceNotEnoughForFeeWarningUseCase`), so prerequisite #1 is mandatory and the
  native balance must be set to **zero/insufficient** (`eth_network_balance=EmptyAnyId`) *with the coin
  present*.

## Why an iOS transfer test may assert something the screen can't show

iOS `waitForNotificationShown()` checks "any `notificationTitle` exists", so the iOS test goes green on a
generic "Error" banner when the fee fails to load — it does **not** prove the intended banner rendered.
Re-derive the correct Android notification string from production source and assert *that* specific banner;
treat the iOS assertion's leniency as a hint, not a spec.