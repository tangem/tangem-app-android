package com.tangem.feature.wallet.presentation.wallet.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam

sealed class WalletScreenAnalyticsEvent(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent("Main Screen", event, params) {

    object ScreenOpened : WalletScreenAnalyticsEvent("Screen opened")
    object WalletSwipe : WalletScreenAnalyticsEvent("Wallet Swipe")

    class EnableBiometrics(state: AnalyticsParam.OnOffState) : WalletScreenAnalyticsEvent(
        event = "Enable Biometric",
        params = mapOf("State" to state.value),
    )

    // TODO [REDACTED_JIRA]
    class NoticeRateAppButton(result: AnalyticsParam.RateApp) : WalletScreenAnalyticsEvent(
        event = "Notice - Rate The App Button Tapped",
        params = mapOf("Result" to result.value),
    )

    object NoticeBackupYourWalletTapped : WalletScreenAnalyticsEvent("Notice - Backup Your Wallet Tapped")
    object NoticeScanYourCardTapped : WalletScreenAnalyticsEvent("Notice - Scan Your Card Tapped")
    object NoticeWalletLocked : WalletScreenAnalyticsEvent("Notice - Wallet Locked")
}