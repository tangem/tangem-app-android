package com.tangem.feature.wallet.child.wallet.model.intents

import arrow.core.getOrElse
import com.tangem.common.routing.AppRoute.*
import com.tangem.common.routing.AppRouter
import com.tangem.common.ui.notifications.NotificationId
import com.tangem.common.ui.userwallet.handle
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic.ButtonSupport
import com.tangem.core.analytics.models.event.AssetsDiscoveryAnalyticsEvent
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.review.ReviewManager
import com.tangem.domain.assetsdiscovery.usecase.AcknowledgeAssetsDiscoveryCompletionUseCase
import com.tangem.domain.card.SetCardWasScannedUseCase
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.feedback.GetWalletMetaInfoUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.hotwallet.CloseHotWalletUpgradeBannerUseCase
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.requireColdWallet
import com.tangem.domain.networks.multi.MultiNetworkStatusFetcher
import com.tangem.domain.notifications.SetShouldShowNotificationUseCase
import com.tangem.domain.notifications.repository.NotificationsRepository
import com.tangem.domain.quotes.multi.MultiQuoteStatusFetcher
import com.tangem.domain.settings.NeverToSuggestRateAppUseCase
import com.tangem.domain.settings.RemindToRateAppLaterUseCase
import com.tangem.domain.staking.StakingIdFactory
import com.tangem.domain.staking.multi.MultiStakingBalanceFetcher
import com.tangem.domain.stories.models.StoryContentIds
import com.tangem.domain.tokens.model.details.NavigationAction
import com.tangem.domain.wallets.usecase.*
import com.tangem.domain.yield.supply.usecase.YieldSupplySetShouldShowMainPromoUseCase
import com.tangem.feature.wallet.presentation.wallet.analytics.WalletScreenAnalyticsEvent
import com.tangem.feature.wallet.presentation.wallet.analytics.WalletScreenAnalyticsEvent.MainScreen
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletEvent
import com.tangem.feature.wallet.presentation.wallet.state.utils.WalletEventSender
import com.tangem.features.pushnotifications.api.analytics.PushNotificationAnalyticEvents
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("TooManyFunctions")
internal interface WalletWarningsClickIntents {

    fun onAddBackupCardClick()

    fun onCloreMigrationClick(cryptoCurrencyStatus: CryptoCurrencyStatus)

    fun onCloseAlreadySignedHashesWarningClick()

    fun onGenerateMissedAddressesClick(userWalletId: UserWalletId, missedAddressCurrencies: List<CryptoCurrency>)

    fun onOpenUnlockWalletsBottomSheetClick()

    fun onLikeAppClick()

    fun onDislikeAppClick()

    fun onCloseRateAppWarningClick()

    fun onSupportClick()

    fun onBackupErrorClick()

    fun onNoteMigrationButtonClick(url: String)

    fun onAllowPermissions()

    fun onDenyPermissions()

    fun onFinishWalletActivationClick(isBackupExists: Boolean)

    fun onUpgradeHotWalletClick(userWalletId: UserWalletId)

    fun onCloseUpgradeBannerClick(userWalletId: UserWalletId)

    fun onDismissAssetsDiscoveryNotification(userWalletId: UserWalletId)

    fun onAssetsDiscoveryManageClick(userWalletId: UserWalletId)

    fun onYieldBoostBannerClick(userWalletId: UserWalletId)

    fun onDismissYieldBoostBanner(userWalletId: UserWalletId)
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
    private val getWalletMetaInfoUseCase: GetWalletMetaInfoUseCase,
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase,
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
    private val closeHotWalletUpgradeBannerUseCase: CloseHotWalletUpgradeBannerUseCase,
    private val acknowledgeAssetsDiscoveryCompletionUseCase: AcknowledgeAssetsDiscoveryCompletionUseCase,
    private val yieldSupplySetShouldShowMainPromoUseCase: YieldSupplySetShouldShowMainPromoUseCase,
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

