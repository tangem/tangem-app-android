package com.tangem.feature.wallet.presentation.organizetokens.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam

sealed class PortfolioOrganizeTokensAnalyticsEvent(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent("Portfolio / Organize Tokens", event, params) {

    class ScreenOpened : PortfolioOrganizeTokensAnalyticsEvent("Organize Tokens Screen Opened")

    class ByBalance : PortfolioOrganizeTokensAnalyticsEvent("Button - By Balance")

    class Group : PortfolioOrganizeTokensAnalyticsEvent("Button - Group")

    class Apply(
        grouping: AnalyticsParam.OnOffState,
        organizeSortType: AnalyticsParam.OrganizeSortType,
    ) : PortfolioOrganizeTokensAnalyticsEvent(
        "Button - Apply",
        params = mapOf(
            "Group" to grouping.value,
            "Sort" to organizeSortType.value,
        ),
    )

    class Cancel : PortfolioOrganizeTokensAnalyticsEvent("Button - Cancel")
}