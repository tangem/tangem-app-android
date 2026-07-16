# Unit Testing Rules

This document covers **unit tests** only — sources under `src/test`, running on the JVM via JUnit 5 (Jupiter). UI / instrumentation tests (`src/androidTest`, Kaspresso + Espresso on the JUnit 4 on-device runner) are a separate concern and out of scope here.

## Stack

| Purpose | Library | Version source |
|---|---|---|
| Test runner | JUnit 5 (Jupiter) | `deps.test.junit5` |
| Mocking | MockK | `deps.test.mockk` |
| Flow testing | Turbine | `deps.test.turbine` |
| Assertions | Google Truth | `deps.test.truth` |
| Coroutines | `kotlinx-coroutines-test` | `deps.test.coroutine` |

All unit tests run on JUnit 5. JUnit 4 (`deps.test.junit` = `junit:junit`) is **not** used in `src/test` at all — it survives only in `src/androidTest` instrumentation. Don't add new JUnit 4 unit tests.

Versions live in `gradle/dependencies.toml`. Do not hardcode library coordinates in module build scripts — always go through the catalog.

## Shared test modules

Depend on these via `testImplementation(projects.*)` — never copy their utilities inline.

Build test fixtures with **factory functions that default every argument** (`createXxx(id = 1, name = "Cat", … )`) rather than calling bloated constructors at each call site. A test then overrides only the fields relevant to it, so the intent stays visible and adding a model field doesn't churn every test. This is the idiom behind the `Mock*Factory` classes below — extend them instead of hand-rolling fixtures.

### `:test:core` (pure JVM)
`test/core/src/main/java/com/tangem/test/core/`. Re-exports as `api`: `test.coroutine`, `test.junit5`, `test.mockk`, `test.truth`, `test.turbine`. Use it as the one-line entry point to pull the whole unit-testing stack into a JVM module. Depends on `domain:core` and `arrow.core` (so its utilities can reference domain abstractions like `FlowProducer`).

Utilities:
- `TestCoroutineExt.getEmittedValues(flow)` — collect a `Flow` into a `List` from a `TestScope`.
- `TestFlowProducerTools(scope, dispatcher)` — test double for `FlowProducerTools` that mirrors production `DefaultFlowProducerTools` (retryWhen + fallback + `distinctUntilChanged` + `shareIn`) on a caller-provided test scope/dispatcher, without analytics/logging. Pass `TestScope.backgroundScope` + a dispatcher built from `testScheduler` so the 2s retry delay is virtual-time-controllable. Use it for `FlowProducer` tests instead of mocking `FlowProducerTools`.
- `@ProvideTestModels` — meta-annotation over JUnit 5 `@MethodSource("provideTestModels")` for parameterized tests.
- `TruthArrowExt` — `assertEither`, `assertEitherRight`, `assertEitherLeft`, `assertSome`, `assertNone` for Arrow types.

### `:common:test` (Android library — legacy, being retired)
`common/test/src/main/java/com/tangem/common/test/`. Factories and fakes for domain/data models. Being phased out in favour of `:test:core` (JVM mechanisms) and `:test:mock` (mock factories); don't add new utilities here.

- `TestAppCoroutineScope(testScope)` — test implementation of `AppCoroutineScope`.
- `MockStateDataStore` — in-memory `DataStore` for tests.
- `Mock*Factory` classes for `CryptoCurrency`, `UserWallet`, `NetworkStatus`, `ScanResponse`, `YieldDTO`, `QuoteResponse`, `UpdateWalletManagerResult` etc.

### `:test:mock`
`test/mock/`. Mock data for models not yet covered elsewhere (currently `MockAccounts`). Add to this module rather than creating new ad-hoc mock files.

## Dispatchers

Never use `Dispatchers.Main`/`IO`/`Default` directly in production code — always inject `CoroutineDispatcherProvider` from `core/utils`.

In tests, override with `TestingCoroutineDispatcherProvider` (defined in `core/utils/src/main/java/com/tangem/utils/coroutines/CoroutineDispatcherProvider.kt`). By default `main`/`mainImmediate`/`io`/`default` are `Dispatchers.Unconfined`, while `single` is a single-thread `Executors.newFixedThreadPool(1)` dispatcher.

For `Model`-layer tests inside features, construct it with a single `StandardTestDispatcher(testScheduler)` for all five roles (built from the enclosing `TestScope`) so `advanceUntilIdle()` controls execution. See any `features/*/impl` model test for the `TestScope.createTestingCoroutineDispatcherProvider()` helper.

## Naming & placement

