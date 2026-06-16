# Swap Feature

Token-to-token exchange. Users pick FROM and TO tokens, get quotes from providers
(DEX/CEX), approve ERC-20 allowances if needed, and execute the swap.

## Module map

```
features/swap/
  api/      — Public contracts (SwapComponent, SwapFeatureToggles)
  impl/     — UI, SwapModel, navigation, DI, token-selection subfeature
  domain/   — SwapInteractor + domain models
    api/    — Domain interfaces (SwapRepository)
    models/ — SwapPair, SwapProvider, SwapState, …
    fee/    — Fee calculation (see Fee Architecture)
  data/     — Repository impls, Retrofit APIs, Moshi DTOs
```

**Package quirk:** API = `com.tangem.features.swap`, Impl = `com.tangem.feature.swap`
(singular `feature` — legacy inconsistency, follow it).

**Build / test:**
```bash
./gradlew :features:swap:impl:compileDebugKotlin
./gradlew :features:swap:domain:test
./gradlew :features:swap:impl:detekt
```

## Where to start reading

| Symbol | Role | Path |
|---|---|---|
| `SwapComponent` | API entry point; `Params(userWalletId, cryptoCurrency?, screenSource, currencyPosition, tangemPayInput, toCryptoCurrency?)` | `api/.../features/swap/SwapComponent.kt` |
| `DefaultSwapComponent` | Decompose component; creates `SwapModel`, owns the child stack + slots | `impl/.../feature/swap/DefaultSwapComponent.kt` |
| `SwapModel` | Central coordinator (~2100 lines). State holder + fee-selector bridge | `impl/.../feature/swap/model/SwapModel.kt` |
| `SwapProcessDataState` | Live domain state for the session (tokens, pairs, providers, `swapDataModel`, amount) | `impl/.../feature/swap/model/SwapProcessDataState.kt` |
| `StateBuilder` | Pure builder: `SwapProcessDataState` → `SwapStateHolder` (Compose UI state) | `impl/.../feature/swap/ui/StateBuilder.kt` |
| `SwapRouter` | Wraps `AppRouter` + `StackNavigation<SwapRoute>`; custom `back()` per route | `impl/.../feature/swap/router/SwapRoute.kt` |
| `SwapInteractor` | Domain API; `loadSwapFee` / `applySwapFee` are the unified fee entry points | `domain/.../feature/swap/domain/SwapInteractor.kt` |
| `SwapInteractorImpl` | ~28 deps; `findBestQuote` dispatches per-provider via `supervisorScope + async` | `domain/.../feature/swap/domain/SwapInteractorImpl.kt` |

