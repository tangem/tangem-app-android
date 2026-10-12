package com.tangem.feature.swap.model

import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.feature.swap.analytics.SwapEvents
import com.tangem.feature.swap.domain.models.ui.SwapState
import com.tangem.feature.swap.models.states.SwapNotificationUM
import java.math.BigDecimal

/**
 * Reports the low custom slippage notice once per appearance: quotes are polled, so the notice is rebuilt on
 * every reload. A different low value counts as a new appearance.
 */
internal class LowSlippageNoticeAnalytics(private val analyticsEventHandler: AnalyticsEventHandler) {

    private var reportedSlippage: BigDecimal? = null

    fun onQuotesLoaded(
        state: SwapState.QuotesLoadedState,
        notifications: List<NotificationUM>,
        customSlippage: BigDecimal?,
    ) {
        val isShown = notifications.any { it is SwapNotificationUM.Warning.LowSlippage }
        when {
            !isShown || customSlippage == null -> reset()
            reportedSlippage?.compareTo(customSlippage) != 0 -> {
                analyticsEventHandler.send(
                    SwapEvents.NoticeCustomSlippageThreshold(
                        fromCurrency = state.fromTokenInfo.swapCurrencyStatus.currency,
                        toCurrency = state.toTokenInfo.swapCurrencyStatus.currency,
                        slippage = customSlippage,
                    ),
                )
                reportedSlippage = customSlippage
            }
        }
    }

    fun reset() {
        reportedSlippage = null
    }
}