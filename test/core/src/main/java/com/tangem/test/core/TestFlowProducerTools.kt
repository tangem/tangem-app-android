package com.tangem.test.core

import com.tangem.domain.core.flow.FlowProducer
import com.tangem.domain.core.flow.FlowProducerTools
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.shareIn

/**
 * Test implementation of [FlowProducerTools] that mirrors the production `DefaultFlowProducerTools`
 * behaviour (retryWhen + fallback + distinctUntilChanged + shareIn) on a caller-provided test
 * scope/dispatcher, without analytics/logging.
 *
 * Pass a [TestScope.backgroundScope] and a `StandardTestDispatcher`/`UnconfinedTestDispatcher` built
 * from the test scheduler so virtual time (e.g. the 2s retry delay) is controllable.
 *
[REDACTED_AUTHOR]
 */
class TestFlowProducerTools(
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher,
) : FlowProducerTools {

    override fun <T> shareInProducer(
        flow: Flow<T>,
        flowProducer: FlowProducer<T>,
        withRetryWhen: Boolean,
    ): SharedFlow<T> {
        var upstream = flow

        if (withRetryWhen) {
            upstream = upstream.retryWhen { _, _ ->
                flowProducer.fallback.onSome { emit(it) }
                delay(timeMillis = 2000)
                true
            }
        }

        return upstream
            .flowOn(dispatcher)
            .distinctUntilChanged()
            .shareIn(
                scope = scope,
                replay = 1,
                started = SharingStarted.WhileSubscribed(
                    stopTimeoutMillis = 0,
                    replayExpirationMillis = 0,
                ),
            )
    }
}