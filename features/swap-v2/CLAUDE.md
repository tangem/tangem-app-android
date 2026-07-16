# Swap V2 / Send-with-Swap Feature

This module implements **Send-with-Swap (SvS)**: a send transaction where the sent token is swapped
(CEX) to a different *receive* token at a *destination address* in one flow. The user picks a receive
token, enters amounts (with Fixed/Float rate), enters a destination address (+ memo for memo-networks),
reviews on Confirm, and sends.

> There is **no standalone token↔token swap UI** in this module — that lives in `features/swap/`
> (see `features/swap/CLAUDE.md`). swap-v2 is the redesigned **send-with-swap** flow plus its shared
> amount/provider/notifications subscreens, built on the **send-v2** subcomponents.

## Module Structure

```
features/swap-v2/
  api/   — com.tangem.features.swap.v2.api
           SendWithSwapComponent (+ Params/Factory), SwapFeatureToggles,
           SwapAmountUpdateTrigger, subcomponents/, choosetoken/
  impl/  — com.tangem.features.swap.v2.impl   (android-library + Hilt/kapt)
           sendviaswap/   — SvS flow root, model, routes, confirm/, success/, analytics/
           amount/        — swap amount screen (model, transformers, converters, entity, ui)
           chooseprovider/— provider selector bottom sheet
           choosetoken/   — receive-token / network selection
           notifications/ — swap-specific notifications (price impact, express errors)
           common/        — ConfirmData, SwapAlertFactory, SwapUtils, entities (ConfirmUM, SwapQuoteUM)
           di/            — Hilt modules
```

**Package naming:** API = `com.tangem.features.swap.v2.api`, Impl = `com.tangem.features.swap.v2.impl`.
Consistent `.v2` segment (unlike the legacy `features/swap` which uses `feature.swap` for impl).

**Build commands:**
```bash
./gradlew :features:swap-v2:impl:compileDebugKotlin
./gradlew :features:swap-v2:api:compileDebugKotlin
./gradlew :features:swap-v2:impl:testDebugUnitTest
./gradlew :features:swap-v2:impl:detekt
```

## The SvS Flow (sendviaswap/)

### Entry: SendWithSwapComponent (api) / DefaultSendWithSwapComponent (impl)
- `SendWithSwapComponent.Params`: `userWalletId`, `currency` (the **FROM** token), `callback`.
- `DefaultSendWithSwapComponent` (`impl/.../sendviaswap/DefaultSendWithSwapComponent.kt`) owns an inner
  `StackNavigation<SendWithSwapRoute>` + `InnerRouter`, creates `SendWithSwapModel` via
  `getOrCreateModel`, and a `childStack` rendering Amount/Destination/Confirm/Success.

### Routes: SendWithSwapRoute
`impl/.../sendviaswap/SendWithSwapRoute.kt` — sealed `Route`, every entry has `isEditMode: Boolean`:
- `Amount(isEditMode)` — implements `SwapAmountRoute`
- `Destination(isEditMode)` — implements send-v2 `DestinationRoute`
- `Confirm` (object, `isEditMode = false`)
- `Success` (object, `isEditMode = false`)

`isEditMode` distinguishes the **linear** forward flow (`Amount → Destination → Confirm`) from
**re-editing** a step *from Confirm* (`showEditAmount`/`showEditDestination` push the step with
`isEditMode = true`; `onNextClick` then **pops** back to Confirm instead of advancing).

### Parent model: SendWithSwapModel
`impl/.../sendviaswap/model/SendWithSwapModel.kt`. `@ModelScoped`. Implements three child callbacks
(`SwapAmountComponent.ModelCallback`, `SendDestinationComponent.ModelCallback`,
`SendWithSwapConfirmComponent.ModelCallback`). Holds the **aggregate** state:
- `uiState: StateFlow<SendWithSwapUM>` — `{ amountUM, destinationUM, feeSelectorUM, confirmUM, navigationUM }`
- `currentRoute: MutableStateFlow<SendWithSwapRoute>`
- `primaryCryptoCurrencyStatusFlow`, `primaryFeePaidCurrencyStatusFlow`, `accountFlow`,
  `isAccountModeFlow`, `isBalanceHiddenFlow` — read-only sources passed down to children as params.

Child→parent merge callbacks:
- `onAmountResult(amountUM)` → `uiState.copy(amountUM = …)`
- `onDestinationResult(destinationUM)` → `uiState.copy(destinationUM = …)`
- `onResult(route, sendWithSwapUM)` → **`if (currentRoute.value == route) uiState.value = …`** (full replace,
  route-guarded; used by Confirm to publish its full state back up)
- `onNavigationResult(navigationUM)` → drives the shared footer button/app-bar.

