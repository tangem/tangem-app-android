package com.tangem.feature.wallet.deeplink.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.domain.wallets.PromoCodeActivationResult

sealed class PromoActivationAnalytics(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent(category = "Promotion", event = event, params = params) {

    data object PromoDeepLinkActivationStart : PromoActivationAnalytics(
        event = "Bitcoin Promo Deep Link Activation",
        params = emptyMap(),
    )

    data class PromoActivation(val result: PromoCodeActivationResult) : PromoActivationAnalytics(
        event = "Bitcoin Promo Activation",
        params = mapOf(
            "State" to when (result) {
                PromoCodeActivationResult.Failed -> "Error"
                PromoCodeActivationResult.InvalidPromoCode -> "Invalid"
                PromoCodeActivationResult.NoBitcoinAddress -> "No Address"
                PromoCodeActivationResult.PromoCodeAlreadyUsed -> "Already Used"
                PromoCodeActivationResult.Activated -> "Activated"
            },
        ),
    )
}