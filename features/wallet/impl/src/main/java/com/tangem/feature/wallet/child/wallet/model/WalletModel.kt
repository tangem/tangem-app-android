package com.tangem.feature.wallet.child.wallet.model

import androidx.compose.runtime.Stable
import arrow.core.getOrElse
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.ui.utils.parseBigDecimal
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.event.MainScreenAnalyticsEvent
import com.tangem.core.analytics.utils.TrackingContextProxy
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.datasource.local.appsflyer.AppsFlyerStore
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.account.usecase.IsAccountsModeEnabledUseCase
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.apptheme.GetAppThemeModeUseCase
import com.tangem.domain.apptheme.model.AppThemeMode
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.*
import com.tangem.domain.notifications.GetIsHuaweiDeviceWithoutGoogleServicesUseCase
import com.tangem.domain.notifications.repository.NotificationsRepository
import com.tangem.domain.qrscanning.models.QrResultSource
import com.tangem.domain.qrscanning.models.SourceType
import com.tangem.domain.qrscanning.usecases.ListenToQrScanningUseCase
import com.tangem.domain.qrscanning.models.ClassifiedQrContent
import com.tangem.domain.qrscanning.models.QrSendTarget
import com.tangem.domain.walletconnect.WcPairService
import com.tangem.domain.walletconnect.model.WcPairRequest
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.pay.usecase.TangemPayMainScreenCustomerInfoUseCase
import com.tangem.domain.qrscanning.usecases.ResolveQrSendTargetsUseCase
import com.tangem.domain.settings.*
import com.tangem.domain.tokens.RefreshMultiCurrencyWalletQuotesUseCase
import com.tangem.domain.wallets.usecase.*
import com.tangem.domain.wallets.usecase.GetWalletIconUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyApyUpdateUseCase
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.presentation.router.InnerWalletRouter
import com.tangem.feature.wallet.presentation.wallet.analytics.WalletScreenAnalyticsEvent
import com.tangem.feature.wallet.presentation.wallet.analytics.utils.SelectedWalletAnalyticsSender
import com.tangem.feature.wallet.presentation.wallet.domain.OnrampStatusFactory
import com.tangem.feature.wallet.presentation.wallet.domain.WalletContentFetcher
import com.tangem.feature.wallet.presentation.wallet.domain.WalletImageResolver
import com.tangem.feature.wallet.presentation.wallet.domain.WalletNameMigrationUseCase
import com.tangem.feature.wallet.presentation.wallet.loaders.WalletScreenContentLoader
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletAlertUM
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletDialogConfig
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletEvent
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletEvent.DemonstrateWalletsScrollPreview.Direction
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletScreenState
import com.tangem.feature.wallet.presentation.wallet.state.transformers.*
import com.tangem.feature.wallet.presentation.wallet.state.utils.WalletEventSender
import com.tangem.feature.wallet.presentation.wallet.ui.components.visa.KycRejectedCallbacks
import com.tangem.feature.wallet.presentation.wallet.utils.ScreenLifecycleProvider
import com.tangem.features.biometry.AskBiometryComponent
import com.tangem.features.pushnotifications.api.PushNotificationsModelCallbacks
import com.tangem.features.wallet.deeplink.WalletDeepLinkActionListener
import com.tangem.features.wallet.featuretoggles.WalletFeatureToggles
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject

private const val TANGEM_PAY_UPDATE_INTERVAL = 60_000L

