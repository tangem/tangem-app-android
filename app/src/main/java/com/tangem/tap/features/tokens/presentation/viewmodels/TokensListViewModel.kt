package com.tangem.tap.features.tokens.presentation.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.map
import com.tangem.blockchain.common.Blockchain
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.ui.extensions.getActiveIconRes
import com.tangem.domain.common.extensions.canHandleToken
import com.tangem.domain.common.extensions.fromNetworkId
import com.tangem.tap.common.extensions.dispatchDialogShow
import com.tangem.tap.common.extensions.dispatchNotification
import com.tangem.tap.common.extensions.fullNameWithoutTestnet
import com.tangem.tap.common.extensions.getGreyedOutIconRes
import com.tangem.tap.common.extensions.getNetworkName
import com.tangem.tap.common.redux.AppDialog
import com.tangem.tap.features.tokens.domain.TokensListInteractor
import com.tangem.tap.features.tokens.domain.models.Token
import com.tangem.tap.features.tokens.domain.models.Token.Network
import com.tangem.tap.features.tokens.presentation.models.TokensListArgs
import com.tangem.tap.features.tokens.presentation.router.TokensListRouter
import com.tangem.tap.features.tokens.presentation.states.NetworkItemState
import com.tangem.tap.features.tokens.presentation.states.TokenItemState
import com.tangem.tap.features.tokens.presentation.states.TokensListStateHolder
import com.tangem.tap.features.tokens.presentation.states.TokensListToolbarState
import com.tangem.tap.features.tokens.redux.TokenWithBlockchain
import com.tangem.tap.features.tokens.redux.TokensAction
import com.tangem.tap.features.wallet.redux.models.WalletDialog
import com.tangem.tap.proxy.AppStateHolder
import com.tangem.tap.store
import com.tangem.utils.coroutines.AppCoroutineDispatcherProvider
import com.tangem.utils.coroutines.Debouncer
import com.tangem.wallet.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.plus
import javax.inject.Inject
import com.tangem.blockchain.common.Token as BlockchainToken

/**
 * ViewModel for tokens list screen
 *
 * @property interactor         feature interactor
 * @property router             feature router
 * @property dispatchers        coroutine dispatchers provider
 * @property reduxStateHolder   redux state holder
 * @param analyticsEventHandler analytics event handler
 *
* [REDACTED_AUTHOR]
 */
