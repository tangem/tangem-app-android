package com.tangem.tap.common.analytics.handlers.firebase

import android.os.Bundle
import androidx.core.os.bundleOf
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.shopify.buy3.Storefront
import com.tangem.blockchain.common.BlockchainError
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.common.Converter
import com.tangem.common.card.Card
import com.tangem.common.core.TangemSdkError
import com.tangem.tap.common.analytics.Analytics
import com.tangem.tap.common.analytics.AnalyticsEvent
import com.tangem.tap.common.analytics.AnalyticsEventHandler
import com.tangem.tap.common.analytics.AnalyticsParam
import com.tangem.tap.common.analytics.ErrorEventHandler
import com.tangem.tap.common.analytics.SdkErrorEventHandler
import com.tangem.tap.common.analytics.ShopifyOrderEventHandler
import com.tangem.tap.common.analytics.TangemSdk
import com.tangem.tap.features.demo.DemoTransactionSender

class FirebaseAnalyticsHandler : AnalyticsEventHandler, ShopifyOrderEventHandler, ErrorEventHandler,
    SdkErrorEventHandler {

    private val fbAnalytics = Firebase.analytics
    private val fbCrashlytics = Firebase.crashlytics

    override fun handleEvent(event: String, params: Map<String, String>) {
        fbAnalytics.logEvent(event, params.toBundle())
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

    override fun handleErrorEvent(error: Throwable, params: Map<String, String>) {
        params.forEach { (key, value) -> fbCrashlytics.setCustomKey(key, value) }
        fbCrashlytics.recordException(error)
    }

    override fun handleCardSdkErrorEvent(
        error: TangemSdkError,
        action: Analytics.ActionToLog,
        params: Map<AnalyticsParam, String>,
        card: Card?,
    ) {
        if (error is TangemSdkError.UserCancelled) return

        val mutableParams = params.toMutableMap()
        if (card != null) mutableParams + prepareParams(card)

        mutableParams[AnalyticsParam.ACTION] = action.key
        mutableParams[AnalyticsParam.ERROR_CODE] = error.code.toString()
        mutableParams[AnalyticsParam.ERROR_DESCRIPTION] = error.javaClass.simpleName
        mutableParams[AnalyticsParam.ERROR_KEY] = "TangemSdkError"

        mutableParams.forEach { fbCrashlytics.setCustomKey(it.key.param, it.value) }
        fbCrashlytics.recordException(TangemSdk.map(error))
    }

    override fun handleBlockchainSdkErrorEvent(
        error: BlockchainError,
        action: Analytics.ActionToLog,
        params: Map<AnalyticsParam, String>,
        card: Card?,
    ) {
        val blockchainSdkError = (error as? BlockchainSdkError) ?: return

        if (blockchainSdkError is BlockchainSdkError.WrappedTangemError) {
            val tangemSdkError = (blockchainSdkError.tangemError as? TangemSdkError) ?: return
            handleCardSdkErrorEvent(tangemSdkError, action, params, card)
            return
        }

        if (blockchainSdkError.customMessage.contains(DemoTransactionSender.ID)) return

        val mutableParams = params.toMutableMap()
        mutableParams[AnalyticsParam.ACTION] = action.key
        mutableParams[AnalyticsParam.ERROR_CODE] = error.code.toString()
        mutableParams[AnalyticsParam.ERROR_DESCRIPTION] = "${error.javaClass.simpleName}: ${error.customMessage}"
        mutableParams[AnalyticsParam.ERROR_KEY] = "BlockchainSdkError"

        mutableParams.forEach { fbCrashlytics.setCustomKey(it.key.param, it.value) }
        fbCrashlytics.recordException(error)
    }

    private fun Map<String, String>.toBundle(): Bundle {
        return bundleOf(*this.toList().toTypedArray())
    }

    companion object {
        const val ORDER_EVENT = FirebaseAnalytics.Event.PURCHASE
    }

    class OrderToParamsConverter : Converter<Storefront.Order, Map<String, String>> {
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
}