package com.tangem.tap.common.analytics.handlers.amplitude

import com.tangem.core.analytics.api.AnalyticsHandler
import com.tangem.core.analytics.api.AnalyticsUserIdHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.tap.common.analytics.AnalyticsEventsLogger
import com.tangem.tap.common.analytics.api.AnalyticsHandlerBuilder
import com.tangem.wallet.BuildConfig

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

    override fun send(event: AnalyticsEvent) {
        client.logEvent(event.id, event.params)
    }

    companion object {
        const val ID = "Amplitude"
    }

    class Builder : AnalyticsHandlerBuilder {
        override fun build(data: AnalyticsHandlerBuilder.Data): AnalyticsHandler {
            return AmplitudeAnalyticsHandler(
                client = if (BuildConfig.TESTER_MENU_ENABLED) {
                    AmplitudeClient(
                        application = data.application,
                        key = requireNotNull(data.config.amplitudeApiKeyDev) {
                            "Amplitude api key not found in ${BuildConfig.BUILD_TYPE}"
                        },
                        logger = AnalyticsEventsLogger(name = ID, jsonConverter = data.jsonConverter),
                    )
                } else {
                    AmplitudeClient(
                        application = data.application,
                        key = data.config.amplitudeApiKey,
                        logger = null,
                    )
                },
            )
        }
    }
}