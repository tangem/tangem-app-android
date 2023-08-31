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
import com.tangem.domain.tokens.GetCryptoCurrencyActionsUseCase
import com.tangem.domain.tokens.GetPrimaryCurrencyStatusUpdatesUseCase
import com.tangem.domain.tokens.GetTokenListUseCase
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
    private val getPrimaryCurrencyUseCase: GetPrimaryCurrencyStatusUpdatesUseCase,
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
    private var cryptoCurrencyStatus: CryptoCurrencyStatus? = null

    private val tokensJobHolder = JobHolder()
    private val marketPriceJobHolder = JobHolder()
    private val buttonsJobHolder = JobHolder()
    private val notificationsJobHolder = JobHolder()

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
        val selectedWalletIndex = if (currentState is WalletLockedState) {
            currentState.getSelectedWalletIndex()
        } else {
            val selectedWallet = getSelectedWalletUseCase().fold(
                ifLeft = { error("Selected wallet is null") },
                ifRight = { it },
            )
            sourceList.indexOfFirst { it.walletId == selectedWallet.walletId }
        }

        uiState = stateFactory.getSkeletonState(wallets = sourceList, selectedWalletIndex = selectedWalletIndex)

        updateContentItems(index = selectedWalletIndex)
    }

    private fun updateContentItems(index: Int, isRefreshing: Boolean = false) {
        val cardTypeResolver = getCardTypeResolver(index)
        when {
            getWallet(index).isLocked -> uiState = stateFactory.getLockedState()
            cardTypeResolver.isMultiwalletAllowed() -> updateMultiCurrencyContent(index, isRefreshing)
            !cardTypeResolver.isMultiwalletAllowed() -> updateSingleCurrencyContent(index, isRefreshing)
        }
    }

    private fun updateMultiCurrencyContent(index: Int, isRefreshing: Boolean = false) {
        val state = requireNotNull(uiState as? WalletMultiCurrencyState) {
            "Impossible to update tokens list if state isn't WalletMultiCurrencyState"
        }

        getTokenListUseCase(userWalletId = state.walletsListConfig.wallets[index].id)
            .distinctUntilChanged()
            .onEach { tokenListEither ->
                uiState = stateFactory.getStateByTokensList(
                    tokenListEither = tokenListEither,
                    isRefreshing = isRefreshing,
                )

                updateNotifications(
                    index = index,
                    tokenList = tokenListEither.fold(ifLeft = { null }, ifRight = { it }),
                )
            }
            .flowOn(dispatchers.io)
            .launchIn(viewModelScope)
            .saveIn(tokensJobHolder)
    }

    private fun updateSingleCurrencyContent(index: Int, isRefreshing: Boolean) {
        val wallet = getWallet(index)
        val blockchain = getCardTypeResolver(index).getBlockchain()
        updateButtons(userWalletId = wallet.walletId, currencyId = blockchain.id)
        updateTxHistory(
            blockchain = blockchain,
            derivationStyle = wallet.scanResponse.derivationStyleProvider.getDerivationStyle(),
        )
        updateMarketPrice(userWalletId = wallet.walletId, isRefreshing = isRefreshing)
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
    private fun updateMarketPrice(userWalletId: UserWalletId, isRefreshing: Boolean) {
        getPrimaryCurrencyUseCase(userWalletId = userWalletId)
            .distinctUntilChanged()
            .onEach { either ->
                uiState = stateFactory.getSingleCurrencyLoadedBalanceState(
                    cryptoCurrencyEither = either,
                    isRefreshing = isRefreshing,
                )

                either.onRight { status -> cryptoCurrencyStatus = status }
            }
            .flowOn(dispatchers.io)
            .launchIn(viewModelScope)
            .saveIn(marketPriceJobHolder)
    }

    private fun updateButtons(userWalletId: UserWalletId, currencyId: String) {
        getCryptoCurrencyActionsUseCase(userWalletId = userWalletId, tokenId = currencyId)
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

    override fun onStop(owner: LifecycleOwner) {
        viewModelScope.launch(dispatchers.io) {
            saveSelectedWallet()
        }
    }

    private suspend fun saveSelectedWallet() {
        val state = uiState
        if (state is WalletState.ContentState) {
            selectWalletUseCase(getWallet(index = state.walletsListConfig.selectedWalletIndex).walletId)
        }
    }

    private fun getWallet(index: Int): UserWallet {
        return requireNotNull(
            value = wallets.getOrNull(index),
            lazyMessage = { "WalletsList doesn't contain element with index = $index" },
        )
    }

    private fun getCardTypeResolver(index: Int): CardTypesResolver = getWallet(index).scanResponse.cardTypesResolver

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

        /*
         * When wallet is changed it's necessary to stop the last jobs.
         * If jobs aren't stopped and wallet is changed then it will update state for the prev wallet.
         */
        tokensJobHolder.update(job = null)
        marketPriceJobHolder.update(job = null)
        buttonsJobHolder.update(job = null)
        notificationsJobHolder.update(job = null)

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

            if (cacheState.isLoadingState()) updateContentItems(index)
        } else {
            uiState = stateFactory.getSkeletonState(wallets = wallets, selectedWalletIndex = index)
            updateContentItems(index = index)
        }
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

    override fun onRefreshSwipe() {
        if (uiState is WalletState.Initial || uiState is WalletLockedState) return

        viewModelScope.launch(dispatchers.io) {
            uiState = stateFactory.getStateAfterContentRefreshing()

            // TODO: [REDACTED_JIRA]
            delay(timeMillis = 500)

            updateContentItems(
                index = requireNotNull(uiState as? WalletState.ContentState).walletsListConfig.selectedWalletIndex,
                isRefreshing = true,
            )
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
        val status = cryptoCurrencyStatus ?: return
        val wallet = getWallet(index = state.walletsListConfig.selectedWalletIndex)

        reduxStateHolder.dispatch(
            TradeCryptoAction.New.Buy(
                userWallet = wallet,
                cryptoCurrencyStatus = status,
                appCurrencyCode = selectedAppCurrencyFlow.value.code,
            ),
        )
    }

    override fun onSendClick() {
        reduxStateHolder.dispatch(TradeCryptoAction.New.Send)
    }

    override fun onReceiveClick() {
        // TODO: [REDACTED_JIRA]
    }

    override fun onSellClick() {
        val status = cryptoCurrencyStatus ?: return

        reduxStateHolder.dispatch(
            TradeCryptoAction.New.Sell(
                cryptoCurrencyStatus = status,
                appCurrencyCode = selectedAppCurrencyFlow.value.code,
            ),
        )
    }

    override fun onManageTokensClick() {
        router.openManageTokensScreen()
    }

    override fun onReloadClick() {
        uiState = stateFactory.getStateAfterContentRefreshing()
        updateSingleCurrencyContent(
            index = requireNotNull(uiState as? WalletState.ContentState).walletsListConfig.selectedWalletIndex,
            isRefreshing = true,
        )
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

    override fun onTokenItemLongClick(currency: CryptoCurrency) {
        uiState = stateFactory.getStateWithTokenActionBottomSheet(
            tokenId = currency.id.value,
        )
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
}