# Transaction history: loading and reading

How the transaction history on the Token Details screen works: where the data comes from, how it gets
into the local DB, how it is read back and what it has become by the time it leaves the data layer.
This is the single doc for the whole pipeline — `domain/txhistory` and `features/txhistory` only point
here.

The pipeline has three parts:

| Part | What it does | Where |
|---|---|---|
| **1. Loading (sync)** | pulls history from the API into the local DB, in the background, with no screen involved | `data/txhistory/fetcher`, `data/txhistory/repository` |
| **2. Reading** | assembles one timeline, `List<TxHistoryInfo>`, out of the DB plus the chain | `data/txhistory/list`, `RefactoredTxHistoryRepository` |
| **3. UI mapping** | `TxHistoryInfo` → `TxHistoryItemsUM` | `features/txhistory/impl` |

Parts 1 and 2 are covered here. Part 3 is separate.

The key property of the architecture: **loading and reading are not wired to each other.** Fetchers
write to the DB, the read pipeline subscribes to the DB. Their only point of contact is Room: a newly
written row reaches an open screen through a `Flow` on its own. The exception is on-chain history
(BSDK / TangemPay) — it is not synced in the background, it is paged lazily right in the read pipeline.

---

## Part 1 — loading: `AppTxHistoryFetcher`

### 1.1 The shape: a tree of fetchers

Fetchers form a three-level tree. Each level owns the lifecycle of the one below and creates children
as entities appear:

```
AppTxHistoryFetcher                      @Singleton, app-wide
└── WalletTxHistoryFetcher               per multi-currency unlocked wallet
    ├── ExpressTxHistoryFetcher          always, together with the wallet
    └── JointOperationTxHistoryFetcher   lazily — only if the wallet has an Account.Joint
```

Contracts: `TxHistoryFetcher.kt`

```kotlin
interface TxHistoryFetcher<T : TxHistoryFetchTrigger> {
    operator fun invoke(params: T)
    fun close()
}
```

Every fetcher is an `AssistedInject` factory taking a `UserWalletId`, except the root
`AppTxHistoryFetcher`, which is a `@Singleton` injected into models directly.

### 1.2 Triggers

`TxHistoryFetchTrigger` is a sealed hierarchy with marker super-interfaces that decide which child
fetcher sees a trigger:

| Trigger | Sent by | `TxHistoryExpressTrigger` | `TxHistoryJointOperationTrigger` |
|---|---|---|---|
| `WalletSelected` | `WalletModel`, on selected-wallet change | ✅ | ✅ |
| `TokenDetailsOpen` | `TxHistoryModel.init` | ✅ | ✅ |
| `TokenDetailsPTR` | `TxHistoryModel.reload()` (pull-to-refresh) | ✅ | ✅ |
| `JointHistoryPoll` | `JointHistoryPoller`, every 10 s | ❌ | ✅ |

Routing lives in `DefaultWalletTxHistoryFetcher`:

```kotlin
private fun routeToWalletFetchers(trigger: TxHistoryFetchTrigger) {
    if (trigger is TxHistoryExpressTrigger) expressFetcher.invoke(trigger)
    if (trigger is TxHistoryJointOperationTrigger) jointOperationFetcher?.invoke(trigger)
}
```

For a new trigger to reach a given fetcher it is enough to mark it with the right super-interface —
no type branching has to be added inside the fetchers themselves.

### 1.3 Trigger transport: `TxHistoryFetcherUtils`

`TxHistoryFetcherUtils` is the shared mini-runtime every fetcher mixes in by delegation
(`..., TxHistoryFetcherUtils by utils`). It provides:

- `triggersBuffer: Channel<TxHistoryFetchTrigger>` (BUFFERED) — one per fetcher, because
  `DefaultTxHistoryFetcherUtils` is bound **without** `@Singleton` (a new instance per injection);
