# Yield Mode (yield-supply) UI tests

Hard-won specifics for testing the Yield Mode feature (`features/yield-supply`, analytics category
"Earning"). The first test (`app/.../tests/yield/YieldModeTest.kt`, case #4938 "first-time landing
activation") is the reference — mirror it. Read this before writing any yield test.

## What the feature is

Depositing a stablecoin (USDC/USDT) into a DeFi protocol (Aave) from the app to earn APY. Distinct from
Staking (`domain/staking`) but both render the shared `EarnBlock` (`common/ui/.../earn/`) on token
details. Activation = a **real signed transaction** (approve + enter), so it needs a **hot wallet**
(mock card can't sign). Use `openMainScreenWithExistingHotWallet(SVS_SEED_PHRASE_12)`.

## Network: use Ethereum, NOT Polygon

- **Ethereum USDC** activation is a **native ETH** transaction → the standard send path works.
- **Polygon USDC** activation routes through the **gasless** flow (`GASLESS_APPROVAL_ENABLED=true` +
  Polygon USDC is gasless-eligible) → `gaslessTransaction()`; the SDK's `EthereumTransactionValidator`
  throws `FailedToSendException` synchronously and the send never completes. Avoid Polygon for yield
  send/activation tests unless you specifically mock the whole gasless-v2 stack.
- Wallet (SVS_SEED_PHRASE_12) EVM address: `0x3369554b994908d249d307b105f8e5e3115615c2`.
- Ethereum USDC contract: `0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48` (chainId 1, decimals 6).

## Running (same as the general doc, repeated because it bit us)

Run **via the orchestrator**, never raw `am instrument` (with `am instrument` the fresh-wallet portfolio
silently never loads):

```bash
curl -s -X POST http://localhost:8081/__admin/mappings/reset      # after editing mocks
./gradlew :app:connectedGoogleMockedAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.tangem.tests.yield.YieldModeTest \
  -Pandroid.testInstrumentationRunnerArguments.wiremockBaseUrl=http://10.0.2.2:8081
```

WireMock on 8081 is a docker container bind-mounting the `tangem-api-mocks` working tree — `mappings/reset`
reloads from disk, no rebuild. App-side logs (`TangemLogger`) also reach logcat; the failure semantics
tree is dumped to logcat (`ComposeTree`).

## Mock set (in tangem-api-mocks, branch `feature/AND-16082_yield_mode_mocks`)

- **Yield API** (`mappings/yield-api/api/v1/`): `yield/markets`, `yield/token/1/{usdc}` (+ `/chart`),
  `module/activate`, `module/deactivate`. Base URL is `wiremock.tests-d.com` (see infra fix below) so the
  path is `/api/v1/yield/...`.
- **On-chain eth_call** (`mappings/providers/ethereum/eth-call-yield.json`, priority 1 to beat the generic
  `eth-call.json`): factory `getModule` (to=`0xd8972a45...`), processor service-fee (to=`0x4ff6178b...`),
  yield status `0xf8e8be9c` + balances `0x16a398f7`/`0x5002bb7e` (to = module `0x1111…`), allowance
  `0xdd62ed3e` (to = USDC) → 0. Status/balances gated by scenario `yield_supply_status` (NotActive=zeros,
  Active=`…01`,`…01`,`…8ac7230489e80000`).
- **History** (`mappings/eth-blockbook.nownodes.io/`): NowNodes BlockBook v2. Ethereum mainnet tx history
  is **NowNodes eth-blockbook**, NOT Etherscan and NOT `/v2/transaction-events` (the latter is a
  push-dedup POST). The host is redirected by the interceptor (infra fix below).
- **Portfolio**: scenario `user_tokens_api` state `YieldUSDCEthereum` (accounts API; native ETH + USDC),
  balances via `moralis_evm_token_balances_api=NonZeroEvmBalances`.

## Scenarios the test drives

`user_tokens_api=YieldUSDCEthereum`, `moralis_evm_token_balances_api=NonZeroEvmBalances`, and
`yield_supply_status` **NotActive → (after activation) Active**. The Active flip is what turns the
EarnBlock from "available" into the active "Yield Mode enabled / Average APY" state AND surfaces the
history row.

## Gotchas that cost real time

1. **`isActive` in `yield/markets` & `yield/token` means "market is available", not "user activated".**
   It MUST be `true` or `YieldSupplyTokenStatusSuccessTransformer` returns `Unavailable` and the block
   never renders. User activation is tracked separately by the on-chain `eth_call` status flip.
2. **`yieldSupplyKey` matching is case-sensitive string equality** (`"${backendId}_$tokenAddress"` vs
   `"${network.rawId}_$contractAddress"`). All addresses in mocks must be **lowercase** — every existing
   mock is. A checksummed address yields `yieldSupplyApy size=0` and no available block.
3. **Active vs available is driven by `CryptoCurrencyStatus.value.yieldSupplyStatus.isActive`** (on-chain
   eth_call `0xf8e8be9c`), not the API `isActive`. Flip the `yield_supply_status` scenario + pull-to-refresh.
4. **Infra fix (production, already applied):** `YieldSupply` ApiConfig MOCK base URL was `yield.tests-d.com`
   (NOT redirected by `WireMockRedirectInterceptor`) → fixed to `wiremock.tests-d.com`. And
   `eth-blockbook.nownodes.io` was added to `REDIRECTABLE_THIRD_PARTY_HOSTS`. Without these the yield/history
   requests bypass WireMock entirely.
5. **Hold-to-confirm must wait for the fee.** The "Start earning" `HoldToConfirmButton` is disabled
   (`holdToConfirmGestures(enabled=false)` swallows the gesture) until the fee is calculated. Don't gate on
   the high-fee notification (it's absent on cheap chains) — use the fee-agnostic retry: a single step that
   `flakySafely`-retries `longClick(HOLD_DURATION_MS)` then asserts the sheet closed
   (`startEarningButton.assertIsNotDisplayed()`). `state.isConfirmed` makes re-holds no-ops, so no double-send.
   A hold step that finishes in ~270 ms instead of ~2 s = the gesture was swallowed (button disabled).
6. **The yield-enter history row is a `TransactionItemUM.Pill`** (converter maps `YieldSupply.Enter` →
   Pill, like Approve/Staking), rendered by `TransactionStatusPill`. It now carries
   `TransactionHistoryItemTestTags.ITEM` (added for parity with `ContentItem`) so `transactionItem(title)`
   finds it. Title string = `yield_module_transaction_enter` ("Yield Mode enabled" / "Режим доходности
   подключен"). The row opens the explorer on click.
7. **"Nothing to add to TxHistory"** in logs is NOT an error — it refers to recent/pending txs, separate
   from the API history.

## testTags added to production (reuse, don't re-add)

`TokenDetailsScreenTestTags.YIELD_SUPPLY_BLOCK` / `YIELD_SUPPLY_AVAILABLE_BLOCK`; `YieldSupplyTestTags`
(`PROMO_CONTINUE_BUTTON`, `START_EARNING_BUTTON`); `TransactionHistoryItemTestTags.ITEM` now on
`TransactionStatusPill`. Page objects: yield locators in `TokenDetailsPageObject`,
`YieldSupplyPromoPageObject`, `YieldSupplyStartEarningPageObject`.