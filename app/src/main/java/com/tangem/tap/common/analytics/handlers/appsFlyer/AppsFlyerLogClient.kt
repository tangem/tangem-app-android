package com.tangem.tap.common.analytics.handlers.appsFlyer

import com.tangem.common.json.MoshiJsonConverter
import com.tangem.tap.common.analytics.AnalyticsEventsLogger

/**
 * Created by Anton Zhilenkov on 22/09/2022.
 */
internal class AppsFlyerLogClient(
    jsonConverter: MoshiJsonConverter,
) : AppsFlyerAnalyticsClient {

    private val logger = AnalyticsEventsLogger(AppsFlyerAnalyticsHandler.ID, jsonConverter)

    override fun logEvent(event: String, params: Map<String, String>) {
        logger.logEvent(event, params)
    }
}
