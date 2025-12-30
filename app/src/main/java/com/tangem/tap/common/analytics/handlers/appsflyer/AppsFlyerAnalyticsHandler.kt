package com.tangem.tap.common.analytics.handlers.appsflyer

import com.tangem.core.analytics.api.AnalyticsHandler
import com.tangem.core.analytics.api.AnalyticsUserIdHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AppsFlyerIncludedEvent
import com.tangem.core.analytics.models.AppsFlyerOnlyEvent
import com.tangem.tap.common.analytics.api.AnalyticsHandlerBuilder

class AppsFlyerAnalyticsHandler(
    private val client: AppsFlyerAnalyticsClient,
) : AnalyticsHandler, AnalyticsUserIdHandler {

    override fun id(): String = ID

    override fun send(event: AnalyticsEvent) {
        when (event) {
            is AppsFlyerOnlyEvent -> {
                client.logEvent(event.id, event.params)
            }
            is AppsFlyerIncludedEvent -> {
                client.logEvent(
                    event = AnalyticsEvent(category = event.category, event = event.appsFlyerReplacedEvent).id,
                    params = event.params,
                )
            }
        }
    }

    override fun setUserId(userId: String) {
        client.setUserId(userId)
    }

    override fun clearUserId() {
        client.clearUserId()
    }

    companion object {
        const val ID = "AppsFlyer"
    }

    class Builder : AnalyticsHandlerBuilder {
        override fun build(data: AnalyticsHandlerBuilder.Data): AnalyticsHandler = AppsFlyerAnalyticsHandler(
            client = if (data.logConfig.isAppsflyerLogEnabled) {
                AppsFlyerLogClient(data.jsonConverter)
            } else {
                AppsFlyerClient(data.application, data.config.appsFlyerApiKey)
            },
        )
    }
}