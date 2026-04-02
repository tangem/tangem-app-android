package com.tangem.tap.common.analytics

import com.tangem.common.json.MoshiJsonConverter
import com.tangem.core.analytics.api.EventLogger
import com.tangem.core.analytics.api.ExceptionLogger
import com.tangem.utils.logging.TangemLogger

class AnalyticsEventsLogger(
    private val name: String,
    private val jsonConverter: MoshiJsonConverter,
) : EventLogger, ExceptionLogger {

    override fun logEvent(event: String, params: Map<String, String>) {
        TangemLogger.d(jsonConverter.prettyPrint(PrintEventModel(name, event, params)))
    }

    override fun logException(error: Throwable, params: Map<String, String>) {
        TangemLogger.e(jsonConverter.prettyPrint(PrintEventModel(name, "error", params)), error)
    }
}

private data class PrintEventModel(
    val client: String,
    val event: String,
    val params: Map<String, String>,
)