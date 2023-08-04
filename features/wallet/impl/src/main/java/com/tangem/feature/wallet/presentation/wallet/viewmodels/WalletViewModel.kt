package com.tangem.feature.wallet.presentation.wallet.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.*
import androidx.paging.cachedIn
import com.tangem.blockchain.common.DerivationStyle
import com.tangem.common.Provider
import com.tangem.common.doOnFailure
import com.tangem.common.doOnSuccess
import com.tangem.domain.card.*
import com.tangem.domain.common.CardTypesResolver
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.demo.IsDemoCardUseCase
import com.tangem.domain.settings.IsUserAlreadyRateAppUseCase
import com.tangem.domain.tokens.GetTokenListUseCase
import com.tangem.domain.tokens.model.TokenList
import com.tangem.domain.tokens.models.Network
import com.tangem.domain.txhistory.usecase.GetTxHistoryItemsCountUseCase
import com.tangem.domain.txhistory.usecase.GetTxHistoryItemsUseCase
import com.tangem.domain.userwallets.UserWalletBuilder
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.usecase.GetExploreUrlUseCase
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.domain.wallets.usecase.SaveWalletUseCase
import com.tangem.feature.wallet.presentation.router.InnerWalletRouter
import com.tangem.feature.wallet.presentation.wallet.state.*
import com.tangem.feature.wallet.presentation.wallet.state.factory.WalletStateFactory
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.properties.Delegates

/**
 * Wallet screen view model
 *
* [REDACTED_AUTHOR]
 */
