package com.tangem.data.txhistory.list.chain

import com.tangem.data.txhistory.list.mergeTxHistoryInfos
import com.tangem.domain.txhistory.list.HistoryTxListManager.HistoryEnvironment
import com.tangem.domain.txhistory.list.HistoryTxListManager.HistoryState
import com.tangem.domain.txhistory.repository.ExpressHistoryPage
import com.tangem.domain.txhistory.repository.TxHistoryRepositoryV2
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

/**
 * Backbone for currencies without an on-chain history source: paginates the express (swap/onramp) overlay itself,
 * using the unified history index. There is no on-chain leg, so every row is a standalone express item.
 */
internal class IndexTableOnChainHistory @AssistedInject constructor(
    private val repository: TxHistoryRepositoryV2,
    @Assisted private val env: HistoryEnvironment,
) : OnChainHistory {

    override val actions: Channel<Action> = Channel()

    override fun history(): Flow<HistoryState> = channelFlow {
        val limit = MutableStateFlow(PAGE_SIZE)
        actions.receiveAsFlow()
            .onEach { action ->
                when (action) {
                    is Action.Reload -> limit.value = PAGE_SIZE
                    Action.LoadMore -> limit.update { it + PAGE_SIZE }
                }
            }
            .launchIn(this)

        limit
            .flatMapLatest { pageLimit ->
                repository.getIndexedExpressHistory(
                    env.userWalletId,
                    env.currency,
                    pageLimit,
                )
            }
            .map { page -> buildState(page) }
            .collect { send(it) }
    }

    private fun buildState(page: ExpressHistoryPage): HistoryState {
        val merged = mergeTxHistoryInfos(
            onChain = emptyList(),
            express = page.items,
            currency = env.currency,
        )
        return if (merged.isEmpty()) {
            HistoryState.Empty
        } else {
            HistoryState.Content(merged, isLoadingMore = false, hasMore = page.hasMore)
        }
    }

    private companion object {
        const val PAGE_SIZE = 50
    }

    @AssistedFactory
    interface Factory {
        fun create(env: HistoryEnvironment): IndexTableOnChainHistory
    }
}