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

## API Integration Guide

### Config Structure

- `ApiConfig` — base API config with `id` (`ApiConfig.ID`), `defaultEnvironment` (`ApiEnvironment`), and `environmentConfigs` (list of `ApiEnvironmentConfig`)
- `ApiEnvironmentConfig` — per-environment settings: `environment`, `baseUrl`, and `headers` (map of header name to `Provider<String>`)

### Config Management

- `ApiConfigsManager` — DI-available component for accessing configs via `getEnvironmentConfig(id: ApiConfig.ID): ApiEnvironmentConfig`
- Two implementations: `ProdApiConfigsManager` (release) and `DevApiConfigsManager` (extends `MutableApiConfigsManager`, used when `BuildConfig.TESTER_MENU_ENABLED`)
- `MutableApiConfigsManager` allows runtime environment switching via Tester Menu without app restart

### Creating a Retrofit API — two ways

There are **two** ways to expose a new Retrofit API. Both build the service through
`RetrofitFactory.build(RetrofitApiSpec(apiConfigId = <ID>, …))` — they differ only in **which config
id** the API rides:

1. **Reuse an existing config by its `ID`.** If the API lives on a gateway that already has a config
   (most commonly the main Tangem gateway → `TangemTech.ID`), do **not** create a new config — just
   reference the existing id. Example: `AddressBookApi`, `TangemTechMarketsApi`, `JointAccountApi`
   and the Polymarket discovery API all provide their service with `apiConfigId = TangemTech.ID`.
   The shared `TangemTech.ID` is exposed cross-stream via `store:datasource:api`.

2. **Create a new `ApiConfig` implementation.** Only when the API has its own base URL / headers /
   environments. Two flavours:
   - **New base URL** → extend `ApiConfig()` directly and declare `environmentConfigs` with explicit
     `baseUrl`s (e.g. `MoonPay`, `PolymarketWeb`, `BlockAid`).
   - **Same Tangem gateway host, but a distinct config** (own headers/auth, gateway URLs) → extend
     **`TangemGatewayApiConfig`** (`core:remote`) instead of hardcoding the gateway URLs — its
     `gatewayEnvironment(environment, headers)` helper fills in the correct
     `api.tangem.org` / `api.tests-d.com` / … base URL per environment. Examples: `News`, `Auth`.

### Adding a New API config

Place the config in **its owning stream's datasource module** (`grow:datasource`, `store:datasource`,
`spend:datasource`, `libs:auth`, … — not `core:datasource-impl`, which now only keeps the shared
`TangemTech` config), in that module's `config` package:

1. Create the `ApiConfig` (or `TangemGatewayApiConfig`) subclass — override `defaultEnvironment` and
   `environmentConfigs`; declare `KEY` + `val ID = ApiConfig.ID(KEY)` in its companion. DI
   dependencies (env-config slice, header/auth providers, `AppInfoProvider`) go through the constructor.
2. Register it in that module's Hilt module with `@Provides @IntoMap @StringKey(<Config>.KEY)`
   returning `ApiConfig`.
3. Provide the Retrofit service in the same module with
   `RetrofitFactory.build(RetrofitApiSpec(apiConfigId = <Config>.ID, …))`.

### Testing — mandatory per-config test

**Every `ApiConfig` implementation MUST be covered by its own unit test**, placed next to the config
in its owning module (there is no central config test — the old `ProdApiConfigsManagerTest` was
removed in favour of per-config tests). The test asserts:

- `config.id` equals the config's `ID`;
- `config.environmentConfigs` maps each `ApiEnvironment` to its exact `baseUrl`
  (`.associate { it.environment to it.baseUrl }` + Truth `containsExactlyEntriesIn`);
- for a config that produces headers — a representative environment's headers, asserting **exact**
  values for deterministic headers and **presence only** for machine/locale-derived ones (e.g.
  `timezone` from `TimeZone.getDefault()`), so the test stays stable across CI machines.

Mock constructor deps (env-config slice, header/auth providers, `AppInfoProvider`). If the config
builds its headers **eagerly** at construction (its `environmentConfigs` is an eager `val` that reads
the providers), construct the config **inside `@BeforeEach` after stubbing** the mocks — not as a
field initializer. Reference: `TangemTechConfigTest`.