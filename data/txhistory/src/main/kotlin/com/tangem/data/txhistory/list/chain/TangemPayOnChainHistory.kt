@file:OptIn(ExperimentalCoroutinesApi::class)

package com.tangem.data.txhistory.list.chain

import com.tangem.data.txhistory.list.mergeTangemPay
import com.tangem.domain.tangempay.model.TangemPayTxHistoryListBatchingContext
import com.tangem.domain.tangempay.model.TangemPayTxHistoryListConfig
import com.tangem.domain.tangempay.repository.TangemPayTxHistoryRepository
import com.tangem.domain.txhistory.list.HistoryTxListManager.HistoryEnvironment
import com.tangem.domain.txhistory.list.HistoryTxListManager.HistoryState
import com.tangem.domain.txhistory.model.OnChainTx
import com.tangem.domain.txhistory.model.TxHistoryInfo
import com.tangem.domain.txhistory.repository.TxHistoryRepositoryV2
import com.tangem.domain.visa.model.TangemPayTxHistoryItem
import com.tangem.pagination.BatchAction
import com.tangem.pagination.BatchListState
import com.tangem.pagination.PaginationStatus
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

private typealias TangemPayTxBatchAction = BatchAction<Int, TangemPayTxHistoryListConfig, Nothing>

/** On-chain backbone backed by the TangemPay tx history, with the express overlay windowed to the loaded page. */
internal class TangemPayOnChainHistory @AssistedInject constructor(
    private val repository: TangemPayTxHistoryRepository,
    private val txHistoryRepository: TxHistoryRepositoryV2,
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
            userWalletId = userWalletId,
            batchSize = BATCH_SIZE,
            context = TangemPayTxHistoryListBatchingContext(
                actionsFlow = actionsFlow.map { it.toBatchAction() },
                coroutineScope = env.modelScope,
            ),
        )

        return batchFlow.state
            .flatMapLatest { batchState -> buildState(batchState) }
    }

    private fun buildState(batchState: BatchListState<Int, List<TangemPayTxHistoryItem>>): Flow<HistoryState> {
        val mergedFlow: Flow<List<TxHistoryInfo>> = txHistoryRepository.getExpressHistory(
            userWalletId = userWalletId,
            currency = currency,
            fromCreatedAtMillis = oldestLoadedTimestamp(batchState),
        ).map { express ->
            val onChain = batchState.data.asSequence()
                .flatMap { it.data.asSequence() }
                .map(OnChainTx::TangemPay)
                .toList()

            mergeTangemPay(onChain = onChain, express = express)
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

    private fun Action.toBatchAction(): TangemPayTxBatchAction = when (this) {
        is Action.Reload -> BatchAction.Reload(TangemPayTxHistoryListConfig(shouldRefresh = shouldRefresh))
        Action.LoadMore -> BatchAction.LoadMore(requestParams = null)
    }

    private fun oldestLoadedTimestamp(batchState: BatchListState<Int, List<TangemPayTxHistoryItem>>): Long =
        batchState.data.asSequence()
            .flatMap { it.data.asSequence() }
            .minOfOrNull { it.date.millis }
            ?: NO_LOWER_BOUND

    private companion object {
        const val BATCH_SIZE = 50
        const val NO_LOWER_BOUND = 0L
    }

    @AssistedFactory
    interface Factory {
        fun create(env: HistoryEnvironment): TangemPayOnChainHistory
    }
}