package com.tangem.feature.tokendetails.presentation.tokendetails.model

import androidx.compose.runtime.Stable
import arrow.core.getOrElse
import arrow.core.merge
import arrow.core.right
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.blockchain.common.address.AddressType
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.ui.bottomsheet.receive.AddressModel
import com.tangem.common.ui.bottomsheet.receive.mapToAddressModels
import com.tangem.common.ui.expressStatus.ExpressStatusBottomSheetConfig
import com.tangem.common.ui.expressStatus.state.ExpressTransactionStateUM
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.api.AnalyticsExceptionHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.ExceptionAnalyticsEvent
import com.tangem.core.decompose.di.GlobalUiMessageSender
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.share.ShareManager
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.haptic.TangemHapticEffect
import com.tangem.core.ui.haptic.VibratorHapticManager
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.account.featuretoggle.AccountsFeatureToggles
import com.tangem.domain.account.status.usecase.GetAccountCurrencyStatusUseCase
import com.tangem.domain.account.status.usecase.ManageCryptoCurrenciesUseCase
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.demo.IsDemoCardUseCase
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TokenReceiveNotification
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.onramp.model.OnrampSource
import com.tangem.domain.promo.ShouldShowPromoTokenUseCase
import com.tangem.domain.promo.models.PromoId
import com.tangem.domain.redux.ReduxStateHolder
import com.tangem.domain.staking.GetStakingAvailabilityUseCase
import com.tangem.domain.staking.GetStakingEntryInfoUseCase
import com.tangem.domain.staking.GetYieldUseCase
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.tokens.*
import com.tangem.domain.tokens.legacy.TradeCryptoAction
import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason
import com.tangem.domain.tokens.model.TokenActionsState
import com.tangem.domain.tokens.model.analytics.*
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
import com.tangem.domain.wallets.usecase.GetExtendedPublicKeyForCurrencyUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.domain.wallets.usecase.NetworkHasDerivationUseCase
import com.tangem.domain.yield.supply.models.YieldSupplyRewardBalance
import com.tangem.domain.yield.supply.usecase.YieldSupplyGetRewardsBalanceUseCase
import com.tangem.feature.tokendetails.deeplink.TokenDetailsDeepLinkActionListener
import com.tangem.feature.tokendetails.presentation.router.InnerTokenDetailsRouter
import com.tangem.feature.tokendetails.presentation.tokendetails.analytics.TokenDetailsCurrencyStatusAnalyticsSender
import com.tangem.feature.tokendetails.presentation.tokendetails.analytics.TokenDetailsNotificationsAnalyticsSender
import com.tangem.feature.tokendetails.presentation.tokendetails.route.TokenDetailsBottomSheetConfig
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenBalanceSegmentedButtonConfig
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.factory.TokenDetailsStateFactory
import com.tangem.feature.tokendetails.presentation.tokendetails.state.factory.express.ExpressStatusFactory
import com.tangem.features.tokendetails.TokenDetailsComponent
import com.tangem.features.tokendetails.impl.R
import com.tangem.features.tokenreceive.TokenReceiveFeatureToggle
import com.tangem.features.txhistory.entity.TxHistoryContentUpdateEmitter
import com.tangem.features.yield.supply.api.YieldSupplyDepositedWarningComponent
import com.tangem.features.yield.supply.api.YieldSupplyFeatureToggles
import com.tangem.features.yield.supply.api.analytics.YieldSupplyAnalytics
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.*
import com.tangem.utils.extensions.isZero
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@Suppress("LongParameterList", "LargeClass", "TooManyFunctions")
@Stable
@ModelScoped
internal class TokenDetailsModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val getSingleCryptoCurrencyStatusUseCase: GetSingleCryptoCurrencyStatusUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val fetchCurrencyStatusUseCase: FetchCurrencyStatusUseCase,
    private val getExploreUrlUseCase: GetExploreUrlUseCase,
    private val getCryptoCurrencyActionsUseCase: GetCryptoCurrencyActionsUseCase,
    private val removeCurrencyUseCase: RemoveCurrencyUseCase,
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    private val getCurrencyWarningsUseCase: GetCurrencyWarningsUseCase,
    private val getExplorerTransactionUrlUseCase: GetExplorerTransactionUrlUseCase,
    private val shouldShowPromoTokenUseCase: ShouldShowPromoTokenUseCase,
    private val updateDelayedCurrencyStatusUseCase: UpdateDelayedNetworkStatusUseCase,
    private val getExtendedPublicKeyForCurrencyUseCase: GetExtendedPublicKeyForCurrencyUseCase,
    private val getStakingEntryInfoUseCase: GetStakingEntryInfoUseCase,
    private val getStakingAvailabilityUseCase: GetStakingAvailabilityUseCase,
    private val getYieldUseCase: GetYieldUseCase,
    private val networkHasDerivationUseCase: NetworkHasDerivationUseCase,
    private val isDemoCardUseCase: IsDemoCardUseCase,
    private val associateAssetUseCase: AssociateAssetUseCase,
    private val retryIncompleteTransactionUseCase: RetryIncompleteTransactionUseCase,
    private val openTrustlineUseCase: OpenTrustlineUseCase,
    private val dismissIncompleteTransactionUseCase: DismissIncompleteTransactionUseCase,
    private val reduxStateHolder: ReduxStateHolder,
    private val analyticsEventsHandler: AnalyticsEventHandler,
    private val vibratorHapticManager: VibratorHapticManager,
    private val clipboardManager: ClipboardManager,
    private val shareManager: ShareManager,
    @GlobalUiMessageSender private val uiMessageSender: UiMessageSender,
    private val txHistoryContentUpdateEmitter: TxHistoryContentUpdateEmitter,
    paramsContainer: ParamsContainer,
    expressStatusFactory: ExpressStatusFactory.Factory,
    getUserWalletUseCase: GetUserWalletUseCase,
    private val appRouter: AppRouter,
    private val router: InnerTokenDetailsRouter,
    private val tokenDetailsDeepLinkActionListener: TokenDetailsDeepLinkActionListener,
    private val analyticsExceptionHandler: AnalyticsExceptionHandler,
    private val tokenReceiveFeatureToggle: TokenReceiveFeatureToggle,
    private val receiveAddressesFactory: ReceiveAddressesFactory,
    private val yieldSupplyFeatureToggles: YieldSupplyFeatureToggles,
    private val saveViewedYieldSupplyWarningUseCase: SaveViewedYieldSupplyWarningUseCase,
    private val saveViewedTokenReceiveWarningUseCase: SaveViewedTokenReceiveWarningUseCase,
    private val needShowYieldSupplyDepositedWarningUseCase: NeedShowYieldSupplyDepositedWarningUseCase,
    private val accountsFeatureToggles: AccountsFeatureToggles,
    private val getAccountCryptoCurrencyStatusUseCase: GetAccountCurrencyStatusUseCase,
    private val manageCryptoCurrenciesUseCase: ManageCryptoCurrenciesUseCase,
    private val yieldSupplyGetRewardsBalanceUseCase: YieldSupplyGetRewardsBalanceUseCase,
) : Model(), TokenDetailsClickIntents, YieldSupplyDepositedWarningComponent.ModelCallback {

    private val params = paramsContainer.require<TokenDetailsComponent.Params>()
    private val userWalletId: UserWalletId = params.userWalletId
    private val cryptoCurrency: CryptoCurrency = params.currency

    private val userWallet: UserWallet = getUserWalletUseCase(userWalletId).getOrNull() ?: error("UserWallet not found")

    private val marketPriceJobHolder = JobHolder()
    private val refreshStateJobHolder = JobHolder()
    private val warningsJobHolder = JobHolder()
    private val expressTxJobHolder = JobHolder()
    private val buttonsJobHolder = JobHolder()
    private val stakingJobHolder = JobHolder()
    private val yieldSupplyBalanceJobHolder = JobHolder()
    private val selectedAppCurrencyFlow: StateFlow<AppCurrency> = createSelectedAppCurrencyFlow()

    private var cryptoCurrencyStatus: CryptoCurrencyStatus? = null
    private var account: Account.CryptoPortfolio? = null
    private var isBalanceLoadedEventSent = false
    private var expressTxStatusTaskScheduler = SingleTaskScheduler<PersistentList<ExpressTransactionStateUM>>()

    /** Transaction id to check for status */
    private val waitForFirstExpressStatusEmmit = MutableStateFlow(false)

    val bottomSheetNavigation: SlotNavigation<TokenDetailsBottomSheetConfig> = SlotNavigation()

    private val stateFactory = TokenDetailsStateFactory(
        currentStateProvider = Provider { uiState.value },
        appCurrencyProvider = Provider(selectedAppCurrencyFlow::value),
        cryptoCurrencyStatusProvider = Provider { cryptoCurrencyStatus },
        clickIntents = this,
        networkHasDerivationUseCase = networkHasDerivationUseCase,
        getUserWalletUseCase = getUserWalletUseCase,
        userWalletId = userWalletId,
        yieldSupplyFeatureToggles = yieldSupplyFeatureToggles,
    )

    private val expressStatusFactory by lazy(mode = LazyThreadSafetyMode.NONE) {
        expressStatusFactory.create(
            clickIntents = this,
            appCurrencyProvider = Provider { selectedAppCurrencyFlow.value },
            currentStateProvider = Provider { uiState.value },
            cryptoCurrencyStatusProvider = Provider { cryptoCurrencyStatus },
            userWallet = userWallet,
            cryptoCurrency = cryptoCurrency,
        )
    }

    private val notificationsAnalyticsSender by lazy(mode = LazyThreadSafetyMode.NONE) {
        TokenDetailsNotificationsAnalyticsSender(
            cryptoCurrency = cryptoCurrency,
            analyticsEventHandler = analyticsEventsHandler,
        )
    }

    private val currencyStatusAnalyticsSender by lazy(mode = LazyThreadSafetyMode.NONE) {
        TokenDetailsCurrencyStatusAnalyticsSender(analyticsEventsHandler)
    }

    private val internalUiState = MutableStateFlow(stateFactory.getInitialState(cryptoCurrency))
    val uiState: StateFlow<TokenDetailsState> = internalUiState

    init {
        updateTopBarMenu()
        initButtons()
        updateContent()
        handleBalanceHiding()
        checkForActionUpdates()
        handleNavigationParam()
    }

    fun onResume() {
        subscribeOnExpressTransactionsUpdates()
    }

    fun onPause() {
        expressTxStatusTaskScheduler.cancelTask()
        expressTxJobHolder.cancel()
    }

    override fun onDestroy() {
        expressTxStatusTaskScheduler.cancelTask()
        expressTxJobHolder.cancel()
        super.onDestroy()
    }

    private fun initButtons() {
        // we need also init buttons before start all loading to avoid buttons blocking
        modelScope.launch {
            val currentCryptoCurrencyStatus = if (accountsFeatureToggles.isFeatureEnabled) {
                getAccountCryptoCurrencyStatusUseCase.invokeSync(
                    userWalletId = userWalletId,
                    currency = cryptoCurrency,
                )
                    .onSome { account = it.account }
                    .getOrNull()
                    ?.status
            } else {
                getSingleCryptoCurrencyStatusUseCase.invokeMultiWalletSync(
                    userWalletId = userWalletId,
                    cryptoCurrencyId = cryptoCurrency.id,
                    isSingleWalletWithTokens = false,
                ).getOrNull()
            }

            currentCryptoCurrencyStatus?.let { status ->
                cryptoCurrencyStatus = status
                updateButtons(currencyStatus = status)
                updateWarnings(cryptoCurrencyStatus = status)
            }
        }
    }

    private fun updateContent() {
        subscribeOnCurrencyStatusUpdates()
        subscribeOnExpressTransactionsUpdates()
    }

    private fun handleBalanceHiding() {
        getBalanceHidingSettingsUseCase()
            .onEach {
                internalUiState.value = stateFactory.getStateWithUpdatedHidden(
                    isBalanceHidden = it.isBalanceHidden,
                )
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
            .onEach {
                sendButtonsEvents(it.states)
                internalUiState.value = stateFactory.getManageButtonsState(actions = it.states)
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
            getCurrencyWarningsUseCase.invoke(
                userWalletId = userWalletId,
                currencyStatus = cryptoCurrencyStatus,
                derivationPath = cryptoCurrency.network.derivationPath,
                isSingleWalletWithTokens = userWallet is UserWallet.Cold &&
                    userWallet.scanResponse.cardTypesResolver.isSingleWalletWithToken(),
            )
                .distinctUntilChanged()
                .onEach {
                    val updatedState = stateFactory.getStateWithNotifications(it)
                    notificationsAnalyticsSender.send(internalUiState.value, updatedState.notifications)
                    internalUiState.value = updatedState
                }
                .launchIn(modelScope)
                .saveIn(warningsJobHolder)
        }
    }

    private fun subscribeOnCurrencyStatusUpdates() {
        if (accountsFeatureToggles.isFeatureEnabled) {
            getAccountCryptoCurrencyStatusUseCase(userWalletId, cryptoCurrency)
                .onEach { account = it.account }
                .map { it.status.right() }
        } else {
            getSingleCryptoCurrencyStatusUseCase.invokeMultiWallet(
                userWalletId = userWalletId,
                currencyId = cryptoCurrency.id,
                isSingleWalletWithTokens = userWallet is UserWallet.Cold &&
                    userWallet.scanResponse.cardTypesResolver.isSingleWalletWithToken(),
            )
        }
            .distinctUntilChanged()
            .onEach { maybeCurrencyStatus ->
                internalUiState.value = stateFactory.getCurrencyLoadedBalanceState(maybeCurrencyStatus)
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

    private fun subscribeOnExpressTransactionsUpdates() {
        expressTxStatusTaskScheduler.cancelTask()
        expressStatusFactory.getExpressStatuses()
            .distinctUntilChanged()
            .onEach { waitForFirstExpressStatusEmmit.value = true }
            .onEach { expressTxs ->
                internalUiState.value = expressStatusFactory.getStateWithUpdatedExpressTxs(
                    expressTxs = expressTxs,
                    updateBalance = ::updateNetworkToSwapBalance,
                )
                expressTxStatusTaskScheduler.scheduleTask(
                    scope = modelScope,
                    task = PeriodicTask(
                        isDelayFirst = false,
                        delay = EXPRESS_STATUS_UPDATE_DELAY,
                        task = {
                            runCatching {
                                expressStatusFactory.getUpdatedExpressStatuses(internalUiState.value.expressTxs)
                            }
                        },
                        onSuccess = { updatedTxs ->
                            internalUiState.value = expressStatusFactory.getStateWithUpdatedExpressTxs(
                                updatedTxs,
                                ::updateNetworkToSwapBalance,
                            )
                        },
                        onError = { /* no-op */ },
                    ),
                )
            }
            .flowOn(dispatchers.main)
            .launchIn(modelScope)
            .saveIn(expressTxJobHolder)
    }

    private fun subscribeOnYieldSupplyBalanceIfActive(status: CryptoCurrencyStatus) {
        if (yieldSupplyFeatureToggles.isYieldSupplyFeatureEnabled &&
            status.value.yieldSupplyStatus?.isActive == true
        ) {
            if (yieldSupplyBalanceJobHolder.isActive && status.value.sources.networkSource != StatusSource.ACTUAL) {
                return
            }
            yieldSupplyGetRewardsBalanceUseCase(status = status, appCurrency = selectedAppCurrencyFlow.value)
                .onEach { formatted ->
                    internalUiState.value = stateFactory.getStateWithUpdatedYieldSupplyDisplayBalance(formatted)
                }
                .flowOn(dispatchers.main)
                .launchIn(modelScope)
                .saveIn(yieldSupplyBalanceJobHolder)
        } else {
            yieldSupplyBalanceJobHolder.cancel()
            internalUiState.value = stateFactory.getStateWithUpdatedYieldSupplyDisplayBalance(
                YieldSupplyRewardBalance.empty(),
            )
        }
    }

    private fun updateNetworkToSwapBalance(toCryptoCurrency: CryptoCurrency) {
        modelScope.launch {
            updateDelayedCurrencyStatusUseCase(
                userWalletId = userWalletId,
                network = toCryptoCurrency.network,
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

                internalUiState.update { state ->
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

            internalUiState.value = stateFactory.getStateWithUpdatedMenu(
                userWallet = userWallet,
                hasDerivations = hasDerivations,
                isSupported = isSupported,
            )
        }
    }

    private suspend fun isXPUBSupported(): Boolean {
        return getExtendedPublicKeyForCurrencyUseCase.isSupported(
            userWalletId = userWalletId,
            network = cryptoCurrency.network,
        )
            .mapLeft {
                analyticsExceptionHandler.sendException(
                    event = ExceptionAnalyticsEvent(
                        exception = it,
                        params = mapOf(
                            "blockchainId" to cryptoCurrency.network.id.rawId.value,
                            "networkId" to cryptoCurrency.network.backendId,
                        ),
                    ),
                )

                Timber.e(
                    /* t = */ it,
                    /* message = */ "Unable to get wallet manager for user wallet %s and network %s",
                    /* ...args = */ userWalletId,
                    cryptoCurrency.network,
                )

                false
            }
            .merge()
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

    override fun onBuyClick(unavailabilityReason: ScenarioUnavailabilityReason) {
        analyticsEventsHandler.send(
            TokenScreenAnalyticsEvent.ButtonWithParams.ButtonBuy(
                token = cryptoCurrency.symbol,
                blockchain = cryptoCurrency.network.name,
                status = unavailabilityReason.toReasonAnalyticsText(),
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

    override fun onGenerateExtendedKey() {
        modelScope.launch(dispatchers.main) {
            val extendedKey = getExtendedPublicKeyForCurrencyUseCase(
                userWalletId,
                cryptoCurrency.network,
            ).fold(
                ifLeft = {
                    Timber.e(it.cause?.localizedMessage.orEmpty())
                    ""
                },
                ifRight = { it },
            )
            if (extendedKey.isNotBlank()) {
                vibratorHapticManager.performOneTime(TangemHapticEffect.OneTime.Click)
                clipboardManager.setText(text = extendedKey, isSensitive = true)

                uiMessageSender.send(
                    message = SnackbarMessage(message = resourceReference(R.string.wallet_notification_address_copied)),
                )
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

            reduxStateHolder.dispatch(
                TradeCryptoAction.Sell(
                    cryptoCurrencyStatus = status,
                    appCurrencyCode = selectedAppCurrencyFlow.value.code,
                ),
            )
        }
    }

    override fun onSwapClick(unavailabilityReason: ScenarioUnavailabilityReason) {
        analyticsEventsHandler.send(
            TokenScreenAnalyticsEvent.ButtonWithParams.ButtonExchange(
                token = cryptoCurrency.symbol,
                blockchain = cryptoCurrency.network.name,
                status = unavailabilityReason.toReasonAnalyticsText(),
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
                        tokenAction = TokenAction.Swap,
                    ),
                )
            } else {
                appRouter.push(
                    AppRoute.Swap(
                        currencyFrom = cryptoCurrency,
                        userWalletId = userWalletId,
                        screenSource = AnalyticsParam.ScreensSources.Token.value,
                    ),
                )
            }
        }
    }

    override fun onDismissDialog() {
        internalUiState.value = stateFactory.getStateWithClosedDialog()
    }

    override fun onHideClick() {
        analyticsEventsHandler.send(TokenScreenAnalyticsEvent.ButtonRemoveToken(cryptoCurrency.symbol))

        modelScope.launch {
            val hasLinkedTokens = removeCurrencyUseCase.hasLinkedTokens(userWalletId, cryptoCurrency)
            internalUiState.value = if (hasLinkedTokens) {
                stateFactory.getStateWithLinkedTokensDialog(cryptoCurrency)
            } else {
                stateFactory.getStateWithConfirmHideTokenDialog(cryptoCurrency)
            }
        }
    }

    override fun onHideConfirmed() {
        modelScope.launch {
            if (accountsFeatureToggles.isFeatureEnabled) {
                val accountId = account?.accountId

                if (accountId == null) {
                    Timber.e("Account ID is null, cannot hide currency ${cryptoCurrency.id}")
                    return@launch
                }

                manageCryptoCurrenciesUseCase(accountId = accountId, remove = cryptoCurrency)
            } else {
                removeCurrencyUseCase(userWalletId, cryptoCurrency)
            }
                .onLeft { Timber.e(it) }
                .onRight { router.popBackStack() }
        }
    }

    override fun onExploreClick() {
        analyticsEventsHandler.send(TokenScreenAnalyticsEvent.ButtonExplore(cryptoCurrency.symbol))
        showErrorIfDemoModeOrElse(action = ::openExplorer)
    }

    private fun openExplorer() {
        val currencyStatus = cryptoCurrencyStatus ?: return

        modelScope.launch(dispatchers.main) {
            when (val addresses = currencyStatus.value.networkAddress) {
                is NetworkAddress.Selectable -> {
                    internalUiState.value = stateFactory.getStateWithChooseAddressBottomSheet(cryptoCurrency, addresses)
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
            internalUiState.value = stateFactory.getStateWithClosedBottomSheet()

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
            internalUiState.value = stateFactory.getStateWithClosedBottomSheet()
        }
    }

    override fun onTransactionClick(txHash: String) {
        getExplorerTransactionUrlUseCase(
            txHash = txHash,
            networkId = cryptoCurrency.network.id,
        ).fold(
            ifLeft = { Timber.e(it.toString()) },
            ifRight = { router.openUrl(url = it) },
        )
    }

    override fun onRefreshSwipe(isRefreshing: Boolean) {
        internalUiState.value = stateFactory.getRefreshingState()

        modelScope.launch(dispatchers.main) {
            listOf(
                async { fetchCurrencyStatusUseCase(userWalletId = userWalletId, id = cryptoCurrency.id) },
                async {
                    updateTxHistory()
                    subscribeOnExpressTransactionsUpdates()
                },
            ).awaitAll()
            internalUiState.value = stateFactory.getRefreshedState()
        }.saveIn(refreshStateJobHolder)
    }

    override fun onDismissBottomSheet() {
        when (val bsContent = internalUiState.value.bottomSheetConfig?.content) {
            is ExpressStatusBottomSheetConfig -> {
                modelScope.launch(dispatchers.main) {
                    expressStatusFactory.removeTransactionOnBottomSheetClosed(bsContent.value)
                }
            }
        }
        internalUiState.value = stateFactory.getStateWithClosedBottomSheet()
    }

    override fun onCloseRentInfoNotification() {
        internalUiState.value = stateFactory.getStateWithRemovedRentNotification()
    }

    override fun onExpressTransactionClick(txId: String) {
        val expressTxState = internalUiState.value.expressTxsToDisplay.firstOrNull { it.info.txId == txId }
            ?: return
        internalUiState.value = expressStatusFactory.getStateWithExpressStatusBottomSheet(expressTxState)
    }

    override fun onGoToProviderClick(url: String) {
        router.openUrl(url)
    }

    override fun onGoToRefundedTokenClick(cryptoCurrency: CryptoCurrency) {
        router.openTokenDetails(userWalletId, cryptoCurrency)
    }

    override fun onOpenUrlClick(url: String) {
        router.openUrl(url)
    }

    override fun onSwapPromoDismiss(promoId: PromoId) {
        modelScope.launch(dispatchers.main) {
            shouldShowPromoTokenUseCase.neverToShow(promoId)
            analyticsEventsHandler.send(
                PromoAnalyticsEvent.PromotionBannerClicked(
                    source = AnalyticsParam.ScreensSources.Token,
                    program = PromoAnalyticsEvent.Program.Empty, // Use it on new promo action
                    action = PromoAnalyticsEvent.PromotionBannerClicked.BannerAction.Closed,
                ),
            )
        }
    }

    override fun onSwapPromoClick(promoId: PromoId) {
        modelScope.launch(dispatchers.main) {
            shouldShowPromoTokenUseCase.neverToShow(promoId)
            analyticsEventsHandler.send(
                PromoAnalyticsEvent.PromotionBannerClicked(
                    source = AnalyticsParam.ScreensSources.Token,
                    program = PromoAnalyticsEvent.Program.Empty, // Use it on new promo action
                    action = PromoAnalyticsEvent.PromotionBannerClicked.BannerAction.Clicked,
                ),
            )
        }
        onSwapClick(ScenarioUnavailabilityReason.None)
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
                userWalletId = userWalletId,
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
                    message?.let {
                        internalUiState.value = stateFactory.getStateWithErrorDialog(stringReference(it))
                        Timber.e(it)
                    }
                },
                ifRight = {
                    internalUiState.value = stateFactory.getStateWithRemovedKaspaIncompleteTransactionNotification()
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
                userWalletId = userWalletId,
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
                    message?.let { internalUiState.value = stateFactory.getStateWithErrorDialog(message) }
                },
                ifRight = { internalUiState.value = stateFactory.getStateWithRemovedRequiredTrustlineNotification() },
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
        modelScope.launch {
            internalUiState.value = stateFactory.getStateWithDismissIncompleteTransactionConfirmDialog()
        }
    }

    override fun onConfirmDismissIncompleteTransactionClick() {
        modelScope.launch {
            dismissIncompleteTransactionUseCase(
                userWalletId = userWalletId,
                currency = cryptoCurrency,
            ).fold(
                ifLeft = { e ->
                    internalUiState.value = stateFactory.getStateWithErrorDialog(
                        stringReference(e.message.orEmpty()),
                    )
                    Timber.e(e.message)
                },
                ifRight = {
                    internalUiState.value = stateFactory.getStateWithRemovedKaspaIncompleteTransactionNotification()
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
                userWalletId = userWalletId,
                currency = cryptoCurrency,
            ).fold(
                ifLeft = { e ->
                    when (e) {
                        is AssociateAssetError.NotEnoughBalance -> {
                            internalUiState.value = stateFactory.getStateWithErrorDialog(
                                resourceReference(
                                    id = R.string.warning_hedera_token_association_not_enough_hbar_message,
                                    formatArgs = wrappedList(e.feeCurrency.symbol),
                                ),
                            )
                        }
                        is AssociateAssetError.DataError -> {
                            internalUiState.value = stateFactory.getStateWithErrorDialog(
                                stringReference(e.message.orEmpty()),
                            )
                            Timber.e(e.message)
                        }
                    }
                },
                ifRight = { internalUiState.value = stateFactory.getStateWithRemovedHederaAssociateNotification() },
            )
        }
    }

    override fun onBalanceSelect(config: TokenBalanceSegmentedButtonConfig) {
        internalUiState.value = stateFactory.getStateWithUpdatedBalanceSegmentedButtonConfig(config)
    }

    override fun onConfirmDisposeExpressStatus() {
        internalUiState.value = stateFactory.getStateWithConfirmHideExpressStatus()
    }

    override fun onDisposeExpressStatus() {
        val bottomSheetState = internalUiState.value.bottomSheetConfig?.content
        if (bottomSheetState is ExpressStatusBottomSheetConfig) {
            modelScope.launch {
                expressStatusFactory.removeTransactionOnBottomSheetClosed(
                    expressState = bottomSheetState.value,
                    isForceDispose = true,
                )
            }
        }
        internalUiState.value = stateFactory.getStateWithClosedBottomSheet()
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

        internalUiState.value = stateFactory.getStateWithActionButtonErrorDialog(unavailabilityReason)

        return true
    }

    private fun openStaking() {
        modelScope.launch {
            getYieldUseCase.invoke(
                cryptoCurrencyId = cryptoCurrency.id,
                symbol = cryptoCurrency.symbol,
            ).onRight { yield ->
                router.openStaking(userWalletId, cryptoCurrency, yield.id)
            }.onLeft {
                Timber.e("Staking is unavailable for ${cryptoCurrency.name}")
                uiMessageSender.send(SnackbarMessage(resourceReference(R.string.staking_error_no_validators_title)))
            }
        }
    }

    private fun checkForActionUpdates() {
        combine(
            tokenDetailsDeepLinkActionListener.tokenDetailsActionFlow,
            waitForFirstExpressStatusEmmit.filter { it },
        ) { transactionId, _ -> transactionId }
            .onEach(::onExpressTransactionClick)
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

    private fun sendOneTimeBalanceLoadedAnalyticsEvent(cryptoCurrencyStatus: CryptoCurrencyStatus?) {
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
            ),
        )
        isBalanceLoadedEventSent = true
    }

    private suspend fun needShowYieldSupplyWarning(): Boolean {
        return yieldSupplyFeatureToggles.isYieldSupplyFeatureEnabled &&
            needShowYieldSupplyDepositedWarningUseCase(cryptoCurrencyStatus)
    }

    private fun isActiveYieldSupply(): Boolean {
        return yieldSupplyFeatureToggles.isYieldSupplyFeatureEnabled &&
            cryptoCurrencyStatus?.value?.yieldSupplyStatus?.isActive == true
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
                        currencyFrom = cryptoCurrency,
                        userWalletId = userWalletId,
                        screenSource = AnalyticsParam.ScreensSources.Token.value,
                    ),
                )
                TokenAction.Info -> Unit
            }
        }
    }

    private fun navigateToReceive() {
        val networkAddress = cryptoCurrencyStatus?.value?.networkAddress ?: return
        if (tokenReceiveFeatureToggle.isNewTokenReceiveEnabled) {
            modelScope.launch {
                configureReceiveAddresses(cryptoCurrencyStatus = cryptoCurrencyStatus)
                    ?.let { bottomSheetNavigation.activate(it) }
            }
        } else {
            analyticsEventsHandler.send(TokenReceiveAnalyticsEvent.ReceiveScreenOpened(cryptoCurrency.symbol))
            internalUiState.value = stateFactory.getStateWithReceiveBottomSheet(
                currency = cryptoCurrency,
                networkAddress = networkAddress,
                onCopyClick = {
                    analyticsEventsHandler.send(TokenReceiveAnalyticsEvent.ButtonCopyAddress(cryptoCurrency.symbol))
                    clipboardManager.setText(text = it, isSensitive = true)
                },
                onShareClick = {
                    analyticsEventsHandler.send(
                        TokenReceiveAnalyticsEvent.ButtonShareAddress(cryptoCurrency.symbol),
                    )
                    shareManager.shareText(text = it)
                },
            )
        }
    }

    private fun handleNavigationParam() {
        if (params.navigationAction is NavigationAction.Staking) {
            openStaking()
        }
    }

    private companion object {
        const val EXPRESS_STATUS_UPDATE_DELAY = 10_000L
    }
}