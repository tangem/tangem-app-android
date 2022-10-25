package com.tangem.tap.common.analytics.handlers.amplitude

import com.tangem.common.card.Card
import com.tangem.tap.common.analytics.AnalyticsEventAnOld
import com.tangem.tap.common.analytics.api.AnalyticsEventHandler

class AmplitudeAnalyticsHandler(
    private val client: AmplitudeAnalyticsClient,
) : AnalyticsEventHandler {

    override fun handleEvent(event: String, params: Map<String, String>) {
        client.logEvent(event, params)
    }

    override fun handleAnalyticsEvent(
        event: AnalyticsEventAnOld,
        params: Map<String, String>,
        card: Card?,
        blockchain: String?,
    ) {
        handleEvent(event.event, prepareParams(card, blockchain, params))
    }
}
