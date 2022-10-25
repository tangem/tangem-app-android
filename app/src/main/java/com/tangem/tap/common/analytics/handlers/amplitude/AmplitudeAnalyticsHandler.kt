package com.tangem.tap.common.analytics.handlers.amplitude

import com.tangem.tap.common.analytics.api.AnalyticsEventHandler

class AmplitudeAnalyticsHandler(
    private val client: AmplitudeAnalyticsClient,
) : AnalyticsEventHandler {

    override fun handleEvent(event: String, params: Map<String, String>) {
        client.logEvent(event, params)
    }
}