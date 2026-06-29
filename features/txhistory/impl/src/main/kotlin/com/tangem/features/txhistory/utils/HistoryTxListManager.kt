package com.tangem.features.txhistory.utils

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.txhistory.model.*
import com.tangem.domain.txhistory.models.PaginationWrapper
import com.tangem.domain.txhistory.repository.TxHistoryRepositoryV2
import com.tangem.pagination.BatchAction
import com.tangem.pagination.BatchListState
import com.tangem.pagination.PaginationStatus
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*

private typealias HistoryTxBatchAction = BatchAction<Int, TxHistoryListConfig, Nothing>

/**
 * Redesign-only transaction-history pipeline that merges the on-chain pagination backbone with the
 * express (swap/onramp) overlay. Unlike [TxHistoryListManager] there is no legacy branch.
 *
 * The express overlay is asset-scoped and time-windowed to the oldest loaded on-chain timestamp
 * (re-subscribed via [flatMapLatest] as more pages load) and re-emits live as the express DB updates,
 * so status changes render without depending on the transaction count.
 */
@Suppress("LongParameterList")
internal class HistoryTxListManager @AssistedInject constructor(
    private val repository: TxHistoryRepositoryV2,
    private val dispatchers: CoroutineDispatcherProvider,
    @Assisted private val userWalletId: UserWalletId,
    @Assisted private val currency: CryptoCurrency,
) {

    private val jobHolder = JobHolder()
    private val actionsFlow: MutableSharedFlow<HistoryTxBatchAction> = MutableSharedFlow(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val state: MutableStateFlow<State> = MutableStateFlow(State())

    val items: Flow<List<TxHistoryInfo>> = state
        .filter { it.hasContent }
        .map { it.items }
        .distinctUntilChanged()

    val paginationStatus: Flow<PaginationStatus<*>> = state.map { it.status }.distinctUntilChanged()

    /**
     * Reactive stream of a single row tracked by its [TxHistoryInfo.txId], for the in-app details sheet.
     *
     * Seeded with the tapped [item] so the sheet always has an immediate snapshot, then re-emits the matching row from
     * the live merged list as its status changes. The seed also covers rows not present in [items] yet (e.g. a pending
     * tx surfaced from the currency status), which would otherwise never resolve.
     */
    fun txHistoryInfoFlow(item: TxHistoryInfo): Flow<TxHistoryInfo> = items
        .mapNotNull { list -> list.firstOrNull { it.txId == item.txId } }
        .onStart { emit(item) }
        .distinctUntilChanged()

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun init() {
        coroutineScope {
            val batchFlow = repository.getTxHistoryBatchFlow(
                context = TxHistoryListBatchingContext(actionsFlow = actionsFlow, coroutineScope = this),
                batchSize = BATCH_SIZE,
            )

            val sharedBatchState = batchFlow.state.shareIn(scope = this, started = SharingStarted.Eagerly, replay = 1)

            val expressFlow = sharedBatchState
                .map(::oldestLoadedTimestamp)
                .distinctUntilChanged()
                .flatMapLatest { fromCreatedAtMillis ->
                    repository.getExpressHistory(userWalletId, currency, fromCreatedAtMillis)
                }
                // Let the merge run on the first on-chain emission before the express query resolves.
                .onStart { emit(emptyList()) }

            combine(sharedBatchState, expressFlow) { batchState, express ->
                buildState(batchState, express)
            }
                .flowOn(dispatchers.default)
                .onEach { state.value = it }
                .launchIn(scope = this)
                .saveIn(jobHolder)
        }
    }

    suspend fun startLoading() {
        actionsFlow.emit(
            BatchAction.Reload(requestParams = TxHistoryListConfig(userWalletId, currency, shouldRefresh = false)),
        )
    }

    suspend fun reload() {
        actionsFlow.emit(
            BatchAction.Reload(requestParams = TxHistoryListConfig(userWalletId, currency, shouldRefresh = true)),
        )
    }

    suspend fun loadMore(userWalletId: UserWalletId, currency: CryptoCurrency) {
        actionsFlow.emit(
            BatchAction.LoadMore(requestParams = TxHistoryListConfig(userWalletId, currency, shouldRefresh = false)),
        )
    }

    private fun buildState(
        batchState: BatchListState<Int, PaginationWrapper<TxInfo>>,
        express: List<ExpressTx>,
    ): State {
        val onChain = batchState.data.asSequence()
            .flatMap { it.data.items.asSequence() }
            .distinctBy(TxInfo::identityKey)
            .toList()

        val merged = mergeTxHistoryInfos(onChain = onChain, express = express)

        return State(status = batchState.status, items = merged)
    }

    private fun oldestLoadedTimestamp(batchState: BatchListState<Int, PaginationWrapper<TxInfo>>): Long =
        batchState.data.asSequence()
            .flatMap { it.data.items.asSequence() }
            .minOfOrNull { it.timestampInMillis }
            ?: NO_LOWER_BOUND

    private data class State(
        val status: PaginationStatus<*> = PaginationStatus.None,
        val items: List<TxHistoryInfo> = emptyList(),
    ) {
        val hasContent: Boolean
            get() = status !is PaginationStatus.None &&
                status !is PaginationStatus.InitialLoading &&
                status !is PaginationStatus.InitialLoadingError
    }

    private companion object {
        const val BATCH_SIZE = 50
        const val NO_LOWER_BOUND = 0L
    }

    @AssistedFactory
    interface Factory {
        fun create(userWalletId: UserWalletId, currency: CryptoCurrency): HistoryTxListManager
    }
}