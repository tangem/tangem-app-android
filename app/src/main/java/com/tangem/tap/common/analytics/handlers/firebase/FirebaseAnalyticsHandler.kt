package com.tangem.tap.common.analytics.handlers.firebase

import com.shopify.buy3.Storefront
import com.tangem.blockchain.common.BlockchainError
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
import com.tangem.tap.common.analytics.events.OrderToParamsConverter
import com.tangem.tap.common.analytics.events.Shop
import com.tangem.tap.common.shop.data.ProductType

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

    override fun send(order: Storefront.Order, productType: ProductType) {
        val eventParams = OrderToParamsConverter().convert(order to productType)
        send(Shop.Purchased(eventParams))
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