package com.tangem.feature.swap.viewmodels

import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.*
import arrow.core.Either
import arrow.core.getOrElse
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.routing.bundle.unbundle
import com.tangem.common.ui.bottomsheet.permission.state.ApproveType
import com.tangem.common.ui.bottomsheet.permission.state.GiveTxPermissionState.InProgress.getApproveTypeOrNull
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.utils.InputNumberFormatter
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.feedback.GetCardInfoUseCase
import com.tangem.domain.feedback.SaveBlockchainErrorUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.BlockchainErrorInfo
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.promo.GetStoryContentUseCase
import com.tangem.domain.promo.ShouldShowStoriesUseCase
import com.tangem.domain.promo.models.StoryContentIds
import com.tangem.domain.tokens.*
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.feature.swap.analytics.SwapEvents
import com.tangem.feature.swap.domain.BlockchainInteractor
import com.tangem.feature.swap.domain.SwapInteractor
import com.tangem.feature.swap.domain.models.ExpressDataError
import com.tangem.feature.swap.domain.models.ExpressException
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.domain.*
import com.tangem.feature.swap.domain.models.ui.*
import com.tangem.feature.swap.models.SwapStateHolder
import com.tangem.feature.swap.models.UiActions
import com.tangem.feature.swap.models.states.SwapNotificationUM
import com.tangem.feature.swap.presentation.R
import com.tangem.feature.swap.router.SwapNavScreen
import com.tangem.feature.swap.router.SwapRouter
import com.tangem.feature.swap.ui.StateBuilder
import com.tangem.feature.swap.utils.formatToUIRepresentation
import com.tangem.features.swap.SwapFeatureToggles
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
    private val blockchainInteractor: BlockchainInteractor,
    private val dispatchers: CoroutineDispatcherProvider,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val getCryptoCurrencyStatusUseCase: GetCryptoCurrencyStatusSyncUseCase,
    private val updateDelayedCurrencyStatusUseCase: UpdateDelayedNetworkStatusUseCase,
    private val getCurrencyStatusUpdatesUseCase: GetCurrencyStatusUpdatesUseCase,
    private val getFeePaidCryptoCurrencyStatusSyncUseCase: GetFeePaidCryptoCurrencyStatusSyncUseCase,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val getCardInfoUseCase: GetCardInfoUseCase,
    private val saveBlockchainErrorUseCase: SaveBlockchainErrorUseCase,
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase,
    private val getMinimumTransactionAmountSyncUseCase: GetMinimumTransactionAmountSyncUseCase,
    private val shouldShowStoriesUseCase: ShouldShowStoriesUseCase,
    private val getStoryContentUseCase: GetStoryContentUseCase,
    private val featureToggles: SwapFeatureToggles,
    swapInteractorFactory: SwapInteractor.Factory,
    private val savedStateHandle: SavedStateHandle,
    private val urlOpener: UrlOpener,
    router: AppRouter,
) : ViewModel(), DefaultLifecycleObserver {

    private val initialCurrencyFrom: CryptoCurrency
        get() {
            return savedStateHandle.get<Bundle>(AppRoute.Swap.CURRENCY_FROM_KEY)
                ?.unbundle(CryptoCurrency.serializer())
                ?: error("no expected parameter CryptoCurrency (from) found")
        }

    private val initialCurrencyTo: CryptoCurrency? = savedStateHandle.get<Bundle>(AppRoute.Swap.CURRENCY_TO_KEY)
        ?.unbundle(CryptoCurrency.serializer())

    private val userWalletId: UserWalletId = savedStateHandle.get<Bundle>(AppRoute.Swap.USER_WALLET_ID_KEY)
        ?.unbundle(UserWalletId.serializer())
        ?: error("no expected parameter UserWalletId found")

    private val isInitiallyReversed: Boolean = savedStateHandle.get<Boolean>(AppRoute.Swap.IS_INITIAL_REVERSE_ORDER)
        ?: false

    private val swapInteractor = swapInteractorFactory.create(userWalletId)

    private lateinit var initialFromStatus: CryptoCurrencyStatus
    private var initialToStatus: CryptoCurrencyStatus? = null
    private var userWallet: UserWallet by Delegates.notNull()

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
            initialCurrencyFrom = initialCurrencyFrom,
            initialCurrencyTo = initialCurrencyTo,
            fromNetworkInfo = blockchainInteractor.getBlockchainInfo(initialCurrencyFrom.network.backendId),
        ),
    )
        private set

    // shows currency order (direct - swap initial to selected, reversed = selected to initial)
    private var isOrderReversed = false
    private val lastAmount = mutableStateOf(INITIAL_AMOUNT)
    private val lastReducedBalanceBy = mutableStateOf(BigDecimal.ZERO)
    private var swapRouter: SwapRouter = SwapRouter(router = router)

    private val isUserResolvableError: (SwapState) -> Boolean = {
        it is SwapState.SwapError &&
            (
                it.error is ExpressDataError.ExchangeTooSmallAmountError ||
                    it.error is ExpressDataError.ExchangeTooBigAmountError
                )
    }

    private val fromTokenBalanceJobHolder = JobHolder()
    private val toTokenBalanceJobHolder = JobHolder()

    private var isAmountChangedByUser: Boolean = false

    val currentScreen: SwapNavScreen
        get() = swapRouter.currentScreen

    init {
        viewModelScope.launch {
            if (featureToggles.isPromoStoriesEnabled) {
                initStories()
                swapRouter.openScreen(SwapNavScreen.PromoStories)
            }
        }
        viewModelScope.launch(dispatchers.io) {
            val fromStatus = getCryptoCurrencyStatusUseCase(userWalletId, initialCurrencyFrom.id).getOrNull()
            val toStatus = initialCurrencyTo?.let { getCryptoCurrencyStatusUseCase(userWalletId, it.id).getOrNull() }
            val wallet = getUserWalletUseCase(userWalletId).getOrNull()

            if (fromStatus == null || wallet == null) {
                uiState = stateBuilder.addAlert(uiState = uiState, onDismiss = swapRouter::back)
            } else {
                userWallet = wallet
                initialFromStatus = fromStatus
                initialToStatus = toStatus
                initTokens(isInitiallyReversed)
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

    override fun onStart(owner: LifecycleOwner) {
        startLoadingQuotesFromLastState(true)
    }

    override fun onStop(owner: LifecycleOwner) {
        singleTaskScheduler.cancelTask()
    }

    override fun onCleared() {
        singleTaskScheduler.cancelTask()
        super.onCleared()
    }

    fun onScreenOpened() {
        analyticsEventHandler.send(SwapEvents.SwapScreenOpened(initialCurrencyFrom.symbol))
    }

    private fun sendSelectTokenScreenOpenedEvent() {
        val isAnyAvailableTokensTo = dataState.tokensDataState?.toGroup?.available?.isNotEmpty() ?: false
        val isAnyAvailableTokensFrom = dataState.tokensDataState?.fromGroup?.available?.isNotEmpty() ?: false
        val isAnyAvailableTokens = isAnyAvailableTokensTo || isAnyAvailableTokensFrom
        analyticsEventHandler.send(SwapEvents.ChooseTokenScreenOpened(availableTokens = isAnyAvailableTokens))
    }

    private fun initTokens(isReverseFromTo: Boolean) {
        viewModelScope.launch(dispatchers.main) {
            runCatching(dispatchers.io) {
                swapInteractor.getTokensDataState(initialCurrencyFrom)
            }.onSuccess { state ->
                updateTokensState(state)
                val selectedCurrency = initialToStatus ?: swapInteractor.getInitialCurrencyToSwap(
                    initialCryptoCurrency = initialCurrencyFrom,
                    state = state,
                    isReverseFromTo = isReverseFromTo,
                )

                applyInitialTokenChoice(
                    state = state,
                    selectedCurrency = selectedCurrency,
                    isReverseFromTo = isReverseFromTo,
                )

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
                Timber.e(it)

                applyInitialTokenChoice(
                    state = TokensDataStateExpress.EMPTY,
                    selectedCurrency = null,
                    isReverseFromTo = isReverseFromTo,
                )

                uiState = stateBuilder.createInitialErrorState(
                    uiState,
                    (it as? ExpressException)?.expressDataError?.code ?: ExpressDataError.UnknownError.code,
                ) {
                    uiState = stateBuilder.createInitialLoadingState(
                        initialCurrencyFrom = initialCurrencyFrom,
                        initialCurrencyTo = initialCurrencyTo,
                        fromNetworkInfo = blockchainInteractor.getBlockchainInfo(
                            initialCurrencyFrom.network.backendId,
                        ),
                    )
                    initTokens(isReverseFromTo)
                }
            }
        }
    }

    private fun initStories() {
        viewModelScope.launch {
            getStoryContentUseCase.invokeSync(StoryContentIds.STORY_FIRST_TIME_SWAP.id).fold(
                ifLeft = {
                    Timber.e("Unable to load stories for ${StoryContentIds.STORY_FIRST_TIME_SWAP.id}")
                },
                ifRight = { story ->
                    story?.let {
                        uiState = stateBuilder.createStoriesState(uiState, story)
                    }
                },
            )
        }
    }

    private fun applyInitialTokenChoice(
        state: TokensDataStateExpress,
        selectedCurrency: CryptoCurrencyStatus?,
        isReverseFromTo: Boolean,
    ) {
        // exceptional case
        if (selectedCurrency == null) {
            analyticsEventHandler.send(SwapEvents.NoticeNoAvailableTokensToSwap)
            uiState = stateBuilder.createNoAvailableTokensToSwapState(
                uiStateHolder = uiState,
                fromToken = initialFromStatus,
            )
            return
        }
        isOrderReversed = isReverseFromTo
        val (fromCurrencyStatus, toCurrencyStatus) = if (isOrderReversed) {
            selectedCurrency to initialFromStatus
        } else {
            initialFromStatus to selectedCurrency
        }
        dataState = dataState.copy(
            fromCryptoCurrency = fromCurrencyStatus,
            toCryptoCurrency = toCurrencyStatus,
            tokensDataState = state,
        )
        startLoadingQuotes(
            fromToken = fromCurrencyStatus,
            toToken = toCurrencyStatus,
            amount = lastAmount.value,
            reduceBalanceBy = lastReducedBalanceBy.value,
            toProvidersList = findSwapProviders(fromCurrencyStatus, toCurrencyStatus),
        )
    }

    private fun updateTokensState(tokenDataState: TokensDataStateExpress) {
        val tokensDataState = if (isOrderReversed) tokenDataState.fromGroup else tokenDataState.toGroup
        uiState = stateBuilder.addTokensToState(
            uiState = uiState,
            tokensDataState = tokensDataState,
            fromToken = dataState.fromCryptoCurrency?.currency ?: initialCurrencyFrom,
        )
    }

    private fun startLoadingQuotes(
        fromToken: CryptoCurrencyStatus,
        toToken: CryptoCurrencyStatus,
        amount: String,
        reduceBalanceBy: BigDecimal,
        toProvidersList: List<SwapProvider>,
        isSilent: Boolean = false,
    ) {
        singleTaskScheduler.cancelTask()
        if (!isSilent) {
            uiState = stateBuilder.createQuotesLoadingState(
                uiState,
                fromToken.currency,
                toToken.currency,
                initialCurrencyFrom.id.value,
            )
        }
        singleTaskScheduler.scheduleTask(
            viewModelScope,
            loadQuotesTask(
                fromToken = fromToken,
                toToken = toToken,
                amount = amount,
                reduceBalanceBy = reduceBalanceBy,
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
                reduceBalanceBy = dataState.reduceBalanceBy,
                toProvidersList = findSwapProviders(fromCurrency, toCurrency),
            )
        }
    }

    private fun loadQuotesTask(
        fromToken: CryptoCurrencyStatus,
        toToken: CryptoCurrencyStatus,
        amount: String,
        reduceBalanceBy: BigDecimal,
        toProvidersList: List<SwapProvider>,
    ): PeriodicTask<Map<SwapProvider, SwapState>> {
        return PeriodicTask(
            UPDATE_DELAY,
            task = {
                uiState = stateBuilder.createSilentLoadState(uiState)
                runCatching(dispatchers.io) {
                    dataState = dataState.copy(
                        amount = amount,
                        reduceBalanceBy = reduceBalanceBy,
                        swapDataModel = null,
                        approveDataModel = null,
                    )
                    swapInteractor.findBestQuote(
                        fromToken = fromToken,
                        toToken = toToken,
                        providers = toProvidersList,
                        amountToSwap = amount,
                        reduceBalanceBy = reduceBalanceBy,
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
                uiState = stateBuilder.addNotification(uiState, null) { startLoadingQuotesFromLastState() }
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
                    feeCryptoCurrencyStatus = dataState.feePaidCryptoCurrency,
                    swapProvider = provider,
                    bestRatedProviderId = bestRatedProviderId,
                    isNeedBestRateBadge = dataState.lastLoadedSwapStates.consideredProvidersStates().size > 1,
                    selectedFeeType = dataState.selectedFee?.feeType ?: FeeType.NORMAL,
                    isReverseSwapPossible = isReverseSwapPossible(),
                )
                if (uiState.notifications.any { it is SwapNotificationUM.Error.UnableToCoverFeeWarning }) {
                    analyticsEventHandler.send(
                        SwapEvents.NoticeNotEnoughFee(
                            token = initialCurrencyFrom.symbol,
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
                    expressDataError = state.error,
                    includeFeeInAmount = state.includeFeeInAmount,
                    isReverseSwapPossible = isReverseSwapPossible(),
                )
                sendErrorAnalyticsEvent(state.error, provider)
            }
        }
    }

    private fun sendErrorAnalyticsEvent(error: ExpressDataError, provider: SwapProvider) {
        val receiveToken = dataState.toCryptoCurrency?.currency?.let {
            "${it.network.backendId}:${it.symbol}"
        }
        analyticsEventHandler.send(
            SwapEvents.NoticeProviderError(
                sendToken = "${initialCurrencyFrom.network.backendId}:${initialCurrencyFrom.symbol}",
                receiveToken = receiveToken ?: "",
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
            val successLoadedData = consideredProviders.getLastLoadedSuccessStates()
            val bestQuotesProvider = findBestQuoteProvider(successLoadedData)
            val currentSelected = dataState.selectedProvider
            if (currentSelected != null && consideredProviders.keys.contains(currentSelected)) {
                // logic for always choose best if already selected provider
                if (isAmountChangedByUser) {
                    isAmountChangedByUser = false
                    bestQuotesProvider ?: currentSelected
                } else {
                    currentSelected
                }
            } else {
                val recommendedProvider = successLoadedData.keys.firstOrNull { it.isRecommended }
                triggerPromoProviderEvent(recommendedProvider, bestQuotesProvider)

                if (isAmountChangedByUser) {
                    isAmountChangedByUser = false
                    recommendedProvider ?: bestQuotesProvider ?: consideredProviders.keys.first()
                } else {
                    recommendedProvider ?: consideredProviders.keys.first()
                }
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
            dataState.copy(approveDataModel = permissionState.requestApproveData)
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
                                    urlOpener.openUrl(url)
                                }
                                analyticsEventHandler.send(
                                    event = SwapEvents.ButtonExplore(initialCurrencyFrom.symbol),
                                )
                            },
                            onStatusClick = {
                                val txExternalUrl = it.txExternalUrl
                                if (!txExternalUrl.isNullOrBlank()) {
                                    urlOpener.openUrl(txExternalUrl)
                                    analyticsEventHandler.send(
                                        event = SwapEvents.ButtonStatus(initialCurrencyFrom.symbol),
                                    )
                                }
                            },
                        )
                        sendSuccessEvent()

                        swapRouter.openScreen(SwapNavScreen.Success)
                    }
                    SwapTransactionState.DemoMode -> {
                        uiState = stateBuilder.createDemoModeAlert(uiState) {
                            uiState = stateBuilder.clearAlert(uiState)
                        }
                    }
                    is SwapTransactionState.Error -> {
                        startLoadingQuotesFromLastState()
                        uiState = stateBuilder.createErrorTransactionAlert(
                            uiState = uiState,
                            error = it,
                            onDismiss = { uiState = stateBuilder.clearAlert(uiState) },
                            onSupportClick = ::onFailedTxEmailClick,
                        )
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
                val approveType =
                    requireNotNull(uiState.permissionState.getApproveTypeOrNull()?.toDomainApproveType()) {
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
                        is SwapTransactionState.Error -> {
                            uiState = stateBuilder.createErrorTransactionAlert(
                                uiState = uiState,
                                error = it,
                                onDismiss = { uiState = stateBuilder.clearAlert(uiState) },
                                onSupportClick = ::onFailedTxEmailClick,
                            )
                        }
                        SwapTransactionState.DemoMode -> {
                            uiState = stateBuilder.createDemoModeAlert(uiState) {
                                uiState = stateBuilder.clearAlert(uiState)
                            }
                        }
                    }
                }.onFailure { makeDefaultAlert() }
            }.onFailure {
                Timber.e(it.message.orEmpty())
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

        if (foundToken != null) {
            val fromToken: CryptoCurrencyStatus
            val toToken: CryptoCurrencyStatus
            if (isOrderReversed) {
                fromToken = foundToken.currencyStatus
                toToken = initialFromStatus

                val newToken = fromToken.currency as? CryptoCurrency.Coin
                if (newToken != null) {
                    subscribeToCoinBalanceUpdates(
                        userWalletId = userWalletId,
                        coin = newToken,
                        isFromCurrency = true,
                    )
                }
            } else {
                fromToken = initialFromStatus
                toToken = foundToken.currencyStatus

                val newToken = toToken.currency as? CryptoCurrency.Coin
                if (newToken != null) {
                    subscribeToCoinBalanceUpdates(
                        userWalletId = userWalletId,
                        coin = newToken,
                        isFromCurrency = false,
                    )
                }
            }

            if (dataState.fromCryptoCurrency != null && dataState.tokensDataState != null) {
                isAmountChangedByUser = true
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
                reduceBalanceBy = lastReducedBalanceBy.value,
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

                dataState = dataState.copy(
                    feePaidCryptoCurrency = getFeePaidCryptoCurrencyStatusSyncUseCase(
                        userWalletId = userWalletId,
                        cryptoCurrencyStatus = it,
                    ).getOrNull() ?: it,
                )

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
        viewModelScope.launch {
            val newFromToken = dataState.toCryptoCurrency
            val newToToken = dataState.fromCryptoCurrency

            if (newFromToken != null && newToToken != null) {
                isAmountChangedByUser = true

                dataState = dataState.copy(
                    fromCryptoCurrency = newFromToken,
                    toCryptoCurrency = newToToken,
                )
                isOrderReversed = !isOrderReversed
                dataState.tokensDataState?.let {
                    updateTokensState(it)
                }

                val minTxAmount = getMinimumTransactionAmountSyncUseCase(
                    userWalletId,
                    newFromToken,
                ).getOrNull()
                val decimals = newFromToken.currency.decimals
                lastAmount.value = cutAmountWithDecimals(decimals, lastAmount.value)
                lastReducedBalanceBy.value = BigDecimal.ZERO
                uiState = stateBuilder.updateSwapAmount(
                    uiState = uiState,
                    amountFormatted = inputNumberFormatter.formatWithThousands(lastAmount.value, decimals),
                    amountRaw = lastAmount.value,
                    fromToken = newFromToken.currency,
                    minTxAmount = minTxAmount,
                )
                startLoadingQuotes(
                    fromToken = newFromToken,
                    toToken = newToToken,
                    amount = lastAmount.value,
                    reduceBalanceBy = lastReducedBalanceBy.value,
                    toProvidersList = findSwapProviders(newFromToken, newToToken),
                )
            }
        }
    }

    private fun onAmountChanged(
        value: String,
        forceQuotesUpdate: Boolean = false,
        reduceBalanceBy: BigDecimal = BigDecimal.ZERO,
    ) {
        viewModelScope.launch {
            val fromToken = dataState.fromCryptoCurrency
            val toToken = dataState.toCryptoCurrency
            if (fromToken != null) {
                val decimals = fromToken.currency.decimals
                val cutValue = cutAmountWithDecimals(decimals, value)
                val minTxAmount = getMinimumTransactionAmountSyncUseCase(
                    userWalletId,
                    fromToken,
                ).getOrNull()
                lastAmount.value = cutValue
                lastReducedBalanceBy.value = reduceBalanceBy
                uiState = stateBuilder.updateSwapAmount(
                    uiState = uiState,
                    amountFormatted = inputNumberFormatter.formatWithThousands(cutValue, decimals),
                    amountRaw = lastAmount.value,
                    fromToken = fromToken.currency,
                    minTxAmount = minTxAmount,
                )

                if (toToken != null) {
                    if (toToken.value.amount != null) {
                        isAmountChangedByUser = true
                    }

                    amountDebouncer.debounce(viewModelScope, DEBOUNCE_AMOUNT_DELAY, forceUpdate = forceQuotesUpdate) {
                        startLoadingQuotes(
                            fromToken = fromToken,
                            toToken = toToken,
                            amount = lastAmount.value,
                            reduceBalanceBy = lastReducedBalanceBy.value,
                            toProvidersList = findSwapProviders(fromToken, toToken),
                        )
                    }
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

    private fun onReduceAmountClicked(newAmount: SwapAmount, reduceBalanceBy: BigDecimal = BigDecimal.ZERO) {
        onAmountChanged(
            value = newAmount.formatToUIRepresentation(),
            forceQuotesUpdate = true,
            reduceBalanceBy = reduceBalanceBy,
        )
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
        uiState = stateBuilder.addAlert(uiState)
    }

    private fun makeDefaultAlert(message: TextReference) {
        uiState = stateBuilder.addAlert(uiState, message)
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
                if (bottomSheet != null && bottomSheet.isShown) {
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
            onReduceToAmount = ::onReduceAmountClicked,
            onReduceByAmount = ::onReduceAmountClicked,
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
                uiState = stateBuilder.dismissBottomSheet(uiState)
                dataState = dataState.copy(selectedFee = it)
                viewModelScope.launch(dispatchers.io) {
                    startLoadingQuotesFromLastState(false)
                }
            },
            onProviderClick = { providerId ->
                analyticsEventHandler.send(SwapEvents.ProviderClicked)
                val states = dataState.lastLoadedSwapStates.getLastLoadedSuccessStates()
                val pricesLowerBest = getPricesLowerBest(providerId, states)
                uiState = stateBuilder.showSelectProviderBottomSheet(
                    uiState = uiState,
                    selectedProviderId = providerId,
                    pricesLowerBest = pricesLowerBest,
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
                swapRouter.openTokenDetails(
                    userWalletId = userWalletId,
                    currency = currency,
                )
            },
            onRetryClick = {
                startLoadingQuotesFromLastState()
            },
            onReceiveCardWarningClick = {
                val selectedProvider = dataState.selectedProvider ?: return@UiActions
                val currencySymbol = dataState.toCryptoCurrency?.currency?.symbol ?: return@UiActions
                val isPriceImpact = uiState.priceImpact is PriceImpact.Value
                uiState = stateBuilder.createAlert(
                    uiState = uiState,
                    isPriceImpact = isPriceImpact,
                    token = currencySymbol,
                    provider = selectedProvider,
                ) {
                    uiState = stateBuilder.clearAlert(uiState)
                }
            },
            onLinkClick = urlOpener::openUrl,
            onSelectTokenClick = {
                swapRouter.openScreen(SwapNavScreen.SelectToken)
                sendSelectTokenScreenOpenedEvent()
            },
            onSuccess = {
                swapRouter.openScreen(SwapNavScreen.Success)
            },
            onStoriesClose = {
                viewModelScope.launch { shouldShowStoriesUseCase.neverToShow(StoryContentIds.STORY_FIRST_TIME_SWAP.id) }
                swapRouter.openScreen(SwapNavScreen.Main)
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

        return groupToFind.available.find { idToFind == it.currencyStatus.currency.id.value }?.providers
            ?: emptyList()
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
        val approveType = uiState.permissionState.getApproveTypeOrNull()
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
        dataState.fromCryptoCurrency?.currency?.network?.let { network ->
            viewModelScope.launch {
                withContext(NonCancellable) {
                    updateForBalance(userWalletId, network)
                }
            }
        }
    }

    private suspend fun updateForBalance(userWalletId: UserWalletId, network: Network) {
        updateDelayedCurrencyStatusUseCase(
            userWalletId = userWalletId,
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

    private fun onFailedTxEmailClick(errorMessage: String) {
        viewModelScope.launch {
            val transaction = dataState.swapDataModel?.transaction
            val fromCurrencyStatus = dataState.fromCryptoCurrency ?: initialFromStatus
            val network = fromCurrencyStatus.currency.network
            val cardInfo = getCardInfoUseCase(userWallet.scanResponse).getOrElse { error("CardInfo must be not null") }

            saveBlockchainErrorUseCase(
                error = BlockchainErrorInfo(
                    errorMessage = errorMessage,
                    blockchainId = network.id.value,
                    derivationPath = network.derivationPath.value,
                    destinationAddress = transaction?.txTo.orEmpty(),
                    tokenSymbol = fromCurrencyStatus.currency.symbol,
                    amount = dataState.amount.orEmpty(),
                    fee = dataState.selectedFee?.feeCryptoFormatted.orEmpty(),
                ),
            )

            val email = FeedbackEmailType.SwapProblem(
                cardInfo = cardInfo,
                providerName = dataState.selectedProvider?.name.orEmpty(),
                txId = transaction?.txId.orEmpty(),
            )

            sendFeedbackEmailUseCase(email)
        }
    }

    private companion object {
        const val INITIAL_AMOUNT = ""
        const val UPDATE_DELAY = 10000L
        const val DEBOUNCE_AMOUNT_DELAY = 1000L
        const val UPDATE_BALANCE_DELAY_MILLIS = 11000L
        const val CHANGELLY_PROVIDER_ID = "changelly"
    }
}