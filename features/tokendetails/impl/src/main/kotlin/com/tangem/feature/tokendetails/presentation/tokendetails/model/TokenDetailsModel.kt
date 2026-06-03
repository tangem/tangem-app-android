package com.tangem.feature.tokendetails.presentation.tokendetails.model

import androidx.compose.runtime.Stable
import arrow.core.getOrElse
import arrow.core.right
import com.tangem.utils.logging.TangemLogger
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.blockchain.common.address.AddressType
import com.tangem.domain.dynamicaddresses.IsDynamicAddressesAvailableUseCase
import com.tangem.domain.dynamicaddresses.IsXpubSupportedUseCase
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toHexString
import com.tangem.domain.dynamicaddresses.repository.DynamicAddressesRepository
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.ui.bottomsheet.receive.AddressModel
import com.tangem.common.ui.bottomsheet.receive.mapToAddressModels
import com.tangem.features.rating.RatingComponent
import com.tangem.feature.swap.domain.SwapFeedbackUseCase
import com.tangem.feature.swap.domain.models.domain.SwapFeedbackParams
import com.tangem.features.swap.SwapFeatureToggles
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.event.OfframpAnalyticsEvent
import com.tangem.core.decompose.di.GlobalUiMessageSender
import com.tangem.common.ui.userwallet.converter.WalletIconUMConverter
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.common.ui.tokens.getUnavailabilityReasonText
import com.tangem.core.ui.DesignFeatureToggles
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.core.ui.ds.image.DeviceIconUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.haptic.TangemHapticEffect
import com.tangem.core.ui.haptic.VibratorHapticManager
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.account.status.usecase.GetAccountCurrencyStatusUseCase
import com.tangem.domain.account.status.usecase.IsAccountsModeEnabledUseCase
import com.tangem.domain.account.status.usecase.IsCryptoCurrencyCouldHideUseCase
import com.tangem.domain.account.status.usecase.ManageCryptoCurrenciesUseCase
import com.tangem.domain.account.status.utils.CryptoCurrencyBalanceFetcher
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.demo.IsDemoCardUseCase
import com.tangem.domain.dynamicaddresses.DynamicAddressesSupportedBlockchains
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TokenReceiveNotification
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.offramp.GetOfframpUrlUseCase
import com.tangem.domain.onramp.model.OnrampSource
import com.tangem.domain.staking.GetStakingAvailabilityUseCase
import com.tangem.domain.staking.GetStakingEntryInfoUseCase
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.model.optionOrNull
import com.tangem.domain.tokens.*
import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason
import com.tangem.domain.tokens.model.TokenActionsState
import com.tangem.domain.tokens.model.analytics.TokenReceiveCopyActionSource
import com.tangem.domain.tokens.model.analytics.TokenReceiveNewAnalyticsEvent
import com.tangem.domain.tokens.model.analytics.TokenScreenAnalyticsEvent
import com.tangem.domain.tokens.model.analytics.TokenScreenAnalyticsEvent.Companion.toReasonAnalyticsText
import com.tangem.domain.tokens.model.analytics.TokenScreenAnalyticsEvent.DetailsScreenOpened.TokenBalance
import com.tangem.domain.tokens.model.details.NavigationAction
import com.tangem.domain.tokens.model.details.TokenAction
import com.tangem.domain.transaction.error.AssociateAssetError
import com.tangem.domain.transaction.error.IncompleteTransactionError
import com.tangem.domain.transaction.error.OpenTrustlineError
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.domain.transaction.usecase.*
import com.tangem.domain.txhistory.usecase.GetExplorerTransactionUrlUseCase
import com.tangem.domain.wallets.usecase.GetExploreUrlUseCase
import com.tangem.domain.wallets.usecase.GetWalletIconUseCase
import com.tangem.domain.wallets.usecase.GetExtendedPublicKeyForCurrencyUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.domain.wallets.usecase.NetworkHasDerivationUseCase
import com.tangem.domain.yield.supply.models.YieldSupplyRewardBalance
import com.tangem.domain.yield.supply.usecase.YieldSupplyGetRewardsBalanceUseCase
import com.tangem.feature.tokendetails.deeplink.TokenDetailsDeepLinkActionListener
import com.tangem.feature.tokendetails.domain.GetCurrencyWarningsUseCase
import com.tangem.feature.tokendetails.presentation.router.InnerTokenDetailsRouter
import com.tangem.feature.tokendetails.presentation.tokendetails.analytics.TokenDetailsCurrencyStatusAnalyticsSender
import com.tangem.feature.tokendetails.presentation.tokendetails.analytics.TokenDetailsNotificationsAnalyticsSender
import com.tangem.feature.tokendetails.presentation.tokendetails.route.TokenDetailsBottomSheetConfig
import com.tangem.feature.tokendetails.presentation.tokendetails.state.AddFundsUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenBalanceSegmentedButtonConfig
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsStateController
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TransferUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.factory.TokenDetailsStateFactory
import com.tangem.feature.tokendetails.presentation.tokendetails.state.transformer.InitializeWithCryptoCurrencyTransformer
import com.tangem.feature.tokendetails.presentation.tokendetails.state.transformer.SetBalanceTransformer
import com.tangem.feature.tokendetails.presentation.tokendetails.state.transformer.UpdateActionButtonsTransformer
import com.tangem.feature.tokendetails.presentation.tokendetails.state.transformer.UpdateAddFundsTransformer
import com.tangem.feature.tokendetails.presentation.tokendetails.state.transformer.UpdateTransferTransformer
import com.tangem.feature.tokendetails.presentation.tokendetails.state.transformer.UpdateZeroBalanceActionsTransformer
import com.tangem.feature.tokendetails.presentation.tokendetails.state.transformer.BindAddFundsActionButtonTransformer
import com.tangem.feature.tokendetails.presentation.tokendetails.state.transformer.BindTransferActionButtonTransformer
import com.tangem.feature.tokendetails.presentation.tokendetails.state.transformer.SetTopBarTitleTransformer
import com.tangem.feature.tokendetails.presentation.tokendetails.state.transformer.ToggleBalanceTypeTransformer
import com.tangem.feature.tokendetails.presentation.tokendetails.state.transformer.UpdateStakingNotificationTransformer
import com.tangem.feature.tokendetails.presentation.tokendetails.state.transformer.UpdateNotificationsTransformer
import com.tangem.feature.tokendetails.presentation.tokendetails.state.transformer.UpdateTopBarMenuTransformer
import com.tangem.features.tokendetails.ExpressTransactionsEvent
import com.tangem.features.tokendetails.ExpressTransactionsEventListener
import com.tangem.features.tokendetails.TokenDetailsComponent
import com.tangem.features.tokendetails.impl.R
import com.tangem.features.txhistory.entity.TxHistoryContentUpdateEmitter
import com.tangem.features.yield.supply.api.YieldSupplyDepositedWarningComponent
import com.tangem.features.yield.supply.api.analytics.YieldSupplyAnalytics
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.*
import com.tangem.utils.extensions.isZero
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList", "LargeClass", "TooManyFunctions", "PropertyUsedBeforeDeclaration")
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@Stable
@ModelScoped
internal class TokenDetailsModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val cryptoCurrencyBalanceFetcher: CryptoCurrencyBalanceFetcher,
    private val getExploreUrlUseCase: GetExploreUrlUseCase,
    private val getCryptoCurrencyActionsUseCase: GetCryptoCurrencyActionsUseCase,
    private val isCryptoCurrencyCouldHideUseCase: IsCryptoCurrencyCouldHideUseCase,
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    private val getCurrencyWarningsUseCase: GetCurrencyWarningsUseCase,
    private val getExplorerTransactionUrlUseCase: GetExplorerTransactionUrlUseCase,
    private val getExtendedPublicKeyForCurrencyUseCase: GetExtendedPublicKeyForCurrencyUseCase,
    private val getStakingEntryInfoUseCase: GetStakingEntryInfoUseCase,
    private val getStakingAvailabilityUseCase: GetStakingAvailabilityUseCase,
    private val networkHasDerivationUseCase: NetworkHasDerivationUseCase,
    private val isDemoCardUseCase: IsDemoCardUseCase,
    private val associateAssetUseCase: AssociateAssetUseCase,
    private val retryIncompleteTransactionUseCase: RetryIncompleteTransactionUseCase,
    private val openTrustlineUseCase: OpenTrustlineUseCase,
    private val dismissIncompleteTransactionUseCase: DismissIncompleteTransactionUseCase,
    private val getOfframpUrlUseCase: GetOfframpUrlUseCase,
    private val urlOpener: UrlOpener,
    private val analyticsEventsHandler: AnalyticsEventHandler,
    private val vibratorHapticManager: VibratorHapticManager,
    private val clipboardManager: ClipboardManager,
    @GlobalUiMessageSender private val uiMessageSender: UiMessageSender,
    private val txHistoryContentUpdateEmitter: TxHistoryContentUpdateEmitter,
    private val expressTransactionsEventListener: ExpressTransactionsEventListener,
    paramsContainer: ParamsContainer,
    getUserWalletUseCase: GetUserWalletUseCase,
    private val appRouter: AppRouter,
    private val router: InnerTokenDetailsRouter,
    private val tokenDetailsDeepLinkActionListener: TokenDetailsDeepLinkActionListener,
    private val receiveAddressesFactory: ReceiveAddressesFactory,
    private val saveViewedYieldSupplyWarningUseCase: SaveViewedYieldSupplyWarningUseCase,
    private val saveViewedTokenReceiveWarningUseCase: SaveViewedTokenReceiveWarningUseCase,
    private val needShowYieldSupplyDepositedWarningUseCase: NeedShowYieldSupplyDepositedWarningUseCase,
    private val getAccountCryptoCurrencyStatusUseCase: GetAccountCurrencyStatusUseCase,
    private val manageCryptoCurrenciesUseCase: ManageCryptoCurrenciesUseCase,
    private val yieldSupplyGetRewardsBalanceUseCase: YieldSupplyGetRewardsBalanceUseCase,
    private val signCloreMessageUseCase: SignCloreMessageUseCase,
    private val isXpubSupportedUseCase: IsXpubSupportedUseCase,
    private val dynamicAddressesRepository: DynamicAddressesRepository,
    private val dynamicAddressesDelegateFactory: DynamicAddressesDelegate.Factory,
    private val isDynamicAddressesAvailableUseCase: IsDynamicAddressesAvailableUseCase,
    private val dialogFactory: TokenDetailsDialogFactory,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val singleAccountListSupplier: SingleAccountListSupplier,
    private val getWalletIconUseCase: GetWalletIconUseCase,
    private val walletIconUMConverter: WalletIconUMConverter,
    private val isAccountsModeEnabledUseCase: IsAccountsModeEnabledUseCase,
    private val designFeatureToggles: DesignFeatureToggles,
    private val redesignStateController: TokenDetailsStateController,
    private val swapFeedbackUseCase: SwapFeedbackUseCase,
    private val swapFeatureToggles: SwapFeatureToggles,
) : Model(),
    TokenDetailsClickIntents,
    YieldSupplyDepositedWarningComponent.ModelCallback {

    private val params = paramsContainer.require<TokenDetailsComponent.Params>()
    private val userWalletId: UserWalletId = params.userWalletId
    private val cryptoCurrency: CryptoCurrency = params.currency

    private val userWallet: UserWallet = getUserWalletUseCase(userWalletId).getOrNull()
        ?: error("UserWallet not found")

    private val marketPriceJobHolder = JobHolder()
    private val refreshStateJobHolder = JobHolder()
    private val warningsJobHolder = JobHolder()
    private val buttonsJobHolder = JobHolder()
    private val stakingJobHolder = JobHolder()
    private val yieldSupplyBalanceJobHolder = JobHolder()
    private val selectedAppCurrencyFlow: StateFlow<AppCurrency> = createSelectedAppCurrencyFlow()
    private val redesignBalanceJobHolder = JobHolder()
    private val redesignEarnJobHolder = JobHolder()

    private var cryptoCurrencyStatus: CryptoCurrencyStatus? = null
    private var account: Account.CryptoPortfolio? = null
    private var isBalanceLoadedEventSent = false

    val bottomSheetNavigation: SlotNavigation<TokenDetailsBottomSheetConfig> = SlotNavigation()
    val ratingSlotNavigation = SlotNavigation<RatingComponent.Params>()

    private val stateFactory = TokenDetailsStateFactory(
        currentStateProvider = Provider { uiState.value },
        appCurrencyProvider = Provider(selectedAppCurrencyFlow::value),
        cryptoCurrencyStatusProvider = Provider { cryptoCurrencyStatus },
        tokenDetailsClickIntents = this,
        networkHasDerivationUseCase = networkHasDerivationUseCase,
        getUserWalletUseCase = getUserWalletUseCase,
        userWalletId = userWalletId,
    )

    val uiState: StateFlow<TokenDetailsState>
        field = MutableStateFlow(stateFactory.getInitialState(cryptoCurrency))

    val redesignUiState: StateFlow<TokenDetailsUM> get() = redesignStateController.uiState

    val addFundsUiState: StateFlow<AddFundsUM>
        field = redesignStateController.uiState
            .map { it.addFundsUM }
            .stateIn(
                scope = modelScope,
                started = SharingStarted.Eagerly,
                initialValue = redesignStateController.value.addFundsUM,
            )

    val transferUiState: StateFlow<TransferUM>
        field = redesignStateController.uiState
            .map { it.transferUM }
            .stateIn(
                scope = modelScope,
                started = SharingStarted.Eagerly,
                initialValue = redesignStateController.value.transferUM,
            )

    // region Clore migration
    // TODO: Remove after Clore migration ends ([REDACTED_TASK_KEY])
    val cloreMigrationModel by lazy(mode = LazyThreadSafetyMode.NONE) {
        CloreMigrationModel(
            signCloreMessageUseCase = signCloreMessageUseCase,
            clipboardManager = clipboardManager,
            uiMessageSender = uiMessageSender,
            router = router,
            userWallet = userWallet,
            cryptoCurrency = cryptoCurrency,
            coroutineScope = modelScope,
            dispatchers = dispatchers,
        )
    }
    // endregion

    // region Dynamic Addresses
    val dynamicAddressesDelegate by lazy(mode = LazyThreadSafetyMode.NONE) {
        dynamicAddressesDelegateFactory.create(
            userWallet = userWallet,
            cryptoCurrencyStatusProvider = Provider { cryptoCurrencyStatus },
            appCurrencyProvider = Provider { selectedAppCurrencyFlow.value },
            coroutineScope = modelScope,
            showBottomSheet = {
                bottomSheetNavigation.activate(TokenDetailsBottomSheetConfig.DynamicAddresses)
            },
            dismissBottomSheet = bottomSheetNavigation::dismiss,
            onDynamicAddressesStateChanged = ::onDynamicAddressesStateChanged,
        )
    }
    // endregion Dynamic Addresses

    private val notificationsAnalyticsSender by lazy(mode = LazyThreadSafetyMode.NONE) {
        TokenDetailsNotificationsAnalyticsSender(
            cryptoCurrency = cryptoCurrency,
            analyticsEventHandler = analyticsEventsHandler,
        )
    }

    private val currencyStatusAnalyticsSender by lazy(mode = LazyThreadSafetyMode.NONE) {
        TokenDetailsCurrencyStatusAnalyticsSender(analyticsEventsHandler)
    }

    init {
        initRedesign()
        updateTopBarMenu()
        initButtons()
        updateContent()
        handleBalanceHiding()
        checkForActionUpdates()
        handleNavigationParam()
    }

    private fun initButtons() {
        // we need also init buttons before start all loading to avoid buttons blocking
        modelScope.launch {
            val currentCryptoCurrencyStatus = getAccountCryptoCurrencyStatusUseCase.invokeSync(
                userWalletId = userWalletId,
                currency = cryptoCurrency,
            )
                .onSome { account = it.account }
                .getOrNull()
                ?.status

            currentCryptoCurrencyStatus?.let { status ->
                cryptoCurrencyStatus = status
                updateButtons(currencyStatus = status)
                updateWarnings(cryptoCurrencyStatus = status)
            }
        }
    }

    private fun updateContent() {
        subscribeOnCurrencyStatusUpdates()
    }

    private fun handleBalanceHiding() {
        getBalanceHidingSettingsUseCase()
            .onEach { settings ->
                uiState.value = stateFactory.getStateWithUpdatedHidden(
                    isBalanceHidden = settings.isBalanceHidden,
                )
                if (designFeatureToggles.isRedesignEnabled) {
                    redesignStateController.update { state ->
                        state.copy(isBalanceHidden = settings.isBalanceHidden)
                    }
                }
            }
            .launchIn(modelScope)
    }

    private fun updateButtons(currencyStatus: CryptoCurrencyStatus) {
        getCryptoCurrencyActionsUseCase(
            userWallet = userWallet,
            cryptoCurrencyStatus = currencyStatus,
        )
            .conflate()
            .distinctUntilChanged()
            .onEach { state ->
                sendButtonsEvents(state.states)
                uiState.value = stateFactory.getManageButtonsState(actions = state.states)
                if (designFeatureToggles.isRedesignEnabled) {
                    val networkSource = currencyStatus.value.sources.networkSource
                    redesignStateController.update(
                        UpdateActionButtonsTransformer(
                            actions = state.states,
                            clickIntents = this@TokenDetailsModel,
                        ),
                    )
                    redesignStateController.update(
                        UpdateAddFundsTransformer(
                            actions = state.states,
                            networkSource = networkSource,
                            clickIntents = this@TokenDetailsModel,
                            onActionDispatched = bottomSheetNavigation::dismiss,
                        ),
                    )
                    redesignStateController.update(
                        UpdateTransferTransformer(
                            actions = state.states,
                            networkSource = networkSource,
                            clickIntents = this@TokenDetailsModel,
                            onActionDispatched = bottomSheetNavigation::dismiss,
                        ),
                    )
                    redesignStateController.update(
                        UpdateZeroBalanceActionsTransformer(
                            actions = state.states,
                            networkSource = networkSource,
                            clickIntents = this@TokenDetailsModel,
                        ),
                    )
                }
            }
            .flowOn(dispatchers.main)
            .launchIn(modelScope)
            .saveIn(buttonsJobHolder)
    }

    private fun sendButtonsEvents(states: List<TokenActionsState.ActionState>) {
        // for now send only for swap
        val swapState = states.firstOrNull { it is TokenActionsState.ActionState.Swap } ?: return
        if (swapState.unavailabilityReason != ScenarioUnavailabilityReason.None) {
            analyticsEventsHandler.send(
                TokenScreenAnalyticsEvent.ActionButtonDisabled(
                    token = cryptoCurrency.symbol,
                    status = swapState.unavailabilityReason.toReasonAnalyticsText(),
                    blockchain = cryptoCurrency.network.name,
                    action = "Swap",
                ),
            )
        }
    }

    private fun updateWarnings(cryptoCurrencyStatus: CryptoCurrencyStatus) {
        modelScope.launch(dispatchers.main) {
            getCurrencyWarningsUseCase(
                userWalletId = userWalletId,
                currencyStatus = cryptoCurrencyStatus,
                derivationPath = cryptoCurrency.network.derivationPath,
            )
                .distinctUntilChanged()
                .onEach { warnings ->
                    val updatedState = stateFactory.getStateWithNotifications(warnings)
                    notificationsAnalyticsSender.send(uiState.value, updatedState.notifications)
                    uiState.value = updatedState

                    redesignStateController.update(
                        UpdateNotificationsTransformer(
                            warnings = warnings,
                            clickIntents = this@TokenDetailsModel,
                        ),
                    )
                }
                .launchIn(modelScope)
                .saveIn(warningsJobHolder)
        }
    }

    private fun subscribeOnCurrencyStatusUpdates() {
        getAccountCryptoCurrencyStatusUseCase(userWalletId, cryptoCurrency)
            .onEach { account = it.account }
            .map { it.status.right() }
            .distinctUntilChanged()
            .onEach { maybeCurrencyStatus ->
                uiState.value = stateFactory.getCurrencyLoadedBalanceState(maybeCurrencyStatus)
                maybeCurrencyStatus.onRight { status ->
                    sendOneTimeBalanceLoadedAnalyticsEvent(status)
                    cryptoCurrencyStatus = status
                    updateButtons(currencyStatus = status)
                    updateWarnings(status)
                    subscribeOnUpdateStakingInfo(status)
                    subscribeOnYieldSupplyBalanceIfActive(status)
                }
                currencyStatusAnalyticsSender.send(maybeCurrencyStatus)
            }
            .flowOn(dispatchers.main)
            .launchIn(modelScope)
            .saveIn(marketPriceJobHolder)
    }

    private fun subscribeOnYieldSupplyBalanceIfActive(status: CryptoCurrencyStatus) {
        if (status.value.yieldSupplyStatus?.isActive == true) {
            if (yieldSupplyBalanceJobHolder.isActive && status.value.sources.networkSource != StatusSource.ACTUAL) {
                return
            }
            yieldSupplyGetRewardsBalanceUseCase(status = status, appCurrency = selectedAppCurrencyFlow.value)
                .onEach { formatted ->
                    uiState.value = stateFactory.getStateWithUpdatedYieldSupplyDisplayBalance(formatted)
                }
                .flowOn(dispatchers.main)
                .launchIn(modelScope)
                .saveIn(yieldSupplyBalanceJobHolder)
        } else {
            yieldSupplyBalanceJobHolder.cancel()
            uiState.value = stateFactory.getStateWithUpdatedYieldSupplyDisplayBalance(
                YieldSupplyRewardBalance.empty(),
            )
        }
    }

    private fun updateTxHistory() {
        modelScope.launch { txHistoryContentUpdateEmitter.triggerUpdate() }
    }

    private fun subscribeOnUpdateStakingInfo(cryptoCurrencyStatus: CryptoCurrencyStatus) {
        getStakingAvailabilityUseCase(
            userWalletId = userWalletId,
            cryptoCurrency = cryptoCurrency,
        )
            .map { it.getOrElse { StakingAvailability.Unavailable } }
            .distinctUntilChanged()
            .onEach { availability ->
                val stakingEntryInfo = if (availability is StakingAvailability.Available) {
                    getStakingEntryInfoUseCase(
                        cryptoCurrencyId = cryptoCurrency.id,
                        symbol = cryptoCurrency.symbol,
                        stakingOption = availability.option,
                    ).getOrNull()
                } else {
                    null
                }

                uiState.update { state ->
                    stateFactory.getStakingInfoState(
                        state = state,
                        stakingEntryInfo = stakingEntryInfo,
                        stakingAvailability = availability,
                        cryptoCurrencyStatus = cryptoCurrencyStatus,
                    )
                }
            }
            .flowOn(dispatchers.main)
            .launchIn(modelScope)
            .saveIn(stakingJobHolder)
    }

    private fun updateTopBarMenu() {
        modelScope.launch(dispatchers.main) {
            val hasDerivations = networkHasDerivationUseCase(
                userWallet = userWallet,
                network = cryptoCurrency.network,
            ).getOrElse { false }

            val isSupported = isXPUBSupported()
            val isDynamicAddressesAvailable = isSupported &&
                isDynamicAddressesAvailableUseCase(userWallet, cryptoCurrency)

            uiState.value = stateFactory.getStateWithUpdatedMenu(
                userWallet = userWallet,
                hasDerivations = hasDerivations,
                isSupported = isSupported,
                isDynamicAddressesAvailable = isDynamicAddressesAvailable,
            )
        }
    }

    private suspend fun isXPUBSupported(): Boolean {
        return isXpubSupportedUseCase(userWalletId = userWalletId, network = cryptoCurrency.network)
    }

    private fun createSelectedAppCurrencyFlow(): StateFlow<AppCurrency> {
        return getSelectedAppCurrencyUseCase()
            .map { maybeAppCurrency ->
                maybeAppCurrency.getOrElse { AppCurrency.Default }
            }
            .stateIn(
                scope = modelScope,
                started = SharingStarted.Eagerly,
                initialValue = AppCurrency.Default,
            )
    }

    override fun onBackClick() {
        router.popBackStack()
    }

    override fun onAddFundsClick() {
        bottomSheetNavigation.activate(TokenDetailsBottomSheetConfig.AddFunds)
    }

    override fun onTransferClick() {
        bottomSheetNavigation.activate(TokenDetailsBottomSheetConfig.Transfer)
    }

    override fun onBuyClick(unavailabilityReason: ScenarioUnavailabilityReason) {
        analyticsEventsHandler.send(
            TokenScreenAnalyticsEvent.ButtonWithParams.ButtonBuy(
                token = cryptoCurrency.symbol,
                blockchain = cryptoCurrency.network.name,
                status = unavailabilityReason.toReasonAnalyticsText(),
                derivationIndex = getAccountIndexOrNull(),
            ),
        )

        if (handleUnavailabilityReason(unavailabilityReason = unavailabilityReason)) {
            return
        }

        val status = cryptoCurrencyStatus ?: return
        modelScope.launch {
            appRouter.push(
                AppRoute.Onramp(
                    userWalletId = userWallet.walletId,
                    currency = status.currency,
                    source = OnrampSource.TOKEN_DETAILS,
                ),
            )
        }
    }

    override fun onBuyCoinClick(cryptoCurrency: CryptoCurrency) {
        analyticsEventsHandler.send(
            TokenScreenAnalyticsEvent.ButtonWithParams.ButtonBuy(
                token = cryptoCurrency.symbol,
                blockchain = cryptoCurrency.network.name,
                status = null,
                derivationIndex = getAccountIndexOrNull(),
            ),
        )
        router.openTokenDetails(userWalletId = userWalletId, currency = cryptoCurrency)
    }

    override fun onStakeBannerClick() {
        analyticsEventsHandler.send(TokenScreenAnalyticsEvent.StakingClicked(cryptoCurrency.symbol))
        openStaking()
    }

    override fun onReloadClick() {
        analyticsEventsHandler.send(TokenScreenAnalyticsEvent.ButtonReload(cryptoCurrency.symbol))
        updateTxHistory()
    }

    override fun onSendClick(unavailabilityReason: ScenarioUnavailabilityReason) {
        analyticsEventsHandler.send(
            TokenScreenAnalyticsEvent.ButtonWithParams.ButtonSend(
                token = cryptoCurrency.symbol,
                blockchain = cryptoCurrency.network.name,
                status = unavailabilityReason.toReasonAnalyticsText(),
                derivationIndex = getAccountIndexOrNull(),
            ),
        )

        if (handleUnavailabilityReason(unavailabilityReason = unavailabilityReason)) {
            return
        }
        modelScope.launch {
            if (needShowYieldSupplyDepositedWarningUseCase(cryptoCurrencyStatus)) {
                bottomSheetNavigation.activate(
                    configuration = TokenDetailsBottomSheetConfig.YieldSupplyWarning(
                        cryptoCurrency = cryptoCurrency,
                        tokenAction = TokenAction.Send,
                    ),
                )
            } else {
                sendCurrency()
            }
        }
    }

    private fun sendCurrency() {
        val route = AppRoute.SendEntryPoint(
            currency = cryptoCurrency,
            userWalletId = userWallet.walletId,
        )

        appRouter.push(route)
    }

    override fun onReceiveClick(unavailabilityReason: ScenarioUnavailabilityReason) {
        cryptoCurrencyStatus?.value?.networkAddress ?: return

        analyticsEventsHandler.send(
            TokenScreenAnalyticsEvent.ButtonWithParams.ButtonReceive(
                token = cryptoCurrency.symbol,
                blockchain = cryptoCurrency.network.name,
                status = unavailabilityReason.toReasonAnalyticsText(),
            ),
        )

        if (handleUnavailabilityReason(unavailabilityReason = unavailabilityReason)) {
            return
        }

        modelScope.launch {
            if (needShowYieldSupplyWarning()) {
                bottomSheetNavigation.activate(
                    configuration = TokenDetailsBottomSheetConfig.YieldSupplyWarning(
                        cryptoCurrency = cryptoCurrency,
                        tokenAction = TokenAction.Receive,
                    ),
                )
            } else {
                navigateToReceive()
            }
        }
    }

    override fun onStakeClick(unavailabilityReason: ScenarioUnavailabilityReason) {
        if (handleUnavailabilityReason(unavailabilityReason = unavailabilityReason)) {
            return
        }

        openStaking()
    }

    override fun onDynamicAddressesClick() = dynamicAddressesDelegate.onDynamicAddressesClick()

    override fun onDynamicAddressesFundsFoundLearnMoreClick() {
        // TODO: open "Learn more" URL once the destination is decided
    }

    private fun onDynamicAddressesStateChanged() {
        updateTopBarMenu()
        modelScope.launch(dispatchers.main) {
            cryptoCurrencyBalanceFetcher.invokeAndAwait(userWalletId = userWalletId, currency = cryptoCurrency)
        }
    }

    override fun onGenerateExtendedKey() {
        modelScope.launch(dispatchers.main) {
            val extendedKey = getExtendedPublicKeyForCurrencyUseCase(
                userWalletId,
                cryptoCurrency.network,
            ).fold(
                ifLeft = { throwable ->
                    TangemLogger.e(throwable.cause?.localizedMessage.orEmpty())
                    ""
                },
                ifRight = { it },
            )
            if (extendedKey.isNotBlank()) {
                vibratorHapticManager.performOneTime(TangemHapticEffect.OneTime.Click)
                clipboardManager.setText(text = extendedKey, isSensitive = true)

                val message = resourceReference(R.string.wallet_notification_address_copied)
                uiMessageSender.send(message = SnackbarMessage(message = message))
            }
        }
    }

    override fun onSellClick(unavailabilityReason: ScenarioUnavailabilityReason) {
        analyticsEventsHandler.send(
            TokenScreenAnalyticsEvent.ButtonWithParams.ButtonSell(
                token = cryptoCurrency.symbol,
                blockchain = cryptoCurrency.network.name,
                status = unavailabilityReason.toReasonAnalyticsText(),
            ),
        )

        if (handleUnavailabilityReason(unavailabilityReason = unavailabilityReason)) {
            return
        }

        showErrorIfDemoModeOrElse {
            val status = cryptoCurrencyStatus ?: return@showErrorIfDemoModeOrElse

            getOfframpUrlUseCase(
                cryptoCurrencyStatus = status,
                appCurrencyCode = selectedAppCurrencyFlow.value.code,
            ).onRight { url ->
                urlOpener.openUrl(url)
                analyticsEventsHandler.send(OfframpAnalyticsEvent.ScreenOpened)
            }
        }
    }

    override fun onSwapClick(unavailabilityReason: ScenarioUnavailabilityReason) {
        handleSwap(unavailabilityReason, AppRoute.Swap.CurrencyPosition.ANY, checkYieldSupply = true)
    }

    override fun onSwapFromClick(unavailabilityReason: ScenarioUnavailabilityReason) {
        handleSwap(unavailabilityReason, AppRoute.Swap.CurrencyPosition.FROM, checkYieldSupply = true)
    }

    override fun onSwapToClick(unavailabilityReason: ScenarioUnavailabilityReason) {
        handleSwap(unavailabilityReason, AppRoute.Swap.CurrencyPosition.TO, checkYieldSupply = false)
    }

    private fun handleSwap(
        unavailabilityReason: ScenarioUnavailabilityReason,
        currencyPosition: AppRoute.Swap.CurrencyPosition,
        checkYieldSupply: Boolean,
    ) {
        analyticsEventsHandler.send(
            TokenScreenAnalyticsEvent.ButtonWithParams.ButtonExchange(
                token = cryptoCurrency.symbol,
                blockchain = cryptoCurrency.network.name,
                status = unavailabilityReason.toReasonAnalyticsText(),
                derivationIndex = getAccountIndexOrNull(),
            ),
        )

        if (handleUnavailabilityReason(unavailabilityReason = unavailabilityReason)) {
            return
        }

        modelScope.launch {
            if (checkYieldSupply && needShowYieldSupplyDepositedWarningUseCase(cryptoCurrencyStatus)) {
                bottomSheetNavigation.activate(
                    configuration = TokenDetailsBottomSheetConfig.YieldSupplyWarning(
                        cryptoCurrency = cryptoCurrency,
                        tokenAction = TokenAction.Swap,
                    ),
                )
            } else {
                appRouter.push(
                    AppRoute.Swap(
                        cryptoCurrency = cryptoCurrency,
                        userWalletId = userWalletId,
                        screenSource = AnalyticsParam.ScreensSources.Token.value,
                        currencyPosition = currencyPosition,
                    ),
                )
            }
        }
    }

    override fun onHideClick() {
        analyticsEventsHandler.send(TokenScreenAnalyticsEvent.ButtonRemoveToken(cryptoCurrency.symbol))

        modelScope.launch {
            val canHide = isCryptoCurrencyCouldHideUseCase(
                userWalletId = userWalletId,
                cryptoCurrency = cryptoCurrency,
            )

            if (canHide) {
                dialogFactory.showConfirmHideToken(currency = cryptoCurrency, onConfirm = ::onHideConfirmed)
            } else {
                dialogFactory.showLinkedTokens(currency = cryptoCurrency)
            }
        }
    }

    override fun onHideConfirmed() {
        modelScope.launch {
            val accountId = account?.accountId

            if (accountId == null) {
                TangemLogger.e("Account ID is null, cannot hide currency ${cryptoCurrency.id}")
                return@launch
            }

            manageCryptoCurrenciesUseCase(accountId = accountId, remove = cryptoCurrency)
                .onLeft { TangemLogger.e("Error", it) }
                .onRight { router.popBackStack() }
        }
    }

    override fun onExploreClick() {
        analyticsEventsHandler.send(TokenScreenAnalyticsEvent.ButtonExplore(cryptoCurrency.symbol))
        showErrorIfDemoModeOrElse(action = ::openExplorer)
    }

    private fun getAccountIndexOrNull(): Int? {
        val account = account
        val isNotMainAccount = account != null && !account.isMainAccount
        return if (isNotMainAccount) account.derivationIndex.value else null
    }

    private fun openExplorer() {
        val currencyStatus = cryptoCurrencyStatus ?: return

        modelScope.launch(dispatchers.main) {
            when (val addresses = currencyStatus.value.networkAddress) {
                is NetworkAddress.Selectable -> {
                    bottomSheetNavigation.activate(
                        configuration = TokenDetailsBottomSheetConfig.ChooseAddress(
                            currency = cryptoCurrency,
                            networkAddress = addresses,
                        ),
                    )
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
        if (userWallet is UserWallet.Cold && isDemoCardUseCase(cardId = userWallet.cardId)) {
            bottomSheetNavigation.dismiss()

            uiMessageSender.send(
                message = SnackbarMessage(message = resourceReference(R.string.alert_demo_feature_disabled)),
            )
        } else {
            action()
        }
    }

    override fun onAddressTypeSelected(addressModel: AddressModel) {
        modelScope.launch {
            router.openUrl(
                url = getExploreUrlUseCase(
                    userWalletId = userWalletId,
                    currency = cryptoCurrency,
                    addressType = AddressType.valueOf(addressModel.type.name),
                ),
            )
            bottomSheetNavigation.dismiss()
        }
    }

    override fun onTransactionClick(txHash: String) {
        getExplorerTransactionUrlUseCase(
            txHash = txHash,
            currency = cryptoCurrency,
        ).fold(
            ifLeft = { TangemLogger.e(it.toString()) },
            ifRight = { router.openUrl(url = it) },
        )
    }

    override fun onRefreshSwipe(isRefreshing: Boolean) {
        uiState.value = stateFactory.getRefreshingState()
        redesignStateController.update { state ->
            state.copy(pullToRefreshConfig = state.pullToRefreshConfig.copy(isRefreshing = true))
        }

        modelScope.launch(dispatchers.main) {
            listOf(
                async {
                    cryptoCurrencyBalanceFetcher.invokeAndAwait(userWalletId = userWalletId, currency = cryptoCurrency)
                },
                async {
                    updateTxHistory()
                    expressTransactionsEventListener.send(ExpressTransactionsEvent.Update)
                },
            ).awaitAll()
            uiState.value = stateFactory.getRefreshedState()
            redesignStateController.update { state ->
                state.copy(pullToRefreshConfig = state.pullToRefreshConfig.copy(isRefreshing = false))
            }
        }.saveIn(refreshStateJobHolder)
        ratingSlotNavigation.dismiss()
    }

    override fun onCloseRentInfoNotification() {
        uiState.value = stateFactory.getStateWithRemovedRentNotification()
    }

    override fun onCopyAddress(): TextReference? {
        val networkAddress = cryptoCurrencyStatus?.value?.networkAddress ?: return null
        val addresses = networkAddress.availableAddresses.mapToAddressModels(cryptoCurrency).toImmutableList()
        val defaultAddress = addresses.firstOrNull()?.value ?: return null

        vibratorHapticManager.performOneTime(TangemHapticEffect.OneTime.Click)
        clipboardManager.setText(text = defaultAddress, isSensitive = true)
        analyticsEventsHandler.send(
            TokenReceiveNewAnalyticsEvent.ButtonCopyAddress(
                token = cryptoCurrency.symbol,
                blockchainName = cryptoCurrency.network.name,
                tokenReceiveSource = TokenReceiveCopyActionSource.Token,
            ),
        )
        return resourceReference(R.string.wallet_notification_address_copied)
    }

    override fun onRetryIncompleteTransactionClick() {
        analyticsEventsHandler.send(
            TokenScreenAnalyticsEvent.RevealTryAgain(
                tokenSymbol = cryptoCurrency.symbol,
                blockchain = cryptoCurrency.network.name,
            ),
        )
        modelScope.launch {
            retryIncompleteTransactionUseCase(
                userWallet = userWallet,
                currency = cryptoCurrency,
            ).fold(
                ifLeft = { e ->
                    val message = when (e) {
                        is IncompleteTransactionError.DataError -> e.message.orEmpty()
                        is IncompleteTransactionError.SendError -> {
                            when (val error = e.error) {
                                is SendTransactionError.UserCancelledError,
                                is SendTransactionError.CreateAccountUnderfunded,
                                is SendTransactionError.TangemSdkError,
                                is SendTransactionError.DemoCardError,
                                -> null
                                is SendTransactionError.DataError -> error.message
                                is SendTransactionError.BlockchainSdkError -> error.message
                                is SendTransactionError.NetworkError -> error.message
                                is SendTransactionError.UnknownError -> error.ex?.localizedMessage
                            }
                        }
                    }
                    if (message != null) {
                        dialogFactory.showError(text = stringReference(message))
                        TangemLogger.e(message)
                    }
                },
                ifRight = {
                    uiState.value = stateFactory.getStateWithRemovedKaspaIncompleteTransactionNotification()
                },
            )
        }
    }

    override fun onOpenTrustlineClick() {
        analyticsEventsHandler.send(
            TokenScreenAnalyticsEvent.Associate(
                tokenSymbol = cryptoCurrency.symbol,
                blockchain = cryptoCurrency.network.name,
            ),
        )
        modelScope.launch(dispatchers.mainImmediate) {
            openTrustlineUseCase(
                userWallet = userWallet,
                currency = cryptoCurrency,
            ).fold(
                ifLeft = { e ->
                    val message: TextReference? = when (e) {
                        is OpenTrustlineError.UnknownError -> e.message?.let { stringReference(it) }
                        is OpenTrustlineError.NotEnoughCoin -> resourceReference(
                            id = R.string.warning_token_required_min_coin_reserve,
                            formatArgs = wrappedList(e.amount, e.symbol),
                        )
                        is OpenTrustlineError.SendError -> when (val error = e.error) {
                            is SendTransactionError.UserCancelledError,
                            is SendTransactionError.CreateAccountUnderfunded,
                            is SendTransactionError.TangemSdkError,
                            is SendTransactionError.DemoCardError,
                            -> null
                            is SendTransactionError.DataError -> error.message
                            is SendTransactionError.BlockchainSdkError -> error.message
                            is SendTransactionError.NetworkError -> error.message
                            is SendTransactionError.UnknownError -> error.ex?.localizedMessage
                        }?.let { stringReference(it) }
                    }

                    if (message != null) {
                        dialogFactory.showError(text = message)
                    }
                },
                ifRight = { uiState.value = stateFactory.getStateWithRemovedRequiredTrustlineNotification() },
            )
        }
    }

    override fun onDismissIncompleteTransactionClick() {
        analyticsEventsHandler.send(
            TokenScreenAnalyticsEvent.RevealCancel(
                tokenSymbol = cryptoCurrency.symbol,
                blockchain = cryptoCurrency.network.name,
            ),
        )
        dialogFactory.showDismissIncompleteTransactionConfirm(
            onConfirm = ::onConfirmDismissIncompleteTransactionClick,
        )
    }

    override fun onConfirmDismissIncompleteTransactionClick() {
        modelScope.launch {
            dismissIncompleteTransactionUseCase(
                userWalletId = userWalletId,
                currency = cryptoCurrency,
            ).fold(
                ifLeft = { e ->
                    dialogFactory.showError(text = stringReference(e.message.orEmpty()))
                    TangemLogger.e("Error: $e")
                },
                ifRight = {
                    uiState.value = stateFactory.getStateWithRemovedKaspaIncompleteTransactionNotification()
                },
            )
        }
    }

    override fun onAssociateClick() {
        analyticsEventsHandler.send(
            TokenScreenAnalyticsEvent.Associate(
                tokenSymbol = cryptoCurrency.symbol,
                blockchain = cryptoCurrency.network.name,
            ),
        )
        modelScope.launch(dispatchers.io) {
            associateAssetUseCase(
                userWallet = userWallet,
                currency = cryptoCurrency,
            ).fold(
                ifLeft = { e ->
                    when (e) {
                        is AssociateAssetError.NotEnoughBalance -> {
                            dialogFactory.showError(
                                text =
                                resourceReference(
                                    id = R.string.warning_hedera_token_association_not_enough_hbar_message,
                                    formatArgs = wrappedList(e.feeCurrency.symbol),
                                ),
                            )
                        }
                        is AssociateAssetError.DataError -> {
                            dialogFactory.showError(text = stringReference(e.message.orEmpty()))
                            TangemLogger.e("Error: $e")
                        }
                    }
                },
                ifRight = { uiState.value = stateFactory.getStateWithRemovedHederaAssociateNotification() },
            )
        }
    }

    override fun onBalanceSelect(config: TokenBalanceSegmentedButtonConfig) {
        uiState.value = stateFactory.getStateWithUpdatedBalanceSegmentedButtonConfig(config)
    }

    fun activateRatingForExpressTx(
        txExternalId: String,
        providerName: String,
        txExternalUrl: String,
        userWalletIdStringValue: String,
    ) {
        if (!swapFeatureToggles.isSwapRateExperienceEnabled) return
        ratingSlotNavigation.activate(
            RatingComponent.Params(
                onLoadRating = {
                    swapFeedbackUseCase.getExistingRating(txExternalId)
                        .fold(ifLeft = { null }, ifRight = { it?.rating })
                },
                onSubmitRating = { rating, feedback ->
                    swapFeedbackUseCase.submit(
                        SwapFeedbackParams(
                            userWalletIdHash = userWalletIdStringValue.hexToBytes()
                                .calculateSha256()
                                .toHexString(),
                            providerName = providerName,
                            txUrl = txExternalUrl,
                            txExternalId = txExternalId,
                            rating = rating,
                            feedback = feedback,
                        ),
                    ).onLeft { TangemLogger.e("Failed to submit swap feedback: $it") }
                },
            ),
        )
    }

    override fun onYieldInfoClick() {
        analyticsEventsHandler.send(
            YieldSupplyAnalytics.EarnedFundsInfo(
                token = cryptoCurrency.symbol,
                blockchain = cryptoCurrency.network.name,
            ),
        )
        bottomSheetNavigation.activate(
            configuration = TokenDetailsBottomSheetConfig.YieldSupplyWarning(
                cryptoCurrency = cryptoCurrency,
                tokenAction = TokenAction.Info,
            ),
        )
    }

    private fun handleUnavailabilityReason(unavailabilityReason: ScenarioUnavailabilityReason): Boolean {
        if (unavailabilityReason == ScenarioUnavailabilityReason.None) return false

        dialogFactory.showError(text = unavailabilityReason.getUnavailabilityReasonText())

        return true
    }

    private fun openStaking() {
        modelScope.launch {
            getStakingAvailabilityUseCase.invokeSync(userWalletId, cryptoCurrency)
                .onRight { availability ->
                    val option = availability.optionOrNull
                    if (option != null) {
                        router.openStaking(
                            userWalletId = userWalletId,
                            cryptoCurrency = cryptoCurrency,
                            integrationId = option.integrationId,
                        )
                    } else {
                        showStakingUnavailable()
                    }
                }
                .onLeft { showStakingUnavailable() }
        }
    }

    private fun showStakingUnavailable() {
        TangemLogger.e("Staking is unavailable for ${cryptoCurrency.name}")
        uiMessageSender.send(SnackbarMessage(resourceReference(R.string.staking_error_no_validators_title)))
    }

    private fun checkForActionUpdates() {
        tokenDetailsDeepLinkActionListener.tokenDetailsActionFlow
            .onEach { txId -> expressTransactionsEventListener.send(ExpressTransactionsEvent.OpenTx(txId)) }
            .launchIn(modelScope)
    }

    private suspend fun configureReceiveAddresses(
        cryptoCurrencyStatus: CryptoCurrencyStatus?,
    ): TokenDetailsBottomSheetConfig? {
        cryptoCurrencyStatus ?: return null

        val notifications = buildList {
            if (isActiveYieldSupply()) {
                add(
                    TokenReceiveNotification(
                        title = R.string.yield_module_balance_info_sheet_title,
                        subtitle = R.string.yield_module_balance_info_sheet_subtitle,
                        isYieldSupplyNotification = true,
                    ),
                )
            }
        }

        val receiveConfig = receiveAddressesFactory.create(
            status = cryptoCurrencyStatus,
            userWalletId = userWalletId,
            notifications = notifications,
        ) ?: return null

        return TokenDetailsBottomSheetConfig.Receive(receiveConfig)
    }

    private suspend fun sendOneTimeBalanceLoadedAnalyticsEvent(cryptoCurrencyStatus: CryptoCurrencyStatus?) {
        if (isBalanceLoadedEventSent || cryptoCurrencyStatus == null) return

        val tokenBalance = when (val value = cryptoCurrencyStatus.value) {
            is CryptoCurrencyStatus.Custom,
            is CryptoCurrencyStatus.Loaded,
            is CryptoCurrencyStatus.NoQuote,
            -> {
                if (value.sources.networkSource.isActual()) {
                    val amount = value.amount
                    if (amount != null && !amount.isZero()) {
                        TokenBalance.Full
                    } else {
                        TokenBalance.Empty
                    }
                } else {
                    return
                }
            }
            is CryptoCurrencyStatus.NoAccount -> TokenBalance.Empty
            is CryptoCurrencyStatus.MissedDerivation -> TokenBalance.NoAddress

            is CryptoCurrencyStatus.NoAmount,
            is CryptoCurrencyStatus.Unreachable,
            -> TokenBalance.Error
            CryptoCurrencyStatus.Loading -> return
        }
        analyticsEventsHandler.send(
            event = TokenScreenAnalyticsEvent.DetailsScreenOpened(
                blockchain = cryptoCurrency.network.name,
                token = cryptoCurrency.symbol,
                tokenBalance = tokenBalance,
                isDynamicAddress = getIsDynamicAddressParam(),
            ),
        )
        isBalanceLoadedEventSent = true
    }

    private suspend fun getIsDynamicAddressParam(): Boolean? {
        if (!DynamicAddressesSupportedBlockchains.isSupportedByNetworkId(cryptoCurrency.network.rawId)) return null
        return dynamicAddressesRepository
            .isDynamicAddressesEnabledForNetwork(userWalletId, cryptoCurrency.network.id)
            .first()
    }

    private suspend fun needShowYieldSupplyWarning(): Boolean {
        return needShowYieldSupplyDepositedWarningUseCase(cryptoCurrencyStatus)
    }

    private fun isActiveYieldSupply(): Boolean {
        return cryptoCurrencyStatus?.value?.yieldSupplyStatus?.isActive == true
    }

    override fun onYieldSupplyWarningAcknowledged(tokenAction: TokenAction) {
        bottomSheetNavigation.dismiss()
        modelScope.launch {
            if (tokenAction != TokenAction.Info) {
                saveViewedYieldSupplyWarningUseCase(cryptoCurrency.name)
            }
            if (tokenAction == TokenAction.Receive) {
                saveViewedTokenReceiveWarningUseCase(cryptoCurrency.name)
            }
            when (tokenAction) {
                TokenAction.Receive -> navigateToReceive()
                TokenAction.Send -> sendCurrency()
                TokenAction.Swap -> appRouter.push(
                    AppRoute.Swap(
                        cryptoCurrency = cryptoCurrency,
                        userWalletId = userWalletId,
                        screenSource = AnalyticsParam.ScreensSources.Token.value,
                    ),
                )
                TokenAction.Info -> Unit
            }
        }
    }

    private fun navigateToReceive() {
        modelScope.launch {
            configureReceiveAddresses(cryptoCurrencyStatus = cryptoCurrencyStatus)
                ?.let { bottomSheetNavigation.activate(it) }
        }
    }

    private fun handleNavigationParam() {
        when (params.navigationAction) {
            is NavigationAction.Staking -> openStaking()
            is NavigationAction.CloreMigration -> onCloreMigrationClick()
            is NavigationAction.YieldSupply,
            null,
            -> Unit
        }
    }

    // region Clore migration
    // TODO: Remove after Clore migration ends ([REDACTED_TASK_KEY])

    override fun onCloreMigrationClick() {
        cloreMigrationModel.onCloreMigrationClick()
        bottomSheetNavigation.activate(
            configuration = TokenDetailsBottomSheetConfig.CloreMigration,
        )
    }

    override fun onCloreSignMessage(message: String) = cloreMigrationModel.onCloreSignMessage(message)

    override fun onOpenCloreClaimPortal() = cloreMigrationModel.onOpenCloreClaimPortal()

    // endregion Clore migration

    private fun observeRedesignBalance() {
        getAccountCryptoCurrencyStatusUseCase(userWalletId, cryptoCurrency)
            .map { it.status }
            .distinctUntilChanged()
            .combine(selectedAppCurrencyFlow) { status, appCurrency -> status to appCurrency }
            .onEach { (status, appCurrency) ->
                redesignStateController.update(
                    SetBalanceTransformer(
                        status = status,
                        appCurrency = appCurrency,
                        onToggleBalanceType = ::toggleRedesignBalanceType,
                    ),
                )
            }
            .flowOn(dispatchers.default)
            .launchIn(modelScope)
            .saveIn(redesignBalanceJobHolder)
    }

    private fun toggleRedesignBalanceType() {
        redesignStateController.update(ToggleBalanceTypeTransformer())
    }

    private fun updateRedesignTopBarMenu() {
        modelScope.launch(dispatchers.main) {
            val hasDerivations = networkHasDerivationUseCase(
                userWallet = userWallet,
                network = cryptoCurrency.network,
            ).getOrElse { false }

            val isSupported = isXPUBSupported()

            redesignStateController.update(
                UpdateTopBarMenuTransformer(
                    userWallet = userWallet,
                    hasDerivations = hasDerivations,
                    isXPubSupported = isSupported,
                    onGenerateExtendedKey = ::onGenerateExtendedKey,
                    onHideClick = ::onHideClick,
                ),
            )
        }
    }

    private fun initRedesign() {
        if (!designFeatureToggles.isRedesignEnabled) return
        initRedesignState()
        observeRedesignBalance()
        updateRedesignTopBarMenu()
        observeRedesignTopBarTitle()
        observeRedesignStakingNotification()
    }

    private fun observeRedesignStakingNotification() {
        val statusFlow = getAccountCryptoCurrencyStatusUseCase(userWalletId, cryptoCurrency)
            .map { it.status }
            .distinctUntilChanged()

        val availabilityFlow = getStakingAvailabilityUseCase(
            userWalletId = userWalletId,
            cryptoCurrency = cryptoCurrency,
        )
            .map { it.getOrElse { StakingAvailability.Unavailable } }
            .distinctUntilChanged()

        val entryInfoFlow = availabilityFlow.mapLatest { availability ->
            (availability as? StakingAvailability.Available)?.let { available ->
                getStakingEntryInfoUseCase(
                    cryptoCurrencyId = cryptoCurrency.id,
                    symbol = cryptoCurrency.symbol,
                    stakingOption = available.option,
                ).getOrNull()
            }
        }

        combine(
            flow = statusFlow,
            flow2 = availabilityFlow,
            flow3 = entryInfoFlow,
            flow4 = selectedAppCurrencyFlow,
        ) { status, availability, entryInfo, appCurrency ->
            redesignStateController.update(
                UpdateStakingNotificationTransformer(
                    cryptoCurrencyStatus = status,
                    stakingAvailability = availability,
                    stakingEntryInfo = entryInfo,
                    appCurrency = appCurrency,
                    clickIntents = this,
                ),
            )
        }
            .flowOn(dispatchers.default)
            .launchIn(modelScope)
            .saveIn(redesignEarnJobHolder)
    }

    private fun initRedesignState() {
        redesignStateController.update(
            InitializeWithCryptoCurrencyTransformer(
                cryptoCurrency = cryptoCurrency,
                onBackClick = ::onBackClick,
                onRefreshSwipe = ::onRefreshSwipe,
            ),
        )
        redesignStateController.update(
            BindAddFundsActionButtonTransformer(
                onClick = ::onAddFundsClick,
                onLongClick = { onCopyAddress() },
            ),
        )
        redesignStateController.update(BindTransferActionButtonTransformer(onClick = ::onTransferClick))
    }

    private fun observeRedesignTopBarTitle() {
        combine(
            flow = userWalletsListRepository.userWallets.filterNotNull(),
            flow2 = isAccountsModeEnabledUseCase.invoke(),
            flow3 = singleAccountListSupplier(userWalletId),
            flow4 = getAccountCryptoCurrencyStatusUseCase(userWalletId, cryptoCurrency)
                .map { status -> status.account }
                .distinctUntilChanged(),
        ) { wallets, accountsModeEnabled, accountList, currentAccount ->
            val currentWallet = wallets.firstOrNull { it.walletId == userWalletId } ?: userWallet
            TopBarTitleInputs(
                hasMultipleWallets = wallets.size > 1,
                hasMultipleAccounts = accountsModeEnabled && accountList.accounts.size > 1,
                walletName = currentWallet.name,
                deviceIconUM = walletIconUMConverter.convert(getWalletIconUseCase(currentWallet)),
                account = currentAccount,
            )
        }
            .distinctUntilChanged()
            .onEach { inputs ->
                redesignStateController.update(
                    SetTopBarTitleTransformer(
                        cryptoCurrency = cryptoCurrency,
                        hasMultipleWallets = inputs.hasMultipleWallets,
                        hasMultipleAccounts = inputs.hasMultipleAccounts,
                        walletName = inputs.walletName,
                        deviceIconUM = inputs.deviceIconUM,
                        account = inputs.account,
                    ),
                )
            }
            .flowOn(dispatchers.default)
            .launchIn(modelScope)
    }

    private data class TopBarTitleInputs(
        val hasMultipleWallets: Boolean,
        val hasMultipleAccounts: Boolean,
        val walletName: String,
        val deviceIconUM: DeviceIconUM,
        val account: Account.CryptoPortfolio?,
    )
}