# Collectibles Feature

Gacha packs from Collector Crypt. Users land on an onboarding screen, optionally watch a stories
explainer, then browse a paginated catalogue of packs ("gacha machines") filtered by category and
search. Buying a pack, the collection screen and the activity log are not built yet — the feature
currently ships the catalogue plus the account chrome, whose balance is live and whose collection widget is
still a stub.

> This is **not** `features/nft` / `domain/nft`, which is the unrelated NFT-collection feature.
> Collectibles owns its own vertical slice: `domain/collectibles`, `data/collectibles`, and the
> `collectibles` endpoints in `grow/datasource`.

**Baseline:** `develop` @ `cbd966b3cd`, 2026-09-02. The `discovery/` package described below was
uncommitted working-tree state when this map was written — it was extracted out of `main/` by the
refactor riding on the [REDACTED_TASK_KEY] branch.

Gated by `FeatureToggles.TWI_1640_COLLECTIBLES_PHASE_ONE_ENABLED`. The toggle is read only by
`DefaultCollectiblesFeatureToggles`; the Feed entry tile consumes it via `CollectiblesFeatureToggles`.

## Module map

```
features/collectibles/
  api/    — Two declarations only: CollectiblesEntryComponent, CollectiblesFeatureToggles
  impl/
    common/ui/    — FoldingBlock (collapses leading blocks when search activates)
    di/           — Component, Model and FeatureToggles Hilt modules
    onboarding/   — First screen; marketing copy, FAQ, "Create Gacha account"
    stories/      — Slide-based explainer, reachable again from the main menu
    main/         — Account chrome: balance, actions, collection widget, context menu, the LazyColumn
    discovery/    — The packs catalogue: tabs, search, paginated pack rows. The only domain-backed part
```

**Package convention:** both sides use the **plural** `com.tangem.features.collectibles.*` — api is
`…collectibles.api`, impl is `…collectibles.impl`. (Unlike swap, there is no singular-`feature` legacy
quirk here.) Impl has no `res/`, no `AndroidManifest.xml` and no `androidTest/`; tests live under
`src/test/kotlin/`.

**Build / test:**
```bash
./gradlew :features:collectibles:impl:compileDebugKotlin
./gradlew :features:collectibles:impl:testDebugUnitTest
./gradlew :features:collectibles:impl:detekt
```

## Where to start reading

| Symbol | Role | Path |
|---|---|---|
| `CollectiblesEntryComponent` | The whole public API; `Params` carry the wallet and which screen to start on | `api/.../collectibles/api/CollectiblesEntryComponent.kt` |
| `DefaultCollectiblesEntryComponent` | Owns the inner `childStack` + `InnerRouter`; the only public impl | `impl/.../impl/DefaultCollectiblesEntryComponent.kt` |
| `FeedTopBlocksModel` | Outside this feature: settles the wallet and the start screen before pushing the route | `features/feed/impl/.../feed/model/v2/FeedTopBlocksModel.kt` |
| `CollectiblesMainComponent` | Hosts the screen and embeds discovery as a plain `child(key = "discovery")` | `impl/.../impl/main/CollectiblesMainComponent.kt` |
| `CollectiblesDiscoveryModel` | Catalogue orchestration: categories, search debounce, pagination actions | `impl/.../impl/discovery/model/CollectiblesDiscoveryModel.kt` |
| `CollectiblesDiscoveryContentTransformer` | Pure `PaginationStatus` → `ContentUM` mapping; de-dup + row chunking | `impl/.../discovery/model/transformer/…Transformer.kt` |
| `CollectiblesDiscoveryUM` | Tabs, `ContentUM` (Loading/Content/Error/Empty), hoisted `TangemSearch.State` | `impl/.../discovery/ui/state/CollectiblesDiscoveryUM.kt` |
| `CollectiblesMainList` | The single `LazyColumn`; header + discovery's items + filler | `impl/.../main/ui/components/CollectiblesMainList.kt` |
| `CollectiblesMainUM` | Screen chrome only — balance, actions, collection, menu | `impl/.../main/ui/state/CollectiblesMainUM.kt` |

