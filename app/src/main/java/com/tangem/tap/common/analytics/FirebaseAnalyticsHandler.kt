package com.tangem.tap.common.analytics

import android.os.Bundle
import androidx.core.os.bundleOf
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.ktx.Firebase
import com.shopify.buy3.Storefront
import com.tangem.common.card.Card
import com.tangem.common.core.TangemSdkError

object FirebaseAnalyticsHandler : AnalyticsHandler() {
    override fun triggerEvent(
        event: AnalyticsEvent,
        card: Card?,
        blockchain: String?,
        params: Map<String, String>
    ) {
        val fullParams = prepareParams(card, blockchain) + params
        Firebase.analytics.logEvent(event.event, fullParams.toBundle())
    }

    fun logException(name: String, throwable: Throwable) {
        Firebase.analytics.logEvent(name, bundleOf(
            "message" to (throwable.message ?: "none"),
            "cause_message" to (throwable.cause?.message ?: "none"),
            "stack_trace" to throwable.stackTraceToString()
        ))
    }

    private fun Map<String, String>.toBundle(): Bundle {
        return bundleOf(*this.toList().toTypedArray())
    }

    override fun logCardSdkError(
        error: TangemSdkError,
        actionToLog: Analytics.ActionToLog,
        parameters: Map<AnalyticsParam, String>?,
        card: Card?,
    ) {
        if (error is TangemSdkError.UserCancelled) return

        val params = parameters?.toMutableMap() ?: mutableMapOf()
        if (card != null) params + prepareParams(card)
        params[AnalyticsParam.ACTION] = actionToLog.key
        params[AnalyticsParam.ERROR_CODE] = error.code.toString()
        params[AnalyticsParam.ERROR_DESCRIPTION] = error.javaClass.simpleName
        params[AnalyticsParam.ERROR_KEY] = "TangemSdkError"

        params.forEach {
            FirebaseCrashlytics.getInstance().setCustomKey(it.key.param, it.value)
        }
        val cardError = TangemSdk.map(error)
        FirebaseCrashlytics.getInstance().recordException(cardError)
    }

    override fun logError(error: Throwable, params: Map<String, String>) {
        params.forEach { (key, value) -> FirebaseCrashlytics.getInstance().setCustomKey(key, value) }
        FirebaseCrashlytics.getInstance().recordException(error)
    }

    override fun getOrderEvent(): String = FirebaseAnalytics.Event.PURCHASE

    override fun getOrderParams(order: Storefront.Order): Map<String, String> {

        val sku = order.lineItems.edges.firstOrNull()?.node?.variant?.sku ?: "unknown"

        val discountCode =
            (order.discountApplications.edges.firstOrNull()?.node as? Storefront.DiscountCodeApplication)?.code

        val discountParams = if (discountCode != null ){
            mapOf(FirebaseAnalytics.Param.DISCOUNT to discountCode)
        } else {
            mapOf()
        }

        return mapOf(
            FirebaseAnalytics.Param.ITEM_ID to sku,
            FirebaseAnalytics.Param.VALUE to order.totalPriceV2.amount,
            FirebaseAnalytics.Param.CURRENCY to order.currencyCode.name
        ) + discountParams
    }

    override fun triggerEvent(event: String, params: Map<String, String>) {
        Firebase.analytics.logEvent(event, params.toBundle())
    }
}
