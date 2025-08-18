package com.tangem.domain.card.analytics

import com.tangem.core.analytics.models.AnalyticsEvent

sealed class IntroductionProcess(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent("Introduction Process", event, params) {

    object ScreenOpened : IntroductionProcess("Introduction Process Screen Opened")
    object ButtonTokensList : IntroductionProcess("Button - Tokens List")
    object ButtonBuyCards : IntroductionProcess("Button - Buy Cards")
    object ButtonScanCard : IntroductionProcess("Button - Scan Card")
}