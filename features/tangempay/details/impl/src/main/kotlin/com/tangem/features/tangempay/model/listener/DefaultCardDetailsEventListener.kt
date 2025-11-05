package com.tangem.features.tangempay.model.listener

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultCardDetailsEventListener @Inject constructor() : CardDetailsEventListener {

    private val _event = MutableSharedFlow<CardDetailsEvent>(
        replay = 1,
        extraBufferCapacity = 0,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val event: Flow<CardDetailsEvent> = _event

    override suspend fun send(event: CardDetailsEvent) {
        _event.emit(event)
    }
}