package com.tangem.core.analytics.handlers.appsflyer

import com.tangem.core.analytics.AnalyticsEventsLogger

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