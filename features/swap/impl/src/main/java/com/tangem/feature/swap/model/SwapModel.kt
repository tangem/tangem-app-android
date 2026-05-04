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
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.ui.bottomsheet.permission.state.ApproveType
import com.tangem.common.ui.bottomsheet.permission.state.GiveTxPermissionState.InProgress.getApproveTypeOrNull
import com.tangem.core.analytics.api.AnalyticsErrorHandler
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.AnalyticsParam.ScreensSources
import com.tangem.core.analytics.models.Basic
import com.tangem.core.analytics.models.event.SwapAnalyticsEvent
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.HoldToConfirmButtonFeatureToggles
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.core.ui.utils.InputNumberFormatter
import com.tangem.core.ui.utils.parseBigDecimal
import com.tangem.datasource.local.appsflyer.AppsFlyerStore
import com.tangem.domain.account.status.model.AccountCryptoCurrencyStatus
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.status.usecase.GetAccountCurrencyStatusUseCase
import com.tangem.domain.account.status.usecase.GetFeePaidCryptoCurrencyStatusSyncUseCase
import com.tangem.domain.account.status.utils.CryptoCurrencyStatusOperations.getCryptoCurrencyStatus
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
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isHotWallet
import com.tangem.domain.pay.WithdrawalResult
import com.tangem.domain.promo.ShouldShowStoriesUseCase
import com.tangem.domain.promo.models.StoryContentIds
import com.tangem.domain.settings.usercountry.GetUserCountryUseCase
import com.tangem.domain.settings.usercountry.models.UserCountry
import com.tangem.domain.settings.usercountry.models.needApplyFCARestrictions
import com.tangem.domain.tangempay.GetTangemPayCurrencyStatusUseCase
import com.tangem.domain.tangempay.GetTangemPayCustomerIdUseCase
import com.tangem.domain.tangempay.TangemPayWithdrawUseCase
import com.tangem.domain.tokens.GetMinimumTransactionAmountSyncUseCase
import com.tangem.domain.tokens.UpdateDelayedNetworkStatusUseCase
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.transaction.usecase.gasless.IsGaslessFeeSupportedForNetwork
import com.tangem.domain.txhistory.usecase.GetExplorerTransactionUrlUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.feature.swap.analytics.SwapEvents
import com.tangem.feature.swap.choosetoken.api.ChooseTokenBridge
import com.tangem.feature.swap.component.SwapFeeSelectorBlockComponent
import com.tangem.feature.swap.converters.SwapTransactionErrorStateConverter
import com.tangem.feature.swap.domain.AllowPermissionsHandler
import com.tangem.feature.swap.domain.SwapInteractor
import com.tangem.feature.swap.domain.TransactionFeeResult
import com.tangem.feature.swap.domain.TxFeeSealedState
import com.tangem.feature.swap.domain.models.ExpressDataError
import com.tangem.feature.swap.domain.models.ExpressException
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.domain.*
import com.tangem.feature.swap.domain.models.ui.*
import com.tangem.feature.swap.models.*
import com.tangem.feature.swap.models.states.SwapNotificationUM
import com.tangem.feature.swap.router.SwapNavScreen
import com.tangem.feature.swap.router.SwapRouter
import com.tangem.feature.swap.ui.StateBuilder
import com.tangem.feature.swap.utils.formatToUIRepresentation
import com.tangem.feature.swap.utils.getContractAddress
import com.tangem.features.approval.api.GiveApprovalComponent
import com.tangem.features.approval.api.GiveApprovalFeatureToggles
import com.tangem.features.send.v2.api.entity.FeeSelectorUM
import com.tangem.features.send.v2.api.subcomponents.feeSelector.FeeSelectorReloadTrigger
import com.tangem.features.swap.SwapComponent
import com.tangem.utils.Provider
import com.tangem.utils.TangemBlogUrlBuilder.RESOURCE_TO_LEARN_ABOUT_APPROVING_IN_SWAP
import com.tangem.utils.coroutines.*
import com.tangem.utils.isNullOrZero
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject

