package com.tangem.tap.common.analytics.handlers.appsflyer

import com.tangem.common.json.MoshiJsonConverter
import com.tangem.tap.common.analytics.AnalyticsEventsLogger

internal class AppsFlyerLogClient(
    jsonConverter: MoshiJsonConverter,
) : AppsFlyerAnalyticsClient {

    private val logger = AnalyticsEventsLogger(AppsFlyerAnalyticsHandler.ID, jsonConverter)

    override fun logEvent(event: String, params: Map<String, String>) {
        logger.logEvent(event, params)
    }
}