package com.tangem.feature.wallet.presentation.wallet.analytics.utils

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.domain.tokens.model.analytics.PromoAnalyticsEvent.*
import com.tangem.feature.wallet.child.wallet.model.WalletActivationBannerType
import com.tangem.feature.wallet.presentation.wallet.analytics.WalletScreenAnalyticsEvent.MainScreen
import com.tangem.feature.wallet.presentation.wallet.analytics.WalletScreenAnalyticsEvent.MainScreen.*
import com.tangem.feature.wallet.presentation.wallet.analytics.WalletScreenAnalyticsEvent.PushBannerPromo.PushBanner
import com.tangem.feature.wallet.presentation.wallet.state.model.*
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

    fun send(displayedWalletUM: WalletUM?, newNotifications: List<WalletNotificationUM>) {
        if (screenLifecycleProvider.isBackgroundState.value) return
        if (newNotifications.isEmpty()) return
        if (displayedWalletUM == null || displayedWalletUM.pullToRefreshConfig.isRefreshing) return

        val totalNotifications = displayedWalletUM.notifications + displayedWalletUM.notificationsCarousel
        val notificationsDiff = newNotifications.filter { it !in totalNotifications }

        val eventsToSend = getEvents2(notificationsDiff)

        eventsToSend.forEach { event ->
            analyticsEventHandler.send(event)
        }
    }

    private fun getEvents(warnings: List<WalletNotification>): Set<AnalyticsEvent> {
        return warnings.mapNotNullTo(mutableSetOf(), ::getEvent)
    }

    private fun getEvents2(notifications: List<WalletNotificationUM>): Set<AnalyticsEvent> {
        return notifications.mapNotNullTo(mutableSetOf(), ::getEvent2)
    }

    @Suppress("CyclomaticComplexMethod")
    private fun getEvent(warning: WalletNotification): AnalyticsEvent? {
        return when (warning) {
            is WalletNotification.Critical.DevCard -> DevelopmentCard()
            is WalletNotification.Critical.FailedCardValidation -> ProductSampleCard()
            is WalletNotification.Warning.MissingBackup -> BackupYourWallet()
            is WalletNotification.Warning.NumberOfSignedHashesIncorrect -> CardSignedTransactions()
            is WalletNotification.Warning.TestNetCard -> TestnetCard()
            is WalletNotification.Informational.DemoCard -> DemoCard()
            is WalletNotification.Informational.MissingAddresses -> MissingAddresses()
            is WalletNotification.RateApp -> HowDoYouLikeTangem()
            is WalletNotification.Critical.BackupError -> BackupError()
            is WalletNotification.NoteMigration -> NotePromo()
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
                NoticeFinishActivation(
                    activationState = activationState,
                    balanceState = balanceState,
                )
            }
            is WalletNotification.Critical.SeedPhraseNotification -> NoticeSeedPhraseSupport()
            is WalletNotification.Critical.SeedPhraseSecondNotification -> NoticeSeedPhraseSupportSecond()
            is WalletNotification.PushNotifications -> PushBanner()
            is WalletNotification.Warning.TangemPayRefreshNeeded -> null
            is WalletNotification.Warning.TangemPayUnreachable -> null
            is WalletNotification.UpgradeHotWalletPromo -> null
        }
    }

    @Suppress("CyclomaticComplexMethod")
    private fun getEvent2(notificationUM: WalletNotificationUM): AnalyticsEvent? {
        return when (notificationUM) {
            WalletNotificationUM.DevCard -> DevelopmentCard()
            WalletNotificationUM.FailedCardValidation -> ProductSampleCard()
            is WalletNotificationUM.MissingBackup -> BackupYourWallet()
            is WalletNotificationUM.NumberOfSignedHashesIncorrect -> CardSignedTransactions()
            WalletNotificationUM.TestnetCard -> TestnetCard()
            WalletNotificationUM.DemoCard -> DemoCard()
            is WalletNotificationUM.MissingAddresses -> MissingAddresses()
            is WalletNotificationUM.RateApp -> HowDoYouLikeTangem()
            is WalletNotificationUM.BackupError -> BackupError()
            is WalletNotificationUM.NoteMigration -> NotePromo()
            is WalletNotificationUM.OnePlusOnePromo -> NoticePromotionBanner(
                source = AnalyticsParam.ScreensSources.Main,
                program = Program.OnePlusOne,
            )
            is WalletNotificationUM.YieldPromo -> NoticePromotionBanner(
                source = AnalyticsParam.ScreensSources.Main,
                program = Program.YieldPromo,
            )
            is WalletNotificationUM.FinishWalletActivation -> {
                val activationState = if (notificationUM.isBackupExists) {
                    NoticeFinishActivation.ActivationState.Unfinished
                } else {
                    NoticeFinishActivation.ActivationState.NotStarted
                }
                val balanceState = when (notificationUM.type) {
                    WalletNotificationType.Warning -> AnalyticsParam.EmptyFull.Full
                    else -> AnalyticsParam.EmptyFull.Empty
                }
                NoticeFinishActivation(
                    activationState = activationState,
                    balanceState = balanceState,
                )
            }
            is WalletNotificationUM.SeedPhraseNotification -> NoticeSeedPhraseSupport()
            is WalletNotificationUM.SeedPhraseSecondNotification -> NoticeSeedPhraseSupportSecond()
            is WalletNotificationUM.PushNotifications -> PushBanner()
            is WalletNotificationUM.UnlockWallets,
            is WalletNotificationUM.NoAccount,
            is WalletNotificationUM.LowSignatures,
            WalletNotificationUM.SomeNetworksUnreachable,
            is WalletNotificationUM.UsedOutdatedData,
            is WalletNotificationUM.CloreMigration,
            -> null
        }
    }
}