- **Test class**: `FooTest` (singular noun). Not `FooSpec`, not `FooBehavior`, not `FooTests`.
- **Test method**: backtick-quoted sentence that **must** follow `GIVEN … WHEN … THEN …` (uppercase). The name states the behaviour under test — precondition, action, expected outcome — not the implementation.
  ```kotlin
  @Test
  fun `GIVEN currency status emitted WHEN model created THEN analytics sent`() = runTest { … }
  ```
  A part may collapse when trivial (e.g. `GIVEN no wallets WHEN load THEN returns empty`), but all three keywords stay present.
- **Test body**: if the body is more than a one-liner (i.e. has distinct setup / action / check phases), it **must** be marked with `// Arrange`, `// Act`, `// Assert` comments. GWT names the behaviour from the outside; AAA structures the code inside.
- **Location**: mirrored packages under `src/test/kotlin/`. No `src/testFixtures/` — shared helpers go to the modules above.

## Unit-test skeleton (JUnit 5)

```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class FooTest {

    private val barUseCase: BarUseCase = mockk()
    private val dispatchers = TestingCoroutineDispatcherProvider()

    private val foo = Foo(barUseCase, dispatchers)

    @BeforeEach
    fun resetMocks() {
        clearMocks(barUseCase)
    }

    @Test
    fun `GIVEN bar returns right WHEN invoke THEN emits value`() = runTest {
        // Arrange
        coEvery { barUseCase(any()) } returns Either.Right(expected)

        // Act
        val actual = foo.invoke(input)

        // Assert
        assertThat(actual).isEqualTo(expected)
        coVerify(exactly = 1) { barUseCase(input) }
    }
}
```

- `@TestInstance(Lifecycle.PER_CLASS)` is **opt-in per class, not the project default.** Add it only where you need a non-static `@MethodSource`/`provideTestModels` provider or want to share expensive setup across methods (~half of test classes do). The JUnit default stays `PER_METHOD` (a fresh instance per test). Beware: `PER_CLASS` reuses one instance across all methods, so mutable fields leak between tests — reset them in `@BeforeEach`.
- **Group by method under test.** When a class/file exposes several functions and each accumulates many tests, don't keep one flat list — give each function its own `@Nested @TestInstance(Lifecycle.PER_CLASS) inner class`. The nesting maps the test structure onto the production API and keeps per-function setup local to its group.
  ```kotlin
  internal class DesignControllerTest {

      @Nested
      @TestInstance(TestInstance.Lifecycle.PER_CLASS)
      inner class GetDesigns {
          @Test fun `GIVEN … WHEN getDesigns THEN all fields included`() { … }
          @Test fun `GIVEN limit WHEN getDesigns THEN list is capped`() { … }
      }

      @Nested
      @TestInstance(TestInstance.Lifecycle.PER_CLASS)
      inner class DeleteDesign {
          @Test fun `GIVEN existing id WHEN deleteDesign THEN removed from db`() { … }
      }
  }
  ```
- You do **not** declare `useJUnitPlatform()` per module — the `configuration` convention plugin applies it (and the JUnit 5 engine) to every module. See "Gradle wiring" below.

## MockK conventions

- Field-level init: `private val x: T = mockk()`; use `mockk(relaxed = true)` only when stubs are not the subject of the test.
- Stub coroutines with `coEvery { … } returns …` / `returnsMany(...)`; verify with `coVerify { … }`, `coVerify(exactly = n) { … }`, `coVerifyOrder { … }`.
- Create mocks once as `val` fields and reset them with `clearMocks(...)` in `@BeforeEach` — recreating mocks (`x = mockk()` inside `@BeforeEach`) every test is measurably expensive (MockK instantiation dominates the runtime of small tests). Only recreate a field when the subject-under-test itself holds mutable state that must be fresh per test.
- For companion/top-level objects use `mockkObject(Obj)` and pair with `unmockkObject(Obj)` in teardown.

## Flow testing

- Default to `TestScope.getEmittedValues(flow)` (from `:test:core`) when you just want the list of values produced during the test scope — this is the most common approach in the codebase.
- Use **Turbine** (`flow.test { … }`) when you specifically need to assert on the emission *sequence* (ordering, intermediate items, completion/error timing), or for hot `SharedFlow`s where you must control collection start/stop.
- Drive hot sources via `MutableSharedFlow` / `MutableStateFlow` and `advanceUntilIdle()` between emission and assertion.
- For `FlowProducer` tests (retry/fallback/shareIn semantics), inject `TestFlowProducerTools` from `:test:core` and use Turbine + `advanceTimeBy(2001); runCurrent()` to step over the 2s retry window deterministically.

## Parameterized tests

When the same behaviour is exercised over a set of inputs, write **one parameterized test** — not several near-identical methods, and not one method with a stack of `assertThat(...)` calls over different inputs. Repeated asserts hide *which* input failed and stop at the first failure; a parameterized test reports each case separately. Add a new case = add a row to the provider.

