package com.tangem.domain.tokens.model.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam.Key.PLACE
import com.tangem.core.analytics.models.AnalyticsParam.Key.PROVIDER
import com.tangem.core.analytics.models.AnalyticsParam.Key.STATUS
import com.tangem.core.analytics.models.AnalyticsParam.Key.TOKEN_CATEGORY
import com.tangem.core.analytics.models.AnalyticsParam.Key.TOKEN_PARAM
import com.tangem.core.analytics.models.EventValue

class TokenExchangeAnalyticsEvent(
    event: String,
    params: Map<String, EventValue> = mapOf(),
) : AnalyticsEvent(TOKEN_CATEGORY, event, params, null) {

    class CexTxStatusOpened(token: String) : TokenScreenAnalyticsEvent(
        event = "Swap Status Opened",
        params = mapOf(TOKEN_PARAM to token.asStringValue()),
    )

    class CexTxStatusChanged(token: String, status: String, provider: String) : TokenScreenAnalyticsEvent(
        event = "Swap Status",
        params = mapOf(
            TOKEN_PARAM to token.asStringValue(),
            STATUS to status.asStringValue(),
            PROVIDER to provider.asStringValue(),
        ),
    )

    class GoToProviderStatus(token: String) : TokenScreenAnalyticsEvent(
        event = "Button - Go To Provider",
        params = mapOf(
            TOKEN_PARAM to token.asStringValue(),
            PLACE to "Status".asStringValue(),
        ),
    )

    class GoToProviderKYC(token: String) : TokenScreenAnalyticsEvent(
        event = "Button - Go To Provider",
        params = mapOf(
            TOKEN_PARAM to token.asStringValue(),
            PLACE to "KYC".asStringValue(),
        ),
    )

    class GoToProviderFail(token: String) : TokenScreenAnalyticsEvent(
        event = "Button - Go To Provider",
        params = mapOf(
            TOKEN_PARAM to token.asStringValue(),
            PLACE to "Fail".asStringValue(),
        ),
    )
}