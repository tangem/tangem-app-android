# core/socket

**WebSocket layer for any feature.** A socket is described the way a Retrofit API is: an interface with
`@Receive` / `@Send` functions plus a `SocketSpec`. The core owns the connection — when to open it,
reconnect and backoff, keep-alive, background and network, close-code handling — and the feature only
describes its protocol.

- `core:socket` — pure JVM: contracts, the connection loop (`DefaultSocketChannel`), the OkHttp transport,
  `KeyedSubscriptions`. No Android, no product types.
- `core:socket:processor` — KSP: for every `@SocketService` interface generates `XxxImpl`, `FakeXxx` and
  `SocketChannelFactory.createXxx(spec)`. No reflection, no R8 rules; a wrong signature fails the build.
- `core:datasource-impl` (`com.tangem.datasource.socket`) — the Android side: `SocketChannelFactory`
  bound in Hilt, foreground from `ProcessLifecycleOwner`, network from `ConnectivityManager`.
  `ProcessLifecycleOwner` is started by androidx.startup's `InitializationProvider`; the app manifest removes
  only `WorkManagerInitializer` from it. Removing the provider again leaves every socket `Paused(Background)`.

Interfaces, DTOs and specs of a concrete socket live in the owning stream's `*:datasource` module, never here.

## Adding a socket — four steps

**1. Module wiring** (`*:datasource` that owns the API):

```kotlin
plugins {
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.ksp)
}

dependencies {
    api(projects.core.socket)
    ksp(projects.core.socket.processor)
}
```

**2. Describe the protocol** — `@Serializable` DTOs and an interface:

```kotlin
@SocketService
interface TickerSocket {
    @Receive fun tickers(): Flow<TickerDto>          // any @Serializable type, sealed hierarchies included
    @Receive fun state(): StateFlow<SocketState>     // optional; state().value is O(1), does not hold the connection
    @Send fun subscribe(frame: SubscribeFrameDto): Boolean
}
```

**3. Provide it** — the generated factory builds it from a `SocketSpec`:

```kotlin
@Provides
@Singleton
fun provideTickerSocket(factory: SocketChannelFactory): TickerSocket =
    factory.createTickerSocket(SocketSpec(url = "wss://example.com/ws"))
```

**4. Collect it** in the repository:

```kotlin
fun observe(symbol: String): Flow<Ticker> = socket.tickers().filter { it.symbol == symbol }.map(converter::convert)
```

That is the whole feature-side code. The connection opens while something collects a `@Receive` flow and
closes `idleTimeout` (5 s) after the last collector leaves.

## Lifetime — what keeps a socket open

- **Creating the service opens nothing.** `createXxx(spec)` costs two idle coroutines (the loop and its state log); until a `@Receive`
  flow is collected the channel does not even observe foreground, network, `lifecycle` or `reconnectOn`.
  The service lives as long as the process, so provide it (and `KeyedSubscriptions`) as `@Singleton` only,
  never per screen or per model.
- **Only a collector holds the connection.** Repositories return cold flows. Never
  `shareIn` / `stateIn` / `launchIn` a socket flow in `appScope` with `Eagerly` or `Lazily` — that keeps the
  connection open for the whole foreground session. Share with `WhileSubscribed` if you must, and collect
  from screen scopes (`modelScope`).
- **Feature off = nobody collects.** A screen hidden behind a toggle or an experiment needs nothing else. A
  background consumer (a badge, a feed card) passes the condition reactively:
  `lifecycle = SocketLifecycle { isFeatureEnabledFlow }`.
- **A collector slower than the stream loses the oldest frames — its own only.** Each collector has its own
  buffer, so other collectors and the reader are never blocked; re-fetch the REST snapshot if gaps matter.

## Rules

- **KSP next to kapt is fine.** Hilt runs on kapt; in a module with both, Gradle runs KSP first, so Hilt
  sees generated types too. Still return your hand-written interface from `@Provides` — callers and tests
  then depend on the contract, not on `XxxImpl`.
- **Every abstract function is `@Receive` or `@Send`.** `@Receive`: no parameters, returns `Flow<T>`
  (`StateFlow<SocketState>` is allowed for the state, so `state().value` is a cheap read).
  `@Send`: one parameter, returns `Boolean` — `true` means "queued", not "delivered".
- **Only JSON objects and arrays are frames.** Any other text — `PONG`, `"ok"`, `42` — is treated as
  protocol chatter and never reaches a collector: `@NetworkJson` is lenient and would otherwise turn a bare
  `PONG` into a string. Wrap a primitive payload in an object DTO; `Flow<String>` / `Flow<Int>` never emit.
- **One sealed type per socket, picked by a discriminator.** A frame is parsed to JSON once; each
  `@Receive` type then decodes it once and shares the result with all its collectors, skipping frames
  that do not decode. `@NetworkJson` ignores unknown keys and fills missing nullable fields, so a loose
  DTO happily decodes a frame meant for another type. Model the stream as a `@Serializable sealed`
  hierarchy with `@JsonClassDiscriminator` and an `Unknown` fallback (`polymorphicUnknownFallback`, see
  `core/remote/CLAUDE.md`). Need one event type alone? `@Receive fun trades(): Flow<EventDto.Trade>` —
  for a subtype of a sealed serializable base the processor decodes through the base and filters, so the
  discriminator decides. Several unrelated `@Receive` DTOs are safe only when their required fields
  cannot match each other's frames.
