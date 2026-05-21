package com.tangem.features.tokendetails

import kotlinx.coroutines.flow.Flow

interface ExpressTransactionsEventListener {

    val event: Flow<ExpressTransactionsEvent>

    suspend fun send(event: ExpressTransactionsEvent)
}

sealed interface ExpressTransactionsEvent {
    data object Update : ExpressTransactionsEvent
    data object Clear : ExpressTransactionsEvent
    data class OpenTx(val txId: String) : ExpressTransactionsEvent
}