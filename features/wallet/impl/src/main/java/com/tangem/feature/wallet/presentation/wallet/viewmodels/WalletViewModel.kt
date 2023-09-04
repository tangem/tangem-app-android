package com.tangem.feature.wallet.presentation.wallet.viewmodels

import androidx.lifecycle.*
import androidx.paging.cachedIn
import arrow.core.getOrElse
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.derivation.DerivationStyle
import com.tangem.common.Provider
import com.tangem.common.doOnFailure
import com.tangem.common.doOnSuccess
import com.tangem.core.navigation.AppScreen
import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.card.*
import com.tangem.domain.common.CardTypesResolver
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.common.util.derivationStyleProvider
import com.tangem.domain.demo.IsDemoCardUseCase
import com.tangem.domain.redux.ReduxStateHolder
import com.tangem.domain.settings.CanUseBiometryUseCase
import com.tangem.domain.settings.IsUserAlreadyRateAppUseCase
import com.tangem.domain.settings.ShouldShowSaveWalletScreenUseCase
import com.tangem.domain.tokens.*
import com.tangem.domain.tokens.legacy.TradeCryptoAction
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.TokenList
import com.tangem.domain.tokens.models.CryptoCurrency
import com.tangem.domain.tokens.models.Network
import com.tangem.domain.txhistory.usecase.GetTxHistoryItemsCountUseCase
import com.tangem.domain.txhistory.usecase.GetTxHistoryItemsUseCase
import com.tangem.domain.userwallets.UserWalletBuilder
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.usecase.*
import com.tangem.feature.wallet.presentation.common.state.TokenItemState
import com.tangem.feature.wallet.presentation.router.InnerWalletRouter
import com.tangem.feature.wallet.presentation.wallet.state.WalletLockedState
import com.tangem.feature.wallet.presentation.wallet.state.WalletMultiCurrencyState
import com.tangem.feature.wallet.presentation.wallet.state.WalletSingleCurrencyState
import com.tangem.feature.wallet.presentation.wallet.state.WalletState
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletBottomSheetConfig
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletCardState
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletTokensListState
import com.tangem.feature.wallet.presentation.wallet.state.factory.WalletStateFactory
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.properties.Delegates

/**
 * Wallet screen view model
 *
[REDACTED_AUTHOR]
 */
