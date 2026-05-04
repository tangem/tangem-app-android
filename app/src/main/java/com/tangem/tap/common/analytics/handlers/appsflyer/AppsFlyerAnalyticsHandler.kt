package com.tangem.tap.common.analytics.handlers.appsflyer

import com.tangem.core.analytics.api.AnalyticsHandler
import com.tangem.core.analytics.api.AnalyticsUserIdHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AppsFlyerIncludedEvent
import com.tangem.core.analytics.models.AppsFlyerOnlyEvent
import com.tangem.tap.common.analytics.api.AnalyticsHandlerBuilder
import com.tangem.utils.logging.TangemLogger

class AppsFlyerAnalyticsHandler(
    private val client: AppsFlyerAnalyticsClient,
) : AnalyticsHandler, AnalyticsUserIdHandler {

    init {
        TangemLogger.withTag("AppsFlyer").i("AppsFlyer Analytics Handler created")
    }

    override fun id(): String = ID

    override fun send(event: AnalyticsEvent) {
        when (event) {
            is AppsFlyerOnlyEvent -> {
                TangemLogger.withTag(
                    "AppsFlyer",
                ).i("Sending event to AppsFlyer: ${event.id} with params: ${event.params}")
                client.logEvent(event.id, event.params)
            }
            is AppsFlyerIncludedEvent -> {
                TangemLogger.withTag(
                    "AppsFlyer",
                ).i("Sending event to AppsFlyer: ${event.id} with params: ${event.params}")
                val replacedEvent = event.appsFlyerReplacedEvent ?: event.event
                client.logEvent(
                    event = AnalyticsEvent(category = event.category, event = replacedEvent).id,
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

    internal class Builder(
        private val appsFlyerClientFactory: AppsFlyerClient.Factory,
    ) : AnalyticsHandlerBuilder {

        init {
            TangemLogger.withTag("AppsFlyer").i("AppsFlyer Analytics Handler Builder created")
        }

        override fun build(data: AnalyticsHandlerBuilder.Data): AnalyticsHandler = AppsFlyerAnalyticsHandler(
            client = if (data.logConfig.isAppsflyerLogEnabled) {
                TangemLogger.withTag("AppsFlyer").i("AppsFlyer log enabled, mock client created")
                AppsFlyerLogClient(data.jsonConverter)
            } else {
                TangemLogger.withTag("AppsFlyer").i("AppsFlyer log disabled, real client created")
                appsFlyerClientFactory.create(apiKey = data.config.appsFlyerApiKey)
            },
        )
    }
}