# Real Assets Feed Tab

Tokenized real-world assets (stocks, commodities, …) as a tab of the v2 feed — Shtorka 2.0.
Structurally it is a markets list: a paginated token list with sort pills and a trend-interval picker,
a "Featured assets" rail above it, and a category/sector filter reached through a pushed screen.

One Gradle module, impl only (`:features:feed:rwa:impl`). There is no `api` module because tabs are
discovered through a multibinding — nothing outside this module ever names the component type.

## How it plugs in

The plug-in contract and the "add a home tab" recipe are in `features/feed/README.md`; only the
RWA-specific bindings are listed here.

- **`RwaFeedTabContributor`** — `@Binds @IntoSet FeedTabContributor`, `id = FeedTabId.RealAssets`.
  That id must also appear in `FeedTabsOrder` (`:features:feed:impl`) or the tab is dropped with an
  error log.
- **`CategorySelectorScreenFactory`** — `@Binds @IntoMap @ClassKey(CategorySelectorRoute::class)` into
  the feed screen registry, in `CategorySelectorModule`.
- **`ModelModule`** — `RwaFeedTabModel`, `RwaFeedSearchTabModel` and `CategorySelectorModel` into
  `ModelComponent`. The *components* need no binding of their own: a bare nested `@AssistedFactory` is
  bound by Hilt automatically (the Crypto tab declares one only because its factory type lives in an
  `api` module).
- **`RwaFeedTabContributor.createSearchTab`** — implementing that method is the *only* declaration that a
  tab participates in search; see the contract KDoc in `FeedTabContributor`.
- `app/build.gradle.kts` carries the Hilt aggregation line (`projects.features.feed.rwa.impl`).

**Two toggles are required to see the tab.** `isNewShtorkaEnabled` (TWI-1608) selects the v2 feed over
the legacy one in `WalletScreen.kt`; `isRwaAndCategoriesEnabled` (TWI-1477) makes the contributor
available. Both are `"version": "undefined"` in `feature_toggles_config.json` — dev builds only.

```bash
./gradlew :features:feed:rwa:impl:compileDebugKotlin
./gradlew :features:feed:rwa:impl:testDebugUnitTest
./gradlew :features:feed:rwa:impl:detekt
```

## Tab composition

```
RwaFeedTabComponent                   (FeedTabComponent; owns LazyListState — see Gotchas)
  ├─ PromoBannersBlockComponent       child("promoBannersBlockComponent")
  └─ RwaFeedTabModel                  uiState: StateFlow<RwaFeedTabUM>
       ├─ RwaFeaturedAssetsManager    → RwaFeaturedUM   (rail; BatchFlowType.Featured, whole bucket)
       └─ RwaBatchFlowManager         → RwaListUM       (paginated list)
            └─ BatchListStateManager  → RwaListItemConverter → RwaListItemUM

RwaFeedSearchTabComponent             (FeedTabComponent; the search screen's page — see Search tab)
  └─ RwaFeedSearchTabModel            uiState: StateFlow<RwaSearchUM>
       ├─ GetSearchResultsUseCase     → RwaSearchPortfolioUM  ("My portfolio" sections)
       └─ RwaBatchFlowManager         → RwaSearchListUM       (same manager, BatchFlowType.Search)
```

Row rendering is shared by both through `ui/RwaListItems.kt` (`rwaTokenItems` / `rwaTokenShimmers`), which
also owns `VisibleTokensTracker` and the `"token:"` item-key prefix it parses.

`RwaFeedTabModel` also implements `CategorySelectorComponent.ModelCallback`, so it is both the tab's
state holder and the receiver of the category-selector result.

## Category resolution

Nothing renders until categories are fetched, because two category **codes** are reserved rather than
offered as filters (`ReservedCategoryCodes`):

- `featured-assets` — backs the Featured rail; its id is passed to `RwaFeaturedAssetsManager.load()`.
- `real-world-assets` — the umbrella scope, surfaced in the selector as "All", and the tab's default
  list scope.

