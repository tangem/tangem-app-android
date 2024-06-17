package com.tangem.tap.common.analytics.events

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam

class ScanFailsDialogAnalytics(button: Buttons, source: AnalyticsParam.ScreensSources) : AnalyticsEvent(
    category = "Cant Scan The Card",
    event = button.event,
    params = mapOf(
        AnalyticsParam.SOURCE to source.value,
    ),
) {
    enum class Buttons(val event: String) {
        TRY_AGAIN("Try again button"),
        HOW_TO_SCAN("Button blog"),
    }
}
