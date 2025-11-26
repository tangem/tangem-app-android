package com.tangem.features.tangempay.utils

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class TangemPayTxHistoryUpdateListener @Inject constructor() {

    private val updateChannel = Channel<Unit>(Channel.BUFFERED)
    val updates: Flow<Unit> = updateChannel.receiveAsFlow()

    suspend fun triggerUpdate() {
        updateChannel.send(Unit)
    }
}