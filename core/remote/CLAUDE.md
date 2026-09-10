# core/remote

**Contract module for networking.** It declares the interfaces, config model and extension points for
talking to backends over Retrofit. The Android/runtime implementations (Retrofit factory,
`ApiConfigsManager` impls, interceptors) live in **`core:datasource-impl`** and are provided through DI
— this module builds no networking runtime of its own.

Pure-JVM leaf (`kotlin.jvm` + `kotlin.serialization` + `configuration`); depends only on `core:utils`
and third-party libraries. Owned by the core Android team.

## What lives here

- **Retrofit** — `RetrofitFactory` (builds a service from a `RetrofitApiSpec`) and `TimeoutAnnotations`.
- **API config model** — `ApiConfig` (+ `ApiConfig.ID`), `ApiEnvironment`, `ApiEnvironmentConfig`,
  `TangemGatewayApiConfig`, and the `ApiConfigsManager` / `MutableApiConfigsManager` contracts.
- **Responses** — `ApiResponse` (+ `ApiResponseError`, `ApiResponseHeaders`) and the `ApiResponseExt`
  helpers (`getOrThrow` / `fold` / `map` / `isNetworkError`).
- **Headers & auth** — `RequestHeader`, `CardAuthHeaderProvider`, `TangemApiKeyHeaderProvider`,
  `SessionAuthQualifiers`.
- **Moshi** — the `@NetworkMoshi` qualifier, the `NetworkMoshiConfigurer` `@IntoSet` extension point,
  and `UnknownEnumMoshiAdapter`.

## Rules

- **Contracts only.** Interfaces, config types, extension points and pure helpers — no Retrofit/OkHttp
  runtime wiring (that goes to `core:datasource-impl`, bound via Hilt).
- **No product or stream types.** Concrete `ApiConfig` subclasses for a specific backend, stream-owned
  APIs and DTOs belong in the owning stream's `*/datasource` module (`grow:datasource`,
  `store:datasource`, `spend:datasource`, `libs:auth`), not here. Only the shared cross-cutting
  networking contract lives in `core:remote`.
- **Leaf discipline.** As a `core:*` module it may depend only on other `core:*` modules and
  third-party libraries — never on `data:*`, `features:*`, `domain:*` or a stream module.
- The legacy package `com.tangem.datasource.api.common.response.*` is retained for `ApiResponseExt` so
  consumers need no import change; new types use `com.tangem.core.remote.*`.

## Adding a Retrofit API / config

This is the guide for wiring a new backend call on top of the contracts above. The types you use
(`ApiConfig`, `RetrofitFactory`, `TangemGatewayApiConfig`, `ApiConfigsManager`) live here; the concrete
runtime (Prod/Dev/Mock `ApiConfigsManager` impls, interceptors) is implemented in `core:datasource-impl`
and wired by `:app`.

### Config structure

- `ApiConfig` — base API config with `id` (`ApiConfig.ID`), `defaultEnvironment` (`ApiEnvironment`), and `environmentConfigs` (list of `ApiEnvironmentConfig`).
- `ApiEnvironmentConfig` — per-environment settings: `environment`, `baseUrl`, and `headers` (map of header name to `Provider<String>`).
- `ApiConfigsManager` — DI-available component for accessing configs via `getEnvironmentConfig(id: ApiConfig.ID): ApiEnvironmentConfig`. Two impls in `core:datasource-impl`: `ProdApiConfigsManager` (release) and `DevApiConfigsManager` (extends `MutableApiConfigsManager`, used when `BuildConfig.TESTER_MENU_ENABLED` — allows runtime environment switching via the Tester Menu without an app restart).

### Creating a Retrofit API — two ways

Both build the service through `RetrofitFactory.build(RetrofitApiSpec(apiConfigId = <ID>, …))`; they
differ only in **which config id** the API rides:

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
     **`TangemGatewayApiConfig`** instead of hardcoding the gateway URLs — its
     `gatewayEnvironment(environment, headers)` helper fills in the correct
     `api.tangem.org` / `api.tests-d.com` / … base URL per environment. Examples: `News`, `Auth`.

### Where a new config goes

Place the config in **its owning stream's datasource module** (`grow:datasource`, `store:datasource`,
`spend:datasource`, `libs:auth`, …), in that module's `config` package — **never in
`core:datasource-impl`** (it holds no product/stream configs; only the shared `TangemTech` config stays
central):

1. Create the `ApiConfig` (or `TangemGatewayApiConfig`) subclass — override `defaultEnvironment` and
   `environmentConfigs`; declare `KEY` + `val ID = ApiConfig.ID(KEY)` in its companion. DI
   dependencies (env-config slice, header/auth providers, `AppInfoProvider`) go through the constructor.
2. Register it in that module's Hilt module with `@Provides @IntoMap @StringKey(<Config>.KEY)`
   returning `ApiConfig`.
3. Provide the Retrofit service in the same module with
   `RetrofitFactory.build(RetrofitApiSpec(apiConfigId = <Config>.ID, …))`.

### Testing — mandatory per-config test

**Every `ApiConfig` implementation MUST be covered by its own unit test**, placed next to the config
in its owning module (there is no central config test). The test asserts:

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