package com.tangem.domain.tokens.models.analytics

import com.tangem.core.analytics.models.AnalyticsEvent

class TokenExchangeAnalyticsEvent(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent("Token", event, params, null) {

    class CexTx(token: String) : TokenScreenAnalyticsEvent(
        event = "Notice - ChangeNow Swap",
        params = mapOf("Token" to token),
    )

    class CexTxOpened(token: String, status: String) : TokenScreenAnalyticsEvent(
        event = "ChangeNow Swap Opened",
        params = mapOf("Token" to token, "Status" to status),
    )

    class Verification(token: String) : TokenScreenAnalyticsEvent(
        event = "Notice - KYC required",
        params = mapOf("Token" to token),
    )

    class Fail(token: String) : TokenScreenAnalyticsEvent(
        event = "Notice - Operation Fail",
        params = mapOf("Token" to token),
    )

    class GoToProviderStatus(token: String) : TokenScreenAnalyticsEvent(
        event = "Button - Go To Provider",
        params = mapOf("Token" to token, "Place" to "Status"),
    )

    class GoToProviderKYC(token: String) : TokenScreenAnalyticsEvent(
        event = "Button - Go To Provider",
        params = mapOf("Token" to token, "Place" to "KYC"),
    )

    class GoToProviderFail(token: String) : TokenScreenAnalyticsEvent(
        event = "Button - Go To Provider",
        params = mapOf("Token" to token, "Place" to "Fail"),
    )
}
