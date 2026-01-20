package com.tangem.feature.wallet.presentation.wallet.subscribers

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import timber.log.Timber

/**
 * Component for implementation of flow subscription
 *
[REDACTED_AUTHOR]
 */
internal abstract class WalletSubscriber {

    protected abstract fun create(coroutineScope: CoroutineScope): Flow<*>

    fun subscribe(coroutineScope: CoroutineScope, dispatcher: CoroutineDispatcher): Job {
        Timber.d("Subscribe on ${this::class.simpleName}")

        return create(coroutineScope)
            .flowOn(dispatcher)
            .launchIn(coroutineScope)
    }
}