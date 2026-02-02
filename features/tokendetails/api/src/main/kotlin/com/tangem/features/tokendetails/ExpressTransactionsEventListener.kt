package com.tangem.features.tokendetails

import kotlinx.coroutines.flow.Flow

interface ExpressTransactionsEventListener {

    val event: Flow<ExpressTransactionsEvent>

    suspend fun send(event: ExpressTransactionsEvent)
}

enum class ExpressTransactionsEvent {
    Update, Clear
}