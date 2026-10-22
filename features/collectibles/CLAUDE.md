# Collectibles Feature

Gacha packs from Collector Crypt. Users land on an onboarding screen, optionally watch a stories
explainer, then browse a paginated catalogue of packs ("gacha machines") filtered by category and
search, open a pack's details — its odds, its metadata and the collectibles in its prize pool — and buy
and open a pack: confirm order → pack throw → reveal video → reward card, then keep the card or sell it
back. The collection screen, the activity log and withdrawal are not built yet; the main screen's
balance is live, its collection widget is a stub.

> This is **not** `features/nft` / `domain/nft`, which is the unrelated NFT-collection feature.
> Collectibles owns its own vertical slice: `domain/collectibles`, `data/collectibles`, and the
> `collectibles` endpoints in `grow/datasource`.

**Baseline:** `develop` @ `64cd60ab09` plus [REDACTED_TASK_KEY] (pack opening), 2026-10-12.

Gated by `FeatureToggles.TWI_1640_COLLECTIBLES_PHASE_ONE_ENABLED`. The toggle is read only by
`DefaultCollectiblesFeatureToggles`; everything outside the feature (the feed tile, the account list
producer) consumes it via `CollectiblesFeatureToggles`.

## Module map

```
features/collectibles/
  api/    — CollectiblesEntryComponent, CollectiblesFeatureToggles,
            walletblock/ (CollectiblesWalletBlockComponent + CollectiblesWalletBlockUM — the wallet screen's account row)
  impl/
    common/ui/      — CollectiblesStatus (inline error/empty), CollectiblesLoadMoreEffect, CollectiblesBottomSheet,
                      CollectiblesPackImage
    di/             — Component, Model and FeatureToggles Hilt modules
    onboarding/     — First screen; marketing copy, FAQ, "Create Gacha account"
    stories/        — Slide-based explainer, reachable again from the main menu
    main/           — Account chrome: balance, actions, collection widget, context menu, the LazyColumn
    addfunds/       — "Add funds" sheet opened from main (swap from a Tangem wallet / receive from another)
    discovery/      — The packs catalogue: tabs, search, paginated pack rows
    machinedetails/ — One machine: hero, odds, pack facts, paginated prize pool, Turbo switch, pinned CTA footer
    packopening/    — Buy + open one pack: confirm order, pack throw, reveal video, reward card
    walletblock/    — DefaultCollectiblesWalletBlockComponent, the account row rendered in the wallet screen
```

`packopening/` in detail:

```
packopening/
  CollectiblesPackOpeningComponent  — the route; bottom-sheet slot, system back handler, reports the app theme
  CollectiblesConfirmOrderBottomSheetConfig, sheet/CollectiblesConfirmOrderSheetComponent — PackUnopened / LowInventory
  PackOpeningConst                  — VIDEO_READY_TIMEOUT_MS, card-slab constants
  model/CollectiblesPackOpeningModel
  model/converter/                  — CollectiblesPackOpeningConverter (order), CollectiblesRewardCardConverter
                                      (RewardCardUM from OpenedCollectiblePack)
  video/                            — PackRevealVideoPlayer, DefaultPackRevealVideoPlayer(+Factory)
  video/source/                     — PackRevealAsset, PackRevealAssetCache,
                                      PackRevealMediaSourceFactory
  ui/components/{confirmorder,packreveal,rewardcard}, ui/preview, ui/state
```

**Package convention:** both sides use the **plural** `com.tangem.features.collectibles.*` — api is
`…collectibles.api`, impl is `…collectibles.impl`. (Unlike swap, there is no singular-`feature` legacy
quirk here.) Impl has no `res/`, no `AndroidManifest.xml` and no `androidTest/`; strings live in
`core/res`, tests under `src/test/kotlin/`.

**Build / test:**
```bash
./gradlew :features:collectibles:impl:compileDebugKotlin
./gradlew :features:collectibles:impl:testDebugUnitTest
./gradlew :features:collectibles:impl:detekt
```

## Where to start reading

