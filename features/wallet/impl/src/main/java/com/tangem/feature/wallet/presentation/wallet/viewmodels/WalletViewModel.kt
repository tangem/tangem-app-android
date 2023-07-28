package com.tangem.feature.wallet.presentation.wallet.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.*
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
import com.tangem.domain.userwallets.UserWalletBuilder
import com.tangem.domain.wallets.models.UserWalletId
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
[REDACTED_AUTHOR]
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
    private val dispatchers: CoroutineDispatcherProvider,
) : ViewModel(), DefaultLifecycleObserver, WalletClickCallbacks {

    /** Feature router */
    var router: InnerWalletRouter by Delegates.notNull()

    private val notificationsListFactory = WalletNotificationsListFactory(
        currentStateProvider = Provider { uiState },
        wasCardScannedCallback = getCardWasScannedUseCase::invoke,
        isUserAlreadyRateAppCallback = isUserAlreadyRateAppUseCase::invoke,
        isDemoCardCallback = isDemoCardUseCase::invoke,
        clickCallbacks = this,
    )

    private val stateFactory = WalletStateFactory(
        currentStateProvider = Provider { uiState },
        clickCallbacks = this,
    )

    /** Screen state */
    var uiState: WalletStateHolder by mutableStateOf(stateFactory.getInitialState())
        private set

    private var cardTypeResolverMap: Map<UserWalletId, CardTypesResolver> by Delegates.notNull()

    private val getTokensJobHolder = JobHolder()
    private val getNotificationJobHolder = JobHolder()

    override fun onCreate(owner: LifecycleOwner) {
        getWalletsUseCase()
            .flowWithLifecycle(owner.lifecycle)
            .distinctUntilChanged()
            .onEach { wallets ->
                if (wallets.isEmpty()) return@onEach

                cardTypeResolverMap = wallets.associate { wallet ->
                    // TODO remove after adding release logic for GetTokenListUseCase
                    if (wallet.scanResponse.cardTypesResolver.isMultiwalletAllowed()) {
                        UserWalletId("123")
                    } else {
                        UserWalletId("321")
                    } to wallet.scanResponse.cardTypesResolver
                }

                uiState = stateFactory.getSkeletonState(wallets = wallets)

                updateContentItems(index = 0)
            }
            .flowOn(dispatchers.io)
            .launchIn(viewModelScope)
    }

    private fun updateContentItems(index: Int, isRefreshing: Boolean = false) {
        // TODO: use GetTransactionsUseCase

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
            .saveIn(jobHolder = getTokensJobHolder)
    }

    private fun updateNotifications(index: Int, tokenList: TokenList) {
        notificationsListFactory.create(
            cardTypesResolver = requireNotNull(
                value = cardTypeResolverMap[uiState.walletsListConfig.wallets[index].id],
                lazyMessage = {
                    "CardTypeResolverMap doesn't contain this id = ${uiState.walletsListConfig.wallets[index].id}"
                },
            ),
            tokenList = tokenList,
        )
            .distinctUntilChanged()
            .onEach { uiState = stateFactory.getStateByNotifications(notifications = it) }
            .flowOn(dispatchers.io)
            .launchIn(viewModelScope)
            .saveIn(jobHolder = getNotificationJobHolder)
    }

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
        // TODO: [REDACTED_JIRA]
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
        // TODO: [REDACTED_JIRA]
    }

    override fun onShareClick() {
        // TODO: [REDACTED_JIRA]
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

    override fun onOrganizeTokensClick() = router.openOrganizeTokensScreen()
}