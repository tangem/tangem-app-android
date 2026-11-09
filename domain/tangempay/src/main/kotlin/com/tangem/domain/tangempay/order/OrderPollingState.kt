package com.tangem.domain.tangempay.order

import com.tangem.domain.tangempay.model.TangemPayOrder
import com.tangem.domain.tangempay.order.OrderPollingState.*

sealed interface OrderPollingState {

    data object Pending : OrderPollingState

    data class Loaded(
        val order: TangemPayOrder,
    ) : OrderPollingState

    data object NotFound : OrderPollingState

    data object Stopped : OrderPollingState
}

val OrderPollingState.isTerminal: Boolean
    get() = when (this) {
        Pending -> false
        is Loaded -> order.status.isTerminal
        NotFound, Stopped -> true
    }

val OrderPollingState.isCompleted: Boolean
    get() = this is Loaded && order.status == TangemPayOrder.Status.COMPLETED