package com.tangem.feature.wallet.presentation.wallet.subscribers

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import com.tangem.utils.logging.TangemLogger

/**
 * Component for implementation of flow subscription
 *
[REDACTED_AUTHOR]
 */
internal abstract class WalletSubscriber {

    protected abstract fun create(coroutineScope: CoroutineScope): Flow<*>

    fun subscribe(coroutineScope: CoroutineScope, dispatchers: CoroutineDispatcher): Job {
        TangemLogger.d("Subscribe on ${this::class.simpleName}")

        return create(coroutineScope)
            .flowOn(dispatchers)
            .launchIn(coroutineScope)
    }
}