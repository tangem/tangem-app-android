package com.tangem.feature.wallet.presentation.wallet.analytics

import com.tangem.core.analytics.models.AnalyticsEvent

sealed class PortfolioEvent(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent("Portfolio", event, params) {

    object Refreshed : PortfolioEvent("Refreshed")

    object ButtonManageTokens : PortfolioEvent("Button - Manage Tokens")

    object TokenTapped : PortfolioEvent("Token is Tapped")

    object OrganizeTokens : PortfolioEvent("Button - Organize Tokens")
}