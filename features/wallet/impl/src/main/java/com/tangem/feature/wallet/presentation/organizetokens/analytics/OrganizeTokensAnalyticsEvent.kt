package com.tangem.feature.wallet.presentation.organizetokens.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam

sealed class OrganizeTokensAnalyticsEvent(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent("Portfolio / Organize Tokens", event, params) {

    object ScreenOpened : OrganizeTokensAnalyticsEvent("Organize Tokens Screen Opened")

    object ByBalance : OrganizeTokensAnalyticsEvent("Button - By Balance")

    object Group : OrganizeTokensAnalyticsEvent("Button - Group")

    class Apply(
        grouping: AnalyticsParam.OnOffState,
        organizeSortType: AnalyticsParam.OrganizeSortType,
    ) : OrganizeTokensAnalyticsEvent(
        "Button - Apply",
        params = mapOf(
            "Group" to grouping.value,
            "Sort" to organizeSortType.value,
        ),
    )

    object Cancel : OrganizeTokensAnalyticsEvent("Button - Cancel")
}
