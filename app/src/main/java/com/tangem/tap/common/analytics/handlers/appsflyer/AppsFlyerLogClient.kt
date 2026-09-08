package com.tangem.tap.common.analytics.handlers.appsflyer

import com.tangem.tap.common.analytics.AnalyticsEventsLogger

internal class AppsFlyerLogClient : AppsFlyerAnalyticsClient {

    private val logger = AnalyticsEventsLogger(AppsFlyerAnalyticsHandler.ID)

    override fun logEvent(event: String, params: Map<String, String>) {
        logger.logEvent(event, params)
    }

    override fun setUserId(userId: String) {
        // No-op
    }

    override fun clearUserId() {
        // No-op
    }
}