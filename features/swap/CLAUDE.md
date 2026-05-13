# Swap Feature

Token-to-token exchange feature. Users select FROM and TO tokens, get quotes from providers (DEX/CEX), approve ERC-20 allowances if needed, and execute swaps.

## Module Structure

```
features/swap/
  api/          — Public contracts (SwapComponent, SwapFeatureToggles)
  impl/         — UI, model, navigation, DI, token selection subfeature
  domain/       — Business logic (SwapInteractor) + domain models
    api/        — Domain interfaces (SwapRepository)
    models/     — Domain model types (SwapPair, SwapProvider, SwapState, etc.)
    fee/        — Fee calculation package (see Fee Architecture below)
  data/         — Repository implementations, Retrofit APIs, Moshi DTOs
```

**Package naming:** API = `com.tangem.features.swap`, Impl = `com.tangem.feature.swap` (singular `feature`, legacy inconsistency).

**Build commands:**
```bash
./gradlew :features:swap:impl:compileDebugKotlin
./gradlew :features:swap:api:compileDebugKotlin
./gradlew :features:swap:domain:compileDebugKotlin
./gradlew :features:swap:domain:test
./gradlew :features:swap:impl:detekt
```

## Key Components

### SwapComponent (API)
Entry point. `Params` requires `userWalletId`, optional `cryptoCurrency`, `screenSource`, `currencyPosition` (`FROM`/`TO`/`ANY`), and `tangemPayInput`.

File: `features/swap/api/src/main/kotlin/com/tangem/features/swap/SwapComponent.kt`

### DefaultSwapComponent (impl)
Decompose component. Creates `SwapModel` via `getOrCreateModel(params)`.

**Child navigation:**
- `childStack(SwapRoute)` — `SwapRoute.Main`, `SwapRoute.Success`, `SwapRoute.SelectToken(isFromDirection)`, rendered via `Children` with fade animation
- `SlotNavigation<Unit>` — approval bottom sheet (`GiveApprovalComponent`)
- `SlotNavigation<FeeSelectorConfig>` — fee selector block

**Injected factories:** `SwapFeeSelectorBlockComponent.Factory`, `GiveApprovalComponent.Factory`, `ChooseTokenComponent.Factory`.

File: `features/swap/impl/src/main/java/com/tangem/feature/swap/DefaultSwapComponent.kt`

### SwapModel (impl)
`@ModelScoped`, extends `Model()`. Central coordinator — ~2100 lines.

**Key state:**
- `dataStateStateFlow: MutableStateFlow<SwapProcessDataState>` — reactive domain data (from/to tokens, pairs, providers, amounts, fees)
- `uiState: SwapStateHolder by mutableStateOf()` — Compose UI state built by `StateBuilder`
- `feeSelectorRepository: FeeSelectorRepository` — inner class that implements `SwapFeeSelectorBlockComponent.ModelRepositoryExtended`; wires the fee selector UI component to `SwapInteractor.loadSwapFee` and `SwapInteractor.applySwapFee`
- `stackNavigation: StackNavigation<SwapRoute>` — stack navigation exposed from `SwapRouter`
- `approvalSlotNavigation: SlotNavigation<Unit>` — approval bottom sheet

File: `features/swap/impl/src/main/java/com/tangem/feature/swap/model/SwapModel.kt`

**Initialization flow (init block):**
1. Subscribes to `chooseTokenBridge.onCurrencyChosen` → `onTokenSelect(result)`
2. Subscribes to `chooseTokenBridge.onClose` → pops slot navigation
3. Checks `ShouldShowStoriesUseCase` → pushes `AppRoute.Stories` if first-time swap
4. Resolves user country for FCA restrictions
5. Loads primary account status, initial currencies, and starts swap pair loading

**Token selection flow:**
1. User taps FROM or TO card → `onSelectTokenClick(direction)` pushes `SwapRoute.SelectToken(isFromDirection)` to stack
2. Stack creates `ChooseTokenComponent` with appropriate bridge (FROM or TO)
3. `ChooseTokenBridge` communicates selection result via Channel
4. `onTokenSelect(result)` assigns selected token to FROM or TO based on `isFromDirection`

