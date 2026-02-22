package com.tangem.features.tangempay

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultTangemPayMainEventListener @Inject constructor() : TangemPayMainEventListener {

    private val _event = MutableSharedFlow<TangemPayMainEvent>(
        replay = 1,
        extraBufferCapacity = 0,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val event: Flow<TangemPayMainEvent> = _event

    override suspend fun send(event: TangemPayMainEvent) {
        _event.emit(event)
    }
}