@Suppress("LongParameterList", "LargeClass")
@Stable
@ModelScoped
internal class WalletModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val stateHolder: WalletStateController,
    private val clickIntents: WalletClickIntents,
    private val walletEventSender: WalletEventSender,
    private val walletsUpdateActionResolver: WalletsUpdateActionResolver,
    private val walletScreenContentLoader: WalletScreenContentLoader,
    private val getSelectedWalletUseCase: GetSelectedWalletUseCase,
    private val getWalletsUseCase: GetWalletsUseCase,
    private val shouldShowAskBiometryUseCase: ShouldShowAskBiometryUseCase,
    private val shouldShowMarketsTooltipUseCase: ShouldShowMarketsTooltipUseCase,
    private val setWalletFirstTimeUsageUseCase: SetWalletFirstTimeUsageUseCase,
    private val canUseBiometryUseCase: CanUseBiometryUseCase,
    private val isWalletsScrollPreviewEnabled: IsWalletsScrollPreviewEnabled,
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    private val selectedWalletAnalyticsSender: SelectedWalletAnalyticsSender,
    private val walletNameMigrationUseCase: WalletNameMigrationUseCase,
    private val refreshMultiCurrencyWalletQuotesUseCase: RefreshMultiCurrencyWalletQuotesUseCase,
    private val walletImageResolver: WalletImageResolver,
    private val onrampStatusFactory: OnrampStatusFactory,
    private val analyticsEventsHandler: AnalyticsEventHandler,
    private val walletContentFetcher: WalletContentFetcher,
    private val walletDeepLinkActionListener: WalletDeepLinkActionListener,
    private val notificationsRepository: NotificationsRepository,
    private val getWalletsListForEnablingUseCase: GetWalletsForAutomaticallyPushEnablingUseCase,
    private val setNotificationsEnabledUseCase: SetNotificationsEnabledUseCase,
    private val getIsHuaweiDeviceWithoutGoogleServicesUseCase: GetIsHuaweiDeviceWithoutGoogleServicesUseCase,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val yieldSupplyApyUpdateUseCase: YieldSupplyApyUpdateUseCase,
    private val tangemPayOnboardingRepository: OnboardingRepository,
    private val tangemPayMainScreenCustomerInfoUseCase: TangemPayMainScreenCustomerInfoUseCase,
    private val getAppThemeModeUseCase: GetAppThemeModeUseCase,
    private val trackingContextProxy: TrackingContextProxy,
    private val singleAccountListSupplier: SingleAccountListSupplier,
    private val isAccountsModeEnabledUseCase: IsAccountsModeEnabledUseCase,
    private val bindRefcodeWithWalletUseCase: BindRefcodeWithWalletUseCase,
    private val appsFlyerStore: AppsFlyerStore,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val getWalletIconUseCase: GetWalletIconUseCase,
    private val walletFeatureToggles: WalletFeatureToggles,
    private val listenToQrScanningUseCase: ListenToQrScanningUseCase,
    private val wcPairService: WcPairService,
    private val resolveQrSendTargetsUseCase: ResolveQrSendTargetsUseCase,
    private val uiMessageSender: UiMessageSender,
    val screenLifecycleProvider: ScreenLifecycleProvider,
    val innerWalletRouter: InnerWalletRouter,
) : Model() {

    val askBiometryModelCallbacks = AskBiometryModelCallbacks()
    val tangemPayKycRejectedCallbacks = TangemPayKycRejectedCallbacks()
    val askForPushNotificationsModelCallbacks = AskForPushNotificationsCallbacks()
    val uiState: StateFlow<WalletScreenState> = stateHolder.uiState

    private val walletsUpdateJobHolder = JobHolder()
    private val refreshWalletJobHolder = JobHolder()
    private val updateTangemPayJobHolder = JobHolder()

    private var shouldRefreshWallet = false
    private val expressTxStatusTaskScheduler = SingleTaskScheduler<Unit>()

    init {
        trackScreenOpened()

        suggestToOpenMarkets()

        maybeMigrateNames()
        maybeSetWalletFirstTimeUsage()
        updateYieldSupplyApy()
        subscribeToUserWalletsUpdates()
        subscribeOnBalanceHiding()
        subscribeOnSelectedWalletFlow()
        subscribeToScreenBackgroundState()
        subscribeOnPushNotificationsPermission()
        subscribeTangemPayOnWalletState()
        subscribeToMainScreenQrScanning()
        enableNotificationsIfNeeded()

        clickIntents.initialize(innerWalletRouter, modelScope)

        modelScope.launch {
            bindRefcodeWithWalletUseCase.retry()
                .onLeft { Timber.e("Failed to bind refcode with wallets: $it") }
        }
    }

    fun onResume() {
        suggestToEnableBiometrics()
        suggestToOpenMarketsOnResume()
    }

    private fun suggestToOpenMarketsOnResume() {
        modelScope.launch {
            if (shouldShowMarketsTooltipUseCase()) {
                stateHolder.update {
                    it.copy(showMarketsOnboarding = true)
                }
            }
        }
    }

    private fun updateYieldSupplyApy() {
        modelScope.launch(dispatchers.default) {
            yieldSupplyApyUpdateUseCase()
        }
    }

    private fun maybeMigrateNames() {
        modelScope.launch {
            walletNameMigrationUseCase()
        }
    }

    private fun maybeSetWalletFirstTimeUsage() {
        modelScope.launch {
            setWalletFirstTimeUsageUseCase()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        stateHolder.clear()
        walletScreenContentLoader.cancelAll()
    }

    private fun suggestToEnableBiometrics() {
        modelScope.launch(dispatchers.main) {
            if (shouldShowAskBiometryBottomSheet()) {
                delay(timeMillis = 1_800)

                innerWalletRouter.dialogNavigation.activate(
                    configuration = WalletDialogConfig.AskForBiometry,
                )
            }
        }
    }

    private fun suggestToOpenMarkets() {
        modelScope.launch {
            withContext(dispatchers.io) { delay(timeMillis = 1_800) }

            if (shouldShowMarketsTooltipUseCase()) {
                stateHolder.update {
                    it.copy(showMarketsOnboarding = true)
                }
            }
        }
    }

    private fun trackScreenOpened() {
        modelScope.launch {
            userWalletsListRepository
                .selectedUserWalletSync()
                ?.let { selectedWallet ->
                    val hasMobileWallet = userWalletsListRepository.userWalletsSync()
                        .any { it is UserWallet.Hot }

                    val accountsCount = if (isAccountsModeEnabledUseCase.invokeSync()) {
                        singleAccountListSupplier(selectedWallet.walletId)
                            .first()
                            .accounts
                            .size
                    } else {
                        null
                    }
                    val result = getAppThemeModeUseCase().firstOrNull()
                    val theme = result?.getOrElse { AppThemeMode.FOLLOW_SYSTEM } ?: AppThemeMode.FOLLOW_SYSTEM
                    val appCurrency = getSelectedAppCurrencyUseCase.invokeSync().getOrElse { AppCurrency.Default }.code
                    analyticsEventsHandler.send(
                        WalletScreenAnalyticsEvent.MainScreen.ScreenOpened(
                            hasMobileWallet = hasMobileWallet,
                            accountsCount = accountsCount,
                            theme = theme.value,
                            isImported = selectedWallet.isImported(),
                            referralId = appsFlyerStore.get()?.refcode,
                            appCurrency = appCurrency,
                        ),
                    )
                }
        }
    }

    private suspend fun shouldShowAskBiometryBottomSheet(): Boolean {
        return userWalletsListRepository.userWalletsSync().any { it is UserWallet.Cold } &&
            shouldShowAskBiometryUseCase() &&
            canUseBiometryUseCase()
    }

    private fun subscribeToUserWalletsUpdates() = channelFlow<Unit> {
        val firstWalletsUseCaseEmit = MutableStateFlow(false)
        suspend fun waitFirstWalletsUseCaseEmit() = firstWalletsUseCaseEmit.filter { it }.first()

        // deepLinkActionFlow must wait for fist getWalletsUseCase() emit to correct handle Action.InitializeWallets
        walletDeepLinkActionListener.selectWalletFlow
            .onEach { waitFirstWalletsUseCaseEmit() }
            .onEach(::selectWalletById)
            .launchIn(this)

        getWalletsUseCase()
            .conflate()
            .distinctUntilChanged()
            .map { userWallets ->
                walletsUpdateActionResolver.resolve(
                    wallets = userWallets,
                    currentState = stateHolder.value,
                )
            }
            .onEach(::updateWallets)
            .onEach { firstWalletsUseCaseEmit.update { true } }
            .launchIn(this)
        awaitClose()
    }
        .flowOn(dispatchers.default)
        .launchIn(modelScope)
        .saveIn(walletsUpdateJobHolder)

    private fun subscribeOnBalanceHiding() {
        getBalanceHidingSettingsUseCase()
            .conflate()
            .distinctUntilChanged()
            .onEach {
                stateHolder.update(transformer = UpdateBalanceHidingModeTransformer(it.isBalanceHidden))
            }
            .flowOn(dispatchers.main)
            .launchIn(modelScope)
    }

    private fun subscribeOnPushNotificationsPermission() {
        modelScope.launch {
            val shouldAskNotificationPermissionsViaBs = notificationsRepository.shouldAskNotificationPermissionsViaBs()
            val shouldShow = notificationsRepository.shouldShowSubscribeOnNotificationsAfterUpdate()
            val isHuaweiDevice = getIsHuaweiDeviceWithoutGoogleServicesUseCase()
            Timber.d(
                "push BS afterUpdate: $shouldShow," +
                    "isHuaweiDevice $isHuaweiDevice",
            )
            if (!shouldAskNotificationPermissionsViaBs) {
                notificationsRepository.setShouldAskNotificationPermissionsViaBs(true)
                return@launch
            }

            if (!shouldShow) {
                return@launch
            }

            delay(timeMillis = 1_800)

            innerWalletRouter.dialogNavigation.activate(
                configuration = WalletDialogConfig.AskForPushNotifications,
            )
        }
    }

    // It's okay here because we need to be able to observe the selected wallet changes
    @Suppress("DEPRECATION")
    private fun subscribeOnSelectedWalletFlow() {
        getSelectedWalletUseCase().onRight { walletFlow ->
            walletFlow
                .conflate()
                .distinctUntilChanged()
                .onEach { selectedWallet ->
                    trackingContextProxy.setContext(selectedWallet)

                    if (selectedWallet.isMultiCurrency) {
                        selectedWalletAnalyticsSender.send(selectedWallet)
                    }

                    subscribeOnExpressTransactionsUpdates(selectedWallet)
                }
                .flowOn(dispatchers.main)
                .launchIn(modelScope)
        }
    }

    private fun selectWalletById(selectedWalletId: UserWalletId) {
        val currentWalletId = stateHolder.getSelectedWalletId()

        if (currentWalletId == selectedWalletId) return

        val currentIndex = stateHolder.getWalletIndexByWalletId(userWalletId = currentWalletId) ?: return
        val newIndex = stateHolder.getWalletIndexByWalletId(userWalletId = selectedWalletId) ?: return

        scrollToWallet(prevIndex = currentIndex, newIndex = newIndex) {
            stateHolder.update { it.copy(selectedWalletIndex = newIndex) }
        }
    }

    // We need to update the current wallet quotes if the application was in the background for more than 10 seconds
    // and then returned to the foreground
    private fun subscribeToScreenBackgroundState() {
        screenLifecycleProvider.isBackgroundState
            .onEach { isBackground ->
                refreshWalletJobHolder.cancel()
                when {
                    isBackground -> needToRefreshTimer()
                    shouldRefreshWallet && !isBackground -> {
                        triggerRefreshWalletQuotes()
                    }
                }
            }
            .launchIn(modelScope)
    }

    private fun subscribeOnExpressTransactionsUpdates(userWallet: UserWallet) {
        if (!userWallet.isMultiCurrency) {
            expressTxStatusTaskScheduler.cancelTask()
            expressTxStatusTaskScheduler.scheduleTask(
                modelScope,
                PeriodicTask(
                    isDelayFirst = false,
                    delay = EXPRESS_STATUS_UPDATE_DELAY,
                    task = {
                        runCatching {
                            onrampStatusFactory.updateOnrmapTransactionStatuses(userWallet)
                        }
                    },
                    onSuccess = { /* no-op */ },
                    onError = { /* no-op */ },
                ),
            )
        }
    }

    private fun subscribeTangemPayOnWalletState() {
        /**
         * Update state each time a user opens/returns to wallet screen
         * and every minute while user stays on the main screen
         */

        combine(
            flow = screenLifecycleProvider.isBackgroundState,
            flow2 = uiState.mapNotNull {
                it.wallets.getOrNull(it.selectedWalletIndex)?.walletCardState?.id
            }.distinctUntilChanged(),
            transform = ::Pair,
        ).onEach { (inBackground, userWalletId) ->
            if (inBackground) {
                updateTangemPayJobHolder.cancel()
                return@onEach
            }

            val savedCustomerInfo =
                tangemPayOnboardingRepository.getSavedCustomerInfo(userWalletId)

            val isShouldLaunchPeriodicUpdate = savedCustomerInfo?.cardInfo == null &&
                tangemPayOnboardingRepository.isTangemPayInitialDataProduced(userWalletId)

            if (isShouldLaunchPeriodicUpdate) {
                updateTangemPayJobHolder.cancel()
                modelScope.launch {
                    tangemPayMainScreenCustomerInfoUseCase.fetch(userWalletId)
                    while (isActive) {
                        delay(TANGEM_PAY_UPDATE_INTERVAL)
                        tangemPayMainScreenCustomerInfoUseCase.fetch(userWalletId)
                    }
                }.saveIn(updateTangemPayJobHolder)
            } else {
                // Don't refresh customer info periodically if the card was already issued, only update on swipe to refresh
                tangemPayMainScreenCustomerInfoUseCase.fetch(userWalletId)
            }
        }.launchIn(modelScope)
    }

    private fun needToRefreshTimer() {
        modelScope.launch {
            delay(REFRESH_WALLET_BACKGROUND_TIMER_MILLIS)
            shouldRefreshWallet = true
        }.saveIn(refreshWalletJobHolder)
    }

    private fun triggerRefreshWalletQuotes() {
        shouldRefreshWallet = false
        val state = stateHolder.uiState.value
        val wallet = state.wallets.getOrNull(state.selectedWalletIndex) ?: return
        modelScope.launch {
            awaitAll(
                async {
                    refreshMultiCurrencyWalletQuotesUseCase(wallet.walletCardState.id).getOrElse {
                        Timber.e("Failed to refreshMultiCurrencyWalletQuotesUseCase $it")
                    }
                },
                async {
                    getWalletsUseCase.invokeSync()
                        .firstOrNull { it.walletId == wallet.walletCardState.id }
                        ?.let(::subscribeOnExpressTransactionsUpdates)
                },
            )
        }.saveIn(refreshWalletJobHolder)
    }

    private suspend fun updateWallets(action: WalletsUpdateActionResolver.Action) {
        when (action) {
            is WalletsUpdateActionResolver.Action.InitializeWallets -> initializeWallets(action)
            is WalletsUpdateActionResolver.Action.ReinitializeNewWallet -> reinitializeNewWallet(action)
            is WalletsUpdateActionResolver.Action.ReinitializeWallets -> reinitializeWallets(action)
            is WalletsUpdateActionResolver.Action.AddWallet -> addWallet(action)
            is WalletsUpdateActionResolver.Action.DeleteWallet -> deleteWallet(action)
            is WalletsUpdateActionResolver.Action.UnlockWallet -> unlockWallet(action)
            is WalletsUpdateActionResolver.Action.UpdateWalletCardCount -> {
                // refresh loader to use actual user wallet
                walletScreenContentLoader.load(
                    userWallet = action.selectedWallet,
                    isRefresh = true,
                    coroutineScope = modelScope,
                )

                stateHolder.update(
                    transformer = UpdateWalletCardsCountTransformer(
                        userWallet = action.selectedWallet,
                        walletImageResolver = walletImageResolver,
                    ),
                )
            }
            is WalletsUpdateActionResolver.Action.RenameWallets -> {
                stateHolder.update(transformer = RenameWalletsTransformer(renamedWallets = action.renamedWallets))
            }
            is WalletsUpdateActionResolver.Action.ReloadWallets -> {
                reloadWarnings(action)
            }
            is WalletsUpdateActionResolver.Action.ReorderWallets -> reorderWallets(action)
            WalletsUpdateActionResolver.Action.EmptyWallets -> {
                Timber.w("Wallets list is empty!")
            }
            is WalletsUpdateActionResolver.Action.Unknown -> {
                Timber.w("Unable to perform action: $action")
            }
        }
    }

    private fun reorderWallets(action: WalletsUpdateActionResolver.Action.ReorderWallets) {
        val currentWalletId = stateHolder.getSelectedWalletId()
        val currentIndex = stateHolder.getWalletIndexByWalletId(userWalletId = currentWalletId)

        stateHolder.update(
            transformer = ReorderWalletsTransformer(
                wallets = action.wallets,
            ),
        )

        val newIndex = stateHolder.getWalletIndexByWalletId(userWalletId = currentWalletId)

        if (currentIndex != null && newIndex != null && currentIndex != newIndex) {
            scrollToWallet(prevIndex = currentIndex, newIndex = newIndex) {
                stateHolder.update { it.copy(selectedWalletIndex = newIndex) }
            }
        }
    }

    private fun reloadWarnings(action: WalletsUpdateActionResolver.Action.ReloadWallets) {
        action.wallets.forEach { userWallet ->
            walletScreenContentLoader.load(
                userWallet = userWallet,
                coroutineScope = modelScope,
                isRefresh = true,
            )
        }
    }

    private suspend fun initializeWallets(action: WalletsUpdateActionResolver.Action.InitializeWallets) {
        stateHolder.update(
            transformer = InitializeWalletsTransformer(
                selectedWalletIndex = action.selectedWalletIndex,
                wallets = action.wallets,
                clickIntents = clickIntents,
                walletImageResolver = walletImageResolver,
                isMainScreenQrScanningEnabled = walletFeatureToggles.isMainScreenQrScanningEnabled,
                getWalletIconUseCase = getWalletIconUseCase,
            ),
        )

        walletScreenContentLoader.load(
            userWallet = action.selectedWallet,
            coroutineScope = modelScope,
        )

        fetchWalletContent(userWallet = action.selectedWallet)

        val otherWallets = action.wallets.minus(action.selectedWallet)

        otherWallets
            .filterNot(UserWallet::isLocked)
            .onEach { userWallet ->
                modelScope.launch { walletContentFetcher(userWalletId = userWallet.walletId) }
            }

        if (action.wallets.size > 1 && isWalletsScrollPreviewEnabled()) {
            val direction = if (action.selectedWalletIndex == action.wallets.lastIndex) {
                Direction.RIGHT
            } else {
                Direction.LEFT
            }

            demonstrateWalletsScrollPreview(direction = direction)
        }
    }

    private fun reinitializeNewWallet(action: WalletsUpdateActionResolver.Action.ReinitializeNewWallet) {
        walletScreenContentLoader.cancel(action.prevWalletId)

        walletScreenContentLoader.load(
            userWallet = action.selectedWallet,
            coroutineScope = modelScope,
        )

        fetchWalletContent(userWallet = action.selectedWallet)

        stateHolder.update(
            ReinitializeNewWalletTransformer(
                prevWalletId = action.prevWalletId,
                newUserWallet = action.selectedWallet,
                clickIntents = clickIntents,
                walletImageResolver = walletImageResolver,
                getWalletIconUseCase = getWalletIconUseCase,
            ),
        )
    }

    private fun reinitializeWallets(action: WalletsUpdateActionResolver.Action.ReinitializeWallets) {
        action.wallets.forEach { userWallet ->
            walletScreenContentLoader.cancel(userWallet.walletId)

            walletScreenContentLoader.load(
                userWallet = userWallet,
                coroutineScope = modelScope,
            )

            fetchWalletContent(userWallet = userWallet)

            stateHolder.update(
                ReinitializeWalletTransformer(
                    userWallet = userWallet,
                    clickIntents = clickIntents,
                    walletImageResolver = walletImageResolver,
                    getWalletIconUseCase = getWalletIconUseCase,
                ),
            )
        }
    }

    private fun addWallet(action: WalletsUpdateActionResolver.Action.AddWallet) {
        fetchWalletContent(userWallet = action.selectedWallet)

        stateHolder.update(
            AddWalletTransformer(
                userWallet = action.selectedWallet,
                clickIntents = clickIntents,
                walletImageResolver = walletImageResolver,
                getWalletIconUseCase = getWalletIconUseCase,
            ),
        )

        walletScreenContentLoader.load(
            userWallet = action.selectedWallet,
            coroutineScope = modelScope,
        )

        scrollToWallet(prevIndex = action.prevWalletIndex, newIndex = action.selectedWalletIndex) {
            stateHolder.update {
                it.copy(selectedWalletIndex = action.selectedWalletIndex)
            }
        }

        enableNotificationsIfNeeded()
    }

    private fun deleteWallet(action: WalletsUpdateActionResolver.Action.DeleteWallet) {
        walletScreenContentLoader.cancel(action.deletedWalletId)

        walletScreenContentLoader.load(
            userWallet = action.selectedWallet,
            coroutineScope = modelScope,
        )

        val newSelectedWalletIndex = if (action.selectedWalletIndex - action.deletedWalletIndex == 1) {
            action.deletedWalletIndex
        } else {
            action.selectedWalletIndex
        }

        scrollToWallet(
            prevIndex = action.deletedWalletIndex,
            newIndex = action.selectedWalletIndex,
            onConsume = {
                updateStateByDeleteWalletTransformer(
                    selectedWalletIndex = newSelectedWalletIndex,
                    deletedWalletId = action.deletedWalletId,
                )
            },
        )
    }

    private fun updateStateByDeleteWalletTransformer(selectedWalletIndex: Int, deletedWalletId: UserWalletId) {
        stateHolder.update(
            DeleteWalletTransformer(
                selectedWalletIndex = selectedWalletIndex,
                deletedWalletId = deletedWalletId,
            ),
        )
    }

    private suspend fun unlockWallet(action: WalletsUpdateActionResolver.Action.UnlockWallet) {
        delay(timeMillis = 700)

        stateHolder.update(
            transformer = UnlockWalletTransformer(
                unlockedWallets = action.unlockedWallets,
                clickIntents = clickIntents,
                walletImageResolver = walletImageResolver,
                getWalletIconUseCase = getWalletIconUseCase,
            ),
        )

        walletScreenContentLoader.load(
            userWallet = action.selectedWallet,
            coroutineScope = modelScope,
        )

        action.unlockedWallets.onEach { userWallet ->
            fetchWalletContent(userWallet = userWallet)
        }
    }

    private fun demonstrateWalletsScrollPreview(direction: Direction) {
        modelScope.launch(dispatchers.mainImmediate) {
            delay(timeMillis = 1_800)

            walletEventSender.send(
                event = WalletEvent.DemonstrateWalletsScrollPreview(direction = direction),
            )
        }
    }

    private fun scrollToWallet(prevIndex: Int, newIndex: Int, onConsume: () -> Unit = {}) {
        // Should not show scroll animation if WalletScreen isn't in the background.
        if (screenLifecycleProvider.isBackgroundState.value) {
            onConsume()
            stateHolder.update(
                ScrollToWalletTransformer(
                    prevIndex = prevIndex,
                    newIndex = newIndex,
                    currentStateProvider = Provider(action = stateHolder::value),
                    withScrollAnimation = false,
                    stateUpdater = { newState -> stateHolder.update { newState } },
                    onConsume = {},
                ),
            )
        } else {
            stateHolder.update(
                ScrollToWalletTransformer(
                    prevIndex = prevIndex,
                    newIndex = newIndex,
                    currentStateProvider = Provider(action = stateHolder::value),
                    stateUpdater = { newState -> stateHolder.update { newState } },
                    onConsume = onConsume,
                ),
            )
        }
    }

    private fun fetchWalletContent(userWallet: UserWallet) {
        if (userWallet.isLocked) return

        /*
         * Updating the balance of the current wallet is an essential part of InitializationWallets,
         * so the coroutine is launched in the current context
         */
        modelScope.launch {
            walletContentFetcher(userWalletId = userWallet.walletId)
        }
    }

    private fun subscribeToMainScreenQrScanning() {
        listenToQrScanningUseCase.listen(SourceType.MAIN_SCREEN)
            .getOrElse { emptyFlow() }
            .onEach { rawResult -> handleQrResult(rawResult.qrCode, rawResult.resultSource) }
            .launchIn(modelScope)
    }

    private suspend fun handleQrResult(qrCode: String, resultSource: QrResultSource) {
        val target = resolveQrSendTargetsUseCase(qrCode)
        handleQrTarget(target, resultSource)
    }

    private fun handleQrTarget(target: QrSendTarget, resultSource: QrResultSource) {
        when (target) {
            is QrSendTarget.WalletConnect -> {
                val source = when (resultSource) {
                    QrResultSource.CLIPBOARD -> WcPairRequest.Source.CLIPBOARD
                    QrResultSource.CAMERA,
                    QrResultSource.GALLERY,
                    -> WcPairRequest.Source.QR
                }
                wcPairService.pair(
                    WcPairRequest(
                        userWalletId = stateHolder.getSelectedWalletId(),
                        uri = target.uri,
                        source = source,
                        screen = WcPairRequest.Screen.MAIN,
                    ),
                )
            }
            is QrSendTarget.Single -> {
                innerWalletRouter.openSend(
                    userWalletId = target.userWalletId,
                    currency = target.currency,
                    address = target.address,
                    amount = target.amount?.parseBigDecimal(target.currency.decimals),
                    tag = target.memo,
                    entryType = AppRoute.Send.EntryType.QR,
                )
            }
            is QrSendTarget.Multiple -> {
                innerWalletRouter.openNetworkSelectionBottomSheet(target)
            }
            is QrSendTarget.AddressSameAsWallet -> {
                uiMessageSender.send(WalletAlertUM.qrCodeAddressSameAsWallet())
            }
            is QrSendTarget.Warning -> {
                uiMessageSender.send(
                    WalletAlertUM.qrCodeUnsupportedParams(
                        unsupportedParams = target.unsupportedParams,
                        onContinue = { handleQrTarget(target.target, resultSource) },
                    ),
                )
            }
            is QrSendTarget.Error -> handleQrError(target.error)
        }
    }

    private fun handleQrError(error: ClassifiedQrContent.Error) {
        when (error) {
            is ClassifiedQrContent.Error.Unrecognized -> {
                analyticsEventsHandler.send(
                    WalletScreenAnalyticsEvent.MainScreen.NoticeUnrecognizedQr(),
                )
                uiMessageSender.send(WalletAlertUM.qrCodeUnrecognized())
            }
            is ClassifiedQrContent.Error.UnsupportedNetwork -> {
                analyticsEventsHandler.send(
                    WalletScreenAnalyticsEvent.MainScreen.NoticeNoAvailableTokens(
                        blockchain = error.blockchain,
                    ),
                )
                uiMessageSender.send(WalletAlertUM.qrCodeUnsupportedNetwork())
            }
        }
    }

    private fun enableNotificationsIfNeeded() {
        modelScope.launch {
            val isUserAllowToEnableNotifications = notificationsRepository.isUserAllowToSubscribeOnPushNotifications()
            if (isUserAllowToEnableNotifications) {
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
        }
    }

    inner class AskBiometryModelCallbacks : AskBiometryComponent.ModelCallbacks {
        override fun onAllowed() {
            analyticsEventsHandler.send(MainScreenAnalyticsEvent.EnableBiometrics(AnalyticsParam.OnOffState.On))
            innerWalletRouter.dialogNavigation.dismiss()
        }

        override fun onDenied() {
            analyticsEventsHandler.send(MainScreenAnalyticsEvent.EnableBiometrics(AnalyticsParam.OnOffState.Off))
            innerWalletRouter.dialogNavigation.dismiss()
        }
    }

    inner class TangemPayKycRejectedCallbacks : KycRejectedCallbacks {
        override fun onClickYourProfile(userWalletId: UserWalletId) {
            clickIntents.onKycRejectedOpenProfileClicked(userWalletId)
        }

        override fun onClickGoToSupport(customerId: String) {
            clickIntents.onKycRejectedGoToSupportClicked(customerId)
        }

        override fun onClickHideKyc(userWalletId: UserWalletId) {
            clickIntents.onKycRejectedHideKycClicked(userWalletId)
        }
    }

    inner class AskForPushNotificationsCallbacks : PushNotificationsModelCallbacks {

        override fun onAllowSystemPermission() {
            innerWalletRouter.dialogNavigation.dismiss()
            enableNotificationsIfNeeded()
        }

        override fun onDenySystemPermission() {
            innerWalletRouter.dialogNavigation.dismiss()
            enableNotificationsIfNeeded()
        }

        override fun onDismiss() {
            innerWalletRouter.dialogNavigation.dismiss()
        }
    }

    private companion object {
        const val REFRESH_WALLET_BACKGROUND_TIMER_MILLIS = 10000L
        const val EXPRESS_STATUS_UPDATE_DELAY = 10000L
    }
}