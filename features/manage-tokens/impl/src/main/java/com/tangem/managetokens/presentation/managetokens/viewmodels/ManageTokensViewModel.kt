package com.tangem.managetokens.presentation.managetokens.viewmodels

import androidx.compose.runtime.*
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
import com.tangem.features.managetokens.navigation.ExpandableState
import com.tangem.managetokens.presentation.common.analytics.ManageTokens
import com.tangem.managetokens.presentation.common.state.AlertState
import com.tangem.managetokens.presentation.common.state.Event
import com.tangem.managetokens.presentation.common.state.NetworkItemState
import com.tangem.managetokens.presentation.managetokens.state.ManageTokensState
import com.tangem.managetokens.presentation.managetokens.state.TokenButtonType
import com.tangem.managetokens.presentation.managetokens.state.TokenItemState
import com.tangem.managetokens.presentation.managetokens.state.factory.*
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.Debouncer
import com.tangem.utils.coroutines.Debouncer.Companion.DEFAULT_WAIT_TIME_MS
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
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
) : ViewModel(), ManageTokensClickIntents, ManageTokensUiEvents {

    private val stateFactory = ManageTokensStateFactory(
        currentStateProvider = Provider { uiState },
        clickIntents = this,
        uiIntents = this,
    )

    var uiState: ManageTokensState by mutableStateOf(stateFactory.getInitialState(flowOf(PagingData.from(emptyList()))))
        private set

    private var expandableState: ExpandableState = ExpandableState.COLLAPSED

    private val currenciesListJobHolder: JobHolder = JobHolder()

    private val debouncer = Debouncer()

    private var allAddedCurrencies: MutableList<CryptoCurrency> = Collections.synchronizedList(
        mutableListOf<CryptoCurrency>(),
    )

    private var wallets: CopyOnWriteArrayList<UserWallet> by Delegates.notNull()

    private var addedCurrenciesByWallet: MutableMap<UserWallet, MutableList<CryptoCurrency>> = ConcurrentHashMap()

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
                    launch {
                        subscribeToCurrencies(userWallets)
                    }.saveIn(currenciesListJobHolder)
                }
        }
    }

    fun setExpandableState(state: State<ExpandableState>) {
        expandableState = state.value
    }

    private suspend fun subscribeToCurrencies(userWallets: List<UserWallet>) {
        wallets = CopyOnWriteArrayList(userWallets.filter { it.isMultiCurrency && !it.isLocked })

        combine(wallets.map { getCurrenciesUseCase.invoke(it.walletId).distinctUntilChanged() }) {
            if (expandableState == ExpandableState.EXPANDED) return@combine

            allAddedCurrencies.clear()
            addedCurrenciesByWallet.clear()

            val walletsWithCurrencies = wallets.zip(
                it.map { currencyList ->
                    currencyList.getOrElse {
                        Timber.e("Couldn't retrieve currency list")
                        emptyList()
                    }
                },
            )

            allAddedCurrencies = walletsWithCurrencies.flatMap { it.second }.toMutableList()

            walletsWithCurrencies.forEach { (wallet, currencies) ->
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
        }.collect()
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
        uiState = uiState.copy(customTokenBottomSheetConfig = uiState.customTokenBottomSheetConfig.copy(isShow = true))
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
            addedCurrenciesByWallet[selectedWallet]?.add(cryptoCurrency)
            allAddedCurrencies.add(cryptoCurrency)
            viewModelScope.launch(dispatchers.io) {
                addCryptoCurrenciesUseCase(
                    userWalletId = selectedWallet.walletId,
                    currency = cryptoCurrency,
                )
            }
            updateUi(token, network)
        } else {
            viewModelScope.launch(dispatchers.io) {
                if (canBeRemovedAndShowAlertIfNot(selectedWallet.walletId, cryptoCurrency)) {
                    addedCurrenciesByWallet[selectedWallet]?.remove(cryptoCurrency)
                    allAddedCurrencies.remove(cryptoCurrency)
                    removeCurrencyUseCase(selectedWallet.walletId, cryptoCurrency)
                    withContext(dispatchers.main) { updateUi(token, network) }
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

    override fun onAddCustomTokenSheetDismissed() {
        uiState = uiState.copy(customTokenBottomSheetConfig = uiState.customTokenBottomSheetConfig.copy(isShow = false))
    }
}