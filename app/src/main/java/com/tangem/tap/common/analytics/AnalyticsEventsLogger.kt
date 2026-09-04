package com.tangem.tap.common.analytics

import com.tangem.core.analytics.api.EventLogger
import com.tangem.core.analytics.api.ExceptionLogger
import com.tangem.utils.logging.TangemLogger

class AnalyticsEventsLogger(
    private val name: String,
) : EventLogger, ExceptionLogger {

    override fun logEvent(event: String, params: Map<String, String>) {
        TangemLogger.d(format(event = event, params = params))
    }

    override fun logException(error: Throwable, params: Map<String, String>) {
        TangemLogger.e(format(event = "error", params = params), error)
    }

    private fun format(event: String, params: Map<String, String>): String = buildString {
        append(name).append(" — ").append(event)
        if (params.isNotEmpty()) {
            append(params.entries.joinToString(prefix = "\n", separator = "\n") { "  ${it.key}: ${it.value}" })
        }
    }
}