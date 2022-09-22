package com.tangem.tap.common.analytics

import android.app.Application
import com.tangem.common.json.MoshiJsonConverter
import com.tangem.domain.common.AnalyticsHandlersLogConfig
import com.tangem.tap.common.analytics.api.AnalyticsEventHandler
import com.tangem.tap.common.analytics.handlers.amplitude.AmplitudeAnalyticsHandler
import com.tangem.tap.common.analytics.handlers.amplitude.AmplitudeClient
import com.tangem.tap.common.analytics.handlers.amplitude.AmplitudeLogClient
import com.tangem.tap.common.analytics.handlers.appsFlyer.AppsFlyerAnalyticsHandler
import com.tangem.tap.common.analytics.handlers.appsFlyer.AppsFlyerClient
import com.tangem.tap.common.analytics.handlers.appsFlyer.AppsFlyerLogClient
import com.tangem.tap.common.analytics.handlers.firebase.FirebaseAnalyticsHandler
import com.tangem.tap.common.analytics.handlers.firebase.FirebaseClient
import com.tangem.tap.common.analytics.handlers.firebase.FirebaseLogClient
import com.tangem.tap.domain.configurable.config.Config

/**
[REDACTED_AUTHOR]
 */
class GlobalAnalyticsEventHandlerBuilder(
    val application: Application,
    val config: Config,
    val isDebug: Boolean,
    val logConfig: AnalyticsHandlersLogConfig,
    val jsonConverter: MoshiJsonConverter,
) {

    private val analyticHandlers = mutableListOf<AnalyticsEventHandler>()

    fun build(): GlobalAnalyticsEventHandler {
        return GlobalAnalyticsHandler(analyticHandlers.toList())
    }

    fun default(): GlobalAnalyticsEventHandler {
        addFirebaseAnalytics()
        addAppsFlyer()
        addAmplitude()

        return GlobalAnalyticsHandler(analyticHandlers.toList())
    }

    fun addFirebaseAnalytics(): GlobalAnalyticsEventHandlerBuilder {
        if (isDebug) {
            if (logConfig.firebase) FirebaseLogClient(createLogger("FirebaseAnalyticsHandler")) else null
        } else {
            FirebaseClient()
        }?.let {
            analyticHandlers.add(FirebaseAnalyticsHandler(it))
        }
        return this
    }

    fun addAppsFlyer(): GlobalAnalyticsEventHandlerBuilder {
        if (isDebug) {
            if (logConfig.appsFlyer) AppsFlyerLogClient(createLogger("AppsFlyerAnalyticsHandler")) else null
        } else {
            AppsFlyerClient(application, config.appsFlyerDevKey)
        }?.let {
            analyticHandlers.add(AppsFlyerAnalyticsHandler(it))
        }
        return this
    }

    fun addAmplitude(): GlobalAnalyticsEventHandlerBuilder {
        if (isDebug) {
            if (logConfig.firebase) AmplitudeLogClient(createLogger("AmplitudeAnalyticsHandler")) else null
        } else {
            AmplitudeClient(application, config.amplitudeApiKey)
        }?.let {
            analyticHandlers.add(AmplitudeAnalyticsHandler(it))
        }
        return this
    }

    private fun createLogger(name: String): AnalyticsEventsLogger {
        return AnalyticsEventsLogger(name, jsonConverter)
    }
}