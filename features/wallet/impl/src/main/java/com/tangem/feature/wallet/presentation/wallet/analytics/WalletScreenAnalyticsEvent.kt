package com.tangem.feature.wallet.presentation.wallet.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.OneTimeAnalyticsEvent
import com.tangem.domain.wallets.models.UserWalletId

sealed class WalletScreenAnalyticsEvent {

    sealed class Basic(
        event: String,
        params: Map<String, String> = mapOf(),
        error: Throwable? = null,
    ) : AnalyticsEvent(category = "Basic", event = event, params = params, error = error) {

        class WalletToppedUp(userWalletId: UserWalletId, walletType: AnalyticsParam.WalletType) :
            Basic(
                event = "Topped up",
                params = mapOf(AnalyticsParam.CURRENCY to walletType.value),
            ),
            OneTimeAnalyticsEvent {

            override val oneTimeEventId: String = id + userWalletId.stringValue
        }

        object WalletOpened : Basic(event = "Wallet Opened")

        class CardWasScanned(source: AnalyticsParam.ScannedFrom) : Basic(
            event = "Card Was Scanned",
            params = mapOf(
                AnalyticsParam.SOURCE to source.value,
            ),
        )

        class BalanceLoaded(balance: AnalyticsParam.CardBalanceState) : Basic(
            event = "Balance Loaded",
            params = mapOf(
                AnalyticsParam.BALANCE to balance.value,
            ),
        )
    }

    sealed class MainScreen(
        event: String,
        params: Map<String, String> = mapOf(),
    ) : AnalyticsEvent(category = "Main Screen", event = event, params = params) {

        object ScreenOpened : MainScreen(event = "Screen opened")
        object WalletSwipe : MainScreen(event = "Wallet Swipe")

        class EnableBiometrics(state: AnalyticsParam.OnOffState) : MainScreen(
            event = "Enable Biometric",
            params = mapOf("State" to state.value),
        )

        class NoticeRateAppButton(result: AnalyticsParam.RateApp) : MainScreen(
            event = "Notice - Rate The App Button Tapped",
            params = mapOf("Result" to result.value),
        )

        object NoticeBackupYourWalletTapped : MainScreen(event = "Notice - Backup Your Wallet Tapped")
        object NoticeScanYourCardTapped : MainScreen(event = "Notice - Scan Your Card Tapped")
        object NoticeWalletLocked : MainScreen(event = "Notice - Wallet Locked")
        object WalletUnlockTapped : MainScreen(event = "Button - Wallet Unlock Tapped")
    }
}