| Symbol | Role | Path |
|---|---|---|
| `CollectiblesEntryComponent` | The public screen API; `Params` carry the wallet and which screen to start on | `api/.../collectibles/api/CollectiblesEntryComponent.kt` |
| `DefaultCollectiblesEntryComponent` | Owns the inner `childStack` + `InnerRouter` | `impl/.../impl/DefaultCollectiblesEntryComponent.kt` |
| `CollectiblesRoute` | The inner stack's `@Serializable` routes | `impl/.../impl/CollectiblesRoute.kt` |
| `CollectiblesBlockModel` | Outside this feature: settles the wallet and the start screen before pushing the route | `features/feed/impl/.../feed/components/v2/blocks/model/CollectiblesBlockModel.kt` |
| `CollectiblesMainComponent` | Hosts the screen, the add-funds/receive slot, and embeds discovery as `child(key = "discovery")` | `impl/.../impl/main/CollectiblesMainComponent.kt` |
| `CollectiblesDiscoveryModel` | Catalogue orchestration: categories, search debounce, pagination actions | `impl/.../impl/discovery/model/CollectiblesDiscoveryModel.kt` |
| `CollectiblesDiscoveryContentTransformer` | Pure `PaginationStatus` → `ContentUM` mapping; de-dup + row chunking | `impl/.../discovery/model/transformer/…Transformer.kt` |
| `CollectiblesDiscoveryUM` | Tabs, `ContentUM` (Loading/Content/Error/Empty), hoisted `TangemSearch.State` | `impl/.../discovery/ui/state/CollectiblesDiscoveryUM.kt` |
| `CollectiblesMachineDetailsModel` | One machine plus its prize-pool pagination; holds the Turbo flag | `impl/.../machinedetails/model/CollectiblesMachineDetailsModel.kt` |
| `CollectiblesPackOpeningModel` | Runs the purchase, picks full vs short reveal, drives the `Stage` | `impl/.../packopening/model/CollectiblesPackOpeningModel.kt` |
| `CollectiblesPackOpeningScreen` | Layers confirm order / reveal video / reward card by `Stage` | `impl/.../packopening/ui/CollectiblesPackOpeningScreen.kt` |
| `DefaultPackRevealVideoPlayer` | Two ExoPlayers (video + skip sound); trips the card reveal | `impl/.../packopening/video/DefaultPackRevealVideoPlayer.kt` |
| `CardChoreography` / `RewardCardTiming` | Pure maths for the card's grow / turn / settle; `Default` and `Skipped` timings; JVM-tested | `impl/.../packopening/ui/components/rewardcard/CardChoreography.kt` |
| `OpenCollectiblePackUseCase` | Buy → pay on chain → await payment → open; emits `OpenPackStep` | `domain/collectibles/.../usecase/OpenCollectiblePackUseCase.kt` |
| `CollectiblesMainList` | The single `LazyColumn`; header + discovery's items + filler | `impl/.../main/ui/components/CollectiblesMainList.kt` |
| `CollectiblesMainUM` | Screen chrome only — balance, actions, collection, menu | `impl/.../main/ui/state/CollectiblesMainUM.kt` |

## Navigation

