package com.tangem.feature.swap.model

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import arrow.core.Either
import arrow.core.getOrElse
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.TangemBlogUrlBuilder
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.core.analytics.api.AnalyticsErrorHandler
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.AnalyticsParam.ScreensSources
import com.tangem.core.analytics.models.Basic
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.core.ui.utils.InputNumberFormatter
import com.tangem.core.ui.utils.parseBigDecimal
import com.tangem.datasource.local.appsflyer.AppsFlyerStore
import com.tangem.domain.account.status.usecase.GetAccountCurrencyStatusUseCase
import com.tangem.domain.account.status.usecase.GetFeePaidCryptoCurrencyStatusSyncUseCase
import com.tangem.domain.account.status.usecase.IsAccountsModeEnabledUseCase
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.express.models.ExpressOperationType
import com.tangem.domain.feedback.GetWalletMetaInfoUseCase
import com.tangem.domain.feedback.SaveBlockchainErrorUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.BlockchainErrorInfo
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.derivationIndex
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.WithdrawalResult
import com.tangem.domain.pay.usecase.GetPaymentAccountCryptoCurrencyStatusUseCase
import com.tangem.domain.promo.ShouldShowStoriesUseCase
import com.tangem.domain.promo.models.StoryContentIds
import com.tangem.domain.settings.usercountry.GetUserCountryUseCase
import com.tangem.domain.settings.usercountry.models.UserCountry
import com.tangem.domain.settings.usercountry.models.needApplyFCARestrictions
import com.tangem.domain.swap.models.SwapCurrencyStatus
import com.tangem.domain.tangempay.GetTangemPayCustomerIdUseCase
import com.tangem.domain.tangempay.TangemPayWithdrawUseCase
import com.tangem.domain.tokens.GetMinimumTransactionAmountSyncUseCase
import com.tangem.domain.tokens.UpdateDelayedNetworkStatusUseCase
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.transaction.usecase.gasless.IsGaslessFeeSupportedForNetwork
import com.tangem.domain.txhistory.usecase.GetExplorerTransactionUrlUseCase
import com.tangem.feature.swap.analytics.SwapEvents
import com.tangem.feature.swap.analytics.SwapQuotePerformanceTracker
import com.tangem.feature.swap.component.SwapFeeSelectorBlockComponent
import com.tangem.feature.swap.converters.SwapTransactionErrorStateConverter
import com.tangem.feature.swap.domain.AllowPermissionsHandler
import com.tangem.feature.swap.domain.SwapInteractor
import com.tangem.feature.swap.domain.TransactionFeeResult
import com.tangem.feature.swap.domain.TxFeeSealedState
import com.tangem.feature.swap.domain.models.ExpressDataError
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.domain.ExchangeProviderType
import com.tangem.feature.swap.domain.models.domain.SwapDataModel
import com.tangem.feature.swap.domain.models.domain.SwapProvider
import com.tangem.feature.swap.domain.models.ui.*
import com.tangem.feature.swap.models.SwapAlertUM
import com.tangem.feature.swap.models.SwapStateHolder
import com.tangem.feature.swap.models.TokenSelectionDirection
import com.tangem.feature.swap.models.UiActions
import com.tangem.feature.swap.models.states.SwapNotificationUM
import com.tangem.feature.swap.router.SwapRoute
import com.tangem.feature.swap.ui.StateBuilder
import com.tangem.feature.swap.utils.formatToUIRepresentation
import com.tangem.feature.swap.utils.getContractAddress
import com.tangem.features.approval.api.GiveApprovalComponent
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenAnalyticsPayload
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenBridge
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenResult
import com.tangem.features.send.v2.api.entity.FeeSelectorUM
import com.tangem.features.send.v2.api.subcomponents.feeSelector.FeeSelectorReloadTrigger
import com.tangem.features.swap.SwapComponent
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.*
import com.tangem.utils.isNullOrZero
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject

typealias SuccessLoadedSwapData = Map<SwapProvider, SwapState.QuotesLoadedState>

