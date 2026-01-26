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
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.common.routing.AppRouter
import com.tangem.common.ui.bottomsheet.permission.state.ApproveType
import com.tangem.common.ui.bottomsheet.permission.state.GiveTxPermissionState.InProgress.getApproveTypeOrNull
import com.tangem.common.ui.markets.models.MarketsListItemUM
import com.tangem.core.analytics.api.AnalyticsErrorHandler
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.utils.InputNumberFormatter
import com.tangem.domain.account.featuretoggle.AccountsFeatureToggles
import com.tangem.domain.account.status.model.AccountCryptoCurrencyStatus
import com.tangem.domain.account.status.usecase.GetAccountCurrencyStatusUseCase
import com.tangem.domain.account.usecase.IsAccountsModeEnabledUseCase
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.card.common.extensions.hotWalletExcludedBlockchains
import com.tangem.domain.express.models.ExpressOperationType
import com.tangem.domain.feedback.GetWalletMetaInfoUseCase
import com.tangem.domain.feedback.SaveBlockchainErrorUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.BlockchainErrorInfo
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.markets.GetMarketsTokenListFlowUseCase
import com.tangem.domain.markets.GetTokenMarketInfoUseCase
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.WithdrawalResult
import com.tangem.domain.promo.GetStoryContentUseCase
import com.tangem.domain.promo.ShouldShowStoriesUseCase
import com.tangem.domain.promo.models.StoryContentIds
import com.tangem.domain.settings.usercountry.GetUserCountryUseCase
import com.tangem.domain.settings.usercountry.models.UserCountry
import com.tangem.domain.settings.usercountry.models.needApplyFCARestrictions
import com.tangem.domain.tangempay.GetTangemPayCurrencyStatusUseCase
import com.tangem.domain.tangempay.TangemPayWithdrawUseCase
import com.tangem.domain.tokens.GetFeePaidCryptoCurrencyStatusSyncUseCase
import com.tangem.domain.tokens.GetMinimumTransactionAmountSyncUseCase
import com.tangem.domain.tokens.GetSingleCryptoCurrencyStatusUseCase
import com.tangem.domain.tokens.UpdateDelayedNetworkStatusUseCase
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.transaction.usecase.gasless.IsGaslessFeeSupportedForNetwork
import com.tangem.domain.txhistory.usecase.GetExplorerTransactionUrlUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.feature.swap.analytics.StoriesEvents
import com.tangem.feature.swap.analytics.SwapEvents
import com.tangem.feature.swap.component.SwapFeeSelectorBlockComponent
import com.tangem.feature.swap.converters.TokenMarketInfoToParamsConverter
import com.tangem.feature.swap.domain.SwapInteractor
import com.tangem.feature.swap.domain.TransactionFeeResult
import com.tangem.feature.swap.domain.TxFeeSealedState
import com.tangem.feature.swap.domain.models.ExpressDataError
import com.tangem.feature.swap.domain.models.ExpressException
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.domain.*
import com.tangem.feature.swap.domain.models.ui.*
import com.tangem.feature.swap.models.AddToPortfolioRoute
import com.tangem.feature.swap.models.SwapCardState
import com.tangem.feature.swap.models.SwapStateHolder
import com.tangem.feature.swap.models.UiActions
import com.tangem.feature.swap.models.market.SwapMarketsListBatchFlowManager
import com.tangem.feature.swap.models.market.state.SwapMarketState
import com.tangem.feature.swap.models.states.SwapNotificationUM
import com.tangem.feature.swap.presentation.R
import com.tangem.feature.swap.router.SwapNavScreen
import com.tangem.feature.swap.router.SwapRouter
import com.tangem.feature.swap.ui.StateBuilder
import com.tangem.feature.swap.utils.formatToUIRepresentation
import com.tangem.features.feed.components.market.details.portfolio.add.AddToPortfolioComponent
import com.tangem.features.feed.components.market.details.portfolio.add.AddToPortfolioManager
import com.tangem.features.send.v2.api.SendFeatureToggles
import com.tangem.features.send.v2.api.entity.FeeSelectorUM
import com.tangem.features.send.v2.api.subcomponents.feeSelector.FeeSelectorReloadTrigger
import com.tangem.features.swap.SwapComponent
import com.tangem.features.swap.SwapFeatureToggles
import com.tangem.lib.crypto.BlockchainUtils
import com.tangem.utils.Provider
import com.tangem.utils.TangemBlogUrlBuilder.RESOURCE_TO_LEARN_ABOUT_APPROVING_IN_SWAP
import com.tangem.utils.coroutines.*
import com.tangem.utils.isNullOrZero
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
import kotlin.collections.filter

