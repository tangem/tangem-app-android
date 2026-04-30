# Swap Feature

Token-to-token exchange feature. Users select FROM and TO tokens, get quotes from providers (DEX/CEX), approve ERC-20 allowances if needed, and execute swaps.

## Module Structure

```
features/swap/
  api/          — Public contracts (SwapComponent, SwapEntryComponent, SwapFeatureToggles)
  impl/         — UI, model, navigation, DI, token selection subfeature
  domain/       — Business logic (SwapInteractor) + domain models
    api/        — Domain interfaces
    models/     — Domain model types (SwapPair, SwapProvider, SwapState, etc.)
  data/         — Repository implementations, Retrofit APIs, Moshi DTOs
```

**Package naming:** API = `com.tangem.features.swap`, Impl = `com.tangem.feature.swap` (singular `feature`, legacy inconsistency).

## Key Components

### SwapComponent (API)
Entry point. `Params` requires `currencyFrom`, `userWalletId`, `screenSource`. Optional: `currencyTo`, `isInitialReverseOrder`, `tangemPayInput`, `preselectedToToken`, `preselectedAccount`.

### SwapEntryComponent (API)
Gateway component with sealed `Params`: `Story`, `Empty`, `Selected`, `Payment`. Routes to stories or directly to swap based on input type. See `entry/SwapEntryRoute.kt` for route definitions.

### DefaultSwapComponent (impl)
Decompose component. Creates `SwapModel` via `getOrCreateModel(params)`.

**Child navigation:**
- `childStack(SwapRoute)` for screen navigation — `SwapRoute.Main`, `SwapRoute.Success`, `SwapRoute.SelectToken(isFromDirection)` rendered via `Children` composable with fade animation
- `SlotNavigation<Unit>` for approval bottom sheet (`GiveApprovalComponent`)
- `SlotNavigation<FeeSelectorConfig>` for fee selector block

**Injected factories:** `SwapFeeSelectorBlockComponent.Factory`, `GiveApprovalComponent.Factory`, `ChooseTokenComponent.Factory`.

### SwapModel (impl)
`@ModelScoped`, extends `Model()`. The central coordinator — ~1500 lines.

**Key state:**
- `dataStateStateFlow: MutableStateFlow<SwapProcessDataState>` — reactive domain data (from/to tokens, pairs, providers, amounts, fees)
- `uiState: SwapStateHolder by mutableStateOf()` — Compose UI state built by `StateBuilder`
- `feeSelectorRepository: FeeSelectorRepository` — fee state management
- `stackNavigation: StackNavigation<SwapRoute>` — stack navigation exposed from `SwapRouter`
- `approvalSlotNavigation: SlotNavigation<Unit>` — approval bottom sheet

**Navigation:**
- `SwapRouter` wraps `AppRouter` + `StackNavigation<SwapRoute>` for screen switching and back navigation
- `swapRouter.openScreen(SwapRoute.SelectToken(isFromDirection))` to push token selection
- `swapRouter.openScreen(SwapRoute.Success)` replaces current with success screen
- `swapRouter.back()` — pops local stack or exits swap via AppRouter

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

### StateBuilder (impl)
Pure transformation class. Takes `UiActions` + providers, builds `SwapStateHolder` from `SwapProcessDataState`.

Key methods: `createInitialLoadingState`, `createQuotesLoadedState`, `createSuccessState`, `loadingPermissionState`, `updateSwapAmount`, `addNotification`, `dismissBottomSheet`.

### SwapRouter (impl)
Wraps `AppRouter` + `StackNavigation<SwapRoute>`. Handles `openScreen(SwapRoute)` to push/replace stack entries and `back()` with special logic: SelectToken pops local stack, Success exits to screen before SwapCrypto in app stack, Main pops AppRouter. `openTokenDetails()` navigates to `AppRoute.CurrencyDetails`.

## Token Selection Subfeature (impl)

Self-contained within `choosetoken/` package:
- `ChooseTokenComponent` — API with `Params(bridge, settings, analyticsPayload)`
- `ChooseTokenBridge` — Channel-based communication: `onCurrencyChosen`, `onClose`, `onTokenSelected` (legacy), `onNewTokenAdded` (legacy). Has `settingsStateFlow` for dynamic settings.
- `ChooseTokenComponent.Settings` — `SwapFrom` (no market block) vs `SwapTo` (with market block)
- `ChooseTokenResult` — Contains `CryptoCurrencyStatus`, `AccountStatus`, `UserWallet`
- `DefaultChooseTokenComponent` — Has its own `ChooseTokenModel` and optional `AddToPortfolioComponent` bottom sheet slot

## Domain Layer

### SwapInteractor
Central domain interface. Methods:
- `getPair(from, to, filterProviderTypes)` → `Either<ExpressError, List<SwapPairLeast>>`
- `findBestQuote(from, to, providers, amount, ...)` → `Map<SwapProvider, SwapState>`
- `onSwap(from, to, provider, swapData, amount, fee, ...)` → `SwapTransactionState`
- `loadFeeForSwapTransaction(...)` → `Either<GetFeeError, TransactionFee/TransactionFeeExtended>`
- `getInitialCurrencyToSwap(accountStatusList, fromUserWallet, isReverse)` → `AccountCryptoCurrencyStatus?`
- `getTokenBalance(token)` → `SwapAmount`

### Key Domain Models
- `SwapPairLeast` — from/to token info + providers list
- `SwapProvider` — providerId, name, type (DEX/CEX/DEX_BRIDGE), rates, slippage, TOS links
- `SwapState` — sealed: `QuotesLoadedState`, `SwapError`, `EmptyAmountState`
- `SwapCurrencyStatus` — wraps `CryptoCurrencyStatus` + `UserWallet` + `Account`
- `SwapAmount` — value + decimals pair
- `SwapDataModel` — quote result with transaction data

## DI Modules

| Module | Scope | Bindings |
|--------|-------|----------|
| `SwapFeatureModule` | Singleton | `SwapComponent.Factory`, `SwapFeatureToggles` |
| `SwapModelModule` | ModelComponent | `SwapModel` into model map |
| `SwapEntryModule` | Singleton + Model | `SwapEntryComponent.Factory`, `SwapEntryModel` |
| `ChooseTokenModule` | Singleton + Model | `ChooseTokenComponent.Factory`, `ChooseTokenBridge.Factory`, `ChooseTokenModel` |
| `SwapSingletonModule` | Singleton | `AmountFormatter` |

## UI Layer

- `SwapScreen` — main swap composable (send card, receive card, swap button, provider, notifications, fee)
- `SwapSuccessScreen` — post-swap success with transaction details
- `SwapScreenContent` — layout with `ConstraintLayout` for card positioning
- `TransactionCard` / `TransactionCardEmpty` — token cards with amount input
- Token cards pass `TokenSelectionDirection.FROM` / `.TO` to `onSelectTokenClick`

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

## Build Commands

```bash
./gradlew :features:swap:impl:compileDebugKotlin
./gradlew :features:swap:api:compileDebugKotlin
./gradlew :features:swap:domain:compileDebugKotlin
./gradlew :features:swap:impl:detekt
```