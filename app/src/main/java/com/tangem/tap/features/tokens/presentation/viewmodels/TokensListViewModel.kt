package com.tangem.tap.features.tokens.presentation.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.core.analytics.Analytics
import com.tangem.core.ui.extensions.getActiveIconRes
import com.tangem.domain.common.extensions.fromNetworkId
import com.tangem.tap.common.analytics.events.AnalyticsParam
import com.tangem.tap.common.analytics.events.ManageTokens
import com.tangem.tap.common.extensions.dispatchDialogShow
import com.tangem.tap.common.extensions.dispatchNotification
import com.tangem.tap.common.extensions.fullNameWithoutTestnet
import com.tangem.tap.common.extensions.getGreyedOutIconRes
import com.tangem.tap.common.extensions.getNetworkName
import com.tangem.tap.common.redux.AppDialog
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.domain.tokens.Contract
import com.tangem.tap.domain.tokens.Currency
import com.tangem.tap.features.tokens.presentation.router.AddTokensRouter
import com.tangem.tap.features.tokens.presentation.states.NetworkItemState
import com.tangem.tap.features.tokens.presentation.states.TokenItemState
import com.tangem.tap.features.tokens.presentation.states.TokensListStateHolder
import com.tangem.tap.features.tokens.presentation.states.TokensListToolbarState
import com.tangem.tap.features.tokens.redux.TokenWithBlockchain
import com.tangem.tap.features.tokens.redux.TokensAction
import com.tangem.tap.features.tokens.redux.TokensState
import com.tangem.tap.features.tokens.ui.compose.fullName
import com.tangem.tap.features.wallet.redux.models.WalletDialog
import com.tangem.tap.store
import com.tangem.utils.coroutines.AppCoroutineDispatcherProvider
import com.tangem.wallet.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.rekotlin.StoreSubscriber
import javax.inject.Inject

/**
 * ViewModel for add tokens screen
 *
 * @property router      feature router
 * @property dispatchers coroutine dispatchers provider
 *
[REDACTED_AUTHOR]
 */