@Suppress("LongParameterList", "LargeClass")
@Stable
@ModelScoped
internal class SwapModel @Inject constructor(
    paramsContainer: ParamsContainer,
    getUserCountryUseCase: GetUserCountryUseCase,
    getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    chooseTokenBridgeFactory: ChooseTokenBridge.Factory,
    private val router: Router,
    private val appRouter: AppRouter,
    override val dispatchers: CoroutineDispatcherProvider,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val analyticsErrorEventHandler: AnalyticsErrorHandler,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val updateDelayedCurrencyStatusUseCase: UpdateDelayedNetworkStatusUseCase,
    private val getFeePaidCryptoCurrencyStatusSyncUseCase: GetFeePaidCryptoCurrencyStatusSyncUseCase,
    private val getWalletMetaInfoUseCase: GetWalletMetaInfoUseCase,
    private val saveBlockchainErrorUseCase: SaveBlockchainErrorUseCase,
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase,
    private val getMinimumTransactionAmountSyncUseCase: GetMinimumTransactionAmountSyncUseCase,
    private val getExplorerTransactionUrlUseCase: GetExplorerTransactionUrlUseCase,
    private val shouldShowStoriesUseCase: ShouldShowStoriesUseCase,
    private val isAccountsModeEnabledUseCase: IsAccountsModeEnabledUseCase,
    private val swapInteractor: SwapInteractor,
    private val urlOpener: UrlOpener,
    private val getAccountCurrencyStatusUseCase: GetAccountCurrencyStatusUseCase,
    private val getPaymentAccountCryptoCurrencyStatusUseCase: GetPaymentAccountCryptoCurrencyStatusUseCase,
    private val tangemPayWithdrawUseCase: TangemPayWithdrawUseCase,
    private val iGaslessFeeSupportedForNetwork: IsGaslessFeeSupportedForNetwork,
    private val feeSelectorReloadTrigger: FeeSelectorReloadTrigger,
    private val getTangemPayCustomerIdUseCase: GetTangemPayCustomerIdUseCase,
    private val appsFlyerStore: AppsFlyerStore,
    private val messageSender: UiMessageSender,
    private val initialCurrenciesResolver: InitialCurrenciesResolver,
    private val allowPermissionsHandler: AllowPermissionsHandler,
) : Model() {

    private val params = paramsContainer.require<SwapComponent.Params>()

    private val initialCryptoCurrency = params.cryptoCurrency
    private val tangemPayInput = params.tangemPayInput

    private var isBalanceHidden = true

    private var isAccountsMode: Boolean = false

    private val selectedAppCurrencyFlow: StateFlow<AppCurrency> = createSelectedAppCurrencyFlow()

    val chooseFromTokenBridge: ChooseTokenBridge = chooseTokenBridgeFactory.create(
        modelScope = modelScope,
        settings = ChooseTokenBridge.Settings.SwapFrom,
        analyticsPayload = setOf(
            ChooseTokenAnalyticsPayload.ScreensSources(ScreensSources.Swap.value),
        ),
    )
    val chooseToTokenBridge: ChooseTokenBridge = chooseTokenBridgeFactory.create(
        modelScope = modelScope,
        settings = ChooseTokenBridge.Settings.SwapTo,
        analyticsPayload = setOf(
            ChooseTokenAnalyticsPayload.ScreensSources(ScreensSources.Swap.value),
        ),
    )

    private val stateBuilder = StateBuilder(
        actions = createUiActions(),
        isBalanceHiddenProvider = Provider { isBalanceHidden },
        appCurrencyProvider = Provider(selectedAppCurrencyFlow::value),
        isAccountsModeProvider = Provider { isAccountsMode },
        iGaslessFeeSupportedForNetwork = iGaslessFeeSupportedForNetwork,
        appRouter = appRouter,
    )

    private val inputNumberFormatter = InputNumberFormatter(
        NumberFormat.getInstance(Locale.getDefault()) as? DecimalFormat
            ?: error("NumberFormat is not DecimalFormat"),
    )

    private val amountDebouncer = Debouncer()
    private val singleTaskScheduler = SingleTaskScheduler<Map<SwapProvider, SwapState>>()
    private val performanceTracker = SwapQuotePerformanceTracker()

    val dataStateStateFlow = MutableStateFlow(SwapProcessDataState())
    var dataState
        get() = dataStateStateFlow.value
        set(value) {
            dataStateStateFlow.value = value
        }

    var uiState: SwapStateHolder by mutableStateOf(stateBuilder.createInitialLoadingState())
        private set

    val feeSelectorRepository = FeeSelectorRepository()

    private val lastAmount = mutableStateOf(INITIAL_AMOUNT)
    private val lastReducedBalanceBy = mutableStateOf(BigDecimal.ZERO)
    private var userCountry: UserCountry? = null

    private val isUserResolvableError: (SwapState) -> Boolean = { swapState ->
        swapState is SwapState.SwapError &&
            (
                swapState.error is ExpressDataError.ExchangeTooSmallAmountError ||
                    swapState.error is ExpressDataError.ExchangeTooBigAmountError
                )
    }

    private val fromTokenBalanceJobHolder = JobHolder()
    private val toTokenBalanceJobHolder = JobHolder()
    private val swapPairsJobHolder = JobHolder()

    private var isAmountChangedByUser: Boolean = false
    private var lastPermissionNotificationTokens: Pair<String, String>? = null

    private var preselectedFromCurrency: CryptoCurrency? = null
    private var preselectedToCurrency: CryptoCurrency? = null

    val approvalSlotNavigation = SlotNavigation<Unit>()

    val approvalCallback = object : GiveApprovalComponent.Callback {
        override fun onApproveClick() {}

        override fun onApproveDone() {
            val fromContractAddress = dataState.fromSwapCurrencyStatus?.currency?.getContractAddress()
            if (fromContractAddress != null) {
                allowPermissionsHandler.addAddressToInProgress(fromContractAddress)
            }
            approvalSlotNavigation.dismiss()
            updateWalletBalance()
            uiState = stateBuilder.loadingPermissionState(uiState)
            startLoadingQuotesFromLastState(isSilent = true)
        }

        override fun onApproveFailed() {
            approvalSlotNavigation.dismiss()
            showAlert()
        }

        override fun onCancelClick() {
            approvalSlotNavigation.dismiss()
            startLoadingQuotesFromLastState(isSilent = true)
        }
    }

    init {
        subscribeToTokenSelection()

        modelScope.launch {
            val storyId = StoryContentIds.STORY_FIRST_TIME_SWAP.id
            if (shouldShowStoriesUseCase.invokeSync(storyId)) {
                router.push(
                    AppRoute.Stories(
                        storyId = storyId,
                        nextScreen = null,
                        screenSource = params.screenSource,
                    ),
                )
            }
        }

        userCountry = getUserCountryUseCase.invokeSync().getOrNull()
            ?: UserCountry.Other(Locale.getDefault().country)

        initTokens()

        getBalanceHidingSettingsUseCase().onEach { settings ->
            isBalanceHidden = settings.isBalanceHidden
            uiState = stateBuilder.updateBalanceHiddenState(uiState, isBalanceHidden)
        }.launchIn(modelScope)
    }

    fun onStart() {
        startLoadingQuotesFromLastState(true)
    }

    fun onStop() {
        singleTaskScheduler.cancelTask()
    }

    override fun onDestroy() {
        singleTaskScheduler.cancelTask()
        performanceTracker.onDestroy()
        super.onDestroy()
    }

    private fun subscribeToTokenSelection() {
        fun sendAnalytics(result: ChooseTokenResult, direction: String) {
            result.analyticsPayload.forEach { analyticPayload ->
                when (analyticPayload) {
                    is ChooseTokenAnalyticsPayload.IsMarketTokenSelected -> {
                        if (analyticPayload.value) {
                            analyticsEventHandler.send(
                                SwapEvents.ChoosePopularToken(
                                    direction = direction,
                                    currency = result.currency.currency,
                                ),
                            )
                        }
                    }
                    else -> Unit
                }
            }
        }

        chooseFromTokenBridge.onCurrencyChosen.receiveAsFlow()
            .onEach { result ->
                onTokenSelect(result = result, isFromDirection = true)
                sendAnalytics(result = result, direction = "From")
            }
            .launchIn(modelScope)

        chooseFromTokenBridge.onClose.receiveAsFlow()
            .onEach {
                analyticsEventHandler.send(SwapEvents.ChooseTokenScreenResult(isTokenChosen = false))
                router.pop()
            }
            .launchIn(modelScope)

        chooseToTokenBridge.onCurrencyChosen.receiveAsFlow()
            .onEach { result ->
                onTokenSelect(result, isFromDirection = false)
                sendAnalytics(result = result, direction = "To")
            }
            .launchIn(modelScope)

        chooseToTokenBridge.onClose.receiveAsFlow()
            .onEach {
                analyticsEventHandler.send(SwapEvents.ChooseTokenScreenResult(isTokenChosen = false))
                router.pop()
            }
            .launchIn(modelScope)
    }

    private fun initTokens() {
        modelScope.launch(dispatchers.default) {
            isAccountsMode = isAccountsModeEnabledUseCase.invokeSync()

            val (fromSwapCurrencyStatus, toSwapCurrencyStatus) = initialCurrenciesResolver(
                userWalletId = params.userWalletId,
                initialCryptoCurrency = initialCryptoCurrency,
                swapCurrencyPosition = params.currencyPosition,
                isPaymentAccount = params.tangemPayInput != null,
            )

            preselectedFromCurrency = fromSwapCurrencyStatus?.currency
            preselectedToCurrency = toSwapCurrencyStatus?.currency

            analyticsEventHandler.send(
                SwapEvents.SwapScreenOpened(
                    fromCurrency = fromSwapCurrencyStatus?.currency,
                    toCurrency = toSwapCurrencyStatus?.currency,
                ),
            )

            dataState = dataState.copy(
                fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                toSwapCurrencyStatus = toSwapCurrencyStatus,
            )

            selectWalletInSelector(
                fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                toSwapCurrencyStatus = toSwapCurrencyStatus,
            )
            filterTokensFromSelector()

            if (fromSwapCurrencyStatus != null) {
                updateFeePaidCryptoCurrencyFor(fromSwapCurrencyStatus)
                subscribeToCoinBalanceUpdatesIfNeeded()
            }

            uiState = stateBuilder.createInitialReadyState(
                uiStateHolder = uiState,
                emptyAmountState = SwapState.EmptyAmountState(
                    zeroAmountEquivalent = stringReference(
                        BigDecimal.ZERO.format {
                            fiat(
                                fiatCurrencyCode = selectedAppCurrencyFlow.value.code,
                                fiatCurrencySymbol = selectedAppCurrencyFlow.value.symbol,
                            )
                        },
                    ),
                ),
                fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                toSwapCurrencyStatus = toSwapCurrencyStatus,
            )

            // Check swap availability if there is pair
            if (fromSwapCurrencyStatus != null && toSwapCurrencyStatus != null) {
                initSwapPairs(
                    fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                    toSwapCurrencyStatus = toSwapCurrencyStatus,
                )
            }
        }
    }

    @Suppress("LongMethod")
    private suspend fun onTokenSelect(result: ChooseTokenResult, isFromDirection: Boolean) {
        val selectedUserWallet = result.wallet
        val selectedCurrencyStatus = result.currency
        val selectedAccount = result.account.account

        val (fromSwapCurrencyStatus, toSwapCurrencyStatus) = if (isFromDirection) {
            SwapCurrencyStatus(
                userWallet = selectedUserWallet,
                status = selectedCurrencyStatus,
                account = selectedAccount,
            ) to dataState.toSwapCurrencyStatus
        } else {
            dataState.fromSwapCurrencyStatus to SwapCurrencyStatus(
                userWallet = selectedUserWallet,
                status = selectedCurrencyStatus,
                account = selectedAccount,
            )
        }

        if (dataState.fromSwapCurrencyStatus != null) {
            isAmountChangedByUser = true
        }

        // Check whether pair was already selected
        if (fromSwapCurrencyStatus == dataState.fromSwapCurrencyStatus &&
            toSwapCurrencyStatus == dataState.toSwapCurrencyStatus
        ) {
            startLoadingQuotesFromLastState(true)
            return
        }

        sendPreselectedTokenChangedIfNeeded(
            isFromDirection = isFromDirection,
            selectedCurrency = selectedCurrencyStatus.currency,
        )

        dataState = if (isFromDirection) {
            // Reset amount if from token is changed
            lastAmount.value = INITIAL_AMOUNT
            lastReducedBalanceBy.value = BigDecimal.ZERO
            SwapProcessDataState(
                fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                toSwapCurrencyStatus = toSwapCurrencyStatus,
            )
        } else {
            dataState.copy(
                fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                toSwapCurrencyStatus = toSwapCurrencyStatus,
            )
        }
        filterTokensFromSelector()
        uiState = stateBuilder.updateCurrenciesState(
            uiStateHolder = uiState,
            emptyAmountState = SwapState.EmptyAmountState(
                zeroAmountEquivalent = stringReference(
                    BigDecimal.ZERO.format {
                        fiat(
                            fiatCurrencyCode = selectedAppCurrencyFlow.value.code,
                            fiatCurrencySymbol = selectedAppCurrencyFlow.value.symbol,
                        )
                    },
                ),
            ),
            fromSwapCurrencyStatus = fromSwapCurrencyStatus,
            toSwapCurrencyStatus = toSwapCurrencyStatus,
            shouldResetAmount = isFromDirection,
        )

        router.pop()

        if (isFromDirection && fromSwapCurrencyStatus != null) {
            updateFeePaidCryptoCurrencyFor(fromSwapCurrencyStatus)
        }

        subscribeToCoinBalanceUpdatesIfNeeded()

        // Check swap availability if there is pair
        if (fromSwapCurrencyStatus != null && toSwapCurrencyStatus != null) {
            initSwapPairs(
                fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                toSwapCurrencyStatus = toSwapCurrencyStatus,
            )
        }
    }

    private fun sendPreselectedTokenChangedIfNeeded(isFromDirection: Boolean, selectedCurrency: CryptoCurrency) {
        val preselected = if (isFromDirection) preselectedFromCurrency else preselectedToCurrency
        if (preselected == null || preselected == selectedCurrency) return

        analyticsEventHandler.send(
            SwapEvents.PreselectedTokenChanged(
                direction = if (isFromDirection) "From" else "To",
                preSelectedToken = preselected,
                selectedToken = selectedCurrency,
            ),
        )

        if (isFromDirection) {
            preselectedFromCurrency = null
        } else {
            preselectedToCurrency = null
        }
    }

    private fun onChangeCardsClicked() {
        modelScope.launch {
            singleTaskScheduler.cancelTask()

            val newFromSwapCurrencyStatus = dataState.toSwapCurrencyStatus
            val newToSwapCurrencyStatus = dataState.fromSwapCurrencyStatus

            isAmountChangedByUser = true

            lastAmount.value = INITIAL_AMOUNT
            lastReducedBalanceBy.value = BigDecimal.ZERO

            dataState = SwapProcessDataState(
                fromSwapCurrencyStatus = newFromSwapCurrencyStatus,
                toSwapCurrencyStatus = newToSwapCurrencyStatus,
                pairs = dataState.pairs,
                selectedPairProviders = dataState.selectedPairProviders,
            )
            filterTokensFromSelector()
            uiState = stateBuilder.updateCurrenciesState(
                uiStateHolder = uiState,
                emptyAmountState = SwapState.EmptyAmountState(
                    zeroAmountEquivalent = stringReference(
                        BigDecimal.ZERO.format {
                            fiat(
                                fiatCurrencyCode = selectedAppCurrencyFlow.value.code,
                                fiatCurrencySymbol = selectedAppCurrencyFlow.value.symbol,
                            )
                        },
                    ),
                ),
                fromSwapCurrencyStatus = newFromSwapCurrencyStatus,
                toSwapCurrencyStatus = newToSwapCurrencyStatus,
                shouldResetAmount = true,
            )

            if (newFromSwapCurrencyStatus != null && newToSwapCurrencyStatus != null) {
                updateFeePaidCryptoCurrencyFor(newFromSwapCurrencyStatus)
                val toProvidersList = swapInteractor.findProvidersForPairWithCheck(
                    fromSwapCurrencyStatus = newFromSwapCurrencyStatus,
                    toSwapCurrencyStatus = newToSwapCurrencyStatus,
                    pairs = dataState.pairs,
                )
                if (toProvidersList.isEmpty()) {
                    handleSwapNotSupported(
                        fromSwapCurrencyStatus = newFromSwapCurrencyStatus,
                        toSwapCurrencyStatus = newToSwapCurrencyStatus,
                    )
                } else {
                    startLoadingQuotes(
                        fromSwapCurrencyStatus = newFromSwapCurrencyStatus,
                        toSwapCurrencyStatus = newToSwapCurrencyStatus,
                        amount = lastAmount.value,
                        reduceBalanceBy = lastReducedBalanceBy.value,
                        toProvidersList = toProvidersList,
                    )
                }
            }
        }
    }

    private fun initSwapPairs(fromSwapCurrencyStatus: SwapCurrencyStatus, toSwapCurrencyStatus: SwapCurrencyStatus) {
        modelScope.launch {
            uiState = stateBuilder.createInitialLoadingState(
                uiStateHolder = uiState,
                fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                toSwapCurrencyStatus = toSwapCurrencyStatus,
            )
            swapInteractor.getPair(
                fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                toSwapCurrencyStatus = toSwapCurrencyStatus,
                filterProviderTypes = if (tangemPayInput?.isWithdrawal == true) {
                    listOf(ExchangeProviderType.CEX)
                } else {
                    ExchangeProviderType.getSwapProviderTypes()
                },
            ).fold(
                ifLeft = { error ->
                    uiState = stateBuilder.createInitialErrorState(
                        uiStateHolder = uiState,
                        fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                        expressError = error,
                        onRetry = { retrySwapPairs(fromSwapCurrencyStatus, toSwapCurrencyStatus) },
                    )
                    TangemLogger.e("Error getting swap pair", error)
                },
                ifRight = { pairs ->
                    val providerList = swapInteractor.findProvidersForPairWithCheck(
                        fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                        toSwapCurrencyStatus = toSwapCurrencyStatus,
                        pairs = pairs,
                    )
                    if (providerList.isEmpty()) {
                        handleSwapNotSupported(
                            fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                            toSwapCurrencyStatus = toSwapCurrencyStatus,
                        )
                    } else {
                        uiState = stateBuilder.updateCurrenciesState(
                            uiStateHolder = uiState,
                            emptyAmountState = SwapState.EmptyAmountState(
                                zeroAmountEquivalent = stringReference(
                                    BigDecimal.ZERO.format {
                                        fiat(
                                            fiatCurrencyCode = selectedAppCurrencyFlow.value.code,
                                            fiatCurrencySymbol = selectedAppCurrencyFlow.value.symbol,
                                        )
                                    },
                                ),
                            ),
                            fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                            toSwapCurrencyStatus = toSwapCurrencyStatus,
                            shouldResetAmount = false,
                        )
                        dataState = dataState.copy(
                            pairs = pairs,
                            selectedPairProviders = providerList,
                        )
                        startLoadingQuotes(
                            amount = lastAmount.value,
                            reduceBalanceBy = lastReducedBalanceBy.value,
                            fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                            toSwapCurrencyStatus = toSwapCurrencyStatus,
                            toProvidersList = providerList,
                        )
                    }
                },
            )
        }.saveIn(swapPairsJobHolder)
    }

    private fun retrySwapPairs(fromSwapCurrencyStatus: SwapCurrencyStatus, toSwapCurrencyStatus: SwapCurrencyStatus) {
        if (swapPairsJobHolder.isActive) return
        initSwapPairs(fromSwapCurrencyStatus, toSwapCurrencyStatus)
    }

    @Suppress("UnusedPrivateMember")
    private fun subscribeToCoinBalanceUpdatesIfNeeded() {
        val fromSwapCurrencyStatus = dataState.fromSwapCurrencyStatus
        val toSwapCurrencyStatus = dataState.toSwapCurrencyStatus
        if (fromSwapCurrencyStatus != null) {
            subscribeToCoinBalanceUpdates(
                swapCurrencyStatus = fromSwapCurrencyStatus,
                isFromCurrency = true,
            )
        }

        if (toSwapCurrencyStatus != null) {
            subscribeToCoinBalanceUpdates(
                swapCurrencyStatus = toSwapCurrencyStatus,
                isFromCurrency = false,
            )
        }
    }

    private fun startLoadingQuotes(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
        amount: String,
        reduceBalanceBy: BigDecimal,
        toProvidersList: List<SwapProvider>,
        isSilent: Boolean = false,
        updateFeeBlock: Boolean = true,
    ) {
        singleTaskScheduler.cancelTask()
        if (amount.isBlank()) return
        if (!isSilent) {
            uiState = stateBuilder.createQuotesLoadingState(
                fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                toSwapCurrencyStatus = toSwapCurrencyStatus,
                uiStateHolder = uiState,
            )
            feeSelectorRepository.state.value = FeeSelectorUM.Loading
            performanceTracker.onLoadingStarted(toProvidersList.size)
        }
        singleTaskScheduler.scheduleTask(
            modelScope,
            loadQuotesTask(
                fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                toSwapCurrencyStatus = toSwapCurrencyStatus,
                amount = amount,
                reduceBalanceBy = reduceBalanceBy,
                toProvidersList = toProvidersList,
                updateFeeBlock = updateFeeBlock,
            ),
        )
    }

    private fun startLoadingQuotesFromLastState(isSilent: Boolean = false, updateFeeBlock: Boolean = true) {
        val fromSwapCurrencyStatus = dataState.fromSwapCurrencyStatus
        val toSwapCurrencyStatus = dataState.toSwapCurrencyStatus
        val amount = dataState.amount
        if (fromSwapCurrencyStatus != null && toSwapCurrencyStatus != null && amount != null) {
            startLoadingQuotes(
                fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                toSwapCurrencyStatus = toSwapCurrencyStatus,
                amount = amount,
                isSilent = isSilent,
                reduceBalanceBy = dataState.reduceBalanceBy,
                toProvidersList = dataState.selectedPairProviders,
                updateFeeBlock = updateFeeBlock,
            )
        }
    }

    private suspend fun updateFeePaidCryptoCurrencyFor(fromSwapCurrencyStatus: SwapCurrencyStatus) {
        val fromCryptoCurrency = fromSwapCurrencyStatus.currency
        val feePaidCryptoCurrency = if (fromSwapCurrencyStatus.account is Account.Payment) {
            fromSwapCurrencyStatus.status
        } else {
            getFeePaidCryptoCurrencyStatusSyncUseCase(
                userWalletId = fromSwapCurrencyStatus.userWalletId,
                cryptoCurrencyStatus = fromSwapCurrencyStatus.status,
            ).onLeft {
                TangemLogger.e(
                    messageString = "Unable to get fee paid crypto currency status for ${fromCryptoCurrency.id}",
                    shouldSanitize = false,
                )
            }.onRight { currencyStatus ->
                if (currencyStatus == null) {
                    TangemLogger.e(
                        messageString = "Fee paid crypto currency status is null for ${fromCryptoCurrency.id}",
                        shouldSanitize = false,
                    )
                }
            }.getOrNull() ?: fromSwapCurrencyStatus.status
        }

        dataState = dataState.copy(feePaidCryptoCurrency = feePaidCryptoCurrency)
    }

    private fun loadQuotesTask(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
        amount: String,
        reduceBalanceBy: BigDecimal,
        toProvidersList: List<SwapProvider>,
        updateFeeBlock: Boolean = true,
    ): PeriodicTask<Map<SwapProvider, SwapState>> {
        var shouldUpdateFeeBlock = updateFeeBlock
        return PeriodicTask(
            delay = UPDATE_DELAY,
            task = {
                uiState = stateBuilder.createSilentLoadState(uiState)
                runCatching(dispatchers.io) {
                    dataState = dataState.copy(
                        amount = amount,
                        reduceBalanceBy = reduceBalanceBy,
                        swapDataModel = null,
                    )
                    swapInteractor.findBestQuote(
                        fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                        toSwapCurrencyStatus = toSwapCurrencyStatus,
                        providers = toProvidersList,
                        amountToSwap = amount,
                        reduceBalanceBy = reduceBalanceBy,
                        txFeeSealedState = getSelectedFeeState(),
                    )
                }
            },
            onSuccess = { providersState ->
                performanceTracker.onLoadingFinished(
                    hasError = providersState.values.none { it is SwapState.QuotesLoadedState },
                )
                if (providersState.isNotEmpty()) {
                    val (provider, state) = updateLoadedQuotes(providersState)
                    setupLoadedState(
                        provider = provider,
                        state = state,
                        fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                        toSwapCurrencyStatus = toSwapCurrencyStatus,
                    )
                    val successStates = providersState.getLastLoadedSuccessStates()
                    val pricesLowerBest = getPricesLowerBest(provider.providerId, successStates)
                    uiState = stateBuilder.updateProvidersBottomSheetContent(
                        uiState = uiState,
                        pricesLowerBest = pricesLowerBest,
                        tokenSwapInfoForProviders = successStates.entries
                            .associate { it.key.providerId to it.value.toTokenInfo },
                    )
                    if (shouldUpdateFeeBlock) {
                        modelScope.launch { feeSelectorReloadTrigger.triggerUpdate() }
                    } else {
                        shouldUpdateFeeBlock = true
                    }
                } else {
                    feeSelectorRepository.state.value =
                        FeeSelectorUM.Error(GetFeeError.UnknownError, isHidden = true)
                    TangemLogger.e("Accidentally empty quotes list")
                }
            },
            onError = { error ->
                TangemLogger.e("Error when loading quotes", error)
                performanceTracker.onLoadingFinished(hasError = true)
                feeSelectorRepository.state.value = FeeSelectorUM.Error(GetFeeError.UnknownError, isHidden = true)
                uiState = stateBuilder.addNotification(uiState, null) { startLoadingQuotesFromLastState() }
            },
        )
    }

    private fun setupLoadedState(
        provider: SwapProvider,
        state: SwapState,
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
    ) {
        when (state) {
            is SwapState.QuotesLoadedState -> {
                setupQuotesLoadedUiState(provider, state)
                sendAnalyticsForNotifications(provider, fromSwapCurrencyStatus.status, toSwapCurrencyStatus.status)
                updatePermissionNotificationState(state)
            }
            is SwapState.EmptyAmountState -> {
                setupEmptyAmountUiState(state, fromSwapCurrencyStatus)
                lastPermissionNotificationTokens = null
            }
            is SwapState.SwapError -> {
                setupErrorUiState(provider, state)
                lastPermissionNotificationTokens = null
            }
        }
    }

    private fun setupQuotesLoadedUiState(provider: SwapProvider, state: SwapState.QuotesLoadedState) {
        fillLoadedDataState(state.permissionState, state.swapDataModel)
        val loadedStates = dataState.lastLoadedSwapStates.getLastLoadedSuccessStates()
        val bestRatedProviderId = findBestQuoteProvider(loadedStates)?.providerId ?: provider.providerId
        uiState = stateBuilder.createQuotesLoadedState(
            uiStateHolder = uiState,
            quoteModel = state,
            feeCryptoCurrencyStatus = dataState.feePaidCryptoCurrency,
            swapProvider = provider,
            bestRatedProviderId = bestRatedProviderId,
            isNeedBestRateBadge = dataState.lastLoadedSwapStates.consideredProvidersStates().size > 1,
            selectedFeeType = (getSelectedFee() as? TxFee.Legacy)?.feeType ?: FeeType.NORMAL,
            needApplyFCARestrictions = userCountry.needApplyFCARestrictions(),
            hideFee = isTangemPayWithdrawal(),
        )
    }

    private fun sendAnalyticsForNotifications(
        provider: SwapProvider,
        fromToken: CryptoCurrencyStatus,
        toToken: CryptoCurrencyStatus?,
    ) {
        if (uiState.notifications.any { it is SwapNotificationUM.Error.UnableToCoverFeeWarning }) {
            analyticsEventHandler.send(
                SwapEvents.NoticeNotEnoughFee(
                    token = fromToken.currency.symbol,
                    blockchain = fromToken.currency.network.name,
                ),
            )
        }
        if (toToken != null && uiState.notifications.any { it is SwapNotificationUM.Warning.HighPriceImpact }) {
            analyticsEventHandler.send(
                SwapEvents.HighPriceImpact(
                    sendToken = fromToken.currency.symbol,
                    receiveToken = toToken.currency.symbol,
                    sendBlockchain = fromToken.currency.network.name,
                    receiveBlockchain = toToken.currency.network.name,
                    providerName = provider.name,
                ),
            )
        }
        if (toToken != null && uiState.notifications.any { it is SwapNotificationUM.Warning.TradeTooHigh }) {
            analyticsEventHandler.send(
                SwapEvents.TradeTooLarge(
                    sendToken = fromToken.currency.symbol,
                    receiveToken = toToken.currency.symbol,
                    sendBlockchain = fromToken.currency.network.name,
                    receiveBlockchain = toToken.currency.network.name,
                    providerName = provider.name,
                ),
            )
        }
    }

    private fun updatePermissionNotificationState(state: SwapState.QuotesLoadedState) {
        val fromCryptoCurrencyStatus = state.fromTokenInfo.swapCurrencyStatus
        val toCryptoCurrencyStatus = state.toTokenInfo.swapCurrencyStatus
        val fromTokenId = fromCryptoCurrencyStatus.currency.id.value
        val toTokenId = toCryptoCurrencyStatus.currency.id.value
        val currentTokenPair = Pair(fromTokenId, toTokenId)

        when {
            uiState.notifications.none { it is SwapNotificationUM.Info.PermissionNeeded } -> {
                lastPermissionNotificationTokens = null
            }
            lastPermissionNotificationTokens != currentTokenPair -> {
                sendNoticePermissionNeededEvent()
                lastPermissionNotificationTokens = currentTokenPair
            }
        }
    }

    private fun setupEmptyAmountUiState(
        state: SwapState.EmptyAmountState,
        fromSwapCurrencyStatus: SwapCurrencyStatus,
    ) {
        uiState = stateBuilder.createQuotesEmptyAmountState(
            uiStateHolder = uiState,
            emptyAmountState = state,
            fromSwapCurrencyStatus = fromSwapCurrencyStatus,
        )
    }

    private fun setupErrorUiState(provider: SwapProvider, state: SwapState.SwapError) {
        singleTaskScheduler.cancelTask()
        uiState = stateBuilder.createQuotesErrorState(
            uiStateHolder = uiState,
            swapProvider = provider,
            fromToken = state.fromTokenInfo,
            toSwapCurrencyStatus = dataState.toSwapCurrencyStatus,
            expressDataError = state.error,
            includeFeeInAmount = state.includeFeeInAmount,
            needApplyFCARestrictions = userCountry.needApplyFCARestrictions(),
        )
        sendErrorAnalyticsEvent(state.error, provider)
    }

    private fun sendErrorAnalyticsEvent(error: ExpressDataError, provider: SwapProvider) {
        val fromCryptoCurrency = dataState.fromSwapCurrencyStatus?.currency?.let { currency ->
            "${currency.network.rawId}:${currency.symbol}"
        }
        val toCryptoCurrency = dataState.toSwapCurrencyStatus?.currency?.let { currency ->
            "${currency.network.rawId}:${currency.symbol}"
        }
        analyticsErrorEventHandler.sendErrorEvent(
            SwapEvents.NoticeProviderError(
                sendToken = fromCryptoCurrency.orEmpty(),
                receiveToken = toCryptoCurrency.orEmpty(),
                provider = provider,
                errorCode = error.code,
                errorMessage = error.message,
            ),
        )
    }

    private fun updateLoadedQuotes(state: Map<SwapProvider, SwapState>): Pair<SwapProvider, SwapState> {
        val nonEmptyStates = state.filter { entry -> entry.value !is SwapState.EmptyAmountState }
        val selectedSwapProvider = if (nonEmptyStates.isNotEmpty()) {
            selectProvider(state)
        } else {
            null
        }
        dataState = dataState.copy(
            selectedProvider = selectedSwapProvider,
            lastLoadedSwapStates = state,
        )
        if (selectedSwapProvider != null) {
            return nonEmptyStates.entries.first { entry -> entry.key == selectedSwapProvider }.toPair()
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

    private fun fillLoadedDataState(permissionState: PermissionDataState, swapDataModel: SwapDataModel?) {
        dataState = if (permissionState is PermissionDataState.PermissionRequired) {
            dataState.copy()
        } else {
            dataState.copy(
                swapDataModel = swapDataModel,
            )
        }
    }

    @Suppress("LongMethod")
    private fun onSwapClick() {
        singleTaskScheduler.cancelTask()
        uiState = stateBuilder.createSwapInProgressState(uiState)
        val provider = requireNotNull(dataState.selectedProvider) { "Selected provider is null" }
        val lastLoadedQuotesState = dataState.lastLoadedSwapStates[provider] as? SwapState.QuotesLoadedState
        if (lastLoadedQuotesState == null) {
            TangemLogger.e("Last loaded quotes state is null")
            return
        }
        val fromSwapCurrencyStatus = requireNotNull(dataState.fromSwapCurrencyStatus)
        val toSwapCurrencyStatus = requireNotNull(dataState.toSwapCurrencyStatus)
        val fee = getSelectedFee()
        val isTangemPayWithdrawal = isTangemPayWithdrawal()

        if (fee == null && !isTangemPayWithdrawal) {
            TangemLogger.e("onSwapClick: fee is null and isWithdrawal is ${tangemPayInput?.isWithdrawal}")
            showAlert(resourceReference(R.string.swapping_fee_estimation_error_text))
            modelScope.launch {
                delay(SWAP_IN_PROGRESS_DELAY)
                startLoadingQuotesFromLastState()
            }
            return
        }
        modelScope.launch(dispatchers.main) {
            runCatching(dispatchers.io) {
                swapInteractor.onSwap(
                    fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                    toSwapCurrencyStatus = toSwapCurrencyStatus,
                    swapProvider = provider,
                    swapData = dataState.swapDataModel,
                    amountToSwap = requireNotNull(dataState.amount),
                    includeFeeInAmount = lastLoadedQuotesState.preparedSwapConfigState.includeFeeInAmount,
                    fee = fee,
                    expressOperationType = ExpressOperationType.SWAP,
                    isTangemPayWithdrawal = isTangemPayWithdrawal,
                )
            }.onSuccess { swapTransactionState ->
                when (swapTransactionState) {
                    is SwapTransactionState.TxSent -> {
                        TangemLogger.i("onSwapClick: onSuccess: txHash: $swapTransactionState", shouldSanitize = false)
                        if (fee == null) {
                            TangemLogger.e("onSwapClick: onSuccess: fee is null after swap")
                            showAlert(resourceReference(R.string.swapping_fee_estimation_error_text))
                            return@onSuccess
                        }
                        sendSuccessSwapEvent(
                            fromSwapCurrencyStatus.currency,
                            (getSelectedFee() as? TxFee.Legacy)?.feeType ?: FeeType.NORMAL,
                        )
                        val url = getExplorerTransactionUrlUseCase(
                            txHash = swapTransactionState.txHash,
                            currency = fromSwapCurrencyStatus.currency,
                        ).getOrElse {
                            TangemLogger.i("tx hash explore not supported")
                            ""
                        }

                        updateWalletBalance()
                        uiState = stateBuilder.createSuccessState(
                            uiState = uiState,
                            swapTransactionState = swapTransactionState,
                            dataState = dataState,
                            txUrl = url,
                            onExploreClick = {
                                if (swapTransactionState.txHash.isNotEmpty()) {
                                    urlOpener.openUrl(url)
                                }
                                analyticsEventHandler.send(
                                    event = SwapEvents.ButtonExplore(fromSwapCurrencyStatus.currency.symbol),
                                )
                            },
                            onStatusClick = {
                                val txExternalUrl = swapTransactionState.txExternalUrl
                                if (!txExternalUrl.isNullOrBlank()) {
                                    urlOpener.openUrl(txExternalUrl)
                                    analyticsEventHandler.send(
                                        event = SwapEvents.ButtonStatus(fromSwapCurrencyStatus.currency.symbol),
                                    )
                                }
                            },
                        )
                        sendSuccessEvent()

                        router.replaceAll(SwapRoute.Success)
                    }
                    SwapTransactionState.DemoMode -> {
                        showDemoModeAlert()
                    }
                    is SwapTransactionState.Error -> {
                        TangemLogger.e(
                            messageString = "onSwapClick: swap transaction error: $swapTransactionState",
                            shouldSanitize = false,
                        )
                        startLoadingQuotesFromLastState()
                        showTransactionErrorAlert(swapTransactionState)
                    }
                    is SwapTransactionState.TangemPayWithdrawalData -> {
                        processTangemPayWithdrawal(
                            fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                            swapTransactionState = swapTransactionState,
                        )
                    }
                }
            }.onFailure { error ->
                TangemLogger.e("Error", error)
                startLoadingQuotesFromLastState()
                showAlert()
            }
        }
    }

    private suspend fun processTangemPayWithdrawal(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        swapTransactionState: SwapTransactionState.TangemPayWithdrawalData,
    ) {
        tangemPayWithdrawUseCase(
            userWallet = fromSwapCurrencyStatus.userWallet,
            cryptoAmount = swapTransactionState.cryptoAmount,
            cryptoCurrencyId = swapTransactionState.cryptoCurrencyId,
            receiverCexAddress = swapTransactionState.cexAddress,
            exchangeData = swapTransactionState.exchangeData,
        )
            .onLeft {
                startLoadingQuotesFromLastState()
                onTangemPayWithdrawalError(swapTransactionState.storeData.txExternalId)
            }
            .onRight { result: WithdrawalResult ->
                when (result) {
                    WithdrawalResult.Cancelled -> {
                        startLoadingQuotesFromLastState()
                    }
                    WithdrawalResult.Success -> {
                        val txUrl = swapTransactionState.storeData.txExternalUrl
                        swapInteractor.storeSwapTransaction(
                            fromSwapCurrencyStatus = swapTransactionState.storeData.fromSwapCurrencyStatus,
                            toSwapCurrencyStatus = swapTransactionState.storeData.toSwapCurrencyStatus,
                            amount = swapTransactionState.storeData.amount,
                            swapProvider = swapTransactionState.storeData.swapProvider,
                            swapDataModel = swapTransactionState.storeData.swapDataModel,
                            txExternalUrl = txUrl,
                            timestamp = System.currentTimeMillis(),
                            txExternalId = swapTransactionState.storeData.txExternalId,
                            averageDuration = null,
                        )
                        uiState = stateBuilder.createTangemPayWithdrawalSuccessState(
                            uiState = uiState,
                            swapTransactionState = swapTransactionState,
                            dataState = dataState,
                            txUrl = txUrl.orEmpty(),
                            onExploreClick = { if (txUrl != null) urlOpener.openUrl(txUrl) },
                        )
                        router.replaceAll(SwapRoute.Success)
                    }
                }
            }
    }

    private suspend fun sendSuccessEvent() {
        val provider = dataState.selectedProvider ?: return
        val fee = (getSelectedFee() as? TxFee.Legacy)?.feeType ?: FeeType.NORMAL
        val fromSwapCurrencyStatus = dataState.fromSwapCurrencyStatus ?: return
        val toSwapCurrencyStatus = dataState.toSwapCurrencyStatus ?: return
        val fromDerivationIndex = fromSwapCurrencyStatus.account.derivationIndex?.value
        val toDerivationIndex = toSwapCurrencyStatus.account.derivationIndex?.value

        analyticsEventHandler.send(
            SwapEvents.SwapInProgressScreen(
                provider = provider,
                commission = fee,
                sendBlockchain = fromSwapCurrencyStatus.currency.network.name,
                receiveBlockchain = toSwapCurrencyStatus.currency.network.name,
                sendToken = fromSwapCurrencyStatus.currency.symbol,
                receiveToken = toSwapCurrencyStatus.currency.symbol,
                feeToken = getFeeToken().symbol,
                fromDerivationIndex = fromDerivationIndex,
                toDerivationIndex = toDerivationIndex,
                referralId = appsFlyerStore.get()?.refcode,
            ),
        )
    }

    private fun subscribeToCoinBalanceUpdates(swapCurrencyStatus: SwapCurrencyStatus, isFromCurrency: Boolean) {
        val swapCurrency = swapCurrencyStatus.currency

        when (swapCurrencyStatus.account) {
            is Account.CryptoPortfolio -> getAccountCurrencyStatusUseCase(
                userWalletId = swapCurrencyStatus.userWalletId,
                currency = swapCurrency,
            ).map { (_, status) -> status }
            is Account.Payment -> getPaymentAccountCryptoCurrencyStatusUseCase(
                userWalletId = swapCurrencyStatus.userWalletId,
                cryptoCurrency = swapCurrency,
            ).map { (_, status) -> status }
        }.distinctUntilChanged { old, new -> old.value.amount == new.value.amount } // Check only balance changes
            .onEach { currencyStatus ->

                when {
                    isFromCurrency && currencyStatus.currency.id == swapCurrency.id -> {
                        dataState = dataState.copy(
                            fromSwapCurrencyStatus = swapCurrencyStatus.copy(status = currencyStatus),
                        )
                    }
                    !isFromCurrency && currencyStatus.currency.id == swapCurrency.id -> {
                        dataState = dataState.copy(
                            toSwapCurrencyStatus = swapCurrencyStatus.copy(status = currencyStatus),
                        )
                    }
                    else -> Unit
                }

                uiState = stateBuilder.updateCurrencyBalanceStatus(
                    uiState = uiState,
                    fromSwapCurrencyStatus = dataState.fromSwapCurrencyStatus,
                    toSwapCurrencyStatus = dataState.toSwapCurrencyStatus,
                    emptyAmountState = SwapState.EmptyAmountState(
                        zeroAmountEquivalent = stringReference(
                            BigDecimal.ZERO.format {
                                fiat(
                                    fiatCurrencyCode = selectedAppCurrencyFlow.value.code,
                                    fiatCurrencySymbol = selectedAppCurrencyFlow.value.symbol,
                                )
                            },
                        ),
                    ),
                )
                startLoadingQuotesFromLastState(isSilent = true)
            }
            .flowOn(dispatchers.main)
            .launchIn(modelScope)
            .saveIn(if (isFromCurrency) fromTokenBalanceJobHolder else toTokenBalanceJobHolder)
    }

    private fun onAmountChanged(
        value: String,
        forceQuotesUpdate: Boolean = false,
        reduceBalanceBy: BigDecimal = BigDecimal.ZERO,
    ) {
        modelScope.launch {
            val fromSwapCurrencyStatus = dataState.fromSwapCurrencyStatus
            val toSwapCurrencyStatus = dataState.toSwapCurrencyStatus
            if (fromSwapCurrencyStatus != null) {
                val decimals = fromSwapCurrencyStatus.currency.decimals
                val cutValue = cutAmountWithDecimals(decimals, value)
                val minTxAmount = getMinimumTransactionAmountSyncUseCase(
                    userWalletId = fromSwapCurrencyStatus.userWalletId,
                    cryptoCurrencyStatus = fromSwapCurrencyStatus.status,
                ).getOrNull()
                lastAmount.value = cutValue
                lastReducedBalanceBy.value = reduceBalanceBy
                uiState = stateBuilder.updateSwapAmount(
                    uiState = uiState,
                    amountFormatted = inputNumberFormatter.formatWithThousands(cutValue, decimals),
                    amountRaw = lastAmount.value,
                    fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                    minTxAmount = minTxAmount,
                )

                if (toSwapCurrencyStatus != null) {
                    if (toSwapCurrencyStatus.status.value.amount != null) {
                        isAmountChangedByUser = true
                    }

                    amountDebouncer.debounce(modelScope, DEBOUNCE_AMOUNT_DELAY, forceUpdate = forceQuotesUpdate) {
                        startLoadingQuotes(
                            fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                            toSwapCurrencyStatus = toSwapCurrencyStatus,
                            amount = lastAmount.value,
                            reduceBalanceBy = lastReducedBalanceBy.value,
                            toProvidersList = dataState.selectedPairProviders,
                        )
                    }
                }
            }
        }
    }

    private fun onMaxAmountClicked() {
        dataState.fromSwapCurrencyStatus?.let { fromCurrency ->
            val balance = swapInteractor.getTokenBalance(fromCurrency.status)
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
            analyticsEventHandler.send(SwapEvents.SendTokenBalanceClicked())
        }
    }

    private fun cutAmountWithDecimals(maxDecimals: Int, amount: String): String {
        return inputNumberFormatter.getValidatedNumberWithFixedDecimals(amount, maxDecimals)
    }

    private fun showAlert(message: TextReference = resourceReference(R.string.common_unknown_error)) {
        messageSender.send(SwapAlertUM.genericError(onConfirmClick = { }, message = message))
    }

    private fun showDemoModeAlert() {
        messageSender.send(
            DialogMessage(
                title = resourceReference(id = R.string.warning_demo_mode_title),
                message = resourceReference(id = R.string.warning_demo_mode_message),
                firstAction = EventMessageAction(
                    title = resourceReference(id = R.string.common_ok),
                    onClick = {},
                ),
            ),
        )
    }

    private fun showTransactionErrorAlert(
        error: SwapTransactionState.Error,
        onSupportClick: (String) -> Unit = ::onFailedTxEmailClick,
    ) {
        val errorAlert = SwapTransactionErrorStateConverter(
            onDismiss = {},
            onSupportClick = onSupportClick,
        ).convert(error)
        errorAlert?.let { messageSender.send(it) }
    }

    private fun onTangemPaySupportClick(txId: String) {
        val fromUserWalletId = dataState.fromSwapCurrencyStatus?.userWalletId ?: return

        modelScope.launch {
            val metaInfo = getWalletMetaInfoUseCase(fromUserWalletId).getOrNull() ?: return@launch
            val customerId = getTangemPayCustomerIdUseCase(fromUserWalletId).getOrNull().orEmpty()
            val email = FeedbackEmailType.Visa.Withdrawal(
                walletMetaInfo = metaInfo,
                customerId = customerId,
                providerName = dataState.selectedProvider?.name.orEmpty(),
                txId = txId,
            )
            analyticsEventHandler.send(Basic.ButtonSupport(source = ScreensSources.Swap))
            sendFeedbackEmailUseCase(email)
        }
    }

    private fun showSwapInfoAlert(isPriceImpact: Boolean, token: String, provider: SwapProvider) {
        messageSender.send(
            SwapAlertUM.informationAlert(
                message = buildSwapInfoMessage(isPriceImpact, token, provider),
                onConfirmClick = {},
            ),
        )
    }

    private fun buildSwapInfoMessage(isPriceImpact: Boolean, token: String, provider: SwapProvider): TextReference {
        val slippage = provider.slippage?.let { "${it.parseBigDecimal(1)}%" }
        val messages = buildList {
            when (provider.type) {
                ExchangeProviderType.CEX -> {
                    if (slippage != null) {
                        add(
                            resourceReference(
                                id = R.string.swapping_alert_cex_description_with_slippage,
                                formatArgs = wrappedList(token, slippage),
                            ),
                        )
                    } else {
                        add(resourceReference(R.string.swapping_alert_cex_description, wrappedList(token)))
                    }
                }
                ExchangeProviderType.DEX,
                ExchangeProviderType.DEX_BRIDGE,
                -> {
                    if (isPriceImpact) {
                        add(resourceReference(R.string.swapping_high_price_impact_description))
                        add(stringReference("\n\n"))
                    }
                    if (slippage != null) {
                        add(
                            resourceReference(
                                id = R.string.swapping_alert_dex_description_with_slippage,
                                formatArgs = wrappedList(slippage),
                            ),
                        )
                    } else {
                        add(resourceReference(R.string.swapping_alert_dex_description, wrappedList(token)))
                    }
                }
            }
        }
        return combinedReference(messages.toWrappedList())
    }

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    private fun createUiActions(): UiActions {
        return UiActions(
            onAmountChanged = { onAmountChanged(it) },
            onSwapClick = {
                onSwapClick()
                val sendTokenSymbol = dataState.fromSwapCurrencyStatus?.currency?.symbol
                val receiveTokenSymbol = dataState.toSwapCurrencyStatus?.currency?.symbol
                if (sendTokenSymbol != null && receiveTokenSymbol != null) {
                    analyticsEventHandler.send(
                        SwapEvents.ButtonSwapClicked(
                            sendToken = sendTokenSymbol,
                            receiveToken = receiveTokenSymbol,
                        ),
                    )
                }
            },
            onChangeCardsClicked = {
                onChangeCardsClicked()
                analyticsEventHandler.send(SwapEvents.ButtonSwipeClicked())
            },
            onBackClicked = {
                val bottomSheet = uiState.bottomSheetConfig
                if (bottomSheet != null && bottomSheet.isShown) {
                    uiState = stateBuilder.dismissBottomSheet(uiState)
                } else {
                    router.pop()
                }
            },
            onMaxAmountSelected = ::onMaxAmountClicked,
            onReduceToAmount = ::onReduceAmountClicked,
            onReduceByAmount = ::onReduceAmountClicked,
            openPermissionBottomSheet = {
                singleTaskScheduler.cancelTask()
                sendGivePermissionClickedEvent()
                approvalSlotNavigation.activate(Unit)
            },
            onAmountSelected = { onAmountSelected(it) },
            onClickFee = {
                val selectedFee = (getSelectedFee() as? TxFee.Legacy)?.feeType ?: FeeType.NORMAL
                val txFeeState =
                    dataState.getCurrentLoadedSwapState()?.txFee as? TxFeeState.MultipleFeeState ?: return@UiActions
                modelScope.launch {
                    val readMoreUrl = TangemBlogUrlBuilder.build(TangemBlogUrlBuilder.Post.WhatIsTransactionFee)
                    uiState = stateBuilder.showSelectFeeBottomSheet(
                        uiState = uiState,
                        selectedFee = selectedFee,
                        txFeeState = txFeeState,
                        readMoreUrl = readMoreUrl,
                    ) {
                        uiState = stateBuilder.dismissBottomSheet(uiState)
                    }
                }
            },
            onSelectFeeType = { txFee ->
                uiState = stateBuilder.dismissBottomSheet(uiState)
                dataState = dataState.copy(selectedFee = txFee)
                modelScope.launch(dispatchers.io) {
                    startLoadingQuotesFromLastState(false)
                }
            },
            onProviderClick = { providerId ->
                analyticsEventHandler.send(SwapEvents.ProviderClicked())
                val states = dataState.lastLoadedSwapStates.getLastLoadedSuccessStates()
                val pricesLowerBest = getPricesLowerBest(providerId, states)
                uiState = stateBuilder.showSelectProviderBottomSheet(
                    uiState = uiState,
                    selectedProviderId = providerId,
                    pricesLowerBest = pricesLowerBest,
                    providersStates = dataState.lastLoadedSwapStates,
                    needApplyFCARestrictions = userCountry.needApplyFCARestrictions(),
                ) { uiState = stateBuilder.dismissBottomSheet(uiState) }
            },
            onProviderSelect = { providerId ->
                val provider = findAndSelectProvider(providerId)
                val swapState = dataState.lastLoadedSwapStates[provider]
                val fromSwapCurrencyStatus = dataState.fromSwapCurrencyStatus
                val toSwapCurrencyStatus = dataState.toSwapCurrencyStatus
                val isNotNullCurrency = fromSwapCurrencyStatus != null && toSwapCurrencyStatus != null
                if (provider != null && swapState != null && isNotNullCurrency) {
                    modelScope.launch {
                        feeSelectorRepository.state.value = FeeSelectorUM.Loading
                        feeSelectorReloadTrigger.triggerUpdate()
                    }
                    analyticsEventHandler.send(SwapEvents.ProviderChosen(provider))
                    uiState = stateBuilder.dismissBottomSheet(uiState)
                    setupLoadedState(
                        provider = provider,
                        state = swapState,
                        fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                        toSwapCurrencyStatus = toSwapCurrencyStatus,
                    )
                }
            },
            onRetryClick = {
                startLoadingQuotesFromLastState()
            },
            onReceiveCardWarningClick = {
                val selectedProvider = dataState.selectedProvider ?: return@UiActions
                val currencySymbol = dataState.toSwapCurrencyStatus?.currency?.symbol ?: return@UiActions
                val isPriceImpact = uiState.priceImpact.type != PriceImpact.Type.NONE
                showSwapInfoAlert(isPriceImpact, currencySymbol, selectedProvider)
            },
            onLinkClick = urlOpener::openUrl,
            onSelectTokenClick = { direction ->
                singleTaskScheduler.cancelTask() // Need to stop auto quotes fetching
                router.push(
                    SwapRoute.SelectToken(isFromDirection = direction == TokenSelectionDirection.FROM),
                )
            },
            onSuccess = {
                router.replaceAll(SwapRoute.Success)
            },
        )
    }

    private fun selectWalletInSelector(
        fromSwapCurrencyStatus: SwapCurrencyStatus?,
        toSwapCurrencyStatus: SwapCurrencyStatus?,
    ) {
        if (fromSwapCurrencyStatus != null && toSwapCurrencyStatus == null) {
            chooseFromTokenBridge.selectWalletTab(fromSwapCurrencyStatus.userWalletId)
            chooseToTokenBridge.selectWalletTab(fromSwapCurrencyStatus.userWalletId)
        } else if (fromSwapCurrencyStatus == null && toSwapCurrencyStatus != null) {
            chooseFromTokenBridge.selectWalletTab(toSwapCurrencyStatus.userWalletId)
            chooseToTokenBridge.selectWalletTab(toSwapCurrencyStatus.userWalletId)
        } else if (fromSwapCurrencyStatus != null && toSwapCurrencyStatus != null) {
            chooseFromTokenBridge.selectWalletTab(fromSwapCurrencyStatus.userWalletId)
            chooseToTokenBridge.selectWalletTab(toSwapCurrencyStatus.userWalletId)
        }
    }

    private fun filterTokensFromSelector() {
        val tokenFilter = { accountStatus: AccountStatus, currencyStatus: CryptoCurrencyStatus ->
            if (currencyStatus.currency.isCustom) {
                false
            } else {
                val fromSwapCurrencyStatus = dataState.fromSwapCurrencyStatus
                val toSwapCurrencyStatus = dataState.toSwapCurrencyStatus

                (fromSwapCurrencyStatus?.account?.accountId != accountStatus.accountId ||
                    fromSwapCurrencyStatus.currency.id != currencyStatus.currency.id) &&
                    (toSwapCurrencyStatus?.account?.accountId != accountStatus.accountId ||
                        toSwapCurrencyStatus.currency.id != currencyStatus.currency.id)
            }
        }

        chooseFromTokenBridge.tokenFilter.value = tokenFilter
        chooseToTokenBridge.tokenFilter.value = tokenFilter
    }

    private fun sendSuccessSwapEvent(fromToken: CryptoCurrency, feeType: FeeType) {
        val event = AnalyticsParam.TxSentFrom.Swap(
            blockchain = fromToken.network.name,
            token = fromToken.symbol,
            feeType = AnalyticsParam.FeeType.fromString(feeType.getNameForAnalytics()),
            feeToken = getFeeToken().symbol,
        )
        analyticsEventHandler.send(
            Basic.TransactionSent(
                sentFrom = event,
                memoType = Basic.TransactionSent.MemoType.Null,
            ),
        )
    }

    private fun getFeeToken(): CryptoCurrency {
        val fromToken = requireNotNull(dataState.fromSwapCurrencyStatus) {
            "fromCryptoCurrency should not be null"
        }
        return when (val fee = getSelectedFee()) {
            is TxFee.FeeComponent -> fee.selectedToken?.currency ?: fromToken.currency
            is TxFee.Legacy,
            null,
            -> fromToken.currency
        }
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
        return state.minByOrNull { entry ->
            val toTokenInfo = entry.value.toTokenInfo
            val fromAmountFiat = entry.value.fromTokenInfo.amountFiat
            val toAmountFiat = toTokenInfo.amountFiat
            if (!fromAmountFiat.isNullOrZero() && !toAmountFiat.isNullOrZero()) {
                fromAmountFiat.divide(
                    toAmountFiat,
                    toTokenInfo.swapCurrencyStatus.currency.decimals,
                    RoundingMode.HALF_UP,
                )
            } else {
                BigDecimal.ZERO
            }
        }?.key
    }

    private fun getPricesLowerBest(selectedProviderId: String, state: SuccessLoadedSwapData): Map<String, Float> {
        val selectedProviderEntry = state
            .filter { entry -> entry.key.providerId == selectedProviderId }
            .entries
            .firstOrNull() ?: return emptyMap()
        val selectedProviderRate = selectedProviderEntry.value.toTokenInfo.tokenAmount.value
        val hundredPercent = BigDecimal("100")
        return state.entries.mapNotNull { entry ->
            if (entry.key != selectedProviderEntry.key) {
                val amount = entry.value.toTokenInfo.tokenAmount.value
                val percentDiff = BigDecimal.ONE.minus(
                    selectedProviderRate.divide(amount, RoundingMode.HALF_UP),
                ).multiply(hundredPercent)
                entry.key.providerId to percentDiff.setScale(2, RoundingMode.HALF_UP).toFloat()
            } else {
                null
            }
        }.toMap()
    }

    private fun createSelectedAppCurrencyFlow(): StateFlow<AppCurrency> {
        return getSelectedAppCurrencyUseCase().map { maybeAppCurrency ->
            maybeAppCurrency.getOrElse { AppCurrency.Default }
        }.stateIn(
            scope = modelScope,
            started = SharingStarted.Eagerly,
            initialValue = AppCurrency.Default,
        )
    }

    private fun handleSwapNotSupported(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
    ) {
        val fromCurrency = fromSwapCurrencyStatus.currency
        val toCurrency = toSwapCurrencyStatus.currency
        analyticsEventHandler.send(
            SwapEvents.NoticeUnavailableToSwapPair(
                sendToken = fromCurrency.symbol,
                receiveToken = toCurrency.symbol,
                sendBlockchain = fromCurrency.network.name,
                receiveBlockchain = toCurrency.network.name,
            ),
        )
        // Cancel periodic quote task if selected token is not supported
        singleTaskScheduler.cancelTask()

        lastReducedBalanceBy.value = BigDecimal.ZERO
        lastAmount.value = INITIAL_AMOUNT
        uiState = stateBuilder.createSwapNotSupportedState(
            uiStateHolder = uiState,
            fromSwapCurrencyStatus = fromSwapCurrencyStatus,
            toSwapCurrencyStatus = toSwapCurrencyStatus,
        )
    }

    private fun isTangemPayWithdrawal(): Boolean {
        return tangemPayInput?.isWithdrawal == true || dataState.fromSwapCurrencyStatus?.account is Account.Payment
    }

    private fun Map<SwapProvider, SwapState>.getLastLoadedSuccessStates(): SuccessLoadedSwapData {
        return this.filter { entry -> entry.value is SwapState.QuotesLoadedState }
            .mapValues { entry -> entry.value as SwapState.QuotesLoadedState }
    }

    private fun Map<SwapProvider, SwapState>.consideredProvidersStates(): Map<SwapProvider, SwapState> {
        return this.filter { entry ->
            entry.value is SwapState.QuotesLoadedState || isUserResolvableError(entry.value)
        }
    }

    private fun sendNoticePermissionNeededEvent() {
        val sendTokenSymbol = dataState.fromSwapCurrencyStatus?.currency?.symbol ?: return
        val receiveTokenSymbol = dataState.toSwapCurrencyStatus?.currency?.symbol ?: return
        val provider = dataState.selectedProvider ?: return
        analyticsEventHandler.send(
            SwapEvents.NoticePermissionNeeded(
                sendToken = sendTokenSymbol,
                receiveToken = receiveTokenSymbol,
                provider = provider,
            ),
        )
    }

    private fun sendGivePermissionClickedEvent() {
        val sendTokenSymbol = dataState.toSwapCurrencyStatus?.currency?.symbol ?: return
        val receiveTokenSymbol = dataState.toSwapCurrencyStatus?.currency?.symbol ?: return
        val provider = dataState.selectedProvider ?: return
        analyticsEventHandler.send(
            SwapEvents.ButtonGivePermissionClicked(
                sendToken = sendTokenSymbol,
                receiveToken = receiveTokenSymbol,
                provider = provider,
            ),
        )
    }

    private fun updateWalletBalance() {
        dataState.fromSwapCurrencyStatus?.let { fromSwapCurrencyStatus ->
            modelScope.launch {
                withContext(NonCancellable) {
                    updateForBalance(
                        fromSwapCurrencyStatus.userWalletId,
                        fromSwapCurrencyStatus.currency.network,
                    )
                }
            }
        }
    }

    private suspend fun updateForBalance(userWalletId: UserWalletId, network: Network) {
        updateDelayedCurrencyStatusUseCase(
            userWalletId = userWalletId,
            network = network,
            delayMillis = UPDATE_BALANCE_DELAY_MILLIS,
        )
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

    private fun onTangemPayWithdrawalError(txId: String?) {
        showTransactionErrorAlert(
            error = SwapTransactionState.Error.TangemPayWithdrawalError(txId.orEmpty()),
            onSupportClick = ::onTangemPaySupportClick,
        )
    }

    private fun onFailedTxEmailClick(errorMessage: String) {
        modelScope.launch {
            val transaction = dataState.swapDataModel?.transaction
            val fromSwapCurrencyStatus = dataState.fromSwapCurrencyStatus
            val fromCurrency = fromSwapCurrencyStatus?.currency ?: params.cryptoCurrency
            val fromWalletId = fromSwapCurrencyStatus?.userWalletId ?: params.userWalletId
            val network = fromCurrency?.network

            saveBlockchainErrorUseCase(
                error = BlockchainErrorInfo(
                    errorMessage = errorMessage,
                    networkId = network?.id,
                    destinationAddress = transaction?.txTo.orEmpty(),
                    tokenSymbol = fromCurrency?.symbol.orEmpty(),
                    amount = dataState.amount.orEmpty(),
                    fee = when (val fee = getSelectedFee()) {
                        is TxFee.FeeComponent -> fee.fee.amount.value?.toString()
                        is TxFee.Legacy -> fee.feeCryptoFormatted
                        null -> ""
                    },
                ),
            )

            val metaInfo = getWalletMetaInfoUseCase(fromWalletId)
                .getOrElse { error("CardInfo must be not null") }

            val email = FeedbackEmailType.SwapProblem(
                walletMetaInfo = metaInfo,
                providerName = dataState.selectedProvider?.name.orEmpty(),
                txId = transaction?.txId.orEmpty(),
            )

            analyticsEventHandler.send(Basic.ButtonSupport(source = ScreensSources.Swap))
            sendFeedbackEmailUseCase(email)
        }
    }

    private fun getSelectedFeeState(): TxFeeSealedState {
        val feeStateUM = feeSelectorRepository.state.value as? FeeSelectorUM.Content

        if (feeStateUM == null) {
            TangemLogger.e(
                messageString = "getSelectedFeeState: FeeSelectorUM is not Content: $feeStateUM, " +
                    "returning Legacy state",
                shouldSanitize = false,
            )
            return TxFeeSealedState.Legacy(
                txFeeState = TxFeeState.Empty,
                selectedFee = dataState.selectedFee?.feeType ?: FeeType.NORMAL,
            )
        }

        val transactionFeeExtended = feeStateUM.feeExtraInfo.transactionFeeExtended
        return TxFeeSealedState.Component(
            txFee = TxFee.FeeComponent(
                transactionFeeResult = transactionFeeExtended?.let { TransactionFeeResult.from(it) }
                    ?: TransactionFeeResult.from(feeStateUM.fees),
                fee = feeStateUM.selectedFeeItem.fee,
                selectedToken = feeStateUM.feeExtraInfo.feeCryptoCurrencyStatus,
            ),
        )
    }

    private fun getSelectedFee(): TxFee? {
        val feeStateUM = feeSelectorRepository.state.value as? FeeSelectorUM.Content

        if (feeStateUM == null) {
            TangemLogger.e(
                messageString = "getSelectedFee: FeeSelectorUM is not Content: $feeStateUM, returning null",
                shouldSanitize = false,
            )
            return null
        }

        val transactionFeeExtended = feeStateUM.feeExtraInfo.transactionFeeExtended

        return TxFee.FeeComponent(
            transactionFeeResult = transactionFeeExtended?.let { TransactionFeeResult.from(it) }
                ?: TransactionFeeResult.from(feeStateUM.fees),
            fee = feeStateUM.selectedFeeItem.fee,
            selectedToken = feeStateUM.feeExtraInfo.feeCryptoCurrencyStatus,
        )
    }

    @Suppress("UnsafeCallOnNullableType")
    inner class FeeSelectorRepository : SwapFeeSelectorBlockComponent.ModelRepositoryExtended {

        override val state = MutableStateFlow<FeeSelectorUM>(
            FeeSelectorUM.Error(GetFeeError.UnknownError, isHidden = true),
        )

        override val forceUpdateState = MutableSharedFlow<FeeSelectorUM>()

        override suspend fun loadFeeExtended(
            selectedToken: CryptoCurrencyStatus?,
        ): Either<GetFeeError, TransactionFeeExtended> {
            val fromSwapCurrencyStatus =
                dataState.fromSwapCurrencyStatus ?: return Either.Left(GetFeeError.UnknownError)
            val selectedProvider = dataStateStateFlow.first { it.selectedProvider != null }.selectedProvider!!

            if (selectedProvider.type != ExchangeProviderType.CEX) {
                return Either.Left(GetFeeError.GaslessError.NetworkIsNotSupported)
            }

            if (dataState.lastLoadedSwapStates[selectedProvider] !is SwapState.QuotesLoadedState) {
                return Either.Left(GetFeeError.UnknownError)
            }

            if (isPermissionNotificationShown()) {
                return Either.Left(GetFeeError.UnknownError)
            }

            return swapInteractor.loadFeeForSwapTransaction(
                fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                provider = selectedProvider,
                amount = lastAmount.value,
                reduceBalanceBy = lastReducedBalanceBy.value,
                selectedFeeToken = selectedToken,
            )
        }

        override fun onResult(newState: FeeSelectorUM) {
            state.value = newState

            if (newState is FeeSelectorUM.Error) {
                modelScope.launch {
                    TangemLogger.e("onResult: FeeSelectorUM is Error, isHidden = true")
                    forceUpdateState.emit(newState.copy(isHidden = true))
                }
                return
            }

            val fromSwapCurrencyStatus = dataState.fromSwapCurrencyStatus

            // If fee currency is same as from currency, we need to reload quotes to update fee info
            val isFeeCurrencySameAsFromCurrency = newState is FeeSelectorUM.Content &&
                fromSwapCurrencyStatus?.currency?.id == newState.feeExtraInfo.feeCryptoCurrencyStatus.currency.id

            // If fee currency is coin, we need to reload quotes to update fee related warnings (e.g. insufficient funds)
            val isCoinFeeSelected = newState is FeeSelectorUM.Content &&
                newState.feeExtraInfo.feeCryptoCurrencyStatus.currency is CryptoCurrency.Coin

            if (isFeeCurrencySameAsFromCurrency || isCoinFeeSelected) {
                TangemLogger.e("onResult: Fee currency is same as from currency or coin fee selected, reloading quotes")

                // block swap button until fee is loaded
                uiState = uiState.copy(
                    swapButton = uiState.swapButton.copy(
                        isEnabled = false,
                        isInProgress = false,
                    ),
                )
                modelScope.launch {
                    startLoadingQuotesFromLastState(
                        isSilent = true,
                        updateFeeBlock = false,
                    )
                }
            }
        }

        private fun isPermissionNotificationShown(): Boolean {
            val permissionState = dataState.getCurrentLoadedSwapState()?.permissionState
            return permissionState != null && permissionState !is PermissionDataState.Empty
        }

        override suspend fun loadFee(): Either<GetFeeError, TransactionFee> {
            TangemLogger.e("loadFee: Start loading fee")

            val fromSwapCurrencyStatus =
                dataState.fromSwapCurrencyStatus ?: return Either.Left(GetFeeError.UnknownError)
            val toSwapCurrencyStatus =
                dataState.toSwapCurrencyStatus ?: return Either.Left(GetFeeError.UnknownError)
            val selectedProvider = dataStateStateFlow.first { it.selectedProvider != null }.selectedProvider!!

            if (dataState.lastLoadedSwapStates[selectedProvider] !is SwapState.QuotesLoadedState) {
                TangemLogger.e(
                    messageString = "loadFee: Quotes not loaded ${dataState.lastLoadedSwapStates[selectedProvider]}",
                    shouldSanitize = false,
                )
                return Either.Left(GetFeeError.UnknownError)
            }

            if (isPermissionNotificationShown()) {
                TangemLogger.e("loadFee: Permission notification is shown, cannot load fee")
                return Either.Left(GetFeeError.UnknownError)
            }

            return swapInteractor.loadFeeForSwapTransaction(
                fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                toSwapCurrencyStatus = toSwapCurrencyStatus,
                provider = selectedProvider,
                amount = lastAmount.value,
                reduceBalanceBy = lastReducedBalanceBy.value,
            ).onLeft {
                TangemLogger.e("loadFee: Failed to load fee with error $it")
            }.onRight {
                TangemLogger.e("loadFee: Fee loaded successfully")
            }
        }

        override fun choosingInProgress(updatedState: Boolean) {
            // We shouldn't load quotes while user is choosing fee
            if (updatedState) {
                singleTaskScheduler.cancelTask()
            } else {
                startLoadingQuotesFromLastState(
                    isSilent = true,
                    updateFeeBlock = false,
                )
            }
        }
    }

    private companion object {
        const val INITIAL_AMOUNT = ""
        const val UPDATE_DELAY = 10000L
        const val DEBOUNCE_AMOUNT_DELAY = 1000L
        const val UPDATE_BALANCE_DELAY_MILLIS = 11000L
        const val SWAP_IN_PROGRESS_DELAY = 200L
        const val CHANGELLY_PROVIDER_ID = "changelly"
    }
}