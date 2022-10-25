package com.tangem.tap.common.analytics.handlers.firebase

import com.google.firebase.analytics.FirebaseAnalytics
import com.shopify.buy3.Storefront
import com.tangem.blockchain.common.BlockchainError
import com.tangem.common.Converter
import com.tangem.common.card.Card
import com.tangem.common.core.TangemSdkError
import com.tangem.tap.common.analytics.Analytics
import com.tangem.tap.common.analytics.AnalyticsEvent
import com.tangem.tap.common.analytics.AnalyticsParam
import com.tangem.tap.common.analytics.api.AnalyticsEventHandler
import com.tangem.tap.common.analytics.api.ErrorEventHandler
import com.tangem.tap.common.analytics.api.SdkErrorEventHandler
import com.tangem.tap.common.analytics.api.ShopifyOrderEventHandler
import com.tangem.tap.common.analytics.converters.BlockchainSdkErrorConverter
import com.tangem.tap.common.analytics.converters.CardSdkErrorConverter

class FirebaseAnalyticsHandler(
    private val client: FirebaseAnalyticsClient,
) : AnalyticsEventHandler, ErrorEventHandler, SdkErrorEventHandler, ShopifyOrderEventHandler {

    override fun handleEvent(event: String, params: Map<String, String>) {
        client.logEvent(event, params)
    }

    override fun handleAnalyticsEvent(
        event: AnalyticsEvent,
        params: Map<String, String>,
        card: Card?,
        blockchain: String?,
    ) {
        handleEvent(event.name, prepareParams(card, blockchain, params))
    }

    override fun handleErrorEvent(error: Throwable, params: Map<String, String>) {
        client.logErrorEvent(error, params)
    }

    override fun handleCardSdkErrorEvent(
        error: TangemSdkError,
        action: Analytics.ActionToLog,
        params: Map<AnalyticsParam, String>,
        card: Card?,
    ) {
        val model = CardSdkErrorConverter.Model(error, action, params, prepareParams(card))
        val converter = CardSdkErrorConverter()
        converter.convert(model)?.let {
            handleErrorEvent(it.throwable, it.params)
        }
    }

    override fun handleBlockchainSdkErrorEvent(
        error: BlockchainError,
        action: Analytics.ActionToLog,
        params: Map<AnalyticsParam, String>,
        card: Card?,
    ) {

        val model = BlockchainSdkErrorConverter.Model(error, action, params, prepareParams(card))
        val converter = BlockchainSdkErrorConverter(CardSdkErrorConverter())
        converter.convert(model)?.let {
            handleErrorEvent(it.throwable, it.params)
        }
    }

    override fun handleShopifyOrderEvent(order: Storefront.Order) {
        handleEvent(FirebaseClient.ORDER_EVENT, OrderToParamsConverter().convert(order))
    }
}

private class OrderToParamsConverter : Converter<Storefront.Order, Map<String, String>> {
    override fun convert(value: Storefront.Order): Map<String, String> {
        val sku = value.lineItems.edges.firstOrNull()?.node?.variant?.sku ?: "unknown"

        val discountCode =
            (value.discountApplications.edges.firstOrNull()?.node as? Storefront.DiscountCodeApplication)?.code

        val discountParams = if (discountCode != null) {
            mapOf(FirebaseAnalytics.Param.DISCOUNT to discountCode)
        } else {
            mapOf()
        }

        return mapOf(
            FirebaseAnalytics.Param.ITEM_ID to sku,
            FirebaseAnalytics.Param.VALUE to value.totalPriceV2.amount,
            FirebaseAnalytics.Param.CURRENCY to value.currencyCode.name,
        ) + discountParams
    }
}