Use the project's `@ProvideTestModels` annotation — it wires `@MethodSource("provideTestModels")` for you.

```kotlin
@ParameterizedTest
@ProvideTestModels
fun create(model: CreateModel) = runTest { … }

private data class CreateModel(val input: Input, val expected: Either<Error, Value>)

private fun provideTestModels() = listOf(
    CreateModel(input = …, expected = Either.Right(…)),
    CreateModel(input = …, expected = Either.Left(Error.Foo)),
)
```

`provideTestModels` is a non-static instance method, so the class needs `@TestInstance(Lifecycle.PER_CLASS)` (or a `@JvmStatic` provider in a companion).

## Assertions

- Default to Truth: `assertThat(actual).isEqualTo(expected)`, `.isInstanceOf(T::class.java)`, `.hasMessageThat().isEqualTo(…)`, `.isNull()`.
- **Assert whole objects, not field-by-field.** When the type is a `data class`, build the expected instance and compare with one `isEqualTo(expected)` — the structural `equals`/`toString` gives a self-explanatory diff. For collections use `.containsExactly(…)` (add `.inOrder()` when order matters). Prefer this over a series of `assertThat(actual.id)…`, `assertThat(actual.name)…` checks, which produce opaque failures and miss unexpected fields.
- For Arrow `Either`/`Option`, prefer `assertEither`, `assertEitherLeft`, `assertEitherRight`, `assertSome`, `assertNone` from `:test:core`.
- Exception testing: `runCatching { … }.exceptionOrNull()` + Truth, not `assertThrows`.

## Feature model tests

`features/*/impl` Decompose models share a heavy dependency graph — extract a `XxxModelTestBase` with pre-built mocks/fixtures and inherit per-scenario test classes from it (see `features/staking/impl/.../presentation/model/StakingModelTestBase` as reference).

Lifecycle:
```kotlin
val model = createModel(testScope = this)
advanceUntilIdle()
// assertions…
model.onDestroy()
```

## Running tests

```bash
./gradlew unitTest                         # all JVM + debug/googleDebug unit tests (root aggregator)
./gradlew :<module>:testDebugUnitTest      # single Android library module
./gradlew :app:testGoogleDebugUnitTest     # app module
./gradlew :<jvm-module>:test               # pure JVM module
./gradlew :<module>:testDebugUnitTest --tests "com.tangem.<Fqn>Test"   # single class
```

The `unitTest` aggregator lives in the root `build.gradle.kts`; it is wired automatically for every `com.android.application`, `com.android.library`, and pure `org.jetbrains.kotlin.jvm` subproject — no need to touch it when adding a new module.

## Gradle wiring for a new test-bearing module

The `configuration` convention plugin (`configureUnitTests` in `plugins/configuration/.../TestConfigurations.kt`) centralizes the JUnit 5 setup for **every** module:

1. `useJUnitPlatform()` on all `Test` tasks — so Jupiter tests are discovered (without it the default JUnit 4 runner runs zero Jupiter tests).
2. `testRuntimeOnly(<test-junit5-engine>)` — the Jupiter runtime engine. The platform without the engine silently runs **zero** tests, so these two are paired in one place.
3. Test logging (full exception format, standard streams, PASSED/SKIPPED/FAILED events, per-task summary).

So a test module must **not** re-declare `useJUnitPlatform()`, the engine, or `testLogging { … }`. It only needs the Jupiter **API** (provided transitively by `:test:core`, or declared explicitly):

```kotlin
// Any module (JVM or Android library) — plugin already supplies platform + engine + logging
plugins {
    alias(deps.plugins.kotlin.jvm)   // or the android-library convention
    id("configuration")
}

dependencies {
    testImplementation(projects.test.core)   // junit5 (api) + mockk + turbine + truth + coroutine-test
    testImplementation(projects.common.test) // add only if the tests need legacy model factories / fakes
}
```

If a module doesn't want the full `:test:core` bundle, declare the Jupiter API directly with `testImplementation(deps.test.junit5)` — the engine still comes from the plugin, so never add `testRuntimeOnly(deps.test.junit5.engine)` per module.

## Module type vs. layer

The domain layer is **not** uniformly pure-JVM: domain modules are split roughly evenly between `org.jetbrains.kotlin.jvm` (pure JVM) and `com.android.library` modules. Don't assume the layer dictates the module type — check the `plugins { }` block to pick the right test task:

- `kotlin.jvm` (pure JVM) → `./gradlew :<module>:test`
- `com.android.library` / `com.android.application` → `./gradlew :<module>:testDebugUnitTest` (`:app` → `testGoogleDebugUnitTest`)

`./gradlew unitTest` runs the right task for every module regardless of type.