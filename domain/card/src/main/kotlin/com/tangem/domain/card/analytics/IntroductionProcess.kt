package com.tangem.domain.card.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.getReferralParams

sealed class IntroductionProcess(
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent("Introduction Process", event, params) {

    class ScreenOpened : IntroductionProcess("Introduction Process Screen Opened")
    class ButtonTokensList : IntroductionProcess("Button - Tokens List")
    class ButtonBuyCards : IntroductionProcess("Button - Buy Cards")
    class ButtonScanCardLegacy : IntroductionProcess("Button - Scan Card")

    class CreateWalletIntroScreenOpened(
        referralId: String?,
    ) : IntroductionProcess(
        event = "Create Wallet Intro Screen Opened",
        params = buildMap {
            putAll(getReferralParams(referralId))
        },
    )

    class ButtonScanCard(
        val source: AnalyticsParam.ScreensSources,
    ) : IntroductionProcess(
        event = "Button - Scan Card",
        params = mapOf(
            AnalyticsParam.Key.SOURCE to source.value,
        ),
    )
}