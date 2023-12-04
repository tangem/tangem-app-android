package com.tangem.feature.swap.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.feature.swap.domain.models.domain.SwapProvider
import com.tangem.feature.swap.domain.models.ui.FeeType

sealed class SwapEvents(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent(SWAP_CATEGORY, event, params) {

    data class SwapScreenOpened(val token: String) : SwapEvents(
        event = "Swap Screen Opened",
        params = mapOf("Token" to token),
    )

    object SendTokenBalanceClicked : SwapEvents(event = "Send Token Balance Clicked")

    data class ChooseTokenScreenOpened(val availableTokens: Boolean) : SwapEvents(
        event = "Choose Token Screen Opened",
        params = mapOf("Available tokens" to if (availableTokens) "Yes" else "No")
    )

    data class СhooseTokenScreenResult(val tokenChosen: Boolean) : SwapEvents(
        event = "Сhoose Token Screen Result",
        params = mapOf("Token Chosen" to if (tokenChosen) "Yes" else "No")
    )

    data class SearchTokenClicked(val token: String) : SwapEvents(
        event = "Searched Token Clicked",
        params = mapOf("Token" to token)
    )

    data class ButtonSwapClicked(val sendToken: String, val receiveToken: String) : SwapEvents(
        event = "Button - Swap",
        params = mapOf("Send Token" to sendToken, "Receive Token" to receiveToken),
    )

    object ButtonGivePermissionClicked : SwapEvents(event = "Button - Give permission")

    data class ButtonPermissionApproveClicked(val sendToken: String, val receiveToken: String) : SwapEvents(
        event = "Button - Permission Approve",
        params = mapOf("Send Token" to sendToken, "Receive Token" to receiveToken),
    )

    object ButtonPermissionCancelClicked : SwapEvents(event = "Button - Permission Cancel")

    object ButtonSwipeClicked : SwapEvents(event = "Button - Swipe")

    data class SwapInProgressScreen(
        val provider: SwapProvider,
        val commission: FeeType, // Market / Fast
        val sendToken: String,
        val receiveToken: String,
    ) : SwapEvents(
        event = "Swap in Progress Screen Opened",
        params = mapOf(
            "Provider" to provider.name,
            "Commission" to if (commission == FeeType.NORMAL) "Market" else "Fast",
            "Send Token" to sendToken,
            "Receive Token" to receiveToken
        )
    )

    object ProviderClicked : SwapEvents("Provider Clicked")

    data class ProviderChosen(val provider: SwapProvider) : SwapEvents(
        event = "Provider Chosen",
        params = mapOf("Provider" to provider.name)
    )

    data class ButtonStatus(val token: String) : SwapEvents(
        event = "Button - Status",
        params = mapOf("Token" to token)
    )

    data class ButtonExplore(val token: String) : SwapEvents(
        event = "Button - Explore",
        params = mapOf("Token" to token)
    )

    object NoticeNoAvailableTokensToSwap : SwapEvents("Notice -  No Available Tokens To Swap")

    object NoticeExchangeRateHasExpired : SwapEvents("Notice - Exchange Rate Has Expired")

    data class NoticeNotEnoughFee(val token: String, val blockchain: String) : SwapEvents(
        event = "Notice - Not Enough Fee",
        params = mapOf(
            "Token" to token,
            "Blockchain" to blockchain
        )
    )

    data class NoticeProviderError(val token: String, val provider: SwapProvider) : SwapEvents(
        event = "Notice - Provider Error",
        params = mapOf(
            "Token" to token,
            "Provider" to provider.name
        )
    )

    data class NoticeExchangeError(
        val sendToken: String,
        val receiveToken: String,
        val provider: SwapProvider,
    ) : SwapEvents(
        event = "Notice - Exchange Error",
        params = mapOf(
            "Send Token" to sendToken,
            "Receive Token" to receiveToken,
            "Provider" to provider.name
        )
    )
}

private const val SWAP_CATEGORY = "Swap"