**Swap execution flow:**
1. `onSwapClick()` — validates state, checks approval, initiates transaction
2. If approval needed → `approvalSlotNavigation.activate(Unit)`
3. On approval done → reloads quotes
4. On swap success → `swapRouter.openScreen(SwapRoute.Success)`

### SwapProcessDataState (impl)
Data class holding the live domain state for the current swap session.

Key fields: `fromSwapCurrencyStatus`, `toSwapCurrencyStatus`, `feePaidCryptoCurrency`, `pairs: List<SwapPairLeast>`, `selectedProvider`, `lastLoadedSwapStates: Map<SwapProvider, SwapState>`, `swapDataModel: SwapDataModel?`, `amount: String?`, `reduceBalanceBy`.

`getCurrentLoadedSwapState()` — convenience to get `lastLoadedSwapStates[selectedProvider] as? QuotesLoadedState`.

File: `features/swap/impl/src/main/java/com/tangem/feature/swap/model/SwapProcessDataState.kt`

### StateBuilder (impl)
Pure transformation class. Takes `UiActions` + providers, builds `SwapStateHolder` from `SwapProcessDataState`.

Key methods: `createInitialLoadingState`, `createQuotesLoadedState`, `createSuccessState`, `loadingPermissionState`, `updateSwapAmount`, `addNotification`, `dismissBottomSheet`.

File: `features/swap/impl/src/main/java/com/tangem/feature/swap/ui/StateBuilder.kt`

### SwapRouter (impl)
Wraps `AppRouter` + `StackNavigation<SwapRoute>`. `openScreen(SwapRoute)` pushes/replaces stack entries. `back()` has special logic: SelectToken pops local stack, Success exits to the screen before SwapCrypto in the app stack, Main pops AppRouter. `openTokenDetails()` navigates to `AppRoute.CurrencyDetails`.

File: `features/swap/impl/src/main/java/com/tangem/feature/swap/router/SwapRoute.kt`

## Token Selection Subfeature (impl)

Self-contained within `choosetoken/` package:
- `ChooseTokenComponent` — API with `Params(bridge, settings, analyticsPayload)`
- `ChooseTokenBridge` — Channel-based communication: `onCurrencyChosen`, `onClose`, `onTokenSelected` (legacy), `onNewTokenAdded` (legacy). Has `settingsStateFlow` for dynamic settings.
- `ChooseTokenComponent.Settings` — `SwapFrom` (no market block) vs `SwapTo` (with market block)
- `ChooseTokenResult` — Contains `CryptoCurrencyStatus`, `AccountStatus`, `UserWallet`
- `DefaultChooseTokenComponent` — Has its own `ChooseTokenModel` and optional `AddToPortfolioComponent` bottom sheet slot

## Domain Layer

### SwapInteractor (interface)

File: `features/swap/domain/src/main/java/com/tangem/feature/swap/domain/SwapInteractor.kt`

All public methods:
- `getPair(from, to, filterProviderTypes)` → `Either<ExpressError, List<SwapPairLeast>>`
- `findProvidersForPair(from, to, pairs)` → `List<SwapProvider>`
- `findProvidersForPairWithCheck(from, to, pairs)` → `List<SwapProvider>` (checks asset requirements/FCA)
- `findBestQuote(from, to, providers, amount, reduceBalanceBy)` → `Map<SwapProvider, SwapState>` (parallel per-provider)
- `onSwap(from, to, provider, swapData, amount, includeFeeInAmount, fee, operationType, isTangemPayWithdrawal)` → `SwapTransactionState`
- `loadSwapFee(provider, fromStatus, toStatus, amount, swapData, selectedFeeToken)` → `Either<GetFeeError, SwapFee>` — unified fee entry point (see Fee Architecture)
- `applySwapFee(state: QuotesLoadedState, fee: SwapFee)` → `QuotesLoadedState` — patches balance checks without re-fetching quotes
- `getTokenBalance(token)` → `SwapAmount`
- `getNativeToken(swapCurrencyStatus)` → `CryptoCurrency`
- `storeSwapTransaction(...)` — persists transaction for status tracking

### SwapInteractorImpl (impl)

File: `features/swap/domain/src/main/java/com/tangem/feature/swap/domain/SwapInteractorImpl.kt`

