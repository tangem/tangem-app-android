package com.tangem.domain.card.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam

sealed class IntroductionProcess(
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent("Introduction Process", event, params) {

    object ScreenOpened : IntroductionProcess("Introduction Process Screen Opened")
    object ButtonTokensList : IntroductionProcess("Button - Tokens List")
    object ButtonBuyCards : IntroductionProcess("Button - Buy Cards")
    object ButtonScanCardLegacy : IntroductionProcess("Button - Scan Card")

    class ButtonScanCard(
        val source: AnalyticsParam.ScreensSources,
    ) : IntroductionProcess(
        event = "Button - Scan Card",
        params = mapOf(
            AnalyticsParam.Key.SOURCE to source.value,
        ),
    )
}