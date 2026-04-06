# domain/core

Cross-cutting domain utilities for async data loading, error handling, and reactive streams. Not business logic — foundational abstractions used across all domain modules.

## LCE (Loading-Content-Error) Pattern

`Lce<E, C>` — sealed class representing async operation state:
- `Loading(partialContent?)` — in progress, may carry partial data
- `Content(content)` — success
- `Error(error)` — failure with typed error

Key APIs:
- `lce { }` builder — executes block in `LceRaise` context with Arrow's Raise DSL for typed error handling
- `lceFlow { }` builder — creates `LceFlow<E, C>` (alias for `Flow<Lce<E, C>>`) via channel-based producer DSL
- `LceRaise.bind()` — extracts content from Lce/Either or short-circuits on error
- Extensions: `fold()`, `map()`, `mapError()`, `toLce()`, `toEither()`

## Flow Packaging

A pattern for complex data streams where work on a single flow is split into three logically separate components: **Producer** (creation), **Supplier** (delivery/caching), and **Fetcher** (refresh). Use it only when you need flexibility in creating, reusing, fetching, and updating a data stream (e.g., network status). Do NOT use for simple cases like reading preferences.

### FlowProducer

`FlowProducer<Data>` — creates the data flow. Implement:
- `fallback: Data` — emitted when the flow throws an exception
- `produce(): Flow<Data>` — the actual flow creation logic

Built-in `produceWithFallback()` catches errors, emits `fallback`, waits 2s, then retries — keeping the flow alive for subscribers.

`FlowProducer.Factory<Params, Producer>` — creates a Producer from params. Typically implemented via Hilt `@AssistedFactory`.

**Implementation pattern:**
1. Define interface extending `FlowProducer<Data>` with inner `Params` data class and `Factory` interface
2. Create `Default*Producer` with `@AssistedInject` constructor taking `@Assisted params` + dependencies
3. Override `fallback` and `produce()`
4. Declare inner `@AssistedFactory` interface extending the Producer's Factory

### FlowSupplier / FlowCachingSupplier

`FlowSupplier<Params, Data>` — delivers a flow by params via `operator fun invoke(params): Flow<Data>`. Also provides `getSyncOrNull(params, timeout)` for one-shot access.

`FlowCachingSupplier<Producer, Params, Data>` — abstract implementation that caches flows by key. Implement:
- `factory: FlowProducer.Factory` — to create producers
- `keyCreator: (Params) -> String` — to generate cache keys

Behavior: returns cached flow if exists, otherwise creates via `factory.create(params).produceWithFallback()`, caches it, and auto-evicts on terminal exception.

**Implementation pattern:**
1. Define abstract class extending `FlowCachingSupplier` with `factory` and `keyCreator` in constructor
2. In DI module, create anonymous subclass providing the factory (injected) and keyCreator lambda

### FlowFetcher

`FlowFetcher<Params>` — triggers data refresh, returns `Either<Throwable, Unit>`. Typically updates a store/data source, causing the Producer's flow to re-emit.

**Implementation pattern:**
1. Define interface extending `FlowFetcher<Params>` with inner `Params` data class
2. Create `Default*Fetcher` with `@Inject` constructor, override `invoke` wrapping logic in `Either.catch { }`, handle errors with `.onLeft { }`

### Testing

- **FlowProducer**: test flow creation logic, params usage, emission behavior, exception handling
- **FlowFetcher**: test successful update path and error path (exception thrown)

## Chain Processing

- `Chain<E, R>` / `ResultChain<E, R>` — single operation in a chain, works with `Either<E, R>`
- `ChainProcessor<E, R>` — folds chains sequentially, stops on first error

## Error Types

- `DataError` — sealed domain error hierarchy: `NetworkError.NoInternetConnection`, `UserWalletError.WrongUserWallet`

## Either Extensions

- `Either.catchOn(dispatcher, block)` — executes on dispatcher, catches exceptions
- `eitherOn(dispatcher, block)` — Raise DSL block on specified dispatcher