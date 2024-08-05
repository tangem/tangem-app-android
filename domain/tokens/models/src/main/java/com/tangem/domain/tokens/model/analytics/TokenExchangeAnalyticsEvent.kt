package com.tangem.domain.tokens.model.analytics

import com.tangem.core.analytics.models.AnalyticsEvent

class TokenExchangeAnalyticsEvent(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent("Token", event, params, null) {

    class CexTxStatusOpened(token: String) : TokenScreenAnalyticsEvent(
        event = "Swap Status Opened",
        params = mapOf("Token" to token),
    )

    class CexTxStatusChanged(token: String, status: String) : TokenScreenAnalyticsEvent(
        event = "Swap Status",
        params = mapOf("Token" to token, "Status" to status),
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
