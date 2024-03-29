package com.tangem.feature.tokendetails.presentation.tokendetails.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.*
import androidx.paging.cachedIn
import arrow.core.getOrElse
import com.tangem.blockchain.common.address.AddressType
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.deeplink.DeepLinksRegistry
import com.tangem.core.deeplink.global.BuyCurrencyDeepLink
import com.tangem.core.deeplink.global.SellCurrencyDeepLink
import com.tangem.core.ui.components.bottomsheets.tokenreceive.AddressModel
import com.tangem.core.ui.components.transactions.state.TxHistoryState
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.datasource.local.swaptx.SwapTransactionStatusStore
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.card.GetExtendedPublicKeyForCurrencyUseCase
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.demo.IsDemoCardUseCase
import com.tangem.domain.redux.ReduxStateHolder
import com.tangem.domain.settings.ShouldShowSwapPromoTokenUseCase
import com.tangem.domain.tokens.*
import com.tangem.domain.tokens.legacy.TradeCryptoAction
import com.tangem.domain.tokens.legacy.TradeCryptoAction.TransactionInfo
import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.NetworkAddress
import com.tangem.domain.tokens.models.analytics.TokenExchangeAnalyticsEvent
import com.tangem.domain.tokens.models.analytics.TokenReceiveAnalyticsEvent
import com.tangem.domain.tokens.models.analytics.TokenScreenAnalyticsEvent
import com.tangem.domain.tokens.models.analytics.TokenSwapPromoAnalyticsEvent
import com.tangem.domain.tokens.repository.QuotesRepository
import com.tangem.domain.txhistory.usecase.GetExplorerTransactionUrlUseCase
import com.tangem.domain.txhistory.usecase.GetTxHistoryItemsCountUseCase
import com.tangem.domain.txhistory.usecase.GetTxHistoryItemsUseCase
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.usecase.GetExploreUrlUseCase
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.feature.swap.domain.SwapTransactionRepository
import com.tangem.feature.swap.domain.api.SwapRepository
import com.tangem.feature.swap.domain.models.domain.ExchangeStatus
import com.tangem.feature.tokendetails.presentation.router.InnerTokenDetailsRouter
import com.tangem.feature.tokendetails.presentation.tokendetails.analytics.TokenDetailsCurrencyStatusAnalyticsSender
import com.tangem.feature.tokendetails.presentation.tokendetails.analytics.TokenDetailsNotificationsAnalyticsSender
import com.tangem.feature.tokendetails.presentation.tokendetails.state.SwapTransactionsState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.factory.TokenDetailsStateFactory
import com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.exchange.ExchangeStatusBottomSheetConfig
import com.tangem.features.tokendetails.featuretoggles.TokenDetailsFeatureToggles
import com.tangem.features.tokendetails.impl.R
import com.tangem.features.tokendetails.navigation.TokenDetailsRouter
import com.tangem.lib.crypto.BlockchainUtils.isBitcoin
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.PersistentList
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.math.BigDecimal
import javax.inject.Inject

