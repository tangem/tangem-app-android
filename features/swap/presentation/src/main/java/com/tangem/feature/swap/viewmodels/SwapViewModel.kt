package com.tangem.feature.swap.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.*
import arrow.core.getOrElse
import com.tangem.common.Provider
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.ui.utils.InputNumberFormatter
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.tokens.GetCryptoCurrencyStatusSyncUseCase
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.feature.swap.analytics.SwapEvents
import com.tangem.feature.swap.domain.BlockchainInteractor
import com.tangem.feature.swap.domain.SwapInteractor
import com.tangem.feature.swap.domain.models.domain.PermissionOptions
import com.tangem.feature.swap.domain.models.domain.RateType
import com.tangem.feature.swap.domain.models.domain.SwapProvider
import com.tangem.feature.swap.domain.models.formatToUIRepresentation
import com.tangem.feature.swap.domain.models.ui.*
import com.tangem.feature.swap.models.SwapPermissionState
import com.tangem.feature.swap.models.SwapStateHolder
import com.tangem.feature.swap.models.UiActions
import com.tangem.feature.swap.models.toDomainApproveType
import com.tangem.feature.swap.presentation.SwapFragment
import com.tangem.feature.swap.router.SwapNavScreen
import com.tangem.feature.swap.router.SwapRouter
import com.tangem.feature.swap.ui.StateBuilder
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.Debouncer
import com.tangem.utils.coroutines.runCatching
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject
import kotlin.properties.Delegates

