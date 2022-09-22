package com.tangem.tap.common.analytics.handlers.firebase

import com.tangem.tap.common.analytics.AnalyticsEventsLogger

/**
[REDACTED_AUTHOR]
 */
internal class FirebaseLogClient(
    private val logger: AnalyticsEventsLogger,
) : FirebaseAnalyticsClient {

    override fun logEvent(event: String, params: Map<String, String>) {
        logger.logEvent(event, params)
    }

    override fun logErrorEvent(error: Throwable, params: Map<String, String>) {
        logger.logErrorEvent(error, params)
    }
}