package com.tangem.features.markets.portfolio.impl.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.domain.markets.TokenMarketParams
import com.tangem.features.markets.portfolio.impl.ui.state.TokenActionsBSContentUM

internal class PortfolioAnalyticsEvent(
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent(category = "Markets / Chart", event = event, params = params) {

    data class EventBuilder(
        val token: TokenMarketParams,
        val source: String?,
    ) {

        fun addToPortfolioClicked() = PortfolioAnalyticsEvent(
            event = "Button - Add To Portfolio",
            params = buildMap {
                put("Token", token.symbol)
                if (source != null) put("Source", source)
            },
        )

        fun popupToChooseAccount() = PortfolioAnalyticsEvent(
            event = "Choose Account Opened",
            params = buildMap {
                if (source != null) put("Source", source)
            },
        )

        fun popupToConfirm() = PortfolioAnalyticsEvent(
            event = "Add Token Screen Opened",
            params = buildMap {
                if (source != null) put("Source", source)
            },
        )

        fun addToNotMainAccount() = PortfolioAnalyticsEvent(
            event = "Button - Add To Account",
            params = buildMap {
                if (source != null) put("Source", source)
            },
        )

        fun addButtonClick() = PortfolioAnalyticsEvent(
            event = "Button - Add Token",
            params = buildMap {
                if (source != null) put("Source", source)
            },
        )

        fun addToPortfolioWalletChanged() = PortfolioAnalyticsEvent(
            event = "Wallet Selected",
            params = buildMap {
                if (source != null) put("Source", source)
            },
        )

        fun addToPortfolioContinue(blockchainNames: List<String>) = PortfolioAnalyticsEvent(
            event = "Token Network Selected",
            params = buildMap {
                put("Count", blockchainNames.size.toString())
                put("Token", token.symbol)
                put("blockchain", blockchainNames.joinToString(separator = ", "))
                if (source != null) put("Source", source)
            },
        )

        fun tokenAdded(blockchainName: String) = PortfolioAnalyticsEvent(
            event = "Token Added",
            params = buildMap {
                put("Token", token.symbol)
                put("Blockchain", blockchainName)
                if (source != null) put("Source", source)
            },
        )

        fun quickActionClick(actionUM: TokenActionsBSContentUM.Action, blockchainName: String) =
            PortfolioAnalyticsEvent(
                event = when (actionUM) {
                    TokenActionsBSContentUM.Action.Buy -> "Button - Buy"
                    TokenActionsBSContentUM.Action.Receive -> "Button - Receive"
                    TokenActionsBSContentUM.Action.Exchange -> "Button - Swap"
                    TokenActionsBSContentUM.Action.Stake -> "Button - Stake"
                    TokenActionsBSContentUM.Action.YieldMode -> "Button - Yield Mode"
                    else -> "error"
                },
                params = buildMap {
                    put("Token", token.symbol)
                    if (source != null) put("Source", source)
                    put("blockchain", blockchainName)
                },
            )

        fun getTokenActionClick(actionUM: TokenActionsBSContentUM.Action) = PortfolioAnalyticsEvent(
            event = when (actionUM) {
                TokenActionsBSContentUM.Action.Buy -> "Popup Get token - Button Buy"
                TokenActionsBSContentUM.Action.Receive -> "Popup Get token - Button Receive"
                TokenActionsBSContentUM.Action.Exchange -> "Popup Get token - Button Exchange"
                TokenActionsBSContentUM.Action.Stake -> "Popup Get token - Button Stake"
                else -> "error"
            },
            params = buildMap {
                if (source != null) put("Source", source)
            },
        )

        fun getTokenLater() = PortfolioAnalyticsEvent(
            event = "Popup Get token - Button Later",
            params = buildMap {
                if (source != null) put("Source", source)
            },
        )
    }
}