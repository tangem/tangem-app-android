package com.tangem.domain.tokens.models.analytics

import com.tangem.core.analytics.models.AnalyticsEvent

sealed class TokenSwapPromoAnalyticsEvent(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent("Swap Promo", event, params, null) {

    object Close : TokenSwapPromoAnalyticsEvent(event = "Button - Close")

    class Exchange(
        token: String,
    ) : TokenSwapPromoAnalyticsEvent(
        event = "Button - Exchange Now",
        params = mapOf("Token" to token),
    )
}