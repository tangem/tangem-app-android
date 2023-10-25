package com.tangem.features.send.impl.presentation.send.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.*
import arrow.core.getOrElse
import com.tangem.common.Provider
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.GetCurrencyStatusUpdatesUseCase
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.send.api.navigation.SendRouter
import com.tangem.features.send.impl.presentation.send.state.SendStateFactory
import com.tangem.features.send.impl.presentation.send.state.SendUiState
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class SendViewModel @Inject constructor(
    private val dispatchers: CoroutineDispatcherProvider,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val getCurrencyStatusUpdatesUseCase: GetCurrencyStatusUpdatesUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel(), DefaultLifecycleObserver, SendClickIntents {

    private val userWalletId: UserWalletId = savedStateHandle.get<String>(SendRouter.USER_WALLET_ID_KEY)
        ?.let { stringValue -> UserWalletId(stringValue) }
        ?: error("This screen can't open without `UserWalletId`")

    private val cryptoCurrency: CryptoCurrency = savedStateHandle[SendRouter.CRYPTO_CURRENCY_KEY]
        ?: error("This screen can't open without `CryptoCurrency`")

    private val selectedAppCurrencyFlow: StateFlow<AppCurrency> = createSelectedAppCurrencyFlow()

    private val stateFactory = SendStateFactory(
        clickIntents = this,
        currentStateProvider = Provider { uiState },
        userWalletProvider = Provider { userWallet },
        appCurrencyProvider = Provider(selectedAppCurrencyFlow::value),
    )

    var uiState: SendUiState by mutableStateOf(stateFactory.getInitialState())
        private set

    private var userWallet: UserWallet? = null

    private var balanceJobHolder = JobHolder()

    override fun onCreate(owner: LifecycleOwner) {
        subscribeOnCurrencyStatusUpdates(owner)
    }

    private fun subscribeOnCurrencyStatusUpdates(owner: LifecycleOwner) {
        viewModelScope.launch(dispatchers.io) {
            getUserWalletUseCase(userWalletId).fold(
                ifRight = { wallet ->
                    userWallet = wallet
                    getCurrencyStatusUpdates(owner, wallet)
                },
                ifLeft = {
                    // TODO add error handling
                    return@launch
                },
            )
        }
    }

    private fun getCurrencyStatusUpdates(owner: LifecycleOwner, wallet: UserWallet) {
        val isSingleWallet = wallet.scanResponse.walletData?.token != null && !wallet.isMultiCurrency
        getCurrencyStatusUpdatesUseCase(
            userWalletId = userWalletId,
            currencyId = cryptoCurrency.id,
            derivationPath = cryptoCurrency.network.derivationPath,
            isSingleWalletWithTokens = isSingleWallet,
        )
            .flowWithLifecycle(owner.lifecycle)
            .conflate()
            .distinctUntilChanged()
            .onEach { either ->
                uiState = stateFactory.getAmountState(
                    cryptoCurrencyStatus = either,
                )
            }
            .flowOn(dispatchers.io)
            .launchIn(viewModelScope)
            .saveIn(balanceJobHolder)
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

    // region screen state navigation
    override fun onNextClick() {
        when (uiState) {
            is SendUiState.Content.AmountState -> onRecipientStateClick()
            is SendUiState.Content.RecipientState -> onFeeStateClick()
            else -> {
                // todo implement
            }
        }
    }

    override fun onPrevClick() {
        // todo implement
    }

    private fun onRecipientStateClick() {
        stateFactory.getOnReceiveState()
    }

    private fun onFeeStateClick() {
        // todo implement
    }
    // endregion

    // region amount state clicks
    override fun onCurrencyChangeClick(isFiat: Boolean) {
        uiState = stateFactory.getOnCurrencyChangedState(isFiat)
    }

    override fun onAmountValueChange(value: String) {
        uiState = stateFactory.getOnAmountValueChange(value)
    }

    override fun onMaxValueClick() {
        val amountState = uiState as? SendUiState.Content.AmountState ?: return

        val amount = if (amountState.isFiatValue) {
            amountState.cryptoCurrencyStatus.value.fiatAmount
        } else {
            amountState.cryptoCurrencyStatus.value.amount
        }
        onAmountValueChange(amount?.toPlainString() ?: "0.00")
    }
    // endregion
}