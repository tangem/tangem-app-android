package com.tangem.feature.tokendetails.presentation.tokendetails.analytics

import com.tangem.core.analytics.models.AnalyticsEvent

/**
[REDACTED_AUTHOR]
 */
sealed class TokenScreenEvent(
    event: String,
    params: Map<String, String> = mapOf(),
    error: Throwable? = null,
) : AnalyticsEvent("Token", event, params, error) {

    class Refreshed(token: String) : TokenScreenEvent(
        event = "Refreshed",
        params = mapOf("Token" to token),
    )

    class ButtonRemoveToken(token: String) : TokenScreenEvent(
        "Button - Remove Token",
        params = mapOf("Token" to token),
    )

    class ButtonExplore(token: String) : TokenScreenEvent(
        event = "Button - Explore",
        params = mapOf("Token" to token),
    )

    class ButtonReload(token: String) : TokenScreenEvent(
        event = "Button - Reload",
        params = mapOf("Token" to token),
    )

    class ButtonBuy(token: String) : TokenScreenEvent(
        event = "Button - Buy",
        params = mapOf("Token" to token),
    )

    class ButtonSell(token: String) : TokenScreenEvent(
        event = "Button - Sell",
        params = mapOf("Token" to token),
    )

    class ButtonExchange(token: String) : TokenScreenEvent(
        event = "Button - Exchange",
        params = mapOf("Token" to token),
    )

    class ButtonSend(token: String) : TokenScreenEvent(
        event = "Button - Send",
        params = mapOf("Token" to token),
    )

    class ButtonReceive(token: String) : TokenScreenEvent(
        event = "Button - Receive",
        params = mapOf("Token" to token),
    )

    class Bought(token: String) : TokenScreenEvent(
        event = "Token Bought",
        params = mapOf("Token" to token),
    )
}