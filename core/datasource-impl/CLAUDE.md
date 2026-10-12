# core/datasource-impl

**Implementation module for `core:local` and `core:remote`.** It holds the concrete implementations of
the contracts those two modules declare — and nothing else.

## Role & boundaries

- `core:local` and `core:remote` are the **contract (API) modules** — interfaces, DTO/config types and
  the extension points other modules compile against (`AppPreferencesStore`, `AppLogsStore`,
  `FileReader`, `AppDataStoreFactory`, `RetrofitFactory`, `ApiResponse`, `NetworkMoshiConfigurer`, …).
- `core:datasource-impl` provides the **Android/runtime implementations** of those contracts
  (`DefaultAppPreferencesStore`, `DefaultAppLogsStore`, the `ApiConfigsManager` impls, interceptors,
  DataStore / SharedPreferences wiring, the corruption/migration plumbing) plus the Hilt `@Module`s that
  bind them. Implementations are **`internal`**; only the DI bindings leave the module.

### Only `:app` may depend on it

`core:datasource-impl` is wired in **exclusively by `:app`**, the way a `features:*:impl` module is:
`app` declares `implementation(projects.core.datasourceImpl)` so Hilt discovers its bindings at the
composition root. **No other module may depend on it** — every other module compiles against the
`core:local` / `core:remote` contracts and receives an implementation through DI at runtime.

This is enforced by the **`verifyDatasourceImplIsolation`** Gradle task, which fails the build if any
project other than `:app` declares a dependency on `:core:datasource-impl`.

### No product or stream components

This module must contain **only** cross-cutting local/remote infrastructure. It must **not** hold:

- product- or feature-specific data sources, repositories, DTOs or stores;
- anything owned by a product stream (`grow` / `store` / `spend`). Stream-owned data sources, API
  configs and stores live in that stream's own `*/datasource` module (`grow:datasource`,
  `store:datasource`, `spend:datasource`, `libs:auth`) — never here.

The only config that legitimately stays here is the shared `TangemTech` gateway config (exposed
cross-stream via `store:datasource:api`).