# core/config-toggles

Feature toggles (and the related excluded-blockchains toggles). Toggles gate
features by app version; the JSON config is the source of truth and the
`FeatureToggles` enum is generated from it at build time.

## How it works

- **Config:** `src/main/assets/configs/feature_toggles_config.json` — a JSON array
  of `{ "name": <STRING>, "version": <STRING> }` (`ConfigToggle`).
- The **convention plugin** generates the `FeatureToggles` enum (one entry per
  `name`) at build time. Reference it as `FeatureToggles.<NAME>`.
- **Entry point:** `FeatureTogglesManager.isFeatureEnabled(FeatureToggles.X)`.
  - `ProdFeatureTogglesManager` (release): a toggle is enabled when the app
    version `>=` its `version`.
  - `DevFeatureTogglesManager` (tester builds, `BuildConfig.TESTER_MENU_ENABLED`):
    runtime-toggleable via the Tester Menu.
- **`version` semantics:**
  - `"undefined"` (`DISABLED_FEATURE_TOGGLE_VERSION`) → OFF in prod; can only be
    flipped ON via the Tester Menu / dev builds. Use this while a feature is in
    development.
  - `"X.Y"` (e.g. `5.40`) → ON in prod from that app version onward
    (`currentVersion >= localVersion`, see `VersionAvailabilityContract`).

## Naming convention (ENFORCED by a test)

- A toggle `name` MUST match `^(AND|TWI)_\d+(?:_[A-Z0-9]+)+$` — start with the
  Jira ticket id (`AND_<id>` for Android tickets, `TWI_<id>` for idea tickets),
  then an `UPPER_SNAKE_CASE` suffix. Example: `AND_15901_STORIES_CONTAINER_ENABLED`.
- Enforced by `FeatureTogglesNamingConventionTest`. Legacy toggles that predate
  the rule are whitelisted in its `EXCLUDED_TOGGLES_LIST` — do **not** add new
  names there without an explicit reason.
- The Kotlin interface property stays human-readable **without** the ticket id:
  `isStoriesContainerEnabled`.

## Per-feature toggles & how to add one

Each feature owns its toggles — feature code reads them through its own
interface, never `FeatureTogglesManager` directly:

- `api/`: `XxxFeatureToggles` interface — `val isYyyEnabled: Boolean`.
- `impl/`: `DefaultXxxFeatureToggles(featureTogglesManager)` exposes each toggle as
  a **getter-backed property**, not a stored value — so it is re-evaluated on every
  read (required for runtime toggling via the Tester Menu):

  ```kotlin
  override val isYyyEnabled: Boolean
      get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.AND_<id>_YYY)
  ```

  Never `val isYyyEnabled = featureTogglesManager.isFeatureEnabled(...)` (evaluated
  once at construction).
- DI: a `@Provides @Singleton` in the feature's Hilt module returning the interface.

To add a toggle:

1. Add `{ "name": "AND_<id>_FOO_ENABLED", "version": "undefined" }` to the config
   JSON (the enum is regenerated at build).
2. Add `val isFooEnabled` to the feature's `XxxFeatureToggles` and map it in
   `DefaultXxxFeatureToggles` (create the interface/impl/DI provider if the
   feature has none yet).
3. Gate code on `xxxFeatureToggles.isFooEnabled`.

## Removing (cleanup)

When a toggle ships at 100%, set its `version` to the release and run the
`cleanup-feature-toggles` skill — it removes the JSON entry, the interface/impl
members, inlines `true`, and drops dead branches. Mark code that must be deleted
together with a toggle using `@RemoveWithToggle("AND_<id>_FOO_ENABLED")`
(`com.tangem.utils.annotations.RemoveWithToggle`); the cleanup skill picks it up.