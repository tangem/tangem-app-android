package com.tangem.feature.tokendetails.presentation.tokendetails.model

import androidx.compose.runtime.Stable
import androidx.paging.cachedIn
import arrow.core.getOrElse
import arrow.core.merge
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
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
import com.tangem.core.ui.components.transactions.state.TxHistoryState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.haptic.TangemHapticEffect
import com.tangem.core.ui.haptic.VibratorHapticManager
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.demo.IsDemoCardUseCase
import com.tangem.domain.models.ReceiveAddressModel
import com.tangem.domain.models.TokenReceiveConfig
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
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
import com.tangem.domain.tokens.model.analytics.TokenReceiveAnalyticsEvent
import com.tangem.domain.tokens.model.analytics.TokenScreenAnalyticsEvent
import com.tangem.domain.tokens.model.analytics.TokenScreenAnalyticsEvent.Companion.toReasonAnalyticsText
import com.tangem.domain.tokens.model.analytics.TokenSwapPromoAnalyticsEvent
import com.tangem.domain.transaction.error.AssociateAssetError
import com.tangem.domain.transaction.error.IncompleteTransactionError
import com.tangem.domain.transaction.error.OpenTrustlineError
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.domain.transaction.usecase.*
import com.tangem.domain.txhistory.usecase.GetExplorerTransactionUrlUseCase
import com.tangem.domain.txhistory.usecase.GetTxHistoryItemsCountUseCase
import com.tangem.domain.txhistory.usecase.GetTxHistoryItemsUseCase
import com.tangem.domain.wallets.usecase.GetExploreUrlUseCase
import com.tangem.domain.wallets.usecase.GetExtendedPublicKeyForCurrencyUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.domain.wallets.usecase.NetworkHasDerivationUseCase
import com.tangem.feature.tokendetails.deeplink.TokenDetailsDeepLinkActionListener
import com.tangem.feature.tokendetails.presentation.router.InnerTokenDetailsRouter
import com.tangem.feature.tokendetails.presentation.tokendetails.analytics.TokenDetailsCurrencyStatusAnalyticsSender
import com.tangem.feature.tokendetails.presentation.tokendetails.analytics.TokenDetailsNotificationsAnalyticsSender
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenBalanceSegmentedButtonConfig
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.factory.TokenDetailsStateFactory
import com.tangem.feature.tokendetails.presentation.tokendetails.state.factory.express.ExpressStatusFactory
import com.tangem.features.send.v2.api.SendFeatureToggles
import com.tangem.features.tokendetails.TokenDetailsComponent
import com.tangem.features.tokendetails.impl.R
import com.tangem.features.tokenreceive.TokenReceiveFeatureToggle
import com.tangem.features.txhistory.entity.TxHistoryContentUpdateEmitter
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.*
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
    private val txHistoryItemsCountUseCase: GetTxHistoryItemsCountUseCase,
    private val txHistoryItemsUseCase: GetTxHistoryItemsUseCase,
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
    private val sendFeatureToggles: SendFeatureToggles,
    private val tokenReceiveFeatureToggle: TokenReceiveFeatureToggle,
    private val getViewedTokenReceiveWarningUseCase: GetViewedTokenReceiveWarningUseCase,
    private val getEnsNameUseCase: GetEnsNameUseCase,
) : Model(), TokenDetailsClickIntents {

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
    private val selectedAppCurrencyFlow: StateFlow<AppCurrency> = createSelectedAppCurrencyFlow()

    private var cryptoCurrencyStatus: CryptoCurrencyStatus? = null
    private var expressTxStatusTaskScheduler = SingleTaskScheduler<PersistentList<ExpressTransactionStateUM>>()

    /** Transaction id to check for status */
    private val waitForFirstExpressStatusEmmit = MutableStateFlow(false)

    val bottomSheetNavigation: SlotNavigation<TokenReceiveConfig> = SlotNavigation()

    private val stateFactory = TokenDetailsStateFactory(
        currentStateProvider = Provider { uiState.value },
        appCurrencyProvider = Provider(selectedAppCurrencyFlow::value),
        cryptoCurrencyStatusProvider = Provider { cryptoCurrencyStatus },
        clickIntents = this,
        networkHasDerivationUseCase = networkHasDerivationUseCase,
        getUserWalletUseCase = getUserWalletUseCase,
        userWalletId = userWalletId,
        symbol = cryptoCurrency.symbol,
        decimals = cryptoCurrency.decimals,
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
        analyticsEventsHandler.send(
            event = TokenScreenAnalyticsEvent.DetailsScreenOpened(token = cryptoCurrency.symbol),
        )
        updateTopBarMenu()
        initButtons()
        updateContent()
        handleBalanceHiding()
        checkForActionUpdates()
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
            val currentCryptoCurrencyStatus = getSingleCryptoCurrencyStatusUseCase.invokeMultiWalletSync(
                userWalletId = userWalletId,
                cryptoCurrencyId = cryptoCurrency.id,
                isSingleWalletWithTokens = false,
            ).getOrNull()
            currentCryptoCurrencyStatus?.let {
                cryptoCurrencyStatus = it
                updateButtons(it)
                updateWarnings(it)
            }
        }
    }

    private fun updateContent() {
        subscribeOnCurrencyStatusUpdates()
        subscribeOnExpressTransactionsUpdates()
        updateTxHistory(refresh = false, showItemsLoading = true, initialUpdating = true)
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
        getSingleCryptoCurrencyStatusUseCase.invokeMultiWallet(
            userWalletId = userWalletId,
            currencyId = cryptoCurrency.id,
            isSingleWalletWithTokens = userWallet is UserWallet.Cold &&
                userWallet.scanResponse.cardTypesResolver.isSingleWalletWithToken(),
        )
            .distinctUntilChanged()
            .onEach { maybeCurrencyStatus ->
                internalUiState.value = stateFactory.getCurrencyLoadedBalanceState(maybeCurrencyStatus)
                maybeCurrencyStatus.onRight { status ->
                    cryptoCurrencyStatus = status
                    updateButtons(currencyStatus = status)
                    updateWarnings(status)
                    subscribeOnUpdateStakingInfo(status)
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

    private fun updateNetworkToSwapBalance(toCryptoCurrency: CryptoCurrency) {
        modelScope.launch {
            updateDelayedCurrencyStatusUseCase(
                userWalletId = userWalletId,
                network = toCryptoCurrency.network,
            )
        }
    }

    /**
     * @param refresh - invalidate cache and get data from remote
     * @param showItemsLoading - show loading items placeholder.
     */
    private fun updateTxHistory(refresh: Boolean, showItemsLoading: Boolean, initialUpdating: Boolean = false) {
        modelScope.launch {
            if (!initialUpdating) {
                txHistoryContentUpdateEmitter.triggerUpdate()
            } else {
                val txHistoryItemsCountEither = txHistoryItemsCountUseCase(
                    userWalletId = userWalletId,
                    currency = cryptoCurrency,
                )

                // if countEither is left, handling error state run inside getLoadingTxHistoryState
                if (showItemsLoading || txHistoryItemsCountEither.isLeft()) {
                    internalUiState.value = stateFactory.getLoadingTxHistoryState(
                        itemsCountEither = txHistoryItemsCountEither,
                        pendingTransactions = internalUiState.value.pendingTxs,
                    )
                }

                txHistoryItemsCountEither.onRight {
                    val maybeTxHistory = txHistoryItemsUseCase(
                        userWalletId = userWalletId,
                        currency = cryptoCurrency,
                        refresh = refresh,
                    ).map { it.cachedIn(modelScope) }

                    internalUiState.value = stateFactory.getLoadedTxHistoryState(maybeTxHistory)
                }
            }
        }
    }

    private fun subscribeOnUpdateStakingInfo(cryptoCurrencyStatus: CryptoCurrencyStatus) {
        getStakingAvailabilityUseCase(
            userWalletId = userWalletId,
            cryptoCurrency = cryptoCurrency,
        )
            .map { it.getOrElse { StakingAvailability.Unavailable } }
            .distinctUntilChanged()
            .onEach {
                val stakingEntryInfo = if (it is StakingAvailability.Available) {
                    getStakingEntryInfoUseCase(
                        cryptoCurrencyId = cryptoCurrency.id,
                        symbol = cryptoCurrency.symbol,
                    ).getOrNull()
                } else {
                    null
                }

                internalUiState.update { state ->
                    stateFactory.getStakingInfoState(
                        state = state,
                        stakingEntryInfo = stakingEntryInfo,
                        stakingAvailability = it,
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
        internalUiState.value = stateFactory.getLoadingTxHistoryState()
        updateTxHistory(refresh = true, showItemsLoading = true)
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

        sendCurrency()
    }

    private fun sendCurrency() {
        val route = if (sendFeatureToggles.isSendWithSwapEnabled) {
            AppRoute.SendEntryPoint(
                currency = cryptoCurrency,
                userWalletId = userWallet.walletId,
            )
        } else {
            AppRoute.Send(
                currency = cryptoCurrency,
                userWalletId = userWallet.walletId,
            )
        }

        appRouter.push(route)
    }

    override fun onReceiveClick(unavailabilityReason: ScenarioUnavailabilityReason) {
        val networkAddress = cryptoCurrencyStatus?.value?.networkAddress ?: return

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
            if (tokenReceiveFeatureToggle.isNewTokenReceiveEnabled) {
                bottomSheetNavigation.activate(
                    configuration = configureReceiveAddresses(addresses = networkAddress),
                )
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

        appRouter.push(
            AppRoute.Swap(
                currencyFrom = cryptoCurrency,
                userWalletId = userWalletId,
                screenSource = AnalyticsParam.ScreensSources.Token.value,
            ),
        )
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
            removeCurrencyUseCase.invoke(userWalletId, cryptoCurrency)
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
                    updateTxHistory(
                        refresh = true,
                        showItemsLoading = internalUiState.value.txHistoryState !is TxHistoryState.Content,
                    )
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
                TokenSwapPromoAnalyticsEvent.PromotionBannerClicked(
                    source = AnalyticsParam.ScreensSources.Token,
                    programName = TokenSwapPromoAnalyticsEvent.ProgramName.Empty, // Use it on new promo action
                    action = TokenSwapPromoAnalyticsEvent.PromotionBannerClicked.BannerAction.Closed,
                ),
            )
        }
    }

    override fun onSwapPromoClick(promoId: PromoId) {
        modelScope.launch(dispatchers.main) {
            shouldShowPromoTokenUseCase.neverToShow(promoId)
            analyticsEventsHandler.send(
                TokenSwapPromoAnalyticsEvent.PromotionBannerClicked(
                    source = AnalyticsParam.ScreensSources.Token,
                    programName = TokenSwapPromoAnalyticsEvent.ProgramName.Empty, // Use it on new promo action
                    action = TokenSwapPromoAnalyticsEvent.PromotionBannerClicked.BannerAction.Clicked,
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
        analyticsEventsHandler.send(TokenReceiveAnalyticsEvent.ButtonCopyAddress(cryptoCurrency.symbol))
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

    private fun handleUnavailabilityReason(unavailabilityReason: ScenarioUnavailabilityReason): Boolean {
        if (unavailabilityReason == ScenarioUnavailabilityReason.None) return false

        internalUiState.value = stateFactory.getStateWithActionButtonErrorDialog(unavailabilityReason)

        return true
    }

    private fun openStaking() {
        modelScope.launch {
            val yield = getYieldUseCase.invoke(
                cryptoCurrencyId = cryptoCurrency.id,
                symbol = cryptoCurrency.symbol,
            ).getOrElse {
                error("Staking is unavailable for ${cryptoCurrency.name}")
            }

            router.openStaking(userWalletId, cryptoCurrency, yield.id)
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

    private suspend fun configureReceiveAddresses(addresses: NetworkAddress): TokenReceiveConfig {
        val ensName = getEnsNameUseCase.invoke(
            userWalletId = userWalletId,
            network = cryptoCurrency.network,
            address = addresses.defaultAddress.value,
        )

        val receiveAddresses = buildList {
            ensName?.let { ens ->
                add(
                    ReceiveAddressModel(
                        nameService = ReceiveAddressModel.NameService.Ens,
                        value = ens,
                        displayName = ens,
                    ),
                )
            }
            addresses.availableAddresses.map { address ->
                add(
                    ReceiveAddressModel(
                        nameService = ReceiveAddressModel.NameService.Default,
                        value = address.value,
                        displayName = "${cryptoCurrency.name} (${cryptoCurrency.symbol})",
                    ),
                )
            }
        }

        return TokenReceiveConfig(
            shouldShowWarning = cryptoCurrency.name !in getViewedTokenReceiveWarningUseCase(),
            cryptoCurrency = cryptoCurrency,
            userWalletId = userWalletId,
            showMemoDisclaimer = cryptoCurrency.network.transactionExtrasType != Network.TransactionExtrasType.NONE,
            receiveAddress = receiveAddresses,
        )
    }

    private companion object {
        const val EXPRESS_STATUS_UPDATE_DELAY = 10_000L
    }
}