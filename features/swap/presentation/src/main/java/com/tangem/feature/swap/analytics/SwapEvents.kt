package com.tangem.feature.swap.analytics

import com.tangem.core.analytics.AnalyticsEvent

sealed class SwapEvents(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent(SWAP_CATEGORY, event, params) {

    data class SwapScreenOpened(val token: String) : SwapEvents(
        event = "Swap Screen Opened",
        params = mapOf("Token" to token),
    )

    object SendTokenBalanceClicked : SwapEvents(event = "Send Token Balance Clicked")
    object ReceiveTokenClicked : SwapEvents(event = "Receive Token Clicked")
    object ChooseTokenScreenOpened : SwapEvents(event = "Choose Token Screen Opened")
    object SearchTokenClicked : SwapEvents(event = "Search Token Clicked")
    data class ButtonSwapClicked(val sendToken: String, val receiveToken: String) : SwapEvents(
        event = "Button - Swap",
        params = mapOf("Send Token" to sendToken, "Receive Token" to receiveToken),
    )

    object ButtonGivePermissionClicked : SwapEvents(event = "Button - Give permission") // ?
    object ButtonPermissionApproveClicked : SwapEvents(event = "Button - Permission Approve")
    object ButtonPermissionCancelClicked : SwapEvents(event = "Button - Permission Cancel")
    object ButtonPermitAndSwapClicked : SwapEvents(event = "Button - Permit and Swap") // ?
    object ButtonSwipeClicked : SwapEvents(event = "Button - Swipe")
    object SwapInProgressScreen : SwapEvents(event = "Swap in Progress Screen Opened")
}

private const val SWAP_CATEGORY = "Swap"
