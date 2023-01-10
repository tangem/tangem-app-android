package com.tangem.tap.common.analytics.handlers.amplitude

import com.tangem.tap.common.analytics.api.AnalyticsHandler
import com.tangem.tap.common.analytics.api.AnalyticsHandlerBuilder

class AmplitudeAnalyticsHandler(
    private val client: AmplitudeAnalyticsClient,
) : AnalyticsHandler {

    override fun id(): String = ID

    override fun send(event: String, params: Map<String, String>) {
        client.logEvent(event, params)
    }

    companion object {
        const val ID = "Amplitude"
    }

    class Builder : AnalyticsHandlerBuilder {
        override fun build(data: AnalyticsHandlerBuilder.Data): AnalyticsHandler? = when {
            !data.isDebug -> AmplitudeClient(data.application, data.config.amplitudeApiKey)
            data.isDebug && data.logConfig.amplitude -> AmplitudeLogClient(data.jsonConverter)
            else -> null
        }?.let { AmplitudeAnalyticsHandler(it) }
    }
}