    override fun onGenerateMissedAddressesClick(
        userWalletId: UserWalletId,
        missedAddressCurrencies: List<CryptoCurrency>,
    ) {
        analyticsEventHandler.send(MainScreen.NoticeScanYourCardTapped())

        modelScope.launch {
            derivePublicKeysUseCase(
                userWalletId = userWalletId,
                currencies = missedAddressCurrencies,
            ).fold(
                ifLeft = { TangemLogger.e("Failed to derive public keys", it) },
                ifRight = {
                    fetchCryptoCurrencies(userWalletId = userWalletId, currencies = missedAddressCurrencies)
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
                    nonBiometricUnlockWalletUseCase(selectedUserWalletId, AnalyticsParam.ScreensSources.Main)
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

            analyticsEventHandler.send(ButtonSupport(source = AnalyticsParam.ScreensSources.Main))
            sendFeedbackEmailUseCase(type = FeedbackEmailType.RateCanBeBetter(walletMetaInfo = cardInfo))
        }
    }

    override fun onCloseRateAppWarningClick() {
        analyticsEventHandler.send(MainScreen.NoticeRateAppButton(AnalyticsParam.RateApp.Closed))

        modelScope.launch(dispatchers.main) {
            remindToRateAppLaterUseCase()
        }
    }

    override fun onSupportClick() {
        val userWallet = getSelectedUserWallet() ?: return

        modelScope.launch {
            val metaInfo = getWalletMetaInfoUseCase(userWallet.walletId).getOrNull() ?: return@launch
            analyticsEventHandler.send(ButtonSupport(source = AnalyticsParam.ScreensSources.Main))
            sendFeedbackEmailUseCase(type = FeedbackEmailType.DirectUserRequest(walletMetaInfo = metaInfo))
        }
    }

    override fun onBackupErrorClick() {
        val userWallet = getSelectedUserWallet() ?: return

        modelScope.launch {
            val metaInfo = getWalletMetaInfoUseCase(userWallet.walletId).getOrNull() ?: return@launch
            analyticsEventHandler.send(ButtonSupport(source = AnalyticsParam.ScreensSources.Main))
            sendFeedbackEmailUseCase(type = FeedbackEmailType.BackupProblem(walletMetaInfo = metaInfo))
        }
    }

    override fun onNoteMigrationButtonClick(url: String) {
        analyticsEventHandler.send(MainScreen.NotePromoButton())
        modelScope.launch(dispatchers.main) {
            router.openUrl(url)
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
                        .onLeft { TangemLogger.e("Unable to fetch networks: $it") }
                },
                async {
                    multiQuoteStatusFetcher(
                        params = MultiQuoteStatusFetcher.Params(
                            currenciesIds = currencies.mapNotNull { it.id.rawCurrencyId }.toSet(),
                            appCurrencyId = null,
                        ),
                    )
                        .onLeft { TangemLogger.e("Unable to fetch quotes: $it") }
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
                        .onLeft { TangemLogger.e("Unable to fetch yield balances: $it") }
                },
            )
                .awaitAll()
        }
    }

    private fun getSelectedUserWallet(): UserWallet? {
        val userWalletId = stateHolder.getSelectedWalletId()
        return getUserWalletUseCase(userWalletId).getOrElse { error ->
            TangemLogger.e(
                """
                    Unable to get user wallet
                    |- ID: $userWalletId
                    |- Exception: $error
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
                TangemLogger.e("Error", it)
            }
        }
    }

    override fun onUpgradeHotWalletClick(userWalletId: UserWalletId) {
        modelScope.launch(dispatchers.main) {
            val userWallet = getUserWalletUseCase(userWalletId).getOrNull()
            if (userWallet is UserWallet.Hot) {
                appRouter.push(UpgradeWallet(userWalletId))
            }
        }
    }

    override fun onCloseUpgradeBannerClick(userWalletId: UserWalletId) {
        modelScope.launch(dispatchers.main) {
            val userWallet = getUserWalletUseCase(userWalletId).getOrNull()
            if (userWallet is UserWallet.Hot) {
                closeHotWalletUpgradeBannerUseCase(userWalletId)
            }
        }
    }

    override fun onDismissAssetsDiscoveryNotification(userWalletId: UserWalletId) {
        analyticsEventHandler.send(AssetsDiscoveryAnalyticsEvent.ButtonCloseBanner())
        acknowledgeAssetsDiscoveryCompletionUseCase(userWalletId)
    }

    override fun onAssetsDiscoveryManageClick(userWalletId: UserWalletId) {
        analyticsEventHandler.send(AssetsDiscoveryAnalyticsEvent.ButtonManageTokens())
        acknowledgeAssetsDiscoveryCompletionUseCase(userWalletId)
        router.openManageTokensScreen(
            AccountId.forMainCryptoPortfolio(userWalletId),
        )
    }

    override fun onYieldBoostBannerClick(userWalletId: UserWalletId) {
        appRouter.push(
            Stories(
                storyId = StoryContentIds.STORY_FIRST_TIME_YIELD_PROMO.id,
                nextScreen = null,
                screenSource = "YieldMainBanner",
                shouldMarkAsSeenOnClose = false,
            ),
        )
    }

    override fun onDismissYieldBoostBanner(userWalletId: UserWalletId) {
        modelScope.launch {
            yieldSupplySetShouldShowMainPromoUseCase(shouldShow = false)
        }
    }
}