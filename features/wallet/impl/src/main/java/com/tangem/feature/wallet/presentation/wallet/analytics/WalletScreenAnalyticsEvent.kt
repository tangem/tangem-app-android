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

        data object WalletOpened : Basic(event = "Wallet Opened")

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

    sealed class Token(
        event: String,
        params: Map<String, String> = mapOf(),
    ) : AnalyticsEvent(category = "Token", event = event, params = params) {

        class PolkadotAccountReset(hasReset: Boolean) : Token(
            event = "Polkadot Account Reset",
            params = mapOf(
                AnalyticsParam.STATE to if (hasReset) "Yes" else "No",
            ),
        )

        class PolkadotImmortalTransactions(hasImmortalTransaction: Boolean) : Token(
            event = "Polkadot Immortal Transactions",
            params = mapOf(
                AnalyticsParam.STATE to if (hasImmortalTransaction) "Yes" else "No",
            ),
        )
    }

    sealed class MainScreen(
        event: String,
        params: Map<String, String> = mapOf(),
    ) : AnalyticsEvent(category = "Main Screen", event = event, params = params) {

        data object ScreenOpened : MainScreen(event = "Screen opened")
        data object WalletSwipe : MainScreen(event = "Wallet Swipe")

        class EnableBiometrics(state: AnalyticsParam.OnOffState) : MainScreen(
            event = "Enable Biometric",
            params = mapOf("State" to state.value),
        )

        class NoticeRateAppButton(result: AnalyticsParam.RateApp) : MainScreen(
            event = "Notice - Rate The App Button Tapped",
            params = mapOf("Result" to result.value),
        )

        data object NoticeBackupYourWalletTapped : MainScreen(event = "Notice - Backup Your Wallet Tapped")
        data object NoticeScanYourCardTapped : MainScreen(event = "Notice - Scan Your Card Tapped")
        data object NoticeWalletLocked : MainScreen(event = "Notice - Wallet Locked")
        data object WalletUnlockTapped : MainScreen(event = "Notice - Wallet Unlock Tapped")

        data object NetworksUnreachable : MainScreen(event = "Notice - Networks Unreachable")

        data object MissingAddresses : MainScreen(event = "Notice - Missing Addresses")

        data object CardSignedTransactions : MainScreen(event = "Notice - Card Signed Transactions")

        data object HowDoYouLikeTangem : MainScreen(event = "Notice - How Do You Like Tangem")

        data object ProductSampleCard : MainScreen(event = "Notice - Product Sample Card")

        data object TestnetCard : MainScreen(event = "Notice - Testnet Card")

        data object DemoCard : MainScreen(event = "Notice - Demo Card")

        data object DevelopmentCard : MainScreen(event = "Notice - Development Card")

        data object WalletUnlock : MainScreen(event = "Notice - Wallet Unlock")

        data object BackupYourWallet : MainScreen(event = "Notice - Backup Your Wallet")

        data object BackupError : MainScreen(event = "Notice - Backup Error")

        data object UnlockAllWithBiometrics : MainScreen(event = "Button - Unlock All With Biometrics")

        data object UnlockWithCardScan : MainScreen(event = "Button - Unlock With Card Scan")

        data object EditWalletTapped : MainScreen(event = "Button - Edit Wallet Tapped")

        data object DeleteWalletTapped : MainScreen(event = "Button - Delete Wallet Tapped")
    }

    sealed class Promotion(
        event: String,
        params: Map<String, String> = mapOf(),
    ) : AnalyticsEvent(category = "Promotion", event = event, params = params) {
        class NoticePromotionBanner(
            source: AnalyticsParam.ScreensSources,
            programName: String,
        ) : Promotion(
            event = "Notice - Promotion Banner",
            params = mapOf(
                AnalyticsParam.SOURCE to source.value,
                "Program Name" to programName,
            ),
        )

        class PromotionBannerClicked(
            source: AnalyticsParam.ScreensSources,
            programName: String,
            action: BannerAction,
        ) : Promotion(
            event = "Promo Banner Clicked",
            params = mapOf(
                AnalyticsParam.SOURCE to source.value,
                "Program Name" to programName,
                "Action" to action.action,
            ),
        ) {
            sealed class BannerAction(val action: String) {
                data object Clicked : BannerAction(action = "Clicked")
                data object Closed : BannerAction(action = "Closed")
            }
        }
    }
}
