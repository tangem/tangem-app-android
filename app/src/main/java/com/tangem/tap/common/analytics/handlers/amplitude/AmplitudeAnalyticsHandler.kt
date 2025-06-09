package com.tangem.tap.common.analytics.handlers.amplitude

import com.tangem.core.analytics.api.AnalyticsHandler
import com.tangem.core.analytics.api.AnalyticsUserIdHandler
import com.tangem.tap.common.analytics.api.AnalyticsHandlerBuilder

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

    override fun send(eventId: String, params: Map<String, String>) {
        client.logEvent(eventId, params)
    }

    companion object {
        const val ID = "Amplitude"
    }

    class Builder : AnalyticsHandlerBuilder {
        override fun build(data: AnalyticsHandlerBuilder.Data): AnalyticsHandler {
            return AmplitudeAnalyticsHandler(
                client = if (data.logConfig.amplitude) {
                    AmplitudeLogClient(data.jsonConverter)
                } else {
                    AmplitudeClient(data.application, data.config.amplitudeApiKey)
                },
            )
        }
    }
}