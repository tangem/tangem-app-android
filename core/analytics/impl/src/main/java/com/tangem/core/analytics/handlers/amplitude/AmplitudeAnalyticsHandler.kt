package com.tangem.core.analytics.handlers.amplitude

import com.tangem.core.analytics.api.AnalyticsHandler
import com.tangem.core.analytics.api.AnalyticsUserIdHandler
import com.tangem.core.analytics.api.AnalyticsUserPropertiesHandler
import com.tangem.core.analytics.models.AnalyticsEvent

internal class AmplitudeAnalyticsHandler(
    private val client: AmplitudeAnalyticsClient,
) : AnalyticsHandler, AnalyticsUserIdHandler, AnalyticsUserPropertiesHandler {

    override fun id(): String = ID
    override fun setUserId(userId: String) {
        client.setUserId(userId)
    }

    override fun clearUserId() {
        client.clearUserId()
    }

    override fun setUserProperties(properties: Map<String, String>) {
        client.setUserProperties(properties)
    }

    override fun send(event: AnalyticsEvent) {
        client.logEvent(event.id, event.params)
    }

    companion object {
        const val ID = "Amplitude"
    }
}