@Suppress("LargeClass", "LongParameterList", "TooManyFunctions")
@HiltViewModel
internal class WalletViewModel @Inject constructor(
    private val getWalletsUseCase: GetWalletsUseCase,
    private val saveWalletUseCase: SaveWalletUseCase,
    private val getSelectedWalletUseCase: GetSelectedWalletUseCase,
    private val selectWalletUseCase: SelectWalletUseCase,
    private val updateWalletUseCase: UpdateWalletUseCase,
    private val deleteWalletUseCase: DeleteWalletUseCase,
    private val getBiometricsStatusUseCase: GetBiometricsStatusUseCase,
    private val setAccessCodeRequestPolicyUseCase: SetAccessCodeRequestPolicyUseCase,
    private val getAccessCodeSavingStatusUseCase: GetAccessCodeSavingStatusUseCase,
    private val getTokenListUseCase: GetTokenListUseCase,
    private val fetchTokenListUseCase: FetchTokenListUseCase,
    private val getPrimaryCurrencyStatusUpdatesUseCase: GetPrimaryCurrencyStatusUpdatesUseCase,
    private val fetchCurrencyStatusUseCase: FetchCurrencyStatusUseCase,
    private val getNetworkCoinStatusUseCase: GetNetworkCoinStatusUseCase,
    private val getCardWasScannedUseCase: GetCardWasScannedUseCase,
    private val isUserAlreadyRateAppUseCase: IsUserAlreadyRateAppUseCase,
    private val isDemoCardUseCase: IsDemoCardUseCase,
    private val scanCardProcessor: ScanCardProcessor,
    private val txHistoryItemsCountUseCase: GetTxHistoryItemsCountUseCase,
    private val txHistoryItemsUseCase: GetTxHistoryItemsUseCase,
    private val getExploreUrlUseCase: GetExploreUrlUseCase,
    private val unlockWalletsUseCase: UnlockWalletsUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val getCryptoCurrencyActionsUseCase: GetCryptoCurrencyActionsUseCase,
    private val shouldShowSaveWalletScreenUseCase: ShouldShowSaveWalletScreenUseCase,
    private val canUseBiometryUseCase: CanUseBiometryUseCase,
    private val shouldSaveUserWalletsUseCase: ShouldSaveUserWalletsUseCase,
    private val reduxStateHolder: ReduxStateHolder,
    private val dispatchers: CoroutineDispatcherProvider,
) : ViewModel(), DefaultLifecycleObserver, WalletClickIntents {

    /** Feature router */
    var router: InnerWalletRouter by Delegates.notNull()

    private val selectedAppCurrencyFlow: StateFlow<AppCurrency> = createSelectedAppCurrencyFlow()

    private val notificationsListFactory = WalletNotificationsListFactory(
        wasCardScannedCallback = getCardWasScannedUseCase::invoke,
        isUserAlreadyRateAppCallback = isUserAlreadyRateAppUseCase::invoke,
        isDemoCardCallback = isDemoCardUseCase::invoke,
        clickIntents = this,
    )

    private val stateFactory = WalletStateFactory(
        currentStateProvider = Provider { uiState },
        currentCardTypeResolverProvider = Provider {
            getCardTypeResolver(
                index = requireNotNull(uiState as? WalletState.ContentState).walletsListConfig.selectedWalletIndex,
            )
        },
        currentWalletProvider = Provider {
            wallets[requireNotNull(uiState as? WalletState.ContentState).walletsListConfig.selectedWalletIndex]
        },
        appCurrencyProvider = Provider(selectedAppCurrencyFlow::value),
        clickIntents = this,
    )

    /** Screen state */
    var uiState: WalletState by uiStateHolder(initialState = stateFactory.getInitialState())

    private var wallets: List<UserWallet> by Delegates.notNull()
    private var singleWalletCryptoCurrencyStatus: CryptoCurrencyStatus? = null

    private val tokensJobHolder = JobHolder()
    private val marketPriceJobHolder = JobHolder()
    private val buttonsJobHolder = JobHolder()
    private val notificationsJobHolder = JobHolder()
    private val refreshContentJobHolder = JobHolder()

    override fun onCreate(owner: LifecycleOwner) {
        viewModelScope.launch(dispatchers.main) {
            delay(timeMillis = 1_800)

            if (router.isWalletLastScreen() && shouldShowSaveWalletScreenUseCase() && canUseBiometryUseCase()) {
                router.openSaveUserWalletScreen()
            }
        }

        getWalletsUseCase()
            .flowWithLifecycle(owner.lifecycle)
            .distinctUntilChanged()
            .onEach(::updateWallets)
            .flowOn(dispatchers.io)
            .launchIn(viewModelScope)
    }

    private fun updateWallets(sourceList: List<UserWallet>) {
        if (sourceList.isEmpty()) return

        wallets = sourceList

        val currentState = uiState
        val previousSelectedWalletIndex = (currentState as? WalletState.ContentState)
            ?.walletsListConfig
            ?.selectedWalletIndex

        val selectedWalletIndex = if (currentState is WalletLockedState) {
            currentState.getSelectedWalletIndex()
        } else {
            val selectedWallet = getSelectedWalletUseCase().fold(
                ifLeft = { error("Selected wallet is null") },
                ifRight = { it },
            )
            sourceList.indexOfFirst { it.walletId == selectedWallet.walletId }
        }

        if (previousSelectedWalletIndex != selectedWalletIndex) {
            uiState = stateFactory.getSkeletonState(wallets = sourceList, selectedWalletIndex = selectedWalletIndex)

            getContentItemsUpdates(index = selectedWalletIndex)
        }
    }

    override fun onBackClick() {
        viewModelScope.launch(dispatchers.main) {
            router.popBackStack(screen = if (shouldSaveUserWalletsUseCase()) AppScreen.Welcome else AppScreen.Home)
        }
    }

    override fun onScanCardClick() {
        val prevRequestPolicyStatus = getBiometricsStatusUseCase()

        // Update access the code policy according access code saving status
        setAccessCodeRequestPolicyUseCase(isBiometricsRequestPolicy = getAccessCodeSavingStatusUseCase())

        viewModelScope.launch(dispatchers.io) {
            scanCardProcessor.scan(allowsRequestAccessCodeFromRepository = true)
                .doOnSuccess {
                    // If card's public key is null then user wallet will be null
                    val userWallet = UserWalletBuilder(scanResponse = it).build()

                    if (userWallet != null) {
                        saveWalletUseCase(userWallet)
                            .onLeft {
                                // Rollback policy if card saving was failed
                                setAccessCodeRequestPolicyUseCase(prevRequestPolicyStatus)
                            }
                    } else {
                        // Rollback policy if card saving was failed
                        setAccessCodeRequestPolicyUseCase(prevRequestPolicyStatus)
                    }
                }
                .doOnFailure {
                    // Rollback policy if card scanning was failed
                    setAccessCodeRequestPolicyUseCase(prevRequestPolicyStatus)
                }
        }
    }

    override fun onDetailsClick() = router.openDetailsScreen()

    override fun onBackupCardClick() = router.openOnboardingScreen()

    override fun onCriticalWarningAlreadySignedHashesClick() {
        uiState = stateFactory.getStateWithOpenWalletBottomSheet(
            content = WalletBottomSheetConfig.BottomSheetContentConfig.CriticalWarningAlreadySignedHashes(
                onOkClick = {},
                onCancelClick = {},
            ),
        )
    }

    override fun onCloseWarningAlreadySignedHashesClick() {
        // TODO: [REDACTED_JIRA]
    }

    override fun onLikeTangemAppClick() {
        uiState = stateFactory.getStateWithOpenWalletBottomSheet(
            content = WalletBottomSheetConfig.BottomSheetContentConfig.LikeTangemApp(
                onRateTheAppClick = ::onRateTheAppClick,
                onShareClick = ::onShareClick,
            ),
        )
    }

    override fun onRateTheAppClick() {
        // TODO: [REDACTED_JIRA]
    }

    override fun onShareClick() {
        // TODO: [REDACTED_JIRA]
    }

    override fun onWalletChange(index: Int) {
        val state = requireNotNull(uiState as? WalletState.ContentState) {
            "Impossible to change wallet if state isn't WalletState.ContentState"
        }

        if (state.walletsListConfig.selectedWalletIndex == index) return

        viewModelScope.launch(dispatchers.io) {
            selectWalletUseCase(getWallet(index = index).walletId)
        }

        val cacheState = WalletStateCache.getState(userWalletId = state.walletsListConfig.wallets[index].id)
        if (cacheState != null) {
            uiState = if (cacheState is WalletState.ContentState) {
                cacheState.copySealed(
                    walletsListConfig = state.walletsListConfig.copy(selectedWalletIndex = index),
                    pullToRefreshConfig = state.pullToRefreshConfig.copy(isRefreshing = false),
                )
            } else {
                cacheState
            }

            if (cacheState.isLoadingState()) {
                getContentItemsUpdates(index)
            }
        } else {
            uiState = stateFactory.getSkeletonState(wallets = wallets, selectedWalletIndex = index)
            getContentItemsUpdates(index = index)
        }
    }

    override fun onRefreshSwipe() {
        val selectedWalletIndex = (uiState as? WalletState.ContentState)
            ?.walletsListConfig
            ?.selectedWalletIndex
            ?: return

        when (uiState) {
            is WalletMultiCurrencyState.Content -> refreshMultiCurrencyContent(selectedWalletIndex)
            is WalletSingleCurrencyState.Content -> refreshSingleCurrencyContent(selectedWalletIndex)
            is WalletState.Initial,
            is WalletMultiCurrencyState.Locked,
            is WalletSingleCurrencyState.Locked,
            -> Unit
        }
    }

    override fun onOrganizeTokensClick() {
        val state = requireNotNull(uiState as? WalletState.ContentState)
        val index = state.walletsListConfig.selectedWalletIndex
        val walletId = state.walletsListConfig.wallets[index].id

        router.openOrganizeTokensScreen(walletId)
    }

    override fun onBuyClick() {
        val state = uiState as? WalletState.ContentState ?: return
        val status = singleWalletCryptoCurrencyStatus ?: return
        val wallet = getWallet(index = state.walletsListConfig.selectedWalletIndex)

        reduxStateHolder.dispatch(
            TradeCryptoAction.New.Buy(
                userWallet = wallet,
                cryptoCurrencyStatus = status,
                appCurrencyCode = selectedAppCurrencyFlow.value.code,
            ),
        )
    }

    override fun onSingleCurrencySendClick(cryptoCurrencyStatus: CryptoCurrencyStatus?) {
        val state = uiState as? WalletState.ContentState ?: return

        val userWallet = getWallet(index = state.walletsListConfig.selectedWalletIndex)
        val coinStatus = if (userWallet.isMultiCurrency) cryptoCurrencyStatus else singleWalletCryptoCurrencyStatus

        reduxStateHolder.dispatch(
            action = TradeCryptoAction.New.SendCoin(
                userWallet = userWallet,
                coinStatus = coinStatus ?: return,
            ),
        )
    }

    override fun onMultiCurrencySendClick(cryptoCurrencyStatus: CryptoCurrencyStatus) {
        if (cryptoCurrencyStatus.currency is CryptoCurrency.Coin) {
            onSingleCurrencySendClick(cryptoCurrencyStatus = cryptoCurrencyStatus)
            return
        }

        val state = uiState as? WalletState.ContentState ?: return

        viewModelScope.launch(dispatchers.io) {
            val userWallet = getWallet(index = state.walletsListConfig.selectedWalletIndex)

            getNetworkCoinStatusUseCase(
                userWalletId = userWallet.walletId,
                networkId = cryptoCurrencyStatus.currency.network.id,
            )
                .take(count = 1)
                .collectLatest {
                    it.onRight { coinStatus ->
                        reduxStateHolder.dispatch(
                            action = TradeCryptoAction.New.SendToken(
                                userWallet = getWallet(index = state.walletsListConfig.selectedWalletIndex),
                                tokenStatus = cryptoCurrencyStatus,
                                coinFiatRate = coinStatus.value.fiatRate,
                            ),
                        )
                    }
                }
        }
    }

    override fun onReceiveClick() {
        // TODO: [REDACTED_JIRA]
    }

    override fun onSellClick() {
        val status = singleWalletCryptoCurrencyStatus ?: return

        reduxStateHolder.dispatch(
            TradeCryptoAction.New.Sell(
                cryptoCurrencyStatus = status,
                appCurrencyCode = selectedAppCurrencyFlow.value.code,
            ),
        )
    }

    override fun onManageTokensClick() {
        reduxStateHolder.dispatch(action = TokensAction.SetArgs.ManageAccess)
        router.openManageTokensScreen()
    }

    override fun onReloadClick() {
        val selectedWalletIndex = (uiState as? WalletSingleCurrencyState)
            ?.walletsListConfig
            ?.selectedWalletIndex
            ?: return

        refreshSingleCurrencyContent(selectedWalletIndex)
    }

    override fun onExploreClick() {
        viewModelScope.launch(dispatchers.io) {
            val wallet = getWallet(
                index = requireNotNull(uiState as? WalletState.ContentState).walletsListConfig.selectedWalletIndex,
            )
            router.openTxHistoryWebsite(
                url = getExploreUrlUseCase(
                    userWalletId = wallet.walletId,
                    networkId = Network.ID(
                        value = wallet.scanResponse.cardTypesResolver.getBlockchain().id,
                    ),
                ),
            )
        }
    }

    override fun onUnlockWalletClick() {
        viewModelScope.launch(dispatchers.io) {
            unlockWalletsUseCase()
        }
    }

    override fun onUnlockWalletNotificationClick() {
        val state = requireNotNull(uiState as? WalletLockedState) {
            "Impossible to unlock wallet if state isn't WalletLockedState"
        }

        uiState = stateFactory.getStateWithOpenWalletBottomSheet(
            content = when (state) {
                is WalletMultiCurrencyState.Locked -> state.bottomSheetConfig.content
                is WalletSingleCurrencyState.Locked -> state.bottomSheetConfig.content
            },
        )
    }

    override fun onTokenItemClick(currency: CryptoCurrency) {
        router.openTokenDetails(currency = currency)
    }

    override fun onTokenItemLongClick(cryptoCurrencyStatus: CryptoCurrencyStatus) {
        uiState = stateFactory.getStateWithTokenActionBottomSheet(cryptoCurrencyStatus)
    }

    override fun onRenameClick(userWalletId: UserWalletId, name: String) {
        viewModelScope.launch(dispatchers.io) {
            updateWalletUseCase(userWalletId = userWalletId, update = { it.copy(name) })
        }
    }

    override fun onDeleteClick(userWalletId: UserWalletId) {
        viewModelScope.launch(dispatchers.io) {
            val either = deleteWalletUseCase(userWalletId)

            val state = requireNotNull(uiState as? WalletState.ContentState)
            if (state.walletsListConfig.wallets.size <= 1 && either.isRight()) onBackClick()
        }
    }

    override fun onDismissBottomSheet() {
        uiState = stateFactory.getStateWithClosedBottomSheet()
    }

    override fun onDismissActionsBottomSheet() {
        (uiState as? WalletMultiCurrencyState.Content)?.let { state ->
            uiState = state.copy(
                tokenActionsBottomSheet = state.tokenActionsBottomSheet?.copy(
                    isShow = false,
                ),
            )
        }
    }

    private fun getContentItemsUpdates(index: Int) {
        /*
         * When wallet is changed it's necessary to stop the last jobs.
         * If jobs aren't stopped and wallet is changed then it will update state for the prev wallet.
         */
        tokensJobHolder.update(job = null)
        marketPriceJobHolder.update(job = null)
        buttonsJobHolder.update(job = null)
        notificationsJobHolder.update(job = null)
        refreshContentJobHolder.update(job = null)

        val wallet = getWallet(index)

        when {
            wallet.isLocked -> {
                uiState = stateFactory.getLockedState()
            }
            wallet.isMultiCurrency -> getMultiCurrencyContent(index)
            !wallet.isMultiCurrency -> getSingleCurrencyContent(index)
        }
    }

    private fun getMultiCurrencyContent(walletIndex: Int) {
        val state = requireNotNull(uiState as? WalletMultiCurrencyState) {
            "Impossible to get a token list updates if state isn't WalletMultiCurrencyState"
        }

        getTokenListUseCase(userWalletId = state.walletsListConfig.wallets[walletIndex].id)
            .distinctUntilChanged()
            .onEach { maybeTokenList ->
                uiState = stateFactory.getStateByTokensList(maybeTokenList)

                updateNotifications(
                    index = walletIndex,
                    tokenList = maybeTokenList.fold(ifLeft = { null }, ifRight = { it }),
                )
            }
            .flowOn(dispatchers.io)
            .launchIn(viewModelScope)
            .saveIn(tokensJobHolder)
    }

    private fun getSingleCurrencyContent(index: Int) {
        val wallet = getWallet(index)
        val blockchain = getCardTypeResolver(index).getBlockchain()
        updateTxHistory(
            blockchain = blockchain,
            derivationStyle = wallet.scanResponse.derivationStyleProvider.getDerivationStyle(),
        )
        updateMarketPrice(userWalletId = wallet.walletId)
        updateNotifications(index)
    }

    private fun updateTxHistory(blockchain: Blockchain, derivationStyle: DerivationStyle?) {
        viewModelScope.launch(dispatchers.io) {
            val derivationPath = blockchain.derivationPath(style = derivationStyle)?.rawPath

            val txHistoryItemsCountEither = txHistoryItemsCountUseCase(
                networkId = Network.ID(blockchain.id),
                derivationPath = derivationPath,
            )

            uiState = stateFactory.getLoadingTxHistoryState(itemsCountEither = txHistoryItemsCountEither)

            txHistoryItemsCountEither.onRight {
                uiState = stateFactory.getLoadedTxHistoryState(
                    txHistoryEither = txHistoryItemsUseCase(
                        networkId = Network.ID(blockchain.id),
                        derivationPath = derivationPath,
                    ).map {
                        it.cachedIn(viewModelScope)
                    },
                )
            }
        }
    }

    // It also update wallet balance
    private fun updateMarketPrice(userWalletId: UserWalletId) {
        getPrimaryCurrencyStatusUpdatesUseCase(userWalletId = userWalletId)
            .distinctUntilChanged()
            .onEach { maybeCryptoCurrencyStatus ->
                uiState = stateFactory.getSingleCurrencyLoadedBalanceState(maybeCryptoCurrencyStatus)

                maybeCryptoCurrencyStatus.onRight { status ->
                    singleWalletCryptoCurrencyStatus = status
                    updateButtons(userWalletId = userWalletId, currency = status.currency)
                }
            }
            .flowOn(dispatchers.io)
            .launchIn(viewModelScope)
            .saveIn(marketPriceJobHolder)
    }

    private fun updateButtons(userWalletId: UserWalletId, currency: CryptoCurrency) {
        getCryptoCurrencyActionsUseCase(userWalletId = userWalletId, cryptoCurrency = currency)
            .distinctUntilChanged()
            .onEach { uiState = stateFactory.getSingleCurrencyManageButtonsState(actions = it.states) }
            .flowOn(dispatchers.io)
            .launchIn(viewModelScope)
            .saveIn(buttonsJobHolder)
    }

    private fun updateNotifications(index: Int, tokenList: TokenList? = null) {
        notificationsListFactory.create(
            cardTypesResolver = getCardTypeResolver(index = index),
            tokenList = tokenList,
        )
            .distinctUntilChanged()
            .onEach { uiState = stateFactory.getStateByNotifications(notifications = it) }
            .flowOn(dispatchers.io)
            .launchIn(viewModelScope)
            .saveIn(notificationsJobHolder)
    }

    private fun refreshMultiCurrencyContent(walletIndex: Int) {
        uiState = stateFactory.getRefreshingState()
        val wallet = getWallet(walletIndex)

        viewModelScope.launch(dispatchers.io) {
            val result = fetchTokenListUseCase(wallet.walletId, refresh = true)

            uiState = stateFactory.getRefreshedState()
            uiState = result.fold(stateFactory::getStateByTokenListError) { uiState }
        }.saveIn(refreshContentJobHolder)
    }

    private fun refreshSingleCurrencyContent(walletIndex: Int) {
        uiState = stateFactory.getRefreshingState()
        val wallet = getWallet(walletIndex)

        viewModelScope.launch(dispatchers.io) {
            val result = fetchCurrencyStatusUseCase(wallet.walletId, refresh = true)

            uiState = stateFactory.getRefreshedState()
            uiState = result.fold(stateFactory::getStateByCurrencyStatusError) { uiState }
        }.saveIn(refreshContentJobHolder)
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

    private fun WalletState.isLoadingState(): Boolean {
        // Check the base components
        if (this is WalletState.ContentState) {
            walletsListConfig.wallets[walletsListConfig.selectedWalletIndex] is WalletCardState.Loading ||
                notifications.isEmpty()
        }

        // Check the special components
        return when (this) {
            is WalletMultiCurrencyState -> {
                val hasLoadingTokens = tokensListState is WalletTokensListState.ContentState &&
                    (tokensListState as WalletTokensListState.ContentState).items
                        .filterIsInstance<WalletTokensListState.TokensListItemState.Token>()
                        .any { it.state is TokenItemState.Loading }

                tokensListState is WalletTokensListState.Loading || hasLoadingTokens
            }
            is WalletSingleCurrencyState -> {
                this is WalletSingleCurrencyState.Content && marketPriceBlockState is MarketPriceBlockState.Loading
            }
            is WalletState.Initial -> false
        }
    }

    private fun getWallet(index: Int): UserWallet {
        return requireNotNull(
            value = wallets.getOrNull(index),
            lazyMessage = { "WalletsList doesn't contain element with index = $index" },
        )
    }

    private fun getCardTypeResolver(index: Int): CardTypesResolver = getWallet(index).scanResponse.cardTypesResolver
}