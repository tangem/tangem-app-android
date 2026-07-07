package com.tangem.features.commonfeatures.impl.managefunds.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam

internal sealed class ManageFundsAnalyticsEvent(
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent(category = CATEGORY, event = event, params = params) {

    class MethodScreenOpened(source: String) : ManageFundsAnalyticsEvent(
        event = "Method Screen Opened",
        params = mapOf(AnalyticsParam.SOURCE to source),
    )

    class ButtonBuy : ManageFundsAnalyticsEvent(event = "Button - Buy")

    class ButtonSwap : ManageFundsAnalyticsEvent(event = "Button - Swap")

    class ButtonReceive : ManageFundsAnalyticsEvent(event = "Button - Receive")

    companion object {
        private const val CATEGORY = "Add Funds"
        const val SOURCE_MAIN_SCREEN = "Main Screen"
    }
}