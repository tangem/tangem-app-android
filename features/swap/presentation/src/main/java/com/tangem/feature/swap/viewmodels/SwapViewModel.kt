package com.tangem.feature.swap.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.*
import com.tangem.common.Provider
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.ui.utils.InputNumberFormatter
import com.tangem.domain.balancehiding.IsBalanceHiddenUseCase
import com.tangem.domain.balancehiding.ListenToFlipsUseCase
import com.tangem.domain.tokens.model.Network
import com.tangem.feature.swap.analytics.SwapEvents
import com.tangem.feature.swap.domain.BlockchainInteractor
import com.tangem.feature.swap.domain.SwapInteractor
import com.tangem.feature.swap.domain.models.domain.Currency
import com.tangem.feature.swap.domain.models.domain.PermissionOptions
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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
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
    private val isBalanceHiddenUseCase: IsBalanceHiddenUseCase,
    private val listenToFlipsUseCase: ListenToFlipsUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel(), DefaultLifecycleObserver {

    private val currency = Json.decodeFromString<Currency>(
        savedStateHandle[SwapFragment.CURRENCY_BUNDLE_KEY]
            ?: error("no expected parameter Currency found"),
    )
    private val derivationPath = savedStateHandle.get<String>(SwapFragment.DERIVATION_PATH)
    private val network = savedStateHandle.get<Network>(SwapFragment.NETWORK)

    private var isBalanceHidden = true

    private val stateBuilder = StateBuilder(
        actions = createUiActions(),
        isBalanceHiddenProvider = Provider { isBalanceHidden },
    )

    private val inputNumberFormatter =
        InputNumberFormatter(NumberFormat.getInstance(Locale.getDefault()) as DecimalFormat)
    private val amountDebouncer = Debouncer()
    private val singleTaskScheduler = SingleTaskScheduler<SwapState>()

    private var dataState by mutableStateOf(SwapProcessDataState(networkId = currency.networkId))

    var uiState: SwapStateHolder by mutableStateOf(
        stateBuilder.createInitialLoadingState(
            initialCurrency = currency,
            networkInfo = blockchainInteractor.getBlockchainInfo(currency.networkId),
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
        swapInteractor.initDerivationPathAndNetwork(derivationPath, network)
        initTokens(currency)
    }

    override fun onCreate(owner: LifecycleOwner) {
        isBalanceHiddenUseCase()
            .flowWithLifecycle(owner.lifecycle)
            .onEach { hidden ->
                isBalanceHidden = hidden
                withContext(dispatchers.main) {
                    uiState = stateBuilder.updateBalanceHiddenState(uiState, isBalanceHidden)
                }
            }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            listenToFlipsUseCase()
                .flowWithLifecycle(owner.lifecycle)
                .collect()
        }
    }

    override fun onCleared() {
        singleTaskScheduler.cancelTask()
        super.onCleared()
    }

    fun onScreenOpened() {
        analyticsEventHandler.send(SwapEvents.SwapScreenOpened(currency.symbol))
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

    private fun initTokens(currency: Currency) {
        viewModelScope.launch(dispatchers.main) {
            runCatching(dispatchers.io) {
                swapInteractor.initTokensToSwap(currency)
            }
                .onSuccess { state ->
                    dataState = dataState.copy(
                        fromCurrency = state.preselectTokens.fromToken,
                        toCurrency = state.preselectTokens.toToken,
                    )
                    updateTokensState(dataState = state.foundTokensState)
                    startLoadingQuotes(
                        fromToken = state.preselectTokens.fromToken,
                        toToken = state.preselectTokens.toToken,
                        amount = lastAmount.value,
                    )
                }
                .onFailure {
                    Timber.e(it)
                }
        }
    }

    private fun updateTokensState(dataState: FoundTokensState) {
        uiState = stateBuilder.addTokensToState(
            uiState = uiState,
            dataState = dataState,
            networkInfo = blockchainInteractor.getBlockchainInfo(currency.networkId),
        )
    }

    private fun startLoadingQuotes(fromToken: Currency, toToken: Currency, amount: String) {
        singleTaskScheduler.cancelTask()
        uiState = stateBuilder.createQuotesLoadingState(uiState, fromToken, toToken, currency.id)
        singleTaskScheduler.scheduleTask(
            viewModelScope,
            loadQuotesTask(
                fromToken = fromToken,
                toToken = toToken,
                amount = amount,
            ),
        )
    }

    private fun startLoadingQuotesFromLastState() {
        val fromCurrency = dataState.fromCurrency
        val toCurrency = dataState.toCurrency
        val amount = dataState.amount
        if (fromCurrency != null && toCurrency != null && amount != null) {
            startLoadingQuotes(fromCurrency, toCurrency, amount)
        }
    }

    private fun loadQuotesTask(fromToken: Currency, toToken: Currency, amount: String): PeriodicTask<SwapState> {
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
                        amountToSwap = amount,
                    )
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
                    currencyToSend = requireNotNull(dataState.fromCurrency),
                    currencyToGet = requireNotNull(dataState.toCurrency),
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
                        forTokenContractAddress = (dataState.fromCurrency as? Currency.NonNativeToken)?.contractAddress
                            ?: "",
                        fromToken = requireNotNull(dataState.fromCurrency) {
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
                    updateTokensState(it)
                }
                .onFailure { }
        }
    }

    private fun onTokenSelect(id: String) {
        val foundToken = swapInteractor.findTokenById(id)

        analyticsEventHandler.send(
            event = SwapEvents.SearchTokenClicked(currencySymbol = foundToken?.symbol),
        )

        if (foundToken != null) {
            val fromToken: Currency
            val toToken: Currency
            if (isOrderReversed) {
                fromToken = foundToken
                toToken = currency
            } else {
                fromToken = currency
                toToken = foundToken
            }
            dataState = dataState.copy(
                fromCurrency = fromToken,
                toCurrency = toToken,
            )
            startLoadingQuotes(fromToken, toToken, lastAmount.value)
            swapRouter.openScreen(SwapNavScreen.Main)
        }
    }

    private fun onChangeCardsClicked() {
        val newFromToken = dataState.toCurrency
        val newToToken = dataState.fromCurrency
        if (newFromToken != null && newToToken != null) {
            dataState = dataState.copy(
                fromCurrency = newFromToken,
                toCurrency = newToToken,
            )
            isOrderReversed = !isOrderReversed
            val decimals = blockchainInteractor.getTokenDecimals(newFromToken)
            lastAmount.value = cutAmountWithDecimals(decimals, lastAmount.value)
            uiState = stateBuilder.updateSwapAmount(
                uiState,
                inputNumberFormatter.formatWithThousands(lastAmount.value, decimals),
            )
            startLoadingQuotes(newFromToken, newToToken, lastAmount.value)
        }
    }

    private fun onAmountChanged(value: String) {
        val fromToken = dataState.fromCurrency
        val toToken = dataState.toCurrency
        if (fromToken != null && toToken != null) {
            val decimals = blockchainInteractor.getTokenDecimals(fromToken)
            val cutValue = cutAmountWithDecimals(decimals, value)
            lastAmount.value = cutValue
            uiState =
                stateBuilder.updateSwapAmount(uiState, inputNumberFormatter.formatWithThousands(cutValue, decimals))
            amountDebouncer.debounce(viewModelScope, DEBOUNCE_AMOUNT_DELAY) {
                startLoadingQuotes(fromToken, toToken, lastAmount.value)
            }
        }
    }

    private fun onMaxAmountClicked() {
        dataState.fromCurrency?.let {
            val balance = swapInteractor.getTokenBalance(currency.networkId, it)
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
                analyticsEventHandler.send(SwapEvents.ButtonGivePermissionClicked)
            },
            hidePermissionBottomSheet = {
                analyticsEventHandler.send(SwapEvents.ButtonPermissionCancelClicked)
            },
            onAmountSelected = { onAmountSelected(it) },
            onChangeApproveType = { approveType ->
                uiState = stateBuilder.updateApproveType(uiState, approveType)
            },
            onSelectItemFee = { feeItem ->
                dataState = dataState.copy(selectedFee = feeItem.data)
                val spendAmount = dataState.swapDataModel?.swapModel?.fromTokenAmount ?: return@UiActions
                val fromToken = dataState.fromCurrency ?: return@UiActions
                viewModelScope.launch(dispatchers.io) {
                    val isFeeEnough = swapInteractor.checkFeeIsEnough(
                        fee = feeItem.data.feeValue,
                        spendAmount = spendAmount,
                        networkId = dataState.networkId,
                        fromToken = fromToken,
                    )
                    uiState = stateBuilder.updateFeeSelectedItem(uiState, feeItem, isFeeEnough)
                }
            },
        )
    }

    companion object {
        private const val INITIAL_AMOUNT = ""
        private const val UPDATE_DELAY = 10000L
        private const val DEBOUNCE_AMOUNT_DELAY = 1000L
    }
}
