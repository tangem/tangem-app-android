package com.tangem.features.forceupdate.impl

import com.tangem.features.forceupdate.ForceUpdateContinuation
import kotlinx.coroutines.channels.Channel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultForceUpdateContinuation @Inject constructor() : ForceUpdateContinuation {

    private val dismissals = Channel<Unit>(capacity = Channel.CONFLATED)

    override suspend fun awaitDismiss() {
        dismissals.receive()
    }

    override fun dismiss() {
        dismissals.trySend(Unit)
    }
}