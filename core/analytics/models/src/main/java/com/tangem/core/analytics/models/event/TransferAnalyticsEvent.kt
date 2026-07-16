package com.tangem.core.analytics.models.event

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam

sealed class TransferAnalyticsEvent(
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent(category = "Transfer", event = event, params = params) {

    class MethodScreenOpened(source: AnalyticsParam.ScreensSources) : TransferAnalyticsEvent(
        event = "Method Screen Opened",
        params = mapOf(AnalyticsParam.SOURCE to source.value),
    )

    class ButtonSell : TransferAnalyticsEvent(event = "Button - Sell")

    class ButtonSwap : TransferAnalyticsEvent(event = "Button - Swap")

    class ButtonSend : TransferAnalyticsEvent(event = "Button - Send")

    class ButtonSwapAndSend : TransferAnalyticsEvent(event = "Button - Swap&Send")
}