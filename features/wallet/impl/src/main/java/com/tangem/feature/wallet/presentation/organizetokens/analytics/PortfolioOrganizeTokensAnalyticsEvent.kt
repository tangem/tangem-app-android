package com.tangem.feature.wallet.presentation.organizetokens.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.EventValue

sealed class PortfolioOrganizeTokensAnalyticsEvent(
    event: String,
    params: Map<String, EventValue> = mapOf(),
) : AnalyticsEvent("Portfolio / Organize Tokens", event, params) {

    object ScreenOpened : PortfolioOrganizeTokensAnalyticsEvent("Organize Tokens Screen Opened")

    object ByBalance : PortfolioOrganizeTokensAnalyticsEvent("Button - By Balance")

    object Group : PortfolioOrganizeTokensAnalyticsEvent("Button - Group")

    class Apply(
        grouping: AnalyticsParam.OnOffState,
        organizeSortType: AnalyticsParam.OrganizeSortType,
    ) : PortfolioOrganizeTokensAnalyticsEvent(
        "Button - Apply",
        params = mapOf(
            "Group" to grouping.value.asStringValue(),
            "Sort" to organizeSortType.value.asStringValue(),
        ),
    )

    object Cancel : PortfolioOrganizeTokensAnalyticsEvent("Button - Cancel")
}