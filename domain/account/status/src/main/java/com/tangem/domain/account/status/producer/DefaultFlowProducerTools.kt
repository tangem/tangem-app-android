package com.tangem.domain.account.status.producer

import com.tangem.core.analytics.api.AnalyticsExceptionHandler
import com.tangem.core.analytics.models.ExceptionAnalyticsEvent
import com.tangem.domain.core.flow.FlowProducer
import com.tangem.domain.core.flow.FlowProducerTools
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import javax.inject.Inject

class DefaultFlowProducerTools @Inject constructor(
    private val scope: AppCoroutineScope,
    private val dispatchers: CoroutineDispatcherProvider,
    private val analyticsExceptionHandler: AnalyticsExceptionHandler,
) : FlowProducerTools {

    override fun <T> shareInProducer(
        flow: Flow<T>,
        flowProducer: FlowProducer<T>,
        withRetryWhen: Boolean,
    ): SharedFlow<T> {
        var upstream = flow

        if (withRetryWhen) {
            val flowProducerName = flowProducer.javaClass.simpleName
            upstream = upstream
                .retryWhen { cause: Throwable, attempt: Long ->
                    logError(cause, flowProducerName, attempt)
                    flowProducer.fallback.onSome { this.emit(value = it) }
                    delay(timeMillis = 2000)

                    true
                }
        }

        return upstream
            .flowOn(dispatchers.default)
            .distinctUntilChanged()
            .shareIn(
                scope = scope,
                replay = 1,
                // stopTimeoutMillis = 0: upstream collection stops immediately when the last subscriber disappears.
                // replayExpirationMillis = 0: replay cache is cleared immediately after upstream stops.
                // This ensures that when there are no subscribers, the first subscriber always triggers a fresh
                // upstream collection instead of receiving a stale replay.
                started = SharingStarted.WhileSubscribed(
                    stopTimeoutMillis = 0,
                    replayExpirationMillis = 0,
                ),
            )
    }

    private fun logError(cause: Throwable, flowProducerName: String, attempt: Long) {
        val tag = "FlowProducerRetryWhen"
        TangemLogger.withTag(tag)
            .e("flowProducerName $flowProducerName attempt $attempt", cause)

        val event = ExceptionAnalyticsEvent(
            exception = cause,
            params = mapOf(
                "source" to tag,
                "flowProducerName" to flowProducerName,
            ),
        )
        analyticsExceptionHandler.sendException(event)
    }
}