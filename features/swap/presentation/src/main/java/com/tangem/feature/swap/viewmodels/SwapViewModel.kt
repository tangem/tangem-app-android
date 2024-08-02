package com.tangem.feature.swap.viewmodels

import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.*
import arrow.core.Either
import arrow.core.getOrElse
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.bundle.unbundle
import com.tangem.common.ui.bottomsheet.permission.state.ApproveType
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.utils.InputNumberFormatter
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.tokens.GetCryptoCurrencyStatusSyncUseCase
import com.tangem.domain.tokens.GetCurrencyStatusUpdatesUseCase
import com.tangem.domain.tokens.UpdateDelayedNetworkStatusUseCase
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.swap.analytics.SwapEvents
import com.tangem.feature.swap.domain.BlockchainInteractor
import com.tangem.feature.swap.domain.SwapInteractor
import com.tangem.feature.swap.domain.models.DataError
import com.tangem.feature.swap.domain.models.ExpressException
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.domain.*
import com.tangem.feature.swap.domain.models.formatToUIRepresentation
import com.tangem.feature.swap.domain.models.ui.*
import com.tangem.feature.swap.models.SwapStateHolder
import com.tangem.feature.swap.models.SwapWarning
import com.tangem.feature.swap.models.UiActions
import com.tangem.feature.swap.presentation.R
import com.tangem.feature.swap.router.SwapNavScreen
import com.tangem.feature.swap.router.SwapRouter
import com.tangem.feature.swap.ui.StateBuilder
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.*
import com.tangem.utils.isNullOrZero
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.NonCancellable
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
    private val updateDelayedCurrencyStatusUseCase: UpdateDelayedNetworkStatusUseCase,
    private val getCurrencyStatusUpdatesUseCase: GetCurrencyStatusUpdatesUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel(), DefaultLifecycleObserver {

    private val initialCryptoCurrency: CryptoCurrency = savedStateHandle.get<Bundle>(AppRoute.Swap.CURRENCY_BUNDLE_KEY)
        ?.unbundle(CryptoCurrency.serializer())
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

    private var dataState by mutableStateOf(SwapProcessDataState())

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

    private val isUserResolvableError: (SwapState) -> Boolean = {
        it is SwapState.SwapError &&
            (it.error is DataError.ExchangeTooSmallAmountError || it.error is DataError.ExchangeTooBigAmountError)
    }

    private val fromTokenBalanceJobHolder = JobHolder()
    private val toTokenBalanceJobHolder = JobHolder()

    val currentScreen: SwapNavScreen
        get() = swapRouter.currentScreen

    init {
        viewModelScope.launch(dispatchers.io) {
            swapInteractor.getSelectedWallet()?.let {
                val cryptoCurrencyStatus =
                    getCryptoCurrencyStatusUseCase(it.walletId, initialCryptoCurrency.id).getOrNull()
                if (cryptoCurrencyStatus == null) {
                    uiState = stateBuilder.addAlert(uiState = uiState, onClick = swapRouter::back)
                } else {
                    initialCryptoCurrencyStatus = cryptoCurrencyStatus
                    initTokens()
                }
            }
        }
    }

    override fun onCreate(owner: LifecycleOwner) {
        getBalanceHidingSettingsUseCase().flowWithLifecycle(owner.lifecycle).onEach {
            isBalanceHidden = it.isBalanceHidden
            withContext(dispatchers.main) {
                uiState = stateBuilder.updateBalanceHiddenState(uiState, isBalanceHidden)
            }
        }.launchIn(viewModelScope)
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

                val userWalletId = swapInteractor.getSelectedWallet()?.walletId ?: return@launch
                (dataState.fromCryptoCurrency?.currency as? CryptoCurrency.Coin)?.let {
                    subscribeToCoinBalanceUpdates(
                        userWalletId = userWalletId,
                        coin = it,
                        isFromCurrency = true,
                    )
                }

                (dataState.toCryptoCurrency?.currency as? CryptoCurrency.Coin)?.let {
                    subscribeToCoinBalanceUpdates(
                        userWalletId = userWalletId,
                        coin = it,
                        isFromCurrency = false,
                    )
                }
            }.onFailure {
                Timber.tag(loggingTag).e(it)

                applyInitialTokenChoice(
                    state = TokensDataStateExpress.EMPTY,
                    selectedCurrency = null,
                )

                uiState = stateBuilder.createInitialErrorState(
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

    private fun updateTokensState(tokenDataState: TokensDataStateExpress) {
        val tokensDataState = if (isOrderReversed) tokenDataState.fromGroup else tokenDataState.toGroup
        uiState = stateBuilder.addTokensToState(
            uiState = uiState,
            tokensDataState = tokensDataState,
            fromToken = dataState.fromCryptoCurrency?.currency ?: initialCryptoCurrency,
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
                    val successStates = providersState.getLastLoadedSuccessStates()
                    val pricesLowerBest = getPricesLowerBest(provider.providerId, successStates)
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
                    isNeedBestRateBadge = dataState.lastLoadedSwapStates.consideredProvidersStates().size > 1,
                    selectedFeeType = dataState.selectedFee?.feeType ?: FeeType.NORMAL,
                    isReverseSwapPossible = isReverseSwapPossible(),
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
                val toTokenStatus = dataState.toCryptoCurrency
                uiState = stateBuilder.createQuotesEmptyAmountState(
                    uiStateHolder = uiState,
                    emptyAmountState = state,
                    fromTokenStatus = fromToken,
                    toTokenStatus = toTokenStatus,
                    isReverseSwapPossible = isReverseSwapPossible(),
                )
            }
            is SwapState.SwapError -> {
                singleTaskScheduler.cancelTask()
                uiState = stateBuilder.createQuotesErrorState(
                    uiStateHolder = uiState,
                    swapProvider = provider,
                    fromToken = state.fromTokenInfo,
                    toToken = dataState.toCryptoCurrency,
                    dataError = state.error,
                    includeFeeInAmount = state.includeFeeInAmount,
                    isReverseSwapPossible = isReverseSwapPossible(),
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
        val consideredProviders = state.consideredProvidersStates()

        return if (consideredProviders.isNotEmpty()) {
            val currentSelected = dataState.selectedProvider
            if (currentSelected != null && consideredProviders.keys.contains(currentSelected)) {
                currentSelected
            } else {
                val successLoadedData = consideredProviders.getLastLoadedSuccessStates()
                val recommendedProvider = successLoadedData.keys.firstOrNull { it.isRecommended }
                val bestQuotesProvider = findBestQuoteProvider(successLoadedData)
                triggerPromoProviderEvent(recommendedProvider, bestQuotesProvider)
                recommendedProvider ?: bestQuotesProvider ?: consideredProviders.keys.first()
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
                selectedFee = updateOrSelectFee(state),
            )
        }
    }

    private fun updateOrSelectFee(state: SwapState.QuotesLoadedState): TxFee? {
        val selectedFeeType = dataState.selectedFee?.feeType ?: FeeType.NORMAL
        return when (val txFee = state.txFee) {
            TxFeeState.Empty -> null
            is TxFeeState.MultipleFeeState -> {
                if (selectedFeeType == FeeType.NORMAL) {
                    txFee.normalFee
                } else {
                    txFee.priorityFee
                }
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
        val fromCurrency = requireNotNull(dataState.fromCryptoCurrency)
        val fee = dataState.selectedFee

        if (fee == null) {
            makeDefaultAlert(resourceReference(R.string.swapping_fee_estimation_error_text))
            return
        }
        viewModelScope.launch(dispatchers.main) {
            runCatching(dispatchers.io) {
                swapInteractor.onSwap(
                    swapProvider = provider,
                    swapData = dataState.swapDataModel,
                    currencyToSend = fromCurrency,
                    currencyToGet = requireNotNull(dataState.toCryptoCurrency),
                    amountToSwap = requireNotNull(dataState.amount),
                    includeFeeInAmount = lastLoadedQuotesState.preparedSwapConfigState.includeFeeInAmount,
                    fee = fee,
                )
            }.onSuccess {
                when (it) {
                    is SwapTransactionState.TxSent -> {
                        sendSuccessSwapEvent(fromCurrency.currency, fee.feeType)
                        val url = blockchainInteractor.getExplorerTransactionLink(
                            networkId = fromCurrency.currency.network.backendId,
                            txHash = it.txHash,
                        )
                        updateWalletBalance()
                        uiState = stateBuilder.createSuccessState(
                            uiState = uiState,
                            swapTransactionState = it,
                            dataState = dataState,
                            txUrl = url,
                            onExploreClick = {
                                if (it.txHash.isNotEmpty()) {
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
                    is SwapTransactionState.UserCancelled -> {
                        startLoadingQuotesFromLastState()
                    }
                    is SwapTransactionState.DemoMode -> {
                        startLoadingQuotesFromLastState()
                        uiState = stateBuilder.createDemoModeAlert(uiState) {
                            uiState = stateBuilder.clearAlert(uiState)
                        }
                    }
                    else -> {
                        startLoadingQuotesFromLastState()
                        uiState = stateBuilder.createErrorTransaction(uiState, it) {
                            uiState = stateBuilder.clearAlert(uiState)
                        }
                    }
                }
            }.onFailure {
                Timber.e(it)
                startLoadingQuotesFromLastState()
                makeDefaultAlert()
            }
        }
    }

    private fun sendSuccessEvent() {
        val provider = dataState.selectedProvider ?: return
        val fee = dataState.selectedFee?.feeType ?: return
        val fromCurrency = dataState.fromCryptoCurrency?.currency ?: return
        val toCurrency = dataState.toCryptoCurrency?.currency ?: return

        analyticsEventHandler.send(
            SwapEvents.SwapInProgressScreen(
                provider = provider,
                commission = fee,
                sendBlockchain = fromCurrency.network.name,
                receiveBlockchain = toCurrency.network.name,
                sendToken = fromCurrency.symbol,
                receiveToken = toCurrency.symbol,
            ),
        )
    }

    private fun givePermissionsToSwap() {
        viewModelScope.launch(dispatchers.main) {
            runCatching {
                val fromToken = requireNotNull(dataState.fromCryptoCurrency?.currency) {
                    "dataState.fromCurrency might not be null"
                }
                val approveDataModel = requireNotNull(dataState.approveDataModel) {
                    "dataState.approveDataModel.spenderAddress shouldn't be null"
                }
                val approveType = requireNotNull(dataState.approveType?.toDomainApproveType()) {
                    "uiState.permissionState should not be null"
                }
                val feeForPermission = when (val fee = approveDataModel.fee) {
                    TxFeeState.Empty -> {
                        makeDefaultAlert(resourceReference(R.string.swapping_fee_estimation_error_text))
                        Timber.e("Fee should not be Empty")
                        return@runCatching
                    }
                    is TxFeeState.MultipleFeeState -> fee.priorityFee
                    is TxFeeState.SingleFeeState -> fee.fee
                }
                runCatching(dispatchers.io) {
                    swapInteractor.givePermissionToSwap(
                        networkId = fromToken.network.backendId,
                        permissionOptions = PermissionOptions(
                            approveData = approveDataModel,
                            forTokenContractAddress = (fromToken as? CryptoCurrency.Token)?.contractAddress.orEmpty(),
                            fromToken = fromToken,
                            approveType = approveType,
                            txFee = feeForPermission,
                            spenderAddress = approveDataModel.spenderAddress,
                        ),
                    )
                }.onSuccess {
                    when (it) {
                        is SwapTransactionState.TxSent -> {
                            sendApproveSuccessEvent(fromToken, feeForPermission.feeType, approveType)
                            updateWalletBalance()
                            uiState = stateBuilder.loadingPermissionState(uiState)
                            uiState = stateBuilder.dismissBottomSheet(uiState)
                            startLoadingQuotesFromLastState(isSilent = true)
                        }
                        is SwapTransactionState.UserCancelled -> Unit
                        else -> {
                            uiState = stateBuilder.createErrorTransaction(uiState, it) {
                                uiState = stateBuilder.clearAlert(uiState)
                            }
                        }
                    }
                }.onFailure { makeDefaultAlert() }
            }.onFailure { showGenericError(it.message.orEmpty()) }
        }
    }

    private fun showGenericError(message: String) {
        makeDefaultAlert(resourceReference(R.string.common_unknown_error))
        Timber.e(message)
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
                it.currencyStatus.currency.name.contains(searchQuery, ignoreCase = true) ||
                    it.currencyStatus.currency.symbol.contains(searchQuery, ignoreCase = true)
            }
            val unavailable = group.unavailable.filter {
                it.currencyStatus.currency.name.contains(searchQuery, ignoreCase = true) ||
                    it.currencyStatus.currency.symbol.contains(searchQuery, ignoreCase = true)
            }
            val filteredTokenDataState = if (isOrderReversed) {
                tokenDataState.copy(
                    fromGroup = tokenDataState.fromGroup.copy(
                        available = available,
                        unavailable = unavailable,
                        afterSearch = true,
                    ),
                )
            } else {
                tokenDataState.copy(
                    toGroup = tokenDataState.toGroup.copy(
                        available = available,
                        unavailable = unavailable,
                        afterSearch = true,
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

        val userWalletId = swapInteractor.getSelectedWallet()?.walletId

        if (foundToken != null) {
            val fromToken: CryptoCurrencyStatus
            val toToken: CryptoCurrencyStatus
            if (isOrderReversed) {
                fromToken = foundToken.currencyStatus
                toToken = initialCryptoCurrencyStatus

                val newToken = fromToken.currency as? CryptoCurrency.Coin
                if (userWalletId != null && newToken != null) {
                    subscribeToCoinBalanceUpdates(
                        userWalletId = userWalletId,
                        coin = newToken,
                        isFromCurrency = true,
                    )
                }
            } else {
                fromToken = initialCryptoCurrencyStatus
                toToken = foundToken.currencyStatus

                val newToken = toToken.currency as? CryptoCurrency.Coin
                if (userWalletId != null && newToken != null) {
                    subscribeToCoinBalanceUpdates(
                        userWalletId = userWalletId,
                        coin = newToken,
                        isFromCurrency = false,
                    )
                }
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

    private fun subscribeToCoinBalanceUpdates(
        userWalletId: UserWalletId,
        coin: CryptoCurrency.Coin,
        isFromCurrency: Boolean,
    ) {
        Timber.d("Subscribe to ${coin.id} balance updates")

        getCurrencyStatusUpdatesUseCase(
            userWalletId = userWalletId,
            currencyId = coin.id,
            isSingleWalletWithTokens = false,
        )
            .mapNotNull { (it as? Either.Right)?.value }
            .distinctUntilChanged { old, new -> old.value.amount == new.value.amount } // Check only balance changes
            .onEach {
                Timber.d("${coin.id} balance is ${it.value.amount}")

                uiState = if (isFromCurrency) {
                    dataState = dataState.copy(fromCryptoCurrency = it)
                    stateBuilder.updateSendCurrencyBalance(uiState, it)
                } else {
                    dataState = dataState.copy(toCryptoCurrency = it)
                    stateBuilder.updateReceiveCurrencyBalance(uiState, it)
                }

                startLoadingQuotesFromLastState(isSilent = true)
            }
            .flowOn(dispatchers.main)
            .launchIn(viewModelScope)
            .saveIn(if (isFromCurrency) fromTokenBalanceJobHolder else toTokenBalanceJobHolder)
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
            dataState.tokensDataState?.let {
                updateTokensState(it)
            }

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
        if (fromToken != null) {
            val decimals = fromToken.currency.decimals
            val cutValue = cutAmountWithDecimals(decimals, value)
            lastAmount.value = cutValue
            uiState =
                stateBuilder.updateSwapAmount(uiState, inputNumberFormatter.formatWithThousands(cutValue, decimals))

            if (toToken != null) {
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
    }

    private fun onMaxAmountClicked() {
        dataState.fromCryptoCurrency?.let {
            val balance = swapInteractor.getTokenBalance(it)
            onAmountChanged(balance.formatToUIRepresentation())
        }
    }

    private fun onReduceAmountClicked(newAmount: SwapAmount) {
        onAmountChanged(newAmount.formatToUIRepresentation())
    }

    private fun onLeaveExistentialDepositClicked(newAmount: SwapAmount) {
        onAmountChanged(newAmount.formatToUIRepresentation())
    }

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

    private fun makeDefaultAlert(message: TextReference) {
        uiState = stateBuilder.addAlert(uiState, message) {
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
                sendPermissionApproveClickedEvent()
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
            onMaxAmountSelected = ::onMaxAmountClicked,
            onReduceAmount = ::onReduceAmountClicked,
            onLeaveExistentialDeposit = ::onLeaveExistentialDepositClicked,
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
                val txFeeState =
                    dataState.getCurrentLoadedSwapState()?.txFee as? TxFeeState.MultipleFeeState ?: return@UiActions
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
                    )
                    setupLoadedState(selectedProvider, updatedState, fromToken)
                }
            },
            onProviderClick = { providerId ->
                analyticsEventHandler.send(SwapEvents.ProviderClicked)
                val states = dataState.lastLoadedSwapStates.getLastLoadedSuccessStates()
                val pricesLowerBest = getPricesLowerBest(providerId, states)
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
            onBuyClick = { currency ->
                swapInteractor.getSelectedWallet()?.let { userWallet ->
                    swapRouter.openTokenDetails(
                        userWalletId = userWallet.walletId,
                        currency = currency,
                    )
                }
            },
            onRetryClick = {
                startLoadingQuotesFromLastState()
            },
            onPolicyClick = {
                swapRouter.openUrl(it)
            },
            onTosClick = {
                swapRouter.openUrl(it)
            },
            onReceiveCardWarningClick = {
                val selectedProvider = dataState.selectedProvider ?: return@UiActions
                val currencySymbol = dataState.toCryptoCurrency?.currency?.symbol ?: return@UiActions
                val isPriceImpact = uiState.priceImpact is PriceImpact.Value
                uiState = stateBuilder.createAlert(
                    uiState = uiState,
                    isPriceImpact = isPriceImpact,
                    token = currencySymbol,
                    providerType = selectedProvider.type,
                ) {
                    uiState = stateBuilder.clearAlert(uiState)
                }
            },
            onFeeReadMoreClick = {
                swapRouter.openUrl(it)
            },
        )
    }

    private fun sendSuccessSwapEvent(fromToken: CryptoCurrency, feeType: FeeType) {
        val event = AnalyticsParam.TxSentFrom.Swap(
            blockchain = fromToken.network.name,
            token = fromToken.symbol,
            feeType = AnalyticsParam.FeeType.fromString(feeType.getNameForAnalytics()),
        )
        analyticsEventHandler.send(
            Basic.TransactionSent(
                sentFrom = event,
                memoType = Basic.TransactionSent.MemoType.Null,
            ),
        )
    }

    private fun sendApproveSuccessEvent(fromToken: CryptoCurrency, feeType: FeeType, approveType: SwapApproveType) {
        val event = AnalyticsParam.TxSentFrom.Approve(
            blockchain = fromToken.network.name,
            token = fromToken.symbol,
            feeType = AnalyticsParam.FeeType.fromString(feeType.getNameForAnalytics()),
            permissionType = approveType.getNameForAnalytics(),
        )
        analyticsEventHandler.send(
            Basic.TransactionSent(
                sentFrom = event,
                memoType = Basic.TransactionSent.MemoType.Null,
            ),
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
            if (!it.value.fromTokenInfo.amountFiat.isNullOrZero() && !it.value.toTokenInfo.amountFiat.isNullOrZero()) {
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

    private fun getPricesLowerBest(selectedProviderId: String, state: SuccessLoadedSwapData): Map<String, Float> {
        val selectedProviderEntry = state.filter { it.key.providerId == selectedProviderId }.entries.firstOrNull()
            ?: return emptyMap()
        val selectedProviderRate = selectedProviderEntry.value.toTokenInfo.tokenAmount.value
        val hundredPercent = BigDecimal("100")
        return state.entries.mapNotNull {
            if (it.key != selectedProviderEntry.key) {
                val amount = it.value.toTokenInfo.tokenAmount.value
                val percentDiff = BigDecimal.ONE.minus(
                    selectedProviderRate.divide(amount, RoundingMode.HALF_UP),
                ).multiply(hundredPercent)
                it.key.providerId to percentDiff.setScale(2, RoundingMode.HALF_UP).toFloat()
            } else {
                null
            }
        }.toMap()
    }

    private fun createSelectedAppCurrencyFlow(): StateFlow<AppCurrency> {
        return getSelectedAppCurrencyUseCase().map { maybeAppCurrency ->
            maybeAppCurrency.getOrElse { AppCurrency.Default }
        }.stateIn(
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
        return this.filter { it.value is SwapState.QuotesLoadedState }
            .mapValues { it.value as SwapState.QuotesLoadedState }
    }

    private fun Map<SwapProvider, SwapState>.consideredProvidersStates(): Map<SwapProvider, SwapState> {
        return this.filter {
            it.value is SwapState.QuotesLoadedState || isUserResolvableError(it.value)
        }
    }

    private fun isReverseSwapPossible(): Boolean {
        val from = dataState.fromCryptoCurrency ?: return false
        val to = dataState.toCryptoCurrency ?: return false

        val currenciesGroup = if (isOrderReversed) {
            dataState.tokensDataState?.toGroup
        } else {
            dataState.tokensDataState?.fromGroup
        } ?: return false

        val chosen = if (isOrderReversed) from else to

        return currenciesGroup.available
            .map { it.currencyStatus.currency }
            .contains(chosen.currency)
    }

    private fun sendPermissionApproveClickedEvent() {
        val sendTokenSymbol = dataState.fromCryptoCurrency?.currency?.symbol
        val receiveTokenSymbol = dataState.toCryptoCurrency?.currency?.symbol
        val approveType = dataState.approveType
        if (sendTokenSymbol != null && receiveTokenSymbol != null && approveType != null) {
            analyticsEventHandler.send(
                SwapEvents.ButtonPermissionApproveClicked(
                    sendToken = sendTokenSymbol,
                    receiveToken = receiveTokenSymbol,
                    approveType = approveType,
                ),
            )
        }
    }

    private fun updateWalletBalance() {
        swapInteractor.getSelectedWallet()?.let { userWallet ->
            dataState.fromCryptoCurrency?.currency?.network?.let { network ->
                viewModelScope.launch {
                    withContext(NonCancellable) {
                        updateForBalance(userWallet, network)
                    }
                }
            }
        }
    }

    private suspend fun updateForBalance(userWallet: UserWallet, network: Network) {
        updateDelayedCurrencyStatusUseCase(
            userWalletId = userWallet.walletId,
            network = network,
            delayMillis = UPDATE_BALANCE_DELAY_MILLIS,
            refresh = true,
        )
    }

    private fun ApproveType.toDomainApproveType(): SwapApproveType {
        return when (this) {
            ApproveType.LIMITED -> SwapApproveType.LIMITED
            ApproveType.UNLIMITED -> SwapApproveType.UNLIMITED
        }
    }

    private fun triggerPromoProviderEvent(recommendedProvider: SwapProvider?, bestQuotesProvider: SwapProvider?) {
        // for now send event only for changelly
        if (recommendedProvider == null ||
            recommendedProvider.providerId != CHANGELLY_PROVIDER_ID ||
            bestQuotesProvider == null
        ) {
            return
        }
        val event = if (recommendedProvider.providerId == bestQuotesProvider.providerId) {
            SwapEvents.ChangellyActivity(SwapEvents.ChangellyActivity.PromoState.Native)
        } else {
            SwapEvents.ChangellyActivity(SwapEvents.ChangellyActivity.PromoState.Recommended)
        }
        analyticsEventHandler.send(event = event)
    }

    private companion object {
        const val loggingTag = "SwapViewModel"
        const val INITIAL_AMOUNT = ""
        const val UPDATE_DELAY = 10000L
        const val DEBOUNCE_AMOUNT_DELAY = 1000L
        const val UPDATE_BALANCE_DELAY_MILLIS = 11000L
        const val CHANGELLY_PROVIDER_ID = "changelly"
    }
}
