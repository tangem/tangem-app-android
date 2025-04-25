package com.tangem.feature.swap.analytics

import com.tangem.common.ui.bottomsheet.permission.state.ApproveType
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.feature.swap.domain.models.domain.SwapProvider
import com.tangem.feature.swap.domain.models.ui.FeeType

private const val SWAP_CATEGORY = "Swap"
private const val PROMO_CATEGORY = "Promo"

sealed class SwapEvents(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent(SWAP_CATEGORY, event, params) {

    data class SwapScreenOpened(val token: String) : SwapEvents(
        event = "Swap Screen Opened",
        params = mapOf("Token" to token),
    )

    data object SendTokenBalanceClicked : SwapEvents(event = "Send Token Balance Clicked")

    data class ChooseTokenScreenOpened(val availableTokens: Boolean) : SwapEvents(
        event = "Choose Token Screen Opened",
        params = mapOf("Available tokens" to if (availableTokens) "Yes" else "No"),
    )

    data class ChooseTokenScreenResult(val tokenChosen: Boolean, val token: String? = null) : SwapEvents(
        event = "Choose Token Screen Result",
        params = buildMap {
            put("Token Chosen", if (tokenChosen) "Yes" else "No")
            token?.let { put("Token", it) }
        },
    )

    data class ButtonSwapClicked(val sendToken: String, val receiveToken: String) : SwapEvents(
        event = "Button - Swap",
        params = mapOf("Send Token" to sendToken, "Receive Token" to receiveToken),
    )

    data object ButtonGivePermissionClicked : SwapEvents(event = "Button - Give permission")

    data class ButtonPermissionApproveClicked(
        val sendToken: String,
        val receiveToken: String,
        val approveType: ApproveType,
    ) : SwapEvents(
        event = "Button - Permission Approve",
        params = mapOf(
            "Send Token" to sendToken,
            "Receive Token" to receiveToken,
            "Type" to if (approveType == ApproveType.LIMITED) "Current Transaction" else "Unlimited",
        ),
    )

    data object ButtonPermissionCancelClicked : SwapEvents(event = "Button - Permission Cancel")

    data object ButtonSwipeClicked : SwapEvents(event = "Button - Swipe")

    data class SwapInProgressScreen(
        val provider: SwapProvider,
        val commission: FeeType, // Market / Fast
        val sendBlockchain: String,
        val receiveBlockchain: String,
        val sendToken: String,
        val receiveToken: String,
    ) : SwapEvents(
        event = "Swap in Progress Screen Opened",
        params = mapOf(
            "Provider" to provider.name,
            "Commission" to if (commission == FeeType.NORMAL) "Market" else "Fast",
            "Send Token" to sendToken,
            "Receive Token" to receiveToken,
            "Send Blockchain" to sendBlockchain,
            "Receive Blockchain" to receiveBlockchain,
        ),
    )

    data object ProviderClicked : SwapEvents("Provider Clicked")

    data class ProviderChosen(val provider: SwapProvider) : SwapEvents(
        event = "Provider Chosen",
        params = mapOf("Provider" to provider.name),
    )

    data class ButtonStatus(val token: String) : SwapEvents(
        event = "Button - Status",
        params = mapOf("Token" to token),
    )

    data class ButtonExplore(val token: String) : SwapEvents(
        event = "Button - Explore",
        params = mapOf("Token" to token),
    )

    data object NoticeNoAvailableTokensToSwap : SwapEvents("Notice - No Available Tokens To Swap")

    data class NoticeNotEnoughFee(val token: String, val blockchain: String) : SwapEvents(
        event = "Notice - Not Enough Fee",
        params = mapOf(
            "Token" to token,
            "Blockchain" to blockchain,
        ),
    )

    data class NoticeProviderError(
        val sendToken: String,
        val receiveToken: String,
        val provider: SwapProvider,
        val errorCode: Int,
    ) : SwapEvents(
        event = "Notice - Express Error",
        params = mapOf(
            "Send Token" to sendToken,
            "Receive Token" to receiveToken,
            "Provider" to provider.name,
            "Error Code" to errorCode.toString(),
        ),
    )
    // TODO parameters

    // region Promo activity
    data class ChangellyActivity(
        val promoState: PromoState,
    ) : AnalyticsEvent(
        category = PROMO_CATEGORY,
        event = "Changelly Activity",
        params = mapOf(
            "State" to promoState.name,
        ),
    ) {
        sealed class PromoState(val name: String) {
            data object Native : PromoState("Native")
            data object Recommended : PromoState("Recommended")
        }
    }
}