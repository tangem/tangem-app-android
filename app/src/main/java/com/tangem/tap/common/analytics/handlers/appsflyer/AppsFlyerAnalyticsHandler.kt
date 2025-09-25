package com.tangem.tap.common.analytics.handlers.appsflyer

import com.tangem.core.analytics.api.AnalyticsHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.tap.common.analytics.api.AnalyticsHandlerBuilder

class AppsFlyerAnalyticsHandler(
    private val client: AppsFlyerAnalyticsClient,
) : AnalyticsHandler {

    override fun id(): String = ID

    override fun send(eventId: String, params: Map<String, String>) {
        client.logEvent(eventId, params)
    }

    override fun send(event: AnalyticsEvent) {
        super.send(event)
    }

    companion object {
        const val ID = "AppsFlyer"
    }

    class Builder : AnalyticsHandlerBuilder {
        override fun build(data: AnalyticsHandlerBuilder.Data): AnalyticsHandler? = when {
            !data.isDebug -> AppsFlyerClient(data.application, data.config.appsFlyerApiKey, data.config.appsAppId)
            data.isDebug && data.logConfig.appsflyer -> AppsFlyerLogClient(data.jsonConverter)
            else -> null
        }?.let { AppsFlyerAnalyticsHandler(it) }
    }
}