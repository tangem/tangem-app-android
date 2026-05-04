# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
# Build debug APK (Google flavor)
./gradlew :app:assembleGoogleDebug

# Run all unit tests (debug/googleDebug variants + JVM modules)
./gradlew unitTest

# Run tests for a single module
./gradlew :features:wallet:impl:testDebugUnitTest    # Android library module
./gradlew :app:testGoogleDebugUnitTest                # App module
./gradlew :domain:tokens:test                         # Pure JVM module

# Run a single test class
./gradlew :core:ui:testDebugUnitTest --tests "com.tangem.core.ui.format.BigDecimalCryptoFormatTest"

# Detekt (static analysis) — runs automatically via convention plugin on applicable modules
./gradlew detekt detektMain

# Build UI tests APKs (for Marathon)
./gradlew :app:assembleGoogleMocked :app:assembleGoogleMockedAndroidTest
# 1. :app:assembleGoogleMocked — main APK (mocked build)
# 2. :app:assembleGoogleMockedAndroidTest — test APK with instrumented tests
```

**Product flavors:** `google` and `huawei` (dimension: `service`). Default development flavor is `google`.

**Build types:** `debug`, `mocked`, `internal`, `external`, `release`.

## Branching

See @.claude/rules/git-rules.md

## Architecture Overview

### Module Layers

The project is a heavily modularized Android app (~220 modules) organized in layers:

- **`app/`** — Application entry point, Hilt setup, navigation root
- **`domain/`** — Business logic and models. Each domain area (e.g., `tokens`, `wallets`, `card`) has a `models` submodule for pure data types and a core module for use cases
- **`data/`** — Repository implementations and data sources, mirrors domain structure
- **`features/`** — UI features using **API/Impl split pattern**: `features:foo:api` defines the public contract, `features:foo:impl` contains the implementation. This enforces clean dependency boundaries
- **`core/`** — Cross-cutting concerns: `ui`, `analytics`, `datasource`, `decompose`, `navigation`, `res`, `utils`, `security`, `pagination`
- **`common/`** — Shared models, routing, UI components, test utilities
- **`libs/`** — SDK wrappers: `blockchain-sdk`, `tangem-sdk-api`, `crypto`, `auth`, `visa`

### Component Architecture (Decompose)

The app uses [Decompose](https://github.com/arkivanov/Decompose) for lifecycle-aware components. Every feature screen follows this structure:

**API module** (`features/{name}/api/`):
- `{Name}Component` interface implementing `ComposableContentComponent`
- Inner `Params` data class for input parameters
- Inner `Factory` interface: `fun create(context: AppComponentContext, params: Params): {Name}Component`

**Impl module** (`features/{name}/impl/`):
- `Default{Name}Component` with `@AssistedInject` constructor taking `@Assisted appComponentContext: AppComponentContext` and `@Assisted params`
- Delegates `AppComponentContext by appComponentContext`
- Creates model via `getOrCreateModel(params)`
- `@Composable Content(modifier)` collects model state via `collectAsStateWithLifecycle()`
- Inner `@AssistedFactory` interface extending the public `Factory`

**Model** (`features/{name}/impl/.../model/`):
- `{Name}Model` extending `Model` base class, annotated `@ModelScoped`, uses `@Inject` constructor
- Receives params via `ParamsContainer.require<ParamsType>()`
- Exposes `StateFlow<{Name}UM>` (UM = UI Model, state class in `ui/state/` subpackage)
- Has `modelScope` (SupervisorJob + mainImmediate), auto-cancelled on destroy

**Child navigation within features:**
- `childStack()` — stacked screen navigation (back stack)
- `childSlot()` — optional overlays/bottom sheets (single or no child)
- `InnerRouter` — feature-internal navigation that delegates unknown routes to parent router

### Feature Package Conventions

- API package: `com.tangem.features.{name}.api` (plural `features`)
- Impl package: `com.tangem.feature.{name}.impl` (singular `feature` — legacy inconsistency, follow existing pattern per feature)
- Component: `{Name}Component` (api), `Default{Name}Component` (impl)
- Model: `{Name}Model` in `model/` subpackage
- UI State: `{Name}UM` in `ui/state/` subpackage
- UI Composable: in `ui/` subpackage

### Key Frameworks & Patterns

- **DI:** Hilt with `@SingletonComponent` scope and custom `@ModelScoped` scope for model-lifecycle dependencies
- **UI:** Jetpack Compose with Material3. Image loading via Coil
- **Navigation:** Custom `AppRouter` + `AppRoute` sealed classes with deep link support via `DeepLinkBuilder`
- **Networking:** Retrofit + Moshi for API communication
- **Local storage:** `AppPreferencesStore` for key-value pairs, `DataStore` for larger data
- **Async:** Kotlin Coroutines + Flow. Inject `CoroutineDispatcherProvider` (from `core/utils`) instead of using `Dispatchers.*` directly — provides `main`, `mainImmediate`, `io`, `default`, `single`
- **Error handling:** Arrow's `Either<Error, Success>` pattern throughout domain/data layers. `DataError` sealed hierarchy for domain errors. See `domain/core/CLAUDE.md` for the LCE pattern
- **Analytics:** `AnalyticsEvent(category, event, params)` in `core/analytics/models/`. Feature events are sealed class hierarchies extending `AnalyticsEvent`. Send via injected `AnalyticsEventHandler`
- **Feature toggles:** `FeatureTogglesManager` in `core/config-toggles/`. Toggles are defined in `core/config-toggles/src/main/assets/configs/feature_toggles_config.json` and auto-generated into a `FeatureToggles` enum by the convention plugin at build time. Each feature module exposes its own `XxxFeatureToggles` interface (in `api/`) with a `DefaultXxxFeatureToggles` implementation (in `impl/`) that delegates to `FeatureTogglesManager`
- **Supported languages:** `SupportedLanguages` in `core/utils/` defines the app's supported locales: en, ru, de, fr, it, ja, uk, zh, es. `getCurrentSupportedLanguageCode()` returns the device locale if supported, otherwise falls back to English. Used by API calls that accept a language parameter

### Build System

- **Gradle 8.14.1**, AGP 8.10.1, Kotlin 2.1.10
- **Version catalogs:** `gradle/dependencies.toml` (external/third-party dependencies) and `gradle/tangem_dependencies.toml` (in-house Tangem SDK dependencies)
- **Convention plugin:** `plugins/configuration/` — applies Detekt, configures test settings, generates environment configs and feature toggles
- **Custom Detekt rules:** `plugins/detekt-rules/`. Detekt configuration is in the `tangem-android-tools` git submodule. Key rule: `UnsafeStringResourceUsage` — prevents direct `stringResource()` / `pluralStringResource()` calls; use the `Safe`-suffixed variants instead
- **Localization:** Managed via [Lokalise](https://lokalise.com). Update strings by running `python3 lokalize.py`
- **GitHub Packages auth:** Requires `gpr.user` and `gpr.key` in `local.properties` for Tangem SDK dependencies

### Testing

- **JUnit 5** (Jupiter) for unit tests
- **MockK** for mocking
- **Turbine** for Flow testing
- **Truth** for assertions
- **Marathon** for UI tests (emulator-based, configured via `Marathonfile`)
- Shared test utilities in `common:test` and `test/core/`