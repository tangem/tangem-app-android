package com.tangem.tap.common.analytics.handlers.amplitude

import com.tangem.core.analytics.api.AnalyticsHandler
import com.tangem.core.analytics.api.AnalyticsUserIdHandler
import com.tangem.core.analytics.models.AnalyticsEvent

class AmplitudeAnalyticsHandler(
    private val client: AmplitudeAnalyticsClient,
) : AnalyticsHandler, AnalyticsUserIdHandler {

    override fun id(): String = ID
    override fun setUserId(userId: String) {
        client.setUserId(userId)
    }

    override fun clearUserId() {
        client.clearUserId()
    }

    override fun send(event: AnalyticsEvent) {
        client.logEvent(event.id, event.params)
    }

    companion object {
        const val ID = "Amplitude"
    }
}