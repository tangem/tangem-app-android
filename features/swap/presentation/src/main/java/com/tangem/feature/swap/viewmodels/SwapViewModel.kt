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
import com.tangem.feature.swap.domain.models.DataError
import com.tangem.feature.swap.domain.models.ExpressException
import com.tangem.feature.swap.domain.models.domain.PermissionOptions
import com.tangem.feature.swap.domain.models.domain.SwapDataModel
import com.tangem.feature.swap.domain.models.domain.SwapProvider
import com.tangem.feature.swap.domain.models.formatToUIRepresentation
import com.tangem.feature.swap.domain.models.ui.*
import com.tangem.feature.swap.models.*
import com.tangem.feature.swap.presentation.SwapFragment
import com.tangem.feature.swap.router.SwapNavScreen
import com.tangem.feature.swap.router.SwapRouter
import com.tangem.feature.swap.ui.StateBuilder
import com.tangem.utils.coroutines.*
import com.tangem.utils.isNullOrZero
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject
import kotlin.math.absoluteValue
import kotlin.properties.Delegates

typealias SuccessLoadedSwapData = Map<SwapProvider, SwapState.QuotesLoadedState>

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
    private val singleTaskScheduler = SingleTaskScheduler<Map<SwapProvider, SwapState>>()

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
            onSelectTokenClick = {
                router.openScreen(SwapNavScreen.SelectToken)
                sendSelectTokenScreenOpenedEvent()
            },
            onSuccess = {
                router.openScreen(SwapNavScreen.Success)
            },
        )
    }

    private fun sendSelectTokenScreenOpenedEvent() {
        val isAnyAvailableTokensTo = dataState.tokensDataState?.toGroup?.available?.isNotEmpty() ?: false
        val isAnyAvailableTokensFrom = dataState.tokensDataState?.fromGroup?.available?.isNotEmpty() ?: false
        val isAnyAvailableTokens = isAnyAvailableTokensTo || isAnyAvailableTokensFrom
        analyticsEventHandler.send(SwapEvents.ChooseTokenScreenOpened(availableTokens = isAnyAvailableTokens))
    }

    private fun initTokens() {
        viewModelScope.launch(dispatchers.main) {
            runCatching(dispatchers.io) {
                swapInteractor.getTokensDataState(initialCryptoCurrency)
            }.onSuccess { state ->
                updateTokensState(state)
                applyInitialTokenChoice(
                    state,
                    swapInteractor.selectInitialCurrencyToSwap(
                        initialCryptoCurrency,
                        state,
                    ),
                )
            }.onFailure {
                Timber.tag(loggingTag).e(it)

                applyInitialTokenChoice(
                    state = TokensDataStateExpress.EMPTY,
                    selectedCurrency = null,
                )

                uiState =
                    stateBuilder.createInitialErrorState(
                        uiState,
                        (it as? ExpressException)?.dataError?.code ?: DataError.UnknownError.code,
                    ) {
                        uiState = stateBuilder.createInitialLoadingState(
                            initialCurrency = initialCryptoCurrency,
                            networkInfo = blockchainInteractor.getBlockchainInfo(
                                initialCryptoCurrency.network.backendId,
                            ),
                        )
                        initTokens()
                    }
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
            analyticsEventHandler.send(SwapEvents.NoticeNoAvailableTokensToSwap)
            uiState = stateBuilder.createNoAvailableTokensToSwapState(
                uiStateHolder = uiState,
                fromToken = fromCurrencyStatus,
            )
        } else {
            startLoadingQuotes(
                fromToken = fromCurrencyStatus,
                toToken = selectedCurrency,
                amount = lastAmount.value,
                toProvidersList = findSwapProviders(fromCurrencyStatus, selectedCurrency),
            )
        }
    }

    private fun updateTokensState(dataState: TokensDataStateExpress) {
        val tokensDataState = if (!isOrderReversed) dataState.toGroup else dataState.fromGroup
        uiState = stateBuilder.addTokensToState(
            uiState = uiState,
            tokensDataState = tokensDataState,
        )
    }

    private fun startLoadingQuotes(
        fromToken: CryptoCurrencyStatus,
        toToken: CryptoCurrencyStatus,
        amount: String,
        toProvidersList: List<SwapProvider>,
        isSilent: Boolean = false,
    ) {
        singleTaskScheduler.cancelTask()
        if (!isSilent) {
            uiState = stateBuilder.createQuotesLoadingState(
                uiState,
                fromToken.currency,
                toToken.currency,
                initialCryptoCurrency.id.value,
            )
        }
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

    private fun startLoadingQuotesFromLastState(isSilent: Boolean = false) {
        val fromCurrency = dataState.fromCryptoCurrency
        val toCurrency = dataState.toCryptoCurrency
        val amount = dataState.amount
        if (fromCurrency != null && toCurrency != null && amount != null) {
            startLoadingQuotes(
                fromToken = fromCurrency,
                toToken = toCurrency,
                amount = amount,
                isSilent = isSilent,
                toProvidersList = findSwapProviders(fromCurrency, toCurrency),
            )
        }
    }

    private fun loadQuotesTask(
        fromToken: CryptoCurrencyStatus,
        toToken: CryptoCurrencyStatus,
        amount: String,
        toProvidersList: List<SwapProvider>,
    ): PeriodicTask<Map<SwapProvider, SwapState>> {
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
                    )
                }
            },
            onSuccess = { providersState ->
                if (providersState.isNotEmpty()) {
                    val (provider, state) = updateLoadedQuotes(providersState)
                    setupLoadedState(provider, state, fromToken)
                    val successStates = providersState
                        .getLastLoadedSuccessStates()
                    val pricesLowerBest = getPricesLowerBest(successStates)
                    uiState = stateBuilder.updateProvidersBottomSheetContent(
                        uiState = uiState,
                        pricesLowerBest = pricesLowerBest,
                        tokenSwapInfoForProviders = successStates.entries
                            .associate { it.key.providerId to it.value.toTokenInfo },
                    )
                } else {
                    Timber.e("Accidentally empty quotes list")
                }
            },
            onError = {
                Timber.e("Error when loading quotes: $it")
                uiState = stateBuilder.addWarning(uiState, null) { startLoadingQuotesFromLastState() }
            },
        )
    }

    private fun setupLoadedState(provider: SwapProvider, state: SwapState, fromToken: CryptoCurrencyStatus) {
        when (state) {
            is SwapState.QuotesLoadedState -> {
                fillLoadedDataState(state, state.permissionState, state.swapDataModel)
                val loadedStates = dataState.lastLoadedSwapStates.getLastLoadedSuccessStates()
                val bestRatedProviderId = findBestQuoteProvider(loadedStates)?.providerId ?: provider.providerId
                uiState = stateBuilder.createQuotesLoadedState(
                    uiStateHolder = uiState,
                    quoteModel = state,
                    fromToken = fromToken.currency,
                    swapProvider = provider,
                    bestRatedProviderId = bestRatedProviderId,
                    isManyProviders = dataState.lastLoadedSwapStates.size > 1,
                    selectedFeeType = dataState.selectedFee?.feeType ?: FeeType.NORMAL,
                )
                if (uiState.warnings.any { it is SwapWarning.UnableToCoverFeeWarning }) {
                    analyticsEventHandler.send(
                        SwapEvents.NoticeNotEnoughFee(
                            token = initialCryptoCurrency.symbol,
                            blockchain = fromToken.currency.network.name,
                        ),
                    )
                }
            }
            is SwapState.EmptyAmountState -> {
                uiState = stateBuilder.createQuotesEmptyAmountState(
                    uiStateHolder = uiState,
                    emptyAmountState = state,
                )
            }
            is SwapState.SwapError -> {
                if (state.error is DataError.UnknownError) {
                    singleTaskScheduler.cancelTask()
                }
                uiState = stateBuilder.createQuotesErrorState(
                    uiStateHolder = uiState,
                    swapProvider = provider,
                    fromToken = state.fromTokenInfo,
                    toToken = dataState.toCryptoCurrency,
                    dataError = state.error,
                )
                sendErrorAnalyticsEvent(state.error, provider)
            }
        }
    }

    private fun sendErrorAnalyticsEvent(error: DataError, provider: SwapProvider) {
        analyticsEventHandler.send(
            SwapEvents.NoticeProviderError(
                token = initialCryptoCurrency.symbol,
                provider = provider,
                errorCode = error.code,
            ),
        )
    }

    private fun updateLoadedQuotes(state: Map<SwapProvider, SwapState>): Pair<SwapProvider, SwapState> {
        val nonEmptyStates = state.filter { it.value !is SwapState.EmptyAmountState }
        val selectedSwapProvider = if (nonEmptyStates.isNotEmpty()) {
            selectProvider(state)
        } else {
            null
        }
        dataState = dataState.copy(
            selectedProvider = selectedSwapProvider,
            lastLoadedSwapStates = state,
        )
        selectedSwapProvider?.let {
            return nonEmptyStates.entries.first { it.key == selectedSwapProvider }.toPair()
        }
        return state.entries.first().toPair()
    }

    private fun selectProvider(state: Map<SwapProvider, SwapState>): SwapProvider {
        val stateSuccess = state.getLastLoadedSuccessStates()
        return if (stateSuccess.isNotEmpty()) {
            val currentSelected = dataState.selectedProvider
            if (currentSelected != null && stateSuccess.keys.contains(currentSelected)) {
                currentSelected
            } else {
                findBestQuoteProvider(stateSuccess) ?: stateSuccess.keys.first()
            }
        } else {
            state.keys.first()
        }
    }

    private fun fillLoadedDataState(
        state: SwapState.QuotesLoadedState,
        permissionState: PermissionDataState,
        swapDataModel: SwapDataModel?,
    ) {
        dataState = if (permissionState is PermissionDataState.PermissionReadyForRequest) {
            dataState.copy(
                approveDataModel = permissionState.requestApproveData,
                approveType = dataState.approveType ?: ApproveType.UNLIMITED,
            )
        } else {
            dataState.copy(
                swapDataModel = swapDataModel,
                selectedFee = selectDefaultFee(state),
            )
        }
    }

    private fun selectDefaultFee(state: SwapState.QuotesLoadedState): TxFee? {
        return dataState.selectedFee
            ?: when (val txFee = state.txFee) {
                TxFeeState.Empty -> null
                is TxFeeState.MultipleFeeState -> {
                    txFee.normalFee
                }
                is TxFeeState.SingleFeeState -> {
                    txFee.fee
                }
            }
    }

    @Suppress("LongMethod")
    private fun onSwapClick() {
        singleTaskScheduler.cancelTask()
        uiState = stateBuilder.createSwapInProgressState(uiState)
        val provider = requireNotNull(dataState.selectedProvider) { "Selected provider is null" }
        val lastLoadedQuotesState = dataState.lastLoadedSwapStates[provider] as? SwapState.QuotesLoadedState
        if (lastLoadedQuotesState == null) {
            Timber.e("Last loaded quotes state is null")
            return
        }
        viewModelScope.launch(dispatchers.main) {
            runCatching(dispatchers.io) {
                swapInteractor.onSwap(
                    swapProvider = provider,
                    networkId = dataState.networkId,
                    swapData = dataState.swapDataModel,
                    currencyToSend = requireNotNull(dataState.fromCryptoCurrency),
                    currencyToGet = requireNotNull(dataState.toCryptoCurrency),
                    amountToSwap = requireNotNull(dataState.amount),
                    includeFeeInAmount = lastLoadedQuotesState.preparedSwapConfigState.includeFeeInAmount,
                    fee = requireNotNull(dataState.selectedFee),
                )
            }
                .onSuccess {
                    when (it) {
                        is TxState.TxSent -> {
                            val url = blockchainInteractor.getExplorerTransactionLink(
                                networkId = dataState.networkId,
                                txAddress = it.txAddress,
                            )
                            uiState = stateBuilder.createSuccessState(
                                uiState = uiState,
                                txState = it,
                                dataState = dataState,
                                txUrl = url,
                                onExploreClick = {
                                    val txHash = it.txAddress
                                    if (txHash.isNotEmpty()) {
                                        swapRouter.openUrl(url)
                                    }
                                    analyticsEventHandler.send(
                                        event = SwapEvents.ButtonExplore(initialCryptoCurrency.symbol),
                                    )
                                },
                                onStatusClick = {
                                    val txExternalUrl = it.txExternalUrl
                                    if (!txExternalUrl.isNullOrBlank()) {
                                        swapRouter.openUrl(txExternalUrl)
                                        analyticsEventHandler.send(
                                            event = SwapEvents.ButtonStatus(initialCryptoCurrency.symbol),
                                        )
                                    }
                                },
                            )
                            sendSuccessEvent()

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
                    Timber.e(it)
                    startLoadingQuotesFromLastState()
                    makeDefaultAlert()
                }
        }
    }

    private fun sendSuccessEvent() {
        val provider = dataState.selectedProvider ?: return
        val fee = dataState.selectedFee?.feeType ?: return
        val sendToken = dataState.fromCryptoCurrency?.currency?.symbol ?: return
        val toToken = dataState.toCryptoCurrency?.currency?.symbol ?: return

        analyticsEventHandler.send(
            SwapEvents.SwapInProgressScreen(
                provider = provider,
                commission = fee,
                sendToken = sendToken,
                receiveToken = toToken,
            ),
        )
    }

    private fun givePermissionsToSwap() {
        viewModelScope.launch(dispatchers.main) {
            runCatching(dispatchers.io) {
                val feeForPermission = when (val fee = dataState.approveDataModel?.fee) {
                    TxFeeState.Empty -> error("Fee should not be Empty")
                    is TxFeeState.MultipleFeeState -> fee.priorityFee
                    is TxFeeState.SingleFeeState -> fee.fee
                    null -> error("Fee should not be null")
                }
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
                        approveType = requireNotNull(dataState.approveType) {
                            "uiState.permissionState should not be null"
                        }.toDomainApproveType(),
                        txFee = feeForPermission,
                        spenderAddress = requireNotNull(dataState.approveDataModel?.spenderAddress) {
                            "dataState.approveDataModel.spenderAddress shouldn't be null"
                        },
                    ),
                )
            }
                .onSuccess {
                    when (it) {
                        is TxState.TxSent -> {
                            uiState = stateBuilder.loadingPermissionState(uiState)
                            uiState = stateBuilder.dismissBottomSheet(uiState)
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
        viewModelScope.launch(dispatchers.io) {
            val tokenDataState = dataState.tokensDataState ?: return@launch
            val group = if (isOrderReversed) {
                tokenDataState.fromGroup
            } else {
                tokenDataState.toGroup
            }
            val available = group.available.filter {
                it.currencyStatus.currency.name.contains(searchQuery, ignoreCase = true)
            }
            val unavailable = group.unavailable.filter {
                it.currencyStatus.currency.name.contains(searchQuery, ignoreCase = true)
            }
            val filteredTokenDataState = if (isOrderReversed) {
                tokenDataState.copy(
                    fromGroup = tokenDataState.fromGroup.copy(
                        available = available,
                        unavailable = unavailable,
                    ),
                )
            } else {
                tokenDataState.copy(
                    toGroup = tokenDataState.toGroup.copy(
                        available = available,
                        unavailable = unavailable,
                    ),
                )
            }
            updateTokensState(filteredTokenDataState)
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
        foundToken?.currencyStatus?.currency?.symbol?.let {
            analyticsEventHandler.send(SwapEvents.ChooseTokenScreenResult(tokenChosen = true, token = it))
        }

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
                selectedProvider = null,
            )
            startLoadingQuotes(
                fromToken = fromToken,
                toToken = toToken,
                amount = lastAmount.value,
                toProvidersList = findSwapProviders(fromToken, toToken),
            )
            swapRouter.openScreen(SwapNavScreen.Main)
            updateTokensState(tokens)
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
                fromToken = newFromToken,
                toToken = newToToken,
                amount = lastAmount.value,
                toProvidersList = findSwapProviders(newFromToken, newToToken),
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
                    fromToken = fromToken,
                    toToken = toToken,
                    amount = lastAmount.value,
                    toProvidersList = findSwapProviders(fromToken, toToken),
                )
            }
        }
    }

    private fun onMaxAmountClicked() {
        dataState.fromCryptoCurrency?.let {
            val balance = swapInteractor.getTokenBalance(it)
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

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    private fun createUiActions(): UiActions {
        return UiActions(
            onSearchEntered = { onSearchEntered(it) },
            onTokenSelected = { onTokenSelect(it) },
            onAmountChanged = { onAmountChanged(it) },
            onSwapClick = {
                onSwapClick()
                val sendTokenSymbol = dataState.fromCryptoCurrency?.currency?.symbol
                val receiveTokenSymbol = dataState.toCryptoCurrency?.currency?.symbol
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
                val sendTokenSymbol = dataState.fromCryptoCurrency?.currency?.symbol
                val receiveTokenSymbol = dataState.toCryptoCurrency?.currency?.symbol
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
            onBackClicked = {
                val bottomSheet = uiState.bottomSheetConfig
                if (bottomSheet != null && bottomSheet.isShow) {
                    uiState = stateBuilder.dismissBottomSheet(uiState)
                } else {
                    if (swapRouter.currentScreen == SwapNavScreen.SelectToken) {
                        analyticsEventHandler.send(SwapEvents.ChooseTokenScreenResult(tokenChosen = false))
                    }
                    swapRouter.back()
                }
                onSearchEntered("")
            },
            onMaxAmountSelected = { onMaxAmountClicked() },
            openPermissionBottomSheet = {
                singleTaskScheduler.cancelTask()
                analyticsEventHandler.send(SwapEvents.ButtonGivePermissionClicked)
                uiState = stateBuilder.showPermissionBottomSheet(uiState) {
                    startLoadingQuotesFromLastState(isSilent = true)
                    analyticsEventHandler.send(SwapEvents.ButtonPermissionCancelClicked)
                    uiState = stateBuilder.dismissBottomSheet(uiState)
                }
            },
            onAmountSelected = { onAmountSelected(it) },
            onChangeApproveType = { approveType ->
                uiState = stateBuilder.updateApproveType(uiState, approveType)
                dataState = dataState.copy(approveType = approveType)
            },
            onClickFee = {
                val selectedFee = dataState.selectedFee?.feeType ?: FeeType.NORMAL
                val txFeeState = dataState.getCurrentLoadedSwapState()?.txFee as? TxFeeState.MultipleFeeState
                    ?: return@UiActions
                uiState = stateBuilder.showSelectFeeBottomSheet(
                    uiState = uiState,
                    selectedFee = selectedFee,
                    txFeeState = txFeeState,
                ) {
                    uiState = stateBuilder.dismissBottomSheet(uiState)
                }
            },
            onSelectFeeType = {
                val state = dataState.getCurrentLoadedSwapState() ?: return@UiActions
                val fromToken = dataState.fromCryptoCurrency ?: return@UiActions
                val amountToSwap = dataState.amount ?: return@UiActions
                val selectedProvider = dataState.selectedProvider ?: return@UiActions
                uiState = stateBuilder.dismissBottomSheet(uiState)
                dataState = dataState.copy(selectedFee = it)
                viewModelScope.launch(dispatchers.io) {
                    val updatedState = swapInteractor.updateQuotesStateWithSelectedFee(
                        state = state,
                        selectedFee = it.feeType,
                        fromToken = fromToken,
                        amountToSwap = amountToSwap,
                        networkId = dataState.networkId,
                    )
                    setupLoadedState(selectedProvider, updatedState, fromToken)
                }
            },
            onProviderClick = { providerId ->
                analyticsEventHandler.send(SwapEvents.ProviderClicked)
                val states = dataState.lastLoadedSwapStates.getLastLoadedSuccessStates()
                val pricesLowerBest = getPricesLowerBest(states)
                val unavailableProviders = getUnavailableProvidersFor(dataState.lastLoadedSwapStates)
                uiState = stateBuilder.showSelectProviderBottomSheet(
                    uiState = uiState,
                    selectedProviderId = providerId,
                    pricesLowerBest = pricesLowerBest,
                    unavailableProviders = unavailableProviders,
                    providersStates = dataState.lastLoadedSwapStates,
                ) { uiState = stateBuilder.dismissBottomSheet(uiState) }
            },
            onProviderSelect = {
                val provider = findAndSelectProvider(it)
                val swapState = dataState.lastLoadedSwapStates[provider]
                val fromToken = dataState.fromCryptoCurrency
                if (provider != null && swapState != null && fromToken != null) {
                    analyticsEventHandler.send(SwapEvents.ProviderChosen(provider))
                    uiState = stateBuilder.dismissBottomSheet(uiState)
                    setupLoadedState(
                        provider = provider,
                        state = swapState,
                        fromToken = fromToken,
                    )
                }
            },
            onBuyClick = {
                swapInteractor.getSelectedWallet()?.let {
                    swapRouter.openTokenDetails(it.walletId, swapInteractor.getNativeToken(dataState.networkId))
                }
            },
            onRetryClick = {
                startLoadingQuotesFromLastState()
            },
        )
    }

    private fun findAndSelectProvider(providerId: String): SwapProvider? {
        val selectedProvider = dataState.lastLoadedSwapStates.keys.firstOrNull { it.providerId == providerId }
        if (selectedProvider != null) {
            dataState = dataState.copy(
                selectedProvider = selectedProvider,
            )
        }
        return selectedProvider
    }

    private fun findBestQuoteProvider(state: SuccessLoadedSwapData): SwapProvider? {
        // finding best quotes
        return state.minByOrNull {
            if (!it.value.fromTokenInfo.amountFiat.isNullOrZero() &&
                !it.value.toTokenInfo.amountFiat.isNullOrZero()
            ) {
                it.value.fromTokenInfo.amountFiat.divide(
                    it.value.toTokenInfo.amountFiat,
                    it.value.toTokenInfo.cryptoCurrencyStatus.currency.decimals,
                    RoundingMode.HALF_UP,
                )
            } else {
                BigDecimal.ZERO
            }
        }?.key
    }

    private fun getPricesLowerBest(state: SuccessLoadedSwapData): Map<String, Float> {
        val bestRateEntry = state.maxByOrNull { it.value.toTokenInfo.tokenAmount.value } ?: return emptyMap()
        val bestRate = bestRateEntry.value.toTokenInfo.tokenAmount.value
        val hundredPercent = BigDecimal("100")
        return state.entries.mapNotNull {
            if (it.key != bestRateEntry.key) {
                val amount = it.value.toTokenInfo.tokenAmount.value
                val percentDiff = BigDecimal.ONE.minus(
                    amount.divide(bestRate, RoundingMode.HALF_UP),
                ).multiply(hundredPercent)
                it.key.providerId to percentDiff.setScale(2, RoundingMode.HALF_UP).toFloat().absoluteValue
            } else {
                null
            }
        }.toMap()
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

    private fun getAllProviders(): List<SwapProvider> {
        return dataState.tokensDataState?.allProviders ?: emptyList()
    }

    private fun getUnavailableProvidersFor(state: Map<SwapProvider, SwapState>): List<SwapProvider> {
        val availableProviders = state.keys.map { it.providerId }
        return getAllProviders().filterNot { availableProviders.contains(it.providerId) }
    }

    private fun Map<SwapProvider, SwapState>.getLastLoadedSuccessStates(): SuccessLoadedSwapData {
        return this
            .filter { it.value is SwapState.QuotesLoadedState }
            .mapValues { it.value as SwapState.QuotesLoadedState }
    }

    companion object {
        private const val loggingTag = "SwapViewModel"
        private const val INITIAL_AMOUNT = ""
        private const val UPDATE_DELAY = 10000L
        private const val DEBOUNCE_AMOUNT_DELAY = 1000L
    }
}
