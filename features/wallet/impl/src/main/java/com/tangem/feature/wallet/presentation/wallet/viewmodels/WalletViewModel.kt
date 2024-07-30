package com.tangem.feature.wallet.presentation.wallet.viewmodels

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.getOrElse
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.navigation.settings.SettingsManager
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.settings.*
import com.tangem.domain.tokens.RefreshMultiCurrencyWalletQuotesUseCase
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.usecase.GetSelectedWalletUseCase
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.feature.wallet.presentation.deeplink.WalletDeepLinksHandler
import com.tangem.feature.wallet.presentation.router.InnerWalletRouter
import com.tangem.feature.wallet.presentation.wallet.analytics.WalletScreenAnalyticsEvent
import com.tangem.feature.wallet.presentation.wallet.analytics.utils.SelectedWalletAnalyticsSender
import com.tangem.feature.wallet.presentation.wallet.domain.WalletNameMigrationUseCase
import com.tangem.feature.wallet.presentation.wallet.loaders.WalletScreenContentLoader
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state.model.PushNotificationsBottomSheetConfig
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletEvent
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletEvent.DemonstrateWalletsScrollPreview.Direction
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletScreenState
import com.tangem.feature.wallet.presentation.wallet.state.transformers.*
import com.tangem.feature.wallet.presentation.wallet.state.utils.WalletEventSender
import com.tangem.feature.wallet.presentation.wallet.utils.ScreenLifecycleProvider
import com.tangem.feature.wallet.presentation.wallet.viewmodels.intents.WalletClickIntents
import com.tangem.features.pushnotifications.api.featuretoggles.PushNotificationsFeatureToggles
import com.tangem.features.pushnotifications.api.utils.PUSH_PERMISSION
import com.tangem.features.pushnotifications.api.utils.getPushPermissionOrNull
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import com.tangem.utils.extensions.indexOfFirstOrNull
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@Suppress("LongParameterList", "LargeClass")
@HiltViewModel
internal class WalletViewModel @Inject constructor(
    private val stateHolder: WalletStateController,
    private val clickIntents: WalletClickIntents,
    private val walletEventSender: WalletEventSender,
    private val walletsUpdateActionResolver: WalletsUpdateActionResolver,
    private val walletScreenContentLoader: WalletScreenContentLoader,
    private val getSelectedWalletUseCase: GetSelectedWalletUseCase,
    private val getWalletsUseCase: GetWalletsUseCase,
    private val shouldShowSaveWalletScreenUseCase: ShouldShowSaveWalletScreenUseCase,
    private val canUseBiometryUseCase: CanUseBiometryUseCase,
    private val isWalletsScrollPreviewEnabled: IsWalletsScrollPreviewEnabled,
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    private val dispatchers: CoroutineDispatcherProvider,
    private val screenLifecycleProvider: ScreenLifecycleProvider,
    private val selectedWalletAnalyticsSender: SelectedWalletAnalyticsSender,
    private val walletDeepLinksHandler: WalletDeepLinksHandler,
    private val walletNameMigrationUseCase: WalletNameMigrationUseCase,
    private val refreshMultiCurrencyWalletQuotesUseCase: RefreshMultiCurrencyWalletQuotesUseCase,
    private val shouldInitiallyAskPermissionUseCase: ShouldInitiallyAskPermissionUseCase,
    private val isFirstTimeAskingPermissionUseCase: IsFirstTimeAskingPermissionUseCase,
    private val shouldAskPermissionUseCase: ShouldAskPermissionUseCase,
    private val pushNotificationsFeatureToggles: PushNotificationsFeatureToggles,
    private val settingsManager: SettingsManager,
    analyticsEventsHandler: AnalyticsEventHandler,
) : ViewModel() {

    val uiState: StateFlow<WalletScreenState> = stateHolder.uiState

    private lateinit var router: InnerWalletRouter
    private val walletsUpdateJobHolder = JobHolder()
    private val refreshWalletJobHolder = JobHolder()
    private var needToRefreshWallet = false

    init {
        analyticsEventsHandler.send(WalletScreenAnalyticsEvent.MainScreen.ScreenOpened)

        suggestToEnableBiometrics()

        maybeMigrateNames()
        subscribeToUserWalletsUpdates()
        subscribeOnBalanceHiding()
        subscribeOnSelectedWalletFlow()
        subscribeToScreenBackgroundState()
        subscribeOnPushNotificationsPermission()
    }

    private fun maybeMigrateNames() {
        viewModelScope.launch {
            walletNameMigrationUseCase()
        }
    }

    fun setWalletRouter(router: InnerWalletRouter) {
        this.router = router
        clickIntents.initialize(router, viewModelScope)
    }

    fun subscribeToLifecycle(lifecycleOwner: LifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(screenLifecycleProvider)
    }

    override fun onCleared() {
        super.onCleared()
        stateHolder.clear()
        walletScreenContentLoader.cancelAll()
    }

    private fun suggestToEnableBiometrics() {
        viewModelScope.launch(dispatchers.main) {
            withContext(dispatchers.io) { delay(timeMillis = 1_800) }

            if (isShowSaveWalletScreenEnabled()) router.openSaveUserWalletScreen()
        }
    }

    private suspend fun isShowSaveWalletScreenEnabled(): Boolean {
        return router.isWalletLastScreen() && shouldShowSaveWalletScreenUseCase() && canUseBiometryUseCase()
    }

    private fun subscribeToUserWalletsUpdates() {
        getWalletsUseCase()
            .conflate()
            .distinctUntilChanged()
            .map {
                walletsUpdateActionResolver.resolve(wallets = it, currentState = stateHolder.value)
            }
            .onEach(::updateWallets)
            .flowOn(dispatchers.main)
            .launchIn(viewModelScope)
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
            .launchIn(viewModelScope)
    }

    private fun subscribeOnPushNotificationsPermission() {
        viewModelScope.launch {
            val isPushToggled = pushNotificationsFeatureToggles.isPushNotificationsEnabled
            val shouldRequestPush = shouldAskPermissionUseCase(PUSH_PERMISSION)
            val isPushPermissionAvailable = getPushPermissionOrNull() != null
            if (!isPushToggled || !shouldRequestPush || !isPushPermissionAvailable) return@launch

            delay(timeMillis = 1_800)

            val isFirstTimeRequested = isFirstTimeAskingPermissionUseCase(PUSH_PERMISSION).getOrElse { true }
            val wasInitiallyAsk = shouldInitiallyAskPermissionUseCase(PUSH_PERMISSION).getOrElse { true }
            val onRequestLater: () -> Unit = if (wasInitiallyAsk) {
                clickIntents::onDelayAskPushPermission
            } else {
                clickIntents::onNeverAskPushPermission
            }
            stateHolder.showBottomSheet(
                content = PushNotificationsBottomSheetConfig(
                    isFirstTimeRequested = isFirstTimeRequested,
                    wasInitiallyAsk = wasInitiallyAsk,
                    onRequest = clickIntents::onRequestPushPermission,
                    onRequestLater = onRequestLater,
                    onAllow = clickIntents::onAllowPushPermission,
                    onDeny = clickIntents::onDenyPushPermission,
                    openSettings = settingsManager::openSettings,
                ),
                onDismiss = onRequestLater,
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

                    changeSelectedWalletState(selectedWalletId = selectedWallet.walletId)

                    walletDeepLinksHandler.registerForWallet(viewModel = this, userWallet = selectedWallet)
                }
                .flowOn(dispatchers.main)
                .launchIn(viewModelScope)
        }
    }

    /** Change selected wallet state if selected wallet [selectedWalletId] was changed in the background */
    private suspend fun changeSelectedWalletState(selectedWalletId: UserWalletId) {
        if (screenLifecycleProvider.isBackgroundState.value && selectedWalletId != stateHolder.getSelectedWalletId()) {
            stateHolder.value.wallets
                .indexOfFirstOrNull { prevState -> prevState.walletCardState.id == selectedWalletId }
                ?.let { selectedIndex ->
                    Timber.e("Selected wallet changed from background state: $selectedWalletId")

                    delay(timeMillis = 1000)
                    scrollToWallet(selectedIndex)
                }
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
                    needToRefreshWallet && !isBackground -> triggerRefreshWalletQuotes()
                }
            }
            .launchIn(viewModelScope)
    }

    private fun needToRefreshTimer() {
        viewModelScope.launch {
            delay(REFRESH_WALLET_BACKGROUND_TIMER_MILLIS)
            needToRefreshWallet = true
        }.saveIn(refreshWalletJobHolder)
    }

    private fun triggerRefreshWalletQuotes() {
        needToRefreshWallet = false
        val state = stateHolder.uiState.value
        val wallet = state.wallets.getOrNull(state.selectedWalletIndex) ?: return
        viewModelScope.launch {
            refreshMultiCurrencyWalletQuotesUseCase(wallet.walletCardState.id).getOrElse {
                Timber.e("Failed to refreshMultiCurrencyWalletQuotesUseCase $it")
            }
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
                    coroutineScope = viewModelScope,
                )

                stateHolder.update(transformer = UpdateWalletCardsCountTransformer(action.selectedWallet))
            }
            is WalletsUpdateActionResolver.Action.UpdateWalletName -> {
                stateHolder.update(transformer = RenameWalletTransformer(action.selectedWalletId, action.name))
            }
            is WalletsUpdateActionResolver.Action.Unknown -> {
                Timber.w("Unable to perfom action: $action")
            }
        }
    }

    private suspend fun initializeWallets(action: WalletsUpdateActionResolver.Action.InitializeWallets) {
        walletScreenContentLoader.load(
            userWallet = action.selectedWallet,
            clickIntents = clickIntents,
            coroutineScope = viewModelScope,
        )

        stateHolder.update(
            transformer = InitializeWalletsTransformer(
                selectedWalletIndex = action.selectedWalletIndex,
                wallets = action.wallets,
                clickIntents = clickIntents,
            ),
        )

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

        walletScreenContentLoader.load(
            userWallet = action.selectedWallet,
            clickIntents = clickIntents,
            coroutineScope = viewModelScope,
        )

        stateHolder.update(
            ReinitializeWalletTransformer(
                prevWalletId = action.prevWalletId,
                newUserWallet = action.selectedWallet,
                clickIntents = clickIntents,
            ),
        )
    }

    private suspend fun addWallet(action: WalletsUpdateActionResolver.Action.AddWallet) {
        walletScreenContentLoader.load(
            userWallet = action.selectedWallet,
            clickIntents = clickIntents,
            coroutineScope = viewModelScope,
        )

        stateHolder.update(
            AddWalletTransformer(
                userWallet = action.selectedWallet,
                clickIntents = clickIntents,
            ),
        )

        withContext(dispatchers.io) { delay(timeMillis = 1000) }

        scrollToWallet(index = action.selectedWalletIndex)
    }

    private suspend fun deleteWallet(action: WalletsUpdateActionResolver.Action.DeleteWallet) {
        walletScreenContentLoader.cancel(action.deletedWalletId)

        walletScreenContentLoader.load(
            userWallet = action.selectedWallet,
            clickIntents = clickIntents,
            coroutineScope = viewModelScope,
        )

        /*
         * Should not show scroll animation if WalletScreen isn't in the background.
         * Example, reset card
         */
        if (screenLifecycleProvider.isBackgroundState.value) {
            updateStateByDeleteWalletTransformer(action)
        } else {
            withContext(dispatchers.io) { delay(timeMillis = 1000) }

            scrollToWallet(
                index = action.selectedWalletIndex,
                onConsume = { updateStateByDeleteWalletTransformer(action) },
            )
        }
    }

    private fun updateStateByDeleteWalletTransformer(action: WalletsUpdateActionResolver.Action.DeleteWallet) {
        stateHolder.update(
            DeleteWalletTransformer(
                selectedWalletIndex = action.selectedWalletIndex,
                deletedWalletId = action.deletedWalletId,
            ),
        )
    }

    private suspend fun unlockWallet(action: WalletsUpdateActionResolver.Action.UnlockWallet) {
        withContext(dispatchers.io) { delay(timeMillis = 700) }

        stateHolder.update(
            transformer = UnlockWalletTransformer(
                unlockedWallets = action.unlockedWallets,
                clickIntents = clickIntents,
            ),
        )

        walletScreenContentLoader.load(
            userWallet = action.selectedWallet,
            clickIntents = clickIntents,
            coroutineScope = viewModelScope,
        )
    }

    private fun scrollToWallet(index: Int, onConsume: () -> Unit = {}) {
        stateHolder.update(
            ScrollToWalletTransformer(
                index = index,
                currentStateProvider = Provider(action = stateHolder::value),
                stateUpdater = { newState -> stateHolder.update { newState } },
                onConsume = onConsume,
            ),
        )
    }

    private companion object {
        const val REFRESH_WALLET_BACKGROUND_TIMER_MILLIS = 10000L
    }
}