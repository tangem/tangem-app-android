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
- **`ModelModule`** — `RwaFeedTabModel` and `CategorySelectorModel` into `ModelComponent`.
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
       ├─ RwaFeaturedAssetsManager    → RwaFeaturedUM   (rail; GetTopFiveMarketTokenUseCase, 5 items)
       └─ RwaBatchFlowManager         → RwaListUM       (paginated list)
            └─ BatchListStateManager  → RwaListItemConverter → RwaListItemUM
```

`RwaFeedTabModel` also implements `CategorySelectorComponent.ModelCallback`, so it is both the tab's
state holder and the receiver of the category-selector result.

## Category resolution

Nothing renders until categories are fetched, because two category **codes** are reserved rather than
offered as filters (`ReservedCategoryCodes`):

- `featured_assets` — backs the Featured rail; its id is passed to `RwaFeaturedAssetsManager.load()`.
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
A missing code degrades rather than throws: no `featured_assets` → `RwaFeaturedUM.Empty` (rail hidden);
no `real-world-assets` → the list renders `RwaListUM.Error`.

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

This is the only place in the feed family that builds its UM with a real
`com.tangem.utils.transformer.Transformer` plus `Converter`s. Extend that pattern here rather than the
inline `uiState.update { it.copy(...) }` style `RwaFeedTabModel` uses.

## Request flow

A single `RwaListRequest(interval, sort, selection)` describes one list request. How a change is
applied is deliberately not uniform:

- **Sort change** — always reloads. Sorting *is* the query (`TokenMarketListConfig.Order`).
- **Category or sector change** — always reloads.
- **Interval change under `RwaSort.MarketCap`** — no reload. `Order.ByRating` does not depend on the
  interval, so the loaded rows stay correct: re-render them and fetch charts for the new interval.
- **Interval change under gainers/losers** — reloads; those orders are *ranked by* the interval, so the
  row set itself changes.

## Gotchas

**Everything under `impl/.../model/list/` has a twin in `features/feed/crypto/impl`.** This module was
created by cloning the Crypto (Market Pulse) tab. `BatchListStateManager` is byte-identical to
`crypto/impl/.../model/list/BatchListStateManager.kt` — same md5 below the `package` line — and
`RwaBatchFlowManager` / `RwaListItemConverter` / `RwaTypes` are near-copies of the `MarketPulse*` files.
A fix here almost always belongs there too — extracting a shared kernel is still open.

**`RwaBatchFlowManager.request` is a `StateFlow` that is never collected.** It is only `.value`-read
inside `reload()`. Writing to `RwaFeedTabModel.listRequest` therefore does nothing on its own — the
model must call `reload()` afterwards. Don't add a filter field and assume the flow will pick it up.

**The reserved category codes are backend contract strings, and they disagree on convention.**
`featured_assets` is snake_case, `real-world-assets` is kebab-case (TWI-1477). Neither is derived from
the other, and a rename on either side silently empties the rail or the whole list with no error.

**Nothing on the feed stack is serialized.** Every v2 navigation call passes `serializer = null`
(`DefaultFeedV2Component.kt`, `FeedHomeComponent.kt`), so feed state does not survive process death and
`@Serializable` on a `FeedRoute` is currently inert. Don't rely on restoration when adding a screen,
and treat the "routes are pure data" rule in `features/feed/README.md` as unenforced by the compiler
(TWI-1608).

being destroyed, so holding scroll position in the component is what makes it survive tab switches.

**Category and sector names still come from the backend `displayName`.** Localized keys exist
(`markets_real_assets_category_*`, `markets_real_assets_sector_xstocks_*`) and are meant to be resolved
from the category `code` with `displayName` as the fallback. Categories can already do that; sectors
cannot — `CoinCategory.Sector` carries no `code`, so those keys stay unused until the data layer adds
one. DS3 component rules are in `.claude/rules/codestyle/design-system.md`.