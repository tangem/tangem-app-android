package com.tangem.tap.common.analytics.handlers.appsFlyer

import com.shopify.buy3.Storefront
import com.tangem.tap.common.analytics.api.AnalyticsEventHandler
import com.tangem.tap.common.analytics.api.AnalyticsHandlerBuilder
import com.tangem.tap.common.analytics.api.ShopifyOrderEventHandler
import com.tangem.tap.common.analytics.converters.ShopOrderToEventConverter
import com.tangem.tap.common.shop.data.ProductType

class AppsFlyerAnalyticsHandler(
    private val client: AppsFlyerAnalyticsClient,
) : AnalyticsEventHandler, ShopifyOrderEventHandler {

    override fun id(): String = ID

    override fun send(event: String, params: Map<String, String>) {
        client.logEvent(event, params)
    }

    override fun send(order: Storefront.Order, productType: ProductType) {
        //TODO: make sending this event through filters
        val event = ShopOrderToEventConverter().convert(order to productType)
        send(event)
    }

    companion object {
        const val ID = "AppsFlyer"
    }

    class Builder : AnalyticsHandlerBuilder {
        override fun build(data: AnalyticsHandlerBuilder.Data): AnalyticsEventHandler? = when {
            !data.isDebug -> AppsFlyerClient(data.application, data.config.appsFlyerDevKey)
            data.isDebug && data.logConfig.appsFlyer -> AppsFlyerLogClient(data.jsonConverter)
            else -> null
        }?.let { AppsFlyerAnalyticsHandler(it) }
    }
}