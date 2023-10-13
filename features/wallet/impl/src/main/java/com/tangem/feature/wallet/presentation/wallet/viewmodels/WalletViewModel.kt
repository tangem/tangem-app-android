package com.tangem.feature.wallet.presentation.wallet.viewmodels

import androidx.lifecycle.*
import androidx.paging.cachedIn
import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.right
import com.tangem.blockchain.blockchains.cardano.CardanoUtils
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.address.AddressType
import com.tangem.common.Provider
import com.tangem.common.card.EllipticCurve
import com.tangem.common.doOnSuccess
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.common.extensions.isZero
import com.tangem.common.extensions.toMapKey
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.navigation.AppScreen
import com.tangem.core.ui.components.bottomsheets.tokenreceive.AddressModel
import com.tangem.core.ui.components.bottomsheets.tokenreceive.TokenReceiveBottomSheetConfig
import com.tangem.core.ui.components.transactions.state.TxHistoryState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.WrappedList
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.IsBalanceHiddenUseCase
import com.tangem.domain.balancehiding.ListenToFlipsUseCase
import com.tangem.domain.card.DerivePublicKeysUseCase
import com.tangem.domain.card.ScanCardProcessor
import com.tangem.domain.card.SetCardWasScannedUseCase
import com.tangem.domain.card.WasCardScannedUseCase
import com.tangem.domain.common.CardTypesResolver
import com.tangem.domain.common.configs.CardConfig
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.common.util.derivationStyleProvider
import com.tangem.domain.demo.IsDemoCardUseCase
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.redux.LegacyAction
import com.tangem.domain.redux.ReduxStateHolder
import com.tangem.domain.settings.*
import com.tangem.domain.tokens.*
import com.tangem.domain.tokens.error.TokenListError
import com.tangem.domain.tokens.legacy.TradeCryptoAction
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.NetworkGroup
import com.tangem.domain.tokens.model.TokenList
import com.tangem.domain.tokens.models.analytics.TokenReceiveAnalyticsEvent
import com.tangem.domain.txhistory.usecase.GetExplorerTransactionUrlUseCase
import com.tangem.domain.txhistory.usecase.GetTxHistoryItemsCountUseCase
import com.tangem.domain.txhistory.usecase.GetTxHistoryItemsUseCase
import com.tangem.domain.userwallets.UserWalletBuilder
import com.tangem.domain.walletconnect.WalletConnectActions
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.usecase.*
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.router.InnerWalletRouter
import com.tangem.feature.wallet.presentation.wallet.analytics.PortfolioEvent
import com.tangem.feature.wallet.presentation.wallet.analytics.WalletScreenAnalyticsEvent
import com.tangem.feature.wallet.presentation.wallet.state.*
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletCardState
import com.tangem.feature.wallet.presentation.wallet.state.factory.TokenListWithWallet
import com.tangem.feature.wallet.presentation.wallet.state.factory.WalletStateFactory
import com.tangem.operations.derivation.ExtendedPublicKeysMap
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
[REDACTED_AUTHOR]
 */
