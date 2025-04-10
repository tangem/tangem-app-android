package com.tangem.core.analytics.models.event

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.AnalyticsParam.Key.ERROR_CODE
import com.tangem.core.analytics.models.AnalyticsParam.Key.TOKEN_PARAM

/**
 * Main screen analytics event
 *
 * @param event  event name
 * @param params params
 */
sealed class MainScreenAnalyticsEvent(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent(category = "Main Screen", event = event, params = params) {

    class EnableBiometrics(state: AnalyticsParam.OnOffState) : MainScreenAnalyticsEvent(
        event = "Enable Biometric",
        params = mapOf("State" to state.value),
    )

    // region Action Buttons feature
    data class ButtonBuy(
        val status: AnalyticsParam.Status,
        val screenType: String? = null,
    ) : MainScreenAnalyticsEvent(
        event = "Button - Buy",
        params = buildMap {
            put(AnalyticsParam.STATUS, status.value)
            screenType?.let { put(AnalyticsParam.TYPE, it) }
        },
    )

    data object ButtonReceive : MainScreenAnalyticsEvent(
        event = "Button - Receive",
    )

    data object LimitsClicked : MainScreenAnalyticsEvent(
        event = "Limits Clicked",
    )

    data object NoticeBalancesInfo : MainScreenAnalyticsEvent(
        event = "Notice - Balances Info",
    )

    data object NoticeLimitsInfo : MainScreenAnalyticsEvent(
        event = "Notice - Limits Info",
    )

    data object ButtonExplore : MainScreenAnalyticsEvent(
        event = "Button - Explore",
    )

    data class ButtonSwap(val status: AnalyticsParam.Status) : MainScreenAnalyticsEvent(
        event = "Button - Swap",
        params = mapOf(AnalyticsParam.STATUS to status.value),
    )

    data class ButtonSell(val status: AnalyticsParam.Status) : MainScreenAnalyticsEvent(
        event = "Button - Sell",
        params = mapOf(AnalyticsParam.STATUS to status.value),
    )

    data object BuyScreenOpened : MainScreenAnalyticsEvent(event = "Buy Screen Opened")

    data object SwapScreenOpened : MainScreenAnalyticsEvent(event = "Swap Screen Opened")

    data object SellScreenOpened : MainScreenAnalyticsEvent(event = "Sell Screen Opened")

    data class BuyTokenClicked(val currencySymbol: String) : MainScreenAnalyticsEvent(
        event = "Buy Token Clicked",
        params = mapOf(TOKEN_PARAM to currencySymbol),
    )

    data class SellTokenClicked(val currencySymbol: String) : MainScreenAnalyticsEvent(
        event = "Sell Token Clicked",
        params = mapOf(TOKEN_PARAM to currencySymbol),
    )

    data class SwapTokenClicked(val currencySymbol: String) : MainScreenAnalyticsEvent(
        event = "Swap Token Clicked",
        params = mapOf(TOKEN_PARAM to currencySymbol),
    )

    data class ReceiveTokenClicked(val currencySymbol: String) : MainScreenAnalyticsEvent(
        event = "Receive Token Clicked",
        params = mapOf(TOKEN_PARAM to currencySymbol),
    )

    data class RemoveTokenClicked(val currencySymbol: String) : MainScreenAnalyticsEvent(
        event = "Remove Button Clicked",
        params = mapOf(TOKEN_PARAM to currencySymbol),
    )

    data class ButtonClose(val source: AnalyticsParam.ScreensSources) : MainScreenAnalyticsEvent(
        event = "Button - Close",
        params = mapOf(AnalyticsParam.SOURCE to source.value),
    )

    data class HotTokenClicked(val currencySymbol: String) : MainScreenAnalyticsEvent(
        event = "Hot Token Clicked",
        params = mapOf(TOKEN_PARAM to currencySymbol),
    )

    data class HotTokenError(val errorCode: String) : MainScreenAnalyticsEvent(
        event = "Hot Token Error",
        params = mapOf(ERROR_CODE to errorCode),
    )
    // endregion

    companion object {
        const val VISA_TYPE = "Visa"
        const val WALLET_TYPE = "Wallet"
    }
}