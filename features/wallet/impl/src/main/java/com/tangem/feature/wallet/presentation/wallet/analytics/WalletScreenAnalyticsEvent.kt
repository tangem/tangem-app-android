package com.tangem.feature.wallet.presentation.wallet.analytics

import com.tangem.core.analytics.models.*
import com.tangem.domain.models.wallet.UserWalletId

sealed class WalletScreenAnalyticsEvent {

    sealed class Basic(
        event: String,
        params: Map<String, String> = mapOf(),
    ) : AnalyticsEvent(category = "Basic", event = event, params = params) {

        class WalletToppedUp(userWalletId: UserWalletId, walletType: AnalyticsParam.WalletType) :
            Basic(
                event = "Topped up",
                params = mapOf(AnalyticsParam.CURRENCY to walletType.value),
            ),
            OneTimeAnalyticsEvent, AppsFlyerIncludedEvent {

            override val oneTimeEventId: String = id + userWalletId.stringValue
        }

        class CardWasScanned(source: AnalyticsParam.ScreensSources) : Basic(
            event = "Card Was Scanned",
            params = mapOf(
                AnalyticsParam.SOURCE to source.value,
            ),
        )

        class BalanceLoaded(balance: AnalyticsParam.CardBalanceState, tokensCount: Int?) : Basic(
            event = "Balance Loaded",
            params = buildMap {
                put(AnalyticsParam.BALANCE, balance.value)
                tokensCount?.let { put(AnalyticsParam.TOKENS_COUNT, it.toString()) }
            },
        )

        class TokenBalance(balance: AnalyticsParam.EmptyFull, token: String) : Basic(
            event = "Token Balance",
            params = mapOf(
                AnalyticsParam.STATE to balance.value,
                AnalyticsParam.TOKEN_PARAM to token,
            ),
        )
    }

    sealed class MainScreen(
        event: String,
        params: Map<String, String> = mapOf(),
    ) : AnalyticsEvent(category = "Main Screen", event = event, params = params) {

        class ScreenOpenedLegacy : MainScreen(event = "Screen opened")

        data class ScreenOpened(
            private val hasMobileWallet: Boolean,
            private val accountsCount: Int?,
            val theme: String,
            val isImported: Boolean,
            val referralId: String?,
        ) : MainScreen(
            event = "Screen opened",
            params = buildMap {
                put("Mobile Wallet", if (hasMobileWallet) "Yes" else "No")
                if (accountsCount != null) put("Accounts Count", accountsCount.toString())
                put("App Theme", theme)
                val seedPhrase = if (isImported) {
                    "Seed Phrase"
                } else {
                    "Seedless"
                }
                put("Wallet Type", seedPhrase)
                putAll(getReferralParams(referralId))
            },
        ), AppsFlyerIncludedEvent

        data class NoticeFinishActivation(
            private val activationState: ActivationState,
            private val balanceState: AnalyticsParam.EmptyFull,
        ) : MainScreen(
            event = "Notice - Finish Activation",
            params = mapOf(
                "Activation State" to activationState.value,
                "Balance State" to balanceState.value,
            ),
        ) {
            enum class ActivationState(val value: String) {
                NotStarted("Not Started"),
                Unfinished("Unfinished"),
            }
        }

        class ButtonFinalizeActivation : MainScreen(
            event = "Button - Finalize Activation",
        )

        class WalletSelected(val isImported: Boolean) : MainScreen(
            event = "Wallet Selected",
            params = mapOf(
                "Wallet Type" to if (isImported) "Seed Phrase" else "Seedless",
            ),
        )

        class EnableBiometrics(state: AnalyticsParam.OnOffState) : MainScreen(
            event = "Enable Biometric",
            params = mapOf("State" to state.value),
        )

        class NoticeRateAppButton(result: AnalyticsParam.RateApp) : MainScreen(
            event = "Notice - Rate The App Button Tapped",
            params = mapOf("Result" to result.value),
        )

        class NoticeBackupYourWalletTapped : MainScreen(event = "Notice - Backup Your Wallet Tapped")
        class NoticeScanYourCardTapped : MainScreen(event = "Notice - Scan Your Card Tapped")
        class WalletUnlockTapped : MainScreen(event = "Notice - Wallet Unlock Tapped")

        class NetworksUnreachable(
            tokens: List<String>,
        ) : MainScreen(
            event = "Notice - Networks Unreachable",
            params = mapOf("Tokens" to tokens.joinToString()),
        )

        class MissingAddresses : MainScreen(event = "Notice - Missing Addresses")

        class CardSignedTransactions : MainScreen(event = "Notice - Card Signed Transactions")

        class HowDoYouLikeTangem : MainScreen(event = "Notice - How Do You Like Tangem")

        class ProductSampleCard : MainScreen(event = "Notice - Product Sample Card")

        class TestnetCard : MainScreen(event = "Notice - Testnet Card")

        class DemoCard : MainScreen(event = "Notice - Demo Card")

        class DevelopmentCard : MainScreen(event = "Notice - Development Card")

        class WalletUnlock : MainScreen(event = "Notice - Wallet Unlock")

        class BackupYourWallet : MainScreen(event = "Notice - Backup Your Wallet")

        class BackupError : MainScreen(event = "Notice - Backup Error")

        class NotePromo : MainScreen(event = "Notice - Note Promo")

        class NotePromoButton : MainScreen(event = "Note Promo Button")

        class UnlockAllWithBiometrics : MainScreen(event = "Button - Unlock All With Biometrics")

        class UnlockWithCardScan : MainScreen(event = "Button - Unlock With Card Scan")

        class EditWalletTapped : MainScreen(event = "Button - Edit Wallet Tapped")

        class DeleteWalletTapped : MainScreen(event = "Button - Delete Wallet Tapped")

        class NoticeSeedPhraseSupport : MainScreen(event = "Notice - Seed Phrase Support")

        class NoticeSeedPhraseSupportSecond : MainScreen(event = "Notice - Seed Phrase Support2")

        class NoticeSeedPhraseSupportButtonNo : MainScreen(event = "Button - Support No")

        class NoticeSeedPhraseSupportButtonYes : MainScreen(event = "Button - Support Yes")

        class NoticeSeedPhraseSupportButtonUsed : MainScreen(event = "Button - Support Used")

        class NoticeSeedPhraseSupportButtonDeclined : MainScreen(event = "Button - Support Declined")

        // region Referral Promo
        class ReferralPromo : MainScreen(event = "Referral Banner")
        class ReferralPromoButtonParticipate : MainScreen(event = "Button - Referral Participate")
        class ReferralPromoButtonDismiss : MainScreen(event = "Button - Referral Dismiss")
        //endregion
    }

    sealed class PushBannerPromo(
        event: String,
        params: Map<String, String> = mapOf(),
    ) : AnalyticsEvent(category = "Promo", event = event, params = params) {

        class PushBanner : PushBannerPromo(event = "Push Banner")
        class ButtonAllowPush : PushBannerPromo(event = "Button - Allow Push")
        class ButtonLaterPush : PushBannerPromo(event = "Button - Later Push")
    }
}