package com.tangem.features.commonfeatures.impl.addtoportfolio.analytics

import com.tangem.common.ui.markets.action.TokenActionsBSContentUM
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam

internal class PortfolioAnalyticsEvent(
    event: String,
    params: Map<String, String> = emptyMap(),
    category: String,
) : AnalyticsEvent(category = category, event = event, params = params) {

    data class EventBuilder(
        val tokenSymbol: String,
        val source: String?,
        val category: String,
    ) {

        fun popupToChooseAccount() = PortfolioAnalyticsEvent(
            event = "Choose Account Opened",
            category = category,
            params = buildMap {
                if (source != null) put(AnalyticsParam.SOURCE, source)
            },
        )

        fun popupToConfirm(blockchain: String) = PortfolioAnalyticsEvent(
            event = "Add Token Screen Opened",
            category = category,
            params = buildMap {
                put(AnalyticsParam.TOKEN_PARAM, tokenSymbol)
                put(AnalyticsParam.BLOCKCHAIN, blockchain)
                if (source != null) put(AnalyticsParam.SOURCE, source)
            },
        )

        fun addToNotMainAccount() = PortfolioAnalyticsEvent(
            event = "Button - Add To Account",
            category = category,
            params = buildMap {
                if (source != null) put("Source", source)
            },
        )

        fun addButtonClick() = PortfolioAnalyticsEvent(
            event = "Button - Add Token",
            category = category,
            params = buildMap {
                if (source != null) put("Source", source)
            },
        )

        fun addToPortfolioWalletChanged() = PortfolioAnalyticsEvent(
            event = "Wallet Selected",
            category = category,
            params = buildMap {
                if (source != null) put("Source", source)
            },
        )

        fun addToPortfolioContinue(blockchainNames: List<String>) = PortfolioAnalyticsEvent(
            event = "Token Network Selected",
            category = category,
            params = buildMap {
                put("Count", blockchainNames.size.toString())
                put("Token", tokenSymbol)
                put("blockchain", blockchainNames.joinToString(separator = ", "))
                if (source != null) put("Source", source)
            },
        )

        fun tokenAdded(blockchainName: String) = PortfolioAnalyticsEvent(
            event = "Token Added",
            category = category,
            params = buildMap {
                put(AnalyticsParam.TOKEN_PARAM, tokenSymbol)
                put(AnalyticsParam.BLOCKCHAIN, blockchainName)
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
            category = category,
            params = buildMap {
                if (source != null) put("Source", source)
            },
        )
    }
}