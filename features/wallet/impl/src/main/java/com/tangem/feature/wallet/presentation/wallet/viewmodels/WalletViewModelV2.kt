package com.tangem.feature.wallet.presentation.wallet.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.getOrElse
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.redux.ReduxStateHolder
import com.tangem.domain.settings.CanUseBiometryUseCase
import com.tangem.domain.settings.IsWalletsScrollPreviewEnabled
import com.tangem.domain.settings.ShouldShowSaveWalletScreenUseCase
import com.tangem.domain.walletconnect.WalletConnectActions
import com.tangem.domain.wallets.usecase.GetSelectedWalletUseCase
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.domain.wallets.usecase.ShouldSaveUserWalletsUseCase
import com.tangem.feature.wallet.presentation.router.InnerWalletRouter
import com.tangem.feature.wallet.presentation.wallet.analytics.WalletScreenAnalyticsEvent
import com.tangem.feature.wallet.presentation.wallet.loaders.WalletScreenContentLoader
import com.tangem.feature.wallet.presentation.wallet.state.WalletEvent
import com.tangem.feature.wallet.presentation.wallet.state.WalletEvent.DemonstrateWalletsScrollPreview.Direction
import com.tangem.feature.wallet.presentation.wallet.state2.WalletScreenState
import com.tangem.feature.wallet.presentation.wallet.state2.WalletStateHolderV2
import com.tangem.feature.wallet.presentation.wallet.state2.transformers.*
import com.tangem.feature.wallet.presentation.wallet.state2.utils.WalletEventSender
import com.tangem.feature.wallet.presentation.wallet.viewmodels.intents.WalletClickIntentsV2
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.properties.Delegates

