package com.tangem.tap.common.analytics.handlers.appsflyer

import com.tangem.core.analytics.api.AnalyticsHandler
import com.tangem.core.analytics.api.AnalyticsUserIdHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AppsFlyerIncludedEvent
import com.tangem.core.analytics.models.AppsFlyerOnlyEvent
import com.tangem.tap.common.analytics.api.AnalyticsHandlerBuilder
import timber.log.Timber

class AppsFlyerAnalyticsHandler(
    private val client: AppsFlyerAnalyticsClient,
) : AnalyticsHandler, AnalyticsUserIdHandler {

    init {
        Timber.tag("AppsFlyer").i("AppsFlyer Analytics Handler created")
    }

    override fun id(): String = ID

    override fun send(event: AnalyticsEvent) {
        when (event) {
            is AppsFlyerOnlyEvent -> {
                Timber.tag("AppsFlyer").i("Sending event to AppsFlyer: ${event.id} with params: ${event.params}")
                client.logEvent(event.id, event.params)
            }
            is AppsFlyerIncludedEvent -> {
                Timber.tag("AppsFlyer").i("Sending event to AppsFlyer: ${event.id} with params: ${event.params}")
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

    internal class Builder(
        private val appsFlyerClientFactory: AppsFlyerClient.Factory,
    ) : AnalyticsHandlerBuilder {

        init {
            Timber.tag("AppsFlyer").i("AppsFlyer Analytics Handler Builder created")
        }

        override fun build(data: AnalyticsHandlerBuilder.Data): AnalyticsHandler = AppsFlyerAnalyticsHandler(
            client = appsFlyerClientFactory.create(apiKey = data.config.appsFlyerApiKey), // TODO(!!!)
        )
    }
}