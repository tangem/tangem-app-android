package com.tangem.tap.common.analytics.handlers.amplitude

import com.tangem.common.json.MoshiJsonConverter
import com.tangem.tap.common.analytics.AnalyticsEventsLogger

/**
[REDACTED_AUTHOR]
 */
internal class AmplitudeLogClient(
    jsonConverter: MoshiJsonConverter,
) : AmplitudeAnalyticsClient {

    private val logger: AnalyticsEventsLogger = AnalyticsEventsLogger(AmplitudeAnalyticsHandler.ID, jsonConverter)

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
}