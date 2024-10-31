package com.tangem.domain.tokens.model.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam.Key.TOKEN_PARAM

sealed class TokenReceiveAnalyticsEvent(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent("Token / Receive", event, params, null) {

    class ReceiveScreenOpened(token: String) : TokenReceiveAnalyticsEvent(
        event = "Receive Screen Opened",
        params = mapOf(TOKEN_PARAM to token),
    )

    class ButtonCopyAddress(token: String) : TokenReceiveAnalyticsEvent(
        event = "Button - Copy Address",
        params = mapOf("Token" to token),
    )

    class ButtonShareAddress(token: String) : TokenReceiveAnalyticsEvent(
        event = "Button - Share Address",
        params = mapOf("Token" to token),
    )
}