`@Inject` constructor with ~28 dependencies. Key injected components:
- `dexSwapFeeCalculator: DexSwapFeeCalculator` — fee calculation for DEX/DEX_BRIDGE
- `cexSwapFeeCalculator: CexSwapFeeCalculator` — fee calculation for CEX

`findBestQuote` dispatches per-provider using `supervisorScope + async`:
- `ExchangeProviderType.DEX` / `DEX_BRIDGE` → `manageDex(...)` or `manageDexSolana(...)`
- `ExchangeProviderType.CEX` → `manageCex(...)`

For DEX (non-Solana): if allowance OK and balance sufficient → `loadDexSwapDataNoFee(...)` which fetches exchange data but sets `feeState = NotEnough()` transiently. Fee is applied later via `applySwapFee`.

`onSwap` dispatch:
- CEX → `onSwapCex(...)` — fetches exchange data, then either `createAndSendGaslessTransactionUseCase` (token fee) or `sendTransactionUseCase` (native fee)
- DEX non-Solana → `onSwapDex(...)` — `createTransactionUseCase` with `createDexTxExtras(..., gasLimit = fee.fee.getGasLimit())`
- DEX Solana → compiled tx signed as-is; `fee` is only used for analytics/UI

### SwapTransferInteractor / SwapTransferInteractorImpl (domain)

Handles within-wallet transfers (same-wallet, same-account coin moves). `shouldTransferInsteadOfSwap(from, to)` detects same-wallet same-currency pairs. `updateTransfer(from, to, amount)` returns a `SwapState.Transfer` (not a quote). No fee calculation involved.

Files:
- `features/swap/domain/src/main/java/com/tangem/feature/swap/domain/transfer/SwapTransferInteractor.kt`
- `features/swap/domain/src/main/java/com/tangem/feature/swap/domain/transfer/SwapTransferInteractorImpl.kt`

## Fee Architecture (post [REDACTED_TASK_KEY] refactor)

The fee subsystem was fully redesigned across three tickets ([REDACTED_TASK_KEY], [REDACTED_TASK_KEY], [REDACTED_TASK_KEY], [REDACTED_TASK_KEY]). All legacy `loadFeeForSwapTransaction`, `loadFeeForDex`, `getFeeForCex` overloads have been **removed**. The current design:

### Class Hierarchy

```
SwapInteractor.loadSwapFee()          ← unified entry point (Phase 3)
  ├─ DEX/DEX_BRIDGE → DexSwapFeeCalculator.calculate()  → DexFeeResult
  │     ├─ Solana path: TransactionData.Compiled (no gas bump)
  │     └─ EVM path:   TransactionData.Uncompiled + patchEthGasLimitForSwap(DEX_PERCENTAGE=112)
  │           └─ fallback: GetEthSpecificFeeUseCase on IllegalStateException
  └─ CEX → CexSwapFeeCalculator.calculate()              → CexFeeResult
        ├─ selectedFeeToken == null   → EstimateFeeForGaslessTxUseCase (no gas bump)
        ├─ selectedFeeToken is Token  → EstimateFeeForTokenUseCase     (no gas bump)
        └─ selectedFeeToken is Coin   → EstimateFeeUseCase + patchEthGasLimitForSwap(SEND_PERCENTAGE=105)

SwapFeeFactory.from(transactionFeeResult, selectedFeeToken, otherNativeFee, feeBucket)
  → SwapFee (the single fee carrier used everywhere downstream)

SwapInteractor.applySwapFee(state, fee)  ← patches QuotesLoadedState (Phase 4)
  → recomputes isBalanceEnough, feeState, includeFeeInAmount, currencyCheck, validationResult
```

### Key Types

