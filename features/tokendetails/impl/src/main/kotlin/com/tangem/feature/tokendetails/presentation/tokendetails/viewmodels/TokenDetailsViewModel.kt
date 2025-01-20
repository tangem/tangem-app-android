package com.tangem.feature.tokendetails.presentation.tokendetails.viewmodels

import android.os.Bundle
import androidx.lifecycle.*
import androidx.paging.cachedIn
import arrow.core.getOrElse
import com.tangem.blockchain.common.address.AddressType
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.routing.bundle.unbundle
import com.tangem.common.ui.expressStatus.ExpressStatusBottomSheetConfig
import com.tangem.common.ui.expressStatus.state.ExpressTransactionStateUM
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.decompose.di.GlobalUiMessageSender
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.deeplink.DeepLinksRegistry
import com.tangem.core.deeplink.global.BuyCurrencyDeepLink
import com.tangem.core.navigation.share.ShareManager
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.core.ui.components.bottomsheets.tokenreceive.AddressModel
import com.tangem.core.ui.components.bottomsheets.tokenreceive.mapToAddressModels
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
import com.tangem.domain.card.GetExtendedPublicKeyForCurrencyUseCase
import com.tangem.domain.card.NetworkHasDerivationUseCase
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.demo.IsDemoCardUseCase
import com.tangem.domain.onramp.model.OnrampSource
import com.tangem.domain.promo.ShouldShowSwapPromoTokenUseCase
import com.tangem.domain.redux.ReduxStateHolder
import com.tangem.domain.staking.GetStakingAvailabilityUseCase
import com.tangem.domain.staking.GetStakingEntryInfoUseCase
import com.tangem.domain.staking.GetStakingIntegrationIdUseCase
import com.tangem.domain.staking.GetYieldUseCase
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.model.StakingEntryInfo
import com.tangem.domain.tokens.*
import com.tangem.domain.tokens.legacy.TradeCryptoAction
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.NetworkAddress
import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason
import com.tangem.domain.tokens.model.analytics.TokenReceiveAnalyticsEvent
import com.tangem.domain.tokens.model.analytics.TokenScreenAnalyticsEvent
import com.tangem.domain.tokens.model.analytics.TokenScreenAnalyticsEvent.Companion.toReasonAnalyticsText
import com.tangem.domain.tokens.model.analytics.TokenSwapPromoAnalyticsEvent
import com.tangem.domain.transaction.error.AssociateAssetError
import com.tangem.domain.transaction.error.IncompleteTransactionError
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.domain.transaction.usecase.AssociateAssetUseCase
import com.tangem.domain.transaction.usecase.DismissIncompleteTransactionUseCase
import com.tangem.domain.transaction.usecase.RetryIncompleteTransactionUseCase
import com.tangem.domain.txhistory.usecase.GetExplorerTransactionUrlUseCase
import com.tangem.domain.txhistory.usecase.GetTxHistoryItemsCountUseCase
import com.tangem.domain.txhistory.usecase.GetTxHistoryItemsUseCase
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.usecase.GetExploreUrlUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.feature.tokendetails.presentation.router.InnerTokenDetailsRouter
import com.tangem.feature.tokendetails.presentation.tokendetails.analytics.TokenDetailsCurrencyStatusAnalyticsSender
import com.tangem.feature.tokendetails.presentation.tokendetails.analytics.TokenDetailsNotificationsAnalyticsSender
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenBalanceSegmentedButtonConfig
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.factory.TokenDetailsStateFactory
import com.tangem.feature.tokendetails.presentation.tokendetails.state.factory.express.ExpressStatusFactory
import com.tangem.features.onramp.OnrampFeatureToggles
import com.tangem.features.tokendetails.impl.R
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@Suppress("LongParameterList", "LargeClass", "TooManyFunctions")
@HiltViewModel
internal class TokenDetailsViewModel @Inject constructor(
    private val dispatchers: CoroutineDispatcherProvider,
    private val getCurrencyStatusUpdatesUseCase: GetCurrencyStatusUpdatesUseCase,
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
    private val shouldShowSwapPromoTokenUseCase: ShouldShowSwapPromoTokenUseCase,
    private val updateDelayedCurrencyStatusUseCase: UpdateDelayedNetworkStatusUseCase,
    private val getExtendedPublicKeyForCurrencyUseCase: GetExtendedPublicKeyForCurrencyUseCase,
    private val getStakingEntryInfoUseCase: GetStakingEntryInfoUseCase,
    private val getStakingAvailabilityUseCase: GetStakingAvailabilityUseCase,
    private val getYieldUseCase: GetYieldUseCase,
    private val networkHasDerivationUseCase: NetworkHasDerivationUseCase,
    private val isDemoCardUseCase: IsDemoCardUseCase,
    private val associateAssetUseCase: AssociateAssetUseCase,
    private val retryIncompleteTransactionUseCase: RetryIncompleteTransactionUseCase,
    private val dismissIncompleteTransactionUseCase: DismissIncompleteTransactionUseCase,
    private val reduxStateHolder: ReduxStateHolder,
    private val analyticsEventsHandler: AnalyticsEventHandler,
    private val vibratorHapticManager: VibratorHapticManager,
    private val clipboardManager: ClipboardManager,
    private val getCryptoCurrencySyncUseCase: GetCryptoCurrencyStatusSyncUseCase,
    private val onrampFeatureToggles: OnrampFeatureToggles,
    private val shareManager: ShareManager,
    @GlobalUiMessageSender private val uiMessageSender: UiMessageSender,
    expressStatusFactory: ExpressStatusFactory.Factory,
    getUserWalletUseCase: GetUserWalletUseCase,
    getStakingIntegrationIdUseCase: GetStakingIntegrationIdUseCase,
    deepLinksRegistry: DeepLinksRegistry,
    savedStateHandle: SavedStateHandle,
    private val appRouter: AppRouter,
) : ViewModel(), DefaultLifecycleObserver, TokenDetailsClickIntents {

    private val userWalletId: UserWalletId = savedStateHandle.get<Bundle>(AppRoute.CurrencyDetails.USER_WALLET_ID_KEY)
        ?.unbundle(UserWalletId.serializer())
        ?: error("This screen can't be opened without `UserWalletId`")

    private val cryptoCurrency: CryptoCurrency =
        savedStateHandle.get<Bundle>(AppRoute.CurrencyDetails.CRYPTO_CURRENCY_KEY)
            ?.unbundle(CryptoCurrency.serializer())
            ?: error("This screen can't be opened without `CryptoCurrency`")

    private val userWallet: UserWallet

    lateinit var router: InnerTokenDetailsRouter

    private val marketPriceJobHolder = JobHolder()
    private val refreshStateJobHolder = JobHolder()
    private val warningsJobHolder = JobHolder()
    private val expressTxJobHolder = JobHolder()
    private val selectedAppCurrencyFlow: StateFlow<AppCurrency> = createSelectedAppCurrencyFlow()

    private var cryptoCurrencyStatus: CryptoCurrencyStatus? = null
    private var stakingEntryInfo: StakingEntryInfo? = null
    private var stakingAvailability: StakingAvailability = StakingAvailability.Unavailable
    private var expressTxStatusTaskScheduler = SingleTaskScheduler<PersistentList<ExpressTransactionStateUM>>()

    private val stateFactory = TokenDetailsStateFactory(
        currentStateProvider = Provider { uiState.value },
        appCurrencyProvider = Provider(selectedAppCurrencyFlow::value),
        stakingEntryInfoProvider = Provider { stakingEntryInfo },
        stakingAvailabilityProvider = Provider { stakingAvailability },
        cryptoCurrencyStatusProvider = Provider { cryptoCurrencyStatus },
        clickIntents = this,
        networkHasDerivationUseCase = networkHasDerivationUseCase,
        getUserWalletUseCase = getUserWalletUseCase,
        userWalletId = userWalletId,
        getStakingIntegrationIdUseCase = getStakingIntegrationIdUseCase,
        symbol = cryptoCurrency.symbol,
        decimals = cryptoCurrency.decimals,
    )

    private val expressStatusFactory by lazy(mode = LazyThreadSafetyMode.NONE) {
        expressStatusFactory.create(
            clickIntents = this,
            appCurrencyProvider = Provider { selectedAppCurrencyFlow.value },
            currentStateProvider = Provider { uiState.value },
            cryptoCurrencyStatusProvider = Provider { cryptoCurrencyStatus },
            userWalletId = userWalletId,
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
        deepLinksRegistry.registerWithViewModel(
            viewModel = this,
            deepLinks = listOf(
                BuyCurrencyDeepLink(
                    onReceive = ::onBuyCurrencyDeepLink,
                ),
            ),
        )
        userWallet = getUserWalletUseCase(userWalletId).getOrNull() ?: error("UserWallet not found")
    }

    private fun onBuyCurrencyDeepLink(externalTxId: String) {
        if (onrampFeatureToggles.isFeatureEnabled) {
            router.openOnrampSuccess(externalTxId)
        } else {
            val currency = cryptoCurrencyStatus?.currency ?: return
            analyticsEventsHandler.send(TokenScreenAnalyticsEvent.Bought(currency.symbol))
        }
    }

    override fun onCreate(owner: LifecycleOwner) {
        analyticsEventsHandler.send(
            event = TokenScreenAnalyticsEvent.DetailsScreenOpened(token = cryptoCurrency.symbol),
        )
        updateTopBarMenu()
        initButtons()
        updateContent()
        handleBalanceHiding(owner)
    }

    override fun onPause(owner: LifecycleOwner) {
        expressTxStatusTaskScheduler.cancelTask()
        expressTxJobHolder.cancel()
        super.onPause(owner)
    }

    override fun onResume(owner: LifecycleOwner) {
        subscribeOnExpressTransactionsUpdates()
        super.onResume(owner)
    }

    override fun onCleared() {
        expressTxStatusTaskScheduler.cancelTask()
        expressTxJobHolder.cancel()
        super.onCleared()
    }

    private fun initButtons() {
        // we need also init buttons before start all loading to avoid buttons blocking
        viewModelScope.launch {
            val currentCryptoCurrencyStatus = getCryptoCurrencySyncUseCase.invoke(
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
        updateTxHistory(refresh = false, showItemsLoading = true)

        updateStakingInfo()
    }

    private fun handleBalanceHiding(owner: LifecycleOwner) {
        getBalanceHidingSettingsUseCase()
            .flowWithLifecycle(owner.lifecycle)
            .onEach {
                internalUiState.value = stateFactory.getStateWithUpdatedHidden(
                    isBalanceHidden = it.isBalanceHidden,
                )
            }
            .launchIn(viewModelScope)
    }

    private suspend fun updateButtons(currencyStatus: CryptoCurrencyStatus) {
        getCryptoCurrencyActionsUseCase(
            userWallet = userWallet,
            cryptoCurrencyStatus = currencyStatus,
        )
            .conflate()
            .distinctUntilChanged()
            .onEach {
                internalUiState.value = stateFactory.getManageButtonsState(actions = it.states)
            }
            .flowOn(dispatchers.main)
            .launchIn(viewModelScope)
    }

    private fun updateWarnings(cryptoCurrencyStatus: CryptoCurrencyStatus) {
        viewModelScope.launch(dispatchers.main) {
            getCurrencyWarningsUseCase.invoke(
                userWalletId = userWalletId,
                currencyStatus = cryptoCurrencyStatus,
                derivationPath = cryptoCurrency.network.derivationPath,
                isSingleWalletWithTokens = userWallet.scanResponse.cardTypesResolver.isSingleWalletWithToken(),
            )
                .distinctUntilChanged()
                .onEach {
                    val updatedState = stateFactory.getStateWithNotifications(it)
                    notificationsAnalyticsSender.send(internalUiState.value, updatedState.notifications)
                    internalUiState.value = updatedState
                }
                .launchIn(viewModelScope)
                .saveIn(warningsJobHolder)
        }
    }

    private fun subscribeOnCurrencyStatusUpdates() {
        viewModelScope.launch(dispatchers.main) {
            getCurrencyStatusUpdatesUseCase(
                userWalletId = userWalletId,
                currencyId = cryptoCurrency.id,
                isSingleWalletWithTokens = userWallet.scanResponse.cardTypesResolver.isSingleWalletWithToken(),
            )
                .distinctUntilChanged()
                .onEach { maybeCurrencyStatus ->
                    internalUiState.value = stateFactory.getCurrencyLoadedBalanceState(maybeCurrencyStatus)
                    maybeCurrencyStatus.onRight { status ->
                        cryptoCurrencyStatus = status
                        updateButtons(currencyStatus = status)
                        updateWarnings(status)
                    }
                    currencyStatusAnalyticsSender.send(maybeCurrencyStatus)
                }
                .flowOn(dispatchers.main)
                .launchIn(viewModelScope)
                .saveIn(marketPriceJobHolder)
        }
    }

    private fun subscribeOnExpressTransactionsUpdates() {
        viewModelScope.launch(dispatchers.main) {
            expressTxStatusTaskScheduler.cancelTask()
            expressStatusFactory
                .getExpressStatuses()
                .distinctUntilChanged()
                .onEach { expressTxs ->
                    internalUiState.value = expressStatusFactory.getStateWithUpdatedExpressTxs(
                        expressTxs,
                        ::updateNetworkToSwapBalance,
                    )
                    expressTxStatusTaskScheduler.scheduleTask(
                        viewModelScope,
                        PeriodicTask(
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
                .launchIn(viewModelScope)
                .saveIn(expressTxJobHolder)
        }
    }

    private fun updateNetworkToSwapBalance(toCryptoCurrency: CryptoCurrency) {
        viewModelScope.launch {
            updateDelayedCurrencyStatusUseCase(
                userWalletId = userWalletId,
                network = toCryptoCurrency.network,
                refresh = true,
            )
        }
    }

    /**
     * @param refresh - invalidate cache and get data from remote
     * @param showItemsLoading - show loading items placeholder.
     */
    private fun updateTxHistory(refresh: Boolean, showItemsLoading: Boolean) {
        viewModelScope.launch(dispatchers.main) {
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
                ).map { it.cachedIn(viewModelScope) }

                internalUiState.value = stateFactory.getLoadedTxHistoryState(maybeTxHistory)
            }
        }
    }

    private fun updateStakingInfo() {
        viewModelScope.launch {
            val availability = getStakingAvailabilityUseCase(
                userWalletId = userWalletId,
                cryptoCurrency = cryptoCurrency,
            ).getOrElse { StakingAvailability.Unavailable }

            stakingAvailability = availability

            if (stakingAvailability is StakingAvailability.Available) {
                val stakingInfo = getStakingEntryInfoUseCase(
                    cryptoCurrencyId = cryptoCurrency.id,
                    symbol = cryptoCurrency.symbol,
                )

                stakingEntryInfo = stakingInfo.getOrNull()
            }
        }
    }

    private fun updateTopBarMenu() {
        viewModelScope.launch(dispatchers.main) {
            val hasDerivations =
                networkHasDerivationUseCase(userWallet.scanResponse, cryptoCurrency.network).getOrElse { false }

            val isSupported = getExtendedPublicKeyForCurrencyUseCase.isSupported(userWalletId, cryptoCurrency.network)

            internalUiState.value = stateFactory.getStateWithUpdatedMenu(
                cardTypesResolver = userWallet.scanResponse.cardTypesResolver,
                hasDerivations = hasDerivations,
                isSupported = isSupported,
            )
        }
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

    override fun onBackClick() {
        router.popBackStack()
    }

    override fun onBuyClick(unavailabilityReason: ScenarioUnavailabilityReason) {
        analyticsEventsHandler.send(TokenScreenAnalyticsEvent.ButtonBuy(cryptoCurrency.symbol))

        if (handleUnavailabilityReason(
                unavailabilityReason = unavailabilityReason,
                tokenSymbol = cryptoCurrency.symbol,
                tokenAction = TokenScreenAnalyticsEvent.NoticeActionInactive.TokenAction.BuyAction,
            )
        ) {
            return
        }

        val status = cryptoCurrencyStatus ?: return
        if (onrampFeatureToggles.isFeatureEnabled) {
            viewModelScope.launch(dispatchers.main) {
                reduxStateHolder.dispatch(
                    TradeCryptoAction.Buy(
                        userWallet = userWallet,
                        source = OnrampSource.TOKEN_DETAILS,
                        cryptoCurrencyStatus = status,
                        appCurrencyCode = selectedAppCurrencyFlow.value.code,
                    ),
                )
            }
        } else {
            showErrorIfDemoModeOrElse {
                viewModelScope.launch(dispatchers.main) {
                    reduxStateHolder.dispatch(
                        TradeCryptoAction.Buy(
                            userWallet = userWallet,
                            source = OnrampSource.TOKEN_DETAILS,
                            cryptoCurrencyStatus = status,
                            appCurrencyCode = selectedAppCurrencyFlow.value.code,
                        ),
                    )
                }
            }
        }
    }

    override fun onBuyCoinClick(cryptoCurrency: CryptoCurrency) {
        analyticsEventsHandler.send(TokenScreenAnalyticsEvent.ButtonBuy(cryptoCurrency.symbol))
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
        analyticsEventsHandler.send(TokenScreenAnalyticsEvent.ButtonSend(cryptoCurrency.symbol))

        if (handleUnavailabilityReason(
                unavailabilityReason = unavailabilityReason,
                tokenSymbol = cryptoCurrency.symbol,
                tokenAction = TokenScreenAnalyticsEvent.NoticeActionInactive.TokenAction.SendAction,
            )
        ) {
            return
        }

        sendCurrency()
    }

    private fun sendCurrency() {
        val route = AppRoute.Send(
            currency = cryptoCurrency,
            userWalletId = userWallet.walletId,
        )

        appRouter.push(route)
    }

    override fun onReceiveClick(unavailabilityReason: ScenarioUnavailabilityReason) {
        val networkAddress = cryptoCurrencyStatus?.value?.networkAddress ?: return

        if (handleUnavailabilityReason(
                unavailabilityReason = unavailabilityReason,
                tokenSymbol = cryptoCurrency.symbol,
                tokenAction = TokenScreenAnalyticsEvent.NoticeActionInactive.TokenAction.ReceiveAction,
            )
        ) {
            return
        }

        viewModelScope.launch(dispatchers.main) {
            analyticsEventsHandler.send(TokenScreenAnalyticsEvent.ButtonReceive(cryptoCurrency.symbol))
            analyticsEventsHandler.send(TokenReceiveAnalyticsEvent.ReceiveScreenOpened(cryptoCurrency.symbol))

            internalUiState.value = stateFactory.getStateWithReceiveBottomSheet(
                currency = cryptoCurrency,
                networkAddress = networkAddress,
                onCopyClick = {
                    analyticsEventsHandler.send(TokenReceiveAnalyticsEvent.ButtonCopyAddress(cryptoCurrency.symbol))
                    clipboardManager.setText(text = it, isSensitive = true)
                },
                onShareClick = {
                    analyticsEventsHandler.send(TokenReceiveAnalyticsEvent.ButtonShareAddress(cryptoCurrency.symbol))
                    shareManager.shareText(text = it)
                },
            )
        }
    }

    override fun onStakeClick(unavailabilityReason: ScenarioUnavailabilityReason) {
        if (handleUnavailabilityReason(
                unavailabilityReason = unavailabilityReason,
                tokenSymbol = cryptoCurrency.symbol,
                tokenAction = null,
            )
        ) {
            return
        }

        openStaking()
    }

    override fun onGenerateExtendedKey() {
        viewModelScope.launch(dispatchers.main) {
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
        analyticsEventsHandler.send(TokenScreenAnalyticsEvent.ButtonSell(cryptoCurrency.symbol))

        if (handleUnavailabilityReason(
                unavailabilityReason = unavailabilityReason,
                tokenSymbol = cryptoCurrency.symbol,
                tokenAction = TokenScreenAnalyticsEvent.NoticeActionInactive.TokenAction.SellAction,
            )
        ) {
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
        analyticsEventsHandler.send(TokenScreenAnalyticsEvent.ButtonExchange(cryptoCurrency.symbol))

        if (handleUnavailabilityReason(
                unavailabilityReason = unavailabilityReason,
                tokenSymbol = cryptoCurrency.symbol,
                tokenAction = TokenScreenAnalyticsEvent.NoticeActionInactive.TokenAction.SwapAction,
            )
        ) {
            return
        }

        appRouter.push(AppRoute.Swap(currencyFrom = cryptoCurrency, userWalletId = userWalletId))
    }

    override fun onDismissDialog() {
        internalUiState.value = stateFactory.getStateWithClosedDialog()
    }

    override fun onHideClick() {
        analyticsEventsHandler.send(TokenScreenAnalyticsEvent.ButtonRemoveToken(cryptoCurrency.symbol))

        viewModelScope.launch {
            val hasLinkedTokens = removeCurrencyUseCase.hasLinkedTokens(userWalletId, cryptoCurrency)
            internalUiState.value = if (hasLinkedTokens) {
                stateFactory.getStateWithLinkedTokensDialog(cryptoCurrency)
            } else {
                stateFactory.getStateWithConfirmHideTokenDialog(cryptoCurrency)
            }
        }
    }

    override fun onHideConfirmed() {
        viewModelScope.launch {
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

        viewModelScope.launch(dispatchers.main) {
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
        if (isDemoCardUseCase(cardId = userWallet.cardId)) {
            internalUiState.value = stateFactory.getStateWithClosedBottomSheet()

            uiMessageSender.send(
                message = SnackbarMessage(message = resourceReference(R.string.alert_demo_feature_disabled)),
            )
        } else {
            action()
        }
    }

    override fun onAddressTypeSelected(addressModel: AddressModel) {
        viewModelScope.launch {
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

        viewModelScope.launch(dispatchers.main) {
            listOf(
                async {
                    fetchCurrencyStatusUseCase(
                        userWalletId = userWalletId,
                        id = cryptoCurrency.id,
                        refresh = true,
                    )
                },
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
                viewModelScope.launch(dispatchers.main) {
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
        val expressTxState = internalUiState.value.expressTxsToDisplay.first { it.info.txId == txId }
        internalUiState.value = expressStatusFactory.getStateWithExpressStatusBottomSheet(expressTxState)
    }

    override fun onGoToProviderClick(url: String) {
        router.openUrl(url)
    }

    override fun onGoToRefundedTokenClick(cryptoCurrency: CryptoCurrency) {
        val bottomSheetState = internalUiState.value.bottomSheetConfig?.content
        if (bottomSheetState is ExpressStatusBottomSheetConfig) {
            viewModelScope.launch {
                expressStatusFactory.removeTransactionOnBottomSheetClosed(
                    expressState = bottomSheetState.value,
                    isForceTerminal = true,
                )
            }
        }
        internalUiState.value = stateFactory.getStateWithClosedBottomSheet()
        router.openTokenDetails(userWalletId, cryptoCurrency)
    }

    override fun onOpenUrlClick(url: String) {
        router.openUrl(url)
    }

    override fun onSwapPromoDismiss() {
        viewModelScope.launch(dispatchers.main) {
            shouldShowSwapPromoTokenUseCase.neverToShow()
            analyticsEventsHandler.send(
                TokenSwapPromoAnalyticsEvent.PromotionBannerClicked(
                    source = AnalyticsParam.ScreensSources.Token,
                    programName = TokenSwapPromoAnalyticsEvent.ProgramName.OKX,
                    action = TokenSwapPromoAnalyticsEvent.PromotionBannerClicked.BannerAction.Closed,
                ),
            )
        }
    }

    override fun onSwapPromoClick() {
        viewModelScope.launch(dispatchers.main) {
            shouldShowSwapPromoTokenUseCase.neverToShow()
            analyticsEventsHandler.send(
                TokenSwapPromoAnalyticsEvent.PromotionBannerClicked(
                    source = AnalyticsParam.ScreensSources.Token,
                    programName = TokenSwapPromoAnalyticsEvent.ProgramName.OKX,
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
        viewModelScope.launch {
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

    override fun onDismissIncompleteTransactionClick() {
        analyticsEventsHandler.send(
            TokenScreenAnalyticsEvent.RevealCancel(
                tokenSymbol = cryptoCurrency.symbol,
                blockchain = cryptoCurrency.network.name,
            ),
        )
        viewModelScope.launch {
            internalUiState.value = stateFactory.getStateWithDismissIncompleteTransactionConfirmDialog()
        }
    }

    override fun onConfirmDismissIncompleteTransactionClick() {
        viewModelScope.launch {
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
        viewModelScope.launch(dispatchers.io) {
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

    private fun handleUnavailabilityReason(
        unavailabilityReason: ScenarioUnavailabilityReason,
        tokenSymbol: String,
        tokenAction: TokenScreenAnalyticsEvent.NoticeActionInactive.TokenAction?,
    ): Boolean {
        if (unavailabilityReason == ScenarioUnavailabilityReason.None) return false
        if (tokenAction != null) {
            analyticsEventsHandler.send(
                TokenScreenAnalyticsEvent.NoticeActionInactive(
                    token = tokenSymbol,
                    tokenAction = tokenAction,
                    reason = unavailabilityReason.toReasonAnalyticsText(),
                ),
            )
        }

        internalUiState.value = stateFactory.getStateWithActionButtonErrorDialog(unavailabilityReason)

        return true
    }

    private fun openStaking() {
        viewModelScope.launch {
            val yield = getYieldUseCase.invoke(
                cryptoCurrencyId = cryptoCurrency.id,
                symbol = cryptoCurrency.symbol,
            ).getOrElse {
                error("Staking is unavailable for ${cryptoCurrency.name}")
            }

            router.openStaking(userWalletId, cryptoCurrency, yield)
        }
    }

    private companion object {
        const val EXPRESS_STATUS_UPDATE_DELAY = 10_000L
    }
}