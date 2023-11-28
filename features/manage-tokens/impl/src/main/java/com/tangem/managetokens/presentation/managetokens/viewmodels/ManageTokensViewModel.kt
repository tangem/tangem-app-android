package com.tangem.managetokens.presentation.managetokens.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.map
import arrow.core.getOrElse
import com.tangem.blockchain.common.Blockchain
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.card.DerivePublicKeysUseCase
import com.tangem.domain.common.util.derivationStyleProvider
import com.tangem.domain.tokens.*
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.domain.wallets.usecase.SelectWalletUseCase
import com.tangem.domain.wallets.usecase.UpdateWalletUseCase
import com.tangem.managetokens.presentation.common.state.AlertState
import com.tangem.managetokens.presentation.common.state.Event
import com.tangem.managetokens.presentation.common.state.NetworkItemState
import com.tangem.managetokens.presentation.managetokens.state.DerivationNotificationState
import com.tangem.managetokens.presentation.managetokens.state.ManageTokensState
import com.tangem.managetokens.presentation.managetokens.state.TokenButtonType
import com.tangem.managetokens.presentation.managetokens.state.TokenItemState
import com.tangem.managetokens.presentation.managetokens.state.factory.*
import com.tangem.operations.derivation.ExtendedPublicKeysMap
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.Debouncer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.collections.set
import kotlin.properties.Delegates

