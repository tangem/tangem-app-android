package com.tangem.feature.wallet.presentation.wallet.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.*
import arrow.core.Either
import com.tangem.common.Provider
import com.tangem.common.doOnFailure
import com.tangem.common.doOnSuccess
import com.tangem.core.ui.components.transactions.TransactionState
import com.tangem.domain.card.GetAccessCodeSavingStatusUseCase
import com.tangem.domain.card.GetBiometricsStatusUseCase
import com.tangem.domain.card.ScanCardProcessor
import com.tangem.domain.card.SetAccessCodeRequestPolicyUseCase
import com.tangem.domain.tokens.GetTokenListUseCase
import com.tangem.domain.tokens.error.TokenListError
import com.tangem.domain.tokens.model.TokenList
import com.tangem.domain.userwallets.UserWalletBuilder
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.domain.wallets.usecase.SaveWalletUseCase
import com.tangem.feature.wallet.presentation.common.state.TokenItemState
import com.tangem.feature.wallet.presentation.router.InnerWalletRouter
import com.tangem.feature.wallet.presentation.wallet.state.*
import com.tangem.feature.wallet.presentation.wallet.state.builder.WalletStateFactory
import com.tangem.feature.wallet.presentation.wallet.utils.TokenListErrorToWalletStateConverter
import com.tangem.feature.wallet.presentation.wallet.utils.TokenListToWalletStateConverter
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    private val saveWalletUseCase: SaveWalletUseCase,
    private val getBiometricsStatusUseCase: GetBiometricsStatusUseCase,
    private val setAccessCodeRequestPolicyUseCase: SetAccessCodeRequestPolicyUseCase,
    private val getAccessCodeSavingStatusUseCase: GetAccessCodeSavingStatusUseCase,
    private val getTokenListUseCase: GetTokenListUseCase,
    private val getWalletsUseCase: GetWalletsUseCase,
    private val scanCardProcessor: ScanCardProcessor,
    private val dispatchers: CoroutineDispatcherProvider,
) : ViewModel(), DefaultLifecycleObserver {

    /** Feature router */
    var router: InnerWalletRouter by Delegates.notNull()

    private val stateFactory = WalletStateFactory(
        routerProvider = Provider { router },
        onScanCardClick = ::onScanCardClick,
        onWalletChange = ::changeWallet,
        onRefreshSwipe = ::refreshContent,
    )

    /** Screen state */
    var uiState by mutableStateOf(stateFactory.getInitialState())
        private set

    override fun onCreate(owner: LifecycleOwner) {
        getWalletsUseCase()
            .distinctUntilChanged()
            .flowWithLifecycle(owner.lifecycle)
            .onEach { wallets ->
                if (wallets.isEmpty()) return@onEach

                uiState = stateFactory.getContentState(wallets = wallets)
                updateContentItems()
            }
            .flowOn(dispatchers.io)
            .launchIn(viewModelScope)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun updateContentItems() {
        getTokenListUseCase(
            userWalletId = uiState.walletsListConfig.wallets.get(
                index = uiState.walletsListConfig.selectedWalletIndex,
            ).id,
        )
            .distinctUntilChanged()
            .mapLatest(::updateStateWithTokenListOrError)
            .onEach {
                uiState = it.copySealed(
                    pullToRefreshConfig = uiState.pullToRefreshConfig.copy(isRefreshing = getRefreshingStatus()),
                )
            }
            .flowOn(dispatchers.io)
            .launchIn(viewModelScope)
    }

    private fun getRefreshingStatus(): Boolean {
        return uiState.contentItems.any { state ->
            val isMultiCurrencyItem = state as? WalletContentItemState.MultiCurrencyItem.Token
            val isSingleCurrencyItem = state as? WalletContentItemState.SingleCurrencyItem.Transaction

            isMultiCurrencyItem?.state is TokenItemState.Loading ||
                isSingleCurrencyItem?.state is TransactionState.Loading ||
                state is WalletContentItemState.Loading
        }
    }

    private fun onScanCardClick() {
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

    private fun changeWallet(index: Int) {
        if (uiState.walletsListConfig.selectedWalletIndex == index) return

        uiState = when (val state = uiState) {
            is WalletStateHolder.MultiCurrencyContent -> state.copy(
                walletsListConfig = uiState.walletsListConfig.copy(selectedWalletIndex = index),
            )
            is WalletStateHolder.SingleCurrencyContent -> state.copy(
                walletsListConfig = uiState.walletsListConfig.copy(selectedWalletIndex = index),
            )
            is WalletStateHolder.UnlockWalletContent -> state.copy(
                walletsListConfig = uiState.walletsListConfig.copy(selectedWalletIndex = index),
            )
            is WalletStateHolder.Loading -> state
        }

        updateContentItems()
    }

    private fun refreshContent() {
        uiState = uiState.copySealed(pullToRefreshConfig = uiState.pullToRefreshConfig.copy(isRefreshing = true))
        updateContentItems()
    }

    private fun updateStateWithTokenListOrError(tokenList: Either<TokenListError, TokenList>): WalletStateHolder {
        val updateStateWithError = { error: TokenListError ->
            val converter = TokenListErrorToWalletStateConverter(uiState)

            converter.convert(error)
        }
        val updateState = { list: TokenList ->
            val converter = TokenListToWalletStateConverter(
                uiState,
                isWalletContentHidden = false, // TODO: https://tangem.atlassian.net/browse/AND-4007
                fiatCurrencyCode = "USD", // TODO: https://tangem.atlassian.net/browse/AND-4006
                fiatCurrencySymbol = "$", // TODO: https://tangem.atlassian.net/browse/AND-4006
            )

            converter.convert(list)
        }

        return tokenList.fold(ifLeft = updateStateWithError, ifRight = updateState)
    }
}
