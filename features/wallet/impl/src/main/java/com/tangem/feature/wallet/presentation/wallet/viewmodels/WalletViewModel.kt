package com.tangem.feature.wallet.presentation.wallet.viewmodels

import androidx.lifecycle.*
import androidx.paging.cachedIn
import arrow.core.getOrElse
import com.tangem.common.Provider
import com.tangem.common.doOnFailure
import com.tangem.common.doOnSuccess
import com.tangem.core.navigation.AppScreen
import com.tangem.core.ui.components.bottomsheets.tokenreceive.AddressModel
import com.tangem.core.ui.components.bottomsheets.tokenreceive.TokenReceiveBottomSheetConfig
import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.IsBalanceHiddenUseCase
import com.tangem.domain.balancehiding.ListenToFlipsUseCase
import com.tangem.domain.card.*
import com.tangem.domain.common.CardTypesResolver
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.demo.IsDemoCardUseCase
import com.tangem.domain.redux.ReduxStateHolder
import com.tangem.domain.settings.*
import com.tangem.domain.tokens.*
import com.tangem.domain.tokens.legacy.TradeCryptoAction
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.TokenList
import com.tangem.domain.tokens.models.CryptoCurrency
import com.tangem.domain.tokens.models.Network
import com.tangem.domain.txhistory.usecase.GetTxHistoryItemsCountUseCase
import com.tangem.domain.txhistory.usecase.GetTxHistoryItemsUseCase
import com.tangem.domain.userwallets.UserWalletBuilder
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.models.SaveWalletError
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.usecase.*
import com.tangem.feature.wallet.presentation.common.state.TokenItemState
import com.tangem.feature.wallet.presentation.router.InnerWalletRouter
import com.tangem.feature.wallet.presentation.wallet.state.*
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletBottomSheetConfig
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletCardState
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletTokensListState
import com.tangem.feature.wallet.presentation.wallet.state.factory.WalletStateFactory
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.properties.Delegates

/**
 * Wallet screen view model
 *
 * @author Andrew Khokhlov on 31/05/2023
 */
