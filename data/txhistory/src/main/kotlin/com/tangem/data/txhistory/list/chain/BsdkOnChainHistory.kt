@file:OptIn(ExperimentalCoroutinesApi::class)

package com.tangem.data.txhistory.list.chain

import com.tangem.data.txhistory.list.chain.BsdkOnChainHistory.Companion.AUTO_LOAD_MORE_TARGET_COUNT
import com.tangem.data.txhistory.list.mergeTxHistoryInfos
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.txhistory.list.HistoryTxListManager.HistoryEnvironment
import com.tangem.domain.txhistory.list.HistoryTxListManager.HistoryState
import com.tangem.domain.txhistory.model.TxHistoryInfo
import com.tangem.domain.txhistory.model.TxHistoryListBatchingContext
import com.tangem.domain.txhistory.model.TxHistoryListConfig
import com.tangem.domain.txhistory.model.identityKey
import com.tangem.domain.txhistory.models.PaginationWrapper
import com.tangem.domain.txhistory.repository.TxHistoryRepositoryV2
import com.tangem.pagination.BatchAction
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.BatchListState
import com.tangem.pagination.PaginationStatus
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

private typealias HistoryTxBatchAction = BatchAction<Int, TxHistoryListConfig, Nothing>

/** On-chain backbone backed by the blockchain SDK history, with the express overlay windowed to the loaded page. */
internal class BsdkOnChainHistory @AssistedInject constructor(
    private val repository: TxHistoryRepositoryV2,
    @Assisted private val env: HistoryEnvironment,
) : OnChainHistory {
    private val userWalletId get() = env.userWalletId
    private val currency get() = env.currency

    override val actions: Channel<Action> = Channel()

    override fun history(): Flow<HistoryState> {
        val actionsFlow = flow {
            // initial load
            emit(Action.Reload(shouldRefresh = false))
            // receive user actions
            actions.receiveAsFlow().collect { emit(it) }
        }
        val batchFlow = repository.getTxHistoryBatchFlow(
            context = TxHistoryListBatchingContext(
                actionsFlow = actionsFlow.map { it.toBatchAction() },
                coroutineScope = env.modelScope,
            ),
            batchSize = BATCH_SIZE,
        )

        return batchFlow.state
            .onEach { batchState -> autoLoadMoreUntilScrollable(batchState) }
            .flatMapLatest { batchState -> buildState(batchState) }
    }

    /**
     * Keeps requesting the next page while the loaded on-chain backbone is too short to make the list scrollable
     * (fewer than [AUTO_LOAD_MORE_TARGET_COUNT] items) or the last page came back empty. Runs only in the
     * [PaginationStatus.Paginating] state, so it stops as soon as the backbone reaches [PaginationStatus.EndOfPagination].
     */
    private fun autoLoadMoreUntilScrollable(batchState: BatchListState<Int, PaginationWrapper<TxInfo>>) {
        val status = batchState.status as? PaginationStatus.Paginating ?: return
        val lastResult = status.lastResult as? BatchFetchResult.Success ?: return
        val loadedItemsCount = batchState.data.sumOf { batch -> batch.data.items.size }
        val shouldLoadMore = loadedItemsCount < AUTO_LOAD_MORE_TARGET_COUNT || lastResult.empty
        if (shouldLoadMore) {
            sendAction(Action.LoadMore)
        }
    }

    private fun buildState(batchState: BatchListState<Int, PaginationWrapper<TxInfo>>): Flow<HistoryState> {
        val mergedFlow: Flow<List<TxHistoryInfo>> = repository.getExpressHistory(
            userWalletId = userWalletId,
            currency = currency,
            fromCreatedAtMillis = oldestLoadedTimestamp(batchState),
        ).map { express ->
            val onChain = batchState.data.asSequence()
                .flatMap { it.data.items.asSequence() }
                .distinctBy(TxInfo::identityKey)
                .toList()
            mergeTxHistoryInfos(onChain = onChain, express = express, currency = currency)
        }

        return when (batchState.status) {
            PaginationStatus.None,
            PaginationStatus.InitialLoading,
            -> flowOf(HistoryState.Loading)
            // Terminal initial failure: surface Error so the user can retry the on-chain page load.
            is PaginationStatus.InitialLoadingError -> flowOf(HistoryState.Error)
            PaginationStatus.NextBatchLoading -> mergedFlow.map { merged ->
                HistoryState.Content(merged, isLoadingMore = true, hasMore = true)
            }
            is PaginationStatus.Paginating<*> -> mergedFlow.map { merged ->
                if (merged.isEmpty()) {
                    HistoryState.Empty
                } else {
                    HistoryState.Content(merged, isLoadingMore = false, hasMore = true)
                }
            }
            PaginationStatus.EndOfPagination -> mergedFlow.map { merged ->
                if (merged.isEmpty()) {
                    HistoryState.Empty
                } else {
                    HistoryState.Content(merged, isLoadingMore = false, hasMore = false)
                }
            }
        }
    }

    private fun Action.toBatchAction(): HistoryTxBatchAction = when (this) {
        is Action.Reload -> BatchAction.Reload(
            TxHistoryListConfig(userWalletId, currency, shouldRefresh = shouldRefresh),
        )
        Action.LoadMore -> BatchAction.LoadMore(TxHistoryListConfig(userWalletId, currency, shouldRefresh = false))
    }

    private fun oldestLoadedTimestamp(batchState: BatchListState<Int, PaginationWrapper<TxInfo>>): Long =
        batchState.data.asSequence()
            .flatMap { it.data.items.asSequence() }
            .minOfOrNull { it.timestampInMillis }
            ?: NO_LOWER_BOUND

    private companion object {
        const val BATCH_SIZE = 50
        const val NO_LOWER_BOUND = 0L

        /** Number of loaded items considered enough to make the list scrollable. */
        const val AUTO_LOAD_MORE_TARGET_COUNT = 20
    }

    @AssistedFactory
    interface Factory {
        fun create(env: HistoryEnvironment): BsdkOnChainHistory
    }
}