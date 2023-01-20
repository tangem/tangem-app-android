package com.tangem.tap.common.analytics.handlers.appsFlyer

import com.appsflyer.AFInAppEventType
import com.tangem.core.analytics.AnalyticsEvent
import com.tangem.core.analytics.api.AnalyticsHandler
import com.tangem.tap.common.analytics.api.AnalyticsHandlerBuilder
import com.tangem.tap.common.analytics.events.Shop

class AppsFlyerAnalyticsHandler(
    private val client: AppsFlyerAnalyticsClient,
) : AnalyticsHandler {

    override fun id(): String = ID

    override fun send(event: String, params: Map<String, String>) {
        client.logEvent(event, params)
    }

    override fun send(event: AnalyticsEvent) {
        if (event is Shop.Purchased) {
            send(AFInAppEventType.PURCHASE, event.params)
        } else {
            super.send(event)
        }
    }

    companion object {
        const val ID = "AppsFlyer"
    }

    class Builder : AnalyticsHandlerBuilder {
        override fun build(data: AnalyticsHandlerBuilder.Data): AnalyticsHandler? = when {
            !data.isDebug -> AppsFlyerClient(data.application, data.config.appsFlyerDevKey)
            data.isDebug && data.logConfig.appsFlyer -> AppsFlyerLogClient(data.jsonConverter)
            else -> null
        }?.let { AppsFlyerAnalyticsHandler(it) }
    }
}
