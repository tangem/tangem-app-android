package com.tangem.feature.wallet.child.wallet.model.intents

import arrow.core.getOrElse
import com.tangem.common.TangemBlogUrlBuilder
import com.tangem.common.routing.AppRoute.*
import com.tangem.common.routing.AppRouter
import com.tangem.common.ui.notifications.NotificationId
import com.tangem.common.ui.userwallet.handle
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.navigation.review.ReviewManager
import com.tangem.domain.card.SetCardWasScannedUseCase
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.feedback.GetWalletMetaInfoUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.requireColdWallet
import com.tangem.domain.networks.multi.MultiNetworkStatusFetcher
import com.tangem.domain.notifications.SetShouldShowNotificationUseCase
import com.tangem.domain.notifications.repository.NotificationsRepository
import com.tangem.domain.onramp.model.OnrampSource
import com.tangem.domain.promo.ShouldShowPromoWalletUseCase
import com.tangem.domain.promo.models.PromoId
import com.tangem.domain.quotes.multi.MultiQuoteStatusFetcher
import com.tangem.domain.settings.NeverToSuggestRateAppUseCase
import com.tangem.domain.settings.RemindToRateAppLaterUseCase
import com.tangem.domain.staking.StakingIdFactory
import com.tangem.domain.staking.multi.MultiStakingBalanceFetcher
import com.tangem.domain.tokens.model.analytics.PromoAnalyticsEvent
import com.tangem.domain.tokens.model.analytics.PromoAnalyticsEvent.Program
import com.tangem.domain.tokens.model.analytics.PromoAnalyticsEvent.PromotionBannerClicked
import com.tangem.domain.tokens.model.details.NavigationAction
import com.tangem.domain.wallets.usecase.*
import com.tangem.feature.wallet.presentation.wallet.analytics.WalletScreenAnalyticsEvent
import com.tangem.feature.wallet.presentation.wallet.analytics.WalletScreenAnalyticsEvent.Basic
import com.tangem.feature.wallet.presentation.wallet.analytics.WalletScreenAnalyticsEvent.MainScreen
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletAlertUM
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletEvent
import com.tangem.feature.wallet.presentation.wallet.state.utils.WalletEventSender
import com.tangem.features.pushnotifications.api.analytics.PushNotificationAnalyticEvents
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@Suppress("TooManyFunctions")
internal interface WalletWarningsClickIntents {

    fun onAddBackupCardClick()

    fun onCloreMigrationClick(cryptoCurrencyStatus: CryptoCurrencyStatus)

    fun onCloseAlreadySignedHashesWarningClick()

    fun onGenerateMissedAddressesClick(missedAddressCurrencies: List<CryptoCurrency>)

    fun onOpenUnlockWalletsBottomSheetClick()

    fun onLikeAppClick()

    fun onDislikeAppClick()

    fun onCloseRateAppWarningClick()

    fun onClosePromoClick(promoId: PromoId)

    fun onPromoClick(promoId: PromoId, cryptoCurrency: CryptoCurrency? = null)

    fun onSupportClick()

    fun onNoteMigrationButtonClick(url: String)

    fun onSeedPhraseNotificationConfirm()

    fun onSeedPhraseNotificationDecline()

    fun onSeedPhraseSecondNotificationAccept()

    fun onSeedPhraseSecondNotificationReject()

    fun onAllowPermissions()

    fun onDenyPermissions()

    fun onFinishWalletActivationClick(isBackupExists: Boolean)

    fun onYieldPromoTermsAndConditionsClick()
}

