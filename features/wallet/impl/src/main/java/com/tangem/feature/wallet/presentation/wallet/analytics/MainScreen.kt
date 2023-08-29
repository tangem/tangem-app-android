package com.tangem.feature.wallet.presentation.wallet.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam

sealed class MainScreen(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent("Main Screen", event, params) {

    object ScreenOpened : MainScreen("Screen opened")
    object WalletSwipe : MainScreen("Wallet Swipe")

    // TODO
    class EnableBiometrics(state: AnalyticsParam.OnOffState) : MainScreen(
        event = "Enable Biometric",
        params = mapOf("State" to state.value),
    )

    // TODO
    class NoticeRateAppButton(result: AnalyticsParam.RateApp) : MainScreen(
        event = "Notice - Rate The App Button Tapped",
        params = mapOf("Result" to result.value),
    )

    object NoticeBackupYourWalletTapped : MainScreen("Notice - Backup Your Wallet Tapped")
    object NoticeScanYourCardTapped : MainScreen("Notice - Scan Your Card Tapped")
    object NoticeWalletLocked : MainScreen("Notice - Wallet Locked")

}