## Navigation

```
AppRoute.Collectibles(userWalletId, startRoute) → DefaultCollectiblesEntryComponent  (childStack "collectiblesStack")
  ├─ CollectiblesRoute.Onboarding(userWalletId) → CollectiblesOnboardingComponent → CollectiblesOnboardingModel
  │     onCreateAccountClick → router.replaceAll(Main(id), Stories)   [Stories pushed on top of Main]
  ├─ CollectiblesRoute.Stories                  → CollectiblesStoriesComponent    → CollectiblesStoriesModel
  │     last-slide continue / close → router.pop() → lands on Main
  └─ CollectiblesRoute.Main(userWalletId)       → CollectiblesMainComponent       → CollectiblesMainModel
        menu "How it works" → router.push(Stories)
        embedded child (NOT a stack or slot entry): child(key = "discovery")
            → CollectiblesDiscoveryComponent → CollectiblesDiscoveryModel
```

**The feature decides nothing about where it opens** — the caller does, and the route says so:
`AppRoute.Collectibles(userWalletId, startRoute)` with `CollectiblesStartRoute` (`Onboarding` / `Main`, in
`common/routing/.../entity/CollectiblesStartRoute.kt`). `DefaultCollectiblesEntryComponent` maps it to the
stack's `initialConfiguration` in one `when`; there are no slots, no gate, no loader route.

**The wallet and the start screen are settled outside, in the feed.** `FeedTopBlocksModel`
(`features/feed/impl`, the Shtorka 2.0 home screen) takes every unlocked wallet on a block tap: one → picked
silently, several → the `PortfolioSelectorComponent` sheet in `FeedTopBlocksComponent`'s `childSlot`, none →
nothing happens. Then `SingleAccountListSupplier` decides the start screen: an `Account.Collectibles` in the
wallet's list means the vault exists, so `Main`; its absence — or an unreadable list — means `Onboarding`.

Why there and not here: `AppRoute.Collectibles` is a full-screen entry of the root stack, and `Children`
draws only the active child, so a modal sheet raised inside this feature scrims an empty screen. Until
`Account.Collectibles` is added to `DefaultSingleAccountListProducer` (next task, together with the wallet
block), the list never holds it and every entry lands on `Onboarding` — harmless, because
`CreateOrReactivateCollectiblesAccountUseCase` returns early for an already-onboarded wallet.

Reached from one place today: `FeedTopBlocksComponent`; the wallet-screen account block will push the same
route with `startRoute = Main`. Tile visibility comes from `CollectiblesFeatureToggles.isCollectiblesEnabled`
threaded through `DefaultFeedV2Component` → `FeedHomeComponent(isCollectiblesVisible = …)`.

Inner navigation is an `InnerRouter<CollectiblesRoute>` — routes of that type go to the inner stack,
everything else falls through to the app `AppRouter`. `onChildBack()` reads
`if (stack.value.backStack.isEmpty()) router.pop() else stackNavigation.pop()`, so back from the first
inner screen leaves the feature entirely. There are no `childSlot`s and no bottom sheets.