```
AppRoute.Collectibles(userWalletId, startRoute) → DefaultCollectiblesEntryComponent  (childStack "collectiblesStack")
  ├─ CollectiblesRoute.Onboarding(userWalletId) → CollectiblesOnboardingComponent → CollectiblesOnboardingModel
  │     account ready → router.replaceAll(Main(id), Stories)   [Stories pushed on top of Main]
  ├─ CollectiblesRoute.Stories                  → CollectiblesStoriesComponent    → CollectiblesStoriesModel
  │     last-slide continue / close → router.pop() → lands on Main
  ├─ CollectiblesRoute.Main(userWalletId)       → CollectiblesMainComponent       → CollectiblesMainModel
  │     menu "How it works" → router.push(Stories)
  │     slot: TopUp (CollectiblesAddFundsComponent) / Receive (TokenReceiveComponent)
  │     "From Tangem wallet" → AppRoute.Swap(accountFlow = CollectiblesTopUp)   [app router]
  │     embedded child (NOT a stack or slot entry): child(key = "discovery")
  │         → CollectiblesDiscoveryComponent → CollectiblesDiscoveryModel
  │             pack tap → router.push(MachineDetails(externalId))
  ├─ CollectiblesRoute.MachineDetails(externalId) → CollectiblesMachineDetailsComponent → CollectiblesMachineDetailsModel
  │     slot: TurboInfo sheet
  │     "Open for N USDC" → router.push(ConfirmOrder(externalId, isTurboEnabled))
  └─ CollectiblesRoute.ConfirmOrder(externalId, isTurboEnabled)
        → CollectiblesPackOpeningComponent → CollectiblesPackOpeningModel
        confirm order → throw → video → card, all inside the one route
        slot: PackUnopened(txId) / LowInventory sheets
        "Enable Turbo" → router.pop(); "Explore packs" → router.popTo(Main(id))
        "Keep card" / a completed "Sell back" → router.pop() to MachineDetails
```

**The feature decides nothing about where it opens** — the caller does, and the route says so:
`AppRoute.Collectibles(userWalletId, startRoute)` with `CollectiblesStartRoute` (`Onboarding` / `Main`, in
`common/routing/.../entity/CollectiblesStartRoute.kt`). `DefaultCollectiblesEntryComponent` maps it to the
stack's `initialConfiguration` in one `when`; there is no gate and no loader route.

**The wallet and the start screen are settled outside, in the feed.** `CollectiblesBlockModel`
(`features/feed/impl`, hosted by `CollectiblesBlockComponent` as `child(key = CollectiblesBlockComponent.KEY)`
inside `FeedTopBlocksComponent`) takes every unlocked multi-currency wallet on a tap: one → picked silently,
several → the `PortfolioSelectorComponent` sheet in the block's own slot, none → nothing happens. Then
`SingleAccountListSupplier` decides the start screen: an `Account.Collectibles` in the wallet's list means
`Main`, its absence — or an unreadable list — means `Onboarding`. `DefaultSingleAccountListProducer`
(`data/account`) adds that account once the collectibles account exists and the toggle is on.

Why there and not here: `AppRoute.Collectibles` is a full-screen entry of the root stack, and `Children`
draws only the active child, so a modal sheet raised inside this feature scrims an empty screen.

Reached from two places: the feed tile (visibility from `CollectiblesFeatureToggles.isCollectiblesEnabled`
via `CollectiblesBlockModel.isVisible`) and the wallet screen's account row, whose click goes through
`DefaultWalletRouter.openCollectibles` with `startRoute = Main`. The row itself is rendered by the
api-level `CollectiblesWalletBlockComponent`.

Inner navigation is an `InnerRouter<CollectiblesRoute>` — routes of that type go to the inner stack,
everything else falls through to the app `AppRouter`. `onChildBack()` reads
`if (stack.value.backStack.isEmpty()) router.pop() else stackNavigation.pop()`, so back from the first
inner screen leaves the feature entirely. Bottom sheets are per-screen `childSlot`s (main, machine details,
pack opening), each with `handleBackButton = false`.

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

## Pack opening

`CollectiblesPackOpeningModel` loads the machine (from memory when opened from the catalogue), watches the
vault balance via `CollectiblesAccountStatusSupplier`, and on approve runs
`OpenCollectiblePackUseCase(userWallet, OpenPackRequest.Buy(externalId, isTurboEnabled))`. The use case
emits `Generating → Confirming → Opening → Opened(pack)` or an `OpenPackStep.Failed`:
`LowInventory` / `PackUnopened(txId)` open a sheet (`PackUnopened` retries with `OpenPackRequest.Reopen`),
everything else is a snackbar.

The UI beat is `CollectiblesPackOpeningUM.Stage` — `Order → Launching → Reveal → Reward`:

1. On `Opened` the model waits for the reveal video (`readiness != Loading`, at most
   `PackOpeningConst.VIDEO_READY_TIMEOUT_MS`), then sets `Launching` with `isFullReveal = Ready`; a full
   reveal also calls `player.start()`.
