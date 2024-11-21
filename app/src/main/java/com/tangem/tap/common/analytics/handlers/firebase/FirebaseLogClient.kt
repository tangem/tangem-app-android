package com.tangem.tap.common.analytics.handlers.firebase

import com.tangem.common.json.MoshiJsonConverter
import com.tangem.core.analytics.models.EventValue
import com.tangem.tap.common.analytics.AnalyticsEventsLogger

/**
[REDACTED_AUTHOR]
 */
internal class FirebaseLogClient(
    jsonConverter: MoshiJsonConverter,
) : FirebaseAnalyticsClient {

    private val logger: AnalyticsEventsLogger = AnalyticsEventsLogger(FirebaseAnalyticsHandler.ID, jsonConverter)

    override fun logEvent(event: String, params: Map<String, EventValue>) {
        logger.logEvent(event, params)
    }

    override fun logErrorEvent(error: Throwable, params: Map<String, EventValue>) {
        logger.logErrorEvent(error, params)
    }
}