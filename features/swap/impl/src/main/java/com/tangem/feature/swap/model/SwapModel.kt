package com.tangem.feature.swap.model

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import arrow.core.Either
import arrow.core.flatMap
import arrow.core.getOrElse
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.ui.bottomsheet.permission.state.ApproveType
import com.tangem.core.analytics.api.AnalyticsErrorHandler
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.AnalyticsParam.ScreensSources
import com.tangem.core.analytics.models.Basic
import com.tangem.core.analytics.models.event.SwapAnalyticsEvent
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.share.ShareManager
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.core.ui.message.dialog.Dialogs
import com.tangem.core.ui.utils.parseBigDecimal
import com.tangem.core.ui.utils.parseBigDecimalOrNull
import com.tangem.core.ui.utils.parseToBigDecimal
import com.tangem.datasource.local.appsflyer.AppsFlyerStore
import com.tangem.domain.account.status.usecase.GetAccountCurrencyStatusUseCase
import com.tangem.domain.account.status.usecase.GetFeePaidCryptoCurrencyStatusSyncUseCase
import com.tangem.domain.account.status.usecase.IsAccountsModeEnabledUseCase
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.card.IsWalletBackupProblematicUseCase
import com.tangem.domain.express.models.ExpressOperationType
import com.tangem.domain.express.models.ProviderFilterType
import com.tangem.domain.feedback.GetWalletMetaInfoUseCase
import com.tangem.domain.feedback.SaveBlockchainErrorUseCase
import com.tangem.domain.feedback.SendBackupProblemEmailUseCase
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
import com.tangem.domain.models.wallet.isHotWallet
import com.tangem.domain.pay.WithdrawalResult
import com.tangem.domain.pay.usecase.GetPaymentAccountCryptoCurrencyStatusUseCase
import com.tangem.domain.settings.usercountry.GetUserCountryUseCase
import com.tangem.domain.settings.usercountry.models.UserCountry
import com.tangem.domain.settings.usercountry.models.needApplyFCARestrictions
import com.tangem.domain.stories.ShouldShowStoriesUseCase
import com.tangem.domain.stories.models.StoryContentIds
import com.tangem.domain.swap.models.PredefinedPercentAmount
import com.tangem.domain.swap.models.SwapCurrencyStatus
import com.tangem.domain.swap.usecase.CalculateAmountUseCase
import com.tangem.domain.tangempay.GetTangemPayCustomerIdUseCase
import com.tangem.domain.tangempay.TangemPayWithdrawWithSwapUseCase
import com.tangem.domain.tokens.GetMinimumTransactionAmountSyncUseCase
import com.tangem.domain.tokens.UpdateDelayedNetworkStatusUseCase
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.transaction.usecase.gasless.IsGaslessFeeSupportedForNetwork
import com.tangem.domain.txhistory.usecase.GetExplorerTransactionUrlUseCase
import com.tangem.feature.swap.analytics.SwapEvents
import com.tangem.feature.swap.analytics.SwapQuotePerformanceTracker
import com.tangem.feature.swap.component.SwapFeeSelectorBlockComponent
import com.tangem.feature.swap.converters.SwapProviderResolver
import com.tangem.feature.swap.converters.SwapTransactionErrorStateConverter
import com.tangem.feature.swap.domain.AllowPermissionsHandler
import com.tangem.feature.swap.domain.GetSwapUiModeUseCase
import com.tangem.feature.swap.domain.SetSwapUiModeUseCase
import com.tangem.feature.swap.domain.SwapInteractor
import com.tangem.feature.swap.domain.fee.TransactionFeeResult
import com.tangem.feature.swap.domain.models.ExpressDataError
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.domain.*
import com.tangem.feature.swap.domain.models.ui.*
import com.tangem.feature.swap.domain.transfer.SwapTransferInteractor
import com.tangem.feature.swap.models.*
import com.tangem.feature.swap.models.states.SwapNotificationUM
import com.tangem.feature.swap.router.SwapRoute
import com.tangem.feature.swap.ui.StateBuilder
import com.tangem.feature.swap.ui.transfer.SwapTransferStateBuilder
import com.tangem.feature.swap.utils.formatToUIRepresentation
import com.tangem.feature.swap.utils.getContractAddress
import com.tangem.features.approval.api.GiveApprovalComponent
import com.tangem.features.approval.api.GiveApprovalEntryComponent
import com.tangem.features.approval.api.SelectApprovalTypeComponent
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenAnalyticsPayload
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenBridge
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenResult
import com.tangem.features.send.api.entity.FeeItem
import com.tangem.features.send.api.entity.FeeSelectorUM
import com.tangem.features.send.api.subcomponents.feeSelector.FeeSelectorReloadTrigger
import com.tangem.features.send.impl.R
import com.tangem.features.swap.SwapComponent
import com.tangem.features.swap.SwapFeatureToggles
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.*
import com.tangem.utils.extensions.filterIf
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale
import javax.inject.Inject

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
    private val isWalletBackupProblematicUseCase: IsWalletBackupProblematicUseCase,
    private val sendBackupProblemEmailUseCase: SendBackupProblemEmailUseCase,
    private val swapInteractor: SwapInteractor,
    private val swapTransferInteractor: SwapTransferInteractor,
    private val swapTransferStateBuilder: SwapTransferStateBuilder,
    private val urlOpener: UrlOpener,
    private val shareManager: ShareManager,
    private val getAccountCurrencyStatusUseCase: GetAccountCurrencyStatusUseCase,
    private val getPaymentAccountCryptoCurrencyStatusUseCase: GetPaymentAccountCryptoCurrencyStatusUseCase,
    private val tangemPayWithdrawWithSwapUseCase: TangemPayWithdrawWithSwapUseCase,
    private val isGaslessFeeSupportedForNetwork: IsGaslessFeeSupportedForNetwork,
    private val feeSelectorReloadTrigger: FeeSelectorReloadTrigger,
    private val getTangemPayCustomerIdUseCase: GetTangemPayCustomerIdUseCase,
    private val appsFlyerStore: AppsFlyerStore,
    private val messageSender: UiMessageSender,
    private val initialCurrenciesResolver: InitialCurrenciesResolver,
    private val allowPermissionsHandler: AllowPermissionsHandler,
    private val swapFeatureToggles: SwapFeatureToggles,
    private val getSwapUiModeUseCase: GetSwapUiModeUseCase,
    private val setSwapUiModeUseCase: SetSwapUiModeUseCase,
    private val calculateAmountUseCase: CalculateAmountUseCase,
) : Model() {

    private val params = paramsContainer.require<SwapComponent.Params>()

    private val initialCryptoCurrency = params.fromCryptoCurrency
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

    private val actions = createUiActions()
    private val stateBuilder = StateBuilder(
        actions = actions,
        isBalanceHiddenProvider = Provider { isBalanceHidden },
        appCurrencyProvider = Provider(selectedAppCurrencyFlow::value),
        isAccountsModeProvider = Provider { isAccountsMode },
        isGaslessFeeSupportedForNetwork = isGaslessFeeSupportedForNetwork,
        swapFeatureToggles = swapFeatureToggles,
        appRouter = appRouter,
    )

    private val amountDebouncer = Debouncer()
    private val transferModeDebouncer = Debouncer()
    private val singleTaskScheduler = SingleTaskScheduler<Map<SwapProvider, SwapState>>()
    private val performanceTracker = SwapQuotePerformanceTracker()

    val dataStateStateFlow = MutableStateFlow(SwapProcessDataState())
    var dataState
        get() = dataStateStateFlow.value
        set(value) {
            dataStateStateFlow.value = value
        }

    var uiState: SwapStateHolder by mutableStateOf(stateBuilder.createInitialLoadingState())
        internal set

    val feeSelectorRepository = FeeSelectorRepository()

    private val lastAmount = mutableStateOf(INITIAL_AMOUNT)
    private val lastReducedBalanceBy = mutableStateOf(BigDecimal.ZERO)

    /** Whether the user is currently entering a fiat amount in the "from" card (vs crypto). */
    private val isFiatInput = mutableStateOf(false)
    private var userCountry: UserCountry? = null

    private val fromTokenBalanceJobHolder = JobHolder()
    private val toTokenBalanceJobHolder = JobHolder()
    private val swapPairsJobHolder = JobHolder()

    private var isAmountChangedByUser: Boolean = false
    private var lastPermissionNotificationTokens: Pair<String, String>? = null

    private var preselectedFromCurrency: CryptoCurrency? = null
    private var preselectedToCurrency: CryptoCurrency? = null

    val isPermissionNotNeeded: Boolean
        get() {
            val permissionState = dataState.getCurrentLoadedSwapState()?.permissionState
            return permissionState == PermissionDataState.Empty ||
                swapFeatureToggles.isSwapIntegratedApproveEnabled &&
                permissionState is PermissionDataState.PermissionSettings
        }

    val approvalSlotNavigation = SlotNavigation<GiveApprovalEntryComponent.Mode>()

    internal val approvalFullCallback = object : GiveApprovalComponent.Callback {
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

    internal val approvalSelectorCallback = object : SelectApprovalTypeComponent.Callback {
        override fun onApproveTypeSelected(spenderAddress: String, approveType: ApproveType) {
            val (swapState, permission) = dataState.lastLoadedSwapStates.firstNotNullOfOrNull { (provider, state) ->
                if (state !is SwapState.QuotesLoadedState) return@firstNotNullOfOrNull null
                val permissionState = state.permissionState

                if (permissionState is PermissionDataState.PermissionSettings &&
                    permissionState.spenderAddress == spenderAddress
                ) {
                    state to permissionState
                } else {
                    null
                }
            } ?: return

            if (permission.type == approveType) {
                approvalSlotNavigation.dismiss()
                return
            }
            dataState = dataState.copy(
                lastLoadedSwapStates = dataState.lastLoadedSwapStates.toMutableMap().apply {
                    put(
                        swapState.swapProvider,
                        swapState.copy(permissionState = permission.copy(type = approveType)),
                    )
                },
            )
            approvalSlotNavigation.dismiss()
            modelScope.launch {
                feeSelectorRepository.state.value = FeeSelectorUM.Loading
                feeSelectorReloadTrigger.triggerLoadingState()
                feeSelectorReloadTrigger.triggerUpdate()
            }
        }

        override fun onCancelClick() {
            approvalSlotNavigation.dismiss()
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

        userCountry = getUserCountryUseCase.invokeSync().getOrNull() ?: UserCountry.Other(Locale.getDefault().country)

        initTokens()

        getBalanceHidingSettingsUseCase().onEach { settings ->
            isBalanceHidden = settings.isBalanceHidden
            uiState = stateBuilder.updateBalanceHiddenState(uiState, isBalanceHidden)
        }.launchIn(modelScope)

        modelScope.launch {
            val swapUIMode = getSwapUiModeUseCase()
            uiState = uiState.copy(swapUIMode = swapUIMode)
            analyticsEventHandler.send(SwapEvents.SwapType(swapUIMode))
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

        chooseFromTokenBridge.onCurrencyChosen.receiveAsFlow().onEach { result ->
            onTokenSelect(result = result, isFromDirection = true)
            sendAnalytics(result = result, direction = "From")
        }.launchIn(modelScope)

        chooseFromTokenBridge.onClose.receiveAsFlow().onEach {
            analyticsEventHandler.send(SwapEvents.ChooseTokenScreenResult(isTokenChosen = false))
            router.pop()
        }.launchIn(modelScope)

        chooseToTokenBridge.onCurrencyChosen.receiveAsFlow().onEach { result ->
            onTokenSelect(result, isFromDirection = false)
            sendAnalytics(result = result, direction = "To")
        }.launchIn(modelScope)

        chooseToTokenBridge.onClose.receiveAsFlow().onEach {
            analyticsEventHandler.send(SwapEvents.ChooseTokenScreenResult(isTokenChosen = false))
            router.pop()
        }.launchIn(modelScope)
    }

    private fun initTokens() {
        modelScope.launch(dispatchers.default) {
            isAccountsMode = isAccountsModeEnabledUseCase.invokeSync()

            val (fromSwapCurrencyStatus, toSwapCurrencyStatus) = initialCurrenciesResolver(
                userWalletId = params.userWalletId,
                initialCryptoCurrency = initialCryptoCurrency,
                swapCurrencyPosition = params.fromCurrencyPosition,
                isPaymentAccount = params.tangemPayInput != null,
                initialToCryptoCurrency = params.toCryptoCurrency,
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

        if (!isFromDirection && isWalletBackupProblematicUseCase(selectedUserWallet)) {
            router.pop()
            showBackupErrorAlert(selectedUserWallet.walletId)
            return
        }

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
            isFiatInput.value = false
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
                isTransferMode = swapTransferInteractor.shouldTransferInsteadOfSwap(
                    fromSwapCurrency = fromSwapCurrencyStatus?.currency,
                    toSwapCurrency = toSwapCurrencyStatus?.currency,
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

    @Suppress("LongMethod")
    private fun onChangeCardsClicked() {
        modelScope.launch {
            singleTaskScheduler.cancelTask()

            val newFromSwapCurrencyStatus = dataState.toSwapCurrencyStatus
            val newToSwapCurrencyStatus = dataState.fromSwapCurrencyStatus

            isAmountChangedByUser = true

            lastAmount.value = INITIAL_AMOUNT
            isFiatInput.value = false
            lastReducedBalanceBy.value = BigDecimal.ZERO

            dataState = SwapProcessDataState(
                fromSwapCurrencyStatus = newFromSwapCurrencyStatus,
                toSwapCurrencyStatus = newToSwapCurrencyStatus,
                pairs = dataState.pairs,
                selectedPairProviders = if (newFromSwapCurrencyStatus == null || newToSwapCurrencyStatus == null) {
                    emptyList()
                } else {
                    swapInteractor.findProvidersForPairWithCheck(
                        fromSwapCurrencyStatus = newFromSwapCurrencyStatus,
                        toSwapCurrencyStatus = newToSwapCurrencyStatus,
                        pairs = dataState.pairs,
                    )
                },
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
                    isTransferMode = swapTransferInteractor.shouldTransferInsteadOfSwap(
                        fromSwapCurrency = newFromSwapCurrencyStatus?.currency,
                        toSwapCurrency = newToSwapCurrencyStatus?.currency,
                    ),
                ),
                fromSwapCurrencyStatus = newFromSwapCurrencyStatus,
                toSwapCurrencyStatus = newToSwapCurrencyStatus,
                shouldResetAmount = true,
            )

            if (newFromSwapCurrencyStatus != null && newToSwapCurrencyStatus != null) {
                updateFeePaidCryptoCurrencyFor(newFromSwapCurrencyStatus)
                val isUpdatedToTransferMode = isUpdatedToTransferMode(
                    fromSwapCurrencyStatus = newFromSwapCurrencyStatus,
                    toSwapCurrencyStatus = newToSwapCurrencyStatus,
                    fromTokenAmount = lastAmount.value,
                )
                if (isUpdatedToTransferMode) return@launch
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
        val isUpdatedToTransferMode = isUpdatedToTransferMode(
            fromSwapCurrencyStatus = fromSwapCurrencyStatus,
            toSwapCurrencyStatus = toSwapCurrencyStatus,
            fromTokenAmount = lastAmount.value,
        )
        if (isUpdatedToTransferMode) {
            analyticsEventHandler.send(
                event = SwapEvents.TransferModeSwitched(
                    fromCurrency = fromSwapCurrencyStatus.currency,
                    toCurrency = toSwapCurrencyStatus.currency,
                ),
            )
            return
        }
        dataState = dataState.copy(currentTransferState = null)
        modelScope.launch {
            uiState = stateBuilder.createInitialLoadingState(
                uiStateHolder = uiState,
                fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                toSwapCurrencyStatus = toSwapCurrencyStatus,
            )
            swapInteractor.getPair(
                fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                toSwapCurrencyStatus = toSwapCurrencyStatus,
                filterProviderTypes = ExchangeProviderType.getSwapProviderTypes(),
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
                ifRight = { pairsRaw ->
                    val pairs = pairsRaw.filterTangemPayProviders(
                        fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                        toSwapCurrencyStatus = toSwapCurrencyStatus,
                    )
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
                        updateCurrenciesStateAndStartLoadingQuotes(
                            fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                            toSwapCurrencyStatus = toSwapCurrencyStatus,
                            pairs = pairs,
                            providerList = providerList,
                        )
                    }
                },
            )
        }.saveIn(swapPairsJobHolder)
    }

    private fun updateCurrenciesStateAndStartLoadingQuotes(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
        pairs: List<SwapPairLeast>,
        providerList: List<SwapProvider>,
    ) {
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
                isTransferMode = swapTransferInteractor.shouldTransferInsteadOfSwap(
                    fromSwapCurrency = fromSwapCurrencyStatus.currency,
                    toSwapCurrency = toSwapCurrencyStatus.currency,
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

    private fun isUpdatedToTransferMode(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
        fromTokenAmount: String,
        forceUpdate: Boolean = true,
    ): Boolean {
        val shouldTransferInsteadOfSwap = swapTransferInteractor.shouldTransferInsteadOfSwap(
            fromSwapCurrencyStatus.currency,
            toSwapCurrencyStatus.currency,
        )
        if (shouldTransferInsteadOfSwap) {
            transferModeDebouncer.debounce(
                coroutineScope = modelScope,
                waitMs = DEBOUNCE_AMOUNT_DELAY,
                forceUpdate = forceUpdate,
            ) {
                singleTaskScheduler.destroyTask()
                swapPairsJobHolder.cancel()
                updateTransferUIState(fromSwapCurrencyStatus, toSwapCurrencyStatus, fromTokenAmount)
            }
        }
        return shouldTransferInsteadOfSwap
    }

    private suspend fun updateTransferUIState(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
        fromTokenAmount: String,
    ) {
        val feePaidCryptoCurrency = dataState.feePaidCryptoCurrency
        val selectedFee = getSelectedSwapFee()?.fee
        val swapState = swapTransferInteractor.updateTransfer(
            fromSwapCurrencyStatus = fromSwapCurrencyStatus,
            toSwapCurrencyStatus = toSwapCurrencyStatus,
            fromTokenAmount = fromTokenAmount,
            feePaidCurrencyStatus = feePaidCryptoCurrency,
            fee = selectedFee,
        )
        uiState = swapTransferStateBuilder.updateTransferTitle(uiState)
        when (swapState) {
            is SwapState.EmptyAmountState -> setupEmptyAmountUiState(swapState, fromSwapCurrencyStatus)
            is SwapState.Transfer -> {
                dataState = dataState.copy(
                    amount = fromTokenAmount,
                    currentTransferState = swapState,
                )
                uiState = swapTransferStateBuilder.createTransferState(
                    actions = actions,
                    transferState = swapState,
                    uiStateHolder = uiState,
                    feePaidCryptoCurrencyStatus = feePaidCryptoCurrency,
                    fee = selectedFee,
                )
                when {
                    uiState.successState != null -> Unit
                    isTangemPayWithdrawal() -> refreshTransferUIStateIfNeeded()
                    else -> {
                        feeSelectorRepository.state.value = FeeSelectorUM.Loading
                        feeSelectorReloadTrigger.triggerUpdate()
                    }
                }
            }
            is SwapState.QuotesLoadedState, is SwapState.SwapError -> Unit
        }
    }

    private fun refreshTransferUIStateIfNeeded(
        feePaidCryptoCurrencyStatus: CryptoCurrencyStatus? = null,
        fee: Fee? = null,
    ) {
        val from = dataState.fromSwapCurrencyStatus ?: return
        val to = dataState.toSwapCurrencyStatus ?: return
        if (!swapTransferInteractor.shouldTransferInsteadOfSwap(from.currency, to.currency)) return
        val currentTransferState = dataState.currentTransferState ?: return
        val amount = dataState.amount ?: return
        modelScope.launch {
            // The cached currentTransferState may have been built when the fee selector
            // had not loaded yet (fee=null). Recompute it with the freshly-loaded fee so
            // isFeeCoverage and sendingAmount reflect the actual fee, otherwise the fee
            // coverage notification stays hidden on first Max click.
            val refreshed = swapTransferInteractor.updateTransfer(
                fromSwapCurrencyStatus = from,
                toSwapCurrencyStatus = to,
                fromTokenAmount = amount,
                feePaidCurrencyStatus = feePaidCryptoCurrencyStatus,
                fee = fee,
            ) as? SwapState.Transfer ?: currentTransferState
            dataState = dataState.copy(
                currentTransferState = refreshed,
                feePaidCryptoCurrency = feePaidCryptoCurrencyStatus ?: dataState.feePaidCryptoCurrency,
            )
            uiState = swapTransferStateBuilder.updateTransferButtonEnableState(
                dataState = dataState,
                transferState = refreshed,
                actions = actions,
                uiStateHolder = uiState,
                feePaidCryptoCurrencyStatus = feePaidCryptoCurrencyStatus,
                fee = fee,
                isTangemPayWithdrawal = isTangemPayWithdrawal(),
            )
        }
    }

    private fun retrySwapPairs(fromSwapCurrencyStatus: SwapCurrencyStatus, toSwapCurrencyStatus: SwapCurrencyStatus) {
        if (swapPairsJobHolder.isActive) return
        initSwapPairs(fromSwapCurrencyStatus, toSwapCurrencyStatus)
    }

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
        dataState = dataState.copy(
            amount = amount,
            reduceBalanceBy = reduceBalanceBy,
        )
        singleTaskScheduler.cancelTask()
        if (amount.isBlank()) {
            uiState = stateBuilder.createQuotesEmptyAmountState(
                uiStateHolder = uiState,
                fromSwapCurrencyStatus = fromSwapCurrencyStatus,
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
            return
        }
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
            val isUpdatedToTransferMode = isUpdatedToTransferMode(
                fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                toSwapCurrencyStatus = toSwapCurrencyStatus,
                fromTokenAmount = lastAmount.value,
            )
            if (isUpdatedToTransferMode) return
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

    @Suppress("LongMethod")
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
                runCatching(dispatchers.default) {
                    swapInteractor.findBestQuote(
                        fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                        toSwapCurrencyStatus = toSwapCurrencyStatus,
                        providers = toProvidersList,
                        amountToSwap = amount,
                        reduceBalanceBy = reduceBalanceBy,
                    )
                }
            },
            onSuccess = { providersState ->
                modelScope.launch {
                    performanceTracker.onLoadingFinished(
                        hasError = providersState.values.none { it is SwapState.QuotesLoadedState },
                    )

                    if (providersState.isNotEmpty()) {
                        val (provider, state) = updateLoadedQuotes(providersState)

                        if (feeSelectorRepository.state.value is FeeSelectorUM.Content &&
                            state is SwapState.QuotesLoadedState
                        ) {
                            val swapFee = getSelectedSwapFee() ?: return@launch
                            val patchedState = withContext(dispatchers.default) {
                                swapInteractor.applySwapFee(
                                    state = state,
                                    fee = swapFee,
                                    lastReducedBalanceBy = lastReducedBalanceBy.value,
                                )
                            }
                            val patchedStates = dataState.lastLoadedSwapStates.toMutableMap().apply {
                                put(provider, patchedState)
                            }
                            dataState = dataState.copy(lastLoadedSwapStates = patchedStates)
                            setupLoadedState(
                                provider = provider,
                                state = patchedState,
                                fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                                toSwapCurrencyStatus = toSwapCurrencyStatus,
                            )
                        } else {
                            setupLoadedState(
                                provider = provider,
                                state = state,
                                fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                                toSwapCurrencyStatus = toSwapCurrencyStatus,
                            )
                        }

                        val successStates = dataState.getLastLoadedSuccessStates()
                        val pricesLowerBest = getPricesLowerBest(provider.providerId, successStates)
                        uiState = stateBuilder.updateProvidersBottomSheetContent(
                            uiState = uiState,
                            pricesLowerBest = pricesLowerBest,
                            tokenSwapInfoForProviders = successStates.entries
                                .associate { it.key.providerId to it.value.toTokenInfo },
                        )
                        if (shouldUpdateFeeBlock && isPermissionNotNeeded) {
                            modelScope.launch { feeSelectorReloadTrigger.triggerUpdate() }
                        } else {
                            shouldUpdateFeeBlock = true
                        }
                    } else {
                        feeSelectorRepository.state.value =
                            FeeSelectorUM.Error(GetFeeError.UnknownError, isHidden = true)
                        TangemLogger.e("Accidentally empty quotes list")
                    }
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
            is SwapState.Transfer -> Unit
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
        val loadedStates = dataState.getLastLoadedSuccessStates()
        val additionalBadge = SwapProviderResolver.resolveBadge(
            provider = provider,
            needApplyFCARestrictions = userCountry.needApplyFCARestrictions(),
            states = loadedStates,
            state = state,
            isSwapBestDexRateEnabled = swapFeatureToggles.isSwapBestDexRateEnabled,
        )
        uiState = stateBuilder.createQuotesLoadedState(
            uiStateHolder = uiState,
            quoteModel = state,
            feeCryptoCurrencyStatus = dataState.feePaidCryptoCurrency,
            swapProvider = provider,
            additionalBadge = additionalBadge,
            swapFee = getSelectedSwapFee(),
            feeError = feeSelectorRepository.state.value as? FeeSelectorUM.Error,
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
        dataState = dataState.copy(amount = "0")
    }

    private fun setupErrorUiState(provider: SwapProvider, state: SwapState.SwapError) {
        singleTaskScheduler.cancelTask()
        val loadedStates = dataState.getLastLoadedSuccessStates()
        val additionalBadge = SwapProviderResolver.resolveBadge(
            provider = provider,
            needApplyFCARestrictions = userCountry.needApplyFCARestrictions(),
            states = loadedStates,
            state = state,
            isSwapBestDexRateEnabled = swapFeatureToggles.isSwapBestDexRateEnabled,
        )
        uiState = stateBuilder.createQuotesErrorState(
            uiStateHolder = uiState,
            swapProvider = provider,
            fromToken = state.fromTokenInfo,
            toSwapCurrencyStatus = dataState.toSwapCurrencyStatus,
            expressDataError = state.error,
            balanceStatus = state.balanceStatus,
            additionalBadge = additionalBadge,
            swapFee = getSelectedSwapFee(),
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
            val bestQuotesProvider = SwapProviderResolver.findBest(
                states = successLoadedData,
                isSwapBestDexRateEnabled = swapFeatureToggles.isSwapBestDexRateEnabled,
            )
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
        val swapFee = getSelectedSwapFee()
        val isTangemPayWithdrawal = isTangemPayWithdrawal()

        if (swapFee == null && !isTangemPayWithdrawal) {
            TangemLogger.e("onSwapClick: fee is null and isTangemPayWithdrawal is $isTangemPayWithdrawal")
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
                    swapData = lastLoadedQuotesState.swapDataModel,
                    amountToSwap = requireNotNull(dataState.amount),
                    balanceStatus = lastLoadedQuotesState.preparedSwapConfigState.balanceStatus,
                    fee = swapFee,
                    expressOperationType = ExpressOperationType.SWAP,
                    isTangemPayWithdrawal = isTangemPayWithdrawal,
                    integratedApproval = lastLoadedQuotesState.integratedApprovalData,
                )
            }.onSuccess { swapTransactionState ->
                when (swapTransactionState) {
                    is SwapTransactionState.TxSent -> {
                        TangemLogger.i("onSwapClick: onSuccess: txHash: $swapTransactionState", shouldSanitize = false)
                        if (swapFee == null) {
                            TangemLogger.e("onSwapClick: onSuccess: fee is null after swap")
                            showAlert(resourceReference(R.string.swapping_fee_estimation_error_text))
                            return@onSuccess
                        }
                        sendSuccessSwapEvent(
                            fromToken = fromSwapCurrencyStatus.currency,
                            feeBucket = swapFee.feeBucket,
                            feeCryptoCurrency = dataState.feePaidCryptoCurrency,
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
                            swapFee = swapFee,
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
                        sendSwapInProgressEvent()

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

    private fun onTransferClick() {
        val fromSwapCurrencyStatus = dataState.fromSwapCurrencyStatus
        val toSwapCurrencyStatus = dataState.toSwapCurrencyStatus
        analyticsEventHandler.send(
            event = SwapEvents.ButtonTransferClicked(
                fromCurrency = fromSwapCurrencyStatus?.currency,
                toCurrency = toSwapCurrencyStatus?.currency,
            ),
        )
        val fee = (feeSelectorRepository.state.value as? FeeSelectorUM.Content)?.selectedFeeItem?.fee
        if (fromSwapCurrencyStatus == null || toSwapCurrencyStatus == null) {
            TangemLogger.e("onTransferClick: missing currency status, aborting")
            showAlert()
            return
        }
        val transferState = dataState.currentTransferState ?: return
        uiState = swapTransferStateBuilder.createTransferInProgressState(uiState)
        modelScope.launch(dispatchers.main) {
            when {
                isTangemPayWithdrawal() -> withdrawTangemPay(
                    transferState = transferState,
                    toSwapCurrencyStatus = toSwapCurrencyStatus,
                )
                fee != null -> sendTransfer(
                    fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                    toSwapCurrencyStatus = toSwapCurrencyStatus,
                    transferState = transferState,
                    fee = fee,
                )
                else -> {
                    TangemLogger.e("onTransferClick: Illegal state, aborting")
                    showAlert()
                }
            }
        }
    }

    private suspend fun withdrawTangemPay(
        transferState: SwapState.Transfer,
        toSwapCurrencyStatus: SwapCurrencyStatus,
    ) {
        swapTransferInteractor.withdrawTangemPay(
            userWallet = transferState.userWallet,
            cryptoAmount = transferState.sendingAmount,
            toSwapCurrencyStatus = toSwapCurrencyStatus,
        )
            .onLeft { error ->
                TangemLogger.e(
                    messageString = "onTransferClick: withdrawTangemPay failed: ${error.getAnalyticsDescription()}",
                )
                startLoadingQuotesFromLastState()
                showAlert()
            }
            .onRight { result ->
                when (result) {
                    WithdrawalResult.Cancelled -> startLoadingQuotesFromLastState()
                    WithdrawalResult.Success -> updateTransferModeTangemPayState()
                }
            }
    }

    private fun updateTransferModeTangemPayState() {
        sendTransferInProgressEvent()
        uiState = swapTransferStateBuilder.createTangemPayWithdrawalSuccessState(
            uiState = uiState,
            dataState = dataState,
            fee = getSelectedSwapFee()?.fee,
            onExploreClick = {
                val txUrl = uiState.successState?.txUrl.orEmpty()
                if (txUrl.isNotEmpty()) {
                    urlOpener.openUrl(txUrl)
                }
            },
            onShareClick = {
                val txUrl = uiState.successState?.txUrl.orEmpty()
                if (txUrl.isNotEmpty()) {
                    shareManager.shareText(txUrl)
                }
            },
        )
        router.replaceAll(SwapRoute.Success)
    }

    private suspend fun sendTransfer(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
        transferState: SwapState.Transfer,
        fee: Fee,
    ) {
        swapTransferInteractor.sendTransfer(
            fromSwapCurrencyStatus = fromSwapCurrencyStatus,
            toSwapCurrencyStatus = toSwapCurrencyStatus,
            sendingAmount = transferState.sendingAmount,
            fee = fee,
            transactionFeeResult = requireNotNull(getSelectedSwapFee()?.transactionFeeResult) {
                "It should be not null at this stage"
            },
        ).fold(
            ifLeft = { error ->
                TangemLogger.e("onTransferClick: transfer failed: ${error.getAnalyticsDescription()}")
                startLoadingQuotesFromLastState()
                showAlert()
            },
            ifRight = { txHash ->
                val txUrl = getExplorerTransactionUrlUseCase(
                    txHash = txHash,
                    currency = fromSwapCurrencyStatus.currency,
                ).getOrElse {
                    TangemLogger.i("onTransferClick: tx hash explore not supported")
                    ""
                }
                updateWalletBalance()
                sendTransferInProgressEvent()
                uiState = swapTransferStateBuilder.createSuccessState(
                    uiState = uiState,
                    dataState = dataState,
                    txUrl = txUrl,
                    timestamp = System.currentTimeMillis(),
                    fee = getSelectedSwapFee()?.fee,
                    onExplorerClick = {
                        if (txUrl.isNotEmpty()) {
                            urlOpener.openUrl(txUrl)
                        }
                    },
                    onShareClick = {
                        if (txUrl.isNotEmpty()) {
                            shareManager.shareText(txUrl)
                        }
                    },
                )
                router.replaceAll(SwapRoute.Success)
            },
        )
    }

    private fun sendTransferInProgressEvent() {
        val fromSwapCurrencyStatus = dataState.fromSwapCurrencyStatus
        val toSwapCurrencyStatus = dataState.toSwapCurrencyStatus
        analyticsEventHandler.send(
            event = SwapEvents.TransferInProgressScreen(
                fromCurrency = fromSwapCurrencyStatus?.currency,
                toCurrency = toSwapCurrencyStatus?.currency,
                feeNetwork = getFeeToken().network,
            ),
        )
    }

    private suspend fun processTangemPayWithdrawal(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        swapTransactionState: SwapTransactionState.TangemPayWithdrawalData,
    ) {
        tangemPayWithdrawWithSwapUseCase(
            userWallet = fromSwapCurrencyStatus.userWallet,
            cryptoAmount = swapTransactionState.cryptoAmount,
            cryptoCurrencyId = swapTransactionState.cryptoCurrencyId,
            receiverCexAddress = swapTransactionState.cexAddress,
            exchangeData = swapTransactionState.exchangeData,
        ).onLeft {
            startLoadingQuotesFromLastState()
            onTangemPayWithdrawalError(swapTransactionState.storeData.txExternalId)
        }.onRight { result: WithdrawalResult ->
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

    private suspend fun sendSwapInProgressEvent() {
        val provider = dataState.selectedProvider ?: return
        val feeBucket = getSelectedSwapFee()?.feeBucket ?: FeeBucket.MARKET
        val fromSwapCurrencyStatus = dataState.fromSwapCurrencyStatus ?: return
        val toSwapCurrencyStatus = dataState.toSwapCurrencyStatus ?: return
        val fromDerivationIndex = fromSwapCurrencyStatus.account.derivationIndex?.value
        val toDerivationIndex = toSwapCurrencyStatus.account.derivationIndex?.value

        val feeToken = getFeeToken()
        val feeAssetType = if (feeToken is CryptoCurrency.Coin) {
            AnalyticsParam.FeeAssetType.Coin
        } else {
            AnalyticsParam.FeeAssetType.Token
        }
        analyticsEventHandler.send(
            SwapEvents.SwapInProgressScreen(
                provider = provider,
                commission = feeBucket,
                sendBlockchain = fromSwapCurrencyStatus.currency.network.name,
                receiveBlockchain = toSwapCurrencyStatus.currency.network.name,
                sendToken = fromSwapCurrencyStatus.currency.symbol,
                receiveToken = toSwapCurrencyStatus.currency.symbol,
                feeToken = feeToken.symbol,
                feeAssetType = feeAssetType,
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
            // Virtual account isn't a swap source in the MVP (withdrawal reuses the send flow)
            is Account.Virtual -> emptyFlow()
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
                startLoadingQuotesFromLastState(isSilent = true) // here
            }.flowOn(dispatchers.main).launchIn(modelScope)
            .saveIn(if (isFromCurrency) fromTokenBalanceJobHolder else toTokenBalanceJobHolder)
    }

    /**
     * Handles raw input from the amount text field. [value] is expressed in the currently active
     * input currency (crypto or fiat). The crypto equivalent is always derived and used downstream.
     */
    private fun onAmountChanged(value: String) {
        val fromSwapCurrencyStatus = dataState.fromSwapCurrencyStatus ?: return
        val fiatRate = fromSwapCurrencyStatus.status.value.fiatRate
        val cryptoDecimals = fromSwapCurrencyStatus.currency.decimals
        val cryptoValue = if (isFiatInput.value && fiatRate != null) {
            value.toCryptoFromFiat(fiatRate, cryptoDecimals)
        } else {
            value
        }
        updateAmount(
            cryptoValue = cryptoValue,
            fieldValue = value,
            forceQuotesUpdate = false,
            reduceBalanceBy = BigDecimal.ZERO,
            isPastedAmount = false,
        )
    }

    /**
     * Applies a crypto amount produced programmatically (max / percent / reduce). The visible field
     * value is converted to the active input currency for display, while quotes still use crypto.
     */
    private fun applyCryptoAmount(
        cryptoValue: String,
        forceQuotesUpdate: Boolean = false,
        reduceBalanceBy: BigDecimal = BigDecimal.ZERO,
    ) {
        val fromSwapCurrencyStatus = dataState.fromSwapCurrencyStatus
        val fiatRate = fromSwapCurrencyStatus?.status?.value?.fiatRate
        val fieldValue = if (fromSwapCurrencyStatus != null && isFiatInput.value && fiatRate != null) {
            cryptoValue.toFiatFromCrypto(fiatRate)
        } else {
            cryptoValue
        }
        updateAmount(
            cryptoValue = cryptoValue,
            fieldValue = fieldValue,
            forceQuotesUpdate = forceQuotesUpdate,
            reduceBalanceBy = reduceBalanceBy,
            isPastedAmount = true,
        )
    }

    private fun updateAmount(
        cryptoValue: String,
        fieldValue: String,
        forceQuotesUpdate: Boolean,
        reduceBalanceBy: BigDecimal,
        isPastedAmount: Boolean,
    ) {
        modelScope.launch {
            val fromSwapCurrencyStatus = dataState.fromSwapCurrencyStatus
            val toSwapCurrencyStatus = dataState.toSwapCurrencyStatus
            if (fromSwapCurrencyStatus != null) {
                val minTxAmount = getMinimumTransactionAmountSyncUseCase(
                    userWalletId = fromSwapCurrencyStatus.userWalletId,
                    cryptoCurrencyStatus = fromSwapCurrencyStatus.status,
                ).getOrNull()
                lastAmount.value = cryptoValue
                lastReducedBalanceBy.value = reduceBalanceBy
                uiState = stateBuilder.updateSwapAmount(
                    uiState = uiState,
                    amountRaw = lastAmount.value,
                    fieldValue = fieldValue,
                    isFiatValue = isFiatInput.value,
                    fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                    minTxAmount = minTxAmount,
                    isPastedAmount = isPastedAmount,
                )

                if (toSwapCurrencyStatus != null) {
                    val isUpdatedToTransferMode = isUpdatedToTransferMode(
                        fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                        toSwapCurrencyStatus = toSwapCurrencyStatus,
                        fromTokenAmount = lastAmount.value,
                        forceUpdate = forceQuotesUpdate,
                    )
                    if (isUpdatedToTransferMode) return@launch
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

    /**
     * Switches the "from" amount field between crypto and fiat entry. The stored crypto amount stays
     * authoritative; only the displayed value and equivalent are recomputed (no quote reload).
     */
    private fun onCurrencyChange(isFiat: Boolean) {
        if (isFiat == isFiatInput.value) return
        val fromSwapCurrencyStatus = dataState.fromSwapCurrencyStatus ?: return
        val fiatRate = fromSwapCurrencyStatus.status.value.fiatRate
        if (isFiat && fiatRate == null) return
        isFiatInput.value = isFiat
        val cryptoValue = lastAmount.value
        val fieldValue = when {
            cryptoValue.isEmpty() -> ""
            isFiat && fiatRate != null -> cryptoValue.toFiatFromCrypto(fiatRate)
            else -> cryptoValue
        }
        modelScope.launch {
            val minTxAmount = getMinimumTransactionAmountSyncUseCase(
                userWalletId = fromSwapCurrencyStatus.userWalletId,
                cryptoCurrencyStatus = fromSwapCurrencyStatus.status,
            ).getOrNull()
            uiState = stateBuilder.updateSwapAmount(
                uiState = uiState,
                amountRaw = cryptoValue,
                fieldValue = fieldValue,
                isFiatValue = isFiat,
                fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                minTxAmount = minTxAmount,
                isPastedAmount = true,
            )
        }
    }

    private fun onMaxAmountClicked() {
        dataState.fromSwapCurrencyStatus?.let { fromCurrency ->
            val balance = swapInteractor.getTokenBalance(fromCurrency.status)
            applyCryptoAmount(balance.formatToUIRepresentation())
        }
    }

    private fun onPredefinedPercentSelected(percent: PredefinedPercentAmount) {
        analyticsEventHandler.send(SwapEvents.FastAmountInput(percent))
        if (percent == PredefinedPercentAmount.MAX) {
            onMaxAmountClicked()
            return
        }
        val fromCurrency = dataState.fromSwapCurrencyStatus ?: return
        val newValue = calculateAmountUseCase(
            balance = fromCurrency.status.value.amount ?: BigDecimal.ZERO,
            decimals = fromCurrency.status.currency.decimals,
            percent = percent,
        )
        applyCryptoAmount(
            SwapAmount(
                value = newValue,
                decimals = fromCurrency.status.currency.decimals,
            ).formatToUIRepresentation(),
        )
    }

    private fun onReduceAmountClicked(newAmount: SwapAmount, reduceBalanceBy: BigDecimal = BigDecimal.ZERO) {
        applyCryptoAmount(
            cryptoValue = newAmount.formatToUIRepresentation(),
            forceQuotesUpdate = true,
            reduceBalanceBy = reduceBalanceBy,
        )
    }

    private fun onAmountSelected(selected: Boolean) {
        if (selected) {
            analyticsEventHandler.send(SwapEvents.SendTokenBalanceClicked())
        }
    }

    private fun String.toCryptoFromFiat(fiatRate: BigDecimal, cryptoDecimals: Int): String {
        return parseToBigDecimal(cryptoDecimals)
            .divide(fiatRate, cryptoDecimals, RoundingMode.DOWN)
            .parseBigDecimal(cryptoDecimals)
    }

    private fun String.toFiatFromCrypto(fiatRate: BigDecimal): String {
        return parseToBigDecimal(FIAT_DECIMALS)
            .multiply(fiatRate)
            .parseBigDecimal(FIAT_DECIMALS)
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

    private fun showBackupErrorAlert(userWalletId: UserWalletId) {
        messageSender.send(
            Dialogs.backupErrorAddFundsDisabled(
                onContactSupport = { modelScope.launch { sendBackupProblemEmailUseCase(userWalletId) } },
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
            onCurrencyChange = { onCurrencyChange(it) },
            onSwapClick = {
                onSwapClick()
                val sendTokenSymbol = dataState.fromSwapCurrencyStatus?.currency?.symbol
                val receiveTokenSymbol = dataState.toSwapCurrencyStatus?.currency?.symbol
                if (sendTokenSymbol != null && receiveTokenSymbol != null) {
                    analyticsEventHandler.send(
                        SwapEvents.ButtonSwapClicked(
                            sendToken = sendTokenSymbol,
                            receiveToken = receiveTokenSymbol,
                            swapUIMode = uiState.swapUIMode,
                        ),
                    )
                }
            },
            onTransferClick = {
                onTransferClick()
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
            onPredefinedPercentSelected = ::onPredefinedPercentSelected,
            onReduceToAmount = ::onReduceAmountClicked,
            onReduceByAmount = ::onReduceAmountClicked,
            onApproveClick = {
                singleTaskScheduler.cancelTask()
                sendGivePermissionClickedEvent()
                val approval = getApprovalParams()
                if (approval != null) {
                    approvalSlotNavigation.activate(
                        GiveApprovalEntryComponent.Mode.FullApproval(approval),
                    )
                }
            },
            onApproveTypeSelect = { provider ->
                val approval = getSelectApprovalTypeParams(provider)
                if (approval != null) {
                    approvalSlotNavigation.activate(
                        GiveApprovalEntryComponent.Mode.SelectOnly(approval),
                    )
                }
            },
            onAmountSelected = { onAmountSelected(it) },
            onProviderClick = { providerId ->
                singleTaskScheduler.cancelTask()
                analyticsEventHandler.send(SwapEvents.ProviderClicked())
                val states = dataState.getLastLoadedSuccessStates()
                val pricesLowerBest = getPricesLowerBest(providerId, states)
                uiState = stateBuilder.showSelectProviderBottomSheet(
                    uiState = uiState,
                    selectedProviderId = providerId,
                    pricesLowerBest = pricesLowerBest,
                    providersStates = dataState.lastLoadedSwapStates,
                    isSwapBestDexRateEnabled = swapFeatureToggles.isSwapBestDexRateEnabled,
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
                    modelScope.launch(dispatchers.default) {
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
            onProviderFilterSelect = { filterType ->
                analyticsEventHandler.send(
                    SwapAnalyticsEvent.FilterProvider(
                        filterType = when (filterType) {
                            ProviderFilterType.ALL -> "All"
                            ProviderFilterType.CEX -> "CEX"
                            ProviderFilterType.DEX -> "DEX"
                        },
                    ),
                )
                uiState = stateBuilder.updateProviderFilterType(uiState, filterType)
            },
            openTokenDetailsScreen = { cryptoCurrency ->
                val fromSwapCurrencyStatus = dataState.fromSwapCurrencyStatus ?: return@UiActions
                val route = AppRoute.CurrencyDetails(
                    userWalletId = fromSwapCurrencyStatus.userWalletId,
                    currency = cryptoCurrency,
                )

                appRouter.push(route)
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
            onSwapUIModeChange = ::onSwapUIModeChange,
            onSwapTypeMenuOpened = ::onSwapTypeMenuOpened,
        )
    }

    private fun onSwapUIModeChange(mode: SwapUIMode) {
        val currentMode = uiState.swapUIMode
        if (currentMode == mode) return
        analyticsEventHandler.send(
            SwapEvents.SwapTypeReSelection(typeFrom = currentMode, typeTo = mode),
        )
        uiState = uiState.copy(swapUIMode = mode)
        modelScope.launch { setSwapUiModeUseCase(mode) }
    }

    private fun onSwapTypeMenuOpened() {
        val fromCurrency = dataState.fromSwapCurrencyStatus?.currency
        val toCurrency = dataState.toSwapCurrencyStatus?.currency
        analyticsEventHandler.send(
            SwapEvents.SwapTypeSelect(
                provider = dataState.selectedProvider,
                sendToken = fromCurrency?.symbol.orEmpty(),
                sendBlockchain = fromCurrency?.network?.name.orEmpty(),
                receiveToken = toCurrency?.symbol,
                receiveBlockchain = toCurrency?.network?.name,
            ),
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

    private fun sendSuccessSwapEvent(
        fromToken: CryptoCurrency,
        feeBucket: FeeBucket,
        feeCryptoCurrency: CryptoCurrencyStatus?,
    ) {
        val feeAssetType = if (feeCryptoCurrency?.currency is CryptoCurrency.Coin) {
            AnalyticsParam.FeeAssetType.Coin
        } else {
            AnalyticsParam.FeeAssetType.Token
        }
        val event = AnalyticsParam.TxSentFrom.Swap(
            blockchain = fromToken.network.name,
            token = fromToken.symbol,
            feeType = AnalyticsParam.FeeType.fromString(feeBucket.toAnalyticsName()),
            feeToken = getFeeToken().symbol,
            feeAssetType = feeAssetType,
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
        return getSelectedSwapFee()?.selectedFeeToken?.currency ?: fromToken.currency
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

    private fun getPricesLowerBest(selectedProviderId: String, state: SuccessLoadedSwapData): Map<String, Float> {
        val selectedProviderEntry =
            state.filter { entry -> entry.key.providerId == selectedProviderId }.entries.firstOrNull()
                ?: return emptyMap()
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
        isFiatInput.value = false
        uiState = stateBuilder.createSwapNotSupportedState(
            uiStateHolder = uiState,
            fromSwapCurrencyStatus = fromSwapCurrencyStatus,
            toSwapCurrencyStatus = toSwapCurrencyStatus,
        )
    }

    fun isTangemPayWithdrawal(fromSwapCurrencyStatus: SwapCurrencyStatus? = dataState.fromSwapCurrencyStatus): Boolean {
        return fromSwapCurrencyStatus?.account is Account.Payment
    }

    private fun List<SwapPairLeast>.filterTangemPayProviders(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
    ) = map { pair ->
        val isTangemPayWithdrawal = isTangemPayWithdrawal(
            swapInteractor.extractFromSwapCurrencyFromPair(
                pair = pair,
                fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                toSwapCurrencyStatus = toSwapCurrencyStatus,
            ),
        )
        val filterProviderTypes = if (isTangemPayWithdrawal) {
            listOf(ExchangeProviderType.CEX)
        } else {
            emptyList()
        }
        pair.copy(
            providers = pair.providers.filterIf(filterProviderTypes.isNotEmpty()) { provider ->
                provider.type in filterProviderTypes
            },
        )
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
            val transaction = dataState.getCurrentLoadedSwapState()?.swapDataModel?.transaction
            val fromSwapCurrencyStatus = dataState.fromSwapCurrencyStatus
            val fromCurrency = fromSwapCurrencyStatus?.currency ?: params.fromCryptoCurrency
            val fromWalletId = fromSwapCurrencyStatus?.userWalletId ?: params.userWalletId
            val network = fromCurrency?.network
            val fee = getSelectedSwapFee()?.fee

            saveBlockchainErrorUseCase(
                error = BlockchainErrorInfo(
                    errorMessage = errorMessage,
                    networkId = network?.id,
                    destinationAddress = transaction?.txTo.orEmpty(),
                    tokenSymbol = fromCurrency?.symbol.orEmpty(),
                    amount = dataState.amount.orEmpty(),
                    fee = fee?.amount?.value?.toString().orEmpty(),
                ),
            )

            val metaInfo = getWalletMetaInfoUseCase(fromWalletId).getOrElse { error("CardInfo must be not null") }

            val email = FeedbackEmailType.SwapProblem(
                walletMetaInfo = metaInfo,
                providerName = dataState.selectedProvider?.name.orEmpty(),
                txId = transaction?.txId.orEmpty(),
            )

            analyticsEventHandler.send(Basic.ButtonSupport(source = ScreensSources.Swap))
            sendFeedbackEmailUseCase(email)
        }
    }

    /**
     * Builds a [SwapFee] from the current fee selector state. Returns null
     * when the selector isn't in a `Content` state (e.g. still loading, error). Mirrors the
     * mapping rules from the redesign plan:
     *  - `transactionFeeResult` comes from `transactionFeeExtended` (gasless) or `fees` (native).
     *  - `fee` is the user-selected `FeeItem.fee` (authoritative).
     *  - `feeBucket` is mapped from the `FeeItem` variant.
     *  - `selectedFeeToken` is the fee currency from `feeExtraInfo`.
     *  - `otherNativeFee` is sourced from `dataState.swapDataModel.transaction` (DEX bridge only).
     */
    private fun getSelectedSwapFee(): SwapFee? {
        val feeStateUM = feeSelectorRepository.state.value as? FeeSelectorUM.Content
        if (feeStateUM == null) {
            TangemLogger.e(
                messageString = "getSelectedSwapFee: FeeSelectorUM is not Content: $feeStateUM, returning null",
                shouldSanitize = false,
            )
            return null
        }
        val transactionFeeExtended = feeStateUM.feeExtraInfo.transactionFeeExtended
        val transactionFeeResult =
            transactionFeeExtended?.let { TransactionFeeResult.from(it) } ?: TransactionFeeResult.from(feeStateUM.fees)
        return SwapFee(
            fee = feeStateUM.selectedFeeItem.fee,
            transactionFeeResult = transactionFeeResult,
            selectedFeeToken = feeStateUM.feeExtraInfo.feeCryptoCurrencyStatus,
            otherNativeFee = resolveOtherNativeFee(),
            feeBucket = feeStateUM.selectedFeeItem.toFeeBucket(),
        )
    }

    /**
     * [REDACTED_TASK_KEY] — Phase 4. Extracts the bridge protocol fee from the cached
     * [SwapDataModel.transaction] payload (DEX bridge providers carry `otherNativeFeeWei`).
     *
     * The UI's `FeeSelectorUM` doesn't carry this value, so we read it from the most-recent
     * swap data. Returns [BigDecimal.ZERO] when no swap data is cached, the transaction is not
     * a DEX payload, or `otherNativeFeeWei` is null (non-bridge providers).
     */
    private fun resolveOtherNativeFee(): BigDecimal {
        val transaction =
            dataState.getCurrentLoadedSwapState()?.swapDataModel?.transaction as? ExpressTransactionModel.DEX
                ?: return BigDecimal.ZERO
        val otherNativeFeeWei = transaction.otherNativeFeeWei ?: return BigDecimal.ZERO
        val nativeDecimals = dataState.fromSwapCurrencyStatus?.currency?.network?.let { network ->
            Blockchain.fromNetworkId(network.rawId)?.decimals()
        } ?: return BigDecimal.ZERO
        return otherNativeFeeWei.movePointLeft(nativeDecimals)
    }

    private fun FeeItem.toFeeBucket(): FeeBucket = when (this) {
        is FeeItem.Slow -> FeeBucket.SLOW
        is FeeItem.Market -> FeeBucket.MARKET
        is FeeItem.Fast -> FeeBucket.FAST
        is FeeItem.Suggested -> FeeBucket.SUGGESTED
        is FeeItem.Custom -> FeeBucket.CUSTOM
        is FeeItem.Loading -> FeeBucket.MARKET
    }

    inner class FeeSelectorRepository : SwapFeeSelectorBlockComponent.ModelRepositoryExtended {

        override val state = MutableStateFlow<FeeSelectorUM>(
            FeeSelectorUM.Error(GetFeeError.UnknownError, isHidden = true),
        )

        override val forceUpdateState = MutableSharedFlow<FeeSelectorUM>()

        /**
         * Resolves the `swapData` to hand to [SwapInteractor.loadSwapFee] for the native (non-gasless)
         * fee load. A DEX/DEX_BRIDGE provider whose quote returned `txType=SEND` (swap-xyz native
         * transfer) re-routes to the CEX-style flow without DEX swapData → returns `null`. A real DEX
         * quote without resolved swapData is an error.
         */
        private fun resolveDexSwapDataForFee(
            quoteState: SwapState.QuotesLoadedState,
        ): Either<GetFeeError, SwapDataModel?> {
            return when (quoteState.swapProvider.type) {
                ExchangeProviderType.DEX, ExchangeProviderType.DEX_BRIDGE -> {
                    if (quoteState.txType == ExpressTxType.SEND) {
                        Either.Right(null)
                    } else {
                        quoteState.swapDataModel?.let { Either.Right(it) }
                            ?: Either.Left(GetFeeError.UnknownError)
                    }
                }
                ExchangeProviderType.CEX -> Either.Right(null)
            }
        }

        override suspend fun loadFee(): Either<GetFeeError, TransactionFee> {
            val fromSwapCurrencyStatus =
                dataState.fromSwapCurrencyStatus ?: return Either.Left(GetFeeError.UnknownError)
            val toSwapCurrencyStatus =
                dataState.toSwapCurrencyStatus ?: return Either.Left(GetFeeError.UnknownError)
            val amount = lastAmount.value.parseBigDecimalOrNull() ?: return Either.Left(GetFeeError.UnknownError)

            val shouldTransferInsteadOfSwap = swapTransferInteractor.shouldTransferInsteadOfSwap(
                fromSwapCurrencyStatus.currency,
                toSwapCurrencyStatus.currency,
            )
            return if (shouldTransferInsteadOfSwap) {
                swapTransferInteractor.loadFee(
                    fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                    toSwapCurrencyStatus = toSwapCurrencyStatus,
                    fromTokenAmount = amount,
                ).onLeft {
                    TangemLogger.e("loadFee[transfer]: Failed to load fee with error $it")
                }.onRight {
                    TangemLogger.e("loadFee[transfer]: Fee loaded successfully")
                }
            } else {
                loadSwapModeFee(
                    fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                    toSwapCurrencyStatus = toSwapCurrencyStatus,
                    amount = amount,
                )
            }
        }

        private suspend fun loadSwapModeFee(
            fromSwapCurrencyStatus: SwapCurrencyStatus,
            toSwapCurrencyStatus: SwapCurrencyStatus,
            amount: BigDecimal,
        ): Either<GetFeeError, TransactionFee> {
            val quoteState = dataState.getCurrentLoadedSwapState() ?: return Either.Left(GetFeeError.UnknownError)

            if (isPermissionNotificationShown()) {
                TangemLogger.e("loadFee: Permission notification is shown, cannot load fee")
                return Either.Left(GetFeeError.UnknownError)
            }

            val amountDecimal = lastAmount.value.replace(",", ".").toBigDecimalOrNull()
                ?: return Either.Left(GetFeeError.UnknownError)
            val swapAmount = SwapAmount(amountDecimal, fromSwapCurrencyStatus.currency.decimals)
            val swapDataForCall = resolveDexSwapDataForFee(quoteState)
                .getOrElse { return Either.Left(it) }

            val integratedSettings = (quoteState.permissionState as? PermissionDataState.PermissionSettings)
                ?.takeIf { swapFeatureToggles.isSwapIntegratedApproveEnabled }

            return swapInteractor.loadSwapFee(
                quotesLoadedState = quoteState,
                fromStatus = fromSwapCurrencyStatus,
                toStatus = toSwapCurrencyStatus,
                amount = swapAmount,
                swapData = swapDataForCall,
                selectedFeeToken = null,
                isGasless = false,
                txType = quoteState.txType,
            ).map { swapFee ->
                when (val res = swapFee.transactionFeeResult) {
                    is TransactionFeeResult.LoadedExtended -> res.fee.transactionFee
                    is TransactionFeeResult.Loaded -> res.fee
                }
            }.onLeft {
                TangemLogger.e("loadFee: Failed to load fee with error $it")
            }.flatMap { swapTxFee ->
                if (integratedSettings != null) {
                    // Get fee & tx data for integrated approval case
                    loadAndStoreIntegratedApproval(
                        fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                        quoteState = quoteState,
                        permissionSettings = integratedSettings,
                        approvalAmount = amount,
                        swapTxFee = swapTxFee,
                    )
                } else {
                    Either.Right(swapTxFee)
                }
            }
        }

        override suspend fun loadFeeExtended(
            selectedToken: CryptoCurrencyStatus?,
        ): Either<GetFeeError, TransactionFeeExtended> {
            val fromSwapCurrencyStatus =
                dataState.fromSwapCurrencyStatus ?: return Either.Left(GetFeeError.UnknownError)
            val toSwapCurrencyStatus =
                dataState.toSwapCurrencyStatus ?: return Either.Left(GetFeeError.UnknownError)
            val amount = lastAmount.value.parseBigDecimalOrNull() ?: return Either.Left(GetFeeError.UnknownError)

            val shouldTransferInsteadOfSwap = swapTransferInteractor.shouldTransferInsteadOfSwap(
                fromSwapCurrencyStatus.currency,
                toSwapCurrencyStatus.currency,
            )
            if (shouldTransferInsteadOfSwap) {
                return swapTransferInteractor.loadFeeExtended(
                    fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                    toSwapCurrencyStatus = toSwapCurrencyStatus,
                    fromTokenAmount = amount,
                    selectedToken = selectedToken,
                )
            } else {
                val quoteState = dataState.getCurrentLoadedSwapState() ?: return Either.Left(GetFeeError.UnknownError)

                if (isPermissionNotificationShown()) {
                    return Either.Left(GetFeeError.UnknownError)
                }

                val swapAmount = SwapAmount(amount, fromSwapCurrencyStatus.currency.decimals)

                // DEX path requires a SwapDataModel and does not support gasless yet. swap-xyz native
                // transfers (txType=SEND) re-route to the CEX-style flow, so they take the CEX fee path.
                val swapDataForCall = when (quoteState.swapProvider.type) {
                    ExchangeProviderType.DEX, ExchangeProviderType.DEX_BRIDGE -> {
                        if (quoteState.txType == ExpressTxType.SEND) {
                            null
                        } else {
                            // TODO support gasless in DEX/DEX_BRIDGE
                            return Either.Left(GetFeeError.GaslessError.NetworkIsNotSupported)
                        }
                    }
                    ExchangeProviderType.CEX -> null
                }

                return swapInteractor.loadSwapFee(
                    quotesLoadedState = quoteState,
                    fromStatus = fromSwapCurrencyStatus,
                    toStatus = toSwapCurrencyStatus,
                    amount = swapAmount,
                    swapData = swapDataForCall,
                    selectedFeeToken = selectedToken,
                    isGasless = true,
                    txType = quoteState.txType,
                ).map { swapFee ->
                    // The fee selector block consumes TransactionFeeExtended; build one when
                    // `transactionFeeResult` is LoadedExtended, else wrap the native fee in a
                    // pass-through TransactionFeeExtended for compatibility with the block API.
                    when (val res = swapFee.transactionFeeResult) {
                        is TransactionFeeResult.LoadedExtended -> res.fee
                        is TransactionFeeResult.Loaded -> TransactionFeeExtended(
                            transactionFee = res.fee,
                            feeTokenId = swapFee.selectedFeeToken.currency.id,
                        )
                    }
                }
            }
        }

        override fun onResult(newState: FeeSelectorUM) {
            state.value = newState

            val fromSwapCurrencyStatus = dataState.fromSwapCurrencyStatus ?: return
            val toSwapCurrencyStatus = dataState.toSwapCurrencyStatus ?: return

            if (newState is FeeSelectorUM.Error) {
                handleFeeError(
                    fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                    toSwapCurrencyStatus = toSwapCurrencyStatus,
                    feeError = newState,
                )
            } else {
                // Transfer mode has its own fee pipeline and doesn't use swap quotes.
                val shouldTransferInsteadOfSwap = swapTransferInteractor.shouldTransferInsteadOfSwap(
                    fromSwapCurrencyStatus.currency,
                    toSwapCurrencyStatus.currency,
                )
                if (shouldTransferInsteadOfSwap) {
                    refreshTransferUIStateIfNeeded(
                        feePaidCryptoCurrencyStatus = getSelectedSwapFee()?.selectedFeeToken,
                        fee = (newState as? FeeSelectorUM.Content)?.selectedFeeItem?.fee,
                    )
                    return
                }

                val quoteState = dataState.getCurrentLoadedSwapState() ?: return
                val swapFee = getSelectedSwapFee() ?: return

                modelScope.launch(dispatchers.default) {
                    val patchedState = swapInteractor.applySwapFee(
                        state = quoteState,
                        fee = swapFee,
                        lastReducedBalanceBy = lastReducedBalanceBy.value,
                    )
                    val patchedStates = dataState.lastLoadedSwapStates.toMutableMap().apply {
                        put(quoteState.swapProvider, patchedState)
                    }
                    withContext(dispatchers.main) {
                        dataState = dataState.copy(
                            lastLoadedSwapStates = patchedStates,
                            feePaidCryptoCurrency = swapFee.selectedFeeToken,
                        )
                        // Refresh UI via the existing pipeline.
                        val updatedFromSwapCurrencyStatus = dataState.fromSwapCurrencyStatus ?: return@withContext
                        val updatedToSwapCurrencyStatus = dataState.toSwapCurrencyStatus ?: return@withContext
                        setupLoadedState(
                            provider = quoteState.swapProvider,
                            state = patchedState,
                            fromSwapCurrencyStatus = updatedFromSwapCurrencyStatus,
                            toSwapCurrencyStatus = updatedToSwapCurrencyStatus,
                        )
                    }
                }
            }
        }

        override fun choosingInProgress(updatedState: Boolean) {
            // We shouldn't load quotes while user is choosing fee
            if (updatedState) {
                singleTaskScheduler.cancelTask()
            } else {
                singleTaskScheduler.resumeLastTask(modelScope)
            }
        }

        private fun isPermissionNotificationShown(): Boolean {
            val permissionState = dataState.getCurrentLoadedSwapState()?.permissionState
            val isApprovalIntegrated = swapFeatureToggles.isSwapIntegratedApproveEnabled &&
                permissionState is PermissionDataState.PermissionSettings
            return permissionState != null && permissionState !is PermissionDataState.Empty && !isApprovalIntegrated
        }

        private fun handleFeeError(
            fromSwapCurrencyStatus: SwapCurrencyStatus,
            toSwapCurrencyStatus: SwapCurrencyStatus,
            feeError: FeeSelectorUM.Error,
        ) {
            val error = feeError.error
            if (error is GetFeeError.EstimateOverrideError) {
                analyticsEventHandler.send(
                    SwapEvents.ApproveGasOverrideError(
                        fromTokenSymbol = error.tokenSymbol,
                        fromTokenBlockchain = error.blockchain,
                        rpcProvider = error.rpcProvider,
                        error = error.error,
                    ),
                )
                val (provider, swapState) = updateLoadedQuotes(
                    dataState.lastLoadedSwapStates.mapValues { (_, state) ->
                        if (state is SwapState.QuotesLoadedState) {
                            val permissionState = state.permissionState
                            if (permissionState is PermissionDataState.PermissionSettings) {
                                swapInteractor.integratedApprovalFallback(
                                    fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                                    spenderAddress = permissionState.spenderAddress,
                                )
                                state.copy(
                                    integratedApprovalData = null,
                                    permissionState = PermissionDataState.PermissionRequired(
                                        isResetApproval = false,
                                        spenderAddress = permissionState.spenderAddress,
                                    ),
                                )
                            } else {
                                state
                            }
                        } else {
                            state
                        }
                    },
                )
                setupLoadedState(
                    provider = provider,
                    state = swapState,
                    fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                    toSwapCurrencyStatus = toSwapCurrencyStatus,
                )
            } else {
                TangemLogger.e("loadFee: ${feeError.error}, isHidden = true")
                refreshTransferUIStateIfNeeded()
                uiState = stateBuilder.createFeeErrorState(
                    uiStateHolder = uiState,
                    quoteModel = dataState.getCurrentLoadedSwapState() ?: return,
                    feeCryptoCurrencyStatus = dataState.feePaidCryptoCurrency,
                    feeError = feeError.error,
                )
                modelScope.launch { forceUpdateState.emit(feeError.copy(isHidden = true)) }
            }
        }
    }

    /**
     * Loads the approval transaction + its fee, stores both on the current
     * [SwapState.QuotesLoadedState] as [IntegratedApprovalData], and returns the *combined*
     * [TransactionFee] (approve + swap, per bucket) for the fee selector to render.
     *
     * The user sees a single fee number that already includes the approval cost. At submission
     * time `onSwapClick` reads the stored [IntegratedApprovalData] back from
     * `lastLoadedSwapStates` and sends both txs in a single DEFAULT-mode batch.
     */
    private suspend fun loadAndStoreIntegratedApproval(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        quoteState: SwapState.QuotesLoadedState,
        permissionSettings: PermissionDataState.PermissionSettings,
        approvalAmount: BigDecimal,
        swapTxFee: TransactionFee,
    ): Either<GetFeeError, TransactionFee> {
        return swapInteractor.loadIntegratedApprovalData(
            fromStatus = fromSwapCurrencyStatus,
            spenderAddress = permissionSettings.spenderAddress,
            approveType = permissionSettings.type,
            approvalAmount = approvalAmount,
        ).onLeft {
            TangemLogger.e("loadAndStoreIntegratedApproval: failed: $it")
        }.map { integratedApprovalData ->
            val selectedProvider = quoteState.swapProvider
            val updatedState = quoteState.copy(integratedApprovalData = integratedApprovalData)
            dataState = dataState.copy(
                lastLoadedSwapStates = dataState.lastLoadedSwapStates.toMutableMap().apply {
                    put(selectedProvider, updatedState)
                },
            )
            combineTransactionFees(integratedApprovalData.approvalFee, swapTxFee)
        }
    }

    /**
     * Per-bucket sum of two EVM [TransactionFee]s — used to present the integrated
     * approve+swap total to the user. Mirrors `GiveApprovalModel.estimateFeeForResetApproval`'s
     * sum strategy (same gas-price, summed gas-limit). Non-EVM fees fall back to the swap fee
     * alone since the integrated path is currently EVM-only (DEX, non-Solana).
     */
    private fun combineTransactionFees(approvalFee: TransactionFee, swapFee: TransactionFee): TransactionFee {
        return when {
            approvalFee is TransactionFee.Choosable && swapFee is TransactionFee.Choosable ->
                TransactionFee.Choosable(
                    minimum = sumEvmFees(approvalFee.minimum, swapFee.minimum),
                    normal = sumEvmFees(approvalFee.normal, swapFee.normal),
                    priority = sumEvmFees(approvalFee.priority, swapFee.priority),
                )
            else -> TransactionFee.Single(normal = sumEvmFees(approvalFee.normal, swapFee.normal))
        }
    }

    /**
     * Sums two [Fee.Ethereum] fees as approval + swap. Adds gas limits (same gas price) and
     * recomputes the on-chain amount. For non-Ethereum fees returns [right] unchanged — the
     * integrated approve+swap path is EVM-only today.
     */
    private fun sumEvmFees(left: Fee, right: Fee): Fee {
        if (left !is Fee.Ethereum || right !is Fee.Ethereum) return right
        val leftValue = left.amount.value ?: return right
        val rightValue = right.amount.value ?: return right
        val combinedValue = leftValue + rightValue
        val combinedGasLimit = left.gasLimit + right.gasLimit
        val combinedAmount = right.amount.copy(value = combinedValue)
        return when (right) {
            is Fee.Ethereum.EIP1559 -> right.copy(amount = combinedAmount, gasLimit = combinedGasLimit)
            is Fee.Ethereum.Legacy -> right.copy(amount = combinedAmount, gasLimit = combinedGasLimit)
            is Fee.Ethereum.TokenCurrency -> right
        }
    }

    internal fun getSelectApprovalTypeParams(provider: SwapProvider): SelectApprovalTypeComponent.Params? {
        val fromSwapCurrencyStatus = dataState.fromSwapCurrencyStatus ?: return null
        val swapState = dataState.lastLoadedSwapStates[provider] as? SwapState.QuotesLoadedState ?: return null
        val permissionState = swapState.permissionState as? PermissionDataState.PermissionSettings ?: return null
        val providerName = swapState.swapProvider.name
        val approvalType = permissionState.type

        return SelectApprovalTypeComponent.Params(
            userWalletId = params.userWalletId,
            cryptoCurrencyStatus = fromSwapCurrencyStatus.status,
            initialApproveType = approvalType,
            amountFooter = resourceReference(
                id = R.string.give_permission_swap_subtitle,
                formatArgs = wrappedList(providerName, fromSwapCurrencyStatus.currency.symbol),
            ),
            spenderAddress = permissionState.spenderAddress,
            callback = approvalSelectorCallback,
        )
    }

    internal fun getApprovalParams(): GiveApprovalComponent.Params? {
        val permissionState = uiState.permissionUM as? SwapPermissionUM.PermissionRequired ?: return null
        val fromSwapCurrencyStatus = dataState.fromSwapCurrencyStatus ?: return null
        val feeCryptoCurrency = dataState.feePaidCryptoCurrency ?: return null
        val providerName = dataState.selectedProvider?.name.orEmpty()
        val isHoldToConfirm = fromSwapCurrencyStatus.userWallet.isHotWallet

        return GiveApprovalComponent.Params(
            userWalletId = params.userWalletId,
            cryptoCurrencyStatus = fromSwapCurrencyStatus.status,
            feeCryptoCurrencyStatus = feeCryptoCurrency,
            amount = dataState.amount.orEmpty(),
            spenderAddress = permissionState.spenderAddress,
            amountFooter = if (permissionState.isResetApproval) {
                resourceReference(R.string.update_approval_permission_subtitle)
            } else {
                resourceReference(
                    id = R.string.give_permission_swap_subtitle,
                    formatArgs = wrappedList(providerName, fromSwapCurrencyStatus.currency.symbol),
                )
            },
            feeFooter = resourceReference(R.string.swap_give_permission_fee_footer),
            isResetApproval = permissionState.isResetApproval,
            isHoldToConfirm = isHoldToConfirm,
            callback = approvalFullCallback,
        )
    }

    private companion object {
        const val INITIAL_AMOUNT = ""
        const val FIAT_DECIMALS = 2
        const val UPDATE_DELAY = 10000L
        const val DEBOUNCE_AMOUNT_DELAY = 1000L
        const val UPDATE_BALANCE_DELAY_MILLIS = 11000L
        const val SWAP_IN_PROGRESS_DELAY = 200L
        const val CHANGELLY_PROVIDER_ID = "changelly"
    }
}