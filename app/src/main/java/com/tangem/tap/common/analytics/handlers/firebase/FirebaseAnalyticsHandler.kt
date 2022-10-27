package com.tangem.tap.common.analytics.handlers.firebase

import com.google.firebase.analytics.FirebaseAnalytics
import com.shopify.buy3.Storefront
import com.tangem.blockchain.common.BlockchainError
import com.tangem.common.Converter
import com.tangem.common.card.Card
import com.tangem.common.core.TangemSdkError
import com.tangem.tap.common.analytics.AnalyticsAnOld
import com.tangem.tap.common.analytics.AnalyticsParamAnOld
import com.tangem.tap.common.analytics.api.AnalyticsEventHandler
import com.tangem.tap.common.analytics.api.AnalyticsHandlerBuilder
import com.tangem.tap.common.analytics.api.ErrorEventHandler
import com.tangem.tap.common.analytics.api.SdkErrorEventHandler
import com.tangem.tap.common.analytics.api.ShopifyOrderEventHandler
import com.tangem.tap.common.analytics.converters.BlockchainSdkErrorConverter
import com.tangem.tap.common.analytics.converters.CardSdkErrorConverter

class FirebaseAnalyticsHandler(
    private val client: FirebaseAnalyticsClient,
) : AnalyticsEventHandler, ErrorEventHandler, SdkErrorEventHandler, ShopifyOrderEventHandler {

    override fun id(): String = ID

    override fun send(event: String, params: Map<String, String>) {
        client.logEvent(event, params)
    }

    override fun send(error: Throwable, params: Map<String, String>) {
        client.logErrorEvent(error, params)
    }

    override fun send(
        error: TangemSdkError,
        action: AnalyticsAnOld.ActionToLog,
        params: Map<AnalyticsParamAnOld, String>,
        card: Card?,
    ) {
        val model = CardSdkErrorConverter.Model(error, action, params, prepareParams(card))
        val converter = CardSdkErrorConverter()
        converter.convert(model)?.let {
            send(it.throwable, it.params)
        }
    }

    override fun send(
        error: BlockchainError,
        action: AnalyticsAnOld.ActionToLog,
        params: Map<AnalyticsParamAnOld, String>,
        card: Card?,
    ) {

        val model = BlockchainSdkErrorConverter.Model(error, action, params, prepareParams(card))
        val converter = BlockchainSdkErrorConverter(CardSdkErrorConverter())
        converter.convert(model)?.let {
            send(it.throwable, it.params)
        }
    }

    override fun send(order: Storefront.Order) {
        send(FirebaseClient.ORDER_EVENT, OrderToParamsConverter().convert(order))
    }

    companion object {
        const val ID = "Firebase"
    }

    class Builder : AnalyticsHandlerBuilder {
        override fun build(data: AnalyticsHandlerBuilder.Data): AnalyticsEventHandler? = when {
            !data.isDebug -> FirebaseClient()
            data.isDebug && data.logConfig.firebase -> FirebaseLogClient(data.jsonConverter)
            else -> null
        }?.let { FirebaseAnalyticsHandler(it) }
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