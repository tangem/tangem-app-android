package com.tangem.feature.wallet.child.wallet.model

import androidx.compose.runtime.Stable
import arrow.core.getOrElse
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.routing.RoutingFeatureToggle
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.event.MainScreenAnalyticsEvent
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.deeplink.DeepLinksRegistry
import com.tangem.core.deeplink.global.ReferralDeepLink
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.nft.ObserveAndClearNFTCacheIfNeedUseCase
import com.tangem.domain.settings.*
import com.tangem.domain.tokens.FetchCurrencyStatusUseCase
import com.tangem.domain.tokens.RefreshMultiCurrencyWalletQuotesUseCase
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.usecase.GetSelectedWalletUseCase
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.presentation.deeplink.WalletDeepLinksHandler
import com.tangem.feature.wallet.presentation.router.InnerWalletRouter
import com.tangem.feature.wallet.presentation.wallet.analytics.WalletScreenAnalyticsEvent
import com.tangem.feature.wallet.presentation.wallet.analytics.utils.SelectedWalletAnalyticsSender
import com.tangem.feature.wallet.presentation.wallet.domain.MultiWalletTokenListStore
import com.tangem.feature.wallet.presentation.wallet.domain.OnrampStatusFactory
import com.tangem.feature.wallet.presentation.wallet.domain.WalletImageResolver
import com.tangem.feature.wallet.presentation.wallet.domain.WalletNameMigrationUseCase
import com.tangem.feature.wallet.presentation.wallet.loaders.WalletScreenContentLoader
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state.model.PushNotificationsBottomSheetConfig
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletDialogConfig
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletEvent
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletEvent.DemonstrateWalletsScrollPreview.Direction
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletScreenState
import com.tangem.feature.wallet.presentation.wallet.state.transformers.*
import com.tangem.feature.wallet.presentation.wallet.state.utils.WalletEventSender
import com.tangem.feature.wallet.presentation.wallet.utils.ScreenLifecycleProvider
import com.tangem.features.biometry.AskBiometryComponent
import com.tangem.features.pushnotifications.api.utils.PUSH_PERMISSION
import com.tangem.features.pushnotifications.api.utils.getPushPermissionOrNull
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject

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
    private val shouldShowSaveWalletScreenUseCase: ShouldShowSaveWalletScreenUseCase,
    private val shouldShowMarketsTooltipUseCase: ShouldShowMarketsTooltipUseCase,
    private val setWalletFirstTimeUsageUseCase: SetWalletFirstTimeUsageUseCase,
    private val canUseBiometryUseCase: CanUseBiometryUseCase,
    private val isWalletsScrollPreviewEnabled: IsWalletsScrollPreviewEnabled,
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    private val selectedWalletAnalyticsSender: SelectedWalletAnalyticsSender,
    private val walletDeepLinksHandler: WalletDeepLinksHandler,
    private val walletNameMigrationUseCase: WalletNameMigrationUseCase,
    private val refreshMultiCurrencyWalletQuotesUseCase: RefreshMultiCurrencyWalletQuotesUseCase,
    private val shouldAskPermissionUseCase: ShouldAskPermissionUseCase,
    private val walletImageResolver: WalletImageResolver,
    private val tokenListStore: MultiWalletTokenListStore,
    private val onrampStatusFactory: OnrampStatusFactory,
    private val analyticsEventsHandler: AnalyticsEventHandler,
    private val deepLinksRegistry: DeepLinksRegistry,
    private val fetchCurrencyStatusUseCase: FetchCurrencyStatusUseCase,
    private val appRouter: AppRouter,
    private val routingFeatureToggle: RoutingFeatureToggle,
    private val observeAndClearNFTCacheIfNeedUseCase: ObserveAndClearNFTCacheIfNeedUseCase,
    val screenLifecycleProvider: ScreenLifecycleProvider,
    val innerWalletRouter: InnerWalletRouter,
) : Model() {

    val askBiometryModelCallbacks = AskBiometryModelCallbacks()
    val uiState: StateFlow<WalletScreenState> = stateHolder.uiState

    private val walletsUpdateJobHolder = JobHolder()
    private val refreshWalletJobHolder = JobHolder()
    private var needToRefreshWallet = false
    private val clearNFTCacheJobHolder = JobHolder()

    private var expressTxStatusTaskScheduler = SingleTaskScheduler<Unit>()

    init {
        analyticsEventsHandler.send(WalletScreenAnalyticsEvent.MainScreen.ScreenOpened)

        suggestToEnableBiometrics()
        suggestToOpenMarkets()

        maybeMigrateNames()
        maybeSetWalletFirstTimeUsage()
        subscribeToUserWalletsUpdates()
        subscribeOnBalanceHiding()
        subscribeOnSelectedWalletFlow()
        subscribeToScreenBackgroundState()
        subscribeOnPushNotificationsPermission()

        clickIntents.initialize(innerWalletRouter, modelScope)
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

        tokenListStore.clear()
        stateHolder.clear()
        walletScreenContentLoader.cancelAll()
    }

    private fun suggestToEnableBiometrics() {
        modelScope.launch(dispatchers.main) {
            withContext(dispatchers.io) { delay(timeMillis = 1_800) }

            if (isShowSaveWalletScreenEnabled()) {
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

            shouldShowMarketsTooltipUseCase(isShown = true)
        }
    }

    private suspend fun isShowSaveWalletScreenEnabled(): Boolean {
        return innerWalletRouter.isWalletLastScreen() && shouldShowSaveWalletScreenUseCase() && canUseBiometryUseCase()
    }

    private fun subscribeToUserWalletsUpdates() {
        getWalletsUseCase()
            .conflate()
            .distinctUntilChanged()
            .map {
                walletsUpdateActionResolver.resolve(
                    wallets = it,
                    currentState = stateHolder.value,
                )
            }
            .onEach(::updateWallets)
            .flowOn(dispatchers.main)
            .launchIn(modelScope)
            .saveIn(walletsUpdateJobHolder)
    }

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
            val shouldRequestPush = shouldAskPermissionUseCase(PUSH_PERMISSION)
            val isPushPermissionAvailable = getPushPermissionOrNull() != null
            if (!shouldRequestPush || !isPushPermissionAvailable) return@launch

            delay(timeMillis = 1_800)

            stateHolder.showBottomSheet(
                content = PushNotificationsBottomSheetConfig(
                    onRequest = clickIntents::onRequestPushPermission,
                    onNeverRequest = { clickIntents.onNeverAskPushPermission(false) },
                    onAllow = clickIntents::onAllowPushPermission,
                    onDeny = clickIntents::onDenyPushPermission,
                ),
                onDismiss = { clickIntents.onNeverAskPushPermission(true) },
            )
        }
    }

    // It's okay here because we need to be able to observe the selected wallet changes
    @Suppress("DEPRECATION")
    private fun subscribeOnSelectedWalletFlow() {
        getSelectedWalletUseCase().onRight {
            it
                .conflate()
                .distinctUntilChanged()
                .onEach { selectedWallet ->
                    if (selectedWallet.isMultiCurrency) {
                        selectedWalletAnalyticsSender.send(selectedWallet)
                    }

                    if (!routingFeatureToggle.isDeepLinkNavigationEnabled) {
                        // Registering here, because `WalletDeepLinksHandler` unregisters deeplink when scope is cancelled
                        // This is temporary solution, will be removed with complete deeplink navigation overhaul
                        addReferralDeepLink(selectedWallet)
                        walletDeepLinksHandler.registerForWallet(scope = modelScope, userWallet = selectedWallet)
                    }
                    subscribeOnExpressTransactionsUpdates(selectedWallet)
                    observeAndClearNFTCacheIfNeedUseCase(selectedWallet)
                }
                .flowOn(dispatchers.main)
                .launchIn(modelScope)
        }
    }

    private fun addReferralDeepLink(userWallet: UserWallet) {
        deepLinksRegistry.register(
            ReferralDeepLink(
                onReceive = {
                    if (userWallet.cardTypesResolver.isTangemWallet()) {
                        appRouter.push(
                            AppRoute.ReferralProgram(userWalletId = userWallet.walletId),
                        )
                    }
                },
            ),
        )
    }

    // We need to update the current wallet quotes if the application was in the background for more than 10 seconds
    // and then returned to the foreground
    private fun subscribeToScreenBackgroundState() {
        screenLifecycleProvider.isBackgroundState
            .onEach { isBackground ->
                refreshWalletJobHolder.cancel()
                when {
                    isBackground -> needToRefreshTimer()
                    needToRefreshWallet && !isBackground -> {
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

    private fun observeAndClearNFTCacheIfNeedUseCase(selectedWallet: UserWallet) {
        observeAndClearNFTCacheIfNeedUseCase
            .invoke(selectedWallet.walletId)
            .launchIn(modelScope)
            .saveIn(clearNFTCacheJobHolder)
    }

    private fun needToRefreshTimer() {
        modelScope.launch {
            delay(REFRESH_WALLET_BACKGROUND_TIMER_MILLIS)
            needToRefreshWallet = true
        }.saveIn(refreshWalletJobHolder)
    }

    private fun triggerRefreshWalletQuotes() {
        needToRefreshWallet = false
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
            is WalletsUpdateActionResolver.Action.ReinitializeWallet -> reinitializeWallet(action)
            is WalletsUpdateActionResolver.Action.AddWallet -> addWallet(action)
            is WalletsUpdateActionResolver.Action.DeleteWallet -> deleteWallet(action)
            is WalletsUpdateActionResolver.Action.UnlockWallet -> unlockWallet(action)
            is WalletsUpdateActionResolver.Action.UpdateWalletCardCount -> {
                // refresh loader to use actual user wallet
                walletScreenContentLoader.load(
                    userWallet = action.selectedWallet,
                    clickIntents = clickIntents,
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
            is WalletsUpdateActionResolver.Action.Unknown -> {
                Timber.w("Unable to perform action: $action")
            }
        }
    }

    private suspend fun initializeWallets(action: WalletsUpdateActionResolver.Action.InitializeWallets) {
        stateHolder.update(
            transformer = InitializeWalletsTransformer(
                selectedWalletIndex = action.selectedWalletIndex,
                wallets = action.wallets,
                clickIntents = clickIntents,
                walletImageResolver = walletImageResolver,
            ),
        )

        walletScreenContentLoader.load(
            userWallet = action.selectedWallet,
            clickIntents = clickIntents,
            coroutineScope = modelScope,
        )

        fetchIfSingleWallet(action.selectedWallet)

        if (action.wallets.size > 1 && isWalletsScrollPreviewEnabled()) {
            withContext(dispatchers.io) { delay(timeMillis = 1_800) }

            walletEventSender.send(
                event = WalletEvent.DemonstrateWalletsScrollPreview(
                    direction = if (action.selectedWalletIndex == action.wallets.lastIndex) {
                        Direction.RIGHT
                    } else {
                        Direction.LEFT
                    },
                ),
            )
        }
    }

    private fun reinitializeWallet(action: WalletsUpdateActionResolver.Action.ReinitializeWallet) {
        walletScreenContentLoader.cancel(action.prevWalletId)
        tokenListStore.remove(action.prevWalletId)

        walletScreenContentLoader.load(
            userWallet = action.selectedWallet,
            clickIntents = clickIntents,
            coroutineScope = modelScope,
        )

        fetchIfSingleWallet(userWallet = action.selectedWallet)

        stateHolder.update(
            ReinitializeWalletTransformer(
                prevWalletId = action.prevWalletId,
                newUserWallet = action.selectedWallet,
                clickIntents = clickIntents,
                walletImageResolver = walletImageResolver,
            ),
        )
    }

    private fun addWallet(action: WalletsUpdateActionResolver.Action.AddWallet) {
        walletScreenContentLoader.load(
            userWallet = action.selectedWallet,
            clickIntents = clickIntents,
            coroutineScope = modelScope,
        )

        fetchIfSingleWallet(userWallet = action.selectedWallet)

        stateHolder.update(
            AddWalletTransformer(
                userWallet = action.selectedWallet,
                clickIntents = clickIntents,
                walletImageResolver = walletImageResolver,
            ),
        )

        scrollToWallet(prevIndex = action.prevWalletIndex, newIndex = action.selectedWalletIndex) {
            stateHolder.update {
                it.copy(selectedWalletIndex = action.selectedWalletIndex)
            }
        }
    }

    private fun deleteWallet(action: WalletsUpdateActionResolver.Action.DeleteWallet) {
        walletScreenContentLoader.cancel(action.deletedWalletId)
        tokenListStore.remove(action.deletedWalletId)

        walletScreenContentLoader.load(
            userWallet = action.selectedWallet,
            clickIntents = clickIntents,
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
        withContext(dispatchers.io) { delay(timeMillis = 700) }

        stateHolder.update(
            transformer = UnlockWalletTransformer(
                unlockedWallets = action.unlockedWallets,
                clickIntents = clickIntents,
                walletImageResolver = walletImageResolver,
            ),
        )

        walletScreenContentLoader.load(
            userWallet = action.selectedWallet,
            clickIntents = clickIntents,
            coroutineScope = modelScope,
        )
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

    private fun fetchIfSingleWallet(userWallet: UserWallet) {
        if (userWallet.scanResponse.cardTypesResolver.isSingleWallet()) {
            modelScope.launch {
                fetchCurrencyStatusUseCase(userWalletId = userWallet.walletId)
                    .onLeft { Timber.e(it.toString()) }
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

    private companion object {
        const val REFRESH_WALLET_BACKGROUND_TIMER_MILLIS = 10000L
        const val EXPRESS_STATUS_UPDATE_DELAY = 10000L
    }
}