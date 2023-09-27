package com.tangem.domain.tokens.models.analytics

import com.tangem.core.analytics.models.AnalyticsEvent

sealed class TokenReceiveAnalyticsEvent(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent("Token / Receive", event, params, null) {

    class ButtonCopyAddress(token: String) : TokenReceiveAnalyticsEvent(
        event = "Button - Copy Address",
        params = mapOf("Token" to token),
    )

    class ButtonShareAddress(token: String) : TokenReceiveAnalyticsEvent(
        event = "Button - Share Address",
        params = mapOf("Token" to token),
    )
}