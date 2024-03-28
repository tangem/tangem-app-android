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

        class CardWasScanned(source: AnalyticsParam.ScreensSources) : Basic(
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

        class TokenBalance(balance: AnalyticsParam.TokenBalanceState, token: String) : Basic(
            event = "Token Balance",
            params = mapOf(
                AnalyticsParam.STATE to balance.value,
                AnalyticsParam.TOKEN to token,
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
        object WalletUnlockTapped : MainScreen(event = "Notice - Wallet Unlock Tapped")

        object NetworksUnreachable : MainScreen(event = "Notice - Networks Unreachable")

        object MissingAddresses : MainScreen(event = "Notice - Missing Addresses")

        object CardSignedTransactions : MainScreen(event = "Notice - Card Signed Transactions")

        object HowDoYouLikeTangem : MainScreen(event = "Notice - How Do You Like Tangem")

        object ProductSampleCard : MainScreen(event = "Notice - Product Sample Card")

        object TestnetCard : MainScreen(event = "Notice - Testnet Card")

        object DemoCard : MainScreen(event = "Notice - Demo Card")

        object DevelopmentCard : MainScreen(event = "Notice - Development Card")

        object WalletUnlock : MainScreen(event = "Notice - Wallet Unlock")

        object BackupYourWallet : MainScreen(event = "Notice - Backup Your Wallet")

        object UnlockAllWithBiometrics : MainScreen(event = "Button - Unlock All With Biometrics")

        object UnlockWithCardScan : MainScreen(event = "Button - Unlock With Card Scan")

        object EditWalletTapped : MainScreen(event = "Button - Edit Wallet Tapped")

        object DeleteWalletTapped : MainScreen(event = "Button - Delete Wallet Tapped")
    }
}
