package com.tangem.domain.tokens.model.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam.Key.ACTION
import com.tangem.core.analytics.models.AnalyticsParam.Key.BLOCKCHAIN
import com.tangem.core.analytics.models.AnalyticsParam.Key.STATUS
import com.tangem.core.analytics.models.AnalyticsParam.Key.TOKEN_PARAM
import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason

/**
[REDACTED_AUTHOR]
 */
sealed class TokenScreenAnalyticsEvent(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent("Token", event, params) {

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

    sealed class ButtonWithParams(
        event: String,
        token: String,
        blockchain: String,
        status: String?,
    ) : TokenScreenAnalyticsEvent(
        event = event,
        params = buildMap {
            put(TOKEN_PARAM, token)
            put(BLOCKCHAIN, blockchain)
            status?.let { put(STATUS, it) }
        },
    ) {

        class ButtonBuy(
            token: String,
            blockchain: String,
            status: String?,
        ) : ButtonWithParams(
            event = "Button - Buy",
            token = token,
            status = status,
            blockchain = blockchain,
        )

        class ButtonSell(
            token: String,
            status: String,
            blockchain: String,
        ) : ButtonWithParams(
            event = "Button - Sell",
            token = token,
            status = status,
            blockchain = blockchain,
        )

        class ButtonExchange(
            token: String,
            status: String,
            blockchain: String,
        ) : ButtonWithParams(
            event = "Button - Exchange",
            token = token,
            status = status,
            blockchain = blockchain,
        )

        class ButtonSend(
            token: String,
            status: String,
            blockchain: String,
        ) : ButtonWithParams(
            event = "Button - Send",
            token = token,
            status = status,
            blockchain = blockchain,
        )

        class ButtonReceive(
            token: String,
            status: String,
            blockchain: String,
        ) : ButtonWithParams(
            event = "Button - Receive",
            token = token,
            status = status,
            blockchain = blockchain,
        )
    }

    class ActionButtonDisabled(
        token: String,
        status: String,
        blockchain: String,
        action: String,
    ) : TokenScreenAnalyticsEvent(
        event = "Action Button Disabled",
        params = mapOf(
            TOKEN_PARAM to token,
            STATUS to status,
            BLOCKCHAIN to blockchain,
            ACTION to action,
        ),
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

    companion object {
        const val AVAILABLE = "Available"
        private const val UNAVAILABLE = "Unavailable"
        private const val EMPTY = "Empty"
        private const val PENDING = "Pending"
        private const val CACHING = "Caching"
        private const val CUSTOM_TOKEN = "Custom Token"
        private const val BLOCKCHAIN_UNREACHABLE = "Blockchain Unreachable"
        private const val ASSET_NOT_FOUND = "Asset NotFound"
        private const val ASSET_LOADING = "Assets Loading"
        private const val ASSETS_ERROR = "Assets Error"
        private const val NO_QUOTE = "Quotes Unavailable"
        private const val SINGLE_WALLET = "Single Wallet"
        private const val LOADING = "Loading"
        private const val ASSET_REQUIREMENT = "AssetRequirement"
        private const val TRUSTLINE_REQUIREMENT = "TrustlineRequirement"

        @Suppress("CyclomaticComplexMethod")
        fun ScenarioUnavailabilityReason.toReasonAnalyticsText(): String {
            return when (this) {
                is ScenarioUnavailabilityReason.BuyUnavailable,
                is ScenarioUnavailabilityReason.NotExchangeable,
                is ScenarioUnavailabilityReason.NotSupportedBySellService,
                is ScenarioUnavailabilityReason.StakingUnavailable,
                -> UNAVAILABLE
                ScenarioUnavailabilityReason.UnassociatedAsset -> ASSET_REQUIREMENT
                ScenarioUnavailabilityReason.TrustlineRequired -> TRUSTLINE_REQUIREMENT
                is ScenarioUnavailabilityReason.EmptyBalance -> EMPTY
                is ScenarioUnavailabilityReason.PendingTransaction -> PENDING
                ScenarioUnavailabilityReason.UsedOutdatedData -> CACHING
                ScenarioUnavailabilityReason.None -> AVAILABLE
                ScenarioUnavailabilityReason.Unreachable -> BLOCKCHAIN_UNREACHABLE
                is ScenarioUnavailabilityReason.AssetNotFound -> ASSET_NOT_FOUND
                is ScenarioUnavailabilityReason.CustomToken -> CUSTOM_TOKEN
                is ScenarioUnavailabilityReason.ExpressLoading -> ASSET_LOADING
                is ScenarioUnavailabilityReason.ExpressUnreachable -> ASSETS_ERROR
                ScenarioUnavailabilityReason.SingleWallet -> SINGLE_WALLET
                is ScenarioUnavailabilityReason.TokenNoQuotes -> NO_QUOTE
                ScenarioUnavailabilityReason.DataLoading -> LOADING
            }
        }
    }
}