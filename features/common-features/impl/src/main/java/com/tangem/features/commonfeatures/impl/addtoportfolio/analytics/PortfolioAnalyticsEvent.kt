package com.tangem.features.commonfeatures.impl.addtoportfolio.analytics

import com.tangem.common.ui.markets.action.TokenActionsBSContentUM
import com.tangem.core.analytics.models.AnalyticsEvent

// todo swap unify with EarnAnalyticsEvent, AddToPortfolioFlow
internal class PortfolioAnalyticsEvent(
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent(category = "Markets / Chart", event = event, params = params) {

    data class EventBuilder(
        val tokenSymbol: String,
        val source: String?,
    ) {

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
                put("Token", tokenSymbol)
                put("blockchain", blockchainNames.joinToString(separator = ", "))
                if (source != null) put("Source", source)
            },
        )

        fun tokenAdded(blockchainName: String) = PortfolioAnalyticsEvent(
            event = "Token Added",
            params = buildMap {
                put("Token", tokenSymbol)
                put("Blockchain", blockchainName)
                if (source != null) put("Source", source)
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