@Suppress("LongParameterList")
@HiltViewModel
internal class WalletViewModel @Inject constructor(
    private val getWalletsUseCase: GetWalletsUseCase,
    private val saveWalletUseCase: SaveWalletUseCase,
    private val getBiometricsStatusUseCase: GetBiometricsStatusUseCase,
    private val setAccessCodeRequestPolicyUseCase: SetAccessCodeRequestPolicyUseCase,
    private val getAccessCodeSavingStatusUseCase: GetAccessCodeSavingStatusUseCase,
    private val getTokenListUseCase: GetTokenListUseCase,
    private val getCardWasScannedUseCase: GetCardWasScannedUseCase,
    private val isUserAlreadyRateAppUseCase: IsUserAlreadyRateAppUseCase,
    private val isDemoCardUseCase: IsDemoCardUseCase,
    private val scanCardProcessor: ScanCardProcessor,
    private val txHistoryItemsCountUseCase: GetTxHistoryItemsCountUseCase,
    private val txHistoryItemsUseCase: GetTxHistoryItemsUseCase,
    private val getExploreUrlUseCase: GetExploreUrlUseCase,
    private val dispatchers: CoroutineDispatcherProvider,
) : ViewModel(), DefaultLifecycleObserver, WalletClickIntents {

    /** Feature router */
    var router: InnerWalletRouter by Delegates.notNull()

    private val notificationsListFactory = WalletNotificationsListFactory(
        currentStateProvider = Provider { uiState },
        wasCardScannedCallback = getCardWasScannedUseCase::invoke,
        isUserAlreadyRateAppCallback = isUserAlreadyRateAppUseCase::invoke,
        isDemoCardCallback = isDemoCardUseCase::invoke,
        clickIntents = this,
    )

    private val stateFactory = WalletStateFactory(
        currentStateProvider = Provider { uiState },
        currentCardTypeResolverProvider = Provider {
            getCardTypeResolver(index = uiState.walletsListConfig.selectedWalletIndex)
        },
        clickIntents = this,
    )

    /** Screen state */
    var uiState: WalletStateHolder by mutableStateOf(stateFactory.getInitialState())
        private set

    private var wallets: List<UserWallet> by Delegates.notNull()

    private val tokensJobHolder = JobHolder()
    private val notificationsJobHolder = JobHolder()

    override fun onCreate(owner: LifecycleOwner) {
        getWalletsUseCase()
            .flowWithLifecycle(owner.lifecycle)
            .distinctUntilChanged()
            .onEach { wallets ->
                if (wallets.isEmpty()) return@onEach
                this.wallets = wallets

                uiState = stateFactory.getSkeletonState(wallets = wallets)

                updateContentItems(index = 0)
            }
            .flowOn(dispatchers.io)
            .launchIn(viewModelScope)
    }

    private fun updateContentItems(index: Int, isRefreshing: Boolean = false) {
        val cardTypeResolver = getCardTypeResolver(index)
        if (cardTypeResolver.isMultiwalletAllowed()) {
            updateByTokensList(index, isRefreshing)
        } else {
            updateByTxHistory(index)
        }
    }

    private fun updateByTokensList(index: Int, isRefreshing: Boolean) {
        getTokenListUseCase(userWalletId = uiState.walletsListConfig.wallets[index].id)
            .distinctUntilChanged()
            .onEach { tokenListEither ->
                uiState = stateFactory.getStateByTokensList(
                    tokenListEither = tokenListEither,
                    isRefreshing = isRefreshing,
                )

                tokenListEither.onRight { updateNotifications(index = index, tokenList = it) }
            }
            .flowOn(dispatchers.io)
            .launchIn(viewModelScope)
            .saveIn(tokensJobHolder)
    }

    private fun updateByTxHistory(index: Int) {
        viewModelScope.launch(dispatchers.io) {
            val blockchain = getWallet(index).scanResponse.cardTypesResolver.getBlockchain()

            val txHistoryItemsCountEither = txHistoryItemsCountUseCase(
                networkId = blockchain.id,
                derivationPath = requireNotNull(blockchain.derivationPath(style = DerivationStyle.LEGACY)).rawPath,
            )

            uiState = stateFactory.getLoadingTxHistoryState(itemsCountEither = txHistoryItemsCountEither)

            txHistoryItemsCountEither.onRight { updateTxHistory(networkId = blockchain.id) }
            updateNotifications(index)
        }
    }

    private fun updateTxHistory(networkId: String) {
        uiState = stateFactory.getLoadedTxHistoryState(
            txHistoryEither = txHistoryItemsUseCase(networkId = networkId).map { it.cachedIn(viewModelScope) },
        )
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
            .saveIn(jobHolder = notificationsJobHolder)
    }

    private fun getWallet(index: Int): UserWallet {
        return requireNotNull(
            value = wallets.getOrNull(index),
            lazyMessage = { "WalletsList doesn't contain element with index = $index" },
        )
    }

    private fun getCardTypeResolver(index: Int): CardTypesResolver = getWallet(index).scanResponse.cardTypesResolver

    override fun onBackClick() = router.popBackStack()

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
        uiState = stateFactory.getStateWithOpenBottomSheet(
            content = WalletBottomSheetConfig.BottomSheetContentConfig.CriticalWarningAlreadySignedHashes(
                onOkClick = {},
                onCancelClick = {},
            ),
        )
    }

    override fun onCloseWarningAlreadySignedHashesClick() {
// [REDACTED_TODO_COMMENT]
    }

    override fun onLikeTangemAppClick() {
        uiState = stateFactory.getStateWithOpenBottomSheet(
            content = WalletBottomSheetConfig.BottomSheetContentConfig.LikeTangemApp(
                onRateTheAppClick = ::onRateTheAppClick,
                onShareClick = ::onShareClick,
            ),
        )
    }

    override fun onRateTheAppClick() {
// [REDACTED_TODO_COMMENT]
    }

    override fun onShareClick() {
// [REDACTED_TODO_COMMENT]
    }

    override fun onWalletChange(index: Int) {
        if (uiState.walletsListConfig.selectedWalletIndex == index) return

        uiState = stateFactory.getStateAfterWalletChanging(index = index)

        updateContentItems(index = index)
    }

    override fun onRefreshSwipe() {
        uiState = stateFactory.getStateAfterContentRefreshing()
        updateContentItems(index = uiState.walletsListConfig.selectedWalletIndex, isRefreshing = true)
    }

    override fun onOrganizeTokensClick() {
        val index = uiState.walletsListConfig.selectedWalletIndex
        val walletId = uiState.walletsListConfig.wallets[index].id

        router.openOrganizeTokensScreen(walletId)
    }

    override fun onBuyClick() {
// [REDACTED_TODO_COMMENT]
    }

    override fun onReloadClick() {
        uiState = stateFactory.getStateAfterContentRefreshing()
        updateByTxHistory(index = uiState.walletsListConfig.selectedWalletIndex)
    }

    override fun onExploreClick() {
        viewModelScope.launch(dispatchers.io) {
            val wallet = getWallet(uiState.walletsListConfig.selectedWalletIndex)
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
}
