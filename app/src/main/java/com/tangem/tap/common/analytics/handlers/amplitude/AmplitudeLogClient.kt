package com.tangem.tap.common.analytics.handlers.amplitude

import com.tangem.tap.common.analytics.AnalyticsEventsLogger

/**
[REDACTED_AUTHOR]
 */
internal class AmplitudeLogClient(
    private val logger: AnalyticsEventsLogger,
) : AmplitudeAnalyticsClient {

    override fun logEvent(event: String, params: Map<String, String>) {
        logger.logEvent(event, params)
    }
}