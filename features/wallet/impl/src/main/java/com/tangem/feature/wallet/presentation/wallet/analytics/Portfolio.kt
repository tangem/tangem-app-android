package com.tangem.feature.wallet.presentation.wallet.analytics

import com.tangem.core.analytics.models.AnalyticsEvent

sealed class Portfolio(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent("Portfolio", event, params) {

    object Refreshed : Portfolio("Refreshed")

    object ButtonManageTokens : Portfolio("Button - Manage Tokens")

    object TokenTapped : Portfolio("Token is Tapped")

    object OrganizeTokens : Portfolio("Button - Organize Tokens")
}
