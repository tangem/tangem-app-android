package com.tangem.tap.common.analytics

import android.app.Application
import com.shopify.buy3.Storefront
import com.tangem.common.card.Card
import com.tangem.common.core.TangemSdkError

class GlobalAnalyticsHandler(val analyticsHandlers: List<AnalyticsHandler>) :
    AnalyticsHandler() {
    override fun triggerEvent(
        event: AnalyticsEvent,
        card: Card?,
        blockchain: String?,
        params: Map<String, String>
    ) {
        analyticsHandlers.forEach { it.triggerEvent(event, card, blockchain) }
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
        fun createDefaultAnalyticHandlers(context: Application): GlobalAnalyticsHandler {
            return GlobalAnalyticsHandler(
                listOf(
                    FirebaseAnalyticsHandler,
                    AppsFlyerAnalyticsHandler(context)
                )
            )
        }
    }
}