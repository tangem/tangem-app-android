# core/local

**Contract module for local storage.** It declares the interfaces, extension points and lightweight
utilities for on-device persistence and in-memory runtime state. The Android/runtime implementations
live in **`core:datasource-impl`** and are provided through DI — this module compiles no Android
component of its own.

Pure-JVM leaf (`kotlin.jvm` + `configuration`); depends only on `core:utils` and third-party libraries.
Owned by the core Android team.

## What lives here

- **Preferences** — `AppPreferencesStore` (key/value over DataStore) with its `utils` extensions and the
  key-migration contracts (`CleanupKeyMigration`, `SharedPreferencesKeyMigrationFactory`).
- **Logs** — `AppLogsStore`: append/read/zip the exportable app-log file.
- **Files & assets** — `FileReader`, `AssetLoader` (load bundled JSON assets by `KType`/`Type`).
- **Runtime state** — in-memory stores `RuntimeStateStore`, `RuntimeSharedStore`,
  `RuntimeSharedMapStore` (+ `KotlinxDataStoreSerializer`).
- **DataStore utilities** — `AppDataStoreFactory`, `MoshiDataStoreSerializer`, `MoshiTypesExt`,
  `DataStoreExt`.
- **Connectivity** — `NetworkConnectionManager`.
- **Moshi** — the `@SdkMoshi` qualifier and `SdkMoshiConfigurer` extension point.

## Rules

- **Contracts only.** Interfaces, extension points and pure helpers — no Android component
  implementations (those go to `core:datasource-impl`, bound via Hilt).
- **No product or stream types.** Nothing owned by a feature or by a product stream
  (`grow` / `store` / `spend`) belongs here; keep those in the owning module.
- **Leaf discipline.** As a `core:*` module it may depend only on other `core:*` modules and
  third-party libraries — never on `data:*`, `features:*`, `domain:*` or a stream module.
- The legacy package `com.tangem.datasource.local.*` is retained for some types (e.g.
  `AppPreferencesStore`) so consumers need no import change after they moved here; new types use
  `com.tangem.core.local.*`.