2. When the throw (`PackLaunchMotion`) finishes: full reveal → `Reveal` (or straight to `Reward` if the
   card reveal was already reached); short version → `Reward` plus `playSkipSound()`.
3. The player's `onCardRevealReached` moves `Reveal → Reward`.
4. A tap during `Reveal` calls `player.skipToReveal()` (seek to `skipToMs`, mute the video, play the short
   sound; `false` once the burst has begun). On success the card uses `RewardCardTiming.Skipped`.

**Reveal media come from the backend.** `MachineDTO.openPackMedia` (`videoLight`, `videoDark`,
`skipAnimationAudio`) → `CollectibleGachaMachine.RevealAssets` (`lightVideoUrl`, `darkVideoUrl`,
`skipAudioUrl`); without `openPackMedia` the machine has no `RevealAssets`, the player gets `null` and goes
`Failed`, and the short version runs. The component reports `LocalIsInDarkTheme` through
`onThemeChanged`, and the model preloads the matching video while still on `Order`; a theme change there
reloads it. Remote URLs are downloaded in full into `PackRevealAssetCache` (a 256 MB LRU media3
`SimpleCache` in `pack-reveal/`) before playback, so the reveal never buffers.

### Sell back

The reward card's "Sell back" sells the won card for the backend's instant buyback price.

- **Availability is polled in the model.** On `OpenPackStep.Opened` (before the throw, so it runs alongside
  the video) the model starts a `SingleTaskScheduler` + `PeriodicTask` that calls
  `GetCollectibleBuybackOfferUseCase` (one request) at once and then every 5 s. It is state-driven like the
  expected-value polling in `CollectiblesMachineDetailsModel`: it runs only while the offer is unknown
  (`BuybackState.Checking`) and the screen is resumed (`lifecycle.subscribe(onResume, onPause)` in the
  component). `buybackAvailable = false` means "not yet", so there is no deadline. Availability comes from
  `/buyback/available` alone, for every machine. While it is unknown the Sell back button spins; a disabled
  "Buyback unavailable" is only for `Unavailable`.
- **The sale is `BuybackCollectibleUseCase`**: `buyback-data` with `fromAddress = owner` (it holds the card)
  and `toAddress = vault` (`getVaultAddress`, so the payout shows in the collectibles balance) →
  `GachaVaultRepository.sendWithOwnerSignature` (the backend's base64 transaction signed as is with the owner
  key) → `buyback-tx-sent` → 5 polls of `/buyback/status` every 2 s. It is `suspend` and returns
  `Either<BuybackError, Unit>`: `confirmed` → right; `expired` or no `confirmed` in 10 s → `NotConfirmed`. There
  are no intermediate steps, the button only spins; the snackbar's amount comes from the offer.
- **Only success leaves the screen** (snackbar + `router.pop()`). `NotAllowed` (the backend refuses
  `buyback-data`) makes the button unavailable with a snackbar; `Cancelled` / `CardRefused` re-enable it
  silently; `Unexpected` re-enables it with "Something went wrong". A message over the card's 930 B
  (`GachaVaultError.TransactionTooLarge`) counts as `Unexpected`.
- **`NotConfirmed` does not re-offer at once.** The sale may still land, so after "Something went wrong" the
  button goes back to `Checking` (spinner) and the same `/buyback/available` polling decides again. Whether the
  gacha guards against a second buyback of the same NFT is still open with Collector Crypt.
- While the sale runs back, close and "Keep card" are ignored (`isBuybackRunning()`): leaving would cancel
  `modelScope` and the tx hash might never reach the backend. "Keep card" is disabled only then
  (`RevealUM.isKeepCardEnabled`), not while the offer is being checked.

`BuybackState` (`Checking` / `Available` / `Unavailable` / `InProgress`) is the model's own;
`CollectiblesSellBackButtonConverter` turns it into `SellBackButtonUM` (text, `isEnabled`, `isLoading`), which lives
in `RevealUM`, not in `RewardCardUM`, because it changes while the card is on screen.

