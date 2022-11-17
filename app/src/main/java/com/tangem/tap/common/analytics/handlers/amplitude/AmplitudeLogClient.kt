package com.tangem.tap.common.analytics.handlers.amplitude

import com.tangem.common.json.MoshiJsonConverter
import com.tangem.tap.common.analytics.AnalyticsEventsLogger

/**
 * Created by Anton Zhilenkov on 22/09/2022.
 */
internal class AmplitudeLogClient(
    jsonConverter: MoshiJsonConverter,
) : AmplitudeAnalyticsClient {

    private val logger: AnalyticsEventsLogger = AnalyticsEventsLogger(AmplitudeAnalyticsHandler.ID, jsonConverter)

    override fun logEvent(event: String, params: Map<String, String>) {
        logger.logEvent(event, params)
    }
}