**An empty `CategorySelection` is the one canonical "no category filter".** The umbrella is a
*presentation* of it in the selector — the row titled "All", checked when nothing is applied, emitting
`CategorySelection()` on tap — and a *request detail* at the network boundary. Its id lives only in
`listRequest`, never in `appliedSelection`, so the tab chip stays `Inactive("All")`. Don't reintroduce
an umbrella-valued selection; the chip and the selector both read "no filter" as `category == null`.
The same rule applies to the sector rail: "All" there unfilters the sector and leaves the category alone.

**The list is never requested unscoped.** With no user filter `orUmbrella` substitutes the umbrella
category at request time, and `reloadList()` re-runs the categories fetch when the umbrella id isn't
resolved yet — an unscoped request would return the entire coin universe rather than real-world assets.
A missing code degrades rather than throws: no `featured-assets` → `RwaFeaturedUM.Empty` (rail hidden);
no `real-world-assets` → the list renders `RwaListUM.Error`.

## Search tab

A second, separate component rather than a mode of the tab: the two share only the asset row, and one
component would build the Featured rail and the filters on the search screen. The host
(`DefaultFeedSearchComponent`) constructs *every* tab's search page when the search screen opens and hands
each one the raw query, so this model owns trimming, the 500 ms debounce, and the rule that nothing is
fetched while the query is blank (`isOnScreen` gate + a blank-query early return).

Search is fixed at the legacy scope — 24h trend, rating order, no sector — and **ignores the tab's applied
category/sector filter**, mirroring the Crypto tab. What it cannot ignore is the umbrella: an unscoped
request answers with the entire coin universe, so `RwaFeedSearchTabModel` resolves
`ReservedCategoryCodes.REAL_WORLD_ASSETS` before any request leaves.

That resolution is the one piece with non-obvious sequencing:

- `loadCategories()` runs on `modelScope`, never inside the query collector — `collectLatest` cancels its
  scope on every keystroke, and a fetch started there would be restarted, and never finish, for a user
  typing faster than it completes. An in-flight guard makes repeated calls free; a cancel-and-restart would
  reintroduce the same starvation from the other side.
- `scheduleReload()` is the only place a request is issued, and it is held in a `JobHolder`. The wait for
  the umbrella is open-ended: a failed fetch leaves it suspended until a retry resolves it, and without
  superseding, the retry's own request **and** the revived original both fire — two identical requests.
- A failed or code-less categories response is a UM state, not a hang: `isCategoriesFailed` feeds the list
  combine and outranks "not found", because a query that never reached the network must not read as
  "no results". `retry()` re-resolves the umbrella before reloading; a bare `listManager.reload()` would go
  out unscoped.

Chrome shared with the other search tabs (section header, "no results") lives in
`:features:feed:search:api` as `FeedSearchSections.kt`, so the tabs cannot drift apart visually.

> **Unverified backend contract.** `GET v1/coins/list` is sent `search` *and* `categoryId` together, which
> no other caller in the app does — Crypto search sends `search` alone, the RWA tab `categoryId` alone. If
> the server treats `search` as a global full-text branch and ignores `categoryId`, this tab silently
> returns crypto results; if it ANDs them but the umbrella is not text-indexed, everything reads as
> `NotFound`. Confirm against a live request before trusting the results.

## Category selector screen

A full screen pushed onto the feed stack via `CategorySelectorRoute`, not a bottom sheet. The tab model
pushes it carrying the currently applied `CategorySelection`, and the route also carries the model
itself as `ModelCallback` — that is how the chosen selection gets back to the tab.

`CategorySelectorModel` tracks a **draft** selection separately from the applied one and hands it to
`CategorySelectorTransformer`, which builds the two sections through `CategoryTagConverter` ("Type"
rows) and `CategorySectorConverter` ("Sectors" rail, with a synthetic "All" tag prepended).

**The Sectors section only exists for a category with more than one sector.** One sector filters no
differently than its category, and zero sectors would render a rail holding nothing but "All", so the
transformer omits the section below that threshold — the category row is then the whole filter and no
`sectorId` is sent. Section presence is decided there, in the `listOfNotNull`; the UI renders whatever
sections it is given.

Both category selectors build their UM with a real `com.tangem.utils.transformer.Transformer` plus
`Converter`s (the Crypto one is `MarketCategorySelectorTransformer`), unlike the inline
`uiState.update { it.copy(...) }` style `RwaFeedTabModel` uses. Extend the transformer pattern here.

