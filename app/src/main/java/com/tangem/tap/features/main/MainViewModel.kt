package com.tangem.tap.features.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangem.blockchainsdk.BlockchainSDKFactory
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.core.analytics.Analytics
import com.tangem.core.analytics.models.Basic
import com.tangem.domain.appcurrency.FetchAppCurrenciesUseCase
import com.tangem.domain.balancehiding.BalanceHidingSettings
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.balancehiding.ListenToFlipsUseCase
import com.tangem.domain.balancehiding.UpdateBalanceHidingSettingsUseCase
import com.tangem.domain.feedback.FeedbackManagerFeatureToggles
import com.tangem.domain.settings.DeleteDeprecatedLogsUseCase
import com.tangem.domain.settings.IncrementAppLaunchCounterUseCase
import com.tangem.domain.staking.FetchStakingTokensUseCase
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.features.send.api.featuretoggles.SendFeatureToggles
import com.tangem.features.staking.api.featuretoggles.StakingFeatureToggles
import com.tangem.tap.common.extensions.setContext
import com.tangem.tap.features.main.model.MainScreenState
import com.tangem.tap.store
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@Suppress("LongParameterList")
@HiltViewModel
internal class MainViewModel @Inject constructor(
    private val updateBalanceHidingSettingsUseCase: UpdateBalanceHidingSettingsUseCase,
    private val listenToFlipsUseCase: ListenToFlipsUseCase,
    private val router: AppRouter,
    private val fetchAppCurrenciesUseCase: FetchAppCurrenciesUseCase,
    private val deleteDeprecatedLogsUseCase: DeleteDeprecatedLogsUseCase,
    private val incrementAppLaunchCounterUseCase: IncrementAppLaunchCounterUseCase,
    private val blockchainSDKFactory: BlockchainSDKFactory,
    private val userWalletsListManager: UserWalletsListManager,
    private val walletManagersFacade: WalletManagersFacade,
    private val sendFeatureToggles: SendFeatureToggles,
    private val feedbackManagerFeatureToggles: FeedbackManagerFeatureToggles,
    private val dispatchers: CoroutineDispatcherProvider,
    private val stakingFeatureToggles: StakingFeatureToggles,
    private val fetchStakingTokensUseCase: FetchStakingTokensUseCase,
    getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
) : ViewModel(), MainIntents {

    private val stateHolder = MainScreenStateHolder(
        intents = this,
    )

    private val balanceHidingSettingsFlow: SharedFlow<BalanceHidingSettings> = getBalanceHidingSettingsUseCase()
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed())

    val state: StateFlow<MainScreenState> = stateHolder.stateFlow

    var isSplashScreenShown: Boolean = true
        private set

    init {
        loadApplicationResources()

        viewModelScope.launch(dispatchers.main) { incrementAppLaunchCounterUseCase() }

        updateAppCurrencies()
        updateSendFeatureToggle()
        observeFlips()
        displayBalancesHidingStatusToast()
        displayHiddenBalancesModalNotification()

        if (stakingFeatureToggles.isStakingEnabled) {
            fetchStakingTokens()
        }

        viewModelScope.launch(dispatchers.main) {
            deleteDeprecatedLogsUseCase()
        }
    }

    /** Loading the resources needed to run the application */
    private fun loadApplicationResources() {
        viewModelScope.launch(dispatchers.main) {
            blockchainSDKFactory.init()
            prepareSelectedWalletFeedback()

            isSplashScreenShown = false
        }
    }

    private fun prepareSelectedWalletFeedback() {
        userWalletsListManager.selectedUserWallet
            .distinctUntilChanged()
            .onEach { userWallet ->
                Analytics.setContext(userWallet.scanResponse)
                Analytics.send(Basic.WalletOpened())

                if (!feedbackManagerFeatureToggles.isLocalLogsEnabled) {
                    store.state.globalState.feedbackManager?.infoHolder?.let { infoHolder ->
                        infoHolder.setCardInfo(userWallet.scanResponse)

                        walletManagersFacade
                            .getAll(userWallet.walletId)
                            .distinctUntilChanged()
                            .onEach(infoHolder::setWalletsInfo)
                            .catch { Timber.e(it) }
                            .launchIn(viewModelScope)
                    }
                }
            }
            .flowOn(dispatchers.io)
            .launchIn(viewModelScope)
    }

    private fun updateAppCurrencies() {
        viewModelScope.launch(dispatchers.main) {
            fetchAppCurrenciesUseCase.invoke()
        }
    }

    private fun fetchStakingTokens() {
        viewModelScope.launch(dispatchers.main) {
            fetchStakingTokensUseCase()
                .onLeft { Timber.e(it.toString(), "Unable to fetch the staking tokens list") }
                .onRight { Timber.d("Staking token list was fetched successfully") }
        }
    }

    private fun updateSendFeatureToggle() {
        viewModelScope.launch(dispatchers.main) {
            sendFeatureToggles.fetchNewSendEnabled()
        }
    }

    private fun observeFlips() {
        listenToFlipsUseCase().launchIn(viewModelScope)
    }

    private fun displayHiddenBalancesModalNotification() {
        balanceHidingSettingsFlow
            .drop(count = 1) // Skip initial emit
            .distinctUntilChanged()
            .filter {
                it.isBalanceHidingNotificationEnabled && it.isBalanceHidden
            }
            .onEach {
                if (state.value.modalNotification?.isShow != true && !it.isUpdateFromToast) {
                    listenToFlipsUseCase.changeUpdateEnabled(false)
                    stateHolder.updateWithHiddenBalancesNotification()
                    router.push(AppRoute.ModalNotification)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun displayBalancesHidingStatusToast() {
        var previousSettings: BalanceHidingSettings? = null

        balanceHidingSettingsFlow
            .onEach { settings ->
                if (previousSettings == null) {
                    previousSettings = settings
                    return@onEach
                }

                /*
                 * Skip if the feature to hide balances has just been
                 * enabled or disabled in the settings, or if the hide balances status has not been changed
                 * */
                if (settings.isHidingEnabledInSettings != previousSettings?.isHidingEnabledInSettings ||
                    settings.isBalanceHidden == previousSettings?.isBalanceHidden
                ) {
                    previousSettings = settings
                    return@onEach
                }

                if (!settings.isUpdateFromToast) {
                    displayBalancesHiddenStatusToast(settings)
                }

                previousSettings = settings
            }
            .launchIn(viewModelScope)
    }

    private fun displayBalancesHiddenStatusToast(settings: BalanceHidingSettings) {
        // If modal notification is enabled and balances are hidden, the toast will not show
        if (!settings.isBalanceHidingNotificationEnabled || !settings.isBalanceHidden) {
            stateHolder.updateWithHiddenBalancesToast(settings.isBalanceHidden)
        }
    }

    override fun onHiddenBalanceToastAction() {
        viewModelScope.launch {
            updateBalanceHidingSettingsUseCase.invoke {
                copy(
                    isBalanceHidden = false,
                    isUpdateFromToast = true,
                )
            }
        }
    }

    override fun onShownBalanceToastAction() {
        viewModelScope.launch {
            updateBalanceHidingSettingsUseCase.invoke {
                copy(
                    isBalanceHidden = true,
                    isUpdateFromToast = true,
                )
            }
        }
    }

    override fun onHiddenBalanceNotificationAction(isPermanent: Boolean) {
        onDismissBottomSheet()

        stateHolder.updateWithHiddenBalancesToast(true)
        if (isPermanent) {
            viewModelScope.launch {
                updateBalanceHidingSettingsUseCase.invoke {
                    copy(isBalanceHidingNotificationEnabled = false)
                }
            }
        }
    }

    override fun onDismissBottomSheet() {
        listenToFlipsUseCase.changeUpdateEnabled(true)
        stateHolder.updateWithoutModalNotification()
        stateHolder.updateWithHiddenBalancesToast(true)
    }
}
