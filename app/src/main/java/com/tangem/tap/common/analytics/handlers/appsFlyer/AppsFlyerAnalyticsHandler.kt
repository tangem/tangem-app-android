package com.tangem.tap.common.analytics.handlers.appsFlyer

import com.appsflyer.AFInAppEventParameterName
import com.appsflyer.AFInAppEventType
import com.shopify.buy3.Storefront
import com.tangem.common.Converter
import com.tangem.tap.common.analytics.api.AnalyticsEventHandler
import com.tangem.tap.common.analytics.api.AnalyticsHandlerBuilder
import com.tangem.tap.common.analytics.api.ShopifyOrderEventHandler

class AppsFlyerAnalyticsHandler(
    private val client: AppsFlyerAnalyticsClient,
) : AnalyticsEventHandler, ShopifyOrderEventHandler {

    override fun id(): String = ID

    override fun send(event: String, params: Map<String, String>) {
        client.logEvent(event, params)
    }

    override fun send(order: Storefront.Order) {
        send(ORDER_EVENT, OrderToParamsConverter().convert(order))
    }

    companion object {
        const val ORDER_EVENT = AFInAppEventType.PURCHASE
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

private class OrderToParamsConverter : Converter<Storefront.Order, Map<String, String>> {

    override fun convert(value: Storefront.Order): Map<String, String> {
        val sku = value.lineItems.edges.firstOrNull()?.node?.variant?.sku ?: "unknown"

        val discountCode =
            (value.discountApplications.edges.firstOrNull()?.node as? Storefront.DiscountCodeApplication)?.code

        val discountParams = if (discountCode != null) {
            mapOf(AFInAppEventParameterName.COUPON_CODE to discountCode)
        } else {
            mapOf()
        }

        return mapOf(
            AFInAppEventParameterName.CONTENT_ID to sku,
            AFInAppEventParameterName.REVENUE to value.totalPriceV2.amount,
            AFInAppEventParameterName.CURRENCY to value.currencyCode.name,
        ) + discountParams
    }
}