@Suppress("LargeClass", "LongParameterList")
@HiltViewModel
internal class SwapViewModel @Inject constructor(
    private val swapInteractor: SwapInteractor,
    private val blockchainInteractor: BlockchainInteractor,
    private val dispatchers: CoroutineDispatcherProvider,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val getCryptoCurrencyStatusUseCase: GetCryptoCurrencyStatusSyncUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel(), DefaultLifecycleObserver {

    private val initialCryptoCurrency: CryptoCurrency = savedStateHandle[SwapFragment.CURRENCY_BUNDLE_KEY]
        ?: error("no expected parameter CryptoCurrency found`")
    private lateinit var initialCryptoCurrencyStatus: CryptoCurrencyStatus

    private var isBalanceHidden = true

    private val selectedAppCurrencyFlow: StateFlow<AppCurrency> = createSelectedAppCurrencyFlow()

    private val stateBuilder = StateBuilder(
        actions = createUiActions(),
        isBalanceHiddenProvider = Provider { isBalanceHidden },
        appCurrencyProvider = Provider(selectedAppCurrencyFlow::value),
    )

    private val inputNumberFormatter =
        InputNumberFormatter(NumberFormat.getInstance(Locale.getDefault()) as DecimalFormat)
    private val amountDebouncer = Debouncer()
    private val singleTaskScheduler = SingleTaskScheduler<SwapState>()

    private var dataState by mutableStateOf(SwapProcessDataState(networkId = initialCryptoCurrency.network.backendId))

    var uiState: SwapStateHolder by mutableStateOf(
        stateBuilder.createInitialLoadingState(
            initialCurrency = initialCryptoCurrency,
            networkInfo = blockchainInteractor.getBlockchainInfo(initialCryptoCurrency.network.backendId),
        ),
    )
        private set

    // shows currency order (direct - swap initial to selected, reversed = selected to initial)
    private var isOrderReversed = false
    private val lastAmount = mutableStateOf(INITIAL_AMOUNT)
    private var swapRouter: SwapRouter by Delegates.notNull()
    val currentScreen: SwapNavScreen
        get() = swapRouter.currentScreen

    init {
        viewModelScope.launch(dispatchers.io) {
            swapInteractor.getSelectedWallet()?.let {
                initialCryptoCurrencyStatus =
                    requireNotNull(getCryptoCurrencyStatusUseCase(it.walletId, initialCryptoCurrency.id).getOrNull()) {
                        "Failed to get initial crypto currency status"
                    }

                swapInteractor.initDerivationPathAndNetwork(
                    derivationPath = initialCryptoCurrency.network.derivationPath.value,
                    network = initialCryptoCurrency.network,
                )
                initTokens()
            }
        }
    }

    override fun onCreate(owner: LifecycleOwner) {
        getBalanceHidingSettingsUseCase()
            .flowWithLifecycle(owner.lifecycle)
            .onEach {
                isBalanceHidden = it.isBalanceHidden
                withContext(dispatchers.main) {
                    uiState = stateBuilder.updateBalanceHiddenState(uiState, isBalanceHidden)
                }
            }
            .launchIn(viewModelScope)
    }

    override fun onCleared() {
        singleTaskScheduler.cancelTask()
        super.onCleared()
    }

    fun onScreenOpened() {
        analyticsEventHandler.send(SwapEvents.SwapScreenOpened(initialCryptoCurrency.symbol))
    }

    fun setRouter(router: SwapRouter) {
        swapRouter = router
        uiState = uiState.copy(
            onBackClicked = router::back,
            onSelectTokenClick = {
                router.openScreen(SwapNavScreen.SelectToken)
                analyticsEventHandler.send(SwapEvents.ChooseTokenScreenOpened)
            },
            onSuccess = {
                router.openScreen(SwapNavScreen.Success)
            },
        )
    }

    @Suppress("UnusedPrivateMember")
    private fun initTokens() {
        // new flow
        viewModelScope.launch(dispatchers.main) {
            runCatching(dispatchers.io) {
                swapInteractor.getTokensDataState(initialCryptoCurrency)
            }.onSuccess { state ->
                updateTokensState(state)
                applyInitialTokenChoice(state, selectInitialCurrencyToSwap(state))
            }.onFailure {
                Timber.tag(loggingTag).e(it)
            }
        }
    }

    private fun applyInitialTokenChoice(state: TokensDataStateExpress, selectedCurrency: CryptoCurrencyStatus?) {
        val fromCurrencyStatus = initialCryptoCurrencyStatus
        dataState = dataState.copy(
            fromCryptoCurrency = fromCurrencyStatus,
            toCryptoCurrency = selectedCurrency,
            tokensDataState = state,
        )
        if (selectedCurrency == null) {
            uiState = stateBuilder.createNoAvailableTokensToSwapState(
                uiStateHolder = uiState,
                fromToken = fromCurrencyStatus,
            )
        } else {
            startLoadingQuotes(
                fromToken = fromCurrencyStatus.currency,
                toToken = selectedCurrency.currency,
                amount = lastAmount.value,
                toProvidersList = findSwapProviders(fromCurrencyStatus, selectedCurrency)
            )
        }
    }

    private fun selectInitialCurrencyToSwap(state: TokensDataStateExpress): CryptoCurrencyStatus? {
// [REDACTED_TODO_COMMENT]
        return state.toGroup.available.firstOrNull()?.currencyStatus
    }

    private fun updateTokensState(dataState: TokensDataStateExpress) {
        val tokensDataState = if (!isOrderReversed) dataState.toGroup else dataState.fromGroup
        uiState = stateBuilder.addTokensToState(
            uiState = uiState,
            tokensDataState = tokensDataState,
        )
    }

    private fun startLoadingQuotes(
        fromToken: CryptoCurrency,
        toToken: CryptoCurrency,
        amount: String,
        toProvidersList: List<SwapProvider>,
    ) {
        singleTaskScheduler.cancelTask()
        uiState = stateBuilder.createQuotesLoadingState(uiState, fromToken, toToken, initialCryptoCurrency.id.value)
        singleTaskScheduler.scheduleTask(
            viewModelScope,
            loadQuotesTask(
                fromToken = fromToken,
                toToken = toToken,
                amount = amount,
                toProvidersList = toProvidersList,
            ),
        )
    }

    private fun startLoadingQuotesFromLastState() {
        val fromCurrency = dataState.fromCryptoCurrency
        val toCurrency = dataState.toCryptoCurrency
        val amount = dataState.amount
        if (fromCurrency != null && toCurrency != null && amount != null) {
            startLoadingQuotes(
                fromToken = fromCurrency.currency,
                toToken = toCurrency.currency,
                amount = amount,
                toProvidersList = findSwapProviders(fromCurrency, toCurrency)
            )
        }
    }

    private fun loadQuotesTask(
        fromToken: CryptoCurrency,
        toToken: CryptoCurrency,
        amount: String,
        toProvidersList: List<SwapProvider>,
    ): PeriodicTask<SwapState> {
        return PeriodicTask(
            UPDATE_DELAY,
            task = {
                uiState = stateBuilder.createSilentLoadState(uiState)
                runCatching(dispatchers.io) {
                    dataState = dataState.copy(
                        amount = amount,
                        swapDataModel = null,
                        approveDataModel = null,
                    )
                    swapInteractor.findBestQuote(
                        networkId = dataState.networkId,
                        fromToken = fromToken,
                        toToken = toToken,
                        providers = toProvidersList,
                        amountToSwap = amount,
                        selectedFee = dataState.selectedFee?.feeType ?: FeeType.NORMAL,
                    ).entries.first().value// TODO
                }
            },
            onSuccess = { swapState ->
                when (swapState) {
                    is SwapState.QuotesLoadedState -> {
                        fillDataState(swapState.permissionState, swapState.swapDataModel)
                        uiState = stateBuilder.createQuotesLoadedState(
                            uiStateHolder = uiState,
                            quoteModel = swapState,
                            fromToken = fromToken,
                        ) { updatedFee ->
                            dataState = dataState.copy(
                                selectedFee = updatedFee,
                            )
                        }
                    }
                    is SwapState.EmptyAmountState -> {
                        uiState = stateBuilder.createQuotesEmptyAmountState(
                            uiStateHolder = uiState,
                            emptyAmountState = swapState,
                        )
                    }
                    is SwapState.SwapError -> {
                        Timber.e("SwapError when loading quotes ${swapState.error}")
                        uiState = stateBuilder.mapError(uiState, swapState.error) { startLoadingQuotesFromLastState() }
                    }
                }
            },
            onError = {
                Timber.e("Error when loading quotes: $it")
                uiState = stateBuilder.addWarning(uiState, null) { startLoadingQuotesFromLastState() }
            },
        )
    }

    private fun fillDataState(permissionState: PermissionDataState, swapDataModel: SwapStateData?) {
        dataState = if (permissionState is PermissionDataState.PermissionReadyForRequest) {
            dataState.copy(
                approveDataModel = permissionState.requestApproveData,
            )
        } else {
            dataState.copy(
                swapDataModel = swapDataModel,
                selectedFee = swapDataModel?.fee?.normalFee,
            )
        }
    }

    private fun onSwapClick() {
        singleTaskScheduler.cancelTask()
        uiState = stateBuilder.createSwapInProgressState(uiState)
        viewModelScope.launch(dispatchers.main) {
            runCatching(dispatchers.io) {
                swapInteractor.onSwap(
                    networkId = dataState.networkId,
                    swapStateData = requireNotNull(dataState.swapDataModel),
                    currencyToSend = requireNotNull(dataState.fromCryptoCurrency?.currency),
                    currencyToGet = requireNotNull(dataState.toCryptoCurrency?.currency),
                    amountToSwap = requireNotNull(dataState.amount),
                    fee = requireNotNull(dataState.selectedFee),
                )
            }
                .onSuccess {
                    when (it) {
                        is TxState.TxSent -> {
                            uiState = stateBuilder.createSuccessState(uiState, it) {
                                val txHash = it.txAddress
                                if (txHash.isNotEmpty()) {
                                    swapRouter.openUrl(
                                        blockchainInteractor.getExplorerTransactionLink(
                                            networkId = dataState.networkId,
                                            txAddress = it.txAddress,
                                        ),
                                    )
                                }
                            }
                            analyticsEventHandler.send(SwapEvents.SwapInProgressScreen)
                            swapRouter.openScreen(SwapNavScreen.Success)
                        }
                        is TxState.UserCancelled -> {
                            startLoadingQuotesFromLastState()
                        }
                        else -> {
                            startLoadingQuotesFromLastState()
                            uiState = stateBuilder.createErrorTransaction(uiState, it) {
                                uiState = stateBuilder.clearAlert(uiState)
                            }
                        }
                    }
                }
                .onFailure {
                    startLoadingQuotesFromLastState()
                    makeDefaultAlert()
                }
        }
    }

    private fun givePermissionsToSwap() {
        viewModelScope.launch(dispatchers.main) {
            runCatching(dispatchers.io) {
                swapInteractor.givePermissionToSwap(
                    networkId = dataState.networkId,
                    permissionOptions = PermissionOptions(
                        approveData = requireNotNull(dataState.approveDataModel) {
                            "dataState.approveDataModel might not be null"
                        },
                        forTokenContractAddress = (dataState.fromCryptoCurrency?.currency as? CryptoCurrency.Token)
                            ?.contractAddress
                            ?: "",
                        fromToken = requireNotNull(dataState.fromCryptoCurrency?.currency) {
                            "dataState.fromCurrency might not be null"
                        },
                        approveType = requireNotNull(uiState.permissionState as? SwapPermissionState.ReadyForRequest) {
                            "uiState.permissionState should be SwapPermissionState.ReadyForRequest"
                        }.approveType.toDomainApproveType(),
                        txFee = requireNotNull(dataState.selectedFee) {
                            "dataState.selectedFee shouldn't be null"
                        },
                    ),
                )
            }
                .onSuccess {
                    when (it) {
                        is TxState.TxSent -> {
                            uiState = stateBuilder.loadingPermissionState(uiState)
                        }
                        is TxState.UserCancelled -> Unit
                        else -> {
                            uiState = stateBuilder.createErrorTransaction(uiState, it) {
                                uiState = stateBuilder.clearAlert(uiState)
                            }
                        }
                    }
                }
                .onFailure {
                    makeDefaultAlert()
                }
        }
    }

    private fun onSearchEntered(searchQuery: String) {
        viewModelScope.launch(dispatchers.main) {
            runCatching(dispatchers.io) {
                swapInteractor.searchTokens(dataState.networkId, searchQuery)
            }
                .onSuccess {
                    // updateTokensState(it)
                }
                .onFailure { }
        }
    }

    private fun onTokenSelect(id: String) {
        val tokens = dataState.tokensDataState ?: return

        val foundToken = if (isOrderReversed) {
            tokens.fromGroup.available.firstOrNull {
                it.currencyStatus.currency.id.value == id
            }
        } else {
            tokens.toGroup.available.firstOrNull {
                it.currencyStatus.currency.id.value == id
            }
        }
        analyticsEventHandler.send(
            event = SwapEvents.SearchTokenClicked(currencySymbol = foundToken?.currencyStatus?.currency?.symbol),
        )

        if (foundToken != null) {
            val fromToken: CryptoCurrencyStatus
            val toToken: CryptoCurrencyStatus
            if (isOrderReversed) {
                fromToken = foundToken.currencyStatus
                toToken = initialCryptoCurrencyStatus
            } else {
                fromToken = initialCryptoCurrencyStatus
                toToken = foundToken.currencyStatus
            }
            dataState = dataState.copy(
                fromCryptoCurrency = fromToken,
                toCryptoCurrency = toToken,
            )
            startLoadingQuotes(
                fromToken = fromToken.currency,
                toToken = toToken.currency,
                amount = lastAmount.value,
                toProvidersList = findSwapProviders(fromToken, toToken)
            )
            swapRouter.openScreen(SwapNavScreen.Main)
        }
    }

    private fun onChangeCardsClicked() {
        val newFromToken = dataState.toCryptoCurrency
        val newToToken = dataState.fromCryptoCurrency
        if (newFromToken != null && newToToken != null) {
            dataState = dataState.copy(
                fromCryptoCurrency = newFromToken,
                toCryptoCurrency = newToToken,
            )
            isOrderReversed = !isOrderReversed
            val decimals = newFromToken.currency.decimals
            lastAmount.value = cutAmountWithDecimals(decimals, lastAmount.value)
            uiState = stateBuilder.updateSwapAmount(
                uiState,
                inputNumberFormatter.formatWithThousands(lastAmount.value, decimals),
            )
            startLoadingQuotes(
                fromToken = newFromToken.currency,
                toToken = newToToken.currency,
                amount = lastAmount.value,
                toProvidersList = findSwapProviders(newFromToken, newToToken)
            )
        }
    }

    private fun onAmountChanged(value: String) {
        val fromToken = dataState.fromCryptoCurrency
        val toToken = dataState.toCryptoCurrency
        if (fromToken != null && toToken != null) {
            val decimals = fromToken.currency.decimals
            val cutValue = cutAmountWithDecimals(decimals, value)
            lastAmount.value = cutValue
            uiState =
                stateBuilder.updateSwapAmount(uiState, inputNumberFormatter.formatWithThousands(cutValue, decimals))
            amountDebouncer.debounce(viewModelScope, DEBOUNCE_AMOUNT_DELAY) {
                startLoadingQuotes(
                    fromToken = fromToken.currency,
                    toToken = toToken.currency,
                    amount = lastAmount.value,
                    toProvidersList = findSwapProviders(fromToken, toToken)
                )
            }
        }
    }

    private fun onMaxAmountClicked() {
        dataState.fromCryptoCurrency?.let {
            val balance = swapInteractor.getTokenBalance(initialCryptoCurrency.network.id.value, it.currency)
            onAmountChanged(balance.formatToUIRepresentation())
        }
    }

    @Suppress("UnusedPrivateMember")
    private fun onAmountSelected(selected: Boolean) {
        if (selected) {
            analyticsEventHandler.send(SwapEvents.SendTokenBalanceClicked)
        }
    }

    private fun cutAmountWithDecimals(maxDecimals: Int, amount: String): String {
        return inputNumberFormatter.getValidatedNumberWithFixedDecimals(amount, maxDecimals)
    }

    private fun makeDefaultAlert() {
        uiState = stateBuilder.addAlert(uiState) {
            uiState = stateBuilder.clearAlert(uiState)
        }
    }

    @Suppress("LongMethod")
    private fun createUiActions(): UiActions {
        return UiActions(
            onSearchEntered = { onSearchEntered(it) },
            onTokenSelected = { onTokenSelect(it) },
            onAmountChanged = { onAmountChanged(it) },
            onSwapClick = {
                onSwapClick()
                val sendTokenSymbol = dataState.fromCurrency?.symbol
                val receiveTokenSymbol = dataState.toCurrency?.symbol
                if (sendTokenSymbol != null && receiveTokenSymbol != null) {
                    analyticsEventHandler.send(
                        SwapEvents.ButtonSwapClicked(
                            sendToken = sendTokenSymbol,
                            receiveToken = receiveTokenSymbol,
                        ),
                    )
                }
            },
            onGivePermissionClick = {
                givePermissionsToSwap()
                val sendTokenSymbol = dataState.fromCurrency?.symbol
                val receiveTokenSymbol = dataState.toCurrency?.symbol
                if (sendTokenSymbol != null && receiveTokenSymbol != null) {
                    analyticsEventHandler.send(
                        SwapEvents.ButtonPermissionApproveClicked(
                            sendToken = sendTokenSymbol,
                            receiveToken = receiveTokenSymbol,
                        ),
                    )
                }
            },
            onChangeCardsClicked = {
                onChangeCardsClicked()
                analyticsEventHandler.send(SwapEvents.ButtonSwipeClicked)
            },
            onBackClicked = { onSearchEntered("") },
            onMaxAmountSelected = { onMaxAmountClicked() },
            openPermissionBottomSheet = {
                singleTaskScheduler.cancelTask()
                analyticsEventHandler.send(SwapEvents.ButtonGivePermissionClicked)
            },
            hidePermissionBottomSheet = {
                startLoadingQuotesFromLastState()
                analyticsEventHandler.send(SwapEvents.ButtonPermissionCancelClicked)
            },
            onAmountSelected = { onAmountSelected(it) },
            onChangeApproveType = { approveType ->
                uiState = stateBuilder.updateApproveType(uiState, approveType)
            },
            onSelectItemFee = { feeItem ->
                dataState = dataState.copy(selectedFee = feeItem.data)
                val spendAmount = dataState.amount?.let { amount ->
                    val fromToken = dataState.fromCryptoCurrency ?: return@let null
                    swapInteractor.getSwapAmountForToken(amount, fromToken.currency)
                } ?: dataState.approveDataModel?.fromTokenAmount
                spendAmount ?: return@UiActions
                val fromToken = dataState.fromCryptoCurrency ?: return@UiActions
                viewModelScope.launch(dispatchers.io) {
                    val isFeeEnough = swapInteractor.checkFeeIsEnough(
                        fee = feeItem.data.feeValue,
                        spendAmount = spendAmount,
                        networkId = dataState.networkId,
                        fromToken = fromToken.currency,
                    )
                    uiState = stateBuilder.updateFeeSelectedItem(uiState, feeItem, isFeeEnough)
                }
            },
            onClickFee = {},
            onSelectFeeType = {},
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

    private fun findSwapProviders(fromToken: CryptoCurrencyStatus, toToken: CryptoCurrencyStatus): List<SwapProvider> {
        val groupToFind = if (isOrderReversed) {
            dataState.tokensDataState?.fromGroup
        } else {
            dataState.tokensDataState?.toGroup
        } ?: return emptyList()

        val idToFind = if (isOrderReversed) {
            fromToken.currency.id.value
        } else {
            toToken.currency.id.value
        }

        return groupToFind.available.find { idToFind == it.currencyStatus.currency.id.value }?.providers ?: emptyList()
    }

    companion object {
        private const val loggingTag = "SwapViewModel"
        private const val INITIAL_AMOUNT = ""
        private const val UPDATE_DELAY = 10000L
        private const val DEBOUNCE_AMOUNT_DELAY = 1000L
    }
}