@HiltViewModel
internal class TokensListViewModel @Inject constructor(
    private val interactor: TokensListInteractor,
    private val router: TokensListRouter,
    private val dispatchers: AppCoroutineDispatcherProvider,
    private val reduxStateHolder: AppStateHolder,
    analyticsEventHandler: AnalyticsEventHandler,
) : ViewModel() {

    private val args = TokensListArgs()
    private val analyticsSender = TokensListAnalyticsSender(analyticsEventHandler)
    private val actionsHandler = ActionsHandler(router = router, debouncer = Debouncer())

    /** Screen state */
    var uiState by mutableStateOf(value = getInitialUiState())
        private set

    private val addedTokenList: MutableList<TokenWithBlockchain> = args.mainScreenTokenList.toMutableList()
    private val addedBlockchainList: MutableList<Blockchain> = args.mainScreenBlockchainList.toMutableList()

    private fun getInitialUiState(): TokensListStateHolder {
        return TokensListStateHolder.Loading(
            toolbarState = getInitialToolbarState(),
            tokens = getInitialTokensList(),
            onTokensLoadStateChanged = actionsHandler::onTokensLoadStateChanged,
        )
    }

    private fun getInitialToolbarState(): TokensListToolbarState {
        return if (args.isManageAccess) {
            TokensListToolbarState.Title.Manage(
                titleResId = R.string.main_manage_tokens,
                onBackButtonClick = actionsHandler::onBackButtonClick,
                onSearchButtonClick = actionsHandler::onSearchButtonClick,
                onAddCustomTokenClick = actionsHandler::onAddCustomTokenClick,
            )
        } else {
            TokensListToolbarState.Title.Read(
                titleResId = R.string.search_tokens_title,
                onBackButtonClick = actionsHandler::onBackButtonClick,
                onSearchButtonClick = actionsHandler::onSearchButtonClick,
            )
        }
    }

    private fun getInitialTokensList(searchText: String = ""): Flow<PagingData<TokenItemState>> {
        return interactor.getTokensList(searchText = searchText).map {
            it.map { token ->
                if (args.isManageAccess) createManageTokenContent(token) else createReadTokenContent(token)
            }
        }
    }

    private fun createManageTokenContent(token: Token): TokenItemState.ManageContent {
        return TokenItemState.ManageContent(
            name = getTokenName(token),
            iconUrl = token.iconUrl,
            networks = token.networks.map(::createManageNetworkContent).toImmutableList(),
            id = token.id,
            symbol = token.symbol,
        )
    }

    private fun createManageNetworkContent(network: Network): NetworkItemState.ManageContent {
        return NetworkItemState.ManageContent(
            name = network.blockchain.fullNameWithoutTestnet.uppercase(),
            protocolName = getNetworkProtocolName(network),
            iconResId = mutableStateOf(
                getNetworkIconResId(network.address, network.blockchain),
            ),
            isMainNetwork = isMainNetwork(network),
            isAdded = mutableStateOf(
                isAdded(address = network.address, blockchain = network.blockchain),
            ),
            id = network.id,
            address = network.address,
            decimalCount = network.decimalCount,
            blockchain = network.blockchain,
            onToggleClick = actionsHandler::onToggleClick,
            onNetworkClick = actionsHandler::onNetworkClick,
        )
    }

    private fun createReadTokenContent(token: Token): TokenItemState.ReadContent {
        return TokenItemState.ReadContent(
            name = getTokenName(token),
            iconUrl = token.iconUrl,
            networks = token.networks.map(::createReadNetworkContent).toImmutableList(),
        )
    }

    private fun createReadNetworkContent(network: Network): NetworkItemState.ReadContent {
        return NetworkItemState.ReadContent(
            name = network.blockchain.fullNameWithoutTestnet.uppercase(),
            protocolName = getNetworkProtocolName(network),
            iconResId = mutableStateOf(getNetworkIconResId(network.address, network.blockchain)),
            isMainNetwork = isMainNetwork(network),
        )
    }

    private fun getTokenName(token: Token) = "${token.name} (${token.symbol})"

    private fun getNetworkProtocolName(network: Network): String {
        return if (network.address == null) {
            MAIN_NETWORK_LABEL
        } else {
            network.blockchain.getNetworkName().uppercase()
        }
    }

    private fun getNetworkIconResId(address: String?, blockchain: Blockchain): Int {
        return if (isAdded(address = address, blockchain = blockchain)) {
            getActiveIconRes(blockchain.id)
        } else {
            blockchain.getGreyedOutIconRes()
        }
    }

    private fun isMainNetwork(network: Network) = network.address == null

    private fun isAdded(address: String?, blockchain: Blockchain?): Boolean {
        return if (address != null) {
            addedTokenList.any { addedToken ->
                address == addedToken.token.contractAddress && blockchain == addedToken.blockchain
            }
        } else {
            addedBlockchainList.contains(blockchain)
        }
    }

    private inner class ActionsHandler(
        private val router: TokensListRouter,
        private val debouncer: Debouncer,
    ) {

        fun onBackButtonClick() {
            router.popBackStack()
        }

        fun onSearchButtonClick() {
            uiState = uiState.copySealed(
                toolbarState = TokensListToolbarState.InputField(
                    onBackButtonClick = this::onBackButtonClick,
                    value = "",
                    onValueChange = this::onSearchValueChange,
                    onCleanButtonClick = this::onCleanButtonClick,
                ),
            )
        }

        fun onAddCustomTokenClick() {
            analyticsSender.sendWhenAddCustomTokenClicked()
            router.openAddCustomTokenScreen()
        }

        fun onToggleClick(toggledToken: TokenItemState.ManageContent, toggledNetwork: NetworkItemState.ManageContent) {
            val blockchain = Blockchain.fromNetworkId(
                networkId = if (toggledNetwork.address == null) toggledNetwork.id else toggledToken.id,
            )
            if (toggledNetwork.address == null && blockchain != null) {
                updateBlockchainItem(toggledNetwork, blockchain)
            } else {
                updateTokenItem(toggledToken, toggledNetwork)
            }
        }
// [REDACTED_TODO_COMMENT]
        fun onNetworkClick() {
            store.dispatchNotification(R.string.contract_address_copied_message)
        }

        fun onTokensLoadStateChanged(state: LoadState) {
            uiState = when (state) {
                is LoadState.NotLoading -> {
                    analyticsSender.sendWhenTokenSearched()

                    if (args.isManageAccess) {
                        TokensListStateHolder.ManageContent(
                            toolbarState = uiState.toolbarState,
                            tokens = uiState.tokens,
                            onTokensLoadStateChanged = uiState.onTokensLoadStateChanged,
                            onSaveButtonClick = this::onSaveButtonClick,
                        )
                    } else {
                        TokensListStateHolder.ReadContent(
                            toolbarState = uiState.toolbarState,
                            tokens = uiState.tokens,
                            onTokensLoadStateChanged = uiState.onTokensLoadStateChanged,
                        )
                    }
                }
                else -> {
                    TokensListStateHolder.Loading(
                        toolbarState = uiState.toolbarState,
                        tokens = uiState.tokens,
                        onTokensLoadStateChanged = uiState.onTokensLoadStateChanged,
                    )
                }
            }
        }

        private fun onSearchValueChange(newValue: String) {
            val state = requireNotNull(uiState.toolbarState as? TokensListToolbarState.InputField)
            uiState = uiState.copySealed(toolbarState = state.copy(value = newValue))

            debouncer.debounce(waitMs = 800L, coroutineScope = viewModelScope + dispatchers.io) {
                uiState = uiState.copySealed(tokens = getInitialTokensList(newValue))
            }
        }

        private fun onCleanButtonClick() {
            val state = requireNotNull(uiState.toolbarState as? TokensListToolbarState.InputField)

            if (state.value.isEmpty()) {
                uiState = uiState.copySealed(toolbarState = getInitialToolbarState())
            } else {
                onSearchValueChange(newValue = "")
            }
        }

        private fun updateBlockchainItem(toggledNetwork: NetworkItemState.ManageContent, blockchain: Blockchain) {
            val isRemoveAction = addedBlockchainList.contains(blockchain)

            if (isRemoveAction) {
                val isTokenWithSameBlockchainFound = addedTokenList.any { it.blockchain == blockchain }
                val isAddedOnMainScreen = args.mainScreenBlockchainList.contains(blockchain)

                if (isTokenWithSameBlockchainFound) {
                    store.dispatchDialogShow(
                        WalletDialog.TokensAreLinkedDialog(
                            currencyTitle = blockchain.name,
                            currencySymbol = blockchain.currency,
                        ),
                    )
                } else if (isAddedOnMainScreen) {
                    store.dispatchDialogShow(
                        WalletDialog.RemoveWalletDialog(
                            currencyTitle = blockchain.name,
                            onOk = {
                                analyticsSender.sendWhenBlockchainAdded(blockchain)
                                addedBlockchainList.remove(blockchain)
                                toggledNetwork.changeToggleState()
                            },
                        ),
                    )
                } else {
                    analyticsSender.sendWhenBlockchainRemoved(blockchain)
                    addedBlockchainList.remove(blockchain)
                    toggledNetwork.changeToggleState()
                }
            } else {
                analyticsSender.sendWhenBlockchainAdded(blockchain)
                addedBlockchainList.add(blockchain)
                toggledNetwork.changeToggleState()
            }
        }

        private fun updateTokenItem(
            toggledToken: TokenItemState.ManageContent,
            toggledNetwork: NetworkItemState.ManageContent,
        ) {
            val token = TokenWithBlockchain(
                token = BlockchainToken(
                    id = toggledToken.id,
                    name = toggledToken.name,
                    symbol = toggledToken.symbol,
                    contractAddress = requireNotNull(toggledNetwork.address),
                    decimals = requireNotNull(toggledNetwork.decimalCount),
                ),
                blockchain = toggledNetwork.blockchain,
            )

            val isRemoveAction = addedTokenList.contains(token)

            if (isRemoveAction) {
                val isAddedOnMainScreen = args.mainScreenTokenList.contains(token)

                if (isAddedOnMainScreen) {
                    store.dispatchDialogShow(
                        WalletDialog.RemoveWalletDialog(
                            currencyTitle = token.token.name,
                            onOk = {
                                analyticsSender.sendWhenTokenRemoved(token.token)
                                addedTokenList.remove(token)
                                toggledNetwork.changeToggleState()
                            },
                        ),
                    )
                } else {
                    analyticsSender.sendWhenTokenRemoved(token.token)
                    addedTokenList.remove(token)
                    toggledNetwork.changeToggleState()
                }
            } else {
                val isUnsupportedToken =
                    !(reduxStateHolder.scanResponse?.card?.canHandleToken(token.blockchain) ?: false)

                if (isUnsupportedToken) {
                    store.dispatchDialogShow(
                        AppDialog.SimpleOkDialogRes(
                            headerId = R.string.common_warning,
                            messageId = R.string.alert_manage_tokens_unsupported_message,
                        ),
                    )
                } else {
                    analyticsSender.sendWhenTokenAdded(token.token)
                    addedTokenList.add(token)
                    toggledNetwork.changeToggleState()
                }
            }
        }

        private fun onSaveButtonClick() {
            analyticsSender.sendWhenSaveButtonClicked()
            store.dispatch(TokensAction.SaveChanges(addedTokenList, addedBlockchainList))
        }
    }

    private companion object {
        const val MAIN_NETWORK_LABEL = "MAIN"
    }
}