| Type | File | Purpose |
|------|------|---------|
| `SwapFee` | `domain/models/ui/SwapFee.kt` | Unified carrier: `fee: Fee`, `transactionFeeResult: TransactionFeeResult`, `selectedFeeToken: CryptoCurrencyStatus`, `otherNativeFee: BigDecimal`, `feeBucket: FeeBucket` |
| `FeeBucket` | `domain/models/ui/FeeBucket.kt` | `SLOW/MARKET/FAST/SUGGESTED/CUSTOM`; `toAnalyticsName()` replaces legacy `FeeType.getNameForAnalytics()` |
| `TransactionFeeResult` | `domain/fee/TransactionFeeResult.kt` | Sealed: `Loaded(TransactionFee)` for native, `LoadedExtended(TransactionFeeExtended)` for gasless/token |
| `DexFeeResult` | `domain/fee/DexFeeResult.kt` | `transactionFee`, `otherNativeFee`, `gas: BigInteger?` |
| `CexFeeResult` | `domain/fee/CexFeeResult.kt` | `transactionFee: TransactionFeeResult` |
| `DexSwapFeeCalculator` | `domain/fee/DexSwapFeeCalculator.kt` | Solana vs EVM branching, 12% gas bump |
| `CexSwapFeeCalculator` | `domain/fee/CexSwapFeeCalculator.kt` | gasless/token/native branching, 5% gas bump |
| `SwapFeeFactory` | `domain/fee/SwapFeeFactory.kt` | `fromLoaded`, `fromLoadedExtended`, `from` (polymorphic) + `selectFee` for bucket picking |
| `PatchEthGasLimitForSwap` | `domain/fee/PatchEthGasLimitForSwap.kt` | Multiplies ETH gas limit. `DEX_PERCENTAGE=112`, `SEND_PERCENTAGE=105` |

### DI for Fee Classes

Two `PatchEthGasLimitForSwap` instances with `@Qualifier`:
- `@SwapDexGasLimit` → `DEX_PERCENTAGE=112` → injected into `DexSwapFeeCalculator`
- `@SwapSendGasLimit` → `SEND_PERCENTAGE=105` → injected into `CexSwapFeeCalculator`

Qualifiers: `features/swap/domain/src/main/java/com/tangem/feature/swap/domain/di/SwapFeeQualifiers.kt`
Bindings: `features/swap/domain/src/main/java/com/tangem/feature/swap/domain/di/SwapDomainModule.kt`

### Fee Selector Wiring (SwapModel.FeeSelectorRepository)

`SwapModel` contains an inner class `FeeSelectorRepository` that implements `SwapFeeSelectorBlockComponent.ModelRepositoryExtended`. This is the bridge between the send-v2 fee selector UI component and the swap domain:

- `loadFeeExtended(selectedToken)` → calls `swapInteractor.loadSwapFee(...)`, wraps result as `TransactionFeeExtended` for the fee selector block
- `loadFee()` → same path, extracts `TransactionFee` from the `SwapFee` result
- `onResult(newState: FeeSelectorUM)` → when fee selector emits `Content`, calls `swapInteractor.applySwapFee(currentQuotesLoadedState, swapFee)` and updates `dataState.lastLoadedSwapStates`

DEX path requires a pre-fetched `swapDataModel` (populated by `loadDexSwapDataNoFee`). CEX passes `swapData = null`.

`FeeItem` → `FeeBucket` mapping lives at `SwapModel.FeeItem.toFeeBucket()` (line ~1921).

`getSelectedSwapFee()` (line ~1882) — reconstructs a `SwapFee` from `feeSelectorRepository.state.value as FeeSelectorUM.Content`.

### otherNativeFee (DEX bridge)

`ExpressTransactionModel.DEX.otherNativeFeeWei` — present only for `DEX_BRIDGE` providers. Converted from Wei in `DexSwapFeeCalculator.calculate()` and propagated as `DexFeeResult.otherNativeFee`. Carried through to `SwapFee.otherNativeFee`.

`applySwapFee` uses `fee.fee.amount.value + fee.otherNativeFee` as the balance check amount. `resolveOtherNativeFee()` in `SwapModel` reads it from `dataState.swapDataModel.transaction` since `FeeSelectorUM` does not carry it.

## Key Domain Models

- `SwapState` (sealed) — `QuotesLoadedState`, `Transfer`, `EmptyAmountState`, `SwapError`
  - `QuotesLoadedState` carries `preparedSwapConfigState: PreparedSwapConfigState` (balance checks, fee state, includeFeeInAmount), `permissionState`, `swapDataModel`, `currencyCheck`, `validationResult`, `minAdaValue`, `swapProvider`
