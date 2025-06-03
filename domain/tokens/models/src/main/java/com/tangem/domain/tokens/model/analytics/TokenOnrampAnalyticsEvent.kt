package com.tangem.domain.tokens.model.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam.Key.PROVIDER
import com.tangem.core.analytics.models.AnalyticsParam.Key.STATUS
import com.tangem.core.analytics.models.AnalyticsParam.Key.TOKEN_PARAM

sealed class TokenOnrampAnalyticsEvent(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent("Onramp", event, params) {

    class OnrampStatusOpened(tokenSymbol: String, provider: String, fiatCurrency: String) : TokenOnrampAnalyticsEvent(
        event = "Onramp Status Opened",
        params = mapOf(
            TOKEN_PARAM to tokenSymbol,
            PROVIDER to provider,
            "Currency Type" to fiatCurrency,
        ),
    )

    class OnrampStatusChanged(tokenSymbol: String, status: String, provider: String, fiatCurrency: String) :
        TokenOnrampAnalyticsEvent(
            event = "Onramp Status",
            params = mapOf(
                TOKEN_PARAM to tokenSymbol,
                STATUS to status,
                PROVIDER to provider,
                "Currency Type" to fiatCurrency,
            ),
        )

    data object GoToProvider : TokenOnrampAnalyticsEvent(event = "Button - Go To Provider")

    data class NoticeKYC(
        val tokenSymbol: String,
        val provider: String,
    ) : TokenOnrampAnalyticsEvent(
        event = "Notice - KYC",
        params = mapOf(
            TOKEN_PARAM to tokenSymbol,
            PROVIDER to provider,
        ),
    )
}