`toCryptoCurrency` pre-selects the **TO** (receive) token, but only if it is already present in the user's
crypto portfolio — resolved by `InitialCurrenciesResolver` (matched by token identity / `isSameTokenAs`,
preferring the FROM account's instance). If the token isn't in the wallet, the TO slot stays empty. Used by
Send-with-Swap's "Swap token" notice when a pair is available only in the regular Swap flow.

`SwapModel` state worth knowing: `dataStateStateFlow` (reactive domain data) and
`uiState: SwapStateHolder` (Compose state); the inner `FeeSelectorRepository` wires the
send-v2 fee selector to `SwapInteractor.loadSwapFee`/`applySwapFee`.

## Navigation

```
AppRoute.Swap → DefaultSwapComponent
  ├─ childStack(SwapRoute)
  │    ├─ Main        → SwapScreen
  │    ├─ Success     → SwapSuccessScreen
  │    └─ SelectToken → ChooseTokenComponent (FROM or TO bridge)
  ├─ SlotNavigation<Unit>             → GiveApprovalComponent (bottom sheet)
  └─ SlotNavigation<FeeSelectorConfig> → SwapFeeSelectorBlockComponent (inline)
```

Injected factories on `DefaultSwapComponent`: `SwapFeeSelectorBlockComponent.Factory`,
`GiveApprovalComponent.Factory`, `ChooseTokenComponent.Factory`.

Token selection: tapping FROM/TO pushes `SwapRoute.SelectToken(isFromDirection)`; the
`ChooseTokenComponent` returns its result over a `ChooseTokenBridge` Channel
(`onCurrencyChosen` → `onTokenSelect`). Swap execution: `onSwapClick()` → (approval slot if
needed) → on success `SwapRoute.Success`.

## Token selection subfeature (`impl/choosetoken/`)

Self-contained. `ChooseTokenComponent` (with `ChooseTokenModel`) communicates via
`ChooseTokenBridge` (Channel-based: `onCurrencyChosen`, `onClose`). `Settings` is `SwapFrom`
(no market block) vs `SwapTo` (with market block). Result type `ChooseTokenResult` carries
`CryptoCurrencyStatus`, `AccountStatus`, `UserWallet`.

## Fee Architecture

Single unified entry: `SwapInteractor.loadSwapFee()` → strategy calculator → `SwapFee`
carrier; `applySwapFee()` patches the loaded quote without re-fetching.

```
loadSwapFee()
  ├─ DEX/DEX_BRIDGE → DexSwapFeeCalculator.calculate() → DexFeeResult
  │     ├─ Solana: TransactionData.Compiled (NO gas bump)
  │     └─ EVM:    Uncompiled + patchEthGasLimitForSwap(DEX=112%)
  │           └─ fallback GetEthSpecificFeeUseCase on IllegalStateException
  └─ CEX → CexSwapFeeCalculator.calculate() → CexFeeResult
        ├─ feeToken == null → EstimateFeeForGaslessTxUseCase (no bump)
        ├─ feeToken Token   → EstimateFeeForTokenUseCase     (no bump)
        └─ feeToken Coin    → EstimateFeeUseCase + patchEthGasLimitForSwap(SEND=105%)

SwapFeeFactory.from(...) → SwapFee   (the single fee carrier downstream)
applySwapFee(state, fee) → patches QuotesLoadedState.balanceStatus / currencyCheck / validationResult
```

Fee types (all under `domain/fee/` unless noted) — open the file for fields:
`SwapFee` (`domain/models/ui/`, the carrier), `FeeBucket` (`domain/models/ui/`,
`SLOW/MARKET/FAST/SUGGESTED/CUSTOM` + `toAnalyticsName()`), `TransactionFeeResult` (sealed:
`Loaded` native / `LoadedExtended` gasless+token), `DexFeeResult`, `CexFeeResult`,
`DexSwapFeeCalculator`, `CexSwapFeeCalculator`, `SwapFeeFactory`, `PatchEthGasLimitForSwap`.

**Fee selector wiring** (`SwapModel.FeeSelectorRepository`, implements
`SwapFeeSelectorBlockComponent.ModelRepositoryExtended`): `loadFeeExtended`/`loadFee` call
`loadSwapFee`; `onResult(FeeSelectorUM)` calls `applySwapFee` on `Content` and updates
`dataState.lastLoadedSwapStates`. `getSelectedSwapFee()` reconstructs a `SwapFee` from the
selector's current `Content` state. `FeeItem.toFeeBucket()` maps UI → bucket.

**`otherNativeFee` (DEX bridge only):** `ExpressTransactionModel.DEX.otherNativeFeeWei`
(present only for `DEX_BRIDGE`) → converted in `DexSwapFeeCalculator` → `SwapFee.otherNativeFee`.
`applySwapFee` checks balance against `fee.amount.value + otherNativeFee`;
`resolveOtherNativeFee()` re-reads it from `dataState.swapDataModel.transaction`.

## Key domain models

`SwapState` (sealed, `domain/models/ui/SwapState.kt`): `QuotesLoadedState`, `Transfer`,
`EmptyAmountState`, `SwapError`. `QuotesLoadedState` carries `preparedSwapConfigState`
(balance/fee checks), `permissionState`, `swapDataModel`, `currencyCheck`, `validationResult`,
`swapProvider`. Other types — open the file: `SwapProvider` (has `type: ExchangeProviderType`
= DEX/CEX/DEX_BRIDGE), `SwapPairLeast`, `SwapDataModel` (`transaction: ExpressTransactionModel`
sealed DEX/CEX, `domain/models/domain/`), `SwapAmount`, `TokenSwapInfo`.

Transfers: `SwapTransferInteractor` handles same-wallet same-currency moves —
`shouldTransferInsteadOfSwap` → `SwapState.Transfer` (no quote, no fee).

## DI modules (where bindings live)

| Module | Provides |
|---|---|
| `SwapFeatureModule` | `SwapComponent.Factory`, `SwapFeatureToggles` |
| `SwapModelModule` / `SwapEntryModule` / `ChooseTokenModule` | Models into the model map + their factories |
| `SwapDomainModule` | `DexSwapFeeCalculator`, `CexSwapFeeCalculator`, the two qualified `PatchEthGasLimitForSwap` |
| `SwapDomainBindModule` | `SwapInteractor`/`SwapTransferInteractor` → impls |

Two `PatchEthGasLimitForSwap` instances are distinguished by `@SwapDexGasLimit` (112%) vs
`@SwapSendGasLimit` (105%) — qualifiers in `domain/di/SwapFeeQualifiers.kt`.

## UI & analytics

UI under `impl/.../feature/swap/ui/`: `SwapScreen` (main), `SwapSuccessScreen`,
`SwapScreenContent` (ConstraintLayout card positioning), `TransactionCard`. Cards pass
`TokenSelectionDirection.FROM`/`.TO` to `onSelectTokenClick`.

Analytics: `SwapEvents` sealed hierarchy (`impl/.../analytics/SwapEvents.kt`). Fee tier name
comes from `FeeBucket.toAnalyticsName()` (`Min/Normal/Max/Suggested/Custom`) → `AnalyticsParam.FeeType.fromString(...)`.

## Testing

Domain tests: JUnit 5 + MockK + Truth. Base `SwapInteractorImplTestBase` wires all ~30 deps
as relaxed mocks, exposes `sut` lazily, and holds builders (`buildSwapCurrencyStatus`, …) —
extend it and stub only what you need. Test files mirror topics: `…LoadSwapFeeTest`,
`…ApplySwapFeeTest`, `…FindBestQuoteTest`, `…LoadDexSwapDataNoFeeTest`,
`fee/{Dex,Cex}SwapFeeCalculatorTest`, `fee/SwapFeeFactoryTest`, `fee/PatchEthGasLimitForSwapTest`,
`transfer/SwapTransferInteractorImplTest`, `impl/StateBuilder*Test`.

## Gotchas

**Fee state is transient on DEX.** `loadDexSwapDataNoFee` returns a `QuotesLoadedState` with
`feeState = NotEnough()` and `isBalanceEnough = false`. Real values are only set after the fee
selector resolves and `applySwapFee` runs. Do not check `preparedSwapConfigState.isBalanceEnough`
before the fee selector has emitted `FeeSelectorUM.Content`.

**`SwapFee` is not stored in `SwapProcessDataState`.** It is reconstructed from
`feeSelectorRepository.state.value` via `getSelectedSwapFee()` at each call site (swap execution,
analytics). `otherNativeFee` must be re-read from `dataState.swapDataModel.transaction` because
`FeeSelectorUM` does not carry it.

**DEX requires a pre-fetched `swapDataModel`.** `FeeSelectorRepository.loadFeeExtended` returns
`Left(UnknownError)` when `dataState.swapDataModel == null`. By design: `manageDex` only calls
`loadDexSwapDataNoFee` (which populates it) when allowance is OK and balance is sufficient. With
insufficient balance or pending approval, the fee selector will not load.

**Two `PatchEthGasLimitForSwap` instances, different percentages.** DEX 12%, CEX 5%, selected by
`@SwapDexGasLimit` / `@SwapSendGasLimit`. Passing the wrong qualifier is a silent bug — no
compile-time check.

**`Fee.Ethereum.TokenCurrency` throws.** `PatchEthGasLimitForSwap.increaseEthGasLimitInNeeded`
calls `error("handle in [REDACTED_TASK_KEY]")` for `TokenCurrency`. This path must not be reached in
production (issue tracked, not yet resolved).

**Solana DEX fee is not patched.** `DexSwapFeeCalculator` skips `patchEthGasLimitForSwap` on
Solana paths. Also: a compiled tx exceeding `SOLANA_TRANSACTION_SIZE_THRESHOLD_BYTES` on a
`UserWallet.Cold` raises `ExpressDataError.TooLargeSolanaTransactionError`.

**`TransactionFeeResult` is not a data class.** `Loaded` / `LoadedExtended` are regular classes,
so structural equality does not hold — use `is`-checks + field comparison in tests.

**Don't reference the removed fee API.** The unified API is `loadSwapFee` / `applySwapFee`. The
old `loadFeeForSwapTransaction` overloads, `loadFeeForDex`, `getFeeForCex`, and
`FeeType.getNameForAnalytics()` were removed — do not reintroduce them in new code or tests.

**Transfer mode vs swap mode.** `shouldTransferInsteadOfSwap` returns `true` for same-wallet
same-currency pairs → UI shows `SwapState.Transfer`, not `QuotesLoadedState`, and no fee selector.