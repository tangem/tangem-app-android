package com.tangem.feature.tokendetails.presentation.tokendetails.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.*
import androidx.paging.cachedIn
import arrow.core.getOrElse
import com.tangem.blockchain.common.address.AddressType
import com.tangem.common.Provider
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.ui.components.transactions.state.TxHistoryState
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.IsBalanceHiddenUseCase
import com.tangem.domain.balancehiding.ListenToFlipsUseCase
import com.tangem.domain.redux.ReduxStateHolder
import com.tangem.domain.tokens.*
import com.tangem.domain.tokens.legacy.TradeCryptoAction
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.models.analytics.TokenReceiveAnalyticsEvent
import com.tangem.domain.txhistory.usecase.GetTxHistoryItemsCountUseCase
import com.tangem.domain.txhistory.usecase.GetTxHistoryItemsUseCase
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.usecase.GetExploreUrlUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.feature.tokendetails.presentation.router.InnerTokenDetailsRouter
import com.tangem.feature.tokendetails.presentation.tokendetails.analytics.TokenScreenEvent
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.factory.TokenDetailsStateFactory
import com.tangem.features.tokendetails.navigation.TokenDetailsRouter
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.properties.Delegates

@Suppress("LongParameterList", "LargeClass")
@HiltViewModel
internal class TokenDetailsViewModel @Inject constructor(
    private val dispatchers: CoroutineDispatcherProvider,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val getCurrencyStatusUpdatesUseCase: GetCurrencyStatusUpdatesUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val fetchCurrencyStatusUseCase: FetchCurrencyStatusUseCase,
    private val txHistoryItemsCountUseCase: GetTxHistoryItemsCountUseCase,
    private val txHistoryItemsUseCase: GetTxHistoryItemsUseCase,
    private val getExploreUrlUseCase: GetExploreUrlUseCase,
    private val getCryptoCurrencyActionsUseCase: GetCryptoCurrencyActionsUseCase,
    private val removeCurrencyUseCase: RemoveCurrencyUseCase,
    private val getNetworkCoinStatusUseCase: GetNetworkCoinStatusUseCase,
    private val isBalanceHiddenUseCase: IsBalanceHiddenUseCase,
    private val listenToFlipsUseCase: ListenToFlipsUseCase,
    private val getCurrencyWarningsUseCase: GetCurrencyWarningsUseCase,
    private val walletManagersFacade: WalletManagersFacade,
    private val reduxStateHolder: ReduxStateHolder,
    private val analyticsEventsHandler: AnalyticsEventHandler,
    savedStateHandle: SavedStateHandle,
) : ViewModel(), DefaultLifecycleObserver, TokenDetailsClickIntents {

    private val userWalletId: UserWalletId = savedStateHandle.get<String>(TokenDetailsRouter.USER_WALLET_ID_KEY)
        ?.let { stringValue -> UserWalletId(stringValue) }
        ?: error("This screen can't open without `UserWalletId`")

    private val cryptoCurrency: CryptoCurrency = savedStateHandle[TokenDetailsRouter.CRYPTO_CURRENCY_KEY]
        ?: error("This screen can't open without `CryptoCurrency`")

    var router by Delegates.notNull<InnerTokenDetailsRouter>()

    private val marketPriceJobHolder = JobHolder()
    private val refreshStateJobHolder = JobHolder()
    private var cryptoCurrencyStatus: CryptoCurrencyStatus? = null

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
        updateContent()
        handleBalanceHiding(owner)
    }

    private fun updateContent() {
        updateMarketPrice()
        updateTxHistory(refresh = false, showItemsLoading = true)
        updateWarnings()
    }

    private fun handleBalanceHiding(owner: LifecycleOwner) {
        isBalanceHiddenUseCase()
            .flowWithLifecycle(owner.lifecycle)
            .onEach { hidden ->
                uiState = stateFactory.getStateWithUpdatedHidden(
                    isBalanceHidden = hidden,
                )
            }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            listenToFlipsUseCase()
                .flowWithLifecycle(owner.lifecycle)
                .collect()
        }
    }

    private fun updateButtons(userWalletId: UserWalletId, currencyStatus: CryptoCurrencyStatus) {
        getCryptoCurrencyActionsUseCase(userWalletId = userWalletId, cryptoCurrencyStatus = currencyStatus)
            .distinctUntilChanged()
            .onEach { uiState = stateFactory.getManageButtonsState(actions = it.states) }
            .flowOn(dispatchers.io)
            .launchIn(viewModelScope)
    }

    private fun updateWarnings() {
        viewModelScope.launch(dispatchers.io) {
            getCurrencyWarningsUseCase.invoke(
                userWalletId = userWalletId,
                currency = cryptoCurrency,
                derivationPath = cryptoCurrency.network.derivationPath,
            )
                .distinctUntilChanged()
                .onEach { uiState = stateFactory.getStateWithNotifications(it) }
                .launchIn(viewModelScope)
        }
    }

    private fun updateMarketPrice() {
        getCurrencyStatusUpdatesUseCase(
            userWalletId = userWalletId,
            currencyId = cryptoCurrency.id,
            derivationPath = cryptoCurrency.network.derivationPath,
        )
            .distinctUntilChanged()
            .onEach { either ->
                uiState = stateFactory.getCurrencyLoadedBalanceState(either)
                either.onRight { status ->
                    cryptoCurrencyStatus = status
                    updateButtons(userWalletId = userWalletId, currencyStatus = status)
                }
            }
            .flowOn(dispatchers.io)
            .launchIn(viewModelScope)
            .saveIn(marketPriceJobHolder)
    }

    /**
     * @param refresh - invalidate cache and get data from remote
     * @param showItemsLoading - show loading items placeholder.
     */
    @Suppress("UnusedPrivateMember") // will be removed after implement caching
    private fun updateTxHistory(refresh: Boolean, showItemsLoading: Boolean) {
        viewModelScope.launch(dispatchers.io) {
            val txHistoryItemsCountEither = txHistoryItemsCountUseCase(
                userWalletId = userWalletId,
                network = cryptoCurrency.network,
            )

            // if countEither is left, handling error state run inside getLoadingTxHistoryState
            if (showItemsLoading || txHistoryItemsCountEither.isLeft()) {
                uiState = stateFactory.getLoadingTxHistoryState(itemsCountEither = txHistoryItemsCountEither)
            }

            txHistoryItemsCountEither.onRight {
                val maybeTxHistory = txHistoryItemsUseCase(
                    userWalletId = userWalletId,
                    currency = cryptoCurrency,
                    refresh = refresh,
                ).map { it.cachedIn(viewModelScope) }

                uiState = stateFactory.getLoadedTxHistoryState(maybeTxHistory)
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
        analyticsEventsHandler.send(TokenScreenEvent.ButtonBuy(cryptoCurrency.symbol))
        val status = cryptoCurrencyStatus ?: return

        viewModelScope.launch(dispatchers.io) {
            reduxStateHolder.dispatch(
                TradeCryptoAction.New.Buy(
                    userWallet = getUserWalletUseCase(userWalletId).getOrElse { return@launch },
                    cryptoCurrencyStatus = status,
                    appCurrencyCode = selectedAppCurrencyFlow.value.code,
                ),
            )
        }
    }

    override fun onReloadClick() {
        analyticsEventsHandler.send(TokenScreenEvent.ButtonReload(cryptoCurrency.symbol))
        uiState = stateFactory.getLoadingTxHistoryState()
        updateTxHistory(refresh = true, showItemsLoading = true)
    }

    override fun onSendClick() {
        analyticsEventsHandler.send(TokenScreenEvent.ButtonSend(cryptoCurrency.symbol))

        val cryptoCurrencyStatus = cryptoCurrencyStatus ?: return

        viewModelScope.launch(dispatchers.io) {
            when (cryptoCurrencyStatus.currency) {
                is CryptoCurrency.Coin -> {
                    reduxStateHolder.dispatch(
                        action = TradeCryptoAction.New.SendCoin(
                            userWallet = getUserWalletUseCase(userWalletId).getOrElse { return@launch },
                            coinStatus = cryptoCurrencyStatus,
                        ),
                    )
                }
                is CryptoCurrency.Token -> sendToken(status = cryptoCurrencyStatus)
            }
        }
    }

    private fun sendToken(status: CryptoCurrencyStatus) {
        viewModelScope.launch(dispatchers.io) {
            val maybeCoinStatus = getNetworkCoinStatusUseCase(
                userWalletId = userWalletId,
                networkId = status.currency.network.id,
                derivationPath = status.currency.network.derivationPath,
            ).firstOrNull()

            maybeCoinStatus?.onRight { coinStatus ->
                reduxStateHolder.dispatch(
                    action = TradeCryptoAction.New.SendToken(
                        userWallet = getUserWalletUseCase(userWalletId).getOrElse { return@launch },
                        tokenStatus = status,
                        coinFiatRate = coinStatus.value.fiatRate,
                    ),
                )
            }
        }
    }

    override fun onReceiveClick() {
        analyticsEventsHandler.send(TokenScreenEvent.ButtonReceive(cryptoCurrency.symbol))

        viewModelScope.launch(dispatchers.io) {
            val addresses = walletManagersFacade.getAddress(
                userWalletId = userWalletId,
                network = cryptoCurrency.network,
            )

            uiState = stateFactory.getStateWithReceiveBottomSheet(
                currency = cryptoCurrency,
                addresses = addresses,
                sendCopyAnalyticsEvent = {
                    analyticsEventsHandler.send(TokenReceiveAnalyticsEvent.ButtonCopyAddress(cryptoCurrency.symbol))
                },
                sendShareAnalyticsEvent = {
                    analyticsEventsHandler.send(TokenReceiveAnalyticsEvent.ButtonShareAddress(cryptoCurrency.symbol))
                },
            )
        }
    }

    override fun onSellClick() {
        analyticsEventsHandler.send(TokenScreenEvent.ButtonSell(cryptoCurrency.symbol))

        val status = cryptoCurrencyStatus ?: return
        reduxStateHolder.dispatch(
            TradeCryptoAction.New.Sell(
                cryptoCurrencyStatus = status,
                appCurrencyCode = selectedAppCurrencyFlow.value.code,
            ),
        )
    }

    override fun onSwapClick() {
        analyticsEventsHandler.send(TokenScreenEvent.ButtonExchange(cryptoCurrency.symbol))

        reduxStateHolder.dispatch(TradeCryptoAction.New.Swap(cryptoCurrency))
    }

    override fun onDismissDialog() {
        uiState = stateFactory.getStateWithClosedDialog()
    }

    override fun onHideClick() {
        analyticsEventsHandler.send(TokenScreenEvent.ButtonRemoveToken(cryptoCurrency.symbol))

        viewModelScope.launch {
            val hasLinkedTokens = removeCurrencyUseCase.hasLinkedTokens(userWalletId, cryptoCurrency)
            uiState = if (hasLinkedTokens) {
                stateFactory.getStateWithLinkedTokensDialog(cryptoCurrency)
            } else {
                stateFactory.getStateWithConfirmHideTokenDialog(cryptoCurrency)
            }
        }
    }

    override fun onHideConfirmed() {
        viewModelScope.launch {
            removeCurrencyUseCase.invoke(userWalletId, cryptoCurrency)
                .onLeft { Timber.e(it) }
                .onRight { router.popBackStack() }
        }
    }

    override fun onExploreClick() {
        analyticsEventsHandler.send(TokenScreenEvent.ButtonExplore(cryptoCurrency.symbol))
        viewModelScope.launch(dispatchers.io) {
            val addresses = walletManagersFacade.getAddress(
                userWalletId = userWalletId,
                network = cryptoCurrency.network,
            )

            if (addresses.size == 1) {
                openUrl(AddressType.Default)
            } else {
                uiState = stateFactory.getStateWithChooseAddressBottomSheet(
                    addresses = addresses,
                    onAddressTypeClick = {
                        openUrl(AddressType.valueOf(it.type.name))
                        uiState = stateFactory.getStateWithClosedBottomSheet()
                    },
                )
            }
        }
    }

    private fun openUrl(addressType: AddressType) {
        viewModelScope.launch {
            router.openUrl(
                url = getExploreUrlUseCase(
                    userWalletId = userWalletId,
                    network = cryptoCurrency.network,
                    addressType = addressType,
                ),
            )
        }
    }

    override fun onRefreshSwipe() {
        analyticsEventsHandler.send(TokenScreenEvent.Refreshed(cryptoCurrency.symbol))

        uiState = stateFactory.getRefreshingState()

        viewModelScope.launch(dispatchers.io) {
            listOf(
                async {
                    fetchCurrencyStatusUseCase(
                        userWalletId = userWalletId,
                        id = cryptoCurrency.id,
                        derivationPath = cryptoCurrency.network.derivationPath,
                        refresh = true,
                    )
                },
                async {
                    updateTxHistory(
                        refresh = true,
                        showItemsLoading = uiState.txHistoryState !is TxHistoryState.Content,
                    )
                },
                async { updateWarnings() },
            ).awaitAll()
            uiState = stateFactory.getRefreshedState()
        }.saveIn(refreshStateJobHolder)
    }

    override fun onDismissBottomSheet() {
        uiState = stateFactory.getStateWithClosedBottomSheet()
    }

    override fun onCloseRentInfoNotification() {
        uiState = stateFactory.getStateWithRemovedRentNotification()
    }
}