- `SwapProvider` — `providerId`, `name`, `type: ExchangeProviderType` (DEX/CEX/DEX_BRIDGE), rates, slippage, TOS links
- `SwapPairLeast` — from/to `LeastTokenInfo` (contractAddress + networkId) + `providers: List<SwapProvider>`
- `SwapDataModel` — quote result with `transaction: ExpressTransactionModel` (sealed: `DEX`, `CEX`)
- `SwapAmount` — `value: BigDecimal` + `decimals: Int`
- `TokenSwapInfo` — `tokenAmount: SwapAmount`, `amountFiat: BigDecimal`, `swapCurrencyStatus: SwapCurrencyStatus`

File locations:
- `features/swap/domain/src/main/java/com/tangem/feature/swap/domain/models/ui/SwapState.kt`
- `features/swap/domain/src/main/java/com/tangem/feature/swap/domain/models/domain/SwapDataModel.kt`
- `features/swap/domain/src/main/java/com/tangem/feature/swap/domain/models/domain/SwapFeeState.kt`

## DI Modules

| Module | Scope | Purpose |
|--------|-------|---------|
| `SwapFeatureModule` | Singleton | `SwapComponent.Factory`, `SwapFeatureToggles` |
| `SwapModelModule` | ModelComponent | `SwapModel` into model map |
| `SwapEntryModule` | Singleton + Model | `SwapEntryComponent.Factory`, `SwapEntryModel` |
| `ChooseTokenModule` | Singleton + Model | `ChooseTokenComponent.Factory`, `ChooseTokenBridge.Factory`, `ChooseTokenModel` |
| `SwapSingletonModule` | Singleton | `AmountFormatter` |
| `SwapDomainModule` | Singleton | `DexSwapFeeCalculator`, `CexSwapFeeCalculator`, two `PatchEthGasLimitForSwap` instances with qualifiers |
| `SwapDomainBindModule` | Singleton | `SwapInteractor` → `SwapInteractorImpl`, `SwapTransferInteractor` → `SwapTransferInteractorImpl` |

## Analytics

`SwapEvents` sealed class hierarchy at `features/swap/impl/src/main/java/com/tangem/feature/swap/analytics/SwapEvents.kt`.

Fee tier analytics: `FeeBucket.toAnalyticsName()` → `"Min"/"Normal"/"Max"/"Suggested"/"Custom"`. Maps to `AnalyticsParam.FeeType.fromString(feeBucket.toAnalyticsName())`. The legacy `FeeType.getNameForAnalytics()` extension was removed in Phase 5 of the fee redesign.

## Navigation Summary

```
AppRouter (global)
  └─ AppRoute.Swap → DefaultSwapComponent
       ├─ childStack(SwapRoute)
       │    ├─ SwapRoute.Main → SwapMainChild (renders SwapScreen)
       │    ├─ SwapRoute.Success → SwapSuccessChild (renders SwapSuccessScreen)
       │    └─ SwapRoute.SelectToken → ChooseTokenComponent (FROM or TO bridge)
       ├─ SlotNavigation<Unit> (Approval)
       │    └─ GiveApprovalComponent (bottom sheet)
       └─ SlotNavigation<FeeSelectorConfig>
            └─ SwapFeeSelectorBlockComponent (inline fee block)
```

## UI Layer

- `SwapScreen` — main swap composable (send card, receive card, swap button, provider, notifications, fee)
- `SwapSuccessScreen` — post-swap success with transaction details
- `SwapScreenContent` — layout with `ConstraintLayout` for card positioning
- `TransactionCard` / `TransactionCardEmpty` — token cards with amount input
- Token cards pass `TokenSelectionDirection.FROM` / `.TO` to `onSelectTokenClick`

Files: `features/swap/impl/src/main/java/com/tangem/feature/swap/ui/`

## Testing

All domain-layer tests use JUnit 5 + MockK + Truth. Base class `SwapInteractorImplTestBase` wires all ~30 `SwapInteractorImpl` dependencies as relaxed mocks and exposes `sut: SwapInteractorImpl` via `lazy`. Tests extend it and stub only what they need.

