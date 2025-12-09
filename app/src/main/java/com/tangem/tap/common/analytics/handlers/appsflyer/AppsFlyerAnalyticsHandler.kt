package com.tangem.tap.common.analytics.handlers.appsflyer

import com.tangem.core.analytics.api.AnalyticsHandler
import com.tangem.core.analytics.api.AnalyticsUserIdHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AppsFlyerEvent
import com.tangem.tap.common.analytics.api.AnalyticsHandlerBuilder

class AppsFlyerAnalyticsHandler(
    private val client: AppsFlyerAnalyticsClient,
) : AnalyticsHandler, AnalyticsUserIdHandler {

    override fun id(): String = ID

    override fun send(event: AnalyticsEvent) {
        if (event !is AppsFlyerEvent) return

        client.logEvent(event.id, event.params)
    }

    override fun setUserId(userId: String) {

    }

    override fun clearUserId() {
        TODO("Not yet implemented")
    }

    companion object {
        const val ID = "AppsFlyer"
    }

    class Builder : AnalyticsHandlerBuilder {
        override fun build(data: AnalyticsHandlerBuilder.Data): AnalyticsHandler? = when {
            !data.isDebug -> AppsFlyerClient(data.application, data.config.appsFlyerApiKey, data.config.appsAppId)
            data.isDebug && data.logConfig.isAppsflyerLogEnabled -> AppsFlyerLogClient(data.jsonConverter)
            else -> null
        }?.let { AppsFlyerAnalyticsHandler(it) }
    }
}