typealias SuccessLoadedSwapData = Map<SwapProvider, SwapState.QuotesLoadedState>

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
    private val getSingleCryptoCurrencyStatusUseCase: GetSingleCryptoCurrencyStatusUseCase,
    private val getFeePaidCryptoCurrencyStatusSyncUseCase: GetFeePaidCryptoCurrencyStatusSyncUseCase,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val getWalletMetaInfoUseCase: GetWalletMetaInfoUseCase,
    private val saveBlockchainErrorUseCase: SaveBlockchainErrorUseCase,
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase,
    private val getMinimumTransactionAmountSyncUseCase: GetMinimumTransactionAmountSyncUseCase,
    private val getExplorerTransactionUrlUseCase: GetExplorerTransactionUrlUseCase,
    private val shouldShowStoriesUseCase: ShouldShowStoriesUseCase,
    private val getStoryContentUseCase: GetStoryContentUseCase,
    getUserCountryUseCase: GetUserCountryUseCase,
    getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    swapInteractorFactory: SwapInteractor.Factory,
    private val urlOpener: UrlOpener,
    router: AppRouter,
    private val isAccountsModeEnabledUseCase: IsAccountsModeEnabledUseCase,
    private val getAccountCurrencyStatusUseCase: GetAccountCurrencyStatusUseCase,
    private val accountsFeatureToggles: AccountsFeatureToggles,
    private val getTangemPayCurrencyStatusUseCase: GetTangemPayCurrencyStatusUseCase,
    private val tangemPayWithdrawUseCase: TangemPayWithdrawUseCase,
    private val iGaslessFeeSupportedForNetwork: IsGaslessFeeSupportedForNetwork,
    private val feeSelectorReloadTrigger: FeeSelectorReloadTrigger,
    private val sendFeatureToggles: SendFeatureToggles,
    private val getMarketsTokenListFlowUseCase: GetMarketsTokenListFlowUseCase,
    swapFeatureToggles: SwapFeatureToggles,
    private val addToPortfolioManagerFactory: AddToPortfolioManager.Factory,
    private val getTokenMarketInfoUseCase: GetTokenMarketInfoUseCase,
    private val excludedBlockchains: ExcludedBlockchains,
    private val getUserWalletsUseCase: GetWalletsUseCase,
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

    private lateinit var initialFromStatus: CryptoCurrencyStatus
    private var initialToStatus: CryptoCurrencyStatus? = null

    private var isBalanceHidden = true
    private var isAccountsMode: Boolean = false

    private val selectedAppCurrencyFlow: StateFlow<AppCurrency> = createSelectedAppCurrencyFlow()

    private val stateBuilder = StateBuilder(
        userWalletProvider = Provider { userWallet },
        actions = createUiActions(),
        isBalanceHiddenProvider = Provider { isBalanceHidden },
        appCurrencyProvider = Provider(selectedAppCurrencyFlow::value),
        isAccountsModeProvider = Provider { isAccountsMode },
        iGaslessFeeSupportedForNetwork = iGaslessFeeSupportedForNetwork,
    )

    private val inputNumberFormatter =
        InputNumberFormatter(
            NumberFormat.getInstance(Locale.getDefault()) as? DecimalFormat
                ?: error("NumberFormat is not DecimalFormat"),
        )
    private val amountDebouncer = Debouncer()
    private val searchDebouncer = Debouncer()
    private val singleTaskScheduler = SingleTaskScheduler<Map<SwapProvider, SwapState>>()

    var dataState by mutableStateOf(SwapProcessDataState())

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
    private var isOrderReversed = false
    private val lastAmount = mutableStateOf(INITIAL_AMOUNT)
    private val lastReducedBalanceBy = mutableStateOf(BigDecimal.ZERO)
    private val swapRouter: SwapRouter = SwapRouter(router = router)
    private var userCountry: UserCountry? = null

    private lateinit var fromAccountCurrencyStatus: AccountCryptoCurrencyStatus
    private var toAccountCurrencyStatus: AccountCryptoCurrencyStatus? = null

    private val isUserResolvableError: (SwapState) -> Boolean = { swapState ->
        swapState is SwapState.SwapError &&
            (
                swapState.error is ExpressDataError.ExchangeTooSmallAmountError ||
                    swapState.error is ExpressDataError.ExchangeTooBigAmountError
                )
    }

    private val fromTokenBalanceJobHolder = JobHolder()
    private val toTokenBalanceJobHolder = JobHolder()
    private val addToPortfolioJobHolder = JobHolder()

    private var isAmountChangedByUser: Boolean = false
    private var lastPermissionNotificationTokens: Pair<String, String>? = null

    private val searchQueryState = MutableStateFlow("")
    private val visibleMarketItemIds = MutableStateFlow<List<CryptoCurrency.RawID>>(emptyList())
    private val searchMarketsListManager by lazy {
        SwapMarketsListBatchFlowManager(
            getMarketsTokenListFlowUseCase = getMarketsTokenListFlowUseCase,
            batchFlowType = GetMarketsTokenListFlowUseCase.BatchFlowType.Search,
            currentAppCurrency = Provider { selectedAppCurrencyFlow.value },
            currentSearchText = Provider { searchQueryState.value },
            modelScope = modelScope,
            dispatchers = dispatchers,
        )
    }

    val currentScreen: SwapNavScreen
        get() = swapRouter.currentScreen

    val bottomSheetNavigation: SlotNavigation<AddToPortfolioRoute> = SlotNavigation()
    val addToPortfolioCallback = object : AddToPortfolioComponent.Callback {
        override fun onDismiss() = bottomSheetNavigation.dismiss()

        override fun onSuccess(addedToken: CryptoCurrency) {
            modelScope.launch {
                bottomSheetNavigation.dismiss()
                uiState.selectTokenState?.let { currentSelectState ->
                    uiState = uiState.copy(
                        selectTokenState = currentSelectState.copy(
                            marketsState = null,
                        ),
                    )
                }
                getAccountCurrencyStatusUseCase.invoke(userWalletId, addedToken)
                    .firstOrNull {
                        it.status.value is CryptoCurrencyStatus.Loaded
                    }?.let { (account, status) ->
                        applyAddedToken(status, account)
                    }
            }
        }
    }
    var addToPortfolioManager: AddToPortfolioManager? = null

    init {
        userCountry = getUserCountryUseCase.invokeSync().getOrNull()
            ?: UserCountry.Other(Locale.getDefault().country)
        modelScope.launch {
            initStories()
            swapRouter.openScreen(SwapNavScreen.PromoStories)
        }

        modelScope.launch(dispatchers.io) {
            if (accountsFeatureToggles.isFeatureEnabled && tangemPayInput == null) {
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
                    uiState = stateBuilder.addAlert(uiState = uiState, onDismiss = swapRouter::back)
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
                    getSingleCryptoCurrencyStatusUseCase.invokeMultiWalletSync(
                        userWalletId = userWalletId,
                        cryptoCurrencyId = currencyTo.id,
                    ).getOrNull()
                }

                if (fromStatus == null) {
                    uiState = stateBuilder.addAlert(uiState = uiState, onDismiss = swapRouter::back)
                } else {
                    initialFromStatus = fromStatus
                    initialToStatus = toStatus
                    initTokens(isInitiallyReversed)
                }
            }
        }

        analyticsEventHandler.send(SwapEvents.SwapScreenOpened(initialCurrencyFrom.symbol))

        getBalanceHidingSettingsUseCase()
            .onEach { settings ->
                isBalanceHidden = settings.isBalanceHidden
                uiState = stateBuilder.updateBalanceHiddenState(uiState, isBalanceHidden)
            }
            .launchIn(modelScope)

        if (swapFeatureToggles.isMarketListFeatureEnabled) {
            combine(
                flow = searchQueryState
                    .onEach { searchQuery ->
                        searchMarketsListManager.reload(searchQuery)
                    },
                flow2 = searchMarketsListManager.uiItems,
                flow3 = searchMarketsListManager.isInInitialLoadingErrorState,
                flow4 = searchMarketsListManager.isSearchNotFoundState,
                flow5 = searchMarketsListManager.totalCount.filterNotNull(),
            ) { searchQuery, uiItems, isError, isSearchNotFound, total ->
                when {
                    searchQuery.isEmpty() -> {
                        visibleMarketItemIds.value = emptyList()
                        null
                    }
                    isError -> SwapMarketState.LoadingError(
                        onRetryClicked = { searchMarketsListManager.reload(searchQuery) },
                    )
                    isSearchNotFound -> SwapMarketState.SearchNothingFound
                    uiItems.isEmpty() -> SwapMarketState.Loading
                    else -> SwapMarketState.Content(
                        items = uiItems,
                        loadMore = { searchMarketsListManager.loadMore() },
                        onItemClick = { item ->
                            addToPortfolioItem(item)
                        },
                        visibleIdsChanged = { visibleMarketItemIds.value = it },
                        total = total,
                    )
                }
            }
                .distinctUntilChanged()
                .onEach { marketsState ->
                    uiState.selectTokenState?.let { currentSelectState ->
                        uiState = uiState.copy(
                            selectTokenState = currentSelectState.copy(
                                marketsState = marketsState,
                            ),
                        )
                    }
                }
                .launchIn(modelScope)
        }

        modelScope.launch {
            visibleMarketItemIds.mapNotNull { rawIDS ->
                if (rawIDS.isNotEmpty()) {
                    searchMarketsListManager.getBatchKeysByItemIds(rawIDS)
                } else {
                    null
                }
            }.distinctUntilChanged().collectLatest { visibleBatchKeys ->
                searchMarketsListManager.loadCharts(visibleBatchKeys)
            }
        }
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

                val (selectedCurrency, selectedAccount) = if (accountsFeatureToggles.isFeatureEnabled) {
                    val selectedAccountCurrency = toAccountCurrencyStatus ?: swapInteractor.getInitialCurrencyToSwapV2(
                        initialCryptoCurrency = initialCurrencyFrom,
                        state = state,
                        isReverseFromTo = isReverseFromTo,
                    )?.let { accountSwapCurrency ->
                        AccountCryptoCurrencyStatus(
                            account = accountSwapCurrency.account,
                            status = accountSwapCurrency.cryptoCurrencyStatus,
                        )
                    }
                    selectedAccountCurrency?.status to selectedAccountCurrency?.account
                } else {
                    val selectedCurrency = initialToStatus ?: swapInteractor.getInitialCurrencyToSwap(
                        initialCryptoCurrency = initialCurrencyFrom,
                        state = state,
                        isReverseFromTo = isReverseFromTo,
                    )
                    selectedCurrency to null
                }

                applyInitialTokenChoice(
                    state = state,
                    selectedCurrency = selectedCurrency,
                    selectedAccount = selectedAccount,
                    isReverseFromTo = isReverseFromTo,
                )

                val fromCryptoCurrency = if (isOrderReversed) {
                    dataState.toCryptoCurrency
                } else {
                    dataState.fromCryptoCurrency
                }

                fromCryptoCurrency?.let { cryptoCurrency ->
                    dataState = dataState.copy(
                        feePaidCryptoCurrency = getFeePaidCryptoCurrencyStatusSyncUseCase(
                            userWalletId = userWalletId,
                            cryptoCurrencyStatus = cryptoCurrency,
                        ).getOrNull(),
                    )
                }

                subscribeToCoinBalanceUpdatesIfNeeded()
            }.onFailure { error ->
                Timber.e(error)

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

    private fun applyAddedToken(addedToken: CryptoCurrencyStatus, addedAccount: Account.CryptoPortfolio?) {
        modelScope.launch {
            runCatching(dispatchers.io) {
                swapInteractor.getTokensDataState(initialCurrencyFrom)
            }.onSuccess { state ->
                updateTokensState(state)

                applyInitialTokenChoice(
                    state = state,
                    selectedCurrency = addedToken,
                    selectedAccount = addedAccount,
                    isReverseFromTo = isOrderReversed,
                )

                subscribeToCoinBalanceUpdatesIfNeeded()

                swapRouter.back()
            }.onFailure { error ->
                Timber.e(error)
            }
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

    private fun initStories() {
        modelScope.launch {
            getStoryContentUseCase.invokeSync(StoryContentIds.STORY_FIRST_TIME_SWAP.id).fold(
                ifLeft = {
                    Timber.e("Unable to load stories for ${StoryContentIds.STORY_FIRST_TIME_SWAP.id}")
                },
                ifRight = { story ->
                    if (story != null) {
                        uiState = stateBuilder.createStoriesState(uiState, story)
                    }
                },
            )
        }
    }

    private fun applyInitialTokenChoice(
        state: TokensDataStateExpress,
        selectedCurrency: CryptoCurrencyStatus?,
        selectedAccount: Account.CryptoPortfolio?,
        isReverseFromTo: Boolean,
    ) {
        // exceptional case
        if (selectedCurrency == null) {
            analyticsEventHandler.send(SwapEvents.NoticeNoAvailableTokensToSwap())
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
        val (fromAccount, toAccount) = if (accountsFeatureToggles.isFeatureEnabled && tangemPayInput == null) {
            if (isOrderReversed) {
                selectedAccount to fromAccountCurrencyStatus.account
            } else {
                fromAccountCurrencyStatus.account to selectedAccount
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
        startLoadingQuotes(
            fromToken = fromCurrencyStatus,
            fromAccount = fromAccount,
            toToken = toCurrencyStatus,
            toAccount = toAccount,
            amount = lastAmount.value,
            reduceBalanceBy = lastReducedBalanceBy.value,
            toProvidersList = findSwapProviders(fromCurrencyStatus, toCurrencyStatus),
        )
    }

    private fun updateTokensState(tokenDataState: TokensDataStateExpress) {
        val tokensDataState = if (isOrderReversed) tokenDataState.fromGroup else tokenDataState.toGroup

        uiState = if (accountsFeatureToggles.isFeatureEnabled) {
            stateBuilder.addTokensToStateV2(
                uiState = uiState,
                tokensDataState = tokensDataState,
                isAccountsMode = isAccountsMode,
            )
        } else {
            stateBuilder.addTokensToState(
                uiState = uiState,
                tokensDataState = tokensDataState,
                fromToken = dataState.fromCryptoCurrency?.currency ?: initialCurrencyFrom,
            )
        }
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
                    setupLoadedState(provider, state, fromToken)
                    val successStates = providersState.getLastLoadedSuccessStates()
                    val pricesLowerBest = getPricesLowerBest(provider.providerId, successStates)
                    uiState = stateBuilder.updateProvidersBottomSheetContent(
                        uiState = uiState,
                        pricesLowerBest = pricesLowerBest,
                        tokenSwapInfoForProviders = successStates.entries
                            .associate { it.key.providerId to it.value.toTokenInfo },
                    )
                    if (updateFeeBlock) {
                        modelScope.launch { feeSelectorReloadTrigger.triggerUpdate() }
                    }
                } else {
                    feeSelectorRepository.state.value = FeeSelectorUM.Error(GetFeeError.UnknownError, isHidden = true)
                    Timber.e("Accidentally empty quotes list")
                }
            },
            onError = { error ->
                Timber.e("Error when loading quotes: $error")
                feeSelectorRepository.state.value = FeeSelectorUM.Error(GetFeeError.UnknownError, isHidden = true)
                uiState = stateBuilder.addNotification(uiState, null) { startLoadingQuotesFromLastState() }
            },
        )
    }

    private fun setupLoadedState(provider: SwapProvider, state: SwapState, fromToken: CryptoCurrencyStatus) {
        when (state) {
            is SwapState.QuotesLoadedState -> {
                setupQuotesLoadedUiState(provider, state, fromToken)
                sendAnalyticsForNotifications(fromToken)
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

    private fun sendAnalyticsForNotifications(fromToken: CryptoCurrencyStatus) {
        if (uiState.notifications.any { it is SwapNotificationUM.Error.UnableToCoverFeeWarning }) {
            analyticsEventHandler.send(
                SwapEvents.NoticeNotEnoughFee(
                    token = initialCurrencyFrom.symbol,
                    blockchain = fromToken.currency.network.name,
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
            Timber.e("Last loaded quotes state is null")
            return
        }
        val fromCurrency = requireNotNull(dataState.fromCryptoCurrency)
        val fee = getSelectedFee()

        if (fee == null && tangemPayInput?.isWithdrawal != true) {
            makeDefaultAlert(resourceReference(R.string.swapping_fee_estimation_error_text))
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
                            makeDefaultAlert(resourceReference(R.string.swapping_fee_estimation_error_text))
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
                            Timber.i("tx hash explore not supported")
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
                        uiState = stateBuilder.createDemoModeAlert(
                            uiState = uiState,
                            onDismiss = { uiState = stateBuilder.clearAlert(uiState) },
                            isReverseSwapPossible = isReverseSwapPossible(),
                        )
                    }
                    is SwapTransactionState.Error -> {
                        startLoadingQuotesFromLastState()
                        uiState = stateBuilder.createErrorTransactionAlert(
                            uiState = uiState,
                            error = swapTransactionState,
                            onDismiss = { uiState = stateBuilder.clearAlert(uiState) },
                            onSupportClick = ::onFailedTxEmailClick,
                            isReverseSwapPossible = isReverseSwapPossible(),
                        )
                    }
                    is SwapTransactionState.TangemPayWithdrawalData -> {
                        processTangemPayWithdrawal(swapTransactionState = swapTransactionState)
                    }
                }
            }.onFailure { error ->
                Timber.e(error)
                startLoadingQuotesFromLastState()
                makeDefaultAlert()
            }
        }
    }

    private suspend fun processTangemPayWithdrawal(swapTransactionState: SwapTransactionState.TangemPayWithdrawalData) {
        tangemPayWithdrawUseCase(
            userWallet = userWallet,
            cryptoAmount = swapTransactionState.cryptoAmount,
            cryptoCurrencyId = swapTransactionState.cryptoCurrencyId,
            receiverCexAddress = swapTransactionState.cexAddress,
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

    private fun sendSuccessEvent() {
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
                fromDerivationIndex = fromDerivationIndex,
                toDerivationIndex = toDerivationIndex,
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
                        makeDefaultAlert(resourceReference(R.string.swapping_fee_estimation_error_text))
                        Timber.e("Fee should not be Empty")
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
                            spenderAddress = approveDataModel.spenderAddress,
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
                            uiState = stateBuilder.createErrorTransactionAlert(
                                uiState = uiState,
                                error = swapTransactionState,
                                onDismiss = { uiState = stateBuilder.clearAlert(uiState) },
                                onSupportClick = ::onFailedTxEmailClick,
                                isReverseSwapPossible = isReverseSwapPossible(),
                            )
                        }
                        SwapTransactionState.DemoMode -> {
                            uiState = stateBuilder.createDemoModeAlert(
                                uiState = uiState,
                                onDismiss = { uiState = stateBuilder.clearAlert(uiState) },
                                isReverseSwapPossible = isReverseSwapPossible(),
                            )
                        }
                        is SwapTransactionState.TangemPayWithdrawalData -> {
                            processTangemPayWithdrawal(swapTransactionState = swapTransactionState)
                        }
                    }
                }.onFailure { makeDefaultAlert() }
            }.onFailure { error ->
                Timber.e(error.message.orEmpty())
                makeDefaultAlert()
            }
        }
    }

    private fun onSearchEntered(searchQuery: String) {
        searchDebouncer.debounce(modelScope, DEBOUNCE_SEARCH_DELAY) {
            searchQueryState.value = searchQuery

            val tokenDataState = dataState.tokensDataState ?: return@debounce
            val group = if (isOrderReversed) {
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

            val filteredTokenDataState = if (isOrderReversed) {
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
    }

    @Suppress("LongMethod")
    private fun onTokenSelect(id: String) {
        val tokens = dataState.tokensDataState ?: return
        val (foundToken, foundAccount) = getSelectedTokenAndAccount(tokens, id)

        foundToken?.currency?.symbol?.let { symbol ->
            analyticsEventHandler.send(SwapEvents.ChooseTokenScreenResult(isTokenChosen = true, token = symbol))
        }

        if (foundToken != null) {
            val fromToken: CryptoCurrencyStatus
            val fromAccount: Account.CryptoPortfolio?
            val toToken: CryptoCurrencyStatus
            val toAccount: Account.CryptoPortfolio?
            if (isOrderReversed) {
                fromToken = foundToken
                fromAccount = foundAccount
                toToken = initialFromStatus
                toAccount = if (accountsFeatureToggles.isFeatureEnabled) {
                    fromAccountCurrencyStatus.account
                } else {
                    null
                }

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
                fromAccount = if (accountsFeatureToggles.isFeatureEnabled) {
                    fromAccountCurrencyStatus.account
                } else {
                    null
                }
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
            startLoadingQuotes(
                fromToken = fromToken,
                fromAccount = fromAccount,
                toToken = toToken,
                toAccount = toAccount,
                amount = lastAmount.value,
                reduceBalanceBy = lastReducedBalanceBy.value,
                toProvidersList = findSwapProviders(fromToken, toToken),
            )
            swapRouter.openScreen(SwapNavScreen.Main)
            updateTokensState(tokens)
        }
    }

    private fun getSelectedTokenAndAccount(
        tokens: TokensDataStateExpress,
        id: String,
    ): Pair<CryptoCurrencyStatus?, Account.CryptoPortfolio?> {
        return if (accountsFeatureToggles.isFeatureEnabled) {
            val accountCryptoCurrencyStatus = if (isOrderReversed) {
                tokens.fromGroup
            } else {
                tokens.toGroup
            }.accountCurrencyList.firstNotNullOfOrNull { accountSwapAvailability ->
                accountSwapAvailability.currencyList.firstOrNull { accountSwapCurrency ->
                    accountSwapCurrency.cryptoCurrencyStatus.currency.id.value == id
                }
            }
            accountCryptoCurrencyStatus?.cryptoCurrencyStatus to accountCryptoCurrencyStatus?.account
        } else {
            if (isOrderReversed) {
                tokens.fromGroup
            } else {
                tokens.toGroup
            }.available.firstOrNull { swapAvailability ->
                swapAvailability.currencyStatus.currency.id.value == id
            }?.currencyStatus to null
        }
    }

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    private fun subscribeToCoinBalanceUpdates(
        userWalletId: UserWalletId,
        coin: CryptoCurrency.Coin,
        isFromCurrency: Boolean,
    ) {
        Timber.d("Subscribe to ${coin.id} balance updates")

        if (accountsFeatureToggles.isFeatureEnabled) {
            getAccountCurrencyStatusUseCase(
                userWalletId = userWalletId,
                currency = coin,
            ).distinctUntilChanged { old, new -> old.status.value.amount == new.status.value.amount } // Check only balance changes
                .onEach { (account, currencyStatus) ->
                    Timber.d("${coin.id} balance is ${currencyStatus.value.amount ?: "null"}")

                    if (isFromCurrency) {
                        dataState = dataState.copy(
                            feePaidCryptoCurrency = getFeePaidCryptoCurrencyStatusSyncUseCase(
                                userWalletId = userWalletId,
                                cryptoCurrencyStatus = currencyStatus,
                            ).getOrNull() ?: currencyStatus,
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
        } else {
            getSingleCryptoCurrencyStatusUseCase.invokeMultiWallet(
                userWalletId = userWalletId,
                currencyId = coin.id,
                isSingleWalletWithTokens = false,
            ).mapNotNull { either -> (either as? Either.Right)?.value }
                .distinctUntilChanged { old, new -> old.value.amount == new.value.amount } // Check only balance changes
                .onEach { status ->
                    Timber.d("${coin.id} balance is ${status.value.amount ?: "null"}")

                    if (isFromCurrency) {
                        dataState = dataState.copy(
                            feePaidCryptoCurrency = getFeePaidCryptoCurrencyStatusSyncUseCase(
                                userWalletId = userWalletId,
                                cryptoCurrencyStatus = status,
                            ).getOrNull() ?: status,
                        )
                    }

                    uiState = when {
                        isFromCurrency && status.currency.id == dataState.fromCryptoCurrency?.currency?.id -> {
                            dataState = dataState.copy(fromCryptoCurrency = status)
                            stateBuilder.updateSendCurrencyBalance(uiState, status)
                        }
                        !isFromCurrency && status.currency.id == dataState.toCryptoCurrency?.currency?.id -> {
                            dataState = dataState.copy(toCryptoCurrency = status)
                            stateBuilder.updateReceiveCurrencyBalance(uiState, status)
                        }
                        else -> {
                            uiState
                        }
                    }
                    startLoadingQuotesFromLastState(isSilent = true)
                }
        }.flowOn(dispatchers.main)
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
                isOrderReversed = !isOrderReversed
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
                analyticsEventHandler.send(SwapEvents.ButtonSwipeClicked())
            },
            onBackClicked = {
                val bottomSheet = uiState.bottomSheetConfig
                if (bottomSheet != null && bottomSheet.isShown) {
                    uiState = stateBuilder.dismissBottomSheet(uiState)
                } else {
                    if (swapRouter.currentScreen == SwapNavScreen.SelectToken) {
                        analyticsEventHandler.send(SwapEvents.ChooseTokenScreenResult(isTokenChosen = false))
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
                sendGivePermissionClickedEvent()
                uiState = stateBuilder.showPermissionBottomSheet(uiState) {
                    startLoadingQuotesFromLastState(isSilent = true)
                    analyticsEventHandler.send(SwapEvents.ButtonPermissionCancelClicked())
                    uiState = stateBuilder.dismissBottomSheet(uiState)
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
                    isReverseSwapPossible = isReverseSwapPossible(),
                    onDismiss = { uiState = stateBuilder.clearAlert(uiState) },
                )
            },
            onLinkClick = urlOpener::openUrl,
            onSelectTokenClick = {
                swapRouter.openScreen(SwapNavScreen.SelectToken)
                sendSelectTokenScreenOpenedEvent()
            },
            onSuccess = {
                swapRouter.openScreen(SwapNavScreen.Success)
            },
            onStoriesClose = { watchCount ->
                analyticsEventHandler.send(
                    StoriesEvents.SwapStories(
                        source = params.screenSource,
                        watchCount = watchCount.toString(),
                    ),
                )
                modelScope.launch { shouldShowStoriesUseCase.neverToShow(StoryContentIds.STORY_FIRST_TIME_SWAP.id) }
                swapRouter.openScreen(SwapNavScreen.Main)
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

        return if (accountsFeatureToggles.isFeatureEnabled) {
            groupToFind.accountCurrencyList.firstNotNullOfOrNull { (_, currencyList) ->
                currencyList.find { accountSwapCurrency ->
                    idToFind == accountSwapCurrency.cryptoCurrencyStatus.currency.id.value &&
                        accountSwapCurrency.isAvailable
                }
            }?.providers
        } else {
            groupToFind.available.find { swapAvailability ->
                idToFind == swapAvailability.currencyStatus.currency.id.value
            }?.providers
        }
            ?.filterForTangemPayWithdrawal()
            .orEmpty()
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

        val currenciesGroup = if (isOrderReversed) {
            dataState.tokensDataState?.toGroup
        } else {
            dataState.tokensDataState?.fromGroup
        } ?: return false

        val chosen = if (isOrderReversed) from else to

        return if (accountsFeatureToggles.isFeatureEnabled) {
            currenciesGroup.accountCurrencyList.flatMap { accountSwapAvailability ->
                accountSwapAvailability.currencyList.map { accountSwapCurrency ->
                    accountSwapCurrency.cryptoCurrencyStatus
                }
            }
        } else {
            currenciesGroup.available.map { swapAvailability -> swapAvailability.currencyStatus }
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
        uiState = stateBuilder.createErrorTransactionAlert(
            uiState = uiState,
            error = SwapTransactionState.Error.TangemPayWithdrawalError(txId.orEmpty()),
            onDismiss = { uiState = stateBuilder.clearAlert(uiState) },
            onSupportClick = ::onTangemPaySupportClick,
            isReverseSwapPossible = isReverseSwapPossible(),
        )
    }

    private fun onTangemPaySupportClick(txId: String?) {
        modelScope.launch {
            val metaInfo = getWalletMetaInfoUseCase(userWallet.walletId).getOrNull() ?: return@launch
            val email = FeedbackEmailType.Visa.Withdrawal(
                walletMetaInfo = metaInfo,
                providerName = dataState.selectedProvider?.name.orEmpty(),
                txId = txId.orEmpty(),
            )
            sendFeedbackEmailUseCase(email)
        }
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

            sendFeedbackEmailUseCase(email)
        }
    }

    private fun addToPortfolioItem(item: MarketsListItemUM) {
        modelScope.launch {
            val tokenInfo = getTokenMarketInfoUseCase(
                selectedAppCurrencyFlow.value,
                item.id,
                item.currencySymbol,
            ).getOrNull() ?: return@launch

            val converter = TokenMarketInfoToParamsConverter()
            val param = converter.convert(tokenInfo)
            val hasOnlyHotWallets = getUserWalletsUseCase.invokeSync().all { it is UserWallet.Hot }

            val networks = tokenInfo.networks?.filter { network ->
                BlockchainUtils.isSupportedNetworkId(
                    blockchainId = network.networkId,
                    excludedBlockchains = excludedBlockchains,
                    hotExcludedBlockchains = hotWalletExcludedBlockchains,
                    hasOnlyHotWallets = hasOnlyHotWallets,
                )
            }.orEmpty()

            addToPortfolioManager = addToPortfolioManagerFactory
                .create(
                    scope = modelScope,
                    token = param,
                    analyticsParams = null,
                ).apply {
                    setTokenNetworks(networks)
                }

            addToPortfolioManager?.state
                ?.firstOrNull { it is AddToPortfolioManager.State.AvailableToAdd }
                ?.run { bottomSheetNavigation.activate(AddToPortfolioRoute) }
        }.saveIn(addToPortfolioJobHolder)
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
            getSingleCryptoCurrencyStatusUseCase.invokeMultiWalletSync(
                userWalletId = userWalletId,
                cryptoCurrencyId = initialCurrencyFrom.id,
            ).getOrNull()
        }
    }

    private fun getSelectedFeeState(): TxFeeSealedState {
        if (!sendFeatureToggles.isGaslessTransactionsEnabled) {
            return TxFeeSealedState.Legacy(
                txFeeState = TxFeeState.Empty,
                selectedFee = dataState.selectedFee?.feeType ?: FeeType.NORMAL,
            )
        }

        val feeStateUM = feeSelectorRepository.state.value as? FeeSelectorUM.Content
            ?: return TxFeeSealedState.Legacy(
                txFeeState = TxFeeState.Empty,
                selectedFee = dataState.selectedFee?.feeType ?: FeeType.NORMAL,
            )

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
        if (!sendFeatureToggles.isGaslessTransactionsEnabled) {
            return dataState.selectedFee
        }

        val feeStateUM = feeSelectorRepository.state.value as? FeeSelectorUM.Content ?: return null
        val transactionFeeExtended = feeStateUM.feeExtraInfo.transactionFeeExtended

        return TxFee.FeeComponent(
            transactionFeeResult = transactionFeeExtended?.let { TransactionFeeResult.from(it) }
                ?: TransactionFeeResult.from(feeStateUM.fees),
            fee = feeStateUM.selectedFeeItem.fee,
            selectedToken = feeStateUM.feeExtraInfo.feeCryptoCurrencyStatus,
        )
    }

    inner class FeeSelectorRepository : SwapFeeSelectorBlockComponent.ModelRepositoryExtended {

        override val state = MutableStateFlow<FeeSelectorUM>(FeeSelectorUM.Loading)

        override suspend fun loadFeeExtended(
            selectedToken: CryptoCurrencyStatus?,
        ): Either<GetFeeError, TransactionFeeExtended> {
            val sendCardData =
                uiState.sendCardData as? SwapCardState.SwapCardData ?: return Either.Left(GetFeeError.UnknownError)
            val receiveCardData =
                uiState.receiveCardData as? SwapCardState.SwapCardData ?: return Either.Left(GetFeeError.UnknownError)
            val fromToken = sendCardData.token ?: return Either.Left(GetFeeError.UnknownError)
            val toToken = receiveCardData.token ?: return Either.Left(GetFeeError.UnknownError)
            val selectedProvider = dataState.selectedProvider ?: return Either.Left(GetFeeError.UnknownError)

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
            if (isPermissionNotificationShown()) {
                state.value = FeeSelectorUM.Error(GetFeeError.UnknownError, isHidden = true)
                return
            }

            state.value = newState

            // If fee currency is same as from currency, we need to reload quotes to update fee info
            if (newState is FeeSelectorUM.Content &&
                dataState.fromCryptoCurrency?.currency?.id == newState.feeExtraInfo.feeCryptoCurrencyStatus.currency.id
            ) {
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
            val sendCardData =
                uiState.sendCardData as? SwapCardState.SwapCardData ?: return Either.Left(GetFeeError.UnknownError)
            val receiveCardData =
                uiState.receiveCardData as? SwapCardState.SwapCardData ?: return Either.Left(GetFeeError.UnknownError)
            val fromToken = sendCardData.token ?: return Either.Left(GetFeeError.UnknownError)
            val toToken = receiveCardData.token ?: return Either.Left(GetFeeError.UnknownError)
            val selectedProvider = dataState.selectedProvider ?: return Either.Left(GetFeeError.UnknownError)

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
            )
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
        const val DEBOUNCE_SEARCH_DELAY = 500L
        const val UPDATE_BALANCE_DELAY_MILLIS = 11000L
        const val CHANGELLY_PROVIDER_ID = "changelly"
    }
}