package com.tangem.tap.common.analytics.handlers.appsFlyer

import com.tangem.tap.common.analytics.api.AnalyticsEventHandler
import com.tangem.tap.common.analytics.api.AnalyticsHandlerBuilder

class AppsFlyerAnalyticsHandler(
    private val client: AppsFlyerAnalyticsClient,
) : AnalyticsEventHandler {

    override fun id(): String = ID

    override fun send(event: String, params: Map<String, String>) {
        client.logEvent(event, params)
    }

    companion object {
        const val ID = "AppsFlyer"
    }

    class Builder : AnalyticsHandlerBuilder {
        override fun build(data: AnalyticsHandlerBuilder.Data): AnalyticsEventHandler? = when {
            !data.isDebug -> AppsFlyerClient(data.application, data.config.appsFlyerDevKey)
            data.isDebug && data.logConfig.appsFlyer -> AppsFlyerLogClient(data.jsonConverter)
            else -> null
        }?.let { AppsFlyerAnalyticsHandler(it) }
    }
}