@Suppress("LargeClass", "LongParameterList", "TooManyFunctions")
@HiltViewModel
internal class WalletViewModel @Inject constructor(
    // region Parameters
    private val getWalletsUseCase: GetWalletsUseCase,
    private val saveWalletUseCase: SaveWalletUseCase,
    getSelectedWalletUseCase: GetSelectedWalletUseCase,
    private val selectWalletUseCase: SelectWalletUseCase,
    private val updateWalletUseCase: UpdateWalletUseCase,
    private val deleteWalletUseCase: DeleteWalletUseCase,
    private val getTokenListUseCase: GetTokenListUseCase,
    private val getCardTokensListUseCase: GetCardTokensListUseCase,
    private val fetchTokenListUseCase: FetchTokenListUseCase,
    private val fetchCardTokenListUseCase: FetchCardTokenListUseCase,
    private val getPrimaryCurrencyStatusUpdatesUseCase: GetPrimaryCurrencyStatusUpdatesUseCase,
    private val fetchCurrencyStatusUseCase: FetchCurrencyStatusUseCase,
    private val getNetworkCoinStatusUseCase: GetNetworkCoinStatusUseCase,
    private val scanCardProcessor: ScanCardProcessor,
    private val derivePublicKeysUseCase: DerivePublicKeysUseCase,
    private val txHistoryItemsCountUseCase: GetTxHistoryItemsCountUseCase,
    private val txHistoryItemsUseCase: GetTxHistoryItemsUseCase,
    private val getExploreUrlUseCase: GetExploreUrlUseCase,
    private val unlockWalletsUseCase: UnlockWalletsUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val getCryptoCurrencyActionsUseCase: GetCryptoCurrencyActionsUseCase,
    private val shouldShowSaveWalletScreenUseCase: ShouldShowSaveWalletScreenUseCase,
    private val canUseBiometryUseCase: CanUseBiometryUseCase,
    private val shouldSaveUserWalletsUseCase: ShouldSaveUserWalletsUseCase,
    private val shouldSaveUserWalletsSyncUseCase: ShouldSaveUserWalletsSyncUseCase,
    private val isBalanceHiddenUseCase: IsBalanceHiddenUseCase,
    private val listenToFlipsUseCase: ListenToFlipsUseCase,
    private val removeCurrencyUseCase: RemoveCurrencyUseCase,
    private val isCryptoCurrencyCoinCouldHide: IsCryptoCurrencyCoinCouldHideUseCase,
    private val walletManagersFacade: WalletManagersFacade,
    private val reduxStateHolder: ReduxStateHolder,
    private val dispatchers: CoroutineDispatcherProvider,
    private val analyticsEventsHandler: AnalyticsEventHandler,
    private val setCardWasScannedUseCase: SetCardWasScannedUseCase,
    private val remindToRateAppLaterUseCase: RemindToRateAppLaterUseCase,
    private val neverToSuggestRateAppUseCase: NeverToSuggestRateAppUseCase,
    private val setWalletWithFundsFoundUseCase: SetWalletWithFundsFoundUseCase,
    private val getExplorerTransactionUrlUseCase: GetExplorerTransactionUrlUseCase,
    wasCardScannedUseCase: WasCardScannedUseCase,
    isReadyToShowRateAppUseCase: IsReadyToShowRateAppUseCase,
    isDemoCardUseCase: IsDemoCardUseCase,
    isNeedToBackupUseCase: IsNeedToBackupUseCase,
    // endregion Parameters
) : ViewModel(), DefaultLifecycleObserver, WalletClickIntents {

    /** Feature router */
    var router: InnerWalletRouter by Delegates.notNull()

    private val selectedAppCurrencyFlow: StateFlow<AppCurrency> = createSelectedAppCurrencyFlow()

    private val notificationsListFactory = WalletNotificationsListFactory(
        wasCardScannedUseCase = wasCardScannedUseCase,
        isReadyToShowRateAppUseCase = isReadyToShowRateAppUseCase,
        isDemoCardUseCase = isDemoCardUseCase,
        isNeedToBackupUseCase = isNeedToBackupUseCase,
        clickIntents = this,
    )

    private var isBalanceHidden = true

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
    private val updateWcJobHolder = JobHolder()
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
        analyticsEventsHandler.send(WalletScreenAnalyticsEvent.ScreenOpened)

        viewModelScope.launch(dispatchers.main) {
            delay(timeMillis = 1_800)

            if (router.isWalletLastScreen() && shouldShowSaveWalletScreenUseCase() && canUseBiometryUseCase()) {
                router.openSaveUserWalletScreen()
            }
        }

        viewModelScope.launch(dispatchers.io) {
            shouldSaveUserWalletsUseCase()
                .flowWithLifecycle(owner.lifecycle)
                .collectLatest {
                    getWalletsUseCase()
                        .flowWithLifecycle(owner.lifecycle)
                        .distinctUntilChanged()
                        .onEach(::updateWallets)
                        .flowOn(dispatchers.io)
                        .launchIn(viewModelScope)
                }
        }

        isBalanceHiddenUseCase()
            .flowWithLifecycle(owner.lifecycle)
            .onEach { hidden ->
                isBalanceHidden = hidden
                WalletStateCache.updateAll { copySealed(isBalanceHidden = hidden) }
                uiState = stateFactory.getHiddenBalanceState(isBalanceHidden = hidden)
            }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            listenToFlipsUseCase()
                .flowWithLifecycle(owner.lifecycle)
                .collect()
        }
    }

    private fun updateWallets(sourceList: List<UserWallet>) {
        wallets = sourceList

        if (sourceList.isEmpty()) return

        when (val action = walletsUpdateActionResolver.resolve(sourceList)) {
            is WalletsUpdateActionResolver.Action.Initialize -> {
                initializeAndLoadState(selectedWalletIndex = action.selectedWalletIndex)
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
                scrollAndUpdateState(action.selectedWalletIndex)
            }
            is WalletsUpdateActionResolver.Action.UpdateWalletCardCount -> {
                uiState = stateFactory.getStateWithUpdatedWalletCardCount()
            }
            is WalletsUpdateActionResolver.Action.Unknown -> Unit
        }
    }

    private fun initializeAndLoadState(selectedWalletIndex: Int) {
        uiState = stateFactory.getSkeletonState(wallets = wallets, selectedWalletIndex = selectedWalletIndex)

        getContentItemsUpdates(index = selectedWalletIndex)
    }

    private fun deleteWalletAndUpdateState(action: WalletsUpdateActionResolver.Action.DeleteWallet) {
        val cacheState = WalletStateCache.getState(userWalletId = action.selectedWalletId)
        if (cacheState != null) {
            uiState = stateFactory.getStateWithoutDeletedWallet(cacheState, action)

            uiState = stateFactory.getStateAndTriggerEvent(
                state = uiState,
                event = WalletEvent.ChangeWallet(action.selectedWalletIndex),
                setUiState = { uiState = it },
            )
            getContentItemsUpdates(action.selectedWalletIndex)
        } else {
            /* It's impossible case because user can delete only visible state, but we support this case */
            scrollAndUpdateState(selectedWalletIndex = action.selectedWalletIndex)
        }
    }

    private fun scrollAndUpdateState(selectedWalletIndex: Int) {
        uiState = stateFactory.getSkeletonState(
            wallets = wallets,
            selectedWalletIndex = selectedWalletIndex,
        )

        uiState = stateFactory.getStateAndTriggerEvent(
            state = uiState,
            event = WalletEvent.ChangeWallet(index = selectedWalletIndex),
            setUiState = { uiState = it },
        )

        getContentItemsUpdates(index = selectedWalletIndex)
    }

    override fun onBackClick() {
        viewModelScope.launch(dispatchers.main) {
            router.popBackStack(screen = if (shouldSaveUserWalletsSyncUseCase()) AppScreen.Welcome else AppScreen.Home)
        }
    }

    override fun onGenerateMissedAddressesClick(missedAddressCurrencies: List<CryptoCurrency>) {
        val state = uiState as? WalletState.ContentState ?: return

        analyticsEventsHandler.send(WalletScreenAnalyticsEvent.NoticeScanYourCardTapped)

        viewModelScope.launch(dispatchers.io) {
            val userWallet = getWallet(index = state.walletsListConfig.selectedWalletIndex)

            deriveMissingCurrencies(
                scanResponse = userWallet.scanResponse,
                currencyList = missedAddressCurrencies,
            ) { scannedCardResponse ->
                updateWalletUseCase(
                    userWalletId = userWallet.walletId,
                    update = { it.copy(scanResponse = scannedCardResponse) },
                )
                    .onRight {
                        fetchTokenListUseCase(userWalletId = it.walletId)
                    }
            }
        }
    }

    // TODO: [REDACTED_JIRA]
    private fun deriveMissingCurrencies(
        scanResponse: ScanResponse,
        currencyList: List<CryptoCurrency>,
        onSuccess: suspend (ScanResponse) -> Unit,
    ) {
        val config = CardConfig.createConfig(scanResponse.card)
        val derivationDataList = currencyList.mapNotNull {
            config.primaryCurve(blockchain = Blockchain.fromId(it.network.id.value))?.let { curve ->
                getNewDerivations(curve, scanResponse, currencyList)
            }
        }

        val derivations = derivationDataList
            .associate(DerivationData::derivations)
            .ifEmpty { return }

        viewModelScope.launch(dispatchers.io) {
            derivePublicKeysUseCase(cardId = scanResponse.card.cardId, derivations = derivations)
                .onRight {
                    val newDerivedKeys = it.entries
                    val oldDerivedKeys = scanResponse.derivedKeys

                    val walletKeys = (newDerivedKeys.keys + oldDerivedKeys.keys).toSet()

                    val updatedDerivedKeys = walletKeys.associateWith { walletKey ->
                        val oldDerivations = ExtendedPublicKeysMap(oldDerivedKeys[walletKey] ?: emptyMap())
                        val newDerivations = newDerivedKeys[walletKey] ?: ExtendedPublicKeysMap(emptyMap())
                        ExtendedPublicKeysMap(oldDerivations + newDerivations)
                    }
                    val updatedScanResponse = scanResponse.copy(derivedKeys = updatedDerivedKeys)

                    onSuccess(updatedScanResponse)
                }
        }
    }

    private fun getNewDerivations(
        curve: EllipticCurve,
        scanResponse: ScanResponse,
        currencyList: List<CryptoCurrency>,
    ): DerivationData? {
        val wallet = scanResponse.card.wallets.firstOrNull { it.curve == curve } ?: return null

        val manageTokensCandidates = currencyList
            .map { Blockchain.fromId(it.network.id.value) }
            .distinct()
            .filter { it.getSupportedCurves().contains(curve) }
            .mapNotNull { it.derivationPath(scanResponse.derivationStyleProvider.getDerivationStyle()) }

        val customTokensCandidates = currencyList
            .filter { Blockchain.fromId(it.network.id.value).getSupportedCurves().contains(curve) }
            .mapNotNull { it.network.derivationPath.value }
            .map(::DerivationPath)

        val bothCandidates = (manageTokensCandidates + customTokensCandidates).distinct().toMutableList()
        if (bothCandidates.isEmpty()) return null

        currencyList.find { it is CryptoCurrency.Coin && Blockchain.fromId(it.network.id.value) == Blockchain.Cardano }
            ?.let { currency ->
                currency.network.derivationPath.value?.let {
                    bothCandidates.add(CardanoUtils.extendedDerivationPath(DerivationPath(it)))
                }
            }

        val mapKeyOfWalletPublicKey = wallet.publicKey.toMapKey()
        val alreadyDerivedKeys: ExtendedPublicKeysMap =
            scanResponse.derivedKeys[mapKeyOfWalletPublicKey] ?: ExtendedPublicKeysMap(emptyMap())
        val alreadyDerivedPaths = alreadyDerivedKeys.keys.toList()

        val toDerive = bothCandidates.filterNot { alreadyDerivedPaths.contains(it) }
        if (toDerive.isEmpty()) return null

        return DerivationData(derivations = mapKeyOfWalletPublicKey to toDerive)
    }

    class DerivationData(val derivations: Pair<ByteArrayKey, List<DerivationPath>>)

    override fun onScanToUnlockWalletClick() {
        val state = uiState as? WalletState.ContentState ?: return
        val lockedWallet = getWallet(index = state.walletsListConfig.selectedWalletIndex)

        viewModelScope.launch(dispatchers.io) {
            scanCardProcessor.scan()
                .doOnSuccess {
                    // If card's public key is null then user wallet will be null
                    val unlockedWallet = UserWalletBuilder(scanResponse = it).build()

                    if (lockedWallet.walletId == unlockedWallet?.walletId) {
                        saveWalletUseCase(userWallet = unlockedWallet, canOverride = true)
                    }
                }
        }
    }

    override fun onDetailsClick() = router.openDetailsScreen()

    override fun onBackupCardClick() {
        analyticsEventsHandler.send(WalletScreenAnalyticsEvent.NoticeBackupYourWalletTapped)
        router.openOnboardingScreen()
    }

    override fun onSignedHashesNotificationCloseClick() {
        val state = uiState as? WalletState.ContentState ?: return
        viewModelScope.launch(dispatchers.main) {
            setCardWasScannedUseCase(
                cardId = getWallet(index = state.walletsListConfig.selectedWalletIndex).cardId,
            )
        }
    }

    override fun onLikeAppClick() {
        analyticsEventsHandler.send(WalletScreenAnalyticsEvent.NoticeRateAppButton(AnalyticsParam.RateApp.Liked))
        uiState = stateFactory.getStateAndTriggerEvent(
            state = uiState,
            event = WalletEvent.RateApp(
                onDismissClick = {
                    viewModelScope.launch(dispatchers.main) {
                        neverToSuggestRateAppUseCase()
                    }
                },
            ),
            setUiState = { uiState = it },
        )
    }

    override fun onDislikeAppClick() {
        analyticsEventsHandler.send(WalletScreenAnalyticsEvent.NoticeRateAppButton(AnalyticsParam.RateApp.Disliked))
        viewModelScope.launch(dispatchers.main) {
            neverToSuggestRateAppUseCase()

            reduxStateHolder.dispatch(LegacyAction.SendEmailRateCanBeBetter)
        }
    }

    override fun onCloseRateAppNotificationClick() {
        analyticsEventsHandler.send(WalletScreenAnalyticsEvent.NoticeRateAppButton(AnalyticsParam.RateApp.Closed))
        viewModelScope.launch(dispatchers.main) {
            remindToRateAppLaterUseCase()
        }
    }

    override fun onWalletChange(index: Int) {
        analyticsEventsHandler.send(WalletScreenAnalyticsEvent.WalletSwipe)

        val state = uiState as? WalletState.ContentState ?: return
        if (state.walletsListConfig.selectedWalletIndex == index) return

        // Reset the job to avoid a redundant state updating
        onWalletChangeJobHolder.update(null)

        /*
         * When wallet is changed it's necessary to stop the last jobs.
         * If jobs aren't stopped and wallet is changed then it will update state for the prev wallet.
         */
        tokensJobHolder.update(job = null)
        updateWcJobHolder.update(job = null)
        marketPriceJobHolder.update(job = null)
        buttonsJobHolder.update(job = null)
        notificationsJobHolder.update(job = null)
        refreshContentJobHolder.update(job = null)

        viewModelScope.launch(dispatchers.main) {
            val userWallet = state.walletsListConfig.wallets[index]
            withContext(dispatchers.io) {
                if (userWallet !is WalletCardState.LockedContent) {
                    selectWalletUseCase(userWalletId = userWallet.id)
                }
            }

            val cacheState = WalletStateCache.getState(userWalletId = userWallet.id)
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

                getContentItemsUpdates(index)
            } else {
                initializeAndLoadState(selectedWalletIndex = index)
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
            is WalletMultiCurrencyState.Content -> {
                analyticsEventsHandler.send(PortfolioEvent.Refreshed)
                refreshMultiCurrencyContent(selectedWalletIndex)
            }
            is WalletSingleCurrencyState.Content -> {
                analyticsEventsHandler.send(PortfolioEvent.Refreshed)
                refreshSingleCurrencyContent(selectedWalletIndex)
            }
            is WalletState.Initial,
            is WalletMultiCurrencyState.Locked,
            is WalletSingleCurrencyState.Locked,
            -> Unit
        }
    }

    private fun refreshMultiCurrencyContent(walletIndex: Int) {
        uiState = stateFactory.getRefreshingState()

        viewModelScope.launch(dispatchers.main) {
            val wallet = getWallet(walletIndex)

            val maybeFetchResult = if (isSingleWalletWithTokens(wallet)) {
                fetchCardTokenListUseCase(userWalletId = wallet.walletId, refresh = true)
            } else {
                fetchTokenListUseCase(userWalletId = wallet.walletId, refresh = true)
            }

            maybeFetchResult.onLeft { uiState = stateFactory.getStateByTokenListError(it) }

            uiState = stateFactory.getRefreshedState()
        }.saveIn(refreshContentJobHolder)
    }

    override fun onOrganizeTokensClick() {
        analyticsEventsHandler.send(PortfolioEvent.OrganizeTokens)

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
        reduxStateHolder.dispatch(TradeCryptoAction.New.Swap(cryptoCurrencyStatus.currency))
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

            val isSingleWalletWithTokens = !userWallet.isMultiCurrency &&
                userWallet.scanResponse.walletData?.token != null

            getNetworkCoinStatusUseCase(
                userWalletId = userWallet.walletId,
                networkId = cryptoCurrencyStatus.currency.network.id,
                derivationPath = cryptoCurrencyStatus.currency.network.derivationPath,
                isSingleWalletWithTokens = isSingleWalletWithTokens,
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
                    onCopyClick = {
                        analyticsEventsHandler.send(TokenReceiveAnalyticsEvent.ButtonCopyAddress(currency.symbol))
                    },
                    onShareClick = {
                        analyticsEventsHandler.send(TokenReceiveAnalyticsEvent.ButtonShareAddress(currency.symbol))
                    },
                ),
            )
        }
    }

    override fun onCopyAddressClick(cryptoCurrencyStatus: CryptoCurrencyStatus) {
        val state = uiState as? WalletState.ContentState ?: return

        viewModelScope.launch(dispatchers.main) {
            val userWallet = getWallet(index = state.walletsListConfig.selectedWalletIndex)

            val defaultAddress = walletManagersFacade.getAddress(
                userWalletId = userWallet.walletId,
                network = cryptoCurrencyStatus.currency.network,
            ).find { it.type == AddressType.Default }

            defaultAddress?.value?.let { address ->
                uiState = stateFactory.getStateAndTriggerEvent(
                    state = uiState,
                    event = WalletEvent.CopyAddress(
                        address = address,
                        toast = resourceReference(R.string.wallet_notification_address_copied),
                    ),
                    setUiState = { uiState = it },
                )
            }
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
        analyticsEventsHandler.send(PortfolioEvent.ButtonManageTokens)
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

    private fun refreshSingleCurrencyContent(walletIndex: Int) {
        uiState = stateFactory.getRefreshingState()
        val wallet = getWallet(walletIndex)

        viewModelScope.launch(dispatchers.main) {
            val result = fetchCurrencyStatusUseCase(wallet.walletId, refresh = true)

            uiState = stateFactory.getRefreshedState()
            uiState = result.fold(stateFactory::getStateByCurrencyStatusError) { uiState }

            singleWalletCryptoCurrencyStatus?.let {
                val singleCurrencyState = uiState as WalletSingleCurrencyState
                if (singleCurrencyState.txHistoryState !is TxHistoryState.Content) {
                    // show loading indicator while refreshing in non content state
                    uiState = stateFactory.getLoadingTxHistoryState(1.right())
                }
                updateTxHistory(wallet.walletId, it.currency, refresh = true)
            }
        }.saveIn(refreshContentJobHolder)
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
                router.openUrl(
                    url = getExploreUrlUseCase(
                        userWalletId = wallet.walletId,
                        network = currencyStatus.currency.network,
                    ),
                )
            }
        }
    }

    override fun onUnlockWalletClick() {
        analyticsEventsHandler.send(WalletScreenAnalyticsEvent.NoticeWalletLocked)

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
        analyticsEventsHandler.send(PortfolioEvent.TokenTapped)
        router.openTokenDetails(getSelectedWallet().walletId, currency)
    }

    override fun onTokenItemLongClick(cryptoCurrencyStatus: CryptoCurrencyStatus) {
        val state = uiState as? WalletState.ContentState ?: return
        val userWallet = getWallet(state.walletsListConfig.selectedWalletIndex)
        viewModelScope.launch(dispatchers.io) {
            getCryptoCurrencyActionsUseCase(userWallet.walletId, cryptoCurrencyStatus)
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

    override fun onDeleteBeforeConfirmationClick(userWalletId: UserWalletId) {
        uiState = stateFactory.getStateAndTriggerEvent(
            state = uiState,
            event = WalletEvent.ShowAlert(
                state = WalletAlertState.RemoveWalletAlert(
                    onConfirmClick = { onDeleteAfterConfirmationClick(userWalletId) },
                ),
            ),
            setUiState = { uiState = it },
        )
    }

    override fun onDeleteAfterConfirmationClick(userWalletId: UserWalletId) {
        val state = uiState as? WalletState.ContentState ?: return
        viewModelScope.launch(dispatchers.io) {
            deleteWalletUseCase(userWalletId)

            popBackIfAllWalletsIsLocked(wallets = state.walletsListConfig.wallets)
        }
    }

    private fun popBackIfAllWalletsIsLocked(wallets: List<WalletCardState>) {
        val unlockedWallet = wallets.count { it !is WalletCardState.LockedContent }

        if (unlockedWallet == 1) {
            router.popBackStack(
                screen = if (wallets.size > 1) AppScreen.Welcome else AppScreen.Home,
            )
        }
    }

    override fun onPerformHideToken(cryptoCurrencyStatus: CryptoCurrencyStatus) {
        val state = uiState as? WalletState.ContentState ?: return
        val userWallet = getWallet(state.walletsListConfig.selectedWalletIndex)
        viewModelScope.launch(dispatchers.io) {
            removeCurrencyUseCase(userWallet.walletId, cryptoCurrencyStatus.currency)
                .fold(
                    ifLeft = {
                        showSnackbar(resourceReference(R.string.common_error))
                    },
                    ifRight = {
                        onDismissActionsBottomSheet()
                    },
                )
        }
    }

    override fun onHideTokensClick(cryptoCurrencyStatus: CryptoCurrencyStatus) {
        viewModelScope.launch(dispatchers.main) {
            val state = uiState as? WalletState.ContentState ?: return@launch
            val userWallet = getWallet(state.walletsListConfig.selectedWalletIndex)
            uiState = stateFactory.getStateAndTriggerEvent(
                state = uiState,
                event = getHideTokeAlert(userWallet.walletId, cryptoCurrencyStatus),
                setUiState = { uiState = it },
            )
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

    override fun onTransactionClick(txHash: String) {
        viewModelScope.launch(dispatchers.io) {
            val wallet = getWallet(
                index = requireNotNull(uiState as? WalletState.ContentState).walletsListConfig.selectedWalletIndex,
            )
            val currencyStatus = getPrimaryCurrencyStatusUpdatesUseCase(wallet.walletId)
                .firstOrNull()
                ?.getOrNull()

            if (currencyStatus != null) {
                router.openUrl(
                    url = getExplorerTransactionUrlUseCase(
                        txHash = txHash,
                        networkId = currencyStatus.currency.network.id,
                    ),
                )
            }
        }
    }

    private suspend fun getHideTokeAlert(
        userWalletId: UserWalletId,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
    ): WalletEvent.ShowAlert {
        val currency = cryptoCurrencyStatus.currency
        return if (currency is CryptoCurrency.Coin && !isCryptoCurrencyCoinCouldHide(userWalletId, currency)) {
            WalletEvent.ShowAlert(
                state = WalletAlertState.DefaultAlert(
                    title = resourceReference(
                        id = R.string.token_details_unable_hide_alert_title,
                        formatArgs = WrappedList(listOf(cryptoCurrencyStatus.currency.name)),
                    ),
                    message = resourceReference(
                        id = R.string.token_details_unable_hide_alert_message,
                        formatArgs = WrappedList(
                            listOf(
                                cryptoCurrencyStatus.currency.name,
                                cryptoCurrencyStatus.currency.network.name,
                            ),
                        ),
                    ),
                    onConfirmClick = null,
                ),
            )
        } else {
            WalletEvent.ShowAlert(
                state = WalletAlertState.DefaultAlert(
                    title = resourceReference(
                        id = R.string.token_details_hide_alert_title,
                        formatArgs = WrappedList(listOf(cryptoCurrencyStatus.currency.name)),
                    ),
                    message = resourceReference(R.string.token_details_hide_alert_message),
                    onConfirmClick = { onPerformHideToken(cryptoCurrencyStatus) },
                ),
            )
        }
    }

    private fun showSnackbar(message: TextReference) {
        uiState = stateFactory.getStateAndTriggerEvent(
            state = uiState,
            event = WalletEvent.ShowToast(message),
            setUiState = { uiState = it },
        )
    }

    private fun getContentItemsUpdates(index: Int) {
        /*
         * When wallet is changed it's necessary to stop the last jobs.
         * If jobs aren't stopped and wallet is changed then it will update state for the prev wallet.
         */
        tokensJobHolder.update(job = null)
        updateWcJobHolder.update(job = null)
        marketPriceJobHolder.update(job = null)
        buttonsJobHolder.update(job = null)
        notificationsJobHolder.update(job = null)
        refreshContentJobHolder.update(job = null)

        val wallet = getWallet(index)

        when {
            wallet.isLocked -> {
                uiState = stateFactory.getLockedState()
            }
            wallet.isMultiCurrency -> getMultiCurrencyContent(wallet, index)
            isSingleWalletWithTokens(wallet) -> getSingleCurrencyWithTokenContent(index)
            !wallet.isMultiCurrency -> getSingleCurrencyContent(index)
        }
    }

    private fun getMultiCurrencyContent(wallet: UserWallet, walletIndex: Int) {
        val state = requireNotNull(uiState as? WalletMultiCurrencyState) {
            "Impossible to get a token list updates if state isn't WalletMultiCurrencyState"
        }

        val tokenListFlow = getTokenListUseCase(userWalletId = state.walletsListConfig.wallets[walletIndex].id)
            .shareIn(viewModelScope, SharingStarted.WhileSubscribed())

        initAndSetupWc(tokenListFlow, wallet)

        tokenListFlow
            .distinctUntilChanged()
            .onEach { maybeTokenList ->
                uiState = stateFactory.getStateByTokensList(maybeTokenList.getTokenListWithWallet(wallet))

                maybeTokenList.onRight { checkMultiWalletWithFunds(it) }

                updateNotifications(
                    index = walletIndex,
                    tokenList = maybeTokenList.fold(ifLeft = { null }, ifRight = { it }),
                )
            }
            .flowOn(dispatchers.io)
            .launchIn(viewModelScope)
            .saveIn(tokensJobHolder)
    }

    private suspend fun checkMultiWalletWithFunds(tokenList: TokenList) {
        val hasNonZeroWallets = when (tokenList) {
            is TokenList.GroupedByNetwork -> {
                tokenList.groups
                    .flatMap(NetworkGroup::currencies)
                    .hasNonZeroWallets()
            }
            is TokenList.Ungrouped -> tokenList.currencies.hasNonZeroWallets()
            is TokenList.Empty -> false
        }

        if (hasNonZeroWallets) {
            setWalletWithFundsFoundUseCase()
        }
    }

    private fun initAndSetupWc(tokenListFlow: MaybeTokenListFlow, wallet: UserWallet) {
        viewModelScope
            .launch(dispatchers.main) {
                initWalletConnectForWallet(wallet)

                tokenListFlow
                    .filterLoadedTokenList()
                    .take(count = 1)
                    .collect { setupWalletConnectOnWallet(wallet) }
            }
            .saveIn(updateWcJobHolder)
    }

    private fun initWalletConnectForWallet(userWallet: UserWallet) {
        reduxStateHolder.dispatch(
            action = WalletConnectActions.New.Initialize(userWallet = userWallet),
        )
    }

    private fun setupWalletConnectOnWallet(userWallet: UserWallet) {
        reduxStateHolder.dispatch(
            action = WalletConnectActions.New.SetupUserChains(userWallet = userWallet),
        )
    }

    private fun isSingleWalletWithTokens(userWallet: UserWallet): Boolean {
        return userWallet.scanResponse.walletData?.token != null && !userWallet.isMultiCurrency
    }

    private fun List<CryptoCurrencyStatus>.isAllCurrenciesLoaded(): Boolean {
        return !this.any { it.value is CryptoCurrencyStatus.Loading }
    }

    private fun MaybeTokenListFlow.filterLoadedTokenList(): MaybeTokenListFlow {
        return filter { either ->
            either.fold(
                ifRight = { list ->
                    when (list) {
                        is TokenList.Ungrouped -> {
                            list.currencies.isAllCurrenciesLoaded()
                        }
                        is TokenList.GroupedByNetwork -> {
                            list.groups.flatMap(NetworkGroup::currencies).isAllCurrenciesLoaded()
                        }
                        else -> false
                    }
                },
                ifLeft = { false },
            )
        }
    }

    private fun List<CryptoCurrencyStatus>.hasNonZeroWallets(): Boolean {
        return any {
            val amount = it.value.amount ?: return@any false
            !amount.isZero()
        }
    }

    private fun Either<TokenListError, TokenList>.getTokenListWithWallet(
        userWallet: UserWallet,
    ): Either<TokenListError, TokenListWithWallet> {
        return this.map {
            TokenListWithWallet(it, userWallet)
        }
    }

    private fun getSingleCurrencyContent(index: Int) {
        val wallet = getWallet(index)
        getPrimaryCurrencyStatusUpdatesUseCase(wallet.walletId)
            .distinctUntilChanged()
            .onEach { maybeCryptoCurrencyStatus ->
                uiState = stateFactory.getSingleCurrencyLoadedBalanceState(maybeCryptoCurrencyStatus)

                maybeCryptoCurrencyStatus.onRight { status ->
                    singleWalletCryptoCurrencyStatus = status

                    if (status.value.amount?.isZero() == false) {
                        setWalletWithFundsFoundUseCase()
                    }

                    updateNotifications(index)
                    updateButtons(wallet.walletId, status)
                    updateTxHistory(wallet.walletId, status.currency, refresh = false)
                }
            }
            .flowOn(dispatchers.io)
            .launchIn(viewModelScope)
            .saveIn(marketPriceJobHolder)
    }

    private fun getSingleCurrencyWithTokenContent(walletIndex: Int) {
        val state = requireNotNull(uiState as? WalletMultiCurrencyState) {
            "Impossible to get a token list updates if state isn't WalletMultiCurrencyState"
        }

        val wallet = getWallet(walletIndex)

        getCardTokensListUseCase(userWalletId = state.walletsListConfig.wallets[walletIndex].id)
            .distinctUntilChanged()
            .onEach { maybeTokenList ->
                uiState = stateFactory.getStateByTokensList(maybeTokenList.getTokenListWithWallet(wallet))

                maybeTokenList.onRight { checkMultiWalletWithFunds(it) }

                updateNotifications(
                    index = walletIndex,
                    tokenList = maybeTokenList.fold(ifLeft = { null }, ifRight = { it }),
                )
            }
            .flowOn(dispatchers.io)
            .launchIn(viewModelScope)
            .saveIn(tokensJobHolder)
    }

    private fun updateTxHistory(userWalletId: UserWalletId, currency: CryptoCurrency, refresh: Boolean) {
        viewModelScope.launch(dispatchers.io) {
            val txHistoryItemsCountEither = txHistoryItemsCountUseCase(
                userWalletId = userWalletId,
                network = currency.network,
            )

            uiState = stateFactory.getLoadingTxHistoryState(
                itemsCountEither = txHistoryItemsCountEither,
            )

            txHistoryItemsCountEither.onRight {
                uiState = stateFactory.getLoadedTxHistoryState(
                    txHistoryEither = txHistoryItemsUseCase(
                        userWalletId = userWalletId,
                        currency = currency,
                        refresh = refresh,
                    ).map {
                        it.cachedIn(viewModelScope)
                    },
                )
            }
        }
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
            selectedWalletId = getWallet(index).walletId,
            cardTypesResolver = getCardTypeResolver(index = index),
            cryptoCurrencyList = if (tokenList != null) {
                when (tokenList) {
                    is TokenList.GroupedByNetwork -> {
                        tokenList.groups
                            .flatMap(NetworkGroup::currencies)
                    }
                    is TokenList.Ungrouped -> tokenList.currencies
                    is TokenList.Empty -> emptyList()
                }
            } else {
                listOfNotNull(singleWalletCryptoCurrencyStatus)
            },
        )
            .distinctUntilChanged()
            .onEach { uiState = stateFactory.getStateByNotifications(notifications = it) }
            .flowOn(dispatchers.io)
            .launchIn(viewModelScope)
            .saveIn(notificationsJobHolder)
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

    private fun getWallet(index: Int): UserWallet {
        return requireNotNull(
            value = wallets.getOrNull(index),
            lazyMessage = { "WalletsList doesn't contain element with index = $index" },
        )
    }

    private fun getSelectedWallet(): UserWallet {
        val state = uiState as? WalletState.ContentState
            ?: error("Unable to get selected user wallet")

        return getWallet(state.walletsListConfig.selectedWalletIndex)
    }

    private fun getCardTypeResolver(index: Int): CardTypesResolver = getWallet(index).scanResponse.cardTypesResolver
}

typealias MaybeTokenListFlow = Flow<Either<TokenListError, TokenList>>