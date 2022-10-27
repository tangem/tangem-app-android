package com.tangem.tap.common.analytics.handlers.amplitude

import com.tangem.tap.common.analytics.api.AnalyticsEventHandler
import com.tangem.tap.common.analytics.api.AnalyticsHandlerBuilder

class AmplitudeAnalyticsHandler(
    private val client: AmplitudeAnalyticsClient,
) : AnalyticsEventHandler {

    override fun id(): String = ID

    override fun send(event: String, params: Map<String, String>) {
        client.logEvent(event, params)
    }

    companion object {
        const val ID = "Amplitude"
    }

    class Builder : AnalyticsHandlerBuilder {
        override fun build(data: AnalyticsHandlerBuilder.Data): AnalyticsEventHandler? = when {
            !data.isDebug -> AmplitudeClient(data.application, data.config.amplitudeApiKey)
            data.isDebug && data.logConfig.appsFlyer -> AmplitudeLogClient(data.jsonConverter)
            else -> null
        }?.let { AmplitudeAnalyticsHandler(it) }
    }
}