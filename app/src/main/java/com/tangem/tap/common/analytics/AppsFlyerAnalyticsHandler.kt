package com.tangem.tap.common.analytics

import android.app.Application
import com.appsflyer.AFInAppEventParameterName
import com.appsflyer.AFInAppEventType
import com.appsflyer.AppsFlyerLib
import com.shopify.buy3.Storefront
import com.tangem.common.card.Card
import com.tangem.common.core.TangemSdkError

class AppsFlyerAnalyticsHandler(val context: Application): AnalyticsHandler() {

    override fun triggerEvent(
        event: AnalyticsEvent,
        card: Card?,
        blockchain: String?,
        params: Map<String, String>
    ) {
        AppsFlyerLib.getInstance().logEvent(context,
            event.event, prepareParams(card, blockchain))
    }

    override fun logCardSdkError(
        error: TangemSdkError,
        actionToLog: Analytics.ActionToLog,
        parameters: Map<AnalyticsParam, String>?,
        card: Card?
    ) {
        //TODO
    }

    override fun logError(error: Throwable, params: Map<String, String>) {
        //TODO
    }

    override fun getOrderEvent(): String = AFInAppEventType.PURCHASE

    override fun getOrderParams(order: Storefront.Order): Map<String, String> {

        val sku = order.lineItems.edges.firstOrNull()?.node?.variant?.sku ?: "unknown"

        val discountCode =
            (order.discountApplications.edges.firstOrNull()?.node as? Storefront.DiscountCodeApplication)?.code

        val discountParams = if (discountCode != null ){
            mapOf(AFInAppEventParameterName.COUPON_CODE to discountCode)
        } else {
            mapOf()
        }

        return mapOf(
            AFInAppEventParameterName.CONTENT_ID to sku,
            AFInAppEventParameterName.REVENUE to order.totalPriceV2.amount,
            AFInAppEventParameterName.CURRENCY to order.currencyCode.name
        ) + discountParams
    }

    override fun triggerEvent(event: String, params: Map<String, String>) {
        AppsFlyerLib.getInstance().logEvent(context, event, params)
    }

}