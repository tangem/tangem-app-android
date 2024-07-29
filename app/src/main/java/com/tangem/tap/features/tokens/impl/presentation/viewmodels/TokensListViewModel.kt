package com.tangem.tap.features.tokens.impl.presentation.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.navigation.AppScreen
import com.tangem.core.navigation.NavigationAction
import com.tangem.core.ui.extensions.getActiveIconRes
import com.tangem.core.ui.extensions.getGreyedOutIconRes
import com.tangem.domain.card.DerivePublicKeysUseCase
import com.tangem.domain.common.TapWorkarounds.useOldStyleDerivation
import com.tangem.domain.common.extensions.canHandleBlockchain
import com.tangem.domain.common.extensions.canHandleToken
import com.tangem.domain.common.extensions.supportedTokens
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.tokens.AddCryptoCurrenciesUseCase
import com.tangem.domain.tokens.GetCryptoCurrenciesUseCase
import com.tangem.domain.tokens.TokenWithBlockchain
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.tap.common.extensions.dispatchWithMain
import com.tangem.tap.common.extensions.fullNameWithoutTestnet
import com.tangem.tap.common.extensions.getNetworkName
import com.tangem.tap.features.customtoken.impl.presentation.models.SupportBlockchainType
import com.tangem.tap.features.tokens.impl.domain.TokensListInteractor
import com.tangem.tap.features.tokens.impl.domain.models.Token
import com.tangem.tap.features.tokens.impl.domain.models.Token.Network
import com.tangem.tap.features.tokens.impl.presentation.models.SupportTokensState
import com.tangem.tap.features.tokens.impl.presentation.router.TokensListRouter
import com.tangem.tap.features.tokens.impl.presentation.states.NetworkItemState
import com.tangem.tap.features.tokens.impl.presentation.states.TokenItemState
import com.tangem.tap.features.tokens.impl.presentation.states.TokensListStateHolder
import com.tangem.tap.features.tokens.impl.presentation.states.TokensListToolbarState
import com.tangem.tap.store
import com.tangem.utils.coroutines.AppCoroutineDispatcherProvider
import com.tangem.utils.coroutines.Debouncer
import com.tangem.wallet.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import timber.log.Timber
import javax.inject.Inject
import kotlin.properties.Delegates
import com.tangem.blockchain.common.Token as BlockchainToken

/**
 * ViewModel for tokens list screen
 *
 * @property interactor                   feature interactor
 * @property router                       feature router
 * @property dispatchers                  coroutine dispatchers provider
 * @property getSelectedWalletSyncUseCase use case that returns selected wallet
 * @param analyticsEventHandler           analytics event handler
 *
[REDACTED_AUTHOR]
 */
