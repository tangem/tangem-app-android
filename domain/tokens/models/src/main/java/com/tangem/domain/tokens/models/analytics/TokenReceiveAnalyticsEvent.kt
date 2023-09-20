package com.tangem.domain.tokens.models.analytics

import com.tangem.core.analytics.models.AnalyticsEvent

sealed class TokenScreenAnalyticsEvent(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent("Token / Receive", event, params, null) {


    class ButtonCopyAddress(token: String) : TokenScreenAnalyticsEvent(
        event = "Button - Copy Address",
        params = mapOf("Token" to token),
    )

    class ButtonShareAddress(token: String) : TokenScreenAnalyticsEvent(
        event = "Button - Share Address",
        params = mapOf("Token" to token),
    )

}