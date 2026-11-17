package com.tangem.features.earn.flow.impl.analytics

import com.tangem.core.analytics.api.AnalyticsEventHandler

internal enum class EarnFlowNotice { InsufficientBalance, InsufficientFee }

internal class EarnFlowNoticeAnalytics(
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val category: String,
) {

    private var screen: EarnFlowScreen? = null
    private var shown: Set<EarnFlowNotice> = emptySet()

    fun update(screen: EarnFlowScreen, notices: Set<EarnFlowNotice>, isFeeInFlight: Boolean = false) {
        val previous = if (screen == this.screen) shown else emptySet()
        // A fee reload empties the fee cards for a moment, which is not the fee notice going away
        val current = if (isFeeInFlight) notices.withFeeNoticeOf(previous) else notices
        this.screen = screen
        shown = current
        (current - previous).forEach { notice ->
            analyticsEventHandler.send(
                when (notice) {
                    EarnFlowNotice.InsufficientBalance ->
                        EarnFlowAnalyticsEvent.InsufficientBalance(category = category, screen = screen)
                    EarnFlowNotice.InsufficientFee ->
                        EarnFlowAnalyticsEvent.InsufficientFee(category = category, screen = screen)
                },
            )
        }
    }

    private fun Set<EarnFlowNotice>.withFeeNoticeOf(previous: Set<EarnFlowNotice>): Set<EarnFlowNotice> =
        if (EarnFlowNotice.InsufficientFee in previous) {
            this + EarnFlowNotice.InsufficientFee
        } else {
            this - EarnFlowNotice.InsufficientFee
        }
}