@Suppress("LargeClass", "LongParameterList", "TooManyFunctions")
@HiltViewModel
internal class ManageTokensViewModel @Inject constructor(
    private val dispatchers: CoroutineDispatcherProvider,
    private val getGlobalTokenListUseCase: GetGlobalTokenListUseCase,
    private val getWalletsUseCase: GetWalletsUseCase,
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
    private val removeCurrencyUseCase: RemoveCurrencyUseCase,
    private val addCryptoCurrenciesUseCase: AddCryptoCurrenciesUseCase,
    private val selectWalletUseCase: SelectWalletUseCase,
    private val getCurrenciesUseCase: GetCryptoCurrenciesUseCase,
    private val derivePublicKeysUseCase: DerivePublicKeysUseCase,
    private val updateWalletUseCase: UpdateWalletUseCase,
    private val fetchTokenListUseCase: FetchTokenListUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
) : ViewModel(), ManageTokensClickIntents, DefaultLifecycleObserver {

    private val debouncer = Debouncer()

    private val stateFactory = ManageTokensStateFactory(
        currentStateProvider = Provider { uiState },
        clickIntents = this,
    )

    var uiState: ManageTokensState by mutableStateOf(stateFactory.getInitialState(flowOf(PagingData.from(emptyList()))))
        private set

    private var allAddedCurrencies: MutableList<CryptoCurrency> = mutableListOf()

    private var wallets: List<UserWallet> by Delegates.notNull()

    private var addedCurrenciesByWallet: MutableMap<UserWallet, MutableList<CryptoCurrency>> = mutableMapOf()

    private var selectedWallet: UserWallet? = null

    private var neededDerivations: Map<UserWallet, List<DerivationData>> = emptyMap()

    private val selectedAppCurrencyFlow: StateFlow<AppCurrency> = createSelectedAppCurrencyFlow()

    private val quotesStateConverter = QuotesToQuotesStateConverter()

    private val networkToNetworkItemStateConverter = NetworkToNetworkItemStateConverter(
        addedCurrenciesByWalletProvider = Provider { addedCurrenciesByWallet },
        selectedWalletProvider = Provider { selectedWallet },
        onNetworkToggleClick = this::onNetworkToggleClick,
    )

    private val networksToChooseNetworkStateConverter = NetworksToChooseNetworkStateConverter(
        networkToNetworkItemStateConverter = networkToNetworkItemStateConverter,
        onNonNativeNetworkHintClick = this::onNonNativeNetworkHintClick,
        onCloseChooseNetworkScreen = this::onCloseChooseNetworkScreen,
    )

    private val tokenConverter = TokenConverter(
        quotesStateConverter = quotesStateConverter,
        networksToChooseNetworkStateConverter = networksToChooseNetworkStateConverter,
        allAddedCurrencies = Provider { allAddedCurrencies },
        selectedAppCurrency = Provider { selectedAppCurrencyFlow.value },
        onTokenItemButtonClick = this::onTokenItemButtonClick,
    )

    init {
        viewModelScope.launch(dispatchers.io) {
            getWalletsUseCase()
                .distinctUntilChanged()
                .collectLatest { userWallets ->
                    wallets = userWallets.filter { it.isMultiCurrency && !it.isLocked }
                    wallets.map { wallet ->
                        val currencies = getCurrenciesUseCase(wallet.walletId).fold(
                            ifLeft = { emptyList() },
                            ifRight = { it },
                        )
                        allAddedCurrencies += currencies
                        addedCurrenciesByWallet[wallet] = currencies.toMutableList()
                    }
                    withContext(dispatchers.main) {
                        uiState = uiState.copy(tokens = getInitialTokensList())
                    }
                    selectedWallet = getSelectedWalletSyncUseCase().fold(
                        ifLeft = { null },
                        ifRight = { if (!it.isMultiCurrency || it.isLocked) null else it },
                    )
                    if (selectedWallet == null && wallets.isNotEmpty()) {
                        selectWalletUseCase(wallets.first().walletId)
                        selectedWallet = wallets.first()
                    }
                    withContext(dispatchers.main) {
                        neededDerivations = MissingDerivationsIdentifier
                            .getMissingDerivationsForWallets(wallets, addedCurrenciesByWallet)
                        updateDerivationNotificationState()
                        uiState = stateFactory.updateChooseWalletState(wallets, userWallets, selectedWallet)
                    }
                }
        }
    }

    private fun getInitialTokensList(searchText: String = ""): Flow<PagingData<TokenItemState>> {
        return getGlobalTokenListUseCase(searchText = searchText).map {
            it.map { token -> tokenConverter.convert(token) }
        }
    }

    private fun isAdded(address: String?, blockchain: Blockchain, currencies: Collection<CryptoCurrency>): Boolean {
        return if (address != null) {
            currencies.any {
                !it.isCustom && it is CryptoCurrency.Token && it.contractAddress == address
            }
        } else {
            currencies.any {
                !it.isCustom && it is CryptoCurrency.Coin && it.name == blockchain.fullName
            }
        }
    }

    private fun updateDerivationNotificationState() {
        val derivationNotificationState = if (neededDerivations.isEmpty()) {
            null
        } else {
            DerivationNotificationState(
                totalNeeded = neededDerivations.values.map { it.map { it.derivationsNeeded() } }.sumOf { it.sum() },
                totalWallets = wallets.size,
                walletsToDerive = neededDerivations.keys.size,
                onGenerateClick = this::onGenerateDerivationClick,
            )
        }
        uiState = uiState.copy(derivationNotification = derivationNotificationState)
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

    override fun onAddCustomTokensButtonClick() {
        TODO("Not yet implemented") // TODO: add when custom tokens are implemented
    }

    override fun onSearchQueryChange(query: String) {
        uiState = uiState.copy(searchBarState = uiState.searchBarState.copy(query = query))

        debouncer.debounce(waitMs = 500L, coroutineScope = viewModelScope + dispatchers.io) {
            val state = stateFactory.showAddCustomTokensButton(query.isNotBlank())
            uiState = state.copy(tokens = getInitialTokensList(query))
        }
    }

    override fun onSearchActiveChange(active: Boolean) {
        uiState = uiState.copy(searchBarState = uiState.searchBarState.copy(active = active))
    }

    override fun onTokenItemButtonClick(token: TokenItemState.Loaded) {
        when (token.availableAction.value) {
            TokenButtonType.ADD, TokenButtonType.EDIT -> {
                uiState = uiState.copy(selectedToken = token)

                val addedCurrenciesOnWallet = addedCurrenciesByWallet[selectedWallet] ?: listOf()

                token.chooseNetworkState.nativeNetworks.forEach {
                    if (it is NetworkItemState.Toggleable) {
                        val isAdded = isAdded(it.address, it.blockchain, addedCurrenciesOnWallet)
                        if (isAdded != it.isAdded.value) (it as? NetworkItemState.Toggleable)?.changeToggleState()
                    }
                }
                token.chooseNetworkState.nonNativeNetworks.forEach {
                    if (it is NetworkItemState.Toggleable) {
                        val isAdded = isAdded(it.address, it.blockchain, addedCurrenciesOnWallet)
                        if (isAdded != it.isAdded.value) (it as? NetworkItemState.Toggleable)?.changeToggleState()
                    }
                }
            }
            TokenButtonType.NOT_AVAILABLE -> {
                uiState = stateFactory.getStateAndTriggerEvent(
                    state = uiState,
                    event = Event.ShowAlert(
                        AlertState.TokenUnavailable(
                            onUpvoteClick = {}, // TODO later, when endpoint is available
                        ),
                    ),
                    setUiState = { uiState = it },
                )
            }
        }
    }

    override fun onGenerateDerivationClick() {
        if (neededDerivations.isNotEmpty()) {
            viewModelScope.launch(dispatchers.io) {
                val wallet = neededDerivations.keys.firstOrNull()
                val derivations = neededDerivations[wallet]?.toMapOfDerivations()
                if (wallet == null || derivations.isNullOrEmpty()) return@launch

                derivePublicKeysUseCase(cardId = null, derivations = derivations)
                    .onRight {
                        val newDerivedKeys = it.entries
                        val oldDerivedKeys = wallet.scanResponse.derivedKeys

                        val walletKeys = (newDerivedKeys.keys + oldDerivedKeys.keys).toSet()

                        val updatedDerivedKeys = walletKeys.associateWith { walletKey ->
                            val oldDerivations = ExtendedPublicKeysMap(oldDerivedKeys[walletKey] ?: emptyMap())
                            val newDerivations = newDerivedKeys[walletKey] ?: ExtendedPublicKeysMap(emptyMap())
                            ExtendedPublicKeysMap(oldDerivations + newDerivations)
                        }
                        val updatedScanResponse = wallet.scanResponse.copy(derivedKeys = updatedDerivedKeys)

                        updateWalletUseCase(
                            userWalletId = wallet.walletId,
                            update = { userWallet -> userWallet.copy(scanResponse = updatedScanResponse) },
                        ).onRight { userWallet ->
                            fetchTokenListUseCase(userWalletId = userWallet.walletId)
                        }

                        updateDerivationNotificationState()
                    }
            }
        }
    }

    override fun onBackClick() {
        TODO("Not yet implemented") // TODO: implement if needed when custom tokens and bottom sheet is complete
    }

    override fun onCloseChooseNetworkScreen() {
        uiState = uiState.copy(selectedToken = null)
    }

    override fun onNetworkToggleClick(token: TokenItemState.Loaded, network: NetworkItemState.Toggleable) {
        val selectedWallet = selectedWallet ?: return
        if (!selectedWallet.isMultiCurrency || selectedWallet.isLocked) return

        if (network.isAdded.value) {
            toggleToken(token, network, selectedWallet)
        } else {
            TokenCompatibility(selectedWallet)
                .check(network.blockchain, network.address == null)
                .fold(
                    ifLeft = { error ->
                        val alert = when (error) {
                            TokenCompatibility.AddTokenError.SolanaTokensUnsupported ->
                                AlertState.TokensUnsupported(network.blockchain.fullName)
                            TokenCompatibility.AddTokenError.UnsupportedBlockchain ->
                                AlertState.TokensUnsupportedBlockchainByCard(network.blockchain.fullName)
                            TokenCompatibility.AddTokenError.UnsupportedCurve ->
                                AlertState.TokensUnsupportedCurve
                        }
                        uiState = stateFactory.getStateAndTriggerEvent(
                            state = uiState,
                            event = Event.ShowAlert(alert),
                            setUiState = { uiState = it },
                        )
                    },
                    ifRight = {
                        toggleToken(token, network, selectedWallet)
                    },
                )
        }
    }

    private fun toggleToken(
        token: TokenItemState.Loaded,
        network: NetworkItemState.Toggleable,
        selectedWallet: UserWallet,
    ) {
        val cryptoCurrency = requireNotNull(
            TokenToCryptoCurrencyConverter(
                network = network,
                derivationStyleProvider = selectedWallet.scanResponse.derivationStyleProvider,
            ).convert(token),
        ) {
            "It is only null if Blockchain is Unknown, which mustn't happen here"
        }

        if (!network.isAdded.value) {
            addedCurrenciesByWallet[selectedWallet]?.add(cryptoCurrency)
            allAddedCurrencies.add(cryptoCurrency)
            viewModelScope.launch(dispatchers.io) {
                addCryptoCurrenciesUseCase(selectedWallet.walletId, cryptoCurrency)
            }
        } else {
            addedCurrenciesByWallet[selectedWallet]?.remove(cryptoCurrency)
            allAddedCurrencies.remove(cryptoCurrency)
            viewModelScope.launch(dispatchers.io) {
                removeCurrencyUseCase(selectedWallet.walletId, cryptoCurrency)
            }
        }
        network.changeToggleState()
        val isAnyAdded = token.chooseNetworkState.nativeNetworks.any {
            it is NetworkItemState.Toggleable && isAdded(
                address = it.address,
                blockchain = it.blockchain,
                currencies = allAddedCurrencies,
            )
        } || token.chooseNetworkState.nonNativeNetworks.any {
            it is NetworkItemState.Toggleable && isAdded(
                address = it.address,
                blockchain = it.blockchain,
                currencies = allAddedCurrencies,
            )
        }
        val buttonType = if (isAnyAdded) TokenButtonType.EDIT else TokenButtonType.ADD
        token.availableAction.value = buttonType
        neededDerivations = MissingDerivationsIdentifier
            .getMissingDerivationsForWallets(wallets, addedCurrenciesByWallet)
        updateDerivationNotificationState()
    }

    override fun onNonNativeNetworkHintClick() {
        uiState = stateFactory.getStateAndTriggerEvent(
            state = uiState,
            event = Event.ShowAlert(AlertState.NonNative),
            setUiState = { uiState = it },
        )
    }

    override fun onSelectWalletsClick() {
        uiState = uiState.copy(
            showChooseWalletScreen = true,
        )
    }

    override fun onChooseWalletClick() {
        uiState = uiState.copy(
            showChooseWalletScreen = true,
        )
    }

    override fun onCloseChoosingWalletClick() {
        uiState = uiState.copy(
            showChooseWalletScreen = false,
        )
    }

    override fun onWalletSelected(walletId: String) {
        viewModelScope.launch(dispatchers.io) {
            selectWalletUseCase(UserWalletId(walletId))
        }
        selectedWallet = wallets.find { it.walletId.stringValue == walletId }
        uiState.selectedToken?.let { onTokenItemButtonClick(it) }
        uiState = stateFactory.updateSelectedWallet(selectedWalletId = selectedWallet?.walletId?.stringValue)
    }
}