@Suppress("LargeClass", "LongParameterList", "TooManyFunctions")
@HiltViewModel
internal class WalletViewModel @Inject constructor(
    private val getWalletsUseCase: GetWalletsUseCase,
    private val saveWalletUseCase: SaveWalletUseCase,
    getSelectedWalletUseCase: GetSelectedWalletUseCase,
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
    private val isBalanceHiddenUseCase: IsBalanceHiddenUseCase,
    private val listenToFlipsUseCase: ListenToFlipsUseCase,
    private val walletManagersFacade: WalletManagersFacade,
    private val reduxStateHolder: ReduxStateHolder,
    private val dispatchers: CoroutineDispatcherProvider,
) : ViewModel(), DefaultLifecycleObserver, WalletClickIntents {

    /** Feature router */
    var router: InnerWalletRouter by Delegates.notNull()

    private val selectedAppCurrencyFlow: StateFlow<AppCurrency> = createSelectedAppCurrencyFlow()
    private var isBalanceHidden = true

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
        isBalanceHiddenProvider = Provider { isBalanceHidden },
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
    private val onWalletChangeJobHolder = JobHolder()

    private val walletsUpdateActionResolver = WalletsUpdateActionResolver(
        currentStateProvider = Provider { uiState },
        getSelectedWalletUseCase = getSelectedWalletUseCase,
    )

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

        isBalanceHiddenUseCase()
            .flowWithLifecycle(owner.lifecycle)
            .onEach { hidden ->
                isBalanceHidden = hidden
                uiState = stateFactory.getHiddenBalanceState(isBalanceHidden = hidden)
            }
            .flowOn(dispatchers.io)
            .launchIn(viewModelScope)

        viewModelScope.launch {
            listenToFlipsUseCase()
                .flowWithLifecycle(owner.lifecycle)
                .flowOn(dispatchers.io)
                .collect()
        }
    }

    private fun updateWallets(sourceList: List<UserWallet>) {
        wallets = sourceList

        if (sourceList.isEmpty()) return

        when (val action = walletsUpdateActionResolver.resolve(sourceList)) {
            is WalletsUpdateActionResolver.Action.InitialWallets -> {
                loadAndUpdateState(index = action.selectedWalletIndex)
            }
            is WalletsUpdateActionResolver.Action.UpdateWalletName -> {
                uiState = stateFactory.getStateWithUpdatedWalletName(name = action.name)
            }
            is WalletsUpdateActionResolver.Action.UnlockWallet -> {
                uiState = stateFactory.getUnlockedState(action)

                getContentItemsUpdates(index = action.selectedWalletIndex)
            }
            is WalletsUpdateActionResolver.Action.DeleteWallet -> {
                deleteWalletAndUpdateState(action = action)
            }
            is WalletsUpdateActionResolver.Action.AddWallet -> {
                loadAndUpdateState(index = action.selectedWalletIndex)
            }
            is WalletsUpdateActionResolver.Action.Unknown -> Unit
        }
    }

    private fun deleteWalletAndUpdateState(action: WalletsUpdateActionResolver.Action.DeleteWallet) {
        val cacheState = WalletStateCache.getState(userWalletId = action.selectedWalletId)
        if (cacheState != null) {
            uiState = stateFactory.getStateWithoutDeletedWallet(cacheState, action)

            if (cacheState.isLoadingState()) {
                uiState = stateFactory.getStateAndTriggerEvent(
                    state = uiState,
                    event = WalletEvent.ChangeWallet(action.selectedWalletIndex),
                    setUiState = { uiState = it },
                )
                getContentItemsUpdates(action.selectedWalletIndex)
            }
        } else {
            loadAndUpdateState(index = action.selectedWalletIndex)
        }
    }

    private fun loadAndUpdateState(index: Int) {
        uiState = stateFactory.getSkeletonState(wallets = wallets, selectedWalletIndex = index)

        uiState = stateFactory.getStateAndTriggerEvent(
            state = uiState,
            event = WalletEvent.ChangeWallet(index = index),
            setUiState = { uiState = it },
        )

        getContentItemsUpdates(index = index)
    }

    override fun onBackClick() {
        viewModelScope.launch(dispatchers.main) {
            router.popBackStack(screen = if (shouldSaveUserWalletsUseCase()) AppScreen.Welcome else AppScreen.Home)
        }
    }

    override fun onScanCardClick() {
        viewModelScope.launch(dispatchers.io) {
            scanCardProcessor.scan()
                .doOnSuccess {
                    // If card's public key is null then user wallet will be null
                    val userWallet = UserWalletBuilder(scanResponse = it).build()

                    if (userWallet != null) {
                        saveWalletUseCase(userWallet = userWallet, canOverride = false)
                            .onLeft { saveWalletError ->
                                when (saveWalletError) {
                                    is SaveWalletError.DataError -> Unit
                                    is SaveWalletError.WalletAlreadySaved -> {
                                        uiState = stateFactory.getStateAndTriggerEvent(
                                            state = uiState,
                                            event = WalletEvent.ShowError(
                                                text = TextReference.Res(saveWalletError.messageId),
                                            ),
                                            setUiState = { uiState = it },
                                        )
                                    }
                                }
                            }
                    }
                }
                .doOnFailure { tangemError ->
                    uiState = stateFactory.getStateAndTriggerEvent(
                        state = uiState,
                        event = WalletEvent.ShowError(
                            text = TextReference.Str(tangemError.customMessage),
                        ),
                        setUiState = { uiState = it },
                    )
                }
        }
    }

    override fun onScanCardNotificationClick() {
        scanToUpdateSelectedWallet(
            onSuccessSave = {
                // Reload currencies with missed derivation
                fetchTokenListUseCase(userWalletId = it.walletId)
            },
        )
    }

    override fun onScanToUnlockWalletClick() {
        scanToUpdateSelectedWallet()
    }

    private fun scanToUpdateSelectedWallet(onSuccessSave: suspend (UserWallet) -> Unit = {}) {
        val state = uiState as? WalletState.ContentState ?: return

        val prevRequestPolicyStatus = getBiometricsStatusUseCase()

        // Update access the code policy according access code saving status
        setAccessCodeRequestPolicyUseCase(isBiometricsRequestPolicy = getAccessCodeSavingStatusUseCase())

        viewModelScope.launch(dispatchers.io) {
            scanCardProcessor.scan(
                cardId = getWallet(state.walletsListConfig.selectedWalletIndex).cardId,
                allowsRequestAccessCodeFromRepository = true,
            )
                .doOnSuccess {
                    // If card's public key is null then user wallet will be null
                    val userWallet = UserWalletBuilder(scanResponse = it).build()

                    if (userWallet != null) {
                        saveWalletUseCase(userWallet = userWallet, canOverride = true)
                            .onLeft {
                                // Rollback policy if card saving was failed
                                setAccessCodeRequestPolicyUseCase(prevRequestPolicyStatus)
                            }
                            .onRight { onSuccessSave(userWallet) }
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
            content = WalletBottomSheetConfig.CriticalWarningAlreadySignedHashes(
                onOkClick = {},
                onCancelClick = {},
            ),
        )
    }

    override fun onCloseWarningAlreadySignedHashesClick() {
        // TODO: https://tangem.atlassian.net/browse/AND-4103
    }

    override fun onLikeTangemAppClick() {
        uiState = stateFactory.getStateWithOpenWalletBottomSheet(
            content = WalletBottomSheetConfig.LikeTangemApp(
                onRateTheAppClick = ::onRateTheAppClick,
                onShareClick = ::onShareClick,
            ),
        )
    }

    override fun onRateTheAppClick() {
        // TODO: https://tangem.atlassian.net/browse/AND-4103
    }

    override fun onShareClick() {
        // TODO: https://tangem.atlassian.net/browse/AND-4103
    }

    override fun onWalletChange(index: Int) {
        val state = uiState as? WalletState.ContentState ?: return
        if (state.walletsListConfig.selectedWalletIndex == index) return

        // Reset the job to avoid a redundant state updating
        onWalletChangeJobHolder.update(null)

        viewModelScope.launch(dispatchers.main) {
            withContext(dispatchers.io) {
                selectWalletUseCase(userWalletId = state.walletsListConfig.wallets[index].id)
            }

            val cacheState = WalletStateCache.getState(userWalletId = state.walletsListConfig.wallets[index].id)
            if (cacheState != null && cacheState !is WalletLockedState) {
                uiState = cacheState.copySealed(
                    walletsListConfig = state.walletsListConfig.copy(
                        selectedWalletIndex = index,
                        wallets = state.walletsListConfig.wallets
                            .mapIndexed { mapIndex, currentWallet ->
                                val cacheWallet = cacheState.walletsListConfig.wallets.getOrNull(mapIndex)

                                if (currentWallet is WalletCardState.Loading && cacheWallet != null &&
                                    cacheWallet.isLoaded()
                                ) {
                                    cacheWallet
                                } else {
                                    currentWallet
                                }
                            }
                            .toImmutableList(),
                    ),
                    pullToRefreshConfig = cacheState.pullToRefreshConfig.copy(isRefreshing = false),
                )

                if (cacheState.isLoadingState()) {
                    getContentItemsUpdates(index)
                }
            } else {
                uiState = stateFactory.getSkeletonState(wallets = wallets, selectedWalletIndex = index)
                getContentItemsUpdates(index = index)
            }
        }
            .saveIn(onWalletChangeJobHolder)
    }

    private fun WalletCardState.isLoaded(): Boolean {
        return this !is WalletCardState.Loading && this !is WalletCardState.LockedContent
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

    override fun onBuyClick(cryptoCurrencyStatus: CryptoCurrencyStatus) {
        val state = uiState as? WalletState.ContentState ?: return
        val wallet = getWallet(index = state.walletsListConfig.selectedWalletIndex)

        reduxStateHolder.dispatch(
            TradeCryptoAction.New.Buy(
                userWallet = wallet,
                cryptoCurrencyStatus = cryptoCurrencyStatus,
                appCurrencyCode = selectedAppCurrencyFlow.value.code,
            ),
        )
    }

    override fun onSwapClick(cryptoCurrencyStatus: CryptoCurrencyStatus) {
        // todo implement onSwapClick https://tangem.atlassian.net/browse/AND-4535
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

    override fun onReceiveClick(cryptoCurrencyStatus: CryptoCurrencyStatus) {
        val state = uiState as? WalletState.ContentState ?: return

        viewModelScope.launch(dispatchers.io) {
            val userWallet = getWallet(index = state.walletsListConfig.selectedWalletIndex)

            val addresses = walletManagersFacade.getAddress(
                userWalletId = userWallet.walletId,
                network = cryptoCurrencyStatus.currency.network,
            )

            val currency = cryptoCurrencyStatus.currency
            uiState = stateFactory.getStateWithOpenWalletBottomSheet(
                content = TokenReceiveBottomSheetConfig(
                    name = currency.name,
                    symbol = currency.symbol,
                    network = currency.network.name,
                    addresses = addresses.map {
                        AddressModel(
                            value = it.value,
                            type = AddressModel.Type.valueOf(it.type.name),
                        )
                    },
                ),
            )
        }
    }

    override fun onSellClick(cryptoCurrencyStatus: CryptoCurrencyStatus) {
        reduxStateHolder.dispatch(
            TradeCryptoAction.New.Sell(
                cryptoCurrencyStatus = cryptoCurrencyStatus,
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
            val currencyStatus = getPrimaryCurrencyStatusUpdatesUseCase(wallet.walletId)
                .firstOrNull()
                ?.getOrNull()

            if (currencyStatus != null) {
                router.openTxHistoryWebsite(
                    url = getExploreUrlUseCase(
                        userWalletId = wallet.walletId,
                        network = currencyStatus.currency.network,
                    ),
                )
            }
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
        val state = uiState as? WalletState.ContentState ?: return
        val userWallet = getWallet(state.walletsListConfig.selectedWalletIndex)
        viewModelScope.launch(dispatchers.io) {
            getCryptoCurrencyActionsUseCase
                .invoke(userWallet.walletId, cryptoCurrencyStatus)
                .take(count = 1)
                .collectLatest {
                    uiState = stateFactory.getStateWithTokenActionBottomSheet(it)
                }
        }
    }

    override fun onRenameClick(userWalletId: UserWalletId, name: String) {
        viewModelScope.launch(dispatchers.io) {
            updateWalletUseCase(userWalletId = userWalletId, update = { it.copy(name) })
        }
    }

    override fun onDeleteClick(userWalletId: UserWalletId) {
        val state = uiState as? WalletState.ContentState ?: return

        viewModelScope.launch(dispatchers.io) {
            val either = deleteWalletUseCase(userWalletId)

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
        updatePrimaryCurrencyStatus(userWalletId = wallet.walletId)
        updateNotifications(index)
    }

    private fun updateTxHistory(network: Network) {
        viewModelScope.launch(dispatchers.io) {
            val txHistoryItemsCountEither = txHistoryItemsCountUseCase(network)

            uiState = stateFactory.getLoadingTxHistoryState(
                itemsCountEither = txHistoryItemsCountEither,
                cryptoCurrencyStatus = singleWalletCryptoCurrencyStatus ?: return@launch,
            )

            txHistoryItemsCountEither.onRight {
                uiState = stateFactory.getLoadedTxHistoryState(
                    txHistoryEither = txHistoryItemsUseCase(
                        network,
                    ).map {
                        it.cachedIn(viewModelScope)
                    },
                )
            }
        }
    }

    private fun updatePrimaryCurrencyStatus(userWalletId: UserWalletId) {
        getPrimaryCurrencyStatusUpdatesUseCase(userWalletId = userWalletId)
            .distinctUntilChanged()
            .onEach { maybeCryptoCurrencyStatus ->
                uiState = stateFactory.getSingleCurrencyLoadedBalanceState(maybeCryptoCurrencyStatus)

                maybeCryptoCurrencyStatus.onRight { status ->
                    singleWalletCryptoCurrencyStatus = status
                    updateButtons(userWalletId = userWalletId, currencyStatus = status)
                    updateTxHistory(status.currency.network)
                }
            }
            .flowOn(dispatchers.io)
            .launchIn(viewModelScope)
            .saveIn(marketPriceJobHolder)
    }

    private fun updateButtons(userWalletId: UserWalletId, currencyStatus: CryptoCurrencyStatus) {
        getCryptoCurrencyActionsUseCase(userWalletId = userWalletId, cryptoCurrencyStatus = currencyStatus)
            .distinctUntilChanged()
            .onEach { uiState = stateFactory.getSingleCurrencyManageButtonsState(actionsState = it) }
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
        if (this is WalletState.ContentState &&
            walletsListConfig.wallets[walletsListConfig.selectedWalletIndex] is WalletCardState.Loading
        ) {
            return true
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
