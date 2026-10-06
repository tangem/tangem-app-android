package com.tangem.features.txhistory.joint

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.isJoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlin.time.Duration.Companion.seconds

/**
 * The backend relays a joint transaction and drops the operation before the chain indexer serves it, so nothing on
 * screen says when it lands. While a joint token's details are on top, its history is reloaded every
 * [POLL_INTERVAL]; the providers serve no ETag, so every reload is a full request and the interval stays coarse.
 * A reload starts the list over from its first page, so it waits while the user has more pages loaded — a new
 * transaction lands on top anyway.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal object JointHistoryPoller {

    private val POLL_INTERVAL = 10.seconds

    fun launchIn(
        scope: CoroutineScope,
        currency: CryptoCurrency,
        isResumed: Flow<Boolean>,
        hasLoadedMorePages: Flow<Boolean>,
        reload: () -> Unit,
    ) {
        if (!currency.isJoint) return

        combine(isResumed, hasLoadedMorePages) { resumed, hasMorePages -> resumed && !hasMorePages }
            .distinctUntilChanged()
            .flatMapLatest { isPolling -> if (isPolling) ticks() else emptyFlow() }
            .onEach { reload() }
            .launchIn(scope)
    }

    private fun ticks(): Flow<Unit> = flow {
        while (currentCoroutineContext().isActive) {
            delay(POLL_INTERVAL)
            emit(Unit)
        }
    }
}