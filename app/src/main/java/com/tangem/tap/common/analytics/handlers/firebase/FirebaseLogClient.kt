package com.tangem.tap.common.analytics.handlers.firebase

import com.tangem.tap.common.analytics.AnalyticsEventsLogger

/**
[REDACTED_AUTHOR]
 */
internal class FirebaseLogClient : FirebaseAnalyticsClient {

    private val logger: AnalyticsEventsLogger = AnalyticsEventsLogger(FirebaseAnalyticsHandler.ID)
    private var userId: String? = null

    override fun setUserId(userId: String) {
        this.userId = userId
    }

    override fun clearUserId() {
        this.userId = null
    }

    override fun logEvent(event: String, params: Map<String, String>) {
        logger.logEvent(event, params)
    }

    override fun logException(error: Throwable, params: Map<String, String>) {
        logger.logException(error, params)
    }
}