- `fetcherScope = appScope + SupervisorJob()` — the fetcher's scope, cancelled in `close()`;
- `defaultLaunchIn(flow)` — starts the main loop with an infinite `retry` plus an
  `ExceptionAnalyticsEvent(source = "TxHistory")`;
- `retryThreeTimes()` — 3 attempts, 1 s apart, for a single network call;
- `receiveTrigger()` / `receiveTriggerInstance<R>()` — receive from the channel (filtered by type);
- `logError` → `TangemLogger.withTag("TxHistory").e(...)`.

An important consequence of using a `Channel`: it has **one** receiver per message. So `invoke(trigger)`
at the `App` level does not fan the trigger out — it is taken by `DefaultAppTxHistoryFetcher`'s own
loop, which then explicitly calls `invoke` on the right child fetcher, putting the trigger into **its**
channel.

### 1.4 `DefaultAppTxHistoryFetcher` — the root

Starts from `init { defaultLaunchIn(buildFlow()) }`, i.e. effectively on first injection
(`WalletModel` or `TxHistoryModel`). Inside one `channelFlow` it does four things:

1. **Tracks the list of wallets.**
   `getWalletsUseCase.invokeAsMap(isOnlyMultiCurrency = true, filterLocked = true)` → `StateFlow`.
   Every new `UserWalletId` creates a `WalletTxHistoryFetcher` (`ConcurrentHashMap` +
   `computeIfAbsent`), every one that disappears is `close()`d and dropped. Single-currency and
   locked wallets are not synced at all.

2. **Saves token metadata.**
   `multiAccountListSupplier()` → a flat list of every wallet's currencies →
   `tokenInfoRepository.save(...)`. This fills the local token table from which the "foreign" assets
   of express deals are later resolved (a currency the user does not hold but that takes part in a swap).

3. **Warms up the lookups.** Once, off the first wallet:
   `expressRepository.getProviders(...)` and `onrampRepository.fetchCurrencies(...)`, both under
   `retryThreeTimes()`. Without them an express row knows neither its provider nor its fiat currency.

4. **Routes triggers to wallets.** The `walletId` is taken from the trigger, `isMultiCurrency` is
   checked, and the trigger goes to `getOrPutFetcher(walletId)`.

`close()` cancels the scope and closes every child. **Nothing calls it right now** — the fetcher lives
as long as the process (there is a `// todo txhistory use lifecycle scope?` on this in
`DefaultTxHistoryFetcherUtils`).

### 1.5 `DefaultWalletTxHistoryFetcher` — the wallet level

Does exactly what the root does, but for the wallet's accounts:

- `expressFetcher` is created straight away in the constructor — every multi-currency wallet has express;
- `jointOperationFetcher` is created **lazily**: when at least one `Account.Joint` appears in
  `singleAccountListSupplier(walletId)`; when every joint account is gone — `close()` and `null`.
  The field is `@Volatile`, because it is written from the accounts coroutine and read from the
  triggers coroutine.

This is the extension point at the wallet level: a new source tied to an account type is added here —
a field, creation in `createForNewAccounts()`, closing in `closeForRemovedAccounts()`, and a line in
`routeToWalletFetchers`.

### 1.6 `DefaultExpressTxHistoryFetcher` — swap and onramp sync

Two independent branches of the same shape: **exchange** (swaps) and **onramp** (purchases). Every
trigger calls `fetchExchange()` and `fetchOnramp()`.

The sync is the classic "initial + delta":

```
                     syncState == null ?
                            │
           yes ─────────────┴───────────── no
             │                              │
   one fetchHistory():                      │
   creates a sync state with                │
   afterCursor and deltaCursor              │
             └──────────────┬───────────────┘
                            ▼
      ┌─────────────────────────────────────────┐
      │ initialPagination()  (in its own Job)    │  walks into the past by afterCursor
      │ while !isInitialCompleted and hasMore    │  until it hits the end of the history
      └─────────────────────────────────────────┘
      ┌─────────────────────────────────────────┐
      │ deltaPagination()                        │  picks up changes by deltaCursor
      │ while hasMore                            │  (new and updated deals)
      └─────────────────────────────────────────┘
```

