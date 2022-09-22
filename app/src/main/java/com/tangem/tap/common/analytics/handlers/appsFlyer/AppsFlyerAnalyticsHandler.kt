package com.tangem.tap.common.analytics.handlers.appsFlyer

import android.content.Context
import com.appsflyer.AFInAppEventParameterName
import com.appsflyer.AFInAppEventType
import com.appsflyer.AppsFlyerLib
import com.shopify.buy3.Storefront
import com.tangem.common.Converter
import com.tangem.common.card.Card
import com.tangem.tap.common.analytics.AnalyticsEvent
import com.tangem.tap.common.analytics.AnalyticsEventHandler
import com.tangem.tap.common.analytics.ShopifyOrderEventHandler

class AppsFlyerAnalyticsHandler(
    private val context: Context,
    key: String,
) : AnalyticsEventHandler, ShopifyOrderEventHandler {

    private val appsFlyerLib: AppsFlyerLib = AppsFlyerLib.getInstance()

    init {
        appsFlyerLib.init(key, null, context)
        appsFlyerLib.start(context)
    }

    override fun handleEvent(event: String, params: Map<String, String>) {
        appsFlyerLib.logEvent(context, event, params)
    }

    override fun handleAnalyticsEvent(
        event: AnalyticsEvent,
        params: Map<String, String>,
        card: Card?,
        blockchain: String?,
    ) {
        handleEvent(event.event, prepareParams(card, blockchain, params))
    }

    override fun handleShopifyOrderEvent(order: Storefront.Order) {
        handleEvent(ORDER_EVENT, OrderToParamsConverter().convert(order))
    }

    companion object {
        const val ORDER_EVENT = AFInAppEventType.PURCHASE
    }

    class OrderToParamsConverter : Converter<Storefront.Order, Map<String, String>> {
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
}
