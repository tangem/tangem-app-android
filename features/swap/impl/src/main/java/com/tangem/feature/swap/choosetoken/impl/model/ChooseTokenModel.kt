package com.tangem.feature.swap.choosetoken.impl.model

import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.feature.swap.choosetoken.api.*
import com.tangem.feature.swap.choosetoken.api.ChooseTokenBridgeInternal.SearchQuery
import com.tangem.feature.swap.choosetoken.api.ChooseTokenBridgeInternal.SearchQuery.Companion.isSearchingState
import com.tangem.feature.swap.choosetoken.impl.converter.SearchBarToggleTransformer
import com.tangem.feature.swap.choosetoken.impl.converter.SearchBarUpdateQueryTransformer
import com.tangem.feature.swap.choosetoken.impl.ui.*
import com.tangem.feature.swap.converters.TokensDataConverter
import com.tangem.feature.swap.models.SwapSelectTokenStateHolder
import com.tangem.feature.swap.models.market.state.SwapMarketState
import com.tangem.feature.swap.presentation.R
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@Suppress("LongParameterList")
@ModelScoped
internal class ChooseTokenModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val settingContextUseCase: SettingContextUseCase,
    private val getWalletsUseCase: GetWalletsUseCase,
    marketBlockDelegateFactory: MarketBlockDelegate.Factory,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params = paramsContainer.require<ChooseTokenComponent.Params>()
    private val bridge: ChooseTokenBridge = params.bridge

    private val searchQueryState: StateFlow<SearchQuery> = bridge.searchQueryState
    private val isSearchingState: Boolean get() = bridge.searchQueryState.isSearchingState
    private val marketBlockDelegate: MarketBlockDelegate = marketBlockDelegateFactory.create(
        modelScope = modelScope,
        searchQueryState = searchQueryState,
        screensSourcesName = bridge.analyticsPayload
            .filterIsInstance<ChooseTokenAnalyticsPayload.ScreensSources>()
            .firstOrNull()?.value.orEmpty(),
    )

    val bottomSheetNavigation get() = marketBlockDelegate.addToPortfolioSlot
    val addToPortfolioManager get() = marketBlockDelegate.addToPortfolioManager
    private val marketsStateFlow: Flow<SwapMarketState?> = if (bridge.settings.isShowMarketBlock) {
        marketBlockDelegate.marketsStateFlow
    } else {
        flowOf(null)
    }

    private val expandedAccountsFlow: MutableStateFlow<Map<AccountId, Boolean>> = MutableStateFlow(emptyMap())
    private val onWalletSelected = Channel<UserWalletId>()

    val stateOld: StateFlow<SwapSelectTokenStateHolder?> = combineUIOld()

    private val contentState: StateFlow<ChooseTokenUM?> = combineUI()
    private val initialState: MutableStateFlow<ChooseTokenInitialUM> = MutableStateFlow(getInitState())
    val state: StateFlow<ChooseTokenFullUM> = combine(
        flow = initialState,
        flow2 = contentState,
        transform = { initial, content ->
            ChooseTokenFullUM(
                initialUM = initial,
                contentUM = content,
            )
        },
    ).stateIn(
        scope = modelScope,
        started = SharingStarted.Eagerly,
        initialValue = ChooseTokenFullUM(initialState.value, contentState.value),
    )

    init {
        addToPortfolioManager.onDismiss.receiveAsFlow()
            .onEach { marketBlockDelegate.addToPortfolioSlot.dismiss() }
            .launchIn(modelScope)
        addToPortfolioManager.onSuccessAdded.receiveAsFlow()
            .onEach { addedResult ->
                val isSearched = ChooseTokenAnalyticsPayload.IsSearched(isSearchingState)
                val newToken = addedResult.addedCurrency.currency to isSearched
                bridge.onNewTokenAdded(newToken)
                val chooseTokenResult = ChooseTokenResult(
                    currency = addedResult.addedCurrency,
                    account = addedResult.account,
                    wallet = addedResult.wallet,
                    analyticsPayload = setOf(isSearched),
                )
                bridge.onCurrencyChosen(chooseTokenResult)
                marketBlockDelegate.addToPortfolioSlot.dismiss()
            }
            .launchIn(modelScope)
    }

    @Suppress("UnusedPrivateMember")
    private fun combineUIOld(): StateFlow<SwapSelectTokenStateHolder?> = combine(
        flow = bridge.currenciesGroup,
        flow2 = settingContextUseCase.invoke(),
        flow3 = marketsStateFlow,
        flow4 = expandedAccountsFlow,
        transform = { currenciesGroup, settingContext, marketState, expandedAccounts ->
            val isAccountsMode = settingContext.isAccountsMode
            val appCurrency = settingContext.appCurrency
            val isBalanceHidden = settingContext.isBalanceHidden

            TokensDataConverter(
                onSearchEntered = { query -> bridge.onSearchQuery(query) },
                onTokenClick = { account, cryptoCurrencyStatus ->
                    val result = ChooseTokenResultOld(
                        account = account,
                        cryptoCurrencyStatus = cryptoCurrencyStatus,
                        isSearched = searchQueryState.isSearchingState,
                    )
                    bridge.onTokenSelected(result)
                },
                onAccountClick = { account ->
                    expandedAccountsFlow.update { expandedList ->
                        val hasSavedAccount = expandedList[account.accountId]
                        val isExpanded = hasSavedAccount == true
                        expandedList + (account.accountId to !isExpanded)
                    }
                },
                expandedAccounts = expandedAccounts,
                tokensDataState = currenciesGroup,
                isBalanceHidden = isBalanceHidden,
                isAccountsMode = isAccountsMode,
                appCurrency = appCurrency,
                marketState = requireNotNull(marketState),
            ).transform()
        },
    )
        .flowOn(dispatchers.default)
        .stateIn(modelScope, SharingStarted.Eagerly, initialValue = null)

    @Suppress("LongMethod")
    private fun combineUI(): StateFlow<ChooseTokenUM?> = channelFlow {
        val allWalletsFlow: StateFlow<Map<UserWalletId, UserWallet>> =
            getWalletsUseCase.invokeAsMap().stateIn(this)

        // todo swap add optional param, store, and GetSelectedWalletUseCase
        val firstSelectedWallet = allWalletsFlow.value.values.first()
        val selectedWalletFlow: StateFlow<UserWallet> =
            onWalletSelected.receiveAsFlow()
                .mapNotNull { walletId -> allWalletsFlow.value[walletId] }
                .stateIn(this, SharingStarted.Eagerly, firstSelectedWallet)

        val fullPortfolioBlockFlow = combine(
            flow = allWalletsFlow,
            flow2 = bridge.portfolioListBlock,
            flow3 = selectedWalletFlow.map { wallet -> wallet.walletId }.distinctUntilChanged(),
            transform = { allWallets, portfolioList, selectedWalletId ->
                val tokensListData = portfolioList[selectedWalletId] ?: return@combine null
                val walletsUM = allWallets.entries
                    .map { (walletId, wallet) ->
                        val searchResultCount: TextReference? = portfolioList[walletId]?.totalTokensCount
                            ?.toString()
                            ?.let(::stringReference)
                            ?.takeIf { isSearchingState }
                        WalletTabUM(
                            text = stringReference(wallet.name),
                            onClick = { onWalletSelected.trySend(walletId) },
                            isSelected = selectedWalletId == walletId,
                            count = searchResultCount,
                        )
                    }
                walletsUM to tokensListData
            },
        )
            .filterNotNull()
            .distinctUntilChanged()

        combine(
            flow = fullPortfolioBlockFlow,
            flow2 = settingContextUseCase.invoke(),
            flow3 = marketsStateFlow,
            transform = { (walletList, tokensData), settings, marketsData ->
                val walletsUM = if (walletList.size != 1) {
                    WalletListUM(walletList.toPersistentList())
                } else {
                    WalletListUM(persistentListOf())
                }
                ChooseTokenUM(
                    walletList = walletsUM,
                    isBalanceHidden = settings.isBalanceHidden,
                    isSearching = isSearchingState,
                    tokensListData = tokensData,
                    marketsState = marketsData,
                )
            },
        )
            .distinctUntilChanged()
            .collect { newUM -> channel.send(newUM) }
    }
        .flowOn(dispatchers.default)
        .stateIn(modelScope, SharingStarted.Eagerly, initialValue = null)

    fun onBackClicked() {
        bridge.onClose()
    }

    private fun getInitialSearchBar(): SearchBarUM = SearchBarUM(
        placeholderText = resourceReference(R.string.common_search),
        query = "",
        isActive = false,
        onQueryChange = { query ->
            initialState.update { prevState -> SearchBarUpdateQueryTransformer(query).transform(prevState) }
            bridge.onSearchQuery(query)
        },
        onActiveChange = { isActive ->
            initialState.update { prevState -> SearchBarToggleTransformer(isActive).transform(prevState) }
        },
    )

    private fun getInitState() = ChooseTokenInitialUM(
        screenTitle = bridge.settings.title,
        onCloseClick = ::onBackClicked,
        searchBar = getInitialSearchBar(),
    )

    companion object {
        const val DEBOUNCE_SEARCH_DELAY = 500L
    }
}