@Suppress("LongParameterList")
@HiltViewModel
internal class TokensListViewModel @Inject constructor(
    private val interactor: TokensListInteractor,
    private val router: TokensListRouter,
    private val dispatchers: AppCoroutineDispatcherProvider,
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
    derivePublicKeysUseCase: DerivePublicKeysUseCase,
    addCryptoCurrenciesUseCase: AddCryptoCurrenciesUseCase,
    analyticsEventHandler: AnalyticsEventHandler,
    getCurrenciesUseCase: GetCryptoCurrenciesUseCase,
) : ViewModel(), DefaultLifecycleObserver {

    private val isManageAccess = store.state.tokensState.isManageAccess
    private val analyticsSender = TokensListAnalyticsSender(analyticsEventHandler)
    private val actionsHandler = ActionsHandler(router = router, debouncer = Debouncer())

    /** Screen state */
    var uiState by mutableStateOf(value = getInitialUiState())
        private set

    private var currentTokensList: List<TokenWithBlockchain> by Delegates.notNull()
    private var currentBlockchainList: List<Blockchain> by Delegates.notNull()

    private var changedTokensList: MutableList<TokenWithBlockchain> = mutableListOf()
    private var changedBlockchainList: MutableList<Blockchain> = mutableListOf()

    private val tokensListMigration = TokensListMigration(
        getSelectedWalletSyncUseCase = getSelectedWalletSyncUseCase,
        getCurrenciesUseCase = getCurrenciesUseCase,
        derivePublicKeysUseCase = derivePublicKeysUseCase,
        addCryptoCurrenciesUseCase = addCryptoCurrenciesUseCase,
    )

    init {
        viewModelScope.launch(dispatchers.main) {
            val (currentCoins, currentTokens) = tokensListMigration.getCurrentCryptoCurrencies()

            currentBlockchainList = currentCoins
            currentTokensList = currentTokens

            changedBlockchainList = currentCoins.toMutableList()
            changedTokensList = currentTokens.toMutableList()

            uiState = uiState.copySealed(tokens = getTokensListBySearchText(text = ""))
        }
    }

    override fun onCreate(owner: LifecycleOwner) {
        if (isManageAccess) analyticsSender.sendWhenScreenOpened()
    }

    private fun getInitialUiState(): TokensListStateHolder {
        return if (isManageAccess) {
            TokensListStateHolder.ManageContent(
                toolbarState = getInitialToolbarState(),
                isLoading = true,
                isDifferentAddressesBlockVisible = isDifferentAddressesBlockVisible(),
                tokens = getInitialTokensList(),
                onTokensLoadStateChanged = actionsHandler::onTokensLoadStateChanged,
                isSavingInProgress = false,
                onSaveButtonClick = actionsHandler::onSaveButtonClick,
            )
        } else {
            TokensListStateHolder.ReadContent(
                toolbarState = getInitialToolbarState(),
                isLoading = true,
                isDifferentAddressesBlockVisible = isDifferentAddressesBlockVisible(),
                tokens = getInitialTokensList(),
                onTokensLoadStateChanged = actionsHandler::onTokensLoadStateChanged,
            )
        }
    }

    private fun getInitialToolbarState(): TokensListToolbarState {
        return if (isManageAccess) {
            TokensListToolbarState.Title.Manage(
                titleResId = R.string.add_tokens_title,
                onBackButtonClick = actionsHandler::onBackButtonClick,
                onSearchButtonClick = actionsHandler::onSearchButtonClick,
                onAddCustomTokenClick = actionsHandler::onAddCustomTokenClick,
            )
        } else {
            TokensListToolbarState.Title.Read(
                titleResId = R.string.common_search_tokens,
                onBackButtonClick = actionsHandler::onBackButtonClick,
                onSearchButtonClick = actionsHandler::onSearchButtonClick,
            )
        }
    }

    private fun isDifferentAddressesBlockVisible(): Boolean {
        return getSelectedWalletSyncUseCase().fold(
            ifLeft = { false },
            ifRight = { it.scanResponse.card.useOldStyleDerivation },
        )
    }

    private fun getInitialTokensList(): Flow<PagingData<TokenItemState>> {
        return flowOf(
            value = PagingData.empty(
                sourceLoadStates = LoadStates(LoadState.Loading, LoadState.Loading, LoadState.Loading),
                mediatorLoadStates = LoadStates(LoadState.Loading, LoadState.Loading, LoadState.Loading),
            ),
        )
    }

    private fun createManageTokenContent(token: Token): TokenItemState.ManageContent {
        val currentTime = System.nanoTime()
        return TokenItemState.ManageContent(
            fullName = getTokenFullName(token),
            iconUrl = token.iconUrl,
            networks = token.networks.map(::createManageNetworkContent).toImmutableList(),
            composedId = token.id + currentTime,
            id = token.id,
            name = token.name,
            symbol = token.symbol,
        )
    }

    private fun createManageNetworkContent(network: Network): NetworkItemState.ManageContent {
        return NetworkItemState.ManageContent(
            name = network.blockchain.fullNameWithoutTestnet.uppercase(),
            protocolName = getNetworkProtocolName(network),
            iconResId = mutableIntStateOf(value = getNetworkIconResId(network.address, network.blockchain)),
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
        val currentTime = System.nanoTime()
        return TokenItemState.ReadContent(
            id = token.id,
            fullName = getTokenFullName(token),
            iconUrl = token.iconUrl,
            networks = token.networks.map(::createReadNetworkContent).toImmutableList(),
            composedId = token.id + currentTime,
        )
    }

    private fun createReadNetworkContent(network: Network): NetworkItemState.ReadContent {
        return NetworkItemState.ReadContent(
            name = network.blockchain.fullNameWithoutTestnet.uppercase(),
            protocolName = getNetworkProtocolName(network),
            iconResId = mutableIntStateOf(value = getNetworkIconResId(network.address, network.blockchain)),
            isMainNetwork = isMainNetwork(network),
        )
    }

    private fun getTokenFullName(token: Token) = "${token.name} (${token.symbol})"

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
            getGreyedOutIconRes(blockchain.id)
        }
    }

    private fun isMainNetwork(network: Network) = network.address == null

    private fun isAdded(address: String?, blockchain: Blockchain?): Boolean {
        return if (address != null) {
            changedTokensList.any { addedToken ->
                address == addedToken.token.contractAddress && blockchain == addedToken.blockchain
            }
        } else {
            changedBlockchainList.contains(blockchain)
        }
    }

    private fun getTokensListBySearchText(text: String): Flow<PagingData<TokenItemState>> {
        return interactor.getTokensList(searchText = text, needFilterExcluded = !isManageAccess)
            .mapToTokenItemState()
            .cachedIn(viewModelScope)
    }

    private fun Flow<PagingData<Token>>.mapToTokenItemState(): Flow<PagingData<TokenItemState>> {
        return map { pagingData ->
            pagingData.map { token ->
                if (isManageAccess) createManageTokenContent(token) else createReadTokenContent(token)
            }
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

        fun onNetworkClick() {
            router.showAddressCopiedNotification()
        }

        fun onTokensLoadStateChanged(state: LoadState) {
            uiState = uiState.copySealed(isLoading = state is LoadState.Loading)
        }

        fun onSaveButtonClick() {
            val state = uiState as? TokensListStateHolder.ManageContent ?: return

            analyticsSender.sendWhenSaveButtonClicked()

            viewModelScope.launch(dispatchers.main) {
                uiState = state.copy(isSavingInProgress = true)

                tokensListMigration.onSaveButtonClick(
                    changedTokensList = changedTokensList,
                    changedBlockchainList = changedBlockchainList,
                )

                uiState = state.copy(isSavingInProgress = false)
                store.dispatchWithMain(NavigationAction.PopBackTo(screen = AppScreen.Wallet))
            }
        }

        private fun onSearchValueChange(newValue: String) {
            val state = requireNotNull(uiState.toolbarState as? TokensListToolbarState.InputField)
            uiState = uiState.copySealed(toolbarState = state.copy(value = newValue))

            debouncer.debounce(waitMs = 800L, coroutineScope = viewModelScope + dispatchers.io) {
                uiState = uiState.copySealed(tokens = getSearchedTokensList(newValue))
            }
        }

        private fun getSearchedTokensList(searchText: String): Flow<PagingData<TokenItemState>> {
            analyticsSender.sendWhenTokenSearched()
            return getTokensListBySearchText(text = searchText)
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
            val isRemoveAction = changedBlockchainList.contains(blockchain)

            if (isRemoveAction) {
                val isTokenWithSameBlockchainFound = changedTokensList.any { it.blockchain == blockchain }
                val isAddedOnMainScreen = currentBlockchainList.contains(blockchain)

                if (isTokenWithSameBlockchainFound) {
                    router.openUnableHideMainTokenAlert(
                        tokenName = blockchain.name,
                        tokenSymbol = blockchain.currency,
                        networkName = blockchain.fullName,
                    )
                } else if (isAddedOnMainScreen) {
                    router.openRemoveWalletAlert(
                        tokenName = blockchain.name,
                        onOkClick = {
                            analyticsSender.sendWhenBlockchainAdded(blockchain)
                            changedBlockchainList.remove(blockchain)
                            toggledNetwork.changeToggleState()
                        },
                    )
                } else {
                    analyticsSender.sendWhenBlockchainRemoved(blockchain)
                    changedBlockchainList.remove(blockchain)
                    toggledNetwork.changeToggleState()
                }
            } else {
                when (getSupportBlockchainType(blockchain)) {
                    SupportBlockchainType.SUPPORTED -> {
                        analyticsSender.sendWhenBlockchainAdded(blockchain)
                        changedBlockchainList.add(blockchain)
                        toggledNetwork.changeToggleState()
                    }
                    SupportBlockchainType.UNSUPPORTED -> {
                        router.openUnsupportedNetworkAlert(blockchain)
                    }
                    SupportBlockchainType.UNABLE_TO_DETERMINE -> {
                        router.showGenericErrorAlertAndPopBack()
                    }
                }
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

            val isRemoveAction = changedTokensList.contains(token)

            if (isRemoveAction) {
                val isAddedOnMainScreen = currentTokensList.contains(token)

                if (isAddedOnMainScreen) {
                    router.openRemoveWalletAlert(
                        tokenName = token.token.name,
                        onOkClick = {
                            analyticsSender.sendWhenTokenRemoved(token.token)
                            changedTokensList.remove(token)
                            toggledNetwork.changeToggleState()
                        },
                    )
                } else {
                    analyticsSender.sendWhenTokenRemoved(token.token)
                    changedTokensList.remove(token)
                    toggledNetwork.changeToggleState()
                }
            } else {
                when (isUnsupportedToken(token.blockchain)) {
                    SupportTokensState.NetworkTokensUnsupported -> {
                        router.openNetworkTokensNotSupportAlert(token.blockchain.fullName)
                    }
                    SupportTokensState.SupportedToken -> {
                        analyticsSender.sendWhenTokenAdded(token.token)
                        changedTokensList.add(token)
                        toggledNetwork.changeToggleState()
                    }
                    SupportTokensState.UnsupportedCurve -> {
                        router.openUnsupportedNetworkAlert(token.blockchain)
                    }
                    null -> {
                        Timber.e("Something went wrong in isUnsupportedToken (no scanResponse found)")
                    }
                }
            }
        }
    }

    private fun isUnsupportedToken(blockchain: Blockchain): SupportTokensState? {
        return getSelectedWalletSyncUseCase().fold(
            ifLeft = { null },
            ifRight = {
                val cardTypesResolver = it.scanResponse.cardTypesResolver
                val supportedTokens = it.scanResponse.card.supportedTokens(cardTypesResolver)

                // refactor this later by moving all this logic in card config
                if (blockchain == Blockchain.Solana && !supportedTokens.contains(Blockchain.Solana)) {
                    return SupportTokensState.NetworkTokensUnsupported
                }
                val canHandleToken = it.scanResponse.card.canHandleToken(
                    supportedTokens = supportedTokens,
                    blockchain = blockchain,
                    cardTypesResolver = cardTypesResolver,
                )
                if (!canHandleToken) {
                    return SupportTokensState.UnsupportedCurve
                }
                return SupportTokensState.SupportedToken
            },
        )
    }

    private fun getSupportBlockchainType(blockchain: Blockchain): SupportBlockchainType {
        return getSelectedWalletSyncUseCase().fold(
            ifLeft = { SupportBlockchainType.UNABLE_TO_DETERMINE },
            ifRight = {
                val canHandleBlockchain = it.scanResponse.card.canHandleBlockchain(
                    blockchain = blockchain,
                    cardTypesResolver = it.scanResponse.cardTypesResolver,
                )
                if (canHandleBlockchain) {
                    SupportBlockchainType.SUPPORTED
                } else {
                    SupportBlockchainType.UNSUPPORTED
                }
            },
        )
    }

    private companion object {
        const val MAIN_NETWORK_LABEL = "MAIN"
    }
}