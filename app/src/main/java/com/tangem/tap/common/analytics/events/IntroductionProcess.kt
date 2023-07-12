package com.tangem.tap.common.analytics.events

import com.tangem.core.analytics.models.AnalyticsEvent

/**
[REDACTED_AUTHOR]
 */
sealed class IntroductionProcess(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent("Introduction Process", event, params) {

    class ScreenOpened : IntroductionProcess("Introduction Process Screen Opened")
    class ButtonTokensList : IntroductionProcess("Button - Tokens List")
    class ButtonBuyCards : IntroductionProcess("Button - Buy Cards")
    class ButtonScanCard : IntroductionProcess("Button - Scan Card")
    class ButtonRequestSupport : IntroductionProcess("Button - Request Support")
}