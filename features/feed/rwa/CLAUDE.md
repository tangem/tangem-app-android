# Real Assets Feed Tab

Tokenized real-world assets as a tab of the v2 feed: paginated token list, sort pills, interval picker,
a "Featured assets" rail, and a category/sector filter on a pushed screen. One module,
`:features:feed:rwa:impl` — no `api`, tabs are discovered through a multibinding.

## How it plugs in

Contract and "add a home tab" recipe: `features/feed/README.md`. RWA bindings:

- `RwaFeedTabContributor` — `@IntoSet FeedTabContributor`, `id = FeedTabId.RealAssets`. The id must also be
  in `FeedTabsOrder` (`:features:feed:impl`) or the tab is dropped with an error log.
- `RwaFeedTabContributor.createSearchTab` — implementing it is the *only* opt-in to search.
- `CategorySelectorScreenFactory` — `@IntoMap @ClassKey(CategorySelectorRoute::class)`, in `CategorySelectorModule`.
- `ModelModule` — `RwaFeedTabModel`, `RwaFeedSearchTabModel`, `CategorySelectorModel`.
- `app/build.gradle.kts` — Hilt aggregation line `projects.features.feed.rwa.impl`.

```bash
./gradlew :features:feed:rwa:impl:testDebugUnitTest :features:feed:rwa:impl:detekt
```

## Tab composition

```
RwaFeedTabComponent            owns LazyListState (see Gotchas)
  ├─ PromoBannersBlockComponent
  └─ RwaFeedTabModel           also CategorySelectorComponent.ModelCallback
       ├─ RwaFeaturedAssetsManager → RwaFeaturedUM  (BatchFlowType.Featured)
       └─ RwaBatchFlowManager      → RwaListUM      (paginated)
RwaFeedSearchTabComponent
  └─ RwaFeedSearchTabModel     GetSearchResultsUseCase + RwaBatchFlowManager (BatchFlowType.Search)
```

Rows for both: `ui/RwaListItems.kt`.

## Category resolution

Two category **codes** are reserved (`ReservedCategoryCodes`), so nothing renders until categories load:

- `featured-assets` — backs the Featured rail.
- `real-world-assets` — the umbrella: the tab's default scope, shown as "All" in the selector.

- **An empty `CategorySelection` is the only "no filter".** The umbrella id lives only in `listRequest`
  (`orUmbrella` at request time), never in `appliedSelection` — the chip reads "no filter" as
  `category == null`. Sector "All" likewise unfilters the sector only.
- **The list is never requested unscoped** — that returns the whole coin universe. `reloadList()`
  re-fetches categories while the umbrella id is unresolved.
- Missing `featured-assets` → rail hidden; missing `real-world-assets` → `RwaListUM.Error`.

## Search tab

The host constructs every tab's search page and passes the raw query; this model owns trimming, the
500 ms debounce and the blank-query no-fetch. Fixed legacy scope (24h, rating, no sector), ignores the
applied filter — but must resolve the umbrella first (unscoped = coin universe).

- `loadCategories()` runs on `modelScope`, never in the `collectLatest` query collector (each keystroke
  would cancel it). In-flight guard, not cancel-and-restart.
- `scheduleReload()` is the only request path, held in a `JobHolder` — otherwise a retry plus the revived
  original wait fire two requests.
- `isCategoriesFailed` outranks "not found"; `retry()` re-resolves the umbrella before reloading.

Shared search chrome: `FeedSearchSections.kt` in `:features:feed:api`.

> **Unverified backend contract:** `GET v1/coins/list` gets `search` *and* `categoryId` together (no other
> caller does). If the server ignores `categoryId` the tab returns crypto; if the umbrella isn't
> text-indexed everything is `NotFound`. Check a live request.

## Category selector screen

Pushed via `CategorySelectorRoute` (not a sheet), carrying the applied `CategorySelection` and the tab
model as `ModelCallback`.

- **A tap applies and closes**, except a category with a sector choice: it becomes a draft, sector taps
  move the draft, and the Apply footer (shown while the draft differs) confirms. No Reset — "All" is the
  reset. An unchanged pick only closes, without `ModelCallback` (no reload, no `Select Category`).
- **Sectors exist only from two up** — `CoinCategory.hasSectorChoice`, read by both the transformer's
  section list and the model's apply-or-draft branch.
- UM is built with a real `Transformer` + `Converter`s (`CategorySelectorTransformer`, `CategoryTagConverter`,
  `CategorySectorConverter`) — extend that, not inline `copy`.

## Request flow

| Change | Reload? |
|---|---|
| Sort | always |
| Category / sector | always |
| Interval under `RwaSort.MarketCap` | no — re-render + fetch charts for the new interval |
| Interval under gainers/losers | yes — ranked by the interval |

- **Featured rail** is a separate request, the app's only `Order.ByCategory` caller (any other order
  discards the curation). Never pages (first batch, ≤ 10).
- **Interval never reloads the rail** — tokens carry every interval's percent; it re-maps.
- **The rail ignores the category filter but sends the country**: `openTokenDetails` resolves ids through
  the rail too, and a country-less token arrives `isRestrictedForCountry = false`, skipping the region gate.

## Gotchas

- **`model/list/` is cloned from `features/feed/crypto/impl`** (`BatchListStateManager` identical, the
  `RwaBatchFlowManager`/`RwaListItemConverter`/`RwaTypes` near-copies). A fix here usually belongs there too.
- **`RwaBatchFlowManager.request` is never collected**, only `.value`-read. Writing `listRequest` does nothing
  until `reload()`. `RwaFeaturedAssetsManager` *does* collect `selectedInterval`, and never sees `listRequest`.
- **Reserved codes are backend strings** (TWI-1477). A rename silently empties the rail or the list.
- **Nothing on the feed stack is serialized** (`serializer = null`): no process-death restore, `@Serializable`
  on a `FeedRoute` is inert (TWI-1608).
- **Interval and sort are model state.** Consequences live in their `drop(1)` collectors in `init`; the
  setters are bare because the UI re-sends the current value and only `StateFlow` equality stops a reload.
- **The component owns `LazyListState`** — unselected pages stay `CREATED`, so scroll survives tab switches.
- **Category/sector names come from `code` via `RwaCategoryNames`**, falling back to `displayName`. `id` is
  what the selector matches and what is sent as `sectorId`; `code` is only the localization key.
- **The umbrella arrives `visible = false`.** `CategorySelectorModel` keeps `isVisible || code == REAL_WORLD_ASSETS`
  — dropping it removes the "All" row.
- **Geo decides `restricted` only, narrows nothing.** US/GB open on `commodities` (else first allowed category,
  never the umbrella) via `RwaDefaultCategory`, once per model. Selector "All" still clears to unfiltered.
- **`UserCountryResolver` has no timer** — the country is published once per process; relies on
  `DefaultSettingsRepository.fetchUserCountryCode()` publishing on every path, else the tab stays loading.
- **Categories are deliberately uncached** — the response carries per-country restrictions; tab, selector and
  search each fetch their own.