@HiltViewModel
internal class TokensListViewModel @Inject constructor(
    private val router: AddTokensRouter,
    private val dispatchers: AppCoroutineDispatcherProvider,
) : ViewModel() {

    private val uiStateBuilder = UiStateBuilder()

    var uiState by mutableStateOf(uiStateBuilder.getInitialUiState())
        private set

    private val reduxSubscriber = ReduxSubscriber()
    private var debounceJob: Job? = null

    private var originalAddedTokenList: List<TokenWithBlockchain>? = null
    private var originalAddedBlockchainList: List<Blockchain>? = null
    private var addedTokenList: List<TokenWithBlockchain>? = null
    private var addedBlockchainList: List<Blockchain>? = null

    fun subscribeOnReduxEvents(lifecycle: Lifecycle) {
        lifecycle.addObserver(reduxSubscriber)
    }

    private object AnalyticsSender {
        fun sendWhenTokenAdded(token: Token) {
            Analytics.send(
                event = ManageTokens.TokenSwitcherChanged(
                    type = AnalyticsParam.CurrencyType.Token(token),
                    state = AnalyticsParam.OnOffState.On,
                ),
            )
        }

        fun sendWhenBlockchainAdded(blockchain: Blockchain) {
            Analytics.send(
                event = ManageTokens.TokenSwitcherChanged(
                    type = AnalyticsParam.CurrencyType.Blockchain(blockchain),
                    state = AnalyticsParam.OnOffState.On,
                ),
            )
        }

        fun sendWhenTokenRemoved(token: Token) {
            Analytics.send(
                event = ManageTokens.TokenSwitcherChanged(
                    type = AnalyticsParam.CurrencyType.Token(token),
                    state = AnalyticsParam.OnOffState.Off,
                ),
            )
        }

        fun sendWhenBlockchainRemoved(blockchain: Blockchain) {
            Analytics.send(
                event = ManageTokens.TokenSwitcherChanged(
                    type = AnalyticsParam.CurrencyType.Blockchain(blockchain),
                    state = AnalyticsParam.OnOffState.Off,
                ),
            )
        }

        fun sendWhenSaveButtonClicked() {
            Analytics.send(ManageTokens.ButtonSaveChanges())
        }
    }

    private inner class ReduxSubscriber : DefaultLifecycleObserver, StoreSubscriber<TokensState> {

        override fun onStart(owner: LifecycleOwner) {
            store.subscribe(
                subscriber = this,
                transform = { state ->
                    state
                        .skipRepeats { old, new -> old.tokensState == new.tokensState }
                        .select(AppState::tokensState)
                },
            )
        }

        override fun onStop(owner: LifecycleOwner) {
            store.unsubscribe(subscriber = this)
        }

        override fun newState(state: TokensState) {
            val isNotOriginalListsInitialized = originalAddedTokenList == null || originalAddedBlockchainList == null
            val isNotCurrentListsInitialized = addedTokenList == null || addedBlockchainList == null

            if (isNotOriginalListsInitialized || isNotCurrentListsInitialized) {
                originalAddedTokenList = state.addedTokens
                originalAddedBlockchainList = state.addedBlockchains
                addedTokenList = state.addedTokens
                addedBlockchainList = state.addedBlockchains
            }

            uiStateBuilder.updateStateByReduxState(state)
        }
    }

    private inner class UiStateBuilder {

        fun getInitialUiState(): TokensListStateHolder {
            return TokensListStateHolder.Loading(toolbarState = getToolbarState())
        }

        fun updateStateByReduxState(state: TokensState) {
            uiState = if (state.currencies.isEmpty()) {
                TokensListStateHolder.Loading(toolbarState = getToolbarState(reduxState = state))
            } else if (state.allowToAdd) {
                TokensListStateHolder.ManageAccess(
                    toolbarState = getToolbarState(reduxState = state),
                    tokens = state.currencies.map(::toManageTokenItemState).toImmutableList(),
                    onSaveButtonClick = {
                        val notNullAddedTokens = addedTokenList ?: return@ManageAccess
                        val notNullAddedBlockchains = addedBlockchainList ?: return@ManageAccess

                        AnalyticsSender.sendWhenSaveButtonClicked()
                        store.dispatch(TokensAction.SaveChanges(notNullAddedTokens, notNullAddedBlockchains))
                    },
                )
            } else {
                TokensListStateHolder.ReadAccess(
                    toolbarState = getToolbarState(state),
                    tokens = state.currencies.map(::toReadTokenItemState).toImmutableList(),
                )
            }
        }

        private fun getToolbarState(reduxState: TokensState = store.state.tokensState): TokensListToolbarState {
            val toolbarState = uiState.toolbarState

            return if (reduxState.searchInput != null && toolbarState is TokensListToolbarState.SearchInputField) {
                TokensListToolbarState.SearchInputField(
                    onBackButtonClick = router::popBackStack,
                    onSearchButtonClick = ::onSearchButtonClick,
                    value = toolbarState.value,
                    onValueChange = ::onSearchValueChange,
                    onCleanButtonClick = ::onCleanButtonClick,
                )
            } else if (reduxState.allowToAdd) {
                TokensListToolbarState.Title.ManageAccess(
                    titleResId = R.string.main_manage_tokens,
                    onBackButtonClick = router::popBackStack,
                    onSearchButtonClick = ::onSearchButtonClick,
                    onAddCustomTokenClick = router::openAddCustomTokenScreen,
                )
            } else {
                TokensListToolbarState.Title.ReadAccess(
                    titleResId = R.string.search_tokens_title,
                    onBackButtonClick = router::popBackStack,
                    onSearchButtonClick = ::onSearchButtonClick,
                )
            }
        }

        private fun onSearchButtonClick() {
            uiState = when (val state = uiState.toolbarState) {
                is TokensListToolbarState.Title -> {
                    uiState.copySealed(
                        toolbarState = TokensListToolbarState.SearchInputField(
                            onBackButtonClick = uiState.toolbarState.onBackButtonClick,
                            onSearchButtonClick = uiState.toolbarState.onSearchButtonClick,
                            value = "",
                            onValueChange = ::onSearchValueChange,
                            onCleanButtonClick = ::onCleanButtonClick,
                        ),
                    )
                }
                is TokensListToolbarState.SearchInputField -> {
                    store.dispatch(TokensAction.SetSearchInput(searchInput = state.value))
                    uiState.copySealed(toolbarState = getToolbarState())
                }
            }
        }

        private fun onSearchValueChange(newValue: String) {
            (uiState.toolbarState as? TokensListToolbarState.SearchInputField)?.let { state ->
                uiState = uiState.copySealed(toolbarState = state.copy(value = newValue))
            }

            debounceJob?.cancel()
            debounceJob = viewModelScope.launch(dispatchers.io) {
                delay(timeMillis = 800L)
                store.dispatch(TokensAction.SetSearchInput(searchInput = newValue))
            }
        }

        private fun onCleanButtonClick() {
            (uiState.toolbarState as? TokensListToolbarState.SearchInputField)?.let { state ->
                if (state.value.isEmpty()) {
                    uiState = uiState.copySealed(toolbarState = getToolbarState())
                } else {
                    onSearchValueChange(newValue = "")
                }
            }
        }

        private fun onToggleClick(tokenId: String, networkId: String) {
            (uiState as? TokensListStateHolder.ManageAccess)?.let { state ->
                val switchedToken = state.tokens.firstOrNull { it.id == tokenId }
                val contractAddress = switchedToken?.networks
                    ?.firstOrNull { it.networkId == networkId }
                    ?.contractAddress

                val blockchain = Blockchain.fromNetworkId(
                    networkId = if (contractAddress == null) networkId else tokenId,
                )

                if (blockchain != null && contractAddress == null) {
                    onBlockchainToggleClick(blockchain)
                } else if (contractAddress != null) {
                    onTokenToggleClick(tokenId, networkId, switchedToken, contractAddress)
                }

                updateTokensList()
            }
        }

        private fun onBlockchainToggleClick(blockchain: Blockchain) {
            val isRemoveAction = addedBlockchainList?.contains(blockchain)

            if (isRemoveAction == true) {
                val isTokenWithSameBlockchainFound = addedTokenList?.any { it.blockchain == blockchain }
                val isAddedOnMainScreen = originalAddedBlockchainList?.contains(blockchain)

                if (isTokenWithSameBlockchainFound == true) {
                    store.dispatchDialogShow(
                        WalletDialog.TokensAreLinkedDialog(
                            currencyTitle = blockchain.name,
                            currencySymbol = blockchain.currency,
                        ),
                    )
                } else if (isAddedOnMainScreen == true) {
                    store.dispatchDialogShow(
                        WalletDialog.RemoveWalletDialog(
                            currencyTitle = blockchain.name,
                            onOk = {
                                AnalyticsSender.sendWhenBlockchainAdded(blockchain)
                                addedBlockchainList = addedBlockchainList?.minus(blockchain)
                                updateTokensList()
                            },
                        ),
                    )
                } else {
                    AnalyticsSender.sendWhenBlockchainRemoved(blockchain)
                    addedBlockchainList = addedBlockchainList?.minus(blockchain)
                }
            } else {
                AnalyticsSender.sendWhenBlockchainAdded(blockchain)
                addedBlockchainList = addedBlockchainList?.plus(blockchain)
            }
        }

        private fun onTokenToggleClick(
            tokenId: String,
            networkId: String,
            switchedToken: TokenItemState.ManageAccess,
            contractAddress: String,
        ) {
            val currency = store.state.tokensState.currencies.firstOrNull { it.id == tokenId } ?: return
            val contract = currency.contracts.firstOrNull { it.networkId == networkId } ?: return
            val token = TokenWithBlockchain(
                token = Token(
                    id = switchedToken.id,
                    name = switchedToken.name,
                    symbol = currency.symbol,
                    contractAddress = contractAddress,
                    decimals = requireNotNull(contract.decimalCount),
                ),
                blockchain = contract.blockchain,
            )
            val isUnsupportedToken = !store.state.tokensState.canHandleToken(token)
            val isRemoveAction = addedTokenList?.contains(token)
            val isAddedOnMainScreen = originalAddedTokenList?.contains(token)
            if (isRemoveAction == true) {
                if (isAddedOnMainScreen == true) {
                    store.dispatchDialogShow(
                        WalletDialog.RemoveWalletDialog(
                            currencyTitle = token.token.name,
                            onOk = {
                                AnalyticsSender.sendWhenTokenRemoved(token.token)
                                addedTokenList = addedTokenList?.minus(token)
                                updateTokensList()
                            },
                        ),
                    )
                } else {
                    AnalyticsSender.sendWhenTokenRemoved(token.token)
                    addedTokenList = addedTokenList?.minus(token)
                }
            } else {
                if (isUnsupportedToken) {
                    store.dispatchDialogShow(
                        AppDialog.SimpleOkDialogRes(
                            headerId = R.string.common_warning,
                            messageId = R.string.alert_manage_tokens_unsupported_message,
                        ),
                    )
                } else {
                    AnalyticsSender.sendWhenTokenAdded(token.token)
                    addedTokenList = addedTokenList?.plus(token)
                }
            }
        }

        private fun updateTokensList() {
            (uiState as? TokensListStateHolder.ManageAccess)?.let { state ->
                val tokens = store.state.tokensState.currencies.mapNotNull { currency ->
                    state.tokens.firstOrNull { it.id == currency.id }?.copy(
                        networks = currency.contracts.mapNotNull { contract ->
                            state.tokens.firstOrNull { it.id == currency.id }
                                ?.networks?.firstOrNull { it.networkId == contract.networkId }
                                ?.copy(
                                    isAdded = contract.isAdded(),
                                    iconResId = if (contract.isAdded()) {
                                        getActiveIconRes(contract.blockchain.id)
                                    } else {
                                        contract.blockchain.getGreyedOutIconRes()
                                    },
                                )
                        }.toImmutableList(),
                    )
                }.toImmutableList()

                uiState = state.copy(tokens = tokens)
            }
        }

        private fun toManageTokenItemState(currency: Currency): TokenItemState.ManageAccess {
            return TokenItemState.ManageAccess(
                name = currency.fullName,
                iconUrl = currency.iconUrl,
                networks = currency.contracts.map(::toManageNetworkItemState).toImmutableList(),
                id = currency.id,
            )
        }

        private fun toManageNetworkItemState(contract: Contract): NetworkItemState.ManageAccess {
            val isAdded = contract.isAdded()
            return NetworkItemState.ManageAccess(
                name = contract.blockchain.fullNameWithoutTestnet.uppercase(),
                protocolName = if (contract.address == null) {
                    MAIN_NETWORK_LABEL
                } else {
                    contract.blockchain.getNetworkName().uppercase()
                },
                iconResId = if (isAdded) {
                    getActiveIconRes(contract.blockchain.id)
                } else {
                    contract.blockchain.getGreyedOutIconRes()
                },
                isMainNetwork = contract.address == null,
                isAdded = isAdded,
                networkId = contract.networkId,
                contractAddress = contract.address,
                onToggleClick = ::onToggleClick,
                onNetworkClick = { store.dispatchNotification(R.string.contract_address_copied_message) },
            )
        }

        private fun toReadTokenItemState(currency: Currency): TokenItemState.ReadAccess {
            return TokenItemState.ReadAccess(
                name = currency.fullName,
                iconUrl = currency.iconUrl,
                networks = currency.contracts.map(::toReadNetworkItemState).toImmutableList(),
            )
        }

        private fun toReadNetworkItemState(contract: Contract): NetworkItemState.ReadAccess {
            return NetworkItemState.ReadAccess(
                name = contract.blockchain.fullNameWithoutTestnet.uppercase(),
                protocolName = if (contract.address == null) {
                    MAIN_NETWORK_LABEL
                } else {
                    contract.blockchain.getNetworkName().uppercase()
                },
                iconResId = if (contract.isAdded()) {
                    getActiveIconRes(contract.blockchain.id)
                } else {
                    contract.blockchain.getGreyedOutIconRes()
                },
                isMainNetwork = contract.address == null,
            )
        }

        private fun Contract.isAdded(): Boolean {
            return if (address != null) {
                addedTokenList?.any { addedToken ->
                    address == addedToken.token.contractAddress && blockchain == addedToken.blockchain
                }
            } else {
                addedBlockchainList?.contains(blockchain)
            } ?: false
        }
    }

    private companion object {
        const val MAIN_NETWORK_LABEL = "MAIN"
    }
}