## Request flow

A single `RwaListRequest(interval, sort, selection)` describes one list request. How a change is
applied is deliberately not uniform:

- **Sort change** — always reloads. Sorting *is* the query (`TokenMarketListConfig.Order`).
- **Category or sector change** — always reloads.
- **Interval change under `RwaSort.MarketCap`** — no reload. `Order.ByRating` does not depend on the
  interval, so the loaded rows stay correct: re-render them and fetch charts for the new interval.
- **Interval change under gainers/losers** — reloads; those orders are *ranked by* the interval, so the
  row set itself changes.

The **Featured rail is a second, independent request** against the same endpoint, and the only caller in
the app that sends `TokenMarketListConfig.Order.ByCategory` (`order=category`) — the curated order is the
whole point of the rail, and any other order re-sorts the bucket and discards it. The manager never
emits `LoadMore`, so the rail never pages and shows whatever the first `BatchFlowType.Featured` batch
returns (capped at `firstBatchSize = 10` in `GetMarketsTokenListFlowUseCase`).

**An interval change never reloads the rail.** A token carries the change percent for *every* interval at
once (`price_change_percentage` holds `24h`/`1w`/`30d` regardless of the `interval` param), so the rail
re-maps what it already has — the same reason the list skips its reload under `RwaSort.MarketCap`. The rail
also ignores the applied category/sector filter, which scopes only the list below it.

**The rail does send the country, though.** It is the one filter the rail shares with the list, because
`openTokenDetails` resolves a tapped id through the list *and then* the rail — a rail token fetched without
a country arrives `isRestrictedForCountry = false` and walks straight past the region gate on the token
screen (`MarketsTokenDetailsModel`), notice and Add-to-portfolio block included.

## Gotchas

**Everything under `impl/.../model/list/` has a twin in `features/feed/crypto/impl`.** This module was
created by cloning the Crypto (Market Pulse) tab. `BatchListStateManager` is statement-for-statement
identical to `crypto/impl/.../model/list/BatchListStateManager.kt` (the KDoc differs, so a raw checksum
comparison reports a false negative), and `RwaBatchFlowManager` / `RwaListItemConverter` / `RwaTypes` are
near-copies of the `MarketPulse*` files. A fix here almost always belongs there too — extracting a shared
kernel is still open.

The two managers have deliberately diverged in **how the search text arrives**. Crypto passes a
`currentSearchText: () -> String?` lambda *and* a `reload(searchText)` argument, and needs a guard so the
two cannot disagree; here `searchText` is a field on `RwaListRequest`, so `reload()` stays argument-free
and the disagreement is unrepresentable. `isEmptyResultState` likewise drops Crypto's query term: a
predicate reading a value outside its own upstream only recomputes when the batch state happens to move,
and so stays stale across the whole gap between a new query and its reload.

**`RwaBatchFlowManager.request` is a `StateFlow` that is never collected.** It is only `.value`-read —
in `reload()`, and for the interval the item converter renders with. Writing to
`RwaFeedTabModel.listRequest` therefore does nothing to the list on its own — the model must call
`reload()` afterwards. Don't add a filter field and assume the flow will pick it up.

`RwaFeaturedAssetsManager` is the opposite: it takes the model's `selectedInterval` flow and *does* collect
it, to re-map the rail. It deliberately does not see `listRequest`, so it is unaffected by the one gap in
that object's upkeep — `reloadList()` skips `syncListRequest()` while the umbrella category is unresolved.

**The reserved category codes are backend contract strings** (`featured-assets`, `real-world-assets`,
TWI-1477). Neither is derived from the other, and a rename on either side silently empties the rail or the
whole list with no error — the rail has no error state and no retry, so a wrong code is indistinguishable
from a category the backend does not serve.

**Nothing on the feed stack is serialized.** Every v2 navigation call passes `serializer = null`
(`DefaultFeedV2Component.kt`, `FeedHomeComponent.kt`), so feed state does not survive process death and
`@Serializable` on a `FeedRoute` is currently inert. Don't rely on restoration when adding a screen,
and treat the "routes are pure data" rule in `features/feed/README.md` as unenforced by the compiler
(TWI-1608).

