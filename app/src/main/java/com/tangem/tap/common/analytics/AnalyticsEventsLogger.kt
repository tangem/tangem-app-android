package com.tangem.tap.common.analytics

import com.tangem.common.json.MoshiJsonConverter
import com.tangem.core.analytics.api.ErrorEventLogger
import com.tangem.core.analytics.api.EventLogger
import timber.log.Timber

class AnalyticsEventsLogger(
    private val name: String,
    private val jsonConverter: MoshiJsonConverter,
) : EventLogger, ErrorEventLogger {

    override fun logEvent(event: String, params: Map<String, String>) {
        Timber.d(jsonConverter.prettyPrint(PrintEventModel(name, event, params)))
    }

    override fun logErrorEvent(error: Throwable, params: Map<String, String>) {
        Timber.e(error, jsonConverter.prettyPrint(PrintEventModel(name, "error", params)))
    }
}

private data class PrintEventModel(
    val client: String,
    val event: String,
    val params: Map<String, String>,
)