## Domain & data

```
domain/collectibles/models/  — CollectibleGachaMachine (+ Odds/Stock/TierRanges/RevealAssets), categories,
                               CollectibleGachaMachineNft, CollectibleRarity, CollectibleTransaction
                               (purchase tx, tx state/status, OpenedCollectiblePack), OpenPackRequest/Step,
                               CollectibleBuyback (offer, tx, status), BuybackError,
                               CollectibleGachaMachinesPage/Batch/ListConfig, CollectiblesError
domain/collectibles/         — CollectiblesRepository, CollectiblesAccountRepository, GachaVaultRepository,
                               account status supplier/fetcher/producer, hot delegate, owner deriver,
                               9 use cases, the Batch* typealiases
data/collectibles/           — DefaultCollectiblesRepository, batch fetchers, converters, CollectiblesMachineStore,
                               account store/producer/cleaner, gacha vault, hot delegate
grow/datasource/collectibles — CollectiblesApi (Retrofit), CollectiblesAuthProvider, request/response DTOs
                               (kotlinx.serialization)
```

The catalogue/machine use cases return `Either<CollectiblesError, T>` via `runSuspendCatching`, except the
two `Get…BatchFlowUseCase`s, which are non-suspend and return the flow directly.
`CreateOrReactivateCollectiblesAccountUseCase` / `CloseCollectiblesVaultUseCase` return
`Either<CollectiblesAccountError, Unit>`, `BuybackCollectibleUseCase` returns `Either<BuybackError, Unit>`;
`OpenCollectiblePackUseCase` returns `Flow<OpenPackStep>` and never throws.

Use cases are plain classes with an `@Inject` constructor — there is **no** `CollectiblesDomainModule`
anywhere, so that annotation is the whole wiring. A use case without it cannot be injected.

**Request headers.** `session-id` is a config-level header on *every* collectibles endpoint —
`Collectibles` (`grow/datasource/config/`) puts `ProviderSuspend(collectiblesAuthProvider::getSessionId)`
into its header map. The id is one UUID per process (`DefaultCollectiblesAuthProvider` is provided
`@Singleton` in `GrowDatasourceModule`). Every endpoint is also annotated `@RequiresSessionAuth`
(`libs/auth`). `user-id` is a per-method Retrofit `@Header` carrying `UserWalletId.stringValue` on
`getMachineNfts`, `generatePurchaseTx`, `purchaseTxSent`, `getTxStatus`, `openPack` and the four
`buyback/*` endpoints. Adding a header to
the config requires updating `CollectiblesConfigTest`, which asserts the exact header set per environment.

## DI modules

| Module | Scope | Provides |
|---|---|---|
| `CollectiblesComponentModule` | `SingletonComponent` | `CollectiblesEntryComponent.Factory`, `CollectiblesDiscoveryComponent.Factory`, `CollectiblesWalletBlockComponent.Factory`, `PackRevealVideoPlayer.Factory`, `PackRevealMediaSourceFactory` |
| `CollectiblesModelModule` | **`ModelComponent`** | All six models `@IntoMap @ClassKey` into the model map read by `getOrCreateModel()` |
| `CollectiblesFeatureTogglesModule` | `SingletonComponent` | `CollectiblesFeatureToggles` |
| `CollectiblesDataModule` (in `data/collectibles/`) | `SingletonComponent` | Repositories, owner deriver, account store/supplier/fetcher, machine store, wallet data cleaner |
| `CollectiblesDelegateModule` (in `data/collectibles/`) | `SingletonComponent` | `CollectiblesHotDelegate` |

The onboarding, stories, main, machine-details and pack-opening components are **not** bound in
`CollectiblesComponentModule` — their nested `@AssistedFactory interface Factory` types are generated by
Hilt and injected directly. Sheet components (`CollectiblesAddFundsComponent`,
`CollectiblesConfirmOrderSheetComponent`, the Turbo info sheet) are constructed by hand in their host.

## Testing

