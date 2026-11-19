# Feature Kit — how a feature plugs into the app

Applies to every new feature and to every change that makes one feature reach into another.
Long version with diagrams and numbers: [Notion — Feature Kit](https://app.notion.com/p/4075d34eb67881718e90db4a71d76993).

## The rule

A cross-cutting need gets **one mechanism owned by its host**: the host owns the contract, the fold
over contributions and their order. Features plug in from their own `impl` with one DI line and their
own logic. Consumers read the folded, generic result and never name a contributor. Dependencies point
one way: **contributor → host api, never host → contributor.**

## Default first

1. One module needs it → plain code in `features/x/impl`. No mechanism.
2. A second module needs it → expose one of the six kinds below from `features/x/api`.
3. A third contributor appears → make it a registry (set or map) owned by the host.

A mechanism counts only once the old path is deleted; a half-migrated one costs both. Migrate behind a
feature toggle, old and new side by side, and delete the old path together with the toggle.

## The six kinds a feature exposes

A feature is reached from outside **only through `features:x:api`**, and only through these kinds.
Contracts live in `api`; every rule, use case and DI binding lives in `impl`.

| # | Kind | Contract | Registration |
|---|---|---|---|
| 1 | Main entry | `XComponent` + `Factory : ComponentFactory<Params, XComponent>` | `@Binds` in x's impl DI module |
| 2 | Deep link | `DeepLinkHandler`, key const in **x's api** | `@Binds @IntoMap @DeepLinkKey(KEY)` |
| 3a | Feed tab | `FeedTabContributor` | `@Binds @IntoSet` + id in `FeedTabsOrder` |
| 3b | Feed top block | `FeedTopBlockContributor` ¹ | `@Binds @IntoSet` |
| 4 | MCP intent | `McpIntentHandler` ¹ | `@Binds @IntoSet` |
| 5 | Embedded block / list item | stateful `ComposableContentComponent` / `ComposableListContentComponent<T>`; stateless `@Composable` + `XUM` in x's api | `ComponentFactory` / none |
| 6 | Headless model | `XSupplier : FlowSupplier<Params, XInfo>` — pure Kotlin, decision already made, batched per wallet | `@Binds` |

¹ Contract not in code yet. The first feature that needs it adds the contract to the host once — in the
host's `api`, with `@Multibinds` and an order list (like `FeedTabsOrder`) in the host's `impl`, agreed
with and reviewed by the host's owners — instead of appending itself to a hard-coded host list. After
that one change, contributors never edit the host again.

## Dependency matrix

| May depend on → | own api | own domain/data | other api | other impl / domain / data | foundation |
|---|---|---|---|---|---|
| feature impl | yes | yes | yes | **no** | yes |
| feature api | — | **no** | **no** | **no** | yes |
| feature domain / data | **no** | yes | **no** | **no** | yes |
| foundation (`core`, `common`, `domain/models`…) | — | — | **no** | **no** | yes |
| `:app` | yes | yes | yes | yes | yes |

## Pick the shape

| You need to… | Shape | Contract lives in | Contributor does | Host does |
|---|---|---|---|---|
| show your thing in someone's screen or list | Registry (set) · `XContributor` | host api | `@Binds @IntoSet` in its impl | `@Multibinds Set`, folds, owns the order list |
| handle a key / path / event type | Registry (map) · `XHandler` | host api | `@Binds @IntoMap @XKey` | `@Multibinds Map`, lookup, key parity test |
| embed a self-contained piece of UI + logic | Component · `Factory : ComponentFactory<Params, X>` | owner api | binds the factory in its impl | passes `Params` (handles + `Settings` + `Callback`), renders |
| answer a question about your feature (no UI) | Supplier · `XSupplier`; availability = `XAvailabilitySupplier` | owner api | `@Binds` in its impl | reads the answer; never reads `XFeatureToggles` / `ABTestsManager` |
| react when the app does X (wallet deleted, push, start) | Event handler · `XHandler` set | host api or foundation (e.g. `domain/common`) | `@Binds @IntoSet` | dispatches to all, isolates failures |

## Must-dos per shape

**Registry (set)** — precedent `FeedTabContributor` (`features/feed/api/.../nav/FeedTabContributor.kt`)
- Hilt sets are unordered: add your id to the host's order list (`FeedTabsOrder` in `feed:impl`). A missing id is logged and skipped.
- `isAvailable` is a getter over your **own** feature's toggle or availability supplier, never a stored `val` — or the tester-menu toggle does nothing.
- Every feed tab is *created* when the feed opens: keep `isAvailable` cheap, load data on first visibility.
- Host side: `@Multibinds` so an empty set resolves; inject `Set<@JvmSuppressWildcards X>`.
- Every `Model` needs its binding, or `getOrCreateModel` throws at runtime:

```kotlin
@Module @InstallIn(SingletonComponent::class)
internal interface XFeedTabModule {
    @Binds @IntoSet fun bindTab(impl: XFeedTabContributor): FeedTabContributor
}

@Module @InstallIn(ModelComponent::class)
internal interface XModelModule {
    @Binds @IntoMap @ClassKey(XFeedTabModel::class) fun bindModel(model: XFeedTabModel): Model
}
```

**Registry (map)** — precedents `DeepLinkHandler` + `@DeepLinkKey` (`common/routing/.../deeplink/`), `FeedScreenFactory`
- The key is a `const` in the owner's api — one source of truth.
- Gate with `isEnabled` and `isOpenableFromPush` on the handler, never in a central policy file.
- Implement with a class or the `DeepLinkHandler(isEnabled = …) { … }` factory; `handle` opens the feature's own Kind 1 entry (its route), parsing query params itself.
- A duplicate key fails only when `:app` compiles; keys that collide after normalization fail at activity start — cover normalized keys in the parity test.
- A new `@MapKey` type is a host decision, not a contributor's.

**Component** — precedent `PortfolioSelectorComponent` (`features/common-features/api/.../portfolioselector/`)
- One job. Data comes in through host-supplied handles or suppliers the component owns; policy through `Settings`; events out through a **per-instance** `Callback`.
- It writes only its own job's state. Never a `@Singleton` event bus between features.
- The host creates it with `child(key = "…")`, never its own context (see `codestyle/decompose-child-context.md`).
- Split by job, never by layer. The owner builds its UM — the host never does.

**Supplier** — base `FlowSupplier` (`domain/core/.../flow/`)
- The one place where the toggle, the wallet predicate and the A/B variant meet. Consumers inject the supplier only.
- Expose a `Flow`/`StateFlow` or a getter — never a stored `val`.
- Don't copy the nearest `FlowCachingSupplier` (5 files); use one only when the answer is fetched data cached per key.

**Event handler** — precedent `UserWalletDataCleaner` (`domain/common/.../wallets/`, folded by `DeleteWalletUseCase`)
- The dispatcher has no `when`. Each handler runs in its own `runSuspendCatching`, so one failure doesn't cancel the rest.
- A broadcast set has no order; if something must run first, the host owns an explicit order list.
- A deliberate no-op in a multi-method contract gets a one-line *why* comment.

## Never

- Re-derive another feature's rule in a host ("can buy", "earn rate", "is enabled"). Ask its Kind 6 supplier; if none exists, add one **to that feature**.
- Build another feature's UM in a host converter, or put another feature's UI in your parent screen.
- Read another feature's `XFeatureToggles`, or assemble its availability from raw data at an entry point.
- Put feature UI in `core:ui` / `common:ui`, or feature analytics in `core/analytics/models`.
- Depend on another feature's `domain:x`, `data:x` or `impl`, or `api(...)`-re-export them through a third module.
- Add feature DI to `:app/tap/di`, a legacy `XDeepLinkHandler.Factory`, or a `PushDeeplinkPolicy` entry.
- Add a `when(type)` branch to a host for a new contributor. If the host has no contract, add one.

## New-feature checklist (analysis stage)

- [ ] Decided whether the feature is pluggable (or will be), and which of the six kinds it exposes.
- [ ] For every other feature it touches: found its contract, or agreed with the owning team to add one there.
- [ ] Planned the changes needed in someone else's feature as that feature's work, not as host edits.
- [ ] One component = one job. Sub-functionality that some scenarios don't use is its own (internal) component.
- [ ] Clients configure the feature through `Params`/`Settings`, not through flags the feature grows per client.
- [ ] Known business plans for the feature are reflected in the contract (what will vary, who will plug in).
- [ ] Every new `Model` has its `@ClassKey` binding; every dependency fits the matrix above.

## Improving existing features (Claude and reviewers)

When a change touches a host that shows an anti-shape — a growing `when(type)`, a host-built foreign UM,
a read of another feature's toggles, a `@Singleton` cross-feature bus, a copied rule or predicate, a new
contributor that forces a host edit — **suggest the kit shape in one line**: name the shape and the
precedent (`"Kind 6 supplier in polymarket:api would replace this toggle read — cf. PortfolioSelector"`).

- Don't refactor unless asked; never mix the migration into an unrelated diff.
- In a review this is a `nit:` unless the change itself adds a new instance of the anti-shape — then it is a finding.
- When asked to migrate: toggle-gated, old path kept until the toggle is removed.