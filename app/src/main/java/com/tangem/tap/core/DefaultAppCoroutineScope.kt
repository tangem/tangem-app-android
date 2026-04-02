package com.tangem.tap.core

import com.tangem.core.analytics.api.AnalyticsExceptionHandler
import com.tangem.core.analytics.models.ExceptionAnalyticsEvent
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

internal class DefaultAppCoroutineScope @Inject constructor(
    dispatchers: CoroutineDispatcherProvider,
    private val analyticsExceptionHandler: AnalyticsExceptionHandler,
) : AppCoroutineScope {

    private val tag = "AppCoroutineScope"

    override val coroutineContext: CoroutineContext = SupervisorJob() +
        // keep IO dispatcher to avoid blocking Default with IO operations
        dispatchers.io +
        CoroutineName(tag) +
        CoroutineExceptionHandler { context, throwable ->
            val coroutineName = context[CoroutineName]?.name.orEmpty()
            logError(throwable, coroutineName)
        }

    private fun logError(throwable: Throwable, coroutineName: String) {
        TangemLogger.withTag(tag).e(
            messageString = "CoroutineName $coroutineName",
            throwable = throwable,
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