package com.tangem.feature.swap.analytics

import com.tangem.common.ui.bottomsheet.permission.state.ApproveType
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsEvent.Companion.asStringValue
import com.tangem.core.analytics.models.EventValue
import com.tangem.feature.swap.domain.models.domain.SwapProvider
import com.tangem.feature.swap.domain.models.ui.FeeType

private const val SWAP_CATEGORY = "Swap"
private const val PROMO_CATEGORY = "Promo"

sealed class SwapEvents(
    event: String,
    params: Map<String, EventValue> = mapOf(),
) : AnalyticsEvent(SWAP_CATEGORY, event, params) {

    data class SwapScreenOpened(val token: String) : SwapEvents(
        event = "Swap Screen Opened",
        params = mapOf("Token" to token.asStringValue()),
    )

    data object SendTokenBalanceClicked : SwapEvents(event = "Send Token Balance Clicked")

    data class ChooseTokenScreenOpened(val availableTokens: Boolean) : SwapEvents(
        event = "Choose Token Screen Opened",
        params = mapOf("Available tokens" to if (availableTokens) "Yes".asStringValue() else "No".asStringValue()),
    )

    data class ChooseTokenScreenResult(val tokenChosen: Boolean, val token: String? = null) : SwapEvents(
        event = "Choose Token Screen Result",
        params = buildMap {
            put("Token Chosen", if (tokenChosen) "Yes".asStringValue() else "No".asStringValue())
            token?.let { put("Token", it.asStringValue()) }
        },
    )

    data class ButtonSwapClicked(val sendToken: String, val receiveToken: String) : SwapEvents(
        event = "Button - Swap",
        params = mapOf("Send Token" to sendToken.asStringValue(), "Receive Token" to receiveToken.asStringValue()),
    )

    data object ButtonGivePermissionClicked : SwapEvents(event = "Button - Give permission")

    data class ButtonPermissionApproveClicked(
        val sendToken: String,
        val receiveToken: String,
        val approveType: ApproveType,
    ) : SwapEvents(
        event = "Button - Permission Approve",
        params = mapOf(
            "Send Token" to sendToken.asStringValue(),
            "Receive Token" to receiveToken.asStringValue(),
            "Type" to if (approveType == ApproveType.LIMITED) {
                "Current Transaction".asStringValue()
            } else {
                "Unlimited".asStringValue()
            },
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
            "Provider" to provider.name.asStringValue(),
            "Commission" to if (commission == FeeType.NORMAL) {
                "Market".asStringValue()
            } else {
                "Fast".asStringValue()
            },
            "Send Token" to sendToken.asStringValue(),
            "Receive Token" to receiveToken.asStringValue(),
            "Send Blockchain" to sendBlockchain.asStringValue(),
            "Receive Blockchain" to receiveBlockchain.asStringValue(),
        ),
    )

    data object ProviderClicked : SwapEvents("Provider Clicked")

    data class ProviderChosen(val provider: SwapProvider) : SwapEvents(
        event = "Provider Chosen",
        params = mapOf("Provider" to provider.name.asStringValue()),
    )

    data class ButtonStatus(val token: String) : SwapEvents(
        event = "Button - Status",
        params = mapOf("Token" to token.asStringValue()),
    )

    data class ButtonExplore(val token: String) : SwapEvents(
        event = "Button - Explore",
        params = mapOf("Token" to token.asStringValue()),
    )

    data object NoticeNoAvailableTokensToSwap : SwapEvents("Notice - No Available Tokens To Swap")

    data class NoticeNotEnoughFee(val token: String, val blockchain: String) : SwapEvents(
        event = "Notice - Not Enough Fee",
        params = mapOf(
            "Token" to token.asStringValue(),
            "Blockchain" to blockchain.asStringValue(),
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
            "Send Token" to sendToken.asStringValue(),
            "Receive Token" to receiveToken.asStringValue(),
            "Provider" to provider.name.asStringValue(),
            "Error Code" to errorCode.asStringValue(),
        ),
    )

    // region Promo activity
    data class ChangellyActivity(
        val promoState: PromoState,
    ) : AnalyticsEvent(
        category = PROMO_CATEGORY,
        event = "Changelly Activity",
        params = mapOf(
            "State" to promoState.name.asStringValue(),
        ),
    ) {
        sealed class PromoState(val name: String) {
            data object Native : PromoState("Native")
            data object Recommended : PromoState("Recommended")
        }
    }
}