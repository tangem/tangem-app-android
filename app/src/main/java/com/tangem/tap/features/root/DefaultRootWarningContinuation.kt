package com.tangem.tap.features.root

import kotlinx.coroutines.channels.Channel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultRootWarningContinuation @Inject constructor() : RootWarningContinuation {

    private val dismissals = Channel<Unit>(capacity = Channel.CONFLATED)

    override suspend fun awaitDismiss() {
        dismissals.receive()
    }

    override fun dismiss() {
        dismissals.trySend(Unit)
    }
}