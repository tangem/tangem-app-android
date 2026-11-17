package com.tangem.features.earn.flow.impl.analytics

import com.tangem.core.analytics.api.AnalyticsEventHandler

internal enum class EarnFlowNotice { InsufficientBalance, InsufficientFee }

internal class EarnFlowNoticeAnalytics(
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val category: String,
) {

    private val shown = mutableMapOf<EarnFlowScreen, Set<EarnFlowNotice>>()

    fun update(screen: EarnFlowScreen, notices: Set<EarnFlowNotice>) {
        val previous = shown.put(screen, notices).orEmpty()
        (notices - previous).forEach { notice ->
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
}