- **REST stays the source of truth.** A socket overlays updates on a REST snapshot; on
  `SocketState.Connected` after a gap, re-fetch the snapshot. Keep polling as a fallback while the state
  is `Connecting` / `WaitingToRetry`.
- **Fakes:** `FakeXxx` exposes a `MutableSharedFlow` per `@Receive` function, a `xxxSent` list per
  `@Send` function and `sendResult`. The processor rejects names that would clash with those properties or
  their JVM accessors, e.g. `@Receive fun isConnected()`.

## SocketSpec

| Field | Default | Use it for |
|---|---|---|
| `url` | — | `wss://…` |
| `headers` | none | handshake headers, resolved before **every** connect attempt (auth) |
| `transport` | the factory's OkHttp client | your own `OkHttpSocketTransport(client)`: interceptors, pinning, proxy, timeouts |
| `lifecycle` | always | an extra condition: credentials present, a screen is visible (`SocketLifecycle`, `and`) |
| `reconnectOn` | none | a flow whose every new value reconnects at once without backoff (account switch, token rotation) |
| `keepAlive` | none | `ClientSends("PING", 10.seconds)` — first ping a full interval after `onOpen`; `ServerSends(text, reply)` for text pings. Protocol-level ping frames need nothing |
| `backoff` | 1 s → 30 s, ×2, jitter 50% | `Backoff.exponential(…)` or your own |
| `closePolicy` | always retry | `SocketClose` → `Retry`, `RetryAfter(delay)` or `Stop` |
| `onOpen` | nothing | the first frames of every connection: auth message, the full subscription set. It must only send: incoming frames wait until it returns. Until then the state stays `Connecting` and frames sent from outside are queued to go out right after it |
| `isSplitArrayFrames` | false | a JSON-array frame is delivered element by element |
| `idleTimeout` | 5 s | how long the connection outlives its last collector |

Foreground and network are always part of the condition — a socket never stays open in the background.
Losing the network pauses (`Paused(NoNetwork)`) without spending backoff attempts; coming back connects
at once, and so does a switch of the default network (Wi-Fi → cellular). Validation changes on the same
network (a captive portal passed) do not interrupt a working connection; backoff retries it. Backoff starts over only after a connection delivered a message or lived 30 s, so a server that
accepts and closes at once is not hammered every second.

Nothing a spec does can kill the socket for good: `headers`, the transport's `open` (a malformed URL or
header) and `onOpen` throwing end the attempt like a dropped connection (`SocketCloseCode.ABNORMAL`), so
the close policy and backoff apply; anything else restarts the loop after 5 s and logs an error.

## Authorization

| Server expects | Do |
|---|---|
| A header on the handshake | `headers = { mapOf("Authorization" to "Bearer ${tokenStore.get()}") }` — `resolve` is `suspend`, so it can read or refresh a token |
| A rejected token to be refreshed | the next `resolve` gets `context.previousClose?.handshakeStatus == 401`: force a refresh there |
| An app-wide OkHttp setup (auth interceptor, pinning, logging) | `transport = OkHttpSocketTransport(client)` with that client; application interceptors run on the handshake. The client must set `pingInterval` (the app's socket client uses 15 s, the constructor rejects none): after the upgrade OkHttp drops the read timeout, so protocol ping frames, which every server answers, are what notice a silently dead connection |
| A token in the query string | an interceptor on that client that rewrites the URL |
| An auth message after connect | `onOpen = { send(AuthFrameDto.serializer(), AuthFrameDto(credentials())) }` — sent again after every reconnect |
| A close code for bad credentials (e.g. 4001) | `closePolicy = { if (it.code == 4001) Stop else Retry }`, invalidate the credentials elsewhere; `SocketState.Stopped` stays until the gate changes |
| No connection without credentials | `lifecycle = { credentials.map { it != null } }` |
| A reconnect when the account changes | `reconnectOn = userWalletIdFlow` |

## Keyed subscriptions

For servers where you subscribe per key (asset id, chat id) use `KeyedSubscriptions` instead of sending
subscribe frames by hand. It reference-counts keys across collectors, batches changes into one frame
(`batchWindow`), unsubscribes after `unsubscribeDelay`, restores the whole set on reconnect via
`openKeys()` in `onOpen`, and opens another service instance when `maxKeysPerService` is reached.
Broadcast sockets do not need it.

## Tests

- Repository / data layer: inject the generated `FakeXxx`, emit into its flows, assert on `xxxSent`.
- Core behaviour: `DefaultSocketChannelTest` / `KeyedSubscriptionsTest` run on a fake `SocketTransport`
  with virtual time — follow them when you extend the core.
- Mocked builds: provide the fake socket instead of `createXxx` (WireMock does not speak WebSocket).