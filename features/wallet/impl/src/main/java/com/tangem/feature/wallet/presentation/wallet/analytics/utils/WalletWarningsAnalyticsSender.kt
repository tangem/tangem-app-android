package com.tangem.feature.wallet.presentation.wallet.analytics.utils

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.domain.tokens.model.analytics.PromoAnalyticsEvent.*
import com.tangem.feature.wallet.child.wallet.model.WalletActivationBannerType
import com.tangem.feature.wallet.presentation.wallet.analytics.WalletScreenAnalyticsEvent
import com.tangem.feature.wallet.presentation.wallet.analytics.WalletScreenAnalyticsEvent.MainScreen
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletNotification
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState
import com.tangem.feature.wallet.presentation.wallet.utils.ScreenLifecycleProvider
import javax.inject.Inject

@ModelScoped
internal class WalletWarningsAnalyticsSender @Inject constructor(
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val screenLifecycleProvider: ScreenLifecycleProvider,
) {

    fun send(displayedUiState: WalletState?, newWarnings: List<WalletNotification>) {
        if (screenLifecycleProvider.isBackgroundState.value) return
        if (newWarnings.isEmpty()) return
        if (displayedUiState == null || displayedUiState.pullToRefreshConfig.isRefreshing) return

        val eventsFromNewWarnings = getEvents(newWarnings)
        val eventsFromDisplayedWarnings = getEvents(displayedUiState.warnings)
        val eventsToSend = eventsFromNewWarnings.filter { it !in eventsFromDisplayedWarnings }

        eventsToSend.forEach { event ->
            analyticsEventHandler.send(event)
        }
    }

    private fun getEvents(warnings: List<WalletNotification>): Set<AnalyticsEvent> {
        return warnings.mapNotNullTo(mutableSetOf(), ::getEvent)
    }

    @Suppress("CyclomaticComplexMethod")
    private fun getEvent(warning: WalletNotification): AnalyticsEvent? {
        return when (warning) {
            is WalletNotification.Critical.DevCard -> MainScreen.DevelopmentCard()
            is WalletNotification.Critical.FailedCardValidation -> MainScreen.ProductSampleCard()
            is WalletNotification.Warning.MissingBackup -> MainScreen.BackupYourWallet()
            is WalletNotification.Warning.NumberOfSignedHashesIncorrect -> MainScreen.CardSignedTransactions()
            is WalletNotification.Warning.TestNetCard -> MainScreen.TestnetCard()
            is WalletNotification.Informational.DemoCard -> MainScreen.DemoCard()
            is WalletNotification.Informational.MissingAddresses -> MainScreen.MissingAddresses()
            is WalletNotification.RateApp -> MainScreen.HowDoYouLikeTangem()
            is WalletNotification.Critical.BackupError -> MainScreen.BackupError()
            is WalletNotification.NoteMigration -> MainScreen.NotePromo()
            is WalletNotification.SwapPromo -> NoticePromotionBanner(
                source = AnalyticsParam.ScreensSources.Main,
                program = Program.Empty, // Use it on new promo action
            )
            is WalletNotification.Sepa -> NoticePromotionBanner(
                source = AnalyticsParam.ScreensSources.Main,
                program = Program.Sepa,
            )
            is WalletNotification.BlackFridayPromo -> NoticePromotionBanner(
                source = AnalyticsParam.ScreensSources.Main,
                program = Program.BlackFriday,
            )
            is WalletNotification.OnePlusOnePromo -> NoticePromotionBanner(
                source = AnalyticsParam.ScreensSources.Main,
                program = Program.OnePlusOne,
            )
            is WalletNotification.YieldPromo -> NoticePromotionBanner(
                source = AnalyticsParam.ScreensSources.Main,
                program = Program.YieldPromo,
            )
            is WalletNotification.ReferralPromo -> MainScreen.ReferralPromo()
            is WalletNotification.VisaPresalePromo -> VisaWaitlistPromo()
            is WalletNotification.UnlockWallets -> null // See [SelectedWalletAnalyticsSender]
            is WalletNotification.Informational.NoAccount,
            is WalletNotification.Warning.LowSignatures,
            is WalletNotification.Warning.SomeNetworksUnreachable,
            is WalletNotification.Warning.NetworksUnreachable,
            is WalletNotification.UsedOutdatedData,
            is WalletNotification.UnlockVisaAccess,
            is WalletNotification.Warning.YeildSupplyApprove, // TODO apply correct event
            is WalletNotification.CloreMigration,
            -> null
            is WalletNotification.FinishWalletActivation -> {
                val activationState = if (warning.isBackupExists) {
                    MainScreen.NoticeFinishActivation.ActivationState.Unfinished
                } else {
                    MainScreen.NoticeFinishActivation.ActivationState.NotStarted
                }
                val balanceState = when (warning.type) {
                    WalletActivationBannerType.Attention -> AnalyticsParam.EmptyFull.Empty
                    WalletActivationBannerType.Warning -> AnalyticsParam.EmptyFull.Full
                }
                MainScreen.NoticeFinishActivation(
                    activationState = activationState,
                    balanceState = balanceState,
                )
            }
            is WalletNotification.Critical.SeedPhraseNotification -> MainScreen.NoticeSeedPhraseSupport()
            is WalletNotification.Critical.SeedPhraseSecondNotification -> MainScreen.NoticeSeedPhraseSupportSecond()
            is WalletNotification.PushNotifications -> WalletScreenAnalyticsEvent.PushBannerPromo.PushBanner()
            is WalletNotification.Warning.TangemPayRefreshNeeded -> null
            WalletNotification.Warning.TangemPayUnreachable -> null
        }
    }
}