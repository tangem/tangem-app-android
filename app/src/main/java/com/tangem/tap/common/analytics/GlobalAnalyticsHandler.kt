package com.tangem.tap.common.analytics

import android.content.Context
import com.amplitude.api.Amplitude
import com.appsflyer.AppsFlyerLib
import com.shopify.buy3.Storefront
import com.tangem.common.card.Card
import com.tangem.common.core.TangemSdkError
import com.tangem.tap.store

class GlobalAnalyticsHandler(val analyticsHandlers: List<AnalyticsHandler>) :
    AnalyticsHandler() {
    override fun triggerEvent(
        event: AnalyticsEvent,
        card: Card?,
        blockchain: String?,
        params: Map<String, String>
    ) {
        analyticsHandlers.forEach { it.triggerEvent(event, card, blockchain, params) }
    }

    override fun triggerEvent(event: String, params: Map<String, String>) {
        analyticsHandlers.forEach { it.triggerEvent(event, params) }
    }

    override fun logCardSdkError(
        error: TangemSdkError,
        actionToLog: Analytics.ActionToLog,
        parameters: Map<AnalyticsParam, String>?,
        card: Card?
    ) {
        analyticsHandlers.forEach { it.logCardSdkError(error, actionToLog, parameters, card) }
    }

    override fun logError(error: Throwable, params: Map<String, String>) {
        analyticsHandlers.forEach { it.logError(error, params) }
    }

    override fun getOrderEvent(): String {
        return ""
    }

    override fun getOrderParams(order: Storefront.Order): Map<String, String> {
       return emptyMap()
    }

    companion object {
        fun createDefaultAnalyticHandlers(context: Context): GlobalAnalyticsHandler {
            return GlobalAnalyticsHandler(
                listOf(
                    FirebaseAnalyticsHandler,
                    AppsFlyerAnalyticsHandler(context),
                    AmplitudeAnalyticsHandler()
                )
            )
        }

        private fun initAnalytics(context: Context) {
            initAppsFlyer(context)
            initAmplitude(context)
        }

        private fun initAppsFlyer(context: Context) {
            val devKey = store.state.globalState.configManager?.config?.appsFlyerDevKey ?: return
            AppsFlyerLib.getInstance().init(devKey, null, context)
            AppsFlyerLib.getInstance().start(context)

        }

        private fun initAmplitude(context: Context) {
            val apiKey = store.state.globalState.configManager?.config?.amplitudeApiKey ?: return
            Amplitude.getInstance()
                .initialize(context.applicationContext, apiKey)
                .enableForegroundTracking(context)
        }
    }
}