@Suppress("LongParameterList")
@HiltViewModel
internal class WalletViewModelV2 @Inject constructor(
    private val stateHolder: WalletStateHolderV2,
    private val clickIntents: WalletClickIntentsV2,
    private val walletEventSender: WalletEventSender,
    private val walletsUpdateActionResolver: WalletsUpdateActionResolverV2,
    private val walletScreenContentLoader: WalletScreenContentLoader,
    private val getSelectedWalletUseCase: GetSelectedWalletUseCase,
    private val getWalletsUseCase: GetWalletsUseCase,
    private val shouldShowSaveWalletScreenUseCase: ShouldShowSaveWalletScreenUseCase,
    private val canUseBiometryUseCase: CanUseBiometryUseCase,
    private val shouldSaveUserWalletsUseCase: ShouldSaveUserWalletsUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val isWalletsScrollPreviewEnabled: IsWalletsScrollPreviewEnabled,
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    analyticsEventsHandler: AnalyticsEventHandler,
    private val dispatchers: CoroutineDispatcherProvider,
    private val reduxStateHolder: ReduxStateHolder,
) : ViewModel() {

    val uiState: StateFlow<WalletScreenState> = stateHolder.uiState

    private val selectedAppCurrencyFlow = createSelectedAppCurrencyFlow()

    private var router: InnerWalletRouter by Delegates.notNull()
    private var walletsUpdateJobHolder: JobHolder = JobHolder()

    init {
        analyticsEventsHandler.send(WalletScreenAnalyticsEvent.MainScreen.ScreenOpened)

        suggestToEnableBiometrics()

        subscribeOnWalletsUpdateFlow()
        subscribeOnBalanceHiding()
        subscribeOnSelectedWalletFlow()
    }

    fun setWalletRouter(router: InnerWalletRouter) {
        this.router = router
        clickIntents.initialize(router, viewModelScope)
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

    private fun subscribeOnWalletsUpdateFlow() {
        viewModelScope.launch(dispatchers.main) {
            shouldSaveUserWalletsUseCase()
                .conflate()
                .distinctUntilChanged()
                .collectLatest { shouldSaveUserWallet ->
                    getWalletsUseCase()
                        .distinctUntilChanged()
                        .conflate()
                        .map {
                            walletsUpdateActionResolver.resolve(
                                wallets = it,
                                currentState = stateHolder.value,
                                canSaveWallets = shouldSaveUserWallet,
                            )
                        }
                        .onEach(::updateWallets)
                        .flowOn(dispatchers.main)
                        .launchIn(viewModelScope)
                        .saveIn(walletsUpdateJobHolder)
                }
        }
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
                        reduxStateHolder.dispatch(
                            action = WalletConnectActions.New.Initialize(userWallet = selectedWallet),
                        )
                    }
                }
                .flowOn(dispatchers.main)
                .launchIn(viewModelScope)
        }
    }

    private fun updateWallets(action: WalletsUpdateActionResolverV2.Action) {
        when (action) {
            is WalletsUpdateActionResolverV2.Action.InitializeWallets -> initializeWallets(action)
            is WalletsUpdateActionResolverV2.Action.ReinitializeWallets -> {
                walletScreenContentLoader.load(
                    userWallet = action.selectedWallet,
                    appCurrency = selectedAppCurrencyFlow.value,
                    clickIntents = clickIntents,
                    coroutineScope = viewModelScope,
                    isRefresh = true,
                )
            }
            is WalletsUpdateActionResolverV2.Action.ReinitializeWallet -> reinitializeWallet(action)
            is WalletsUpdateActionResolverV2.Action.AddWallet -> addWallet(action)
            is WalletsUpdateActionResolverV2.Action.DeleteWallet -> deleteWallet(action)
            is WalletsUpdateActionResolverV2.Action.UnlockWallet -> unlockWallet(action)
            is WalletsUpdateActionResolverV2.Action.UpdateWalletCardCount -> {
                stateHolder.update(transformer = UpdateWalletCardsCountTransformer(action.selectedWallet))
            }
            is WalletsUpdateActionResolverV2.Action.UpdateWalletName -> {
                stateHolder.update(transformer = RenameWalletTransformer(action.selectedWalletId, action.name))
            }
            is WalletsUpdateActionResolverV2.Action.Unknown -> Unit
        }
    }

    private fun initializeWallets(action: WalletsUpdateActionResolverV2.Action.InitializeWallets) {
        walletScreenContentLoader.load(
            userWallet = action.selectedWallet,
            appCurrency = selectedAppCurrencyFlow.value,
            clickIntents = clickIntents,
            coroutineScope = viewModelScope,
        )

        stateHolder.update(
            transformer = InitializeWalletsTransformer(
                selectedWalletIndex = action.selectedWalletIndex,
                selectedWallet = action.selectedWallet,
                wallets = action.wallets,
                clickIntents = clickIntents,
            ),
        )

        viewModelScope.launch(dispatchers.main) {
            if (action.wallets.size > 1 && isWalletsScrollPreviewEnabled()) {
                withContext(dispatchers.io) {
                    delay(timeMillis = 1_800)
                }

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
    }

    private fun reinitializeWallet(action: WalletsUpdateActionResolverV2.Action.ReinitializeWallet) {
        viewModelScope.launch(dispatchers.main) {
            walletScreenContentLoader.cancel(action.prevWalletId)

            walletScreenContentLoader.load(
                userWallet = action.selectedWallet,
                appCurrency = selectedAppCurrencyFlow.value,
                clickIntents = clickIntents,
                coroutineScope = viewModelScope,
            )

            stateHolder.update(
                ReinitializeWalletTransformer(userWallet = action.selectedWallet, clickIntents = clickIntents),
            )
        }
    }

    private fun addWallet(action: WalletsUpdateActionResolverV2.Action.AddWallet) {
        viewModelScope.launch(dispatchers.main) {
            stateHolder.update(
                AddWalletTransformer(
                    userWallet = action.selectedWallet,
                    clickIntents = clickIntents,
                ),
            )

            walletScreenContentLoader.load(
                userWallet = action.selectedWallet,
                appCurrency = selectedAppCurrencyFlow.value,
                clickIntents = clickIntents,
                coroutineScope = viewModelScope,
            )

            withContext(dispatchers.io) { delay(timeMillis = 700) }

            scrollToWallet(index = action.selectedWalletIndex)
        }
    }

    private fun deleteWallet(action: WalletsUpdateActionResolverV2.Action.DeleteWallet) {
        viewModelScope.launch(dispatchers.main) {
            walletScreenContentLoader.load(
                userWallet = action.selectedWallet,
                appCurrency = selectedAppCurrencyFlow.value,
                clickIntents = clickIntents,
                coroutineScope = viewModelScope,
            )

            scrollToWallet(index = action.selectedWalletIndex)

            withContext(dispatchers.io) { delay(timeMillis = 700) }

            stateHolder.update(
                DeleteWalletTransformer(
                    selectedWalletIndex = action.selectedWalletIndex,
                    deletedWalletId = action.deletedWalletId,
                ),
            )
        }
    }

    private fun unlockWallet(action: WalletsUpdateActionResolverV2.Action.UnlockWallet) {
        viewModelScope.launch(dispatchers.main) {
            withContext(dispatchers.io) { delay(timeMillis = 700) }

            stateHolder.update(
                transformer = UnlockWalletTransformer(
                    unlockedWallets = action.unlockedWallets,
                    clickIntents = clickIntents,
                ),
            )

            walletScreenContentLoader.load(
                userWallet = action.selectedWallet,
                appCurrency = selectedAppCurrencyFlow.value,
                clickIntents = clickIntents,
                coroutineScope = viewModelScope,
            )
        }
    }

    private fun scrollToWallet(index: Int) {
        stateHolder.update(
            ScrollToWalletTransformer(
                index = index,
                currentStateProvider = Provider(action = stateHolder::value),
                stateUpdater = { newState -> stateHolder.update { newState } },
            ),
        )
    }

    private fun createSelectedAppCurrencyFlow(): StateFlow<AppCurrency> {
        return getSelectedAppCurrencyUseCase()
            .map { maybeAppCurrency ->
                maybeAppCurrency.getOrElse { AppCurrency.Default }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = AppCurrency.Default,
            )
    }
}