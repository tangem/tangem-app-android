package com.tangem.features.managetokens.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.features.managetokens.component.ManageTokensSource

internal sealed class ManageTokensAnalyticEvent(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent(
    category = "ManageTokens",
    event = event,
    params = params,
) {

    class ScreenOpened(source: ManageTokensSource) : ManageTokensAnalyticEvent(
        event = "Manage Tokens Screen Opened",
        params = mapOf(AnalyticsParam.Key.SOURCE to source.name),
    )

    class TokensIsNotFound(query: String, source: ManageTokensSource) : ManageTokensAnalyticEvent(
        event = "Token Is Not Found",
        params = mapOf(
            AnalyticsParam.Key.INPUT to query,
            AnalyticsParam.Key.SOURCE to source.name,
        ),
    )

    class TokenSwitcherChanged(
        tokenSymbol: String,
        isSelected: Boolean,
        source: ManageTokensSource,
    ) : ManageTokensAnalyticEvent(
        event = "Token Switcher Changed",
        params = mapOf(
            AnalyticsParam.Key.TOKEN_PARAM to tokenSymbol,
            AnalyticsParam.Key.STATE to AnalyticsParam.OnOffState.from(isSelected),
            AnalyticsParam.Key.SOURCE to source.name,
        ),
    )

    class TokenAdded(
        tokensCount: Int,
        source: ManageTokensSource,
    ) : ManageTokensAnalyticEvent(
        event = "Token Added",
        params = mapOf(
            AnalyticsParam.Key.COUNT to tokensCount.toString(),
            AnalyticsParam.Key.SOURCE to source.name,
        ),
    )

    // TODO: Will be used later
    data object ButtonLater : ManageTokensAnalyticEvent(event = "Button - Later")
}