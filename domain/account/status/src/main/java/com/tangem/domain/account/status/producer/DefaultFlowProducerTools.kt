package com.tangem.domain.account.status.producer

import com.tangem.core.analytics.api.AnalyticsExceptionHandler
import com.tangem.core.analytics.models.ExceptionAnalyticsEvent
import com.tangem.domain.core.flow.FlowProducer
import com.tangem.domain.core.flow.FlowProducerScope
import com.tangem.domain.core.flow.FlowProducerTools
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class DefaultFlowProducerAppScope @Inject constructor(
    private val dispatchers: CoroutineDispatcherProvider,
    private val analyticsExceptionHandler: AnalyticsExceptionHandler,
) : FlowProducerScope {

    private val tag = "FlowProducerScope"

    override val coroutineContext: CoroutineContext = SupervisorJob() +
        dispatchers.default +
        CoroutineName(tag) +
        CoroutineExceptionHandler { context, throwable ->
            @Suppress("NullableToStringCall")
            val coroutineName = context[CoroutineName]?.name.toString()
            logError(throwable, coroutineName)
        }

    private fun logError(throwable: Throwable, coroutineName: String) {
        Timber.tag("FlowProducerExceptionHandler").e(
            throwable,
            "CoroutineName $coroutineName",
        )
        val event = ExceptionAnalyticsEvent(
            exception = throwable,
            params = mapOf(
                "source" to tag,
                "coroutineName" to coroutineName,
            ),
        )
        analyticsExceptionHandler.sendException(event)
    }
}

class DefaultFlowProducerTools @Inject constructor(
    private val scope: FlowProducerScope,
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
            .distinctUntilChanged()
            .shareIn(
                scope = scope,
                replay = 1,
                // params control flow cleanup
                started = SharingStarted.WhileSubscribed(
                    stopTimeoutMillis = 5_000,
                    replayExpirationMillis = 30_000,
                ),
            )
    }

    private fun logError(cause: Throwable, flowProducerName: String, attempt: Long) {
        val tag = "FlowProducerRetryWhen"
        Timber.tag(tag)
            .e(cause, "flowProducerName $flowProducerName attempt $attempt")

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