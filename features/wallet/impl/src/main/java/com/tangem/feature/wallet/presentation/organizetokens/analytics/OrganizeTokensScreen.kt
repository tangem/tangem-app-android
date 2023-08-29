package com.tangem.feature.wallet.presentation.organizetokens.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam

sealed class OrganizeTokensScreen(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent("Portfolio / Organize Tokens", event, params) {

    object ScreenOpened : OrganizeTokensScreen("Organize Tokens Screen Opened")

    object ByBalance : OrganizeTokensScreen("Button - By Balance")

    object Group : OrganizeTokensScreen("Button - Group")

    class Apply(
        grouping: AnalyticsParam.OnOffState,
        organizeSortType: AnalyticsParam.OrganizeSortType,
    ) : OrganizeTokensScreen(
        "Button - Apply", params = mapOf(
            "Group" to grouping.value,
            "Sort" to organizeSortType.value
        )
    )

    object Cancel : OrganizeTokensScreen("Button - Cancel")
}