### childStack subscription = the state-sync mechanism (READ THIS)
`DefaultSendWithSwapComponent.init { childStack.subscribe(CREATE_DESTROY) { stack → componentScope.launch { … } } }`:
on every active-child change it **pushes the parent's current snapshot into the newly-active child** and
then emits the new route:
```kotlin
when (active) {
  is SwapAmountComponent       -> active.updateState(uiState.value.amountUM)
  is SendDestinationComponent  -> active.updateState(uiState.value.destinationUM)   // screen
  is SendWithSwapConfirmComponent ->
      if (model.currentRoute.value.isEditMode) active.updateState(uiState.value)    // ← gated!
}
model.currentRoute.emit(stack.active.configuration)   // emitted AFTER the isEditMode read
```
The `isEditMode` check intentionally reads the **previous** route (the emit happens afterwards) so it is
true exactly when returning to a *reused* Confirm from an edit step. In the linear flow Confirm is
re-created fresh from `params.sendWithSwapUM`, so no re-push is needed.

### Confirm: SendWithSwapConfirmComponent / SendWithSwapConfirmModel
`impl/.../sendviaswap/confirm/`. The Confirm screen embeds **read-only blocks** reused from send-v2:
- `SwapAmountBlockComponent` (swap-v2)
- `SendDestinationBlockComponent` (send-v2) — shows address + memo, click → `showEditDestination`
- `FeeSelectorBlockComponent` (send-v2)
- `SendNotificationsComponent` (send-v2) + `SwapNotificationsComponent` (swap-v2)

`SendWithSwapConfirmModel`:
- `uiState: StateFlow<SendWithSwapUM>` seeded from `params.sendWithSwapUM`.
- `confirmData: ConfirmData` (computed) — extracts `enteredFromAmount/enteredToAmount`,
  `enteredDestination`, `enteredMemo`, `fee`, statuses, quote, rateType, amountType, priceImpact from
  `uiState`; this is what the transaction + notifications are built from.
- `onFeeResult/onAmountResult/onDestinationResult` — block callbacks copy into `uiState`.
- `updateState(sendWithSwapUM)` — full replace (used by the edit-mode re-push).
- `configConfirmNavigation` — `combine(uiState, currentRoute).filter { route is Confirm }` →
  `callback.onResult(Confirm, state.copy(navigationUM = …))` (publishes confirm state up to the parent).
- Sending: `SwapTransactionSender` (CEX only; DEX/DEX_BRIDGE/ONRAMP rejected). Success →
  `SendWithSwapConfirmSentStateTransformer` + `router.replaceAll(Success)`.

### Success: SendWithSwapSuccessComponent
`impl/.../sendviaswap/success/` — renders `ConfirmUM.Success` (tx date, explorer url, provider, swap data).

## Amount screen (amount/)

- `SwapAmountComponent` / `SwapAmountModel` (`amount/model/SwapAmountModel.kt`, ~big orchestrator).
- State `SwapAmountUM` (`amount/entity/SwapAmountUM.kt`): `Empty(swapDirection)` | `Content` with
  `primaryAmount`/`secondaryAmount` fields, `primary/secondaryCryptoCurrencyStatus`,
  `swapRateType: ExpressRateType` (Fixed|Float), `swapQuotes`, `selectedQuote: SwapQuoteUM`, `priceImpact`.
- Quotes are loaded periodically via a task scheduler and through `GetSwapQuoteUseCase`.
- Transformers (`amount/model/transformers/`): `SwapAmountValueChangeTransformer`,
  `SwapAmountSelectQuoteTransformer`, `SwapAmountSetQuotesTransformer`,
  `SwapAmountChangeAmountTypeTransformer`, `SwapAmount{Reduce*,Max,Paste,…}Transformer`, applied via
  `uiState.transformerUpdate(…)`.
- **Fixed vs Float:** `SwapAmountType.To` must use `ExpressRateType.Fixed` (the float API can't target a
  to-amount); `SwapAmountType.From` uses `Float`. Provider filtering checks
  `provider.rateTypes.contains(rateType)` before requesting a quote.

## Choose provider / token, Notifications

- `chooseprovider/` — `SwapChooseProviderComponent`/`Model`, bottom-sheet provider list (converters
  `SwapProviderListItemConverter`, `SwapProviderStateConverter`).
- `choosetoken/` — receive-token + network selection (`SwapChooseTokenNetworkModel`, transformers).
- `notifications/` — `SwapNotificationsComponent`/`Model`, driven by `SwapNotificationsUpdateTrigger`/
  `…Listener`; produces price-impact / express-error / destination-tag-required notifications.

## Reused send-v2 subcomponents (API boundary)

