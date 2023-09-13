package com.tangem.feature.tokendetails.presentation.tokendetails.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.*
import androidx.paging.cachedIn
import arrow.core.getOrElse
import com.tangem.common.Provider
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.redux.ReduxStateHolder
import com.tangem.domain.tokens.*
import com.tangem.domain.tokens.legacy.TradeCryptoAction
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.models.CryptoCurrency
import com.tangem.domain.txhistory.usecase.GetTxHistoryItemsCountUseCase
import com.tangem.domain.txhistory.usecase.GetTxHistoryItemsUseCase
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.usecase.GetExploreUrlUseCase
import com.tangem.domain.wallets.usecase.GetSelectedWalletUseCase
import com.tangem.feature.tokendetails.presentation.router.InnerTokenDetailsRouter
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.factory.TokenDetailsStateFactory
import com.tangem.features.tokendetails.navigation.TokenDetailsRouter
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.properties.Delegates

@Suppress("LongParameterList")
@HiltViewModel
internal class TokenDetailsViewModel @Inject constructor(
    private val dispatchers: CoroutineDispatcherProvider,
    private val getSelectedWalletUseCase: GetSelectedWalletUseCase,
    private val getCurrencyStatusUpdatesUseCase: GetCurrencyStatusUpdatesUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val fetchCurrencyStatusUseCase: FetchCurrencyStatusUseCase,
    private val txHistoryItemsCountUseCase: GetTxHistoryItemsCountUseCase,
    private val txHistoryItemsUseCase: GetTxHistoryItemsUseCase,
    private val getExploreUrlUseCase: GetExploreUrlUseCase,
    private val getCryptoCurrencyActionsUseCase: GetCryptoCurrencyActionsUseCase,
    private val removeCurrencyUseCase: RemoveCurrencyUseCase,
    private val getNetworkCoinStatusUseCase: GetNetworkCoinStatusUseCase,
    private val reduxStateHolder: ReduxStateHolder,
    savedStateHandle: SavedStateHandle,
) : ViewModel(), DefaultLifecycleObserver, TokenDetailsClickIntents {

    private val cryptoCurrency: CryptoCurrency = savedStateHandle[TokenDetailsRouter.SELECTED_CURRENCY_KEY]
        ?: error("no expected parameter CryptoCurrency found")

    var router by Delegates.notNull<InnerTokenDetailsRouter>()

    private val marketPriceJobHolder = JobHolder()
    private val refreshStateJobHolder = JobHolder()
    private var cryptoCurrencyStatus: CryptoCurrencyStatus? = null
    private var wallet by Delegates.notNull<UserWallet>()

    private val selectedAppCurrencyFlow: StateFlow<AppCurrency> = createSelectedAppCurrencyFlow()
    private val stateFactory = TokenDetailsStateFactory(
        currentStateProvider = Provider { uiState },
        appCurrencyProvider = Provider(selectedAppCurrencyFlow::value),
        clickIntents = this,
        symbol = cryptoCurrency.symbol,
        decimals = cryptoCurrency.decimals,
    )
    var uiState: TokenDetailsState by mutableStateOf(stateFactory.getInitialState(cryptoCurrency))
        private set

    override fun onCreate(owner: LifecycleOwner) {
        getWallet()
        updateContent(selectedWallet = wallet)
    }

    private fun getWallet() {
        return getSelectedWalletUseCase()
            .fold(
                ifLeft = { error("Can not get selected wallet $it") },
                ifRight = { wallet = it },
            )
    }

    private fun updateContent(selectedWallet: UserWallet) {
        updateMarketPrice(selectedWallet = selectedWallet)
        updateButtons(userWalletId = selectedWallet.walletId, currency = cryptoCurrency)
        updateTxHistory()
    }

    private fun updateButtons(userWalletId: UserWalletId, currency: CryptoCurrency) {
        getCryptoCurrencyActionsUseCase(userWalletId = userWalletId, cryptoCurrency = currency)
            .distinctUntilChanged()
            .onEach { uiState = stateFactory.getManageButtonsState(actions = it.states) }
            .flowOn(dispatchers.io)
            .launchIn(viewModelScope)
    }

    private fun updateMarketPrice(selectedWallet: UserWallet) {
        getCurrencyStatusUpdatesUseCase(
            userWalletId = selectedWallet.walletId,
            currencyId = cryptoCurrency.id,
        )
            .distinctUntilChanged()
            .onEach { either ->
                uiState = stateFactory.getCurrencyLoadedBalanceState(either)
                either.onRight { status -> cryptoCurrencyStatus = status }
            }
            .flowOn(dispatchers.io)
            .launchIn(viewModelScope)
            .saveIn(marketPriceJobHolder)
    }

    private fun updateTxHistory(refresh: Boolean = false) {
        viewModelScope.launch(dispatchers.io) {
            val txHistoryItemsCountEither = txHistoryItemsCountUseCase(
                network = cryptoCurrency.network,
            )

            if (!refresh) {
                uiState = stateFactory.getLoadingTxHistoryState(itemsCountEither = txHistoryItemsCountEither)
            }

            txHistoryItemsCountEither.onRight {
                uiState = stateFactory.getLoadedTxHistoryState(
                    txHistoryEither = txHistoryItemsUseCase(
                        network = cryptoCurrency.network,
                    ).map {
                        it.cachedIn(viewModelScope)
                    },
                )
            }
        }
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

    override fun onBackClick() {
        router.popBackStack()
    }

    override fun onBuyClick() {
        val status = cryptoCurrencyStatus ?: return

        reduxStateHolder.dispatch(
            TradeCryptoAction.New.Buy(
                userWallet = wallet,
                cryptoCurrencyStatus = status,
                appCurrencyCode = selectedAppCurrencyFlow.value.code,
            ),
        )
    }

    override fun onReloadClick() {
        updateTxHistory()
    }

    override fun onSendClick() {
        val cryptoCurrencyStatus = cryptoCurrencyStatus ?: return

        when (cryptoCurrencyStatus.currency) {
            is CryptoCurrency.Coin -> {
                reduxStateHolder.dispatch(
                    action = TradeCryptoAction.New.SendCoin(
                        userWallet = wallet,
                        coinStatus = cryptoCurrencyStatus,
                    ),
                )
            }
            is CryptoCurrency.Token -> sendToken(status = cryptoCurrencyStatus)
        }
    }

    private fun sendToken(status: CryptoCurrencyStatus) {
        viewModelScope.launch(dispatchers.io) {
            getNetworkCoinStatusUseCase(
                userWalletId = wallet.walletId,
                networkId = status.currency.network.id,
            )
                .take(count = 1)
                .collectLatest {
                    it.onRight { coinStatus ->
                        reduxStateHolder.dispatch(
                            action = TradeCryptoAction.New.SendToken(
                                userWallet = wallet,
                                tokenStatus = status,
                                coinFiatRate = coinStatus.value.fiatRate,
                            ),
                        )
                    }
                }
        }
    }

    override fun onReceiveClick() {
        // TODO: [REDACTED_JIRA]
    }

    override fun onSellClick() {
        val status = cryptoCurrencyStatus ?: return
        reduxStateHolder.dispatch(
            TradeCryptoAction.New.Sell(
                cryptoCurrencyStatus = status,
                appCurrencyCode = selectedAppCurrencyFlow.value.code,
            ),
        )
    }

    override fun onSwapClick() {
        reduxStateHolder.dispatch(TradeCryptoAction.New.Swap(cryptoCurrency))
    }

    override fun onDismissDialog() {
        uiState = stateFactory.getStateWithClosedDialog()
    }

    override fun onHideClick() {
        viewModelScope.launch {
            val hasLinkedTokens = removeCurrencyUseCase.hasLinkedTokens(wallet.walletId, cryptoCurrency)
            uiState = if (hasLinkedTokens) {
                stateFactory.getStateWithLinkedTokensDialog(cryptoCurrency)
            } else {
                stateFactory.getStateWithConfirmHideTokenDialog(cryptoCurrency)
            }
        }
    }

    override fun onHideConfirmed() {
        viewModelScope.launch {
            removeCurrencyUseCase.invoke(wallet.walletId, cryptoCurrency)
                .onLeft { Timber.e(it) }
                .onRight { router.popBackStack() }
        }
    }

    override fun onExploreClick() {
        viewModelScope.launch {
            router.openUrl(
                url = getExploreUrlUseCase(
                    userWalletId = wallet.walletId,
                    network = cryptoCurrency.network,
                ),
            )
        }
    }

    override fun onRefreshSwipe() {
        uiState = stateFactory.getRefreshingState()

        viewModelScope.launch(dispatchers.io) {
            fetchCurrencyStatusUseCase.invoke(
                userWalletId = wallet.walletId,
                id = cryptoCurrency.id,
                refresh = true,
            )
            updateTxHistory(refresh = true)

            uiState = stateFactory.getRefreshedState()
        }.saveIn(refreshStateJobHolder)
    }
}