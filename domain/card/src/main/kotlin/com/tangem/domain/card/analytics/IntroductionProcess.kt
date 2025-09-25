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
    object ButtonCreateNewWallet : IntroductionProcess("Button - Create New Wallet")
    object ButtonAddExistingWallet : IntroductionProcess("Button - Add Existing Wallet")
    data class ButtonScanCard(
        val source: AnalyticsParam.ScreensSources,
    ) : IntroductionProcess(
        event = "Button - Scan Card",
        params = mapOf(
            AnalyticsParam.SOURCE to source.value,
        ),
    )
}