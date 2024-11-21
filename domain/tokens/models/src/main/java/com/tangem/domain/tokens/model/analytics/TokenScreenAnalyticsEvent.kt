package com.tangem.domain.tokens.model.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.EventValue
import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason

/**
[REDACTED_AUTHOR]
 */
sealed class TokenScreenAnalyticsEvent(
    event: String,
    params: Map<String, EventValue> = mapOf(),
    error: Throwable? = null,
) : AnalyticsEvent("Token", event, params, error) {

    /** Legacy event. It has a unique category, but it also is sent on TokenScreen */
    class DetailsScreenOpened(token: String) : AnalyticsEvent(
        category = "Details Screen",
        event = "Details Screen Opened",
        params = mapOf("Token" to token.asStringValue()),
    )

    class ButtonRemoveToken(token: String) : TokenScreenAnalyticsEvent(
        "Button - Remove Token",
        params = mapOf("Token" to token.asStringValue()),
    )

    class ButtonExplore(token: String) : TokenScreenAnalyticsEvent(
        event = "Button - Explore",
        params = mapOf("Token" to token.asStringValue()),
    )

    class ButtonReload(token: String) : TokenScreenAnalyticsEvent(
        event = "Button - Reload",
        params = mapOf("Token" to token.asStringValue()),
    )

    class ButtonBuy(token: String) : TokenScreenAnalyticsEvent(
        event = "Button - Buy",
        params = mapOf("Token" to token.asStringValue()),
    )

    class ButtonSell(token: String) : TokenScreenAnalyticsEvent(
        event = "Button - Sell",
        params = mapOf("Token" to token.asStringValue()),
    )

    class ButtonExchange(token: String) : TokenScreenAnalyticsEvent(
        event = "Button - Exchange",
        params = mapOf("Token" to token.asStringValue()),
    )

    class ButtonSend(token: String) : TokenScreenAnalyticsEvent(
        event = "Button - Send",
        params = mapOf("Token" to token.asStringValue()),
    )

    class ButtonReceive(token: String) : TokenScreenAnalyticsEvent(
        event = "Button - Receive",
        params = mapOf("Token" to token.asStringValue()),
    )

    class ButtonCopyAddress(token: String) : TokenScreenAnalyticsEvent(
        event = "Button - Copy Address",
        params = mapOf("Token" to token.asStringValue()),
    )

    class Bought(token: String) : TokenScreenAnalyticsEvent(
        event = "Token Bought",
        params = mapOf("Token" to token.asStringValue()),
    )

    class Associate(tokenSymbol: String, blockchain: String) : TokenScreenAnalyticsEvent(
        event = "Button - Token Trustline",
        params = mapOf(
            "Token" to tokenSymbol.asStringValue(),
            "Blockchain" to blockchain.asStringValue(),
        ),
    )

    data class StakingClicked(val token: String) : TokenScreenAnalyticsEvent(
        event = "Staking Clicked",
        params = mapOf("Token" to token.asStringValue()),
    )

    class NoticeActionInactive(token: String, tokenAction: TokenAction, reason: String) : TokenScreenAnalyticsEvent(
        "Notice - Action Inactive",
        params = buildMap {
            put("Token", token.asStringValue())
            put("Action", tokenAction.action.asStringValue())
            if (reason.isNotEmpty()) {
                put("Reason", reason.asStringValue())
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

        fun ScenarioUnavailabilityReason.toReasonAnalyticsText(): String {
            return when (this) {
                is ScenarioUnavailabilityReason.BuyUnavailable -> UNAVAILABLE
                is ScenarioUnavailabilityReason.EmptyBalance -> EMPTY
                ScenarioUnavailabilityReason.None -> ""
                is ScenarioUnavailabilityReason.NotExchangeable -> UNAVAILABLE
                is ScenarioUnavailabilityReason.NotSupportedBySellService -> UNAVAILABLE
                is ScenarioUnavailabilityReason.PendingTransaction -> PENDING
                is ScenarioUnavailabilityReason.StakingUnavailable -> UNAVAILABLE
                ScenarioUnavailabilityReason.UnassociatedAsset -> UNAVAILABLE
                ScenarioUnavailabilityReason.Unreachable -> ""
            }
        }
    }
}