JUnit 5 + MockK + Truth + Turbine via `testImplementation(projects.test.core)`, plus `test.mock`,
`common.test` and `core.error`, already declared — new model/converter tests need no `build.gradle.kts`
change. See `.claude/rules/unit-testing.md` for the project-wide conventions.

Models are constructed **directly** — `Model` is a plain abstract class whose only abstract member is
`dispatchers`, so there is no Decompose runtime, no Hilt and no `AppComponentContext` in a model test.
Tests use the shared `TestScope.createTestingCoroutineDispatcherProvider()` from `:test:core` and call
`model.onDestroy()` to cancel `modelScope`.

Reference tests in this feature: `CollectiblesDiscoveryContentTransformerTest` (parameterized status
table), `CollectiblesDiscoveryModelTest` (fake `BatchFlow` + captured `BatchingContext`),
`CollectiblesPackOpeningModelTest` (relaxed `PackRevealVideoPlayer` mock, captured `Listener` to fire the
card reveal), `DefaultPackRevealVideoPlayerTest` (mocked `ExoPlayer`s), and the root-level `MachineFactory.kt`
for the all-arguments-defaulted fixtures (machines, categories, NFTs, `createOpenedPack`).

## Gotchas

**The main screen's balance is real, the collection size is not.** The balance comes from
`CollectiblesAccountStatusSupplier` + `GetSelectedAppCurrencyUseCase`, mapped by `CollectiblesBalanceConverter`;
an unread vault shows a dash rather than a zero. The collection widget shows `STUB_CARDS_COUNT`.
The amount is the vault's USDC, read by listing the token accounts the vault PDA owns and picking the mint the
program config names; the SOL on the vault owner key is not part of it.

**`actionsFlow` needs `replay = 1`.** The first `Reload` is emitted while pagination is still subscribing;
a shared flow without a replay cache silently drops what it cannot deliver yet, and the catalogue never
loads.

