package com.tangem.features.commonfeatures.impl.addfunds.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam

internal sealed class AddFundsAnalyticsEvent(
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent(category = CATEGORY, event = event, params = params) {

    class MethodScreenOpened(source: String) : AddFundsAnalyticsEvent(
        event = "Method Screen Opened",
        params = mapOf(AnalyticsParam.SOURCE to source),
    )

    class ButtonBuy : AddFundsAnalyticsEvent(event = "Button - Buy")

    class ButtonSwap : AddFundsAnalyticsEvent(event = "Button - Swap")

    class ButtonReceive : AddFundsAnalyticsEvent(event = "Button - Receive")

    class ButtonGoToToken : AddFundsAnalyticsEvent(event = "Button - Go to Token")

    companion object {
        private const val CATEGORY = "Add Funds"
        const val SOURCE_MAIN_SCREEN = "Main Screen"
    }
}