package com.tangem.feature.wallet.presentation.wallet.viewmodels

import androidx.compose.runtime.MutableState
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.navigation.AppScreen
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.redux.ReduxStateHolder
import com.tangem.domain.settings.CanUseBiometryUseCase
import com.tangem.domain.settings.IsWalletsScrollPreviewEnabled
import com.tangem.domain.settings.ShouldShowSaveWalletScreenUseCase
import com.tangem.domain.walletconnect.WalletConnectActions
import com.tangem.domain.wallets.usecase.GetSelectedWalletUseCase
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.feature.wallet.presentation.deeplink.WalletDeepLinksHandler
import com.tangem.feature.wallet.presentation.router.InnerWalletRouter
import com.tangem.feature.wallet.presentation.wallet.analytics.WalletScreenAnalyticsEvent
import com.tangem.feature.wallet.presentation.wallet.analytics.utils.SelectedWalletAnalyticsSender
import com.tangem.feature.wallet.presentation.wallet.loaders.WalletScreenContentLoader
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletEvent
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletEvent.DemonstrateWalletsScrollPreview.Direction
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletScreenState
import com.tangem.feature.wallet.presentation.wallet.state.transformers.*
import com.tangem.feature.wallet.presentation.wallet.state.utils.WalletEventSender
import com.tangem.feature.wallet.presentation.wallet.utils.ScreenLifecycleProvider
import com.tangem.feature.wallet.presentation.wallet.viewmodels.intents.WalletClickIntents
import com.tangem.features.managetokens.navigation.ExpandableState
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@Suppress("LongParameterList")
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
    analyticsEventsHandler: AnalyticsEventHandler,
    private val dispatchers: CoroutineDispatcherProvider,
    private val reduxStateHolder: ReduxStateHolder,
    private val screenLifecycleProvider: ScreenLifecycleProvider,
    private val selectedWalletAnalyticsSender: SelectedWalletAnalyticsSender,
    private val walletDeepLinksHandler: WalletDeepLinksHandler,
) : ViewModel() {

    val uiState: StateFlow<WalletScreenState> = stateHolder.uiState

    private lateinit var router: InnerWalletRouter
    private var walletsUpdateJobHolder: JobHolder = JobHolder()

    init {
        analyticsEventsHandler.send(WalletScreenAnalyticsEvent.MainScreen.ScreenOpened)

        suggestToEnableBiometrics()

        subscribeToUserWalletsUpdates()
        subscribeOnBalanceHiding()
        subscribeOnSelectedWalletFlow()
    }

    fun setWalletRouter(router: InnerWalletRouter) {
        this.router = router
        clickIntents.initialize(router, viewModelScope)
    }

    fun setExpandableState(state: MutableState<ExpandableState>) {
        stateHolder.update {
            it.copy(manageTokensExpandableState = state)
        }
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

    private fun subscribeOnSelectedWalletFlow() {
        getSelectedWalletUseCase().onRight {
            it
                .conflate()
                .distinctUntilChanged()
                .onEach { selectedWallet ->
                    if (selectedWallet.isMultiCurrency) {
                        Timber.d("WalletConnect: initialize and setup networks for ${selectedWallet.walletId}")

                        reduxStateHolder.dispatch(
                            action = WalletConnectActions.New.Initialize(userWallet = selectedWallet),
                        )

                        reduxStateHolder.dispatch(
                            action = WalletConnectActions.New.SetupUserChains(userWallet = selectedWallet),
                        )

                        selectedWalletAnalyticsSender.send(selectedWallet)
                    }

                    walletDeepLinksHandler.registerForSingleCurrencyWallets(
                        viewModel = this,
                        userWallet = selectedWallet,
                    )
                }
                .flowOn(dispatchers.main)
                .launchIn(viewModelScope)
        }
    }

    private suspend fun updateWallets(action: WalletsUpdateActionResolver.Action) {
        when (action) {
            is WalletsUpdateActionResolver.Action.InitializeWallets -> initializeWallets(action)
            is WalletsUpdateActionResolver.Action.ReinitializeWallet -> reinitializeWallet(action)
            is WalletsUpdateActionResolver.Action.AddWallet -> addWallet(action)
            is WalletsUpdateActionResolver.Action.DeleteWallet -> deleteWallet(action)
            is WalletsUpdateActionResolver.Action.UnlockWallet -> unlockWallet(action)
            is WalletsUpdateActionResolver.Action.UpdateWalletCardCount -> {
                stateHolder.update(transformer = UpdateWalletCardsCountTransformer(action.selectedWallet))
            }
            is WalletsUpdateActionResolver.Action.UpdateWalletName -> {
                stateHolder.update(transformer = RenameWalletTransformer(action.selectedWalletId, action.name))
            }
            is WalletsUpdateActionResolver.Action.NoAccessibleWallets -> closeScreen(screen = AppScreen.Welcome)
            is WalletsUpdateActionResolver.Action.NoWallets -> closeScreen(screen = AppScreen.Home)
            is WalletsUpdateActionResolver.Action.Unknown -> Unit
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

        withContext(dispatchers.io) { delay(timeMillis = 700) }

        scrollToWallet(index = action.selectedWalletIndex)
    }

    private suspend fun deleteWallet(action: WalletsUpdateActionResolver.Action.DeleteWallet) {
        walletScreenContentLoader.load(
            userWallet = action.selectedWallet,
            clickIntents = clickIntents,
            coroutineScope = viewModelScope,
        )

        /*
         * If card is reset to factory settings, then Compose need some time to draw the WalletScreen.
         * Otherwise, scroll isn't happened
         */
        withContext(dispatchers.io) { delay(timeMillis = 1000) }

        scrollToWallet(
            index = action.selectedWalletIndex,
            onConsume = {
                stateHolder.update(
                    DeleteWalletTransformer(
                        selectedWalletIndex = action.selectedWalletIndex,
                        deletedWalletId = action.deletedWalletId,
                    ),
                )
            },
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

    private fun closeScreen(screen: AppScreen) {
        if (!screenLifecycleProvider.isBackground) {
            stateHolder.clear()
            router.popBackStack(screen = screen)
        }
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
}