@Suppress("LongParameterList", "LargeClass", "TooManyFunctions")
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
    private val getFeePaidCryptoCurrencyStatusSyncUseCase: GetFeePaidCryptoCurrencyStatusSyncUseCase,
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    private val getCurrencyWarningsUseCase: GetCurrencyWarningsUseCase,
    private val getExplorerTransactionUrlUseCase: GetExplorerTransactionUrlUseCase,
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
    private val shouldShowSwapPromoTokenUseCase: ShouldShowSwapPromoTokenUseCase,
    private val updateDelayedCurrencyStatusUseCase: UpdateDelayedNetworkStatusUseCase,
    private val getExtendedPublicKeyForCurrencyUseCase: GetExtendedPublicKeyForCurrencyUseCase,
    private val swapRepository: SwapRepository,
    private val swapTransactionRepository: SwapTransactionRepository,
    private val quotesRepository: QuotesRepository,
    private val swapTransactionStatusStore: SwapTransactionStatusStore,
    private val isDemoCardUseCase: IsDemoCardUseCase,
    private val reduxStateHolder: ReduxStateHolder,
    private val analyticsEventsHandler: AnalyticsEventHandler,
    featureToggles: TokenDetailsFeatureToggles,
    deepLinksRegistry: DeepLinksRegistry,
    savedStateHandle: SavedStateHandle,
) : ViewModel(), DefaultLifecycleObserver, TokenDetailsClickIntents {

    private val userWalletId: UserWalletId = savedStateHandle.get<String>(TokenDetailsRouter.USER_WALLET_ID_KEY)
        ?.let { stringValue -> UserWalletId(stringValue) }
        ?: error("This screen can't open without `UserWalletId`")

    private val cryptoCurrency: CryptoCurrency = savedStateHandle[TokenDetailsRouter.CRYPTO_CURRENCY_KEY]
        ?: error("This screen can't open without `CryptoCurrency`")

    lateinit var router: InnerTokenDetailsRouter

    private val marketPriceJobHolder = JobHolder()
    private val refreshStateJobHolder = JobHolder()
    private val warningsJobHolder = JobHolder()
    private val swapTxJobHolder = JobHolder()
    private var cryptoCurrencyStatus: CryptoCurrencyStatus? = null

    private var swapTxStatusTaskScheduler = SingleTaskScheduler<PersistentList<SwapTransactionsState>>()

    private val selectedAppCurrencyFlow: StateFlow<AppCurrency> = createSelectedAppCurrencyFlow()

    private val stateFactory = TokenDetailsStateFactory(
        currentStateProvider = Provider { uiState },
        appCurrencyProvider = Provider(selectedAppCurrencyFlow::value),
        clickIntents = this,
        symbol = cryptoCurrency.symbol,
        decimals = cryptoCurrency.decimals,
        featureToggles = featureToggles,
    )

    private val exchangeStatusFactory by lazy(mode = LazyThreadSafetyMode.NONE) {
        ExchangeStatusFactory(
            swapTransactionRepository = swapTransactionRepository,
            swapRepository = swapRepository,
            quotesRepository = quotesRepository,
            getSelectedWalletSyncUseCase = getSelectedWalletSyncUseCase,
            swapTransactionStatusStore = swapTransactionStatusStore,
            dispatchers = dispatchers,
            clickIntents = this,
            appCurrencyProvider = Provider { selectedAppCurrencyFlow.value },
            analyticsEventsHandlerProvider = Provider { analyticsEventsHandler },
            currentStateProvider = Provider { uiState },
            userWalletId = userWalletId,
            cryptoCurrency = cryptoCurrency,
        )
    }

    private val notificationsAnalyticsSender by lazy(mode = LazyThreadSafetyMode.NONE) {
        TokenDetailsNotificationsAnalyticsSender(
            cryptoCurrency = cryptoCurrency,
            analyticsEventHandler = analyticsEventsHandler,
        )
    }

    private val currencyStatusAnalyticsSender by lazy(mode = LazyThreadSafetyMode.NONE) {
        TokenDetailsCurrencyStatusAnalyticsSender(analyticsEventsHandler)
    }

    var uiState: TokenDetailsState by mutableStateOf(stateFactory.getInitialState(cryptoCurrency))
        private set

    init {
        deepLinksRegistry.registerWithViewModel(
            viewModel = this,
            deepLinks = listOf(
                BuyCurrencyDeepLink(::onBuyCurrencyDeepLink),
                SellCurrencyDeepLink(::onSellCurrencyDeepLink),
            ),
        )
    }

    private fun onBuyCurrencyDeepLink() {
        val currency = cryptoCurrencyStatus?.currency ?: return
        analyticsEventsHandler.send(TokenScreenAnalyticsEvent.Bought(currency.symbol))
    }

    private fun onSellCurrencyDeepLink(data: SellCurrencyDeepLink.Data) {
        sendCurrency(
            status = cryptoCurrencyStatus ?: return,
            transactionInfo = data.let {
                TransactionInfo(
                    transactionId = it.transactionId,
                    destinationAddress = it.depositWalletAddress,
                    amount = it.baseCurrencyAmount,
                    tag = it.depositWalletAddressTag,
                )
            },
        )
    }

    override fun onCreate(owner: LifecycleOwner) {
        analyticsEventsHandler.send(
            event = TokenScreenAnalyticsEvent.DetailsScreenOpened(token = cryptoCurrency.symbol),
        )
        updateTopBarMenu()
        updateContent()
        handleBalanceHiding(owner)
    }

    override fun onCleared() {
        swapTxStatusTaskScheduler.cancelTask()
        super.onCleared()
    }

    private fun updateContent() {
        subscribeOnCurrencyStatusUpdates()
        subscribeOnExchangeTransactionsUpdates()
        updateTxHistory(refresh = false, showItemsLoading = true)
    }

    private fun handleBalanceHiding(owner: LifecycleOwner) {
        getBalanceHidingSettingsUseCase()
            .flowWithLifecycle(owner.lifecycle)
            .onEach {
                uiState = stateFactory.getStateWithUpdatedHidden(
                    isBalanceHidden = it.isBalanceHidden,
                )
            }
            .launchIn(viewModelScope)
    }

    private suspend fun updateButtons(userWalletId: UserWalletId, currencyStatus: CryptoCurrencyStatus) {
        val userWallet = getUserWalletUseCase(userWalletId).getOrElse { return }
        getCryptoCurrencyActionsUseCase(
            userWallet = userWallet,
            cryptoCurrencyStatus = currencyStatus,
        )
            .conflate()
            .distinctUntilChanged()
            .onEach { uiState = stateFactory.getManageButtonsState(actions = it.states) }
            .flowOn(dispatchers.io)
            .launchIn(viewModelScope)
    }

    private fun updateWarnings(cryptoCurrencyStatus: CryptoCurrencyStatus) {
        viewModelScope.launch(dispatchers.io) {
            val wallet = getUserWalletUseCase(userWalletId).getOrElse { return@launch }
            getCurrencyWarningsUseCase.invoke(
                userWalletId = userWalletId,
                currencyStatus = cryptoCurrencyStatus,
                derivationPath = cryptoCurrency.network.derivationPath,
                isSingleWalletWithTokens = wallet.scanResponse.cardTypesResolver.isSingleWalletWithToken(),
            )
                .distinctUntilChanged()
                .onEach {
                    val updatedState = stateFactory.getStateWithNotifications(it)
                    notificationsAnalyticsSender.send(uiState, updatedState.notifications)
                    uiState = updatedState
                }
                .launchIn(viewModelScope)
                .saveIn(warningsJobHolder)
        }
    }

    private fun subscribeOnCurrencyStatusUpdates() {
        viewModelScope.launch(dispatchers.io) {
            val wallet = getUserWalletUseCase(userWalletId).getOrElse { return@launch }
            getCurrencyStatusUpdatesUseCase(
                userWalletId = userWalletId,
                currencyId = cryptoCurrency.id,
                isSingleWalletWithTokens = wallet.scanResponse.cardTypesResolver.isSingleWalletWithToken(),
            )
                .distinctUntilChanged()
                .onEach { maybeCurrencyStatus ->
                    uiState = stateFactory.getCurrencyLoadedBalanceState(maybeCurrencyStatus)
                    maybeCurrencyStatus.onRight { status ->
                        cryptoCurrencyStatus = status
                        updateButtons(userWalletId = userWalletId, currencyStatus = status)
                        updateWarnings(status)
                    }
                    currencyStatusAnalyticsSender.send(maybeCurrencyStatus)
                }
                .flowOn(dispatchers.io)
                .launchIn(viewModelScope)
                .saveIn(marketPriceJobHolder)
        }
    }

    private fun subscribeOnExchangeTransactionsUpdates() {
        viewModelScope.launch(dispatchers.io) {
            swapTxStatusTaskScheduler.cancelTask()
            exchangeStatusFactory.invoke()
                .distinctUntilChanged()
                .filterNot { it.isEmpty() }
                .onEach { swapTxs ->
                    updateSwapTx(swapTxs)
                    swapTxStatusTaskScheduler.scheduleTask(
                        viewModelScope,
                        PeriodicTask(
                            delay = EXCHANGE_STATUS_UPDATE_DELAY,
                            task = {
                                runCatching(dispatchers.io) {
                                    exchangeStatusFactory.updateSwapTxStatuses(uiState.swapTxs)
                                }
                            },
                            onSuccess = ::updateSwapTx,
                            onError = { /* no-op */ },
                        ),
                    )
                }
                .flowOn(dispatchers.io)
                .launchIn(viewModelScope)
                .saveIn(swapTxJobHolder)
        }
    }

    private fun updateSwapTx(swapTxs: PersistentList<SwapTransactionsState>) {
        val config = uiState.bottomSheetConfig
        val exchangeBottomSheet = config?.content as? ExchangeStatusBottomSheetConfig
        val currentTx = swapTxs.firstOrNull { it.txId == exchangeBottomSheet?.value?.txId }
        if (currentTx?.activeStatus == ExchangeStatus.Finished) {
            updateNetworkToSwapBalance(currentTx.toCryptoCurrency)
        }
        uiState = uiState.copy(
            swapTxs = swapTxs,
            bottomSheetConfig = currentTx?.let(
                stateFactory::updateStateWithExchangeStatusBottomSheet,
            ) ?: config,
        )
    }

    private fun updateNetworkToSwapBalance(toCryptoCurrency: CryptoCurrency) {
        viewModelScope.launch {
            updateDelayedCurrencyStatusUseCase(
                userWalletId = userWalletId,
                network = toCryptoCurrency.network,
                refresh = true,
            )
        }
    }

    /**
     * @param refresh - invalidate cache and get data from remote
     * @param showItemsLoading - show loading items placeholder.
     */
    private fun updateTxHistory(refresh: Boolean, showItemsLoading: Boolean) {
        viewModelScope.launch(dispatchers.io) {
            val txHistoryItemsCountEither = txHistoryItemsCountUseCase(
                userWalletId = userWalletId,
                currency = cryptoCurrency,
            )

            // if countEither is left, handling error state run inside getLoadingTxHistoryState
            if (showItemsLoading || txHistoryItemsCountEither.isLeft()) {
                uiState = stateFactory.getLoadingTxHistoryState(
                    itemsCountEither = txHistoryItemsCountEither,
                    pendingTransactions = uiState.pendingTxs,
                )
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

    private fun updateTopBarMenu() {
        viewModelScope.launch(dispatchers.main) {
            val wallet = getUserWalletUseCase(userWalletId).getOrElse { return@launch }

            uiState = stateFactory.getStateWithUpdatedMenu(
                cardTypesResolver = wallet.scanResponse.cardTypesResolver,
                isBitcoin = isBitcoin(cryptoCurrency.network.id.value),
            )
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

    override fun onBuyClick(unavailabilityReason: ScenarioUnavailabilityReason) {
        analyticsEventsHandler.send(TokenScreenAnalyticsEvent.ButtonBuy(cryptoCurrency.symbol))

        if (handleUnavailabilityReason(unavailabilityReason)) return

        showErrorIfDemoModeOrElse {
            val status = cryptoCurrencyStatus ?: return@showErrorIfDemoModeOrElse

            viewModelScope.launch(dispatchers.main) {
                reduxStateHolder.dispatch(
                    TradeCryptoAction.Buy(
                        userWallet = getUserWalletUseCase(userWalletId).getOrElse { return@launch },
                        cryptoCurrencyStatus = status,
                        appCurrencyCode = selectedAppCurrencyFlow.value.code,
                    ),
                )
            }
        }
    }

    override fun onBuyCoinClick(cryptoCurrency: CryptoCurrency) {
        analyticsEventsHandler.send(TokenScreenAnalyticsEvent.ButtonBuy(cryptoCurrency.symbol))
        router.openTokenDetails(userWalletId = userWalletId, currency = cryptoCurrency)
    }

    override fun onReloadClick() {
        analyticsEventsHandler.send(TokenScreenAnalyticsEvent.ButtonReload(cryptoCurrency.symbol))
        uiState = stateFactory.getLoadingTxHistoryState()
        updateTxHistory(refresh = true, showItemsLoading = true)
    }

    override fun onSendClick(unavailabilityReason: ScenarioUnavailabilityReason) {
        analyticsEventsHandler.send(TokenScreenAnalyticsEvent.ButtonSend(cryptoCurrency.symbol))

        if (handleUnavailabilityReason(unavailabilityReason)) return

        sendCurrency(status = cryptoCurrencyStatus ?: return)
    }

    private fun sendCurrency(status: CryptoCurrencyStatus, transactionInfo: TransactionInfo? = null) {
        viewModelScope.launch(dispatchers.main) {
            val maybeFeeCurrencyStatus =
                getFeePaidCryptoCurrencyStatusSyncUseCase(userWalletId, status).getOrNull()

            when (val currency = status.currency) {
                is CryptoCurrency.Coin -> {
                    reduxStateHolder.dispatch(
                        action = TradeCryptoAction.SendCoin(
                            userWallet = getUserWalletUseCase(userWalletId).getOrElse { return@launch },
                            coinStatus = status,
                            feeCurrencyStatus = maybeFeeCurrencyStatus,
                            transactionInfo = transactionInfo,
                        ),
                    )
                }
                is CryptoCurrency.Token -> {
                    sendToken(
                        tokenCurrency = currency,
                        tokenFiatRate = status.value.fiatRate,
                        feeCurrencyStatus = maybeFeeCurrencyStatus,
                        transactionInfo = transactionInfo,
                    )
                }
            }
        }
    }

    private fun sendToken(
        tokenCurrency: CryptoCurrency.Token,
        tokenFiatRate: BigDecimal?,
        feeCurrencyStatus: CryptoCurrencyStatus?,
        transactionInfo: TransactionInfo?,
    ) {
        viewModelScope.launch(dispatchers.io) {
            val wallet = getUserWalletUseCase(userWalletId).getOrElse { return@launch }
            val maybeCoinStatus = getNetworkCoinStatusUseCase(
                userWalletId = userWalletId,
                networkId = tokenCurrency.network.id,
                derivationPath = tokenCurrency.network.derivationPath,
                isSingleWalletWithTokens = wallet.scanResponse.cardTypesResolver.isSingleWalletWithToken(),
            )
                .conflate()
                .distinctUntilChanged()
                .firstOrNull()

            reduxStateHolder.dispatchWithMain(
                action = TradeCryptoAction.SendToken(
                    userWallet = wallet,
                    tokenCurrency = tokenCurrency,
                    tokenFiatRate = tokenFiatRate,
                    coinFiatRate = maybeCoinStatus?.fold(
                        ifLeft = { null },
                        ifRight = { it.value.fiatRate },
                    ),
                    feeCurrencyStatus = feeCurrencyStatus,
                    transactionInfo = transactionInfo,
                ),
            )
        }
    }

    override fun onReceiveClick(unavailabilityReason: ScenarioUnavailabilityReason) {
        val networkAddress = cryptoCurrencyStatus?.value?.networkAddress ?: return

        if (handleUnavailabilityReason(unavailabilityReason)) return

        viewModelScope.launch(dispatchers.io) {
            analyticsEventsHandler.send(TokenScreenAnalyticsEvent.ButtonReceive(cryptoCurrency.symbol))
            analyticsEventsHandler.send(TokenReceiveAnalyticsEvent.ReceiveScreenOpened)

            uiState = stateFactory.getStateWithReceiveBottomSheet(
                currency = cryptoCurrency,
                networkAddress = networkAddress,
                sendCopyAnalyticsEvent = {
                    analyticsEventsHandler.send(TokenReceiveAnalyticsEvent.ButtonCopyAddress(cryptoCurrency.symbol))
                },
                sendShareAnalyticsEvent = {
                    analyticsEventsHandler.send(TokenReceiveAnalyticsEvent.ButtonShareAddress(cryptoCurrency.symbol))
                },
            )
        }
    }

    override fun onGenerateExtendedKey() {
        viewModelScope.launch(dispatchers.main) {
            val extendedKey = getExtendedPublicKeyForCurrencyUseCase(
                userWalletId,
                cryptoCurrency.network.derivationPath,
            ).fold(
                ifLeft = {
                    Timber.e(it.cause?.localizedMessage.orEmpty())
                    ""
                },
                ifRight = { it },
            )
            if (extendedKey.isNotBlank()) {
                router.share(extendedKey)
            }
        }
    }

    override fun onSellClick(unavailabilityReason: ScenarioUnavailabilityReason) {
        analyticsEventsHandler.send(TokenScreenAnalyticsEvent.ButtonSell(cryptoCurrency.symbol))

        if (handleUnavailabilityReason(unavailabilityReason)) return

        showErrorIfDemoModeOrElse {
            val status = cryptoCurrencyStatus ?: return@showErrorIfDemoModeOrElse

            reduxStateHolder.dispatch(
                TradeCryptoAction.Sell(
                    cryptoCurrencyStatus = status,
                    appCurrencyCode = selectedAppCurrencyFlow.value.code,
                ),
            )
        }
    }

    override fun onSwapClick(unavailabilityReason: ScenarioUnavailabilityReason) {
        analyticsEventsHandler.send(TokenScreenAnalyticsEvent.ButtonExchange(cryptoCurrency.symbol))

        if (handleUnavailabilityReason(unavailabilityReason)) return

        reduxStateHolder.dispatch(TradeCryptoAction.Swap(cryptoCurrency))
    }

    override fun onDismissDialog() {
        uiState = stateFactory.getStateWithClosedDialog()
    }

    override fun onHideClick() {
        analyticsEventsHandler.send(TokenScreenAnalyticsEvent.ButtonRemoveToken(cryptoCurrency.symbol))

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
        analyticsEventsHandler.send(TokenScreenAnalyticsEvent.ButtonExplore(cryptoCurrency.symbol))
        showErrorIfDemoModeOrElse(action = ::openExplorer)
    }

    private fun openExplorer() {
        val currencyStatus = cryptoCurrencyStatus ?: return

        viewModelScope.launch(dispatchers.io) {
            when (val addresses = currencyStatus.value.networkAddress) {
                is NetworkAddress.Selectable -> {
                    uiState = stateFactory.getStateWithChooseAddressBottomSheet(cryptoCurrency, addresses)
                }
                is NetworkAddress.Single -> {
                    router.openUrl(
                        url = getExploreUrlUseCase(
                            userWalletId = userWalletId,
                            currency = cryptoCurrency,
                            addressType = AddressType.Default,
                        ),
                    )
                }
                null -> Unit
            }
        }
    }

    private fun showErrorIfDemoModeOrElse(action: () -> Unit) {
        viewModelScope.launch(dispatchers.main) {
            val wallet = getUserWalletUseCase(userWalletId = userWalletId).getOrElse { return@launch }

            if (isDemoCardUseCase(cardId = wallet.cardId)) {
                uiState = stateFactory.getStateWithClosedBottomSheet()
                uiState = stateFactory.getStateAndTriggerEvent(
                    state = uiState,
                    errorMessage = resourceReference(id = R.string.alert_demo_feature_disabled),
                    setUiState = { uiState = it },
                )
            } else {
                action()
            }
        }
    }

    override fun onAddressTypeSelected(addressModel: AddressModel) {
        viewModelScope.launch {
            router.openUrl(
                url = getExploreUrlUseCase(
                    userWalletId = userWalletId,
                    currency = cryptoCurrency,
                    addressType = AddressType.valueOf(addressModel.type.name),
                ),
            )
            uiState = stateFactory.getStateWithClosedBottomSheet()
        }
    }

    override fun onTransactionClick(txHash: String) {
        getExplorerTransactionUrlUseCase(
            txHash = txHash,
            networkId = cryptoCurrency.network.id,
        ).fold(
            ifLeft = { Timber.e(it.toString()) },
            ifRight = { router.openUrl(url = it) },
        )
    }

    override fun onRefreshSwipe() {
        analyticsEventsHandler.send(TokenScreenAnalyticsEvent.Refreshed(cryptoCurrency.symbol))

        uiState = stateFactory.getRefreshingState()

        viewModelScope.launch(dispatchers.io) {
            listOf(
                async {
                    fetchCurrencyStatusUseCase(
                        userWalletId = userWalletId,
                        id = cryptoCurrency.id,
                        refresh = true,
                    )
                },
                async {
                    updateTxHistory(
                        refresh = true,
                        showItemsLoading = uiState.txHistoryState !is TxHistoryState.Content,
                    )
                    subscribeOnExchangeTransactionsUpdates()
                },
            ).awaitAll()
            uiState = stateFactory.getRefreshedState()
        }.saveIn(refreshStateJobHolder)
    }

    override fun onDismissBottomSheet() {
        if (uiState.bottomSheetConfig?.content is ExchangeStatusBottomSheetConfig) {
            viewModelScope.launch(dispatchers.main) {
                uiState = exchangeStatusFactory.removeTransactionOnBottomSheetClosed()
            }
        }
        uiState = stateFactory.getStateWithClosedBottomSheet()
    }

    override fun onCloseRentInfoNotification() {
        uiState = stateFactory.getStateWithRemovedRentNotification()
    }

    override fun onSwapTransactionClick(txId: String) {
        val swapTxState = uiState.swapTxs.first { it.txId == txId }
        analyticsEventsHandler.send(TokenExchangeAnalyticsEvent.CexTxStatusOpened(cryptoCurrency.symbol))
        uiState = stateFactory.getStateWithExchangeStatusBottomSheet(swapTxState)
    }

    override fun onGoToProviderClick(url: String) {
        router.openUrl(url)
    }

    override fun onSwapPromoDismiss() {
        viewModelScope.launch(dispatchers.main) {
            shouldShowSwapPromoTokenUseCase.neverToShow()
            analyticsEventsHandler.send(TokenSwapPromoAnalyticsEvent.Close)
        }
    }

    override fun onSwapPromoClick() {
        viewModelScope.launch(dispatchers.main) {
            shouldShowSwapPromoTokenUseCase.neverToShow()
            analyticsEventsHandler.send(TokenSwapPromoAnalyticsEvent.Exchange(cryptoCurrency.symbol))
        }
        onSwapClick(ScenarioUnavailabilityReason.None)
    }

    private fun handleUnavailabilityReason(unavailabilityReason: ScenarioUnavailabilityReason): Boolean {
        if (unavailabilityReason == ScenarioUnavailabilityReason.None) return false

        uiState = stateFactory.getStateWithActionButtonErrorDialog(unavailabilityReason)

        return true
    }

    private companion object {
        const val EXCHANGE_STATUS_UPDATE_DELAY = 10_000L
    }
}
