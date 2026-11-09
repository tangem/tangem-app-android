package com.tangem.domain.tangempay.order

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class RunningOrder(
    val ref: OrderRef,
    val state: Flow<OrderPollingState>,
)

suspend fun RunningOrder.awaitTerminal(): OrderPollingState {
    return state.first { it.isTerminal }
}