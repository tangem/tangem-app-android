package com.tangem.tap.features.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangem.blockchainsdk.BlockchainSDKFactory
import com.tangem.core.analytics.Analytics
import com.tangem.core.decompose.di.GlobalUiMessageSender
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.BottomSheetMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.datasource.api.common.config.managers.ApiConfigsManager
import com.tangem.domain.appcurrency.FetchAppCurrenciesUseCase
import com.tangem.domain.balancehiding.BalanceHidingSettings
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.balancehiding.ListenToFlipsUseCase
import com.tangem.domain.balancehiding.UpdateBalanceHidingSettingsUseCase
import com.tangem.domain.settings.DeleteDeprecatedLogsUseCase
import com.tangem.domain.settings.IncrementAppLaunchCounterUseCase
import com.tangem.domain.settings.usercountry.FetchUserCountryUseCase
import com.tangem.domain.staking.FetchStakingTokensUseCase
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.tap.common.extensions.setContext
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
    private val fetchAppCurrenciesUseCase: FetchAppCurrenciesUseCase,
    deleteDeprecatedLogsUseCase: DeleteDeprecatedLogsUseCase,
    private val incrementAppLaunchCounterUseCase: IncrementAppLaunchCounterUseCase,
    private val blockchainSDKFactory: BlockchainSDKFactory,
    private val userWalletsListManager: UserWalletsListManager,
    private val dispatchers: CoroutineDispatcherProvider,
    private val fetchStakingTokensUseCase: FetchStakingTokensUseCase,
    private val apiConfigsManager: ApiConfigsManager,
    private val fetchUserCountryUseCase: FetchUserCountryUseCase,
    @GlobalUiMessageSender
    private val messageSender: UiMessageSender,
    getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
) : ViewModel() {

    private val balanceHidingSettingsFlow: SharedFlow<BalanceHidingSettings> = getBalanceHidingSettingsUseCase()
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed())

    var isSplashScreenShown: Boolean = true
        private set

    init {
        loadApplicationResources()

        viewModelScope.launch(dispatchers.main) { incrementAppLaunchCounterUseCase() }

        viewModelScope.launch {
            fetchUserCountryUseCase().onLeft {
                Timber.e("Unable to fetch the user country code $it")
            }
        }

        updateAppCurrencies()
        observeFlips()
        displayBalancesHidingStatusToast()
        displayHiddenBalancesModalNotification()

        fetchStakingTokens()

        deleteDeprecatedLogsUseCase()
    }

    /** Loading the resources needed to run the application */
    private fun loadApplicationResources() {
        viewModelScope.launch(dispatchers.main) {
            apiConfigsManager.initialize()

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
            fetchStakingTokensUseCase(true)
                .onLeft { Timber.e(it.toString(), "Unable to fetch the staking tokens list") }
                .onRight { Timber.d("Staking token list was fetched successfully") }
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
                if (!it.isUpdateFromToast) {
                    listenToFlipsUseCase.changeUpdateEnabled(false)

                    val message = BottomSheetMessage.invoke(
                        iconResId = R.drawable.ic_eye_off_outline_24,
                        title = resourceReference(R.string.balance_hidden_title),
                        message = resourceReference(R.string.balance_hidden_description),
                        onDismissRequest = ::onBottomSheetDismissed,
                        firstActionBuilder = {
                            EventMessageAction(
                                title = resourceReference(R.string.balance_hidden_got_it_button),
                                onClick = {
                                    onHiddenBalanceNotificationAction(isPermanent = false)
                                    onDismissRequest()
                                },
                            )
                        },
                        secondActionBuilder = {
                            EventMessageAction(
                                title = resourceReference(R.string.balance_hidden_do_not_show_button),
                                onClick = {
                                    onHiddenBalanceNotificationAction(isPermanent = true)
                                    onDismissRequest()
                                },
                            )
                        },
                    )
                    messageSender.send(message)
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
            val message = SnackbarMessage(
                message = if (settings.isBalanceHidden) {
                    resourceReference(R.string.toast_balances_hidden)
                } else {
                    resourceReference(R.string.toast_balances_shown)
                },
                actionLabel = resourceReference(R.string.toast_undo),
                action = {
                    if (settings.isBalanceHidden) {
                        onHiddenBalanceToastAction()
                    } else {
                        onShownBalanceToastAction()
                    }
                },
            )

            messageSender.send(message)
        }
    }

    private fun onHiddenBalanceToastAction() {
        viewModelScope.launch {
            updateBalanceHidingSettingsUseCase.invoke {
                copy(
                    isBalanceHidden = false,
                    isUpdateFromToast = true,
                )
            }
        }
    }

    private fun onShownBalanceToastAction() {
        viewModelScope.launch {
            updateBalanceHidingSettingsUseCase.invoke {
                copy(
                    isBalanceHidden = true,
                    isUpdateFromToast = true,
                )
            }
        }
    }

    private fun onHiddenBalanceNotificationAction(isPermanent: Boolean) {
        val message = SnackbarMessage(
            message = resourceReference(R.string.toast_balances_hidden),
            actionLabel = resourceReference(R.string.toast_undo),
            action = ::onHiddenBalanceToastAction,
        )
        messageSender.send(message)

        if (isPermanent) {
            viewModelScope.launch {
                updateBalanceHidingSettingsUseCase.invoke {
                    copy(isBalanceHidingNotificationEnabled = false)
                }
            }
        }
    }

    private fun onBottomSheetDismissed() {
        listenToFlipsUseCase.changeUpdateEnabled(isUpdateEnabled = true)
    }
}