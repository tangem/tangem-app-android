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