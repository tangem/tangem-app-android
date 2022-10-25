package com.tangem.tap.common.analytics.handlers.appsFlyer

import com.tangem.tap.common.analytics.AnalyticsEventsLogger

/**
[REDACTED_AUTHOR]
 */
internal class AppsFlyerLogClient(
    private val logger: AnalyticsEventsLogger,
) : AppsFlyerAnalyticsClient {

    override fun logEvent(event: String, params: Map<String, String>) {
        logger.logEvent(event, params)
    }
}