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
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
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
import com.tangem.managetokens.presentation.common.analytics.ManageTokens
import com.tangem.managetokens.presentation.common.state.AlertState
import com.tangem.managetokens.presentation.common.state.Event
import com.tangem.managetokens.presentation.common.state.NetworkItemState
import com.tangem.managetokens.presentation.managetokens.state.ManageTokensState
import com.tangem.managetokens.presentation.managetokens.state.TokenButtonType
import com.tangem.managetokens.presentation.managetokens.state.TokenItemState
import com.tangem.managetokens.presentation.managetokens.state.factory.*
import com.tangem.managetokens.presentation.router.InnerManageTokensRouter
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.Debouncer
import com.tangem.utils.coroutines.Debouncer.Companion.DEFAULT_WAIT_TIME_MS
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.collections.set
import kotlin.properties.Delegates

@Suppress("LongParameterList", "LargeClass")
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
    private val getMissedAddressesCryptoCurrenciesUseCase: GetMissedAddressesCryptoCurrenciesUseCase,
    private val fetchTokenListUseCase: FetchTokenListUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val checkCurrencyCompatibilityUseCase: CheckCurrencyCompatibilityUseCase,
    private val isCryptoCurrencyCoinCouldHide: IsCryptoCurrencyCoinCouldHideUseCase,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : ViewModel(), ManageTokensClickIntents, ManageTokensUiEvents, DefaultLifecycleObserver {

    private val debouncer = Debouncer()

    private val stateFactory = ManageTokensStateFactory(
        currentStateProvider = Provider { uiState },
        clickIntents = this,
        uiIntents = this,
    )

    var router: InnerManageTokensRouter by Delegates.notNull()

    var uiState: ManageTokensState by mutableStateOf(stateFactory.getInitialState(flowOf(PagingData.from(emptyList()))))
        private set

    private var allAddedCurrencies: MutableList<CryptoCurrency> = mutableListOf()

    private var wallets: List<UserWallet> by Delegates.notNull()

    private var addedCurrenciesByWallet: MutableMap<UserWallet, MutableList<CryptoCurrency>> = mutableMapOf()

    private var selectedWallet: UserWallet? = null

    private var currenciesToGenerateAddresses: Map<UserWalletId, List<CryptoCurrency>> = emptyMap()

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
        analyticsEventHandler.send(ManageTokens.ScreenOpened())

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
                    updateDerivationNotificationState()
                    withContext(dispatchers.main) {
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

    private fun updateDerivationNotificationState() {
        viewModelScope.launch(dispatchers.io) {
            getMissedAddressesCryptoCurrenciesUseCase(wallets.map { it.walletId })
                .distinctUntilChanged()
                .collectLatest {
                    it.onRight { mapOfMissingDerivations ->
                        currenciesToGenerateAddresses = mapOfMissingDerivations
                        withContext(dispatchers.main) { updateDerivation() }
                    }
                }
        }
    }

    private fun updateDerivation() {
        val totalNeeded = currenciesToGenerateAddresses.values.sumOf { derivations -> derivations.size }
        val walletsToDerive = currenciesToGenerateAddresses.values
            .filter { derivations -> derivations.isNotEmpty() }.size
        uiState = stateFactory.updateDerivationNotification(
            totalNeeded = totalNeeded,
            totalWallets = wallets.size,
            walletsToDerive = walletsToDerive,
        )
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
        analyticsEventHandler.send(ManageTokens.ButtonCustomToken)
        router.openCustomTokensScreen()
    }

    override fun onSearchQueryChange(query: String) {
        uiState = uiState.copy(searchBarState = uiState.searchBarState.copy(query = query))

        debouncer.debounce(waitMs = DEFAULT_WAIT_TIME_MS, coroutineScope = viewModelScope + dispatchers.io) {
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
                if (token.availableAction.value == TokenButtonType.ADD) {
                    analyticsEventHandler.send(ManageTokens.ButtonAdd(token.currencySymbol))
                }
                if (token.availableAction.value == TokenButtonType.EDIT) {
                    analyticsEventHandler.send(ManageTokens.ButtonEdit(token.currencySymbol))
                }

                uiState = uiState.copy(selectedToken = token)
                val addedCurrenciesOnWallet = addedCurrenciesByWallet[selectedWallet] ?: listOf()
                stateFactory.updateTokenNetworksOnTokenSelection(token, addedCurrenciesOnWallet)
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

    override fun onGetAddressesClick() {
        if (currenciesToGenerateAddresses.isNotEmpty()) {
            viewModelScope.launch(dispatchers.io) {
                val cardCount = currenciesToGenerateAddresses.count { it.value.isNotEmpty() }
                analyticsEventHandler.send(ManageTokens.ButtonGenerateAddresses(cardCount))

                currenciesToGenerateAddresses.forEach { (walletId, currenciesToDerive) ->
                    if (currenciesToDerive.isNotEmpty()) {
                        derivePublicKeysUseCase(walletId, currenciesToDerive)
                            .onRight {
                                updateDerivationNotificationState()
                                fetchTokenListUseCase(userWalletId = walletId)
                            }
                    }
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
            analyticsEventHandler.send(
                ManageTokens.TokenSwitcherChanged(token = token.currencySymbol, AnalyticsParam.OnOffState.Off),
            )
            toggleToken(token, network, selectedWallet)
        } else {
            analyticsEventHandler.send(
                ManageTokens.TokenSwitcherChanged(token = token.currencySymbol, AnalyticsParam.OnOffState.On),
            )
            viewModelScope.launch(dispatchers.io) {
                checkCompatibilityAndToggleToken(token, network, selectedWallet)
            }
        }
    }

    private suspend fun checkCompatibilityAndToggleToken(
        token: TokenItemState.Loaded,
        network: NetworkItemState.Toggleable,
        selectedWallet: UserWallet,
    ) {
        checkCurrencyCompatibilityUseCase(
            networkId = network.id,
            isMainNetwork = network.address == null,
            userWalletId = selectedWallet.walletId,
        )
            .fold(
                ifLeft = { error ->
                    uiState = stateFactory.getStateAndTriggerEvent(
                        state = uiState,
                        event = Event.ShowAlert(
                            stateFactory.transformAddTokenErrorToAlert(
                                error,
                                network.name,
                            ),
                        ),
                        setUiState = { uiState = it },
                    )
                },
                ifRight = {
                    withContext(dispatchers.main) { toggleToken(token, network, selectedWallet) }
                },
            )
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
            updateUi(token, network)
            addedCurrenciesByWallet[selectedWallet]?.add(cryptoCurrency)
            allAddedCurrencies.add(cryptoCurrency)
            viewModelScope.launch(dispatchers.io) {
                addCryptoCurrenciesUseCase(
                    userWalletId = selectedWallet.walletId,
                    currency = cryptoCurrency,
                )
            }
        } else {
            viewModelScope.launch(dispatchers.io) {
                if (canBeRemovedAndShowAlertIfNot(selectedWallet.walletId, cryptoCurrency)) {
                    withContext(dispatchers.main) { updateUi(token, network) }
                    addedCurrenciesByWallet[selectedWallet]?.remove(cryptoCurrency)
                    allAddedCurrencies.remove(cryptoCurrency)
                    removeCurrencyUseCase(selectedWallet.walletId, cryptoCurrency)
                }
            }
        }
    }

    private fun updateUi(token: TokenItemState.Loaded, network: NetworkItemState.Toggleable) {
        stateFactory.toggleNetworkState(token, network, allAddedCurrencies)
        updateDerivationNotificationState()
    }

    private suspend fun canBeRemovedAndShowAlertIfNot(
        walletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
    ): Boolean {
        return if (cryptoCurrency is CryptoCurrency.Coin &&
            !isCryptoCurrencyCoinCouldHide(walletId, cryptoCurrency)
        ) {
            uiState = stateFactory.getStateAndTriggerEvent(
                state = uiState,
                event = Event.ShowAlert(
                    AlertState.CannotHideNetworkWithTokens(
                        tokenName = cryptoCurrency.name,
                        networkName = cryptoCurrency.network.name,
                    ),
                ),
                setUiState = { uiState = it },
            )
            false
        } else {
            true
        }
    }

    override fun onNonNativeNetworkHintClick() {
        analyticsEventHandler.send(ManageTokens.NoticeNonNativeNetworkClicked)
        uiState = stateFactory.getStateAndTriggerEvent(
            state = uiState,
            event = Event.ShowAlert(AlertState.NonNative),
            setUiState = { uiState = it },
        )
    }

    override fun onChooseWalletClick() {
        analyticsEventHandler.send(ManageTokens.ButtonChooseWallet)
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
        analyticsEventHandler.send(ManageTokens.WalletSelected(ManageTokens.WalletSelected.Source.MainToken))
        viewModelScope.launch(dispatchers.io) {
            selectWalletUseCase(UserWalletId(walletId))
        }
        selectedWallet = wallets.find { it.walletId.stringValue == walletId }
        uiState.selectedToken?.let { onTokenItemButtonClick(it) }
        uiState = stateFactory.updateSelectedWallet(selectedWalletId = selectedWallet?.walletId?.stringValue)
    }

    override fun onEmptySearchResult(query: String) {
        analyticsEventHandler.send(ManageTokens.TokenIsNotFound(query))
    }
}