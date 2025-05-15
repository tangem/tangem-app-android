package com.tangem.tap.common.analytics.handlers.firebase

import com.tangem.common.json.MoshiJsonConverter
import com.tangem.tap.common.analytics.AnalyticsEventsLogger

/**
[REDACTED_AUTHOR]
 */
internal class FirebaseLogClient(
    jsonConverter: MoshiJsonConverter,
) : FirebaseAnalyticsClient {

    private val logger: AnalyticsEventsLogger = AnalyticsEventsLogger(FirebaseAnalyticsHandler.ID, jsonConverter)
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