package com.tangem.features.txhistory.entity

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultTxHistoryUpdater @Inject constructor() : TxHistoryUpdateListener, TxHistoryContentUpdateEmitter {

    private val updateChannel = Channel<Unit>(Channel.BUFFERED)
    override val updates: Flow<Unit> = updateChannel.receiveAsFlow()

    override suspend fun triggerUpdate() {
        updateChannel.send(Unit)
    }
}