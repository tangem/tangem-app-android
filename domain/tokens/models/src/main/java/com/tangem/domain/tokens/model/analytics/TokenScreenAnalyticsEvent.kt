package com.tangem.domain.tokens.model.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason

/**
[REDACTED_AUTHOR]
 */
sealed class TokenScreenAnalyticsEvent(
    event: String,
    params: Map<String, String> = mapOf(),
    error: Throwable? = null,
) : AnalyticsEvent("Token", event, params, error) {

    /** Legacy event. It has a unique category, but it also is sent on TokenScreen */
    class DetailsScreenOpened(token: String) : AnalyticsEvent(
        category = "Details Screen",
        event = "Details Screen Opened",
        params = mapOf("Token" to token),
    )

    class ButtonRemoveToken(token: String) : TokenScreenAnalyticsEvent(
        "Button - Remove Token",
        params = mapOf("Token" to token),
    )

    class ButtonExplore(token: String) : TokenScreenAnalyticsEvent(
        event = "Button - Explore",
        params = mapOf("Token" to token),
    )

    class ButtonReload(token: String) : TokenScreenAnalyticsEvent(
        event = "Button - Reload",
        params = mapOf("Token" to token),
    )

    class ButtonBuy(token: String) : TokenScreenAnalyticsEvent(
        event = "Button - Buy",
        params = mapOf("Token" to token),
    )

    class ButtonSell(token: String) : TokenScreenAnalyticsEvent(
        event = "Button - Sell",
        params = mapOf("Token" to token),
    )

    class ButtonExchange(token: String) : TokenScreenAnalyticsEvent(
        event = "Button - Exchange",
        params = mapOf("Token" to token),
    )

    class ButtonSend(token: String) : TokenScreenAnalyticsEvent(
        event = "Button - Send",
        params = mapOf("Token" to token),
    )

    class ButtonReceive(token: String) : TokenScreenAnalyticsEvent(
        event = "Button - Receive",
        params = mapOf("Token" to token),
    )

    class ButtonCopyAddress(token: String) : TokenScreenAnalyticsEvent(
        event = "Button - Copy Address",
        params = mapOf("Token" to token),
    )

    class Bought(token: String) : TokenScreenAnalyticsEvent(
        event = "Token Bought",
        params = mapOf("Token" to token),
    )

    class Associate(tokenSymbol: String, blockchain: String) : TokenScreenAnalyticsEvent(
        event = "Button - Token Trustline",
        params = mapOf("Token" to tokenSymbol, "Blockchain" to blockchain),
    )

    class RevealTryAgain(tokenSymbol: String, blockchain: String) : TokenScreenAnalyticsEvent(
        event = "Button - Reveal Try Again",
        params = mapOf("Token" to tokenSymbol, "Blockchain" to blockchain),
    )

    class RevealCancel(tokenSymbol: String, blockchain: String) : TokenScreenAnalyticsEvent(
        event = "Button - Reveal Cancel",
        params = mapOf("Token" to tokenSymbol, "Blockchain" to blockchain),
    )

    data class StakingClicked(val token: String) : TokenScreenAnalyticsEvent(
        event = "Staking Clicked",
        params = mapOf("Token" to token),
    )

    class NoticeActionInactive(token: String, tokenAction: TokenAction, reason: String) : TokenScreenAnalyticsEvent(
        event = "Notice - Action Inactive",
        params = buildMap {
            put("Token", token)
            put("Action", tokenAction.action)
            if (reason.isNotEmpty()) {
                put("Reason", reason)
            }
        },
    ) {
        sealed class TokenAction(val action: String) {
            data object BuyAction : TokenAction("Buy")
            data object SellAction : TokenAction("Sell")
            data object SwapAction : TokenAction("Swap")
            data object SendAction : TokenAction("Send")
            data object ReceiveAction : TokenAction("Receive")
        }
    }

    companion object {
        private const val UNAVAILABLE = "Unavailable"
        private const val EMPTY = "Empty"
        private const val PENDING = "Pending"
        private const val CACHING = "Caching"

        fun ScenarioUnavailabilityReason.toReasonAnalyticsText(): String {
            return when (this) {
                is ScenarioUnavailabilityReason.BuyUnavailable,
                is ScenarioUnavailabilityReason.NotExchangeable,
                is ScenarioUnavailabilityReason.NotSupportedBySellService,
                is ScenarioUnavailabilityReason.StakingUnavailable,
                ScenarioUnavailabilityReason.UnassociatedAsset,
                -> UNAVAILABLE
                is ScenarioUnavailabilityReason.EmptyBalance -> EMPTY
                is ScenarioUnavailabilityReason.PendingTransaction -> PENDING
                ScenarioUnavailabilityReason.UsedOutdatedData -> CACHING
                ScenarioUnavailabilityReason.None,
                ScenarioUnavailabilityReason.Unreachable,
                -> ""
            }
        }
    }
}