SvS consumes these `features/send-v2/api` contracts (impl injected via DI):
- `SendDestinationComponent.Factory` — the navigable **address/memo screen**.
- `SendDestinationBlockComponent.Factory` — the **read-only block** on Confirm.
- `FeeSelectorBlockComponent.Factory` + `FeeSelectorReloadTrigger`.
- `SendNotificationsComponent.Factory` + `SendNotificationsUpdateTrigger`/`…Listener`.
- Entities: `DestinationUM`, `FeeSelectorUM`, `NavigationUM`, `PredefinedValues`.

The shared destination model is **`features/send-v2/.../subcomponents/destination/model/SendDestinationModel.kt`**.
Its `updateState(destinationUM)` does `if (Content && isInitialized) _uiState.value = destinationUM`
(StateFlow dedups equal values). `saveResult()` (push to the parent callback) runs on Next, on
auto-next, and **on back only when `!route.isEditMode`**.

## DI modules (di/ and per-subpackage di/)

| Module | Scope | Provides |
|---|---|---|
| `SwapFeatureModules` | Singleton | `SwapFeatureToggles` |
| `SendWithSwapModule` | Singleton + Model | `SendWithSwapComponent.Factory`, `SendWithSwapModel` |
| `SwapAmountModule` | Singleton + Model | `SwapAmountModel`, `SwapAmountUpdateTrigger/Listener`, `SwapAmountReduceTrigger/Listener` |
| `SendWithSwapConfirmModule` | Model | `SendWithSwapConfirmModel` |
| `SwapChooseProviderModule` | Model | `SwapChooseProviderModel` |
| `SwapChooseTokenModule` | Singleton + Model | choose-token factories/model |
| `SwapNotificationsModule` | Singleton + Model | `SwapNotificationsModel`, `SwapNotificationsUpdateTrigger/Listener` |

## Analytics

- `SendWithSwapAnalyticEvents` (`sendviaswap/analytics/`) — `ConfirmationScreenOpened`,
  `AmountScreenOpened`, `TransactionScreenOpened`, `OnSendClick`, `NoticeFixedRate/FloatRate`,
  `Error{InsufficientBalance,MinAmount,MaxAmount,ExpressQuote}`, `HighPriceImpact`, `TradeTooLarge`;
  category = `CommonSendAnalyticEvents.SEND_CATEGORY`. `ExpressRateType.toAnalyticsRateType()` maps rate.
- `SwapAmountAnalyticEvents` + `SwapAmountAnalyticsSender` (`amount/analytics/`) — provider selector events.

## State-management patterns & gotchas

- **Transformer pattern:** `uiState.transformerUpdate(SomeTransformer(...))`; transformers early-return
  `prevState` if not the expected subtype (`as? Content ?: return prevState`).
- **Three+ StateFlows hold the destination at once.** The memo/address lives in: the navigable
  Destination **screen** model (#A), the parent `SendWithSwapModel.uiState.destinationUM` (#B), the
  `SendWithSwapConfirmModel.uiState.destinationUM` (#C), and the Confirm-embedded destination **block**
  model (#D, what Confirm actually displays). They are synced by **snapshot copies** (`updateState`,
  `onResult`, `onDestinationResult`) over `StateFlow.value =` (which **dedups by `equals`**), plus the
  block's self-feeding `init { uiState.onEach { onResult(it) } }`. This is fragile — see [REDACTED_TASK_KEY]
  ("floating memo": an edit on #A intermittently fails to reach #D). Prefer a single source of truth
  when touching this area; do **not** assume an `updateState` re-push actually emits (equal value = no-op).
- **Edit-mode back does not persist.** Leaving an edit step via the back arrow / system back skips
  `saveResult()` (`SendDestinationModel.configDestinationNavigation`, `if (!route.isEditMode)`), so the
  parent keeps the pre-edit value. The footer "Continue"/"Next" button always persists. This is shared
  by regular Send + NFT Send + SvS.
- **`onResult` is route-guarded.** `SendWithSwapModel.onResult` only applies when
  `currentRoute.value == route`, which protects against late/stale Confirm emissions overwriting the
  parent after navigating away. Keep that guard if you refactor.
- **`currentRoute.emit` runs at the END of the subscribe coroutine**, so the `isEditMode` re-push gate
  reads the *previous* route. Relies on `componentScope` launches being serialized (main dispatcher).
- **CEX-only.** `SwapTransactionSender` rejects DEX/DEX_BRIDGE/ONRAMP. Destination address for CEX is
  only known after exchange-data, so confirm notifications pass `destinationAddress = null` for the
  send-notifications path.

## Testing

JUnit 5 + MockK + Turbine + Truth (see project `.claude/rules/unit-testing.md`). Feature-model tests
build the heavy graph with relaxed mocks and a single `StandardTestDispatcher`; drive with
`advanceUntilIdle()` and `model.onDestroy()`. For SvS state-sync regressions, prefer parent-model
(`SendWithSwapModel`) tests asserting that an edit propagated through `onDestinationResult` is the value
that `uiState.destinationUM` ends up holding across an edit→confirm round trip.