**The interval and the sort are model state, not UI state.** `RwaFeedTabModel.selectedInterval` and
`selectedSort` are the sources of truth (`RwaFeedTabUM.interval` and `sortTabs` only mirror them), and every
consequence of a switch lives in that value's own `drop(1)` collector in `init` — `selectInterval()` and
`selectSort()` are just setters. This mirrors `CryptoFeedTabModel`, and the conflation is load-bearing:
both the interval menu and the sort tabs invoke their callback unconditionally, including on the value
already selected, so the only thing that stops a redundant reload is a `StateFlow` refusing to re-emit an
equal value. Add interval- or sort-driven work to those collectors, not to the click handlers.

The interval is presented as a chip + dropdown (`ui/RwaIntervalMenu.kt`, anchored by `IntervalFilter` in
`RwaFeedTabContent.kt`), copied from the Crypto tab's `MarketPulseIntervalMenu` — itself a copy of the
legacy `SortByMenu`. `TangemContextMenu` is a slot API with no row or selection component, which is why
all three hand-roll the same row and check-circle.

The sort is presented as `TangemTabNavigation` (DS3), built in the model as an
`ImmutableList<TangemTabItemUM>`: the component derives selection from each item's `isSelected` and takes a
per-item `onClick`, so there is no index contract and no feature-local pill UM. `RwaSort.id` is the tab key
— domain-derived, never text- or resource-derived. The Crypto tab was migrated the same way in the same
change, so `sortTabs` is a real twin there too.

being destroyed, so holding scroll position in the component is what makes it survive tab switches.

**Category and sector names are resolved from their `code`, never from the backend `displayName`.**
`RwaCategoryNames` maps a category code — and a (category code, sector code) pair, since a sector code
is only unique within its category — to `markets_real_assets_category_*` /
`markets_real_assets_sector_xstocks_*`, falling back to `displayName` for anything it does not know, so
a category the server adds before the app ships a key degrades rather than breaks. Only `xstocks`
sectors have keys so far, and only `values-de` and `values-pt-rBR` are translated — every other locale
resolves to the English base.

The code travels in `CategorySelection.Entry` so the applied-filter chip resolves names the same way as
the selector. **Don't confuse it with `id`:** `id` is the identity the selector matches on and the value
`RwaBatchFlowManager` sends as `sectorId`; `code` is only ever the localization key. DS3 component rules
are in `.claude/rules/codestyle/design-system.md`.

**The umbrella arrives with `visible = false`.** The categories spec says only visible categories are
returned; the backend returns the umbrella anyway, flagged invisible. `CategorySelectorModel` therefore
filters on `isVisible || code == REAL_WORLD_ASSETS` — dropping it would remove the row that clears the
category filter, leaving no way back to "All". `RwaFeedTabModel` reads the umbrella from the unfiltered
list, so the list scope was never affected.

**Geo: the country only decides `restricted`, it narrows nothing.** `getCoinCategories(country)` sends
ISO 3166-1 alpha-2 as a query param; omitting it returns the same categories with every `restricted`
false. That is why the tab's default category cannot be derived from the flag — the umbrella is
restricted nowhere. `US`/`GB` open on `commodities` instead — or, if the backend stops serving it or
blocks it too, on the first **allowed** category by `displayOrder` that the selector would offer, and on a
blocked one only when every category is blocked (`RwaDefaultCategory`), never on the umbrella. It is applied once per model so a later reload cannot overwrite the user's pick, and it is also
what the Category selector's **Reset** restores: reset goes back to the scope the region opens on, not to
the scope the screen was opened with.

`UserCountryResolver` waits up to 3s for the start-up geo fetch — the synchronous read is an in-memory
flow rebuilt every process, so every launch races it — and falls back to the device region rather than
asking without a country.

**The categories endpoint is deliberately uncached.** Every `getCoinCategories` call goes to the
network — no `CacheRegistry` entry, no in-memory store, no persisted snapshot. The response carries the
region restrictions, and a cached copy would keep showing the restrictions of whatever country the app
last asked about. The cost is one request per caller: the tab, the Category selector and the search tab
each fetch their own.