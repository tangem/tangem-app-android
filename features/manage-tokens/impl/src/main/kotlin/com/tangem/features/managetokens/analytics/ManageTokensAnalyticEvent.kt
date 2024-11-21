package com.tangem.features.managetokens.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.EventValue
import com.tangem.features.managetokens.component.ManageTokensSource

internal sealed class ManageTokensAnalyticEvent(
    event: String,
    params: Map<String, EventValue> = mapOf(),
) : AnalyticsEvent(
    category = "Manage Tokens",
    event = event,
    params = params,
) {

    class ScreenOpened(source: ManageTokensSource) : ManageTokensAnalyticEvent(
        event = "Manage Tokens Screen Opened",
        params = mapOf(AnalyticsParam.Key.SOURCE to source.analyticsName.asStringValue()),
    )

    class TokensIsNotFound(query: String, source: ManageTokensSource) : ManageTokensAnalyticEvent(
        event = "Token Is Not Found",
        params = mapOf(
            AnalyticsParam.Key.INPUT to query.asStringValue(),
            AnalyticsParam.Key.SOURCE to source.analyticsName.asStringValue(),
        ),
    )

    class TokenSwitcherChanged(
        tokenSymbol: String,
        isSelected: Boolean,
        source: ManageTokensSource,
    ) : ManageTokensAnalyticEvent(
        event = "Token Switcher Changed",
        params = mapOf(
            AnalyticsParam.Key.TOKEN_PARAM to tokenSymbol.asStringValue(),
            AnalyticsParam.Key.STATE to AnalyticsParam.OnOffState.from(isSelected).asStringValue(),
            AnalyticsParam.Key.SOURCE to source.analyticsName.asStringValue(),
        ),
    )

    class TokenAdded(
        tokensCount: Int,
        source: ManageTokensSource,
    ) : ManageTokensAnalyticEvent(
        event = "Token Added",
        params = mapOf(
            AnalyticsParam.Key.COUNT to tokensCount.asStringValue(),
            AnalyticsParam.Key.SOURCE to source.analyticsName.asStringValue(),
        ),
    )
    data object ButtonLater : ManageTokensAnalyticEvent(
        event = "Button - Later",
        params = mapOf(AnalyticsParam.SOURCE to ManageTokensSource.ONBOARDING.analyticsName.asStringValue()),
    )
}