typealias SuccessLoadedSwapData = Map<SwapProvider, SwapState.QuotesLoadedState>

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("LongParameterList", "LargeClass")
@Stable
@ModelScoped
internal class SwapModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val analyticsErrorEventHandler: AnalyticsErrorHandler,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val updateDelayedCurrencyStatusUseCase: UpdateDelayedNetworkStatusUseCase,
    private val getFeePaidCryptoCurrencyStatusSyncUseCase: GetFeePaidCryptoCurrencyStatusSyncUseCase,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val getWalletMetaInfoUseCase: GetWalletMetaInfoUseCase,
    private val saveBlockchainErrorUseCase: SaveBlockchainErrorUseCase,
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase,
    private val getMinimumTransactionAmountSyncUseCase: GetMinimumTransactionAmountSyncUseCase,
    private val getExplorerTransactionUrlUseCase: GetExplorerTransactionUrlUseCase,
    private val shouldShowStoriesUseCase: ShouldShowStoriesUseCase,
    getUserCountryUseCase: GetUserCountryUseCase,
    getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    swapInteractorFactory: SwapInteractor.Factory,
    private val urlOpener: UrlOpener,
    router: AppRouter,
    private val isAccountsModeEnabledUseCase: IsAccountsModeEnabledUseCase,
    private val getAccountCurrencyStatusUseCase: GetAccountCurrencyStatusUseCase,
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
    private val getTangemPayCurrencyStatusUseCase: GetTangemPayCurrencyStatusUseCase,
    private val tangemPayWithdrawUseCase: TangemPayWithdrawUseCase,
    private val iGaslessFeeSupportedForNetwork: IsGaslessFeeSupportedForNetwork,
    private val feeSelectorReloadTrigger: FeeSelectorReloadTrigger,
    private val getTangemPayCustomerIdUseCase: GetTangemPayCustomerIdUseCase,
    private val appsFlyerStore: AppsFlyerStore,
    private val holdToConfirmButtonFeatureToggles: HoldToConfirmButtonFeatureToggles,
    private val messageSender: UiMessageSender,
    private val allowPermissionsHandler: AllowPermissionsHandler,
    chooseTokenBridgeFactory: ChooseTokenBridge.Factory,
    giveApprovalFeatureToggles: GiveApprovalFeatureToggles,
) : Model() {

    private val params = paramsContainer.require<SwapComponent.Params>()

    private val initialCurrencyFrom = params.currencyFrom
    private val initialCurrencyTo = params.currencyTo
    private val userWalletId = params.userWalletId
    private val isInitiallyReversed = params.isInitialReverseOrder
    private val tangemPayInput = params.tangemPayInput

    private val userWallet by lazy {
        requireNotNull(
            getUserWalletUseCase(userWalletId).getOrNull(),
        ) { "No wallet found for id: $userWalletId" }
    }
    private val swapInteractor = swapInteractorFactory.create(userWalletId)

    val isHoldToConfirmEnabled: Boolean =
        holdToConfirmButtonFeatureToggles.isHoldToConfirmEnabled && userWallet.isHotWallet

    private lateinit var initialFromStatus: CryptoCurrencyStatus
    private var initialToStatus: CryptoCurrencyStatus? = null

    private var isBalanceHidden = true
    private var isAccountsMode: Boolean = false

    private val selectedAppCurrencyFlow: StateFlow<AppCurrency> = createSelectedAppCurrencyFlow()

    val chooseTokenBridge: ChooseTokenBridge = chooseTokenBridgeFactory.create(modelScope)

    private val stateBuilder = StateBuilder(
        userWalletProvider = Provider { userWallet },
        actions = createUiActions(),
        isBalanceHiddenProvider = Provider { isBalanceHidden },
        appCurrencyProvider = Provider(selectedAppCurrencyFlow::value),
        isAccountsModeProvider = Provider { isAccountsMode },
        iGaslessFeeSupportedForNetwork = iGaslessFeeSupportedForNetwork,
        holdToConfirmButtonFeatureToggles = holdToConfirmButtonFeatureToggles,
    )

    private val inputNumberFormatter =
        InputNumberFormatter(
            NumberFormat.getInstance(Locale.getDefault()) as? DecimalFormat
                ?: error("NumberFormat is not DecimalFormat"),
        )
    private val amountDebouncer = Debouncer()
    private val singleTaskScheduler = SingleTaskScheduler<Map<SwapProvider, SwapState>>()

    val dataStateStateFlow = MutableStateFlow(SwapProcessDataState())
    var dataState
        get() = dataStateStateFlow.value
        set(value) {
            dataStateStateFlow.value = value
        }

    var uiState: SwapStateHolder by mutableStateOf(
        stateBuilder.createInitialLoadingState(
            initialCurrencyFrom = initialCurrencyFrom,
            initialCurrencyTo = initialCurrencyTo,
            fromNetworkInfo = initialCurrencyFrom.getNetworkInfo(),
        ),
    )
        private set

    val feeSelectorRepository = FeeSelectorRepository()

    // shows currency order (direct - swap initial to selected, reversed = selected to initial)
    private val isOrderReversed = MutableStateFlow(value = params.isInitialReverseOrder)
    private val lastAmount = mutableStateOf(INITIAL_AMOUNT)
    private val lastReducedBalanceBy = mutableStateOf(BigDecimal.ZERO)
    private val swapRouter: SwapRouter = SwapRouter(router = router)
    private var userCountry: UserCountry? = null

    private var fromAccountCurrencyStatus: AccountCryptoCurrencyStatus? = null
    private var toAccountCurrencyStatus: AccountCryptoCurrencyStatus? = null

    /**
     * If user came from Tangem Pay -> fromAccountCurrencyStatus == null
     * If user didn't come from Tangem Pay -> fromAccountCurrencyStatus != null
     *
     * Remove when accounts are integrated into Tangem Pay
     */
    private val canUseFromAccountCurrencyStatus = tangemPayInput == null

    private val isUserResolvableError: (SwapState) -> Boolean = { swapState ->
        swapState is SwapState.SwapError &&
            (
                swapState.error is ExpressDataError.ExchangeTooSmallAmountError ||
                    swapState.error is ExpressDataError.ExchangeTooBigAmountError
                )
    }

    private val fromTokenBalanceJobHolder = JobHolder()
    private val toTokenBalanceJobHolder = JobHolder()

    private var isAmountChangedByUser: Boolean = false
    private var lastPermissionNotificationTokens: Pair<String, String>? = null

    val currentScreen: SwapNavScreen
        get() = swapRouter.currentScreen

    val approvalSlotNavigation = SlotNavigation<Unit>()
    private val shouldUseGaslessApproval: Boolean = giveApprovalFeatureToggles.isGaslessApprovalEnabled

    val approvalCallback = object : GiveApprovalComponent.Callback {
        override fun onApproveClick() {
            sendPermissionApproveClickedEvent()
        }

        override fun onApproveDone() {
            val fromContractAddress = dataState.fromCryptoCurrency?.currency?.getContractAddress()
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
            analyticsEventHandler.send(SwapEvents.ButtonPermissionCancelClicked())
        }
    }

    init {
        chooseTokenBridge.searchQueryState
            .onEach { query -> onSearchEntered(query) }
            .launchIn(modelScope)

        chooseTokenBridge.onNewTokenAdded.receiveAsFlow()
            .onEach { (addedToken, isSearched) ->
                applyAddedToken(addedToken, isSearched.value)
            }
            .launchIn(modelScope)

        chooseTokenBridge.onTokenSelected.receiveAsFlow()
            .onEach { (addedToken, isSearched) ->
                onTokenSelect(addedToken, isSearched.value)
            }
            .launchIn(modelScope)

        chooseTokenBridge.onClose.receiveAsFlow()
            .onEach {
                analyticsEventHandler.send(SwapEvents.ChooseTokenScreenResult(isTokenChosen = false))
                swapRouter.back()
            }
            .launchIn(modelScope)

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

        modelScope.launch(dispatchers.io) {
            if (canUseFromAccountCurrencyStatus) {
                isAccountsMode = isAccountsModeEnabledUseCase.invokeSync()

                val fromAccountStatus = getAccountCurrencyStatusUseCase.invokeSync(
                    userWalletId = userWalletId,
                    currency = initialCurrencyFrom,
                ).getOrNull()
                val toAccountStatus = initialCurrencyTo?.let { currencyTo ->
                    getAccountCurrencyStatusUseCase.invokeSync(
                        userWalletId = userWalletId,
                        currency = currencyTo,
                    ).getOrNull()
                }

                if (fromAccountStatus == null) {
                    showAlert()
                    swapRouter.back()
                } else {
                    fromAccountCurrencyStatus = fromAccountStatus
                    toAccountCurrencyStatus = toAccountStatus
                    initialFromStatus = fromAccountStatus.status
                    initialToStatus = toAccountStatus?.status
                    initTokens(isInitiallyReversed)
                }
            } else {
                val fromStatus = getFromStatus()
                val toStatus = initialCurrencyTo?.let { currencyTo ->
                    singleAccountStatusListSupplier.getSyncOrNull(params.userWalletId)
                        .getCryptoCurrencyStatus(currencyTo)
                        .getOrNull()
                }

                if (fromStatus == null) {
                    showAlert()
                    swapRouter.back()
                } else {
                    initialFromStatus = fromStatus
                    initialToStatus = toStatus
                    initTokens(isInitiallyReversed)
                }
            }
        }

        analyticsEventHandler.send(
            SwapEvents.SwapScreenOpened(
                token = initialCurrencyFrom.symbol,
                blockchain = initialCurrencyFrom.network.name,
            ),
        )

        getBalanceHidingSettingsUseCase()
            .onEach { settings ->
                isBalanceHidden = settings.isBalanceHidden
                uiState = stateBuilder.updateBalanceHiddenState(uiState, isBalanceHidden)
            }
            .launchIn(modelScope)
    }

    fun onStart() {
        startLoadingQuotesFromLastState(true)
    }

    fun onStop() {
        singleTaskScheduler.cancelTask()
    }

    override fun onDestroy() {
        singleTaskScheduler.cancelTask()
        super.onDestroy()
    }

    private fun sendSelectTokenScreenOpenedEvent() {
        val isAnyAvailableTokensTo = dataState.tokensDataState?.toGroup?.available?.isNotEmpty() == true
        val isAnyAvailableTokensFrom = dataState.tokensDataState?.fromGroup?.available?.isNotEmpty() == true
        val isAnyAvailableAccountTokensTo = !dataState.tokensDataState?.toGroup?.accountCurrencyList.isNullOrEmpty()
        val isAnyAvailableAccountTokensFrom = !dataState.tokensDataState?.fromGroup?.accountCurrencyList.isNullOrEmpty()
        val isAnyAvailableTokens = isAnyAvailableTokensTo || isAnyAvailableTokensFrom ||
            isAnyAvailableAccountTokensTo || isAnyAvailableAccountTokensFrom
        analyticsEventHandler.send(SwapEvents.ChooseTokenScreenOpened(hasAvailableTokens = isAnyAvailableTokens))
    }

    @Suppress("LongMethod")
    private fun initTokens(isReverseFromTo: Boolean) {
        modelScope.launch(dispatchers.main) {
            runCatching(dispatchers.io) {
                swapInteractor.getTokensDataState(initialCurrencyFrom)
            }.onSuccess { state ->
                updateTokensState(state)

                val (selectedCurrency, selectedAccount) = run {
                    var selectedAccountCurrency = toAccountCurrencyStatus

                    if (selectedAccountCurrency == null) {
                        val amountSwapCurrency = swapInteractor.getInitialCurrencyToSwap(
                            initialCryptoCurrency = initialCurrencyFrom,
                            state = state,
                            isReverseFromTo = isReverseFromTo,
                        )

                        if (amountSwapCurrency != null) {
                            selectedAccountCurrency = AccountCryptoCurrencyStatus(
                                account = amountSwapCurrency.account,
                                status = amountSwapCurrency.cryptoCurrencyStatus,
                            )
                        }
                    }

                    selectedAccountCurrency?.status to selectedAccountCurrency?.account
                }

                val isApplied = applyInitialTokenChoice(
                    state = state,
                    selectedCurrency = selectedCurrency,
                    selectedAccount = selectedAccount,
                    isReverseFromTo = isReverseFromTo,
                )

                // assume that fromCryptoCurrency selected according reverse flag,
                // so update fee paid currency according to it
                val fromCryptoCurrency = dataState.fromCryptoCurrency

                if (isApplied && fromCryptoCurrency != null) {
                    TangemLogger.i(
                        "updateFeePaidCryptoCurrencyFor: id = ${fromCryptoCurrency.currency.id}, " +
                            "isReverseFromTo: $isReverseFromTo",
                    )
                    updateFeePaidCryptoCurrencyFor(fromCryptoCurrency)
                } else {
                    TangemLogger.e("updateFeePaidCryptoCurrencyFor failed: fromCryptoCurrency is null")
                }

                subscribeToCoinBalanceUpdatesIfNeeded()
            }.onFailure { error ->
                TangemLogger.e("Error", error)

                applyInitialTokenChoice(
                    state = TokensDataStateExpress.EMPTY,
                    selectedCurrency = null,
                    selectedAccount = null,
                    isReverseFromTo = isReverseFromTo,
                )

                uiState = stateBuilder.createInitialErrorState(
                    uiState,
                    (error as? ExpressException)?.expressDataError?.code ?: ExpressDataError.UnknownError.code,
                ) {
                    uiState = stateBuilder.createInitialLoadingState(
                        initialCurrencyFrom = initialCurrencyFrom,
                        initialCurrencyTo = initialCurrencyTo,
                        fromNetworkInfo = initialCurrencyFrom.getNetworkInfo(),
                    )
                    initTokens(isReverseFromTo)
                }
            }
        }
    }

    private suspend fun applyAddedToken(addedToken: CryptoCurrency, isSearched: Boolean) {
        analyticsEventHandler.send(
            SwapEvents.ChooseTokenScreenResult(isTokenChosen = true, token = addedToken.symbol),
        )
        analyticsEventHandler.send(
            SwapAnalyticsEvent.TokenSelected(
                token = addedToken.symbol,
                source = ScreensSources.Markets,
                isSearched = isSearched,
            ),
        )
        val status = getAccountCurrencyStatusUseCase.invoke(userWalletId, addedToken)
            // todo swap are sure?? about status.value is CryptoCurrencyStatus.Loaded
            .firstOrNull { it.status.value is CryptoCurrencyStatus.Loaded }
            ?: return
        val (selectedAccount, selectedCurrency) = status

        runCatching(dispatchers.io) {
            swapInteractor.getTokensDataState(initialCurrencyFrom)
        }.onSuccess { state ->
            updateTokensState(state)

            applyInitialTokenChoice(
                state = state,
                selectedCurrency = selectedCurrency,
                selectedAccount = selectedAccount,
                isReverseFromTo = isOrderReversed.value,
            )

            subscribeToCoinBalanceUpdatesIfNeeded()

            swapRouter.back()
        }.onFailure { error ->
            TangemLogger.e("Error", error)
        }
    }

    private fun subscribeToCoinBalanceUpdatesIfNeeded() {
        (dataState.fromCryptoCurrency?.currency as? CryptoCurrency.Coin)?.let { coin ->
            subscribeToCoinBalanceUpdates(
                userWalletId = userWalletId,
                coin = coin,
                isFromCurrency = true,
            )
        }

        (dataState.toCryptoCurrency?.currency as? CryptoCurrency.Coin)?.let { coin ->
            subscribeToCoinBalanceUpdates(
                userWalletId = userWalletId,
                coin = coin,
                isFromCurrency = false,
            )
        }
    }

    /**
     * returns true if tokens are selected and dataState is updated,
     * false if selected token is null and alert is shown with error message
     */
    private fun applyInitialTokenChoice(
        state: TokensDataStateExpress,
        selectedCurrency: CryptoCurrencyStatus?,
        selectedAccount: Account.CryptoPortfolio?,
        isReverseFromTo: Boolean,
    ): Boolean {
        // exceptional case
        if (selectedCurrency == null) {
            TangemLogger.e("No available tokens to swap for ${initialCurrencyFrom.symbol}")
            analyticsEventHandler.send(SwapEvents.NoticeNoAvailableTokensToSwap())
            uiState = stateBuilder.createNoAvailableTokensToSwapState(
                uiStateHolder = uiState,
                fromToken = initialFromStatus,
            )
            return false
        }
        isOrderReversed.value = isReverseFromTo
        val (fromCurrencyStatus, toCurrencyStatus) = if (isOrderReversed.value) {
            selectedCurrency to initialFromStatus
        } else {
            initialFromStatus to selectedCurrency
        }
        val (fromAccount, toAccount) = if (canUseFromAccountCurrencyStatus) {
            if (isOrderReversed.value) {
                selectedAccount to requireNotNull(fromAccountCurrencyStatus).account
            } else {
                requireNotNull(fromAccountCurrencyStatus).account to selectedAccount
            }
        } else {
            null to null
        }
        dataState = dataState.copy(
            fromCryptoCurrency = fromCurrencyStatus,
            fromAccount = fromAccount,
            toCryptoCurrency = toCurrencyStatus,
            toAccount = toAccount,
            tokensDataState = state,
        )

        if (handleSwapNotSupported(
                state = state,
                fromToken = fromCurrencyStatus,
                toToken = toCurrencyStatus,
                fromAccount = fromAccount,
                toAccount = toAccount,
            )
        ) {
            return true
        }

        startLoadingQuotes(
            fromToken = fromCurrencyStatus,
            fromAccount = fromAccount,
            toToken = toCurrencyStatus,
            toAccount = toAccount,
            amount = lastAmount.value,
            reduceBalanceBy = lastReducedBalanceBy.value,
            toProvidersList = findSwapProviders(fromCurrencyStatus, toCurrencyStatus),
        )
        return true
    }

    private fun updateTokensState(tokenDataState: TokensDataStateExpress) {
        val tokensDataState = if (isOrderReversed.value) tokenDataState.fromGroup else tokenDataState.toGroup
        chooseTokenBridge.updateCurrenciesGroup(tokensDataState)
    }

    private fun startLoadingQuotes(
        fromToken: CryptoCurrencyStatus,
        fromAccount: Account.CryptoPortfolio?,
        toToken: CryptoCurrencyStatus,
        toAccount: Account.CryptoPortfolio?,
        amount: String,
        reduceBalanceBy: BigDecimal,
        toProvidersList: List<SwapProvider>,
        isSilent: Boolean = false,
        updateFeeBlock: Boolean = true,
    ) {
        singleTaskScheduler.cancelTask()
        if (!isSilent) {
            uiState = stateBuilder.createQuotesLoadingState(
                uiStateHolder = uiState,
                fromToken = fromToken.currency,
                toToken = toToken.currency,
                fromAccount = fromAccount,
                toAccount = toAccount,
                mainTokenId = initialCurrencyFrom.id.value,
            )
            feeSelectorRepository.state.value = FeeSelectorUM.Loading
        }
        singleTaskScheduler.scheduleTask(
            modelScope,
            loadQuotesTask(
                fromToken = fromToken,
                fromAccount = fromAccount,
                toToken = toToken,
                toAccount = toAccount,
                amount = amount,
                reduceBalanceBy = reduceBalanceBy,
                toProvidersList = toProvidersList,
                updateFeeBlock = updateFeeBlock,
            ),
        )
    }

    private fun startLoadingQuotesFromLastState(isSilent: Boolean = false, updateFeeBlock: Boolean = true) {
        val fromCurrency = dataState.fromCryptoCurrency
        val toCurrency = dataState.toCryptoCurrency
        val amount = dataState.amount
        if (fromCurrency != null && toCurrency != null && amount != null) {
            startLoadingQuotes(
                fromToken = fromCurrency,
                fromAccount = dataState.fromAccount,
                toToken = toCurrency,
                toAccount = dataState.toAccount,
                amount = amount,
                isSilent = isSilent,
                reduceBalanceBy = dataState.reduceBalanceBy,
                toProvidersList = findSwapProviders(fromCurrency, toCurrency),
                updateFeeBlock = updateFeeBlock,
            )
        }
    }

    private suspend fun updateFeePaidCryptoCurrencyFor(fromToken: CryptoCurrencyStatus) {
        dataState = dataState.copy(
            feePaidCryptoCurrency = getFeePaidCryptoCurrencyStatusSyncUseCase(
                userWalletId = userWalletId,
                cryptoCurrencyStatus = fromToken,
            )
                .onLeft { TangemLogger.e("Unable to get fee paid crypto currency status for ${fromToken.currency.id}") }
                .onRight { currencyStatus ->
                    if (currencyStatus == null) {
                        TangemLogger.e("Fee paid crypto currency status is null for ${fromToken.currency.id}")
                    }
                }
                .getOrNull(),
        )
    }

    private fun loadQuotesTask(
        fromToken: CryptoCurrencyStatus,
        fromAccount: Account.CryptoPortfolio?,
        toToken: CryptoCurrencyStatus,
        toAccount: Account.CryptoPortfolio?,
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
                        approveDataModel = null,
                    )
                    swapInteractor.findBestQuote(
                        fromToken = fromToken,
                        fromAccount = fromAccount,
                        toToken = toToken,
                        toAccount = toAccount,
                        providers = toProvidersList,
                        amountToSwap = amount,
                        reduceBalanceBy = reduceBalanceBy,
                        txFeeSealedState = getSelectedFeeState(),
                    )
                }
            },
            onSuccess = { providersState ->
                if (providersState.isNotEmpty()) {
                    val (provider, state) = updateLoadedQuotes(providersState)
                    setupLoadedState(provider = provider, state = state, fromToken = fromToken, toToken = toToken)
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
                    feeSelectorRepository.state.value = FeeSelectorUM.Error(GetFeeError.UnknownError, isHidden = true)
                    TangemLogger.e("Accidentally empty quotes list")
                }
            },
            onError = { error ->
                TangemLogger.e("Error when loading quotes: $error")
                feeSelectorRepository.state.value = FeeSelectorUM.Error(GetFeeError.UnknownError, isHidden = true)
                uiState = stateBuilder.addNotification(uiState, null) { startLoadingQuotesFromLastState() }
            },
        )
    }

    private fun setupLoadedState(
        provider: SwapProvider,
        state: SwapState,
        fromToken: CryptoCurrencyStatus,
        toToken: CryptoCurrencyStatus?,
    ) {
        when (state) {
            is SwapState.QuotesLoadedState -> {
                setupQuotesLoadedUiState(provider, state, fromToken)
                sendAnalyticsForNotifications(provider, fromToken, toToken)
                updatePermissionNotificationState(state)
            }
            is SwapState.EmptyAmountState -> {
                setupEmptyAmountUiState(state, fromToken)
                lastPermissionNotificationTokens = null
            }
            is SwapState.SwapError -> {
                setupErrorUiState(provider, state)
                lastPermissionNotificationTokens = null
            }
        }
    }

    private fun setupQuotesLoadedUiState(
        provider: SwapProvider,
        state: SwapState.QuotesLoadedState,
        fromToken: CryptoCurrencyStatus,
    ) {
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
            selectedFeeType = (getSelectedFee() as? TxFee.Legacy)?.feeType ?: FeeType.NORMAL,
            isReverseSwapPossible = isReverseSwapPossible(),
            needApplyFCARestrictions = userCountry.needApplyFCARestrictions(),
            hideFee = tangemPayInput?.isWithdrawal == true,
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
                    token = initialCurrencyFrom.symbol,
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
        val fromTokenId = state.fromTokenInfo.cryptoCurrencyStatus
            .currency.id.value
        val toTokenId = state.toTokenInfo.cryptoCurrencyStatus
            .currency.id.value
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

    private fun setupEmptyAmountUiState(state: SwapState.EmptyAmountState, fromToken: CryptoCurrencyStatus) {
        val toTokenStatus = dataState.toCryptoCurrency
        uiState = stateBuilder.createQuotesEmptyAmountState(
            uiStateHolder = uiState,
            emptyAmountState = state,
            fromTokenStatus = fromToken,
            toTokenStatus = toTokenStatus,
            isReverseSwapPossible = isReverseSwapPossible(),
            toAccount = dataState.toAccount,
        )
    }

    private fun setupErrorUiState(provider: SwapProvider, state: SwapState.SwapError) {
        singleTaskScheduler.cancelTask()
        uiState = stateBuilder.createQuotesErrorState(
            uiStateHolder = uiState,
            swapProvider = provider,
            fromToken = state.fromTokenInfo,
            toToken = dataState.toCryptoCurrency,
            expressDataError = state.error,
            includeFeeInAmount = state.includeFeeInAmount,
            isReverseSwapPossible = isReverseSwapPossible(),
            needApplyFCARestrictions = userCountry.needApplyFCARestrictions(),
            toAccount = dataState.toAccount,
        )
        sendErrorAnalyticsEvent(state.error, provider)
    }

    private fun sendErrorAnalyticsEvent(error: ExpressDataError, provider: SwapProvider) {
        val receiveToken = dataState.toCryptoCurrency?.currency?.let { currency ->
            "${currency.network.backendId}:${currency.symbol}"
        }
        analyticsErrorEventHandler.sendErrorEvent(
            SwapEvents.NoticeProviderError(
                sendToken = "${initialCurrencyFrom.network.backendId}:${initialCurrencyFrom.symbol}",
                receiveToken = receiveToken.orEmpty(),
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

    private fun updateOrSelectFee(state: SwapState.QuotesLoadedState): TxFee.Legacy? {
        val selectedFeeType = (getSelectedFee() as? TxFee.Legacy)?.feeType ?: FeeType.NORMAL
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
            TangemLogger.e("Last loaded quotes state is null")
            return
        }
        val fromCurrency = requireNotNull(dataState.fromCryptoCurrency)
        val fee = getSelectedFee()

        if (fee == null && tangemPayInput?.isWithdrawal != true) {
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
                    swapProvider = provider,
                    swapData = dataState.swapDataModel,
                    currencyToSend = fromCurrency,
                    currencyToGet = requireNotNull(dataState.toCryptoCurrency),
                    amountToSwap = requireNotNull(dataState.amount),
                    fromAccount = dataState.fromAccount,
                    toAccount = dataState.toAccount,
                    includeFeeInAmount = lastLoadedQuotesState.preparedSwapConfigState.includeFeeInAmount,
                    fee = fee,
                    expressOperationType = ExpressOperationType.SWAP,
                    isTangemPayWithdrawal = tangemPayInput?.isWithdrawal == true,
                )
            }.onSuccess { swapTransactionState ->
                when (swapTransactionState) {
                    is SwapTransactionState.TxSent -> {
                        if (fee == null) {
                            TangemLogger.e("onSwapClick: onSuccess: fee is null after swap")
                            showAlert(resourceReference(R.string.swapping_fee_estimation_error_text))
                            return@onSuccess
                        }
                        sendSuccessSwapEvent(
                            fromCurrency.currency,
                            (getSelectedFee() as? TxFee.Legacy)?.feeType ?: FeeType.NORMAL,
                        )
                        val url = getExplorerTransactionUrlUseCase(
                            txHash = swapTransactionState.txHash,
                            currency = fromCurrency.currency,
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
                                    event = SwapEvents.ButtonExplore(initialCurrencyFrom.symbol),
                                )
                            },
                            onStatusClick = {
                                val txExternalUrl = swapTransactionState.txExternalUrl
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
                        showDemoModeAlert()
                    }
                    is SwapTransactionState.Error -> {
                        startLoadingQuotesFromLastState()
                        showTransactionErrorAlert(swapTransactionState)
                    }
                    is SwapTransactionState.TangemPayWithdrawalData -> {
                        processTangemPayWithdrawal(swapTransactionState = swapTransactionState)
                    }
                }
            }.onFailure { error ->
                TangemLogger.e("Error", error)
                startLoadingQuotesFromLastState()
                showAlert()
            }
        }
    }

    private suspend fun processTangemPayWithdrawal(swapTransactionState: SwapTransactionState.TangemPayWithdrawalData) {
        tangemPayWithdrawUseCase(
            userWallet = userWallet,
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
                            currencyToSend = swapTransactionState.storeData.currencyToSend,
                            currencyToGet = swapTransactionState.storeData.currencyToGet,
                            fromAccount = swapTransactionState.storeData.fromAccount,
                            toAccount = swapTransactionState.storeData.toAccount,
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
                        swapRouter.openScreen(SwapNavScreen.Success)
                    }
                }
            }
    }

    private suspend fun sendSuccessEvent() {
        val provider = dataState.selectedProvider ?: return
        val fee = (getSelectedFee() as? TxFee.Legacy)?.feeType ?: FeeType.NORMAL
        val fromCurrency = dataState.fromCryptoCurrency?.currency ?: return
        val toCurrency = dataState.toCryptoCurrency?.currency ?: return
        val fromDerivationIndex = dataState.fromAccount?.derivationIndex?.value
        val toDerivationIndex = dataState.toAccount?.derivationIndex?.value

        analyticsEventHandler.send(
            SwapEvents.SwapInProgressScreen(
                provider = provider,
                commission = fee,
                sendBlockchain = fromCurrency.network.name,
                receiveBlockchain = toCurrency.network.name,
                sendToken = fromCurrency.symbol,
                receiveToken = toCurrency.symbol,
                feeToken = getFeeToken().symbol,
                fromDerivationIndex = fromDerivationIndex,
                toDerivationIndex = toDerivationIndex,
                referralId = appsFlyerStore.get()?.refcode,
            ),
        )
    }

    @Suppress("LongMethod")
    private fun givePermissionsToSwap() {
        modelScope.launch(dispatchers.main) {
            runSuspendCatching {
                val fromCryptoCurrency = requireNotNull(dataState.fromCryptoCurrency) {
                    "dataState.fromCryptoCurrency might not be null"
                }
                val fromToken = fromCryptoCurrency.currency

                val approveDataModel = requireNotNull(dataState.approveDataModel) {
                    "dataState.approveDataModel.spenderAddress shouldn't be null"
                }
                val approveType =
                    requireNotNull(uiState.permissionState.getApproveTypeOrNull()?.toDomainApproveType()) {
                        "uiState.permissionState should not be null"
                    }
                val feeForPermission = when (val fee = approveDataModel.fee) {
                    TxFeeState.Empty -> {
                        showAlert(resourceReference(R.string.swapping_fee_estimation_error_text))
                        TangemLogger.e("Fee should not be Empty")
                        return@launch
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
                            fromTokenStatus = fromCryptoCurrency,
                            approveType = approveType,
                            txFee = feeForPermission,
                            spenderAddress = requireNotNull(dataState.approveDataModel).spenderAddress,
                        ),
                    )
                }.onSuccess { swapTransactionState ->
                    when (swapTransactionState) {
                        is SwapTransactionState.TxSent -> {
                            // TODO [REDACTED_TASK_KEY] gasless analytics
                            sendApproveSuccessEvent(fromToken, feeForPermission.feeType, approveType)
                            updateWalletBalance()
                            uiState = stateBuilder.loadingPermissionState(uiState)
                            uiState = stateBuilder.dismissBottomSheet(uiState)
                            startLoadingQuotesFromLastState(isSilent = true)
                        }
                        is SwapTransactionState.Error -> {
                            showTransactionErrorAlert(swapTransactionState)
                        }
                        SwapTransactionState.DemoMode -> {
                            showDemoModeAlert()
                        }
                        is SwapTransactionState.TangemPayWithdrawalData -> {
                            processTangemPayWithdrawal(swapTransactionState = swapTransactionState)
                        }
                    }
                }.onFailure { showAlert() }
            }.onFailure { error ->
                TangemLogger.e(error.message.orEmpty())
                showAlert()
            }
        }
    }

    private fun onSearchEntered(searchQuery: String) {
        val tokenDataState = dataState.tokensDataState ?: return
        val group = if (isOrderReversed.value) {
            tokenDataState.fromGroup
        } else {
            tokenDataState.toGroup
        }

        val available = group.available.filter { swapAvailability ->
            swapAvailability.currencyStatus.currency.name.contains(searchQuery, ignoreCase = true) ||
                swapAvailability.currencyStatus.currency.symbol.contains(searchQuery, ignoreCase = true)
        }
        val unavailable = group.unavailable.filter { swapAvailability ->
            swapAvailability.currencyStatus.currency.name.contains(searchQuery, ignoreCase = true) ||
                swapAvailability.currencyStatus.currency.symbol.contains(searchQuery, ignoreCase = true)
        }
        val accountCurrencyList = group.accountCurrencyList.mapNotNull { accountSwapAvailability ->
            val filteredCurrencies = accountSwapAvailability.currencyList.filter { accountSwapCurrency ->
                val currency = accountSwapCurrency.cryptoCurrencyStatus.currency
                currency.name.contains(searchQuery, ignoreCase = true) ||
                    currency.symbol.contains(searchQuery, ignoreCase = true)
            }

            if (filteredCurrencies.isEmpty()) {
                return@mapNotNull null
            }

            accountSwapAvailability.copy(
                currencyList = filteredCurrencies,
            )
        }

        val filteredTokenDataState = if (isOrderReversed.value) {
            tokenDataState.copy(
                fromGroup = tokenDataState.fromGroup.copy(
                    available = available,
                    unavailable = unavailable,
                    accountCurrencyList = accountCurrencyList,
                    isAfterSearch = true,
                ),
            )
        } else {
            tokenDataState.copy(
                toGroup = tokenDataState.toGroup.copy(
                    available = available,
                    unavailable = unavailable,
                    accountCurrencyList = accountCurrencyList,
                    isAfterSearch = true,
                ),
            )
        }
        updateTokensState(filteredTokenDataState)
    }

    @Suppress("LongMethod")
    private fun onTokenSelect(id: String, isSearched: Boolean) {
        val tokens = dataState.tokensDataState ?: return
        val (foundToken, foundAccount) = getSelectedTokenAndAccount(tokens, id)

        foundToken?.currency?.symbol?.let { symbol ->
            analyticsEventHandler.send(
                SwapEvents.ChooseTokenScreenResult(isTokenChosen = true, token = symbol),
            )

            analyticsEventHandler.send(
                SwapAnalyticsEvent.TokenSelected(
                    token = symbol,
                    source = ScreensSources.Portfolio,
                    isSearched = isSearched,
                ),
            )
        }

        if (foundToken != null) {
            val fromToken: CryptoCurrencyStatus
            val fromAccount: Account.CryptoPortfolio?
            val toToken: CryptoCurrencyStatus
            val toAccount: Account.CryptoPortfolio?
            if (isOrderReversed.value) {
                fromToken = foundToken
                fromAccount = foundAccount
                toToken = initialFromStatus
                toAccount = fromAccountCurrencyStatus?.account

                val newToken = fromToken.currency as? CryptoCurrency.Coin
                if (newToken != null) {
                    subscribeToCoinBalanceUpdates(
                        userWalletId = userWalletId,
                        coin = newToken,
                        isFromCurrency = true,
                    )
                } else {
                    fromTokenBalanceJobHolder.cancel()
                }
            } else {
                fromToken = initialFromStatus
                fromAccount = fromAccountCurrencyStatus?.account
                toToken = foundToken
                toAccount = foundAccount

                val newToken = toToken.currency as? CryptoCurrency.Coin
                if (newToken != null) {
                    subscribeToCoinBalanceUpdates(
                        userWalletId = userWalletId,
                        coin = newToken,
                        isFromCurrency = false,
                    )
                } else {
                    toTokenBalanceJobHolder.cancel()
                }
            }

            if (dataState.fromCryptoCurrency != null && dataState.tokensDataState != null) {
                isAmountChangedByUser = true
            }

            dataState = dataState.copy(
                fromCryptoCurrency = fromToken,
                fromAccount = fromAccount,
                toCryptoCurrency = toToken,
                toAccount = toAccount,
                selectedProvider = null,
            )
            swapRouter.openScreen(SwapNavScreen.Main)
            if (handleSwapNotSupported(
                    state = tokens,
                    fromToken = fromToken,
                    toToken = toToken,
                    fromAccount = fromAccount,
                    toAccount = toAccount,
                )
            ) {
                return
            }
            modelScope.launch {
                TangemLogger.i(
                    "updateFeePaidCryptoCurrencyFor: id = ${fromToken.currency.id}, " +
                        "isOrderReversed: ${isOrderReversed.value}",
                )
                if ((uiState.sendCardData as? SwapCardState.SwapCardData)?.type is TransactionCardType.ReadOnly) {
                    uiState = stateBuilder.createInitialLoadingState(
                        initialCurrencyFrom = fromToken.currency,
                        initialCurrencyTo = toToken.currency,
                        fromNetworkInfo = fromToken.currency.getNetworkInfo(),
                    )
                }
                updateFeePaidCryptoCurrencyFor(fromToken)
                startLoadingQuotes(
                    fromToken = fromToken,
                    fromAccount = fromAccount,
                    toToken = toToken,
                    toAccount = toAccount,
                    amount = lastAmount.value,
                    reduceBalanceBy = lastReducedBalanceBy.value,
                    toProvidersList = findSwapProviders(fromToken, toToken),
                )
            }
            updateTokensState(tokens)
        }
    }

    private fun getSelectedTokenAndAccount(
        tokens: TokensDataStateExpress,
        id: String,
    ): Pair<CryptoCurrencyStatus?, Account.CryptoPortfolio?> {
        val accountCryptoCurrencyStatus = if (isOrderReversed.value) {
            tokens.fromGroup
        } else {
            tokens.toGroup
        }.accountCurrencyList.firstNotNullOfOrNull { accountSwapAvailability ->
            accountSwapAvailability.currencyList.firstOrNull { accountSwapCurrency ->
                accountSwapCurrency.cryptoCurrencyStatus.currency.id.value == id
            }
        }
        return accountCryptoCurrencyStatus?.cryptoCurrencyStatus to accountCryptoCurrencyStatus?.account
    }

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    private fun subscribeToCoinBalanceUpdates(
        userWalletId: UserWalletId,
        coin: CryptoCurrency.Coin,
        isFromCurrency: Boolean,
    ) {
        TangemLogger.d("Subscribe to ${coin.id} balance updates")

        getAccountCurrencyStatusUseCase(
            userWalletId = userWalletId,
            currency = coin,
        ).distinctUntilChanged { old, new -> old.status.value.amount == new.status.value.amount } // Check only balance changes
            .onEach { (account, currencyStatus) ->
                TangemLogger.d("${coin.id} balance is ${currencyStatus.value.amount ?: "null"}")

                if (isFromCurrency) {
                    dataState = dataState.copy(
                        feePaidCryptoCurrency = getFeePaidCryptoCurrencyStatusSyncUseCase(
                            userWalletId = userWalletId,
                            cryptoCurrencyStatus = currencyStatus,
                        )
                            .onLeft {
                                TangemLogger.e(
                                    "Coin balance: Unable to get fee paid crypto currency status for " +
                                        "${currencyStatus.currency.id}",
                                )
                            }
                            .onRight { status ->
                                if (status == null) {
                                    TangemLogger.e(
                                        "Coin balance: Fee paid crypto currency status is null " +
                                            "for ${currencyStatus.currency.id}",
                                    )
                                }
                            }
                            .getOrNull()
                            ?: currencyStatus,
                    )
                }

                uiState = when {
                    isFromCurrency && currencyStatus.currency.id == dataState.fromCryptoCurrency?.currency?.id -> {
                        dataState = dataState.copy(
                            fromCryptoCurrency = currencyStatus,
                            fromAccount = account,
                        )
                        stateBuilder.updateSendCurrencyBalance(uiState, currencyStatus)
                    }
                    !isFromCurrency && currencyStatus.currency.id == dataState.toCryptoCurrency?.currency?.id -> {
                        dataState = dataState.copy(
                            toCryptoCurrency = currencyStatus,
                            toAccount = account,
                        )
                        stateBuilder.updateReceiveCurrencyBalance(uiState, currencyStatus)
                    }
                    else -> {
                        uiState
                    }
                }
                startLoadingQuotesFromLastState(isSilent = true)
            }
            .flowOn(dispatchers.main)
            .launchIn(modelScope)
            .saveIn(if (isFromCurrency) fromTokenBalanceJobHolder else toTokenBalanceJobHolder)
    }

    private fun onChangeCardsClicked() {
        modelScope.launch {
            val newFromToken = dataState.toCryptoCurrency
            val newFromAccount = dataState.toAccount
            val newToToken = dataState.fromCryptoCurrency
            val newToAccount = dataState.fromAccount

            if (newFromToken != null && newToToken != null) {
                isAmountChangedByUser = true

                dataState = dataState.copy(
                    fromCryptoCurrency = newFromToken,
                    fromAccount = newFromAccount,
                    toCryptoCurrency = newToToken,
                    toAccount = newToAccount,
                )
                isOrderReversed.value = !isOrderReversed.value
                TangemLogger.i(
                    "updateFeePaidCryptoCurrencyFor: id = ${newFromToken.currency.id}, " +
                        "isOrderReversed: ${isOrderReversed.value}",
                )
                updateFeePaidCryptoCurrencyFor(newFromToken)
                dataState.tokensDataState?.let { tokensDataState ->
                    updateTokensState(tokensDataState)
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
                    fromAccount = dataState.fromAccount,
                )
                startLoadingQuotes(
                    fromToken = newFromToken,
                    fromAccount = newFromAccount,
                    toToken = newToToken,
                    toAccount = newToAccount,
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
        modelScope.launch {
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
                    fromAccount = dataState.fromAccount,
                )

                if (toToken != null) {
                    if (toToken.value.amount != null) {
                        isAmountChangedByUser = true
                    }

                    amountDebouncer.debounce(modelScope, DEBOUNCE_AMOUNT_DELAY, forceUpdate = forceQuotesUpdate) {
                        startLoadingQuotes(
                            fromToken = fromToken,
                            fromAccount = dataState.fromAccount,
                            toToken = toToken,
                            toAccount = dataState.toAccount,
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
        dataState.fromCryptoCurrency?.let { fromCurrency ->
            val balance = swapInteractor.getTokenBalance(fromCurrency)
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
        modelScope.launch {
            val metaInfo = getWalletMetaInfoUseCase(userWallet.walletId).getOrNull() ?: return@launch
            val customerId = getTangemPayCustomerIdUseCase(userWallet.walletId).getOrNull().orEmpty()
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
                analyticsEventHandler.send(SwapEvents.ButtonSwipeClicked())
            },
            onBackClicked = {
                val bottomSheet = uiState.bottomSheetConfig
                if (bottomSheet != null && bottomSheet.isShown) {
                    uiState = stateBuilder.dismissBottomSheet(uiState)
                } else {
                    swapRouter.back()
                }
                onSearchEntered("")
            },
            onMaxAmountSelected = ::onMaxAmountClicked,
            onReduceToAmount = ::onReduceAmountClicked,
            onReduceByAmount = ::onReduceAmountClicked,
            openPermissionBottomSheet = {
                singleTaskScheduler.cancelTask()
                sendGivePermissionClickedEvent()
                if (shouldUseGaslessApproval) {
                    approvalSlotNavigation.activate(Unit)
                } else {
                    uiState = stateBuilder.showPermissionBottomSheet(uiState) {
                        startLoadingQuotesFromLastState(isSilent = true)
                        analyticsEventHandler.send(SwapEvents.ButtonPermissionCancelClicked())
                        uiState = stateBuilder.dismissBottomSheet(uiState)
                    }
                }
            },
            onAmountSelected = { onAmountSelected(it) },
            onChangeApproveType = { approveType ->
                uiState = stateBuilder.updateApproveType(uiState, approveType)
            },
            onClickFee = {
                val selectedFee = (getSelectedFee() as? TxFee.Legacy)?.feeType ?: FeeType.NORMAL
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
                val fromToken = dataState.fromCryptoCurrency
                val toToken = dataState.toCryptoCurrency
                if (provider != null && swapState != null && fromToken != null) {
                    modelScope.launch {
                        feeSelectorRepository.state.value = FeeSelectorUM.Loading
                        feeSelectorReloadTrigger.triggerUpdate()
                    }
                    analyticsEventHandler.send(SwapEvents.ProviderChosen(provider))
                    uiState = stateBuilder.dismissBottomSheet(uiState)
                    setupLoadedState(
                        provider = provider,
                        state = swapState,
                        fromToken = fromToken,
                        toToken = toToken,
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
                val isPriceImpact = uiState.priceImpact.type != PriceImpact.Type.NONE
                showSwapInfoAlert(isPriceImpact, currencySymbol, selectedProvider)
            },
            onLinkClick = urlOpener::openUrl,
            onSelectTokenClick = {
                swapRouter.openScreen(SwapNavScreen.SelectToken)
                sendSelectTokenScreenOpenedEvent()
            },
            onSuccess = {
                swapRouter.openScreen(SwapNavScreen.Success)
            },
            onOpenLearnMoreAboutApproveClick = {
                urlOpener.openUrl(RESOURCE_TO_LEARN_ABOUT_APPROVING_IN_SWAP)
            },
        )
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
        val fromToken = requireNotNull(dataState.fromCryptoCurrency) {
            "fromCryptoCurrency should not be null"
        }
        return when (val fee = getSelectedFee()) {
            is TxFee.FeeComponent -> fee.selectedToken?.currency ?: fromToken.currency
            is TxFee.Legacy,
            null,
            -> fromToken.currency
        }
    }

    private fun sendApproveSuccessEvent(fromToken: CryptoCurrency, feeType: FeeType, approveType: SwapApproveType) {
        val feeToken = getFeeToken().symbol
        val event = AnalyticsParam.TxSentFrom.Approve(
            blockchain = fromToken.network.name,
            token = fromToken.symbol,
            feeType = AnalyticsParam.FeeType.fromString(feeType.getNameForAnalytics()),
            permissionType = approveType.getNameForAnalytics(),
            feeToken = feeToken,
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
        return state.minByOrNull { entry ->
            val toTokenInfo = entry.value.toTokenInfo
            val fromAmountFiat = entry.value.fromTokenInfo.amountFiat
            val toAmountFiat = toTokenInfo.amountFiat
            if (!fromAmountFiat.isNullOrZero() && !toAmountFiat.isNullOrZero()) {
                fromAmountFiat.divide(
                    toAmountFiat,
                    toTokenInfo.cryptoCurrencyStatus.currency.decimals,
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

    private fun findSwapProviders(fromToken: CryptoCurrencyStatus, toToken: CryptoCurrencyStatus): List<SwapProvider> {
        val groupToFind = if (isOrderReversed.value) {
            dataState.tokensDataState?.fromGroup
        } else {
            dataState.tokensDataState?.toGroup
        } ?: return emptyList()

        val idToFind = if (isOrderReversed.value) {
            fromToken.currency.id.value
        } else {
            toToken.currency.id.value
        }

        return groupToFind.accountCurrencyList.firstNotNullOfOrNull { (_, currencyList) ->
            currencyList.find { accountSwapCurrency ->
                idToFind == accountSwapCurrency.cryptoCurrencyStatus.currency.id.value &&
                    accountSwapCurrency.isAvailable
            }
        }?.providers
            ?.filterForTangemPayWithdrawal()
            .orEmpty()
    }

    /**
     * @return true if swap is not supported and UI was updated to show error state
     */
    private fun handleSwapNotSupported(
        state: TokensDataStateExpress,
        fromToken: CryptoCurrencyStatus,
        toToken: CryptoCurrencyStatus,
        fromAccount: Account.CryptoPortfolio?,
        toAccount: Account.CryptoPortfolio?,
    ): Boolean {
        val selectedCurrency = if (isOrderReversed.value) fromToken else toToken
        if (isTokenAvailableForSwap(state, selectedCurrency, isOrderReversed.value)) return false

        analyticsEventHandler.send(
            SwapEvents.NoticeUnavailableToSwapPair(
                sendToken = fromToken.currency.symbol,
                receiveToken = toToken.currency.symbol,
                sendBlockchain = fromToken.currency.network.name,
                receiveBlockchain = toToken.currency.network.name,
            ),
        )
        // Cancel periodic quote task if selected token is not supported
        singleTaskScheduler.cancelTask()
        // Reset data state
        dataState = SwapProcessDataState(
            tokensDataState = dataState.tokensDataState,
        )
        lastReducedBalanceBy.value = BigDecimal.ZERO
        lastAmount.value = INITIAL_AMOUNT
        uiState = stateBuilder.createSwapNotSupportedState(
            uiStateHolder = uiState,
            fromToken = fromToken,
            toToken = toToken,
            fromAccount = fromAccount,
            toAccount = toAccount,
            mainTokenId = initialCurrencyFrom.id.value,
        )
        return true
    }

    private fun isTokenAvailableForSwap(
        state: TokensDataStateExpress,
        selectedCurrency: CryptoCurrencyStatus,
        isReverseFromTo: Boolean,
    ): Boolean {
        val group = if (isReverseFromTo) state.fromGroup else state.toGroup
        val idToFind = selectedCurrency.currency.id.value

        return group.accountCurrencyList.any { (_, currencyList) ->
            currencyList.any { accountSwapCurrency ->
                idToFind == accountSwapCurrency.cryptoCurrencyStatus.currency.id.value &&
                    accountSwapCurrency.isAvailable
            }
        }
    }

    private fun List<SwapProvider>.filterForTangemPayWithdrawal(): List<SwapProvider> {
        return if (tangemPayInput?.isWithdrawal == true) {
            filter { it.type == ExchangeProviderType.CEX }
        } else {
            this
        }
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

    private fun isReverseSwapPossible(): Boolean {
        if (tangemPayInput != null) return false
        val from = dataState.fromCryptoCurrency ?: return false
        val to = dataState.toCryptoCurrency ?: return false

        val currenciesGroup = if (isOrderReversed.value) {
            dataState.tokensDataState?.toGroup
        } else {
            dataState.tokensDataState?.fromGroup
        } ?: return false

        val chosen = if (isOrderReversed.value) from else to

        return currenciesGroup.accountCurrencyList.flatMap { accountSwapAvailability ->
            accountSwapAvailability.currencyList.map { accountSwapCurrency ->
                accountSwapCurrency.cryptoCurrencyStatus
            }
        }.map { currencyStatus -> currencyStatus.currency }.contains(chosen.currency)
    }

    private fun sendNoticePermissionNeededEvent() {
        val sendTokenSymbol = dataState.fromCryptoCurrency?.currency?.symbol ?: return
        val receiveTokenSymbol = dataState.toCryptoCurrency?.currency?.symbol ?: return
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
        val sendTokenSymbol = dataState.fromCryptoCurrency?.currency?.symbol ?: return
        val receiveTokenSymbol = dataState.toCryptoCurrency?.currency?.symbol ?: return
        val provider = dataState.selectedProvider ?: return
        analyticsEventHandler.send(
            SwapEvents.ButtonGivePermissionClicked(
                sendToken = sendTokenSymbol,
                receiveToken = receiveTokenSymbol,
                provider = provider,
            ),
        )
    }

    private fun sendPermissionApproveClickedEvent() {
        val sendTokenSymbol = dataState.fromCryptoCurrency?.currency?.symbol ?: return
        val receiveTokenSymbol = dataState.toCryptoCurrency?.currency?.symbol ?: return
        val approveType = uiState.permissionState.getApproveTypeOrNull() ?: return
        val provider = dataState.selectedProvider ?: return

        analyticsEventHandler.send(
            SwapEvents.ButtonPermissionApproveClicked(
                sendToken = sendTokenSymbol,
                receiveToken = receiveTokenSymbol,
                approveType = approveType,
                provider = provider,
            ),
        )
    }

    private fun updateWalletBalance() {
        dataState.fromCryptoCurrency?.currency?.network?.let { network ->
            modelScope.launch {
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

    private fun onTangemPayWithdrawalError(txId: String?) {
        showTransactionErrorAlert(
            error = SwapTransactionState.Error.TangemPayWithdrawalError(txId.orEmpty()),
            onSupportClick = ::onTangemPaySupportClick,
        )
    }

    private fun onFailedTxEmailClick(errorMessage: String) {
        modelScope.launch {
            val transaction = dataState.swapDataModel?.transaction
            val fromCurrencyStatus = dataState.fromCryptoCurrency ?: initialFromStatus
            val network = fromCurrencyStatus.currency.network

            saveBlockchainErrorUseCase(
                error = BlockchainErrorInfo(
                    errorMessage = errorMessage,
                    blockchainId = network.rawId,
                    derivationPath = network.derivationPath.value,
                    destinationAddress = transaction?.txTo.orEmpty(),
                    tokenSymbol = fromCurrencyStatus.currency.symbol,
                    amount = dataState.amount.orEmpty(),
                    fee = when (val fee = getSelectedFee()) {
                        is TxFee.FeeComponent -> fee.fee.amount.value?.toString()
                        is TxFee.Legacy -> fee.feeCryptoFormatted
                        null -> ""
                    },
                ),
            )

            val metaInfo = getWalletMetaInfoUseCase(userWallet.walletId)
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

    private fun CryptoCurrency.getNetworkInfo(): NetworkInfo {
        return NetworkInfo(
            name = this.network.name,
            blockchainId = this.network.rawId,
        )
    }

    private suspend fun getFromStatus(): CryptoCurrencyStatus? {
        return if (tangemPayInput != null) {
            getTangemPayCurrencyStatusUseCase(
                currency = initialCurrencyFrom,
                cryptoAmount = tangemPayInput.cryptoAmount,
                fiatAmount = tangemPayInput.fiatAmount,
                depositAddress = tangemPayInput.depositAddress,
            )
        } else {
            singleAccountStatusListSupplier.getSyncOrNull(params.userWalletId)
                .getCryptoCurrencyStatus(currency = initialCurrencyFrom)
                .getOrNull()
        }
    }

    private fun getSelectedFeeState(): TxFeeSealedState {
        val feeStateUM = feeSelectorRepository.state.value as? FeeSelectorUM.Content

        if (feeStateUM == null) {
            TangemLogger.e("getSelectedFeeState: FeeSelectorUM is not Content: $feeStateUM, returning Legacy state")
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
            TangemLogger.e("getSelectedFee: FeeSelectorUM is not Content: $feeStateUM, returning null")
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
            val fromToken = dataState.fromCryptoCurrency ?: return Either.Left(GetFeeError.UnknownError)
            val toToken = dataState.toCryptoCurrency ?: return Either.Left(GetFeeError.UnknownError)
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
                fromToken = fromToken,
                fromAccount = dataState.fromAccount,
                toToken = toToken,
                toAccount = dataState.toAccount,
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

            // If fee currency is same as from currency, we need to reload quotes to update fee info
            val isFeeCurrencySameAsFromCurrency = newState is FeeSelectorUM.Content &&
                dataState.fromCryptoCurrency?.currency?.id == newState.feeExtraInfo.feeCryptoCurrencyStatus.currency.id

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

            val fromToken = dataState.fromCryptoCurrency ?: return Either.Left(GetFeeError.UnknownError)
            val toToken = dataState.toCryptoCurrency ?: return Either.Left(GetFeeError.UnknownError)
            val selectedProvider = dataStateStateFlow.first { it.selectedProvider != null }.selectedProvider!!

            if (dataState.lastLoadedSwapStates[selectedProvider] !is SwapState.QuotesLoadedState) {
                TangemLogger.e("loadFee: Quotes not loaded ${dataState.lastLoadedSwapStates[selectedProvider]}")
                return Either.Left(GetFeeError.UnknownError)
            }

            if (isPermissionNotificationShown()) {
                TangemLogger.e("loadFee: Permission notification is shown, cannot load fee")
                return Either.Left(GetFeeError.UnknownError)
            }

            return swapInteractor.loadFeeForSwapTransaction(
                fromToken = fromToken,
                fromAccount = dataState.fromAccount,
                toToken = toToken,
                toAccount = dataState.toAccount,
                provider = selectedProvider,
                amount = lastAmount.value,
                reduceBalanceBy = lastReducedBalanceBy.value,
            )
                .onLeft {
                    TangemLogger.e("loadFee: Failed to load fee with error $it")
                }
                .onRight {
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