package com.tangem.tap.common.analytics

import android.app.Application
import com.tangem.common.json.MoshiJsonConverter
import com.tangem.domain.common.AnalyticsHandlersLogConfig
import com.tangem.tap.common.analytics.handlers.amplitude.AmplitudeAnalyticsHandler
import com.tangem.tap.common.analytics.handlers.appsFlyer.AppsFlyerAnalyticsHandler
import com.tangem.tap.common.analytics.handlers.firebase.FirebaseAnalyticsHandler
import com.tangem.tap.domain.configurable.config.Config

/**
* [REDACTED_AUTHOR]
 */
class AnalyticsEventHandlerBuilder(
    private val application: Application,
    private val config: Config,
    private val isDebug: Boolean,
    private val logConfig: AnalyticsHandlersLogConfig,
    private val jsonConverter: MoshiJsonConverter,
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

    fun addFirebaseAnalytics(): AnalyticsEventHandlerBuilder {
        if (isDebug) {
            addEventLogger(logConfig.firebase, FirebaseAnalyticsHandler::class.java.simpleName)
        } else {
            analyticHandlers.add(FirebaseAnalyticsHandler())
        }
        return this
    }

    fun addAppsFlyer(): AnalyticsEventHandlerBuilder {
        if (isDebug) {
            addEventLogger(logConfig.appsFlyer, AppsFlyerAnalyticsHandler::class.java.simpleName)
        } else {
            analyticHandlers.add(AppsFlyerAnalyticsHandler(application, config.appsFlyerDevKey))
        }
        return this
    }

    fun addAmplitude(): AnalyticsEventHandlerBuilder {
        if (isDebug) {
            addEventLogger(logConfig.amplitude, AmplitudeAnalyticsHandler::class.java.simpleName)
        } else {
            analyticHandlers.add(AmplitudeAnalyticsHandler(application, config.amplitudeApiKey))
        }
        return this
    }

    private fun addEventLogger(logsEnabled: Boolean, name: String) {
        // if (logsEnabled) analyticHandlers.add(AnalyticsLogsPrinter(name, jsonConverter))
    }
}