**The categories are a precondition, not a decoration.** `reloadMachines()` sits *inside* the `onRight`
block in `loadCategories()`: a categories failure hides the tabs and puts the screen into `ContentUM.Error`
without requesting a single pack. They carry `totalMachinesQuantity`, which tells an empty catalogue from an
unreachable one and drives `hasSearchableCatalogue` (the search field's visibility), so a screen without them
cannot be rendered honestly. `reload()` retries them, and the catalogue follows on success.

**The transformer must `distinctBy(id)`.** The keyset cursor pages over a shifting order, so a machine that
moved between two page requests arrives in two batches. Duplicate lazy-list keys crash the list.

**`None`/`InitialLoading` over existing `Content` keeps the rows.** It sets `isReloading = true` instead of
emptying the list, because Compose discards a `LazyListState`'s scroll position outright once the first
visible index no longer exists.

**Search activation swaps to a second `LazyListState`.** `createSearchEntry` seeds a fresh list state from
the main one's `firstVisibleItemIndex/Offset`; the hidden leading blocks are still emitted at zero height
so indices keep their meaning. Changing what the header emits means revisiting that arithmetic.

**`getMachine` is served from memory when it can be.** The catalogue endpoint returns a complete machine per
list entry, so `DefaultCollectiblesRepository` writes every page into `CollectiblesMachineStore` (an in-memory
`RuntimeSharedStore` keyed by `externalId`) and `getMachine(skipCache = false)` reads it — a pack opened from
the catalogue paints without a round trip. The prize pool is deliberately *not* cached across entries: it is
live inventory that shrinks as packs are opened. Do not reach for `CacheRegistry` here — its time-based expiry
is globally disabled, so `skipCache = false` would degenerate to fetch-once-per-process.

**The prize pool pages by page number, the catalogue by cursor.** `CollectibleGachaMachineNftsBatchFetcher`
advances from the page it *requested*, not the one the backend echoes, so a backend that repeats a page number
cannot pin the pagination.

**The rarity ladder is `common`/`uncommon`/`rare`/`epic`, one vocabulary from the wire to the pixel.**
`CollectibleRarity` (`domain/collectibles/models`) names it; `MachineDTO`'s nested odds/stock/tierRanges
DTOs use the same four keys, and `CollectiblesMachineTiersConverter` keys its `TIERS` table on the enum.
`epic` is the only optional tier — a machine without it renders three odds cards instead of four, which
is what the `mapNotNull` in that converter is for. Do not reintroduce a tier whose field name and rendered
label disagree.

**The opening flow is one route.** `CollectiblesRoute.ConfirmOrder` covers the confirm screen, the video
and the revealed card, because the pack is thrown off the confirm screen straight into the video
(`PackLaunchMotion`) and a route boundary would cut that animation in half. The model owns only the coarse
`Stage`; the per-frame choreography runs off `Animatable`s in the UI and the card reveal comes from the player, so
neither recomposes the screen every frame.

**The pack is thrown only once the card is known.** `launchPack` runs on `OpenPackStep.Opened`: an earlier
throw could not be taken back if the purchase then failed. While the purchase runs, back is hidden and
ignored, and stays ignored until the card is shown (`Stage.Reward`) — the component registers its own
`BackCallback`, since leaving would cancel the job and a paid pack with it.

**The card reveal comes off presented frames, with an end-of-video backstop.** `DefaultPackRevealVideoPlayer`
trips the card reveal from `setVideoFrameMetadataListener`, comparing each frame the decoder actually *presented*
against `cardRevealMs` — so it stalls with a starving decoder instead of running ahead of it, which is the
required behaviour rather than a bug. `Player.STATE_ENDED` (and a playback error) is the backstop. Do not
add a `currentPosition` poll for the card reveal: it advances on the timeline and would fire while a stalled
picture was still frozen. The reveal video carries its sound muxed in; the second ExoPlayer only plays the
short skip/fallback sound.

**Card reveal and skip timings are not in the API.** The backend sends URLs only; `PackRevealAsset`
defaults to `cardRevealMs = 7_000` / `skipToMs = 6_100` for the current render. A re-rendered video needs those
updated, and `skipToMs` must land on a keyframe.

**The animation never gates the result.** Video not ready within the timeout, a blank URL, a download or
codec failure, or system animations off (`ANIMATOR_DURATION_SCALE == 0`, which also mutes the skip sound)
— every path falls back to the short version and ends on the card.

**Buyback refusals are not mapped yet.** `CollectibleException.BuybackRefused` waits for the backend's own error
codes (a TODO in `CollectiblesErrorResolver`); until then a refused `buyback-data` is `Unexpected`.

**Statuses are DTO enums.** `CollectibleTxResponse.CollectibleTxStatusDTO` (purchase) and
`BuybackStatusResponse.BuybackStatusDTO` name the wire values with `@SerialName`; an unknown one is coerced to
`UNKNOWN` by `NetworkJsonFactory` (`coerceInputValues`), and the converters map enum to enum with a `when`.

**A card signs at most 930 B of message.** `sendWithOwnerSignature` checks the size before the tap on a card
wallet (`GachaVaultError.TransactionTooLarge`); `SendLargeSolanaTransactionUseCase` cannot help, it rebuilds
the transaction and would break the backend's blockhash and other signatures.

**Trust `/buyback/status`, not the send.** Blink, the first Solana provider, sends with `skipPreflight = true`,
so a transaction that fails on chain can still look sent.

`Expired` / `Refunded` failures and the buyback refusal snackbar show hardcoded English (the refusal text
waits for the analysts). The disabled Sell back button and the success snackbar are localized:
`collectibles_open_pack_buyback_unavailable` and `collectibles_open_pack_sell_back_success`.

**A few strings are still literals.** Most copy is in `core/res` (`collectibles_*`), but some
`stringReference("…")` literals remain (menu, add funds, status, the snackbars above).
New strings go through the `lokalise-strings` skill.

**The `"all"` tab id is synthesized.** `CollectiblesDiscoveryTabsConverter` prepends an "All cards" tab with
id `"all"` that reports `null` as the category. A backend category with that literal id would collide as a
lazy-list key.