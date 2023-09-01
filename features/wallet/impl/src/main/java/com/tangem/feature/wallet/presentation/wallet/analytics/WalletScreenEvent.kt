package com.tangem.feature.wallet.presentation.wallet.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam

sealed class WalletScreenEvent(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent("Main Screen", event, params) {

    object ScreenOpened : WalletScreenEvent("Screen opened")
    object WalletSwipe : WalletScreenEvent("Wallet Swipe")

    // TODO
    class EnableBiometrics(state: AnalyticsParam.OnOffState) : WalletScreenEvent(
        event = "Enable Biometric",
        params = mapOf("State" to state.value),
    )

    // TODO
    class NoticeRateAppButton(result: AnalyticsParam.RateApp) : WalletScreenEvent(
        event = "Notice - Rate The App Button Tapped",
        params = mapOf("Result" to result.value),
    )

    object NoticeBackupYourWalletTapped : WalletScreenEvent("Notice - Backup Your Wallet Tapped")
    object NoticeScanYourCardTapped : WalletScreenEvent("Notice - Scan Your Card Tapped")
    object NoticeWalletLocked : WalletScreenEvent("Notice - Wallet Locked")
}