@Suppress("LargeClass", "LongParameterList", "TooManyFunctions")
@ModelScoped
internal class WalletWarningsClickIntentsImplementor @Inject constructor(
    private val stateHolder: WalletStateController,
    private val walletEventSender: WalletEventSender,
    private val derivePublicKeysUseCase: DerivePublicKeysUseCase,
    private val setCardWasScannedUseCase: SetCardWasScannedUseCase,
    private val neverToSuggestRateAppUseCase: NeverToSuggestRateAppUseCase,
    private val remindToRateAppLaterUseCase: RemindToRateAppLaterUseCase,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val nonBiometricUnlockWalletUseCase: NonBiometricUnlockWalletUseCase,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val dispatchers: CoroutineDispatcherProvider,
    private val shouldShowPromoWalletUseCase: ShouldShowPromoWalletUseCase,
    private val getWalletMetaInfoUseCase: GetWalletMetaInfoUseCase,
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase,
    private val seedPhraseNotificationUseCase: SeedPhraseNotificationUseCase,
    private val urlOpener: UrlOpener,
    private val multiNetworkStatusFetcher: MultiNetworkStatusFetcher,
    private val multiQuoteStatusFetcher: MultiQuoteStatusFetcher,
    private val multiStakingBalanceFetcher: MultiStakingBalanceFetcher,
    private val stakingIdFactory: StakingIdFactory,
    private val appRouter: AppRouter,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val setShouldShowNotificationUseCase: SetShouldShowNotificationUseCase,
    private val notificationsRepository: NotificationsRepository,
    private val setNotificationsEnabledUseCase: SetNotificationsEnabledUseCase,
    private val getWalletsListForEnablingUseCase: GetWalletsForAutomaticallyPushEnablingUseCase,
    private val uiMessageSender: UiMessageSender,
    private val reviewManager: ReviewManager,
) : BaseWalletClickIntents(), WalletWarningsClickIntents {

    override fun onAddBackupCardClick() {
        analyticsEventHandler.send(MainScreen.NoticeBackupYourWalletTapped())

        prepareAndStartOnboardingProcess()
    }

    override fun onCloreMigrationClick(cryptoCurrencyStatus: CryptoCurrencyStatus) {
        val userWallet = getSelectedUserWallet() ?: return

        router.openTokenDetails(
            userWalletId = userWallet.walletId,
            currencyStatus = cryptoCurrencyStatus,
            navigationAction = NavigationAction.CloreMigration,
        )
    }

    private fun prepareAndStartOnboardingProcess() {
        modelScope.launch(dispatchers.main) {
            getSelectedUserWallet()?.requireColdWallet()?.let {
                router.openOnboardingScreen(
                    scanResponse = it.scanResponse,
                    continueBackup = true,
                )
            }
        }
    }

    override fun onCloseAlreadySignedHashesWarningClick() {
        modelScope.launch(dispatchers.main) {
            val userWallet = getSelectedUserWallet() ?: return@launch

            if (userWallet is UserWallet.Cold) {
                setCardWasScannedUseCase(cardId = userWallet.cardId)
            }
        }
    }

    override fun onGenerateMissedAddressesClick(missedAddressCurrencies: List<CryptoCurrency>) {
        analyticsEventHandler.send(Basic.CardWasScanned(AnalyticsParam.ScreensSources.Main))
        analyticsEventHandler.send(MainScreen.NoticeScanYourCardTapped())

        modelScope.launch {
            val userWallet = getSelectedUserWallet() ?: return@launch

            derivePublicKeysUseCase(
                userWalletId = userWallet.walletId,
                currencies = missedAddressCurrencies,
            ).fold(
                ifLeft = { Timber.e(it, "Failed to derive public keys") },
                ifRight = {
                    fetchCryptoCurrencies(userWalletId = userWallet.walletId, currencies = missedAddressCurrencies)
                },
            )
        }
    }

    override fun onOpenUnlockWalletsBottomSheetClick() {
        analyticsEventHandler.send(MainScreen.WalletUnlockTapped())

        modelScope.launch {
            userWalletsListRepository.unlockAllWallets()
                .onLeft {
                    val selectedUserWalletId = stateHolder.getSelectedWalletId()
                    nonBiometricUnlockWalletUseCase(selectedUserWalletId)
                        .onLeft { error ->
                            error.handle(
                                onAlreadyUnlocked = {},
                                onUserCancelled = {},
                                analyticsEventHandler = analyticsEventHandler,
                                isFromUnlockAll = true,
                                showMessage = uiMessageSender::send,
                            )
                        }
                }
        }
    }

    override fun onLikeAppClick() {
        analyticsEventHandler.send(MainScreen.NoticeRateAppButton(AnalyticsParam.RateApp.Liked))

        reviewManager.request {
            modelScope.launch(dispatchers.main) {
                neverToSuggestRateAppUseCase()
            }
        }
    }

    override fun onDislikeAppClick() {
        analyticsEventHandler.send(MainScreen.NoticeRateAppButton(AnalyticsParam.RateApp.Disliked))

        modelScope.launch(dispatchers.main) {
            neverToSuggestRateAppUseCase()

            val userWallet = getSelectedUserWallet() ?: return@launch
            val cardInfo = getWalletMetaInfoUseCase(userWallet.walletId).getOrNull() ?: return@launch

            sendFeedbackEmailUseCase(type = FeedbackEmailType.RateCanBeBetter(walletMetaInfo = cardInfo))
        }
    }

    override fun onCloseRateAppWarningClick() {
        analyticsEventHandler.send(MainScreen.NoticeRateAppButton(AnalyticsParam.RateApp.Closed))

        modelScope.launch(dispatchers.main) {
            remindToRateAppLaterUseCase()
        }
    }

    override fun onClosePromoClick(promoId: PromoId) {
        analyticsEventHandler.send(
            when (promoId) {
                PromoId.Referral -> MainScreen.ReferralPromoButtonDismiss()
                PromoId.Sepa -> PromotionBannerClicked(
                    source = AnalyticsParam.ScreensSources.Main,
                    program = Program.Sepa,
                    action = PromotionBannerClicked.BannerAction.Closed(),
                )
                PromoId.VisaPresale -> PromoAnalyticsEvent.VisaWaitlistPromoDismiss()
                PromoId.BlackFriday -> PromotionBannerClicked(
                    source = AnalyticsParam.ScreensSources.Main,
                    program = Program.BlackFriday,
                    action = PromotionBannerClicked.BannerAction.Closed(),
                )
                PromoId.OnePlusOne -> PromotionBannerClicked(
                    source = AnalyticsParam.ScreensSources.Main,
                    program = Program.OnePlusOne,
                    action = PromotionBannerClicked.BannerAction.Closed(),
                )
                PromoId.YieldPromo -> PromotionBannerClicked(
                    source = AnalyticsParam.ScreensSources.Main,
                    program = Program.YieldPromo,
                    action = PromotionBannerClicked.BannerAction.Closed(),
                )
            },
        )
        modelScope.launch(dispatchers.main) {
            shouldShowPromoWalletUseCase.neverToShow(promoId)
        }
    }

    override fun onPromoClick(promoId: PromoId, cryptoCurrency: CryptoCurrency?) {
        val userWallet = getSelectedUserWallet() ?: return
        when (promoId) {
            PromoId.Referral -> {
                analyticsEventHandler.send(MainScreen.ReferralPromoButtonParticipate())
                appRouter.push(ReferralProgram(userWalletId = userWallet.walletId))
            }
            PromoId.Sepa -> {
                analyticsEventHandler.send(
                    PromotionBannerClicked(
                        source = AnalyticsParam.ScreensSources.Main,
                        program = Program.Sepa,
                        action = PromotionBannerClicked.BannerAction.Clicked(),
                    ),
                )
                cryptoCurrency ?: return
                appRouter.push(
                    Onramp(
                        userWalletId = userWallet.walletId,
                        currency = cryptoCurrency,
                        source = OnrampSource.SEPA_BANNER,
                        shouldLaunchSepa = true,
                    ),
                )
            }
            PromoId.VisaPresale -> {
                analyticsEventHandler.send(PromoAnalyticsEvent.VisaWaitlistPromoJoin())
                urlOpener.openUrl(VISA_PROMO_LINK)
            }
            PromoId.BlackFriday -> {
                analyticsEventHandler.send(
                    PromotionBannerClicked(
                        source = AnalyticsParam.ScreensSources.Main,
                        program = Program.BlackFriday,
                        action = PromotionBannerClicked.BannerAction.Clicked(),
                    ),
                )
                urlOpener.openUrl(BLACK_FRIDAY_PROMO_LINK)
            }
            PromoId.OnePlusOne -> {
                analyticsEventHandler.send(
                    PromotionBannerClicked(
                        source = AnalyticsParam.ScreensSources.Main,
                        program = Program.OnePlusOne,
                        action = PromotionBannerClicked.BannerAction.Clicked(),
                    ),
                )
                urlOpener.openUrl(ONE_PLUS_ONE_PROMO_LINK)
            }
            PromoId.YieldPromo -> Unit // banner is not clickable, only terms and conditions button
        }
    }

    override fun onSupportClick() {
        val userWallet = getSelectedUserWallet() ?: return

        modelScope.launch {
            val metaInfo = getWalletMetaInfoUseCase(userWallet.walletId).getOrNull() ?: return@launch
            sendFeedbackEmailUseCase(type = FeedbackEmailType.DirectUserRequest(walletMetaInfo = metaInfo))
        }
    }

    override fun onNoteMigrationButtonClick(url: String) {
        analyticsEventHandler.send(MainScreen.NotePromoButton())
        modelScope.launch(dispatchers.main) {
            router.openUrl(url)
        }
    }

    override fun onSeedPhraseNotificationConfirm() {
        val userWallet = getSelectedUserWallet() ?: return

        analyticsEventHandler.send(MainScreen.NoticeSeedPhraseSupportButtonYes())

        uiMessageSender.send(
            WalletAlertUM.seedPhraseConfirm {
                modelScope.launch {
                    seedPhraseNotificationUseCase.confirm(userWalletId = userWallet.walletId)

                    urlOpener.openUrl(
                        url = TangemBlogUrlBuilder.build(post = TangemBlogUrlBuilder.Post.SeedNotify),
                    )
                }
            },
        )
    }

    override fun onSeedPhraseNotificationDecline() {
        val userWallet = getSelectedUserWallet() ?: return

        analyticsEventHandler.send(MainScreen.NoticeSeedPhraseSupportButtonNo())

        uiMessageSender.send(
            WalletAlertUM.seedPhraseDismiss {
                modelScope.launch {
                    seedPhraseNotificationUseCase.decline(userWalletId = userWallet.walletId)
                }
            },
        )
    }

    override fun onSeedPhraseSecondNotificationAccept() {
        val userWallet = getSelectedUserWallet() ?: return

        analyticsEventHandler.send(MainScreen.NoticeSeedPhraseSupportButtonUsed())

        uiMessageSender.send(
            WalletAlertUM.seedPhraseConfirm {
                modelScope.launch {
                    seedPhraseNotificationUseCase.acceptSecond(userWalletId = userWallet.walletId)

                    urlOpener.openUrl(
                        url = TangemBlogUrlBuilder.build(post = TangemBlogUrlBuilder.Post.SeedNotifySecond),
                    )
                }
            },
        )
    }

    override fun onSeedPhraseSecondNotificationReject() {
        val userWallet = getSelectedUserWallet() ?: return

        analyticsEventHandler.send(MainScreen.NoticeSeedPhraseSupportButtonDeclined())

        modelScope.launch {
            seedPhraseNotificationUseCase.rejectSecond(userWalletId = userWallet.walletId)
        }
    }

    override fun onFinishWalletActivationClick(isBackupExists: Boolean) {
        analyticsEventHandler.send(MainScreen.ButtonFinalizeActivation())
        val userWalletId = stateHolder.getSelectedWalletId()
        val route = if (isBackupExists) {
            WalletActivation(userWalletId = userWalletId, isBackupExists = true)
        } else {
            WalletBackup(
                userWalletId = userWalletId,
                isColdWalletOptionShown = true,
            )
        }

        appRouter.push(route)
    }

    override fun onAllowPermissions() {
        analyticsEventHandler.send(WalletScreenAnalyticsEvent.PushBannerPromo.ButtonAllowPush())
        walletEventSender.send(
            event = WalletEvent.RequestPushPermissions(
                onAllow = {
                    modelScope.launch {
                        updateSubscribeOnPushPermissions(true)
                        analyticsEventHandler.send(
                            PushNotificationAnalyticEvents.PermissionStatus(isAllowed = true),
                        )
                        enableNotificationsIfNeeded()
                    }
                },
                onDeny = {
                    modelScope.launch {
                        updateSubscribeOnPushPermissions(false)
                        analyticsEventHandler.send(
                            PushNotificationAnalyticEvents.PermissionStatus(isAllowed = false),
                        )
                    }
                },
            ),
        )
    }

    override fun onDenyPermissions() {
        modelScope.launch {
            analyticsEventHandler.send(WalletScreenAnalyticsEvent.PushBannerPromo.ButtonLaterPush())
            updateSubscribeOnPushPermissions(false)
        }
    }

    private suspend fun updateSubscribeOnPushPermissions(shouldAllow: Boolean) {
        notificationsRepository.setUserAllowToSubscribeOnPushNotifications(shouldAllow)
        setShouldShowNotificationUseCase(
            key = NotificationId.EnablePushesReminderNotification.key,
            value = false,
        )
    }

    private suspend fun fetchCryptoCurrencies(userWalletId: UserWalletId, currencies: List<CryptoCurrency>) {
        coroutineScope {
            listOf(
                async {
                    multiNetworkStatusFetcher(
                        params = MultiNetworkStatusFetcher.Params(
                            userWalletId = userWalletId,
                            networks = currencies.map(CryptoCurrency::network).toSet(),
                        ),
                    )
                        .onLeft { Timber.e("Unable to fetch networks: $it") }
                },
                async {
                    multiQuoteStatusFetcher(
                        params = MultiQuoteStatusFetcher.Params(
                            currenciesIds = currencies.mapNotNull { it.id.rawCurrencyId }.toSet(),
                            appCurrencyId = null,
                        ),
                    )
                        .onLeft { Timber.e("Unable to fetch quotes: $it") }
                },
                async {
                    val stakingIds = currencies.mapNotNullTo(hashSetOf()) {
                        stakingIdFactory.create(userWalletId = userWalletId, cryptoCurrency = it).getOrNull()
                    }

                    multiStakingBalanceFetcher(
                        params = MultiStakingBalanceFetcher.Params(
                            userWalletId = userWalletId,
                            stakingIds = stakingIds,
                        ),
                    )
                        .onLeft { Timber.e("Unable to fetch yield balances: $it") }
                },
            )
                .awaitAll()
        }
    }

    private fun getSelectedUserWallet(): UserWallet? {
        val userWalletId = stateHolder.getSelectedWalletId()
        return getUserWalletUseCase(userWalletId).getOrElse {
            Timber.e(
                """
                    Unable to get user wallet
                    |- ID: $userWalletId
                    |- Exception: $it
                """.trimIndent(),
            )

            null
        }
    }

    private suspend fun enableNotificationsIfNeeded() {
        val alreadyEnabledWallets = notificationsRepository.getWalletAutomaticallyEnabledList().map {
            UserWalletId(it)
        }
        val walletsListWhichShouldBeEnabled = getWalletsListForEnablingUseCase(alreadyEnabledWallets)
        walletsListWhichShouldBeEnabled.forEach { userWalletId ->
            setNotificationsEnabledUseCase(userWalletId, true).onRight {
                notificationsRepository.setNotificationsWasEnabledAutomatically(userWalletId.stringValue)
            }.onLeft {
                Timber.e(it)
            }
        }
    }

    override fun onYieldPromoTermsAndConditionsClick() {
        analyticsEventHandler.send(
            PromotionBannerClicked(
                source = AnalyticsParam.ScreensSources.Main,
                program = Program.YieldPromo,
                action = PromotionBannerClicked.BannerAction.Clicked(),
            ),
        )
        urlOpener.openUrl(YIELD_PROMO_TERMS_LINK)
    }

    private companion object {
        const val VISA_PROMO_LINK = "https://tangem.com/en/cardwaitlist/?utm_source=tangem-app-banner" +
            "&utm_medium=banner" +
            "&utm_campaign=tangempaywaitlist"
        const val BLACK_FRIDAY_PROMO_LINK = "https://tangem.com/en/pricing/" +
            "?promocode=BF2025" +
            "&utm_source=tangem-app-banner" +
            "&utm_medium=banner" +
            "&utm_campaign=BlackFriday2025"
        const val ONE_PLUS_ONE_PROMO_LINK = "https://tangem.com/pricing/" +
            "?cat=family" +
            "&utm_source=tangem-app-banner" +
            "&utm_medium=banner" +
            "&utm_campaign=BOGO50"
        const val YIELD_PROMO_TERMS_LINK = "https://tangem.com/docs/yield-mode-toc.html"
    }
}