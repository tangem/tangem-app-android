package com.tangem.feature.wallet.child.managetokens.analytics

import com.tangem.core.analytics.models.AnalyticsEvent

internal sealed class PortfolioAnalyticsEvent(
    event: String,
) : AnalyticsEvent(category = "Portfolio", event = event) {

    class ButtonAddManage : PortfolioAnalyticsEvent(event = "Button - Add Manage")

    class ButtonAddTokens : PortfolioAnalyticsEvent(event = "Button - Add tokens")

    class ButtonOrganizeTokens : PortfolioAnalyticsEvent(event = "Button - Organize Tokens")
}