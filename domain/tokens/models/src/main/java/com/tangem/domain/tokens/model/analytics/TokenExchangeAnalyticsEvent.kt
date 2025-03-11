package com.tangem.domain.tokens.model.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam.Key.PLACE
import com.tangem.core.analytics.models.AnalyticsParam.Key.PROVIDER
import com.tangem.core.analytics.models.AnalyticsParam.Key.STATUS
import com.tangem.core.analytics.models.AnalyticsParam.Key.TOKEN_PARAM

class TokenExchangeAnalyticsEvent(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent("Token", event, params, null) {

    class CexTxStatusOpened(token: String) : TokenScreenAnalyticsEvent(
        event = "Swap Status Opened",
        params = mapOf(TOKEN_PARAM to token),
    )

    class CexTxStatusChanged(token: String, status: String, provider: String) : TokenScreenAnalyticsEvent(
        event = "Swap Status",
        params = mapOf(
            TOKEN_PARAM to token,
            STATUS to status,
            PROVIDER to provider,
        ),
    )

    class GoToProviderStatus(token: String) : TokenScreenAnalyticsEvent(
        event = "Button - Go To Provider",
        params = mapOf(TOKEN_PARAM to token, PLACE to "Status"),
    )

    class GoToProviderKYC(token: String) : TokenScreenAnalyticsEvent(
        event = "Button - Go To Provider",
        params = mapOf(TOKEN_PARAM to token, PLACE to "KYC"),
    )

    class GoToProviderFail(token: String) : TokenScreenAnalyticsEvent(
        event = "Button - Go To Provider",
        params = mapOf(TOKEN_PARAM to token, PLACE to "Fail"),
    )
}