Races are kept out through the `Job` fields: if `deltaPaginationJob?.isActive == true` a new trigger
is ignored; `initialPaginationJob` outlives triggers and is not restarted while active. So the initial
pass downloads the whole history once, and delta runs on every trigger.

The sync state is the `history_sync_state` table (`HistorySyncStateEntity`): key `(type, userWalletId)`
where `type` ∈ `EXCHANGE | ONRAMP | …`; fields `afterCursor` (the initial pass's cursor), `deltaCursor`
(the delta cursor), `isInitialCompleted`.

Writing and the cursors live in `DefaultExpressHistoryRepository`. Importantly, `storeExchanges` /
`storeOnramps` write the entity and its index rows **in one transaction**:

```kotlin
database.withTransaction {
    expressHistoryDao.upsertExchanges(entities)
    historyIndexDao.upsert(entities.flatMap { it.toHistoryIndexEntities() })
}
tokenInfoRepository.fetchMissing(/* assets of both legs of the deal */)
```

For the index see §2.6. `fetchMissing` pulls the metadata of unknown tokens so the row can show the
icon and symbol of an asset that is not in the user's portfolio.

### 1.7 `DefaultJointOperationTxHistoryFetcher` — joint operation sync

Structurally a copy of one branch of the express fetcher (`fetch()` → initial + delta), only with one
branch instead of two and `type = JOINT_OPERATION` in `history_sync_state`.

Differences in `DefaultJointOperationHistoryRepository`:

- **Only finished operations are put in the DB** (`FINISHED_STATUSES`) — in-flight voting lives in its
  own feature and has no business in a history feed;
- `JointOperationEntity` + `JointOperationVoteEntity` (two tables, votes apart);
- **the sync resets on 400.** The backend answers a cursor it no longer accepts with 400; in that case
  `getOrResetSyncState` / `refusedCursorCause` delete `history_sync_state`, so the next sync starts
  from scratch instead of failing on one cursor forever;
- **a single operation can be re-fetched**, `fetchOperation(walletId, operationId)`, with an ETag
  (`operationETags: ConcurrentHashMap`): `304 NOT_MODIFIED` → `NotModified`, `404`/`403` → `Gone`.
  Needed for an operation's details opened from a push or deep link.
- **joint operations do not reach `history_index`** — they have their own read path (§2.7).

### 1.8 What part 1 does not cover

Blockchain history (BSDK) and TangemPay history are **not synced** by the fetchers. They are paged at
read time (§2.4) and cached in `TxHistoryItemsStore` / `CacheRegistry`. Only what the chain cannot show
is synced in the background: express deals and joint operations.

### 1.9 Adding a new loading source — checklist

Following how joint accounts were added:

1. **Trigger.** If a new reason to load is needed — a new `TxHistoryFetchTrigger` subtype plus a marker
   super-interface (`TxHistoryXxxTrigger`). If the existing ones are enough — just mark them with the
   new super-interface.
2. **Fetcher contract** in `TxHistoryFetcher.kt`:
   `interface XxxTxHistoryFetcher : TxHistoryFetcher<TxHistoryXxxTrigger>`.
3. **Implementation** `DefaultXxxTxHistoryFetcher` in `data/txhistory/fetcher`: `@AssistedInject` with
   `walletId`, `TxHistoryFetcherUtils by utils`,
   `init { defaultLaunchIn(receiveTriggerInstance<…>().onEach { fetch() }) }`, and a
   `initialPaginationJob` / `deltaPaginationJob` pair.
4. **Repository** in `data/txhistory/repository`: `fetchHistory` / `fetchHistoryDelta` plus the Room
   write plus `persistHistoryState`. A new `HistorySyncStateEntity.Type`.
5. **Tables** in `store/datasource/.../txhistory/db`: entity, DAO, registration in `TxHistoryDatabase`
   (`version++` and an `AutoMigration`).
6. **Wire into the tree** — a field and its lifecycle in `DefaultWalletTxHistoryFetcher` (or in
   `DefaultAppTxHistoryFetcher` if the source is not tied to a wallet), a line in `routeToWalletFetchers`.
7. **DI** — a binding in `TxHistoryDataModule`.
8. **Decide whether `history_index` is needed** (§2.6): it is needed only if the rows must paginate on
   their own, without an on-chain backbone.

---

## Part 2 — reading, up to `TxHistoryInfo`

### 2.1 The result model: `TxHistoryInfo`

`domain/txhistory/.../model/TxHistoryInfo.kt` — one row of the timeline, what the data layer ends at.
A sealed hierarchy by origin:

```
TxHistoryInfo                       txId: String, timestampMillis: Long
├── OnChainTx                       a blockchain transaction
│   ├── OnChainTx.BSDK(txInfo: TxInfo)                 ← the main backbone
│   └── OnChainTx.TangemPay(txInfo: TangemPayTxHistoryItem)
├── ExpressTx                       an express operation (+ its matched on-chain leg, optionally)
│   ├── ExpressTx.Swap(tx: ExchangeTransaction, isOutgoing, txInfo: OnChainTx?)
│   └── ExpressTx.Onramp(tx: OnrampTransaction, txInfo: OnChainTx?)
└── JointOperationTx(operation, transfer, txInfo: OnChainTx?)
```

Invariants worth knowing before making changes:

- `txId` is **the identity of the row**. For express it is the deal's `txId`, for `OnChainTx.BSDK` it is
  `identityKey() = "$txHash|$type|$destinationType"`. One hash can produce several rows (the deal's
  payload plus the events kept out of it, such as a gasless fee or a yield withdrawal), which is why the
  key is composite and not just the hash.
- `timestampMillis` is the position in the timeline. For `ExpressTx` and `JointOperationTx` it is the
  block time of the matched leg, and while there is none — `createdAt` / `finishedAt`.
- `explorerHash` is the hash for the explorer link; `null` when there is no on-chain leg. An express
  deal's `txId` must not be substituted here — it would build a broken link.
- `idToCopy` is what the user copies and takes to support: the on-chain hash for `OnChainTx`, the
  provider's `externalTxId` for `ExpressTx` (for a DEX, which has none — its own `txId`).

The hierarchy is extended exactly here: a new row type is a new subtype, and the compiler then points at
every `when` that needs completing (`explorerHash`, `idToCopy`, the UI converter).

### 2.2 The read contract: `HistoryTxListManager`

`domain/txhistory/.../list/HistoryTxListManager.kt`. Created **per (wallet, currency)** through
`Factory` and living in the screen's `modelScope`:

```kotlin
val historyTxListManager = historyTxListManagerFactory.create(
    userWalletId = params.userWalletId,
    currency = params.currency,
    modelScope = modelScope,
)
```

The API surface:

| Member | Meaning |
|---|---|
| `state: StateFlow<HistoryState>` | the single source of truth for the UI |
| `historySources: Flow<HistorySources>` | what exactly the history is backed by (the screen needs it for analytics / CTA) |
| `txExpressHistoryItemFlow(txId)` | one row, reactively, for the details sheet |
| `reload()` / `loadMore()` | user actions |

`HistoryState` is exactly one of: `Loading`, `Unavailable`, `Empty`, `Error`,
`Content(items, isLoadingMore, hasMore)`.

`HistorySources` describes "what the history is assembled from": `onChainSource: OnChainSource`
(`BSDK` / `TangemPay` / `IndexTable`) plus the `isExchangeAvailable` / `isOnrampAvailable` flags.

### 2.3 `DefaultHistoryTxListManager` — source selection and pipeline assembly

The manager itself **does not read data** — it decides *where* to read it from and assembles the chain.

```
       actionsFlow: Channel<Action>            ← reload() / loadMore()
               │
   ┌───────────┴─────────────────────────────────────────────┐
   │ first pass: loadSources()                               │
   │   • resolveOnChainSource()  — which backbone            │
   │   • expressServiceFetcher   — is swap/onramp available  │
   │   failure → HistoryState.Error, wait for the next Action │
   └───────────┬─────────────────────────────────────────────┘
               │ .first() — this collector ends as soon as the sources are known
               ▼
       isHistoryAvailable(sources) ? ── no ──► HistoryState.Unavailable (done)
               │ yes
               ▼
   OnChainHistory = BsdkOnChainHistory | TangemPayOnChainHistory | IndexTableOnChainHistory
               │
   actionsFlow ─┴──► onChainHistory.sendAction(...)       (actions now go here)
               │
   onChainHistory.history() ──► emptyAsUnavailable() ──► state
```

Two details that are easy to break:

- **`actionsFlow` is a `Channel`, not a broadcast.** While the sources are not resolved yet, actions are
  taken by the first collector (which re-runs `loadSources`). After a successful `first()` the first
  collector falls away, and every later action goes to the `OnChainHistory`. That is exactly how retry
  from the `Error` state works.
- **Sticky content** in `init`:
  ```kotlin
  .runningReduce { previous, new -> if (previous.isContent && !new.isContent) previous else new }
  ```
  a list already on screen does not collapse into `Loading`/`Error` on a background refresh.

**Choosing the on-chain source** (`resolveOnChainSource`):

| Condition | Source |
|---|---|
| the currency is found in an `Account.CryptoPortfolio` (`Account.Joint` included) or in a Money account, and `getTxHistoryItemsCount` succeeded | `BSDK` |
| same, but the count failed with `TxHistoryStateError.EmptyTxHistories` | `BSDK` (history empty, but supported) |
| same, but the count failed with `TxHistoryStateError.TxHistoryNotImplemented` | `IndexTable` |
| same, but `DataError` / `AddressNotReady` | exception → `HistoryState.Error` |
| the currency belongs to an `Account.Payment` | `TangemPay` |
| everything else | `IndexTable` |

`Account.Joint` is a subtype of `Account.CryptoPortfolio`, so a joint currency takes the `BSDK` branch;
that is the reason joint rows are mixed in precisely in `BsdkOnChainHistory`.

**History availability** (`isHistoryAvailable`): always true for `BSDK` and `TangemPay`; for
`IndexTable` — only if swap or onramp is available, otherwise there is nothing to show.

**`emptyAsUnavailable`**: for `IndexTable` an empty list is not "no transactions yet" but "the
blockchain's own history is unsupported", so `Empty` is replaced by `Unavailable`.

`awaitExpressAsset()` is wrapped in `runSuspendCatching`: express only decorates the timeline, and its
failure must not drag the on-chain source down with it (they start in one `coroutineScope`, where an
exception cancels the sibling).

### 2.4 `OnChainHistory` — three backbones

The contract is deliberately narrow:

```kotlin
internal interface OnChainHistory {
    val actions: Channel<Action>            // Action.Reload(shouldRefresh) | Action.LoadMore
    fun sendAction(action: Action) = actions.trySend(action)
    fun history(): Flow<HistoryState>
}
```

`Action` is deliberately decoupled from `BatchAction` of `core:pagination` — a source that does not
page in batches (like `IndexTable`) has no use for it.

#### `BsdkOnChainHistory` — the main one

1. **Pagination.** `repository.getTxHistoryBatchFlow(context, batchSize = 50)` — a `BatchListSource`
   from `core:pagination`. First of all it emits `Action.Reload(shouldRefresh = false)` itself.
2. **Dust filter.** Incoming transactions worth less than `$0.01` are dropped. Never counted as dust:
   outgoing, `Approve`, `Staking`, `IncentiveClaim` — there what matters is not the size but the fact.
3. **Auto load-more.** `autoLoadMoreUntilScrollable`: while fewer than 20 rows are loaded or the last
   page came back empty, it sends `LoadMore` itself. Otherwise, after the dust filter, the screen can
   be left with three rows and no way to scroll on.
4. **Overlay window.** `oldestLoadedTimestamp(batchState)` — the minimum timestamp of the loaded
   on-chain rows. It is the lower bound the express and joint overlays are requested by, so the working
   set does not grow to the whole history.
5. **Merge.** `combine(getExpressHistory(...), finishedJointTransfers.observe(...))` →
   `mergeTxHistoryInfos(onChain, express, currency, joint)`.
6. **Status.** `PaginationStatus` → `HistoryState` (`InitialLoading` → `Loading`,
   `InitialLoadingError` → `Error`, `NextBatchLoading` → `Content(isLoadingMore = true)`,
   `Paginating` → `Content(hasMore = true)`, `EndOfPagination` → `Content(hasMore = false)`;
   an empty merged list → `Empty`).

#### `TangemPayOnChainHistory`

The same shape, a different repository (`TangemPayTxHistoryRepository`) and a different merge
(`mergeTangemPay` — hash only, no heuristics). The joint overlay is not mixed in. The comment on
`mergeTangemPay` records that the hash semantics have to be re-checked against live data — TangemPay
history is not wired end-to-end yet.

#### `IndexTableOnChainHistory`

The backbone for currencies with no on-chain history at all. It pages the express layer itself:

```kotlin
val limit = MutableStateFlow(PAGE_SIZE)          // 50
// Reload → limit = PAGE_SIZE;  LoadMore → limit += PAGE_SIZE
limit.flatMapLatest { repository.getIndexedExpressHistory(walletId, currency, it) }
     .collect { page -> … mergeTxHistoryInfos(onChain = emptyList(), express = page.items, …) }
```

A separate case: `merged.isEmpty() && page.hasMore` → the window widens itself. The index counts every
operation it recorded, and not all of them reach the timeline (an expired deal is no row of the
history); a window filled entirely with those means "the window is narrow", not "there is no history".

### 2.5 `RefactoredTxHistoryRepository` — where the data actually comes from

The implementation of `TxHistoryRepositoryV2`. Four methods:

**`getTxHistoryBatchFlow`** — on-chain pagination. `BatchListSource` → `TxHistoryPageBatchFetcher`.
Inside: `cacheRegistry.invokeOnExpire(key = "tx_history_page_…", skipCache = shouldRefresh)` →
`walletManagersFacade.getTxHistoryItems(...)` → `txHistoryItemsStore.store(...)`, then a read from the
store. For `Page.Initial` the `getRecentTransactions()` are additionally mixed in — unconfirmed local
transactions that are not in the API answer yet.

**`getExpressHistory(walletId, currency, fromOnChainTimestampMillis)`** — the reactive express layer:

- addresses: `walletManagersFacade.getAddresses(...)` + `usedDynamicAddresses(...)`. A payout may land
  on a non-default address type (legacy P2PKH BTC) — the default address alone is not enough;
- the time bound is shifted back by **24 h** (`EXPRESS_CREATED_AT_SKEW`): the parameter arrives as a

- the bound is formatted as ISO-8601 UTC with an explicit `.000` — `created_at` is stored as a string
  in the DB, and lexicographic order matches chronological, so the filter goes straight into SQL;
- a `combine` of five flows: outgoing swaps, incoming swaps, onramps, providers, fiat currencies;
- active (unfinished) operations are **always** included, whatever the bound;
- models are built by `ExpressTransactionAssetFactory` + `ExpressSwapConverter` /
  `ExpressOnrampConverter`, ending with `distinctBy { it.txId }` (a row can satisfy both swap queries;
  the outgoing interpretation wins).

**`getIndexedExpressHistory(walletId, currency, limit)`** — the same query, but the window bound comes
from the index: `historyIndexDao.observePage(addresses, cursor = null, limit)` → the `sortTimeMillis` of
the last (oldest) row of the page → `getExpressHistory(...)` with that bound.
`hasMore = page.size >= limit`.

**`getExpressTxById(walletId, currency, txId)`** — one deal by id for the details sheet, when it is not
in the pagination window yet (opened from a push or deep link). It looks in both the exchange and the
onramp table — the id spaces do not intersect, so at most one matches.

In the manager these two paths are sewn together in `txExpressHistoryItemFlow`: if the row is already in
`state` it is served from there; otherwise the standalone DB flow is listened to, and as soon as
pagination merges the row in, the standalone subscription is cancelled.

### 2.6 `history_index` — what it is for and when it is needed

`HistoryIndexEntity`, `HistoryIndexDao`.

**The problem.** Normally the timeline is paged by the on-chain history: a page of the chain is loaded,
its lower time bound is taken, and the overlays are pulled within it. With no chain history
(`TxHistoryNotImplemented`, a currency outside the portfolio) there is nothing to page by. Paging the
express tables themselves does not work either: there are two of them (exchange and onramp), each with
its own ordering, and "the 50 most recent operations" cannot be had from two separate queries.

**The solution.** One flat index table over all local sources. A row stores the minimum sufficient for
sorting and lookup:

```
history_index
  type              EXCHANGE | ONRAMP
  entity_id         id of the record in its own table
  address           the address the row is visible under on the screen (NOCASE)
  sort_time_millis  the time the timeline is sorted by
  PRIMARY KEY (type, entity_id, address)
  INDEX (address, sort_time_millis, entity_id)
```

**Why the address is part of the key.** One operation can be shown under different addresses: a swap
between two of the user's own tokens is visible both on the from-token screen (under `from_address`) and
on the to-token screen (under `payout_address`). See `HistoryIndexConverter` — a swap yields
`setOf(fromAddress, payoutAddress)` rows (the set collapses coinciding addresses), an onramp yields one
under `payout_address`.

**Why the query groups.** A token details screen may read several addresses at once, and then one
operation would come back twice. `GROUP BY type, entity_id` + `MAX(sort_time_millis)` collapses it into
one row and at the same time makes the key `(sort_time_millis, entity_id)` unique — without that, rows
would be skipped or duplicated at page boundaries.

**The cursor.** `Cursor(sortTimeMillis, entityId)` — keyset pagination, the cursor is taken from the last
(oldest) row of the previous page. `IndexTableOnChainHistory` currently **does not use** it: it always
reads `cursor = null` with a growing `limit`, so the flow stays reactive and a new operation lands on top
of the already loaded window. The cursor mode is for real page-by-page fetching later.

**Who writes it.** Only `DefaultExpressHistoryRepository`, in the same transaction as the entity itself.
Joint operations are not in the index: they always ride on top of the BSDK backbone, which pages itself.

**The rule.** A source needs the index if and only if its rows must define the pagination window
themselves, that is, when they can be the only content of the timeline.

### 2.7 Overlays: express and joint

**Express** comes from `getExpressHistory` (§2.5) as a `List<ExpressTx>` — already with the provider, the
fiat currency and both legs' assets, but **without** the on-chain leg (`txInfo = null`).

**Joint** — `FinishedJointTransfers`. It works only for `currency.isJoint` (the network's derivation path
is `Joint`), otherwise an empty list straight away. The steps: find the `Account.Joint` with that network
→ get the `chainId` through `JointNetworkReader` →
`repository.getFinishedOperations(accountId, chainId, fromFinishedAtMillis)` → for each operation decode
the Safe transaction through `JointOperationTransferResolver.resolve(...)`; operations that are not a
transfer of **this** currency are dropped (`mapNotNull`). No skew is applied to the time bound here: the
backend marks an operation finished only after its receipt, so it cannot finish earlier than its block.

The point of a separate entity: a joint operation's votes live only on the backend, so the row must exist
even when there is nothing on the chain — a cancelled operation, or a failure before the chain. The
matched on-chain leg is needed only for the block time and the fee; whether it succeeded is decided by
`FinishedJointOperation.status`, not by the presence of a transaction.

### 2.8 Merging: `mergeTxHistoryInfos`

A pure function. All the logic is four phases over a set of "claimed" on-chain rows (`claimedKeys` by
`identityKey()`, not by hash):

```
Phase 0  joint     an operation claims its relayer transaction by txHash
                   (the payload row only; a row holding nothing but the refund stays the fee it is)
Phase 1  express   deterministic match on payin_hash / payout_hash (ExpressTx.matchHash)
Phase 2  express   heuristics for the ops whose hash the provider never returned:
                   direction + address + amount + time window; on a tie, the on-chain tx
                   closest in time to the deal's creation
Phase 3  on-chain  everything unclaimed passes through as OnChainTx.BSDK
         ↓
   sortedByDescending(timestampMillis)      — the sort is stable
```

Why the heuristics exist at all: the payout leg's hash is filled in by status polling and may never
arrive. Without a match an incoming swap would be shown twice — as the standalone express row and as its
BSDK leg.

What to know if you touch the matching:

- hashes are compared lower-cased (`asHashKey()`): a provider often returns upper-case, BSDK lower-case;
  the original casing is kept on the row itself, because explorer links can be case-sensitive;
- addresses are compared case-insensitively (EVM checksum);
- amount tolerances: `0%` exact (a non-UTXO payin and a confirmed actual amount), `0.1%` for a payin on
  UTXO chains, `5%` slippage on an incoming leg, `15%` on a refund; the time window is 24 h;
- `isHiddenWhenUnmatched`: an unmatched **terminal unsuccessful** incoming swap leg is hidden (a phantom
  "you received"), a successful one is always shown, and so are the outgoing side and any onramp;
- `expressLeg()` picks the deal's payload among the rows of one hash, not the events kept out of it
  (`GaslessFee`, `YieldSupply.Exit/Send`) — otherwise a separate event would be swallowed into the
  express row and lost from the timeline.

### 2.9 Where part 2 ends

`HistoryTxListManager.state` gives `HistoryState.Content(items: List<TxHistoryInfo>, …)`. `TxHistoryModel`
subscribes to it, adds context (`lookupDataFlow`, `networkTokensFlow`), groups by date and converts each
row through `TxHistoryInfoFieldConverter` — that is already part 3.

### 2.10 Extending the read side

**A new on-chain backbone** (Gateway, say):

1. `OnChainSource.Gateway` in `HistoryTxListManager`;
2. `GatewayOnChainHistory : OnChainHistory` in `data/txhistory/list/chain` — `@AssistedInject` with
   `HistoryEnvironment`, its own pagination, its own `PaginationStatus` → `HistoryState` mapping;
3. a branch in `resolveOnChainSource()` and in `isHistoryAvailable()`;
4. the factory into `DefaultHistoryTxListManager`'s constructor and a branch in
   `when (historySources.onChainSource)`;
5. if the rows do not look like `TxInfo` — a new `OnChainTx` subtype and a merge function of its own
   (like `mergeTangemPay`).

**A new overlay on top of an existing backbone** (the way joint accounts were done):

1. a `TxHistoryInfo` subtype (or reuse an existing one);
2. an observer source in `data/txhistory/list` modelled on `FinishedJointTransfers`: a Room subscription,
   a "does it belong to this currency" filter, a time bound;
3. add it to the `combine` inside `BsdkOnChainHistory.buildState` and pass it to `mergeTxHistoryInfos`;
4. a matching phase in the merger — **before** the express phases, if the overlay outranks them for an
   on-chain row;
5. background sync — see the checklist in §1.9;
6. `history_index` — only if the overlay must be able to be the whole content of the timeline (§2.6).

**A new reason to reload the timeline** — `reload()` on the manager plus a trigger to the fetcher; the
worked example is `TxHistoryModel.reloadWithJointDelta()` with `JointHistoryPoller` (10 s, while the
screen is in the foreground and the user has not loaded additional pages).