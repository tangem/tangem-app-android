package com.tangem.feature.tokendetails.presentation.tokendetails.state.utils

import com.tangem.features.tokendetails.ExpressTransactionsEvent
import com.tangem.features.tokendetails.ExpressTransactionsEventListener
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultExpressTransactionsEventListener @Inject constructor() : ExpressTransactionsEventListener {

    private val _event = MutableSharedFlow<ExpressTransactionsEvent>(
        replay = 1,
        extraBufferCapacity = 0,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val event: Flow<ExpressTransactionsEvent> = _event

    override suspend fun send(event: ExpressTransactionsEvent) {
        _event.emit(event)
    }
}