package com.tangem.core.analytics.models.event

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam

/** Shown with the "Network fee info unreachable" banner on any screen that calculates a fee. */
class NetworkFeeUnavailableAnalyticsEvent(
    source: Source,
) : AnalyticsEvent(
    category = "Notice",
    event = "Network Fee Unavailable",
    params = mapOf(AnalyticsParam.SOURCE to source.value),
) {

    enum class Source(val value: String) {
        Send("Send"),
        Swap("Swap"),
        Staking("Staking"),
        WalletConnect("WalletConnect"),
        Earning("Earning"),
    }
}