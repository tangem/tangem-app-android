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
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.common.extensions.isZero
import com.tangem.common.extensions.toMapKey
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.navigation.AppScreen
import com.tangem.core.ui.components.bottomsheets.chooseaddress.ChooseAddressBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.tokenreceive.AddressModel
import com.tangem.core.ui.components.bottomsheets.tokenreceive.TokenReceiveBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.tokenreceive.mapToAddressModels
import com.tangem.core.ui.components.transactions.state.TxHistoryState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.WrappedList
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.domain.analytics.ChangeCardAnalyticsContextUseCase
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.card.DerivePublicKeysUseCase
import com.tangem.domain.card.SetCardWasScannedUseCase
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
import com.tangem.domain.tokens.model.*
import com.tangem.domain.tokens.models.analytics.TokenReceiveAnalyticsEvent
import com.tangem.domain.tokens.models.analytics.TokenScreenAnalyticsEvent
import com.tangem.domain.txhistory.usecase.GetExplorerTransactionUrlUseCase
import com.tangem.domain.txhistory.usecase.GetTxHistoryItemsCountUseCase
import com.tangem.domain.txhistory.usecase.GetTxHistoryItemsUseCase
import com.tangem.domain.walletconnect.WalletConnectActions
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.models.UnlockWalletsError
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.usecase.*
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.router.InnerWalletRouter
import com.tangem.feature.wallet.presentation.wallet.analytics.PortfolioEvent
import com.tangem.feature.wallet.presentation.wallet.analytics.WalletScreenAnalyticsEvent
import com.tangem.feature.wallet.presentation.wallet.domain.HasSingleWalletSignedHashesUseCase
import com.tangem.feature.wallet.presentation.wallet.domain.ScanCardToUnlockWalletClickHandler
import com.tangem.feature.wallet.presentation.wallet.domain.ScanCardToUnlockWalletError
import com.tangem.feature.wallet.presentation.wallet.state.*
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletCardState
import com.tangem.feature.wallet.presentation.wallet.state.factory.TokenListWithWallet
import com.tangem.feature.wallet.presentation.wallet.state.factory.WalletStateFactory
import com.tangem.feature.wallet.presentation.wallet.subscribers.MaybeTokenListFlow
import com.tangem.operations.derivation.ExtendedPublicKeysMap
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import com.tangem.utils.extensions.DELAY_SDK_DIALOG_CLOSE
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
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
    getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
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
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    private val removeCurrencyUseCase: RemoveCurrencyUseCase,
    private val isCryptoCurrencyCoinCouldHide: IsCryptoCurrencyCoinCouldHideUseCase,
    private val walletManagersFacade: WalletManagersFacade,
    private val reduxStateHolder: ReduxStateHolder,
    private val dispatchers: CoroutineDispatcherProvider,
    private val analyticsEventsHandler: AnalyticsEventHandler,
    private val changeCardAnalyticsContextUseCase: ChangeCardAnalyticsContextUseCase,
    private val setCardWasScannedUseCase: SetCardWasScannedUseCase,
    private val remindToRateAppLaterUseCase: RemindToRateAppLaterUseCase,
    private val neverToSuggestRateAppUseCase: NeverToSuggestRateAppUseCase,
    private val setWalletWithFundsFoundUseCase: SetWalletWithFundsFoundUseCase,
    private val getExplorerTransactionUrlUseCase: GetExplorerTransactionUrlUseCase,
    private val isDemoCardUseCase: IsDemoCardUseCase,
    private val scanCardToUnlockWalletUseCase: ScanCardToUnlockWalletClickHandler,
    private val shouldShowSwapPromoWalletUseCase: ShouldShowSwapPromoWalletUseCase,
    isReadyToShowRateAppUseCase: IsReadyToShowRateAppUseCase,
    isNeedToBackupUseCase: IsNeedToBackupUseCase,
    getMissedAddressesCryptoCurrenciesUseCase: GetMissedAddressesCryptoCurrenciesUseCase,
    hasSingleWalletSignedHashesUseCase: HasSingleWalletSignedHashesUseCase,
    // endregion Parameters
) : ViewModel(), DefaultLifecycleObserver, WalletClickIntents {

    /** Feature router */
    var router: InnerWalletRouter by Delegates.notNull()

    private val selectedAppCurrencyFlow: StateFlow<AppCurrency> = createSelectedAppCurrencyFlow()

    private val notificationsListFactory = WalletNotificationsListFactory(
        isDemoCardUseCase = isDemoCardUseCase,
        isReadyToShowRateAppUseCase = isReadyToShowRateAppUseCase,
        isNeedToBackupUseCase = isNeedToBackupUseCase,
        getMissedAddressCryptoCurrenciesUseCase = getMissedAddressesCryptoCurrenciesUseCase,
        hasSingleWalletSignedHashesUseCase = hasSingleWalletSignedHashesUseCase,
        shouldShowSwapPromoWalletUseCase = shouldShowSwapPromoWalletUseCase,
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
        getSelectedWalletSyncUseCase = getSelectedWalletSyncUseCase,
    )

    override fun onCreate(owner: LifecycleOwner) {
        analyticsEventsHandler.send(WalletScreenAnalyticsEvent.MainScreen.ScreenOpened)

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

        getBalanceHidingSettingsUseCase()
            .flowWithLifecycle(owner.lifecycle)
            .onEach {
                isBalanceHidden = it.isBalanceHidden
                WalletStateCache.updateAll { copySealed(isBalanceHidden = it.isBalanceHidden) }
                uiState = stateFactory.getHiddenBalanceState(isBalanceHidden = it.isBalanceHidden)
            }
            .launchIn(viewModelScope)
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
        router.popBackStack()
    }

    override fun onGenerateMissedAddressesClick(missedAddressCurrencies: List<CryptoCurrency>) {
        val state = uiState as? WalletState.ContentState ?: return

        analyticsEventsHandler.send(WalletScreenAnalyticsEvent.Basic.CardWasScanned(AnalyticsParam.ScannedFrom.Main))
        analyticsEventsHandler.send(WalletScreenAnalyticsEvent.MainScreen.NoticeScanYourCardTapped)

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
                getNewDerivations(curve, scanResponse, it)
            }
        }

        val derivations = buildMap<ByteArrayKey, MutableList<DerivationPath>> {
            derivationDataList.forEach {
                val current = this[it.derivations.first]
                if (current != null) {
                    current.addAll(it.derivations.second)
                    current.distinct()
                } else {
                    this[it.derivations.first] = it.derivations.second.toMutableList()
                }
            }
        }.ifEmpty { return }

        viewModelScope.launch(dispatchers.io) {
            derivePublicKeysUseCase(cardId = null, derivations = derivations)
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
        currency: CryptoCurrency,
    ): DerivationData? {
        val wallet = scanResponse.card.wallets.firstOrNull { it.curve == curve } ?: return null

        val blockchain = Blockchain.fromId(currency.network.id.value)
        val supportedCurves = blockchain.getSupportedCurves()
        val path = blockchain.derivationPath(scanResponse.derivationStyleProvider.getDerivationStyle())
            .takeIf { supportedCurves.contains(curve) }

        val customPath = currency.network.derivationPath.value?.let {
            DerivationPath(it)
        }.takeIf { supportedCurves.contains(curve) }

        val bothCandidates = listOfNotNull(path, customPath).distinct().toMutableList()
        if (bothCandidates.isEmpty()) return null

        if (currency is CryptoCurrency.Coin && blockchain == Blockchain.Cardano) {
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

        analyticsEventsHandler.send(event = WalletScreenAnalyticsEvent.MainScreen.WalletUnlockTapped)

        val lockedWallet = getWallet(index = state.walletsListConfig.selectedWalletIndex)

        viewModelScope.launch(dispatchers.main) {
            scanCardToUnlockWalletUseCase(walletId = lockedWallet.walletId)
                .onLeft { error ->
                    when (error) {
                        ScanCardToUnlockWalletError.WrongCardIsScanned -> {
                            delay(timeMillis = DELAY_SDK_DIALOG_CLOSE)

                            uiState = stateFactory.getStateAndTriggerEvent(
                                state = uiState,
                                event = WalletEvent.ShowAlert(state = WalletAlertState.WrongCardIsScanned),
                                setUiState = { uiState = it },
                            )
                        }
                        ScanCardToUnlockWalletError.ManyScanFails -> {
                            router.openScanFailedDialog()
                        }
                    }
                }
        }
    }

    override fun onDetailsClick() = router.openDetailsScreen()

    override fun onBackupCardClick() {
        analyticsEventsHandler.send(WalletScreenAnalyticsEvent.MainScreen.NoticeBackupYourWalletTapped)
        reduxStateHolder.dispatch(
            LegacyAction.StartOnboardingProcess(
                scanResponse = getSelectedWallet().scanResponse,
                canSkipBackup = false,
            ),
        )
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
        analyticsEventsHandler.send(
            WalletScreenAnalyticsEvent.MainScreen.NoticeRateAppButton(AnalyticsParam.RateApp.Liked),
        )
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
        analyticsEventsHandler.send(
            WalletScreenAnalyticsEvent.MainScreen.NoticeRateAppButton(AnalyticsParam.RateApp.Disliked),
        )
        viewModelScope.launch(dispatchers.main) {
            neverToSuggestRateAppUseCase()

            reduxStateHolder.dispatch(LegacyAction.SendEmailRateCanBeBetter)
        }
    }

    override fun onCloseRateAppNotificationClick() {
        analyticsEventsHandler.send(
            WalletScreenAnalyticsEvent.MainScreen.NoticeRateAppButton(AnalyticsParam.RateApp.Closed),
        )
        viewModelScope.launch(dispatchers.main) {
            remindToRateAppLaterUseCase()
        }
    }

    override fun onWalletChange(index: Int) {
        val state = uiState as? WalletState.ContentState ?: return
        if (state.walletsListConfig.selectedWalletIndex == index) return

        changeCardAnalyticsContextUseCase(scanResponse = getWallet(index).scanResponse)
        analyticsEventsHandler.send(WalletScreenAnalyticsEvent.MainScreen.WalletSwipe)

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
                selectWalletUseCase(userWallet.id)
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

            val maybeFetchResult = if (wallet.scanResponse.cardTypesResolver.isSingleWalletWithToken()) {
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

        analyticsEventsHandler.send(
            event = TokenScreenAnalyticsEvent.ButtonBuy(cryptoCurrencyStatus.currency.symbol),
        )

        showErrorIfDemoModeOrElse {
            reduxStateHolder.dispatch(
                TradeCryptoAction.New.Buy(
                    userWallet = getWallet(index = state.walletsListConfig.selectedWalletIndex),
                    cryptoCurrencyStatus = cryptoCurrencyStatus,
                    appCurrencyCode = selectedAppCurrencyFlow.value.code,
                ),
            )
        }
    }

    override fun onSwapClick(cryptoCurrencyStatus: CryptoCurrencyStatus) {
        analyticsEventsHandler.send(
            event = TokenScreenAnalyticsEvent.ButtonExchange(cryptoCurrencyStatus.currency.symbol),
        )

        reduxStateHolder.dispatch(TradeCryptoAction.New.Swap(cryptoCurrencyStatus.currency))
    }

    override fun onSingleCurrencySendClick(cryptoCurrencyStatus: CryptoCurrencyStatus?) {
        val state = uiState as? WalletState.ContentState ?: return

        val userWallet = getWallet(index = state.walletsListConfig.selectedWalletIndex)
        val coinStatus = if (userWallet.isMultiCurrency) cryptoCurrencyStatus else singleWalletCryptoCurrencyStatus
        coinStatus ?: return

        analyticsEventsHandler.send(
            event = TokenScreenAnalyticsEvent.ButtonSend(coinStatus.currency.symbol),
        )

        reduxStateHolder.dispatch(
            action = TradeCryptoAction.New.SendCoin(userWallet = userWallet, coinStatus = coinStatus),
        )
    }

    override fun onMultiCurrencySendClick(cryptoCurrencyStatus: CryptoCurrencyStatus) {
        val state = uiState as? WalletState.ContentState ?: return

        analyticsEventsHandler.send(
            event = TokenScreenAnalyticsEvent.ButtonSend(cryptoCurrencyStatus.currency.symbol),
        )

        val userWallet = getWallet(index = state.walletsListConfig.selectedWalletIndex)
        when (cryptoCurrencyStatus.currency) {
            is CryptoCurrency.Coin -> {
                uiState = stateFactory.getStateWithClosedBottomSheet()
                reduxStateHolder.dispatch(
                    action = TradeCryptoAction.New.SendCoin(
                        userWallet = userWallet,
                        coinStatus = cryptoCurrencyStatus,
                    ),
                )
            }
            is CryptoCurrency.Token -> sendToken(userWallet, cryptoCurrencyStatus)
        }
    }

    private fun sendToken(userWallet: UserWallet, cryptoCurrencyStatus: CryptoCurrencyStatus) {
        viewModelScope.launch(dispatchers.io) {
            getNetworkCoinStatusUseCase(
                userWalletId = userWallet.walletId,
                networkId = cryptoCurrencyStatus.currency.network.id,
                derivationPath = cryptoCurrencyStatus.currency.network.derivationPath,
                isSingleWalletWithTokens = userWallet.scanResponse.cardTypesResolver.isSingleWalletWithToken(),
            )
                .take(count = 1)
                .collectLatest {
                    it.onRight { coinStatus ->
                        uiState = stateFactory.getStateWithClosedBottomSheet()
                        reduxStateHolder.dispatch(
                            action = TradeCryptoAction.New.SendToken(
                                userWallet = userWallet,
                                tokenCurrency = requireNotNull(cryptoCurrencyStatus.currency as? CryptoCurrency.Token),
                                tokenFiatRate = cryptoCurrencyStatus.value.fiatRate,
                                coinFiatRate = coinStatus.value.fiatRate,
                            ),
                        )
                    }
                }
        }
    }

    override fun onReceiveClick(cryptoCurrencyStatus: CryptoCurrencyStatus) {
        analyticsEventsHandler.send(
            event = TokenScreenAnalyticsEvent.ButtonReceive(cryptoCurrencyStatus.currency.symbol),
        )

        viewModelScope.launch(dispatchers.io) {
            analyticsEventsHandler.send(event = TokenReceiveAnalyticsEvent.ReceiveScreenOpened)

            val currency = cryptoCurrencyStatus.currency
            uiState = stateFactory.getStateWithOpenWalletBottomSheet(
                content = TokenReceiveBottomSheetConfig(
                    name = currency.name,
                    symbol = currency.symbol,
                    network = currency.network.name,
                    addresses = cryptoCurrencyStatus.value.networkAddress
                        ?.availableAddresses
                        ?.mapToAddressModels(currency)
                        .orEmpty()
                        .toImmutableList(),
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

        analyticsEventsHandler.send(
            event = TokenScreenAnalyticsEvent.ButtonCopyAddress(cryptoCurrencyStatus.currency.symbol),
        )

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
        analyticsEventsHandler.send(
            event = TokenScreenAnalyticsEvent.ButtonSell(cryptoCurrencyStatus.currency.symbol),
        )

        showErrorIfDemoModeOrElse {
            reduxStateHolder.dispatch(
                action = TradeCryptoAction.New.Sell(
                    cryptoCurrencyStatus = cryptoCurrencyStatus,
                    appCurrencyCode = selectedAppCurrencyFlow.value.code,
                ),
            )
        }
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

    override fun onCloseSwapPromoNotificationClick() {
        viewModelScope.launch(dispatchers.main) {
            shouldShowSwapPromoWalletUseCase.neverToShow()
            analyticsEventsHandler.send(WalletScreenAnalyticsEvent.SwapPromo)
        }
    }

    // FIXME: refreshSingleCurrencyContent mustn't update the TxHistory and Buttons. It only must fetch primary
    //  currency. Now it not works because GetPrimaryCurrency's subscriber uses .distinctUntilChanged()
    private fun refreshSingleCurrencyContent(walletIndex: Int) {
        uiState = stateFactory.getRefreshingState()
        val wallet = getWallet(walletIndex)

        viewModelScope.launch(dispatchers.main) {
            singleWalletCryptoCurrencyStatus?.let {
                updateButtons(userWallet = wallet, currencyStatus = it)
            }
            val result = fetchCurrencyStatusUseCase(wallet.walletId, refresh = true)

            uiState = stateFactory.getRefreshedState()
            uiState = result.fold(stateFactory::getStateByCurrencyStatusError) { uiState }

            singleWalletCryptoCurrencyStatus?.let {
                val singleCurrencyState = uiState as WalletSingleCurrencyState
                if (singleCurrencyState.txHistoryState !is TxHistoryState.Content) {
                    // show loading indicator while refreshing in non content state
                    uiState = stateFactory.getLoadingTxHistoryState(
                        itemsCountEither = 1.right(),
                        pendingTransactions = it.value.pendingTransactions,
                    )
                }
                updateTxHistory(userWalletId = wallet.walletId, currencyStatus = it, refresh = true)
            }
        }.saveIn(refreshContentJobHolder)
    }

    override fun onExploreClick() {
        showErrorIfDemoModeOrElse(action = ::openExplorer)
    }

    private fun openExplorer() {
        val state = uiState as? WalletState.ContentState ?: return
        val currencyStatus = singleWalletCryptoCurrencyStatus ?: return
        val currency = currencyStatus.currency

        viewModelScope.launch(dispatchers.main) {
            val userWalletId = getWallet(state.walletsListConfig.selectedWalletIndex).walletId

            when (val addresses = currencyStatus.value.networkAddress) {
                is NetworkAddress.Selectable -> {
                    uiState = stateFactory.getStateWithOpenWalletBottomSheet(
                        ChooseAddressBottomSheetConfig(
                            addressModels = addresses.availableAddresses
                                .mapToAddressModels(currency)
                                .toImmutableList(),
                            onClick = {
                                onAddressTypeSelected(
                                    userWalletId = userWalletId,
                                    currency = currency,
                                    addressModel = it,
                                )
                            },
                        ),
                    )
                }
                is NetworkAddress.Single -> {
                    router.openUrl(
                        url = getExploreUrlUseCase(
                            userWalletId = userWalletId,
                            currency = currency,
                            addressType = AddressType.Default,
                        ),
                    )
                }
                null -> Unit
            }
        }
    }

    private fun onAddressTypeSelected(
        userWalletId: UserWalletId,
        currency: CryptoCurrency,
        addressModel: AddressModel,
    ) {
        viewModelScope.launch(dispatchers.main) {
            router.openUrl(
                url = getExploreUrlUseCase(
                    userWalletId = userWalletId,
                    currency = currency,
                    addressType = AddressType.valueOf(addressModel.type.name),
                ),
            )
            uiState = stateFactory.getStateWithClosedBottomSheet()
        }
    }

    private fun showErrorIfDemoModeOrElse(action: () -> Unit) {
        val state = uiState as? WalletState.ContentState ?: return
        val cardId = getWallet(index = state.walletsListConfig.selectedWalletIndex).cardId

        if (isDemoCardUseCase(cardId = cardId)) {
            uiState = stateFactory.getStateWithClosedBottomSheet()
            uiState = stateFactory.getStateAndTriggerEvent(
                state = uiState,
                event = WalletEvent.ShowError(
                    text = resourceReference(id = R.string.alert_demo_feature_disabled),
                ),
                setUiState = { uiState = it },
            )
        } else {
            action()
        }
    }

    override fun onUnlockWalletClick() {
        analyticsEventsHandler.send(WalletScreenAnalyticsEvent.MainScreen.NoticeWalletLocked)

        viewModelScope.launch(dispatchers.main) {
            unlockWalletsUseCase(throwIfNotAllWalletsUnlocked = true)
                .onLeft(::handleUnlockWalletsError)
        }
    }

    private fun handleUnlockWalletsError(error: UnlockWalletsError) {
        val event = when (error) {
            is UnlockWalletsError.DataError,
            is UnlockWalletsError.UnableToUnlockWallets,
            -> WalletEvent.ShowToast(resourceReference(R.string.user_wallet_list_error_unable_to_unlock))
            is UnlockWalletsError.NoUserWalletSelected,
            is UnlockWalletsError.NotAllUserWalletsUnlocked,
            -> WalletEvent.ShowAlert(WalletAlertState.RescanWallets)
        }

        uiState = stateFactory.getStateAndTriggerEvent(
            state = uiState,
            event = event,
            setUiState = { uiState = it },
        )
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
        val userWallet = getSelectedWallet()
        viewModelScope.launch(dispatchers.io) {
            getCryptoCurrencyActionsUseCase(
                userWallet = userWallet,
                cryptoCurrencyStatus = cryptoCurrencyStatus,
            )
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
                    ifLeft = { showToast(resourceReference(R.string.common_error)) },
                    ifRight = { uiState = stateFactory.getStateWithClosedBottomSheet() },
                )
        }
    }

    override fun onHideTokensClick(cryptoCurrencyStatus: CryptoCurrencyStatus) {
        analyticsEventsHandler.send(
            event = TokenScreenAnalyticsEvent.ButtonRemoveToken(cryptoCurrencyStatus.currency.symbol),
        )

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

    override fun onTransactionClick(txHash: String) {
        singleWalletCryptoCurrencyStatus?.let { currencyStatus ->
            router.openUrl(
                url = getExplorerTransactionUrlUseCase(
                    txHash = txHash,
                    networkId = currencyStatus.currency.network.id,
                ),
            )
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

    private fun showToast(message: TextReference) {
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
            wallet.scanResponse.cardTypesResolver.isSingleWalletWithToken() -> getSingleCurrencyWithTokenContent(index)
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
            .conflate()
            .distinctUntilChanged()
            .onEach { maybeTokenList ->
                uiState = stateFactory.getStateByTokensList(maybeTokenList.getTokenListWithWallet(wallet))

                maybeTokenList.onRight {
                    analyticsEventsHandler.sendBalanceLoadedEvent(it)
                    checkMultiWalletWithFunds(it)
                }

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
            .conflate()
            .distinctUntilChanged()
            .onEach { maybeCryptoCurrencyStatus ->
                uiState = stateFactory.getSingleCurrencyLoadedBalanceState(maybeCryptoCurrencyStatus)

                maybeCryptoCurrencyStatus.onRight { status ->
                    val fiatAmount = status.value.fiatAmount

                    val cardBalanceState = when (status.value) {
                        is CryptoCurrencyStatus.Loaded,
                        is CryptoCurrencyStatus.NoAccount,
                        is CryptoCurrencyStatus.NoAmount,
                        -> {
                            when {
                                fiatAmount == null -> null
                                fiatAmount.isZero() -> AnalyticsParam.CardBalanceState.Empty
                                else -> AnalyticsParam.CardBalanceState.Full
                            }
                        }
                        is CryptoCurrencyStatus.NoQuote -> AnalyticsParam.CardBalanceState.NoRate
                        is CryptoCurrencyStatus.Unreachable -> AnalyticsParam.CardBalanceState.BlockchainError
                        is CryptoCurrencyStatus.MissedDerivation,
                        is CryptoCurrencyStatus.Loading,
                        is CryptoCurrencyStatus.Custom,
                        -> null
                    }

                    cardBalanceState?.let {
                        analyticsEventsHandler.send(
                            event = WalletScreenAnalyticsEvent.Basic.BalanceLoaded(balance = it),
                        )
                    }

                    singleWalletCryptoCurrencyStatus = status

                    if (status.value.amount?.isZero() == false) {
                        setWalletWithFundsFoundUseCase()
                    }

                    updateNotifications(index)
                    updateButtons(userWallet = wallet, currencyStatus = status)
                    updateTxHistory(userWalletId = wallet.walletId, currencyStatus = status, refresh = false)
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
            .conflate()
            .distinctUntilChanged()
            .onEach { maybeTokenList ->
                uiState = stateFactory.getStateByTokensList(maybeTokenList.getTokenListWithWallet(wallet))

                maybeTokenList.onRight { tokenList ->
                    analyticsEventsHandler.sendBalanceLoadedEvent(tokenList)
                    checkMultiWalletWithFunds(tokenList)
                }

                updateNotifications(
                    index = walletIndex,
                    tokenList = maybeTokenList.fold(ifLeft = { null }, ifRight = { it }),
                )
            }
            .flowOn(dispatchers.io)
            .launchIn(viewModelScope)
            .saveIn(tokensJobHolder)
    }

    private fun AnalyticsEventHandler.sendBalanceLoadedEvent(tokenList: TokenList) {
        val cardBalanceState = when (val fiatBalance = tokenList.totalFiatBalance) {
            is TokenList.FiatBalance.Failed -> {
                val currenciesStatuses = when (tokenList) {
                    is TokenList.Empty -> emptyList()
                    is TokenList.GroupedByNetwork -> tokenList.groups.flatMap(NetworkGroup::currencies)
                    is TokenList.Ungrouped -> tokenList.currencies
                }

                when {
                    currenciesStatuses.isEmpty() -> AnalyticsParam.CardBalanceState.Empty
                    currenciesStatuses.any { it.value is CryptoCurrencyStatus.NoQuote } -> {
                        AnalyticsParam.CardBalanceState.NoRate
                    }
                    else -> AnalyticsParam.CardBalanceState.BlockchainError
                }
            }
            is TokenList.FiatBalance.Loaded -> {
                if (fiatBalance.amount > BigDecimal.ZERO) {
                    AnalyticsParam.CardBalanceState.Full
                } else if (fiatBalance.amount.isZero()) {
                    AnalyticsParam.CardBalanceState.Empty
                } else {
                    null
                }
            }
            TokenList.FiatBalance.Loading -> null
        }

        cardBalanceState?.let {
            send(event = WalletScreenAnalyticsEvent.Basic.BalanceLoaded(balance = it))
        }
    }

    private fun updateTxHistory(userWalletId: UserWalletId, currencyStatus: CryptoCurrencyStatus, refresh: Boolean) {
        viewModelScope.launch(dispatchers.io) {
            val txHistoryItemsCountEither = txHistoryItemsCountUseCase(
                userWalletId = userWalletId,
                currency = currencyStatus.currency,
            )

            uiState = stateFactory.getLoadingTxHistoryState(
                itemsCountEither = txHistoryItemsCountEither,
                pendingTransactions = currencyStatus.value.pendingTransactions,
            )

            txHistoryItemsCountEither.onRight {
                uiState = stateFactory.getLoadedTxHistoryState(
                    txHistoryEither = txHistoryItemsUseCase(
                        userWalletId = userWalletId,
                        currency = currencyStatus.currency,
                        refresh = refresh,
                    ).map {
                        it.cachedIn(viewModelScope)
                    },
                )
            }
        }
    }

    private fun updateButtons(userWallet: UserWallet, currencyStatus: CryptoCurrencyStatus) {
        getCryptoCurrencyActionsUseCase(
            userWallet = userWallet,
            cryptoCurrencyStatus = currencyStatus,
        )
            .conflate()
            .distinctUntilChanged()
            .onEach { uiState = stateFactory.getSingleCurrencyManageButtonsState(actionsState = it) }
            .flowOn(dispatchers.io)
            .launchIn(viewModelScope)
            .saveIn(buttonsJobHolder)
    }

    private fun updateNotifications(index: Int, tokenList: TokenList? = null) {
        notificationsListFactory.create(
            selectedWallet = getWallet(index),
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
            .conflate()
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