**Discovery is not a screen.** `CollectiblesDiscoveryComponent` implements
`ComposableListContentComponent<CollectiblesDiscoveryUM>` (`fun LazyListScope.content(uiState, modifier)`),
so it contributes items into `CollectiblesMainList`'s `LazyColumn` rather than rendering its own. It also
exposes `tabScrollState` (shared with the host's pinned overlay) and `onLoadMore()`.

## Discovery: pagination, filtering, search

Pagination uses `core/pagination`'s `BatchFlow` — **not** the `FlowProducer`/supplier pattern used
elsewhere in the app. The model owns a `MutableSharedFlow<CollectibleGachaMachinesBatchAction>` and hands
it to the use case inside a `CollectibleGachaMachinesBatchingContext`; the resulting
`machinesBatchFlow.state` is the single source of catalogue state.

```
CollectiblesDiscoveryModel
  actionsFlow (replay = 1) ──► GetCollectibleGachaMachinesBatchFlowUseCase(context) ──► BatchFlow.state
        ▲                                                                                    │
        │ BatchAction.Reload(categoryId, search)  — category tap, debounced query, retry      │
        │ BatchAction.LoadMore()                  — onLoadMore() while Paginating             ▼
        └──────────────────────────────────  CollectiblesDiscoveryContentTransformer ──► ContentUM
```

Category and search are **server-side** filters carried in `CollectibleGachaMachinesListConfig`; each
change reloads from page one. The query is debounced 500 ms, `trim()`med, and blank collapses to `null`.

`PaginationStatus` → `ContentUM`, as the transformer maps it:

| Status | Result |
|---|---|
| `None`, `InitialLoading` | `Loading` — unless the previous state was `Content`, then `Content(isReloading = true)` |
| `InitialLoadingError` | `Error(message, onReloadClick)` |
| `NextBatchLoading` | `Content(isLoadingNextPage = true)` |
| `Paginating`, `EndOfPagination` | `Content(isLoadingNextPage = false)` |

Any status with no machines after flattening yields `Empty`. Rows are `chunked(2)` into `PackRowUM`s keyed
by their first pack's id.

The backend pages with a body-level keyset `cursor`, which is why `data/collectibles` carries a custom
`CollectibleGachaMachinesBatchFetcher` instead of the stock `CursorBatchFetcher` (the latter derives the
cursor from the last item) and signals exhaustion with `BatchFetchResult.Error(EndOfPaginationException())`.

## Domain & data

```
domain/collectibles/models/  — CollectibleGachaMachine (+ Odds/Stock/TierRanges), MachineCategory,
                               CollectibleGachaMachinesPage/Batch/ListConfig, CollectiblesError
domain/collectibles/         — CollectiblesRepository, 5 use cases, the Batch* typealiases
data/collectibles/           — DefaultCollectiblesRepository, CollectibleGachaMachinesBatchFetcher, converters
grow/datasource/collectibles — CollectiblesApi (Retrofit), CollectiblesAuthProvider, request/response DTOs
```

Use cases return `Either<CollectiblesError, T>` via `runSuspendCatching`, except
`GetCollectibleGachaMachinesBatchFlowUseCase`, which is non-suspend and returns the flow directly. Only
**two of the five** are wired into the UI today — `GetCollectibleGachaMachineCategoriesUseCase` and
`GetCollectibleGachaMachinesBatchFlowUseCase`, both injected by `CollectiblesDiscoveryModel`.
`GetCollectibleGachaMachineUseCase`, `GenerateCollectibleTxUseCase` and `SubmitCollectibleTxUseCase` exist
and are tested but have no production call site yet.

**Request headers ([REDACTED_TASK_KEY]).** `session-id` is a config-level header on *every* collectibles endpoint —
`Collectibles` (`grow/datasource/config/`) puts `ProviderSuspend(collectiblesAuthProvider::getSessionId)`
into its header map, which the pre-existing `SwitchEnvironmentInterceptor` applies. The id is one UUID per
process (`DefaultCollectiblesAuthProvider` is a `@Singleton`). `user-id` is different: a per-method
Retrofit `@Header` on `generateTx`/`submitTx` only, carrying `UserWalletId.stringValue` plumbed down from
the use cases. Adding a header to the config requires updating `CollectiblesConfigTest`, which asserts the
exact header set per environment.

## DI modules

| Module | Scope | Provides |
|---|---|---|
| `CollectiblesComponentModule` | `SingletonComponent` | `CollectiblesEntryComponent.Factory`, `CollectiblesDiscoveryComponent.Factory` |
| `CollectiblesModelModule` | **`ModelComponent`** | All four models `@IntoMap @ClassKey` into the model map read by `getOrCreateModel()` |
| `CollectiblesFeatureTogglesModule` | `SingletonComponent` | `CollectiblesFeatureToggles` |
| `CollectiblesDomainModule` (in `app/`) | `SingletonComponent` | The two use cases discovery injects |
| `CollectiblesDataModule` (in `data/collectibles/`) | `SingletonComponent` | `CollectiblesRepository` |

The onboarding, stories and main components are **not** bound in `CollectiblesComponentModule` — their
nested `@AssistedFactory interface Factory` types are generated by Hilt and injected directly. Only the
entry and discovery components have an interface/impl split needing an explicit `@Binds`.

Domain use cases are plain classes with no `@Inject` constructor, so a missing `@Provides` in
`CollectiblesDomainModule` is how a newly-used use case fails — at Hilt compile time, in `app/`, far from
the feature.

## Testing

JUnit 5 + MockK + Truth + Turbine via `testImplementation(projects.test.core)`, already declared — new
model/converter tests need no `build.gradle.kts` change. See `.claude/rules/unit-testing.md` for the
project-wide conventions.

Models are constructed **directly** — `Model` is a plain abstract class whose only abstract member is
`dispatchers`, so there is no Decompose runtime, no Hilt and no `AppComponentContext` in a model test.
Each test file carries its own `private fun TestScope.createTestingCoroutineDispatcherProvider()` building
one `StandardTestDispatcher(testScheduler)` for all five roles (there is no shared helper in this repo),
and calls `model.onDestroy()` to cancel `modelScope`.

Reference tests in this feature: `CollectiblesDiscoveryContentTransformerTest` (parameterized status
table), `CollectiblesDiscoveryModelTest` (fake `BatchFlow` + captured `BatchingContext`), and
`discovery/MachineFactory.kt` for the all-arguments-defaulted fixtures.

## Gotchas

**The main screen's balance is real, the collection size is not.** The balance comes from
`CollectiblesAccountStatusSupplier` + `GetSelectedAppCurrencyUseCase`, mapped by `CollectiblesBalanceConverter`;
an unread vault shows a dash rather than a zero. The collection widget is still hardcoded (`TODO([REDACTED_TASK_KEY])`).
The vault's USDC is always zero until the blockchain SDK can read it, so today the fiat figure is the SOL on the
vault owner key.

**`actionsFlow` needs `replay = 1`.** The first `Reload` is emitted while pagination is still subscribing;
a shared flow without a replay cache silently drops what it cannot deliver yet, and the catalogue never
loads.

**Category failure degrades, it does not fail.** `reloadMachines()` sits *outside* the `onRight` block in
`loadCategories()`, so when categories fail the tabs stay empty and the catalogue still loads unfiltered.
Do not "fix" this by moving the call inside the success branch.

**The transformer must `distinctBy(id)`.** The keyset cursor pages over a shifting order, so a machine that
moved between two page requests arrives in two batches. Duplicate lazy-list keys crash the list.

**`None`/`InitialLoading` over existing `Content` keeps the rows.** It sets `isReloading = true` instead of
emptying the list, because Compose discards a `LazyListState`'s scroll position outright once the first
visible index no longer exists.

**Search activation swaps to a second `LazyListState`.** `createSearchEntry` seeds a fresh list state from
the main one's `firstVisibleItemIndex/Offset`; the hidden leading blocks are still emitted at zero height
so indices keep their meaning. Changing what the header emits means revisiting that arithmetic.

**Almost all copy is hardcoded English.** Onboarding, stories, tab labels, badge text and the error/empty
messages are `stringReference("…")` literals in converters and transformers, pending Lokalise. Only
`R.string.common_search`, `R.string.common_in_progress` and the two reused `prediction_onboarding_legal*`
keys are real resources. New strings go through the `lokalise-strings` skill.

**Routes are not `@Serializable`.** The stack is built with `childStack(serializer = null)`, so the inner
back stack does not survive process death.

**The `"all"` tab id is synthesized.** `CollectiblesDiscoveryTabsConverter` prepends an "All cards" tab with
id `"all"` that reports `null` as the category. A backend category with that literal id would collide as a
lazy-list key.