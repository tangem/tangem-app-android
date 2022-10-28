package com.tangem.tap.common.analytics.handlers.appsFlyer

import com.appsflyer.AFInAppEventParameterName
import com.appsflyer.AFInAppEventType
import com.shopify.buy3.Storefront
import com.tangem.common.Converter
import com.tangem.tap.common.analytics.api.AnalyticsEventHandler
import com.tangem.tap.common.analytics.api.ShopifyOrderEventHandler

class AppsFlyerAnalyticsHandler(
    private val client: AppsFlyerAnalyticsClient,
) : AnalyticsEventHandler, ShopifyOrderEventHandler {

    override fun handleEvent(event: String, params: Map<String, String>) {
        client.logEvent(event, params)
    }

    override fun handleShopifyOrderEvent(order: Storefront.Order) {
        handleEvent(ORDER_EVENT, OrderToParamsConverter().convert(order))
    }

    companion object {
        const val ORDER_EVENT = AFInAppEventType.PURCHASE
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