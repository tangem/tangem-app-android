# :features:feed:api — feed contracts

Contracts of the v2 feed (Shtorka 2.0). A feature plugs into the
feed from **its own Gradle module** — the feed host (`:features:feed:impl`) needs **zero code
edits**. Reference plug-ins: `:features:feed:crypto:impl` (tab), `:features:feed:search:impl`
(screen).

## Add a full-screen feed destination (route + screen)

1. Depend on `:features:feed:api` (the plug-in contracts live in the `com.tangem.features.feed.nav` package).
2. Declare the route — pure data (`@Serializable` data class/object, no lambdas), implementing
   `FeedRoute`. Put it in your **api** module if other features navigate to it; keep it internal
   otherwise.
3. Declare your component interface in your **api** module extending `FeedScreenComponent`, with a
   `Factory : ComponentFactory<YourRoute, YourComponent>` (standard api/impl split — the component
   stays reusable outside the feed). Implement it as `Default{Name}Component` (`Content` is
   mandatory; override `Header` with a `TangemTopNavigation` — pass
   `fadeEnabled = false, blurBackground = false`, the sheet header is an opaque capsule). Model
   gets the feed `Router` injected: push `FeedRoute`s for feed destinations, `AppRoute`s for
   global ones, `router.pop()` for back.
4. Bind an adapter into the screen registry:

   ```kotlin
   @Module
   @InstallIn(SingletonComponent::class)
   internal interface FooScreenModule {
       @Binds
       @IntoMap
       @ClassKey(FooFeedRoute::class)
       fun bindFooScreenFactory(impl: FooScreenFactory): FeedScreenFactory
   }
   ```

5. Register your models in `ModelComponent` (`@Binds @IntoMap @ClassKey(...): Model`).
6. Add `implementation(projects.features.yourModule.impl)` to `app/build.gradle.kts`
   (Hilt aggregation — the only line outside your module).

## Add a home tab

Steps 1, 5, 6 as above, plus: implement `FeedTabComponent` (owns *scrolling*, not navigation — no
header, no own full-screen stacks; hold scroll state like `LazyListState` in the component, pages
stay CREATED while unselected) and bind a `FeedTabContributor` with `@Binds/@Provides @IntoSet`.

Tab **order** is controlled in one place: add your `FeedTabId` constant (companion of `FeedTabId`)
to `FeedTabsOrder` in `:features:feed:impl`. A contributed tab missing from that list is skipped
with an error log.

## Current limitations (build out with the first consumer)

- Cross-module sheets over the feed — no route support; feature-internal bottom sheets keep using
  their own `childSlot`, as everywhere in the app.
- Deep-link entry (`FeedEntryRoute` → initial feed stack mappers) — not wired; v2 currently opens
  only inside the wallet shtorka.
- Remote tab ordering/availability — contributors are static; `FeedTabContributor.isAvailable` is
  read once, synchronously.