Test files by topic:
- `SwapInteractorImplTestBase.kt` — base class; also contains `buildSwapCurrencyStatus(...)` and other builders
- `SwapInteractorImplLoadSwapFeeTest.kt` — unified `loadSwapFee` (all strategy branches: DEX-EVM, DEX-Solana, DEX bridge, CEX gasless-native, CEX gasless-token, CEX explicit-token, null swapData, zero amount)
- `SwapInteractorImplApplySwapFeeTest.kt` — `applySwapFee` balance/fee-state patching
- `SwapInteractorImplFindBestQuoteTest.kt` — provider dispatch, balance checks
- `SwapInteractorImplLoadDexSwapDataNoFeeTest.kt` — DEX quote-load without fee
- `fee/DexSwapFeeCalculatorTest.kt` — DEX calculator (Solana, EVM, gas fallback, bridge fee)
- `fee/CexSwapFeeCalculatorTest.kt` — CEX calculator (gasless, token, native)
- `fee/SwapFeeFactoryTest.kt` — `SwapFeeFactory` bucket selection
- `fee/PatchEthGasLimitForSwapTest.kt` — gas limit bump math
- `transfer/SwapTransferInteractorImplTest.kt` — transfer detection and state building
- `impl/StateBuilderInitialStateTest.kt`, `StateBuilderPairsTest.kt` — UI state construction

## Gotchas

**Fee state is transient on DEX.** `loadDexSwapDataNoFee` returns a `QuotesLoadedState` with `feeState = NotEnough()` and `isBalanceEnough = false`. The real values are only set after the fee selector resolves and calls `applySwapFee`. Do not check `preparedSwapConfigState.isBalanceEnough` before the fee selector has emitted a `FeeSelectorUM.Content` state.

**`SwapFee` is not carried in `SwapProcessDataState`.** It is reconstructed from `feeSelectorRepository.state.value` via `getSelectedSwapFee()` at each call site (swap execution, analytics). `otherNativeFee` must be re-read from `dataState.swapDataModel.transaction` because `FeeSelectorUM` does not carry it.

**DEX requires pre-fetched `swapDataModel`.** `FeeSelectorRepository.loadFeeExtended` returns `Left(UnknownError)` when `dataState.swapDataModel == null`. This is by design: `manageDex` only calls `loadDexSwapDataNoFee` (which populates `swapDataModel`) when allowance is OK and balance is sufficient. If the user has insufficient balance or a pending approval, the fee selector will not load.

**`PatchEthGasLimitForSwap` has two instances with different percentages.** DEX uses 12%, CEX uses 5%. They are distinguished by `@SwapDexGasLimit` and `@SwapSendGasLimit` qualifiers. Passing the wrong qualifier to a calculator is a silent bug with no compile-time check.

**`Fee.Ethereum.TokenCurrency` throws.** `PatchEthGasLimitForSwap.increaseEthGasLimitInNeeded` calls `error("handle in [REDACTED_TASK_KEY]")` for `TokenCurrency`. This path must not be reached in production. The issue is tracked but not yet resolved.

**Solana DEX fee is not patched.** Unlike EVM, `DexSwapFeeCalculator` skips `patchEthGasLimitForSwap` for Solana paths. Also: if the compiled transaction exceeds `SOLANA_TRANSACTION_SIZE_THRESHOLD_BYTES` and the wallet is `UserWallet.Cold`, the calculator raises `ExpressDataError.TooLargeSolanaTransactionError`.

**`TransactionFeeResult` sealed class is not a data class.** `Loaded(val fee: TransactionFee)` and `LoadedExtended(val fee: TransactionFeeExtended)` use regular `class`, so structural equality does not hold. Use `is`-checks and field comparison in tests.

**`SwapInteractor` interface vs `SwapInteractorImpl`.** The interface exposes `loadSwapFee` and `applySwapFee` (the new unified API). The old `loadFeeForSwapTransaction` overloads (two overloads) and `loadFeeForDex` private method have been fully removed. Do not reference them in new code or tests.

**Transfer mode vs swap mode.** `SwapTransferInteractor.shouldTransferInsteadOfSwap` detects same-wallet same-currency pairs and returns `true`, causing the UI to show `SwapState.Transfer` instead of `SwapState.QuotesLoadedState`. No fee selector is shown in transfer mode.