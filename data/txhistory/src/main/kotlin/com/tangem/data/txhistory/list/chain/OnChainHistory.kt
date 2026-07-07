package com.tangem.data.txhistory.list.chain

import com.tangem.domain.txhistory.list.HistoryTxListManager.HistoryState
import com.tangem.pagination.BatchAction
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow

/** One history backbone (BSDK / TangemPay / index-backed express): consumes [Action]s and emits [HistoryState]. */
internal interface OnChainHistory {
    val actions: Channel<Action>
    fun sendAction(action: Action) = actions.trySend(action)

    fun history(): Flow<HistoryState>
}

/** Neutral external action, decoupled from the pagination [BatchAction]. */
internal sealed interface Action {
    data class Reload(val shouldRefresh: Boolean) : Action
    data object LoadMore : Action
}