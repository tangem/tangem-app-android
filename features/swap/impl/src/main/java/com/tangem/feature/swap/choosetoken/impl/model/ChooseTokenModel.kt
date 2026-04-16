package com.tangem.feature.swap.choosetoken.impl.model

import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.models.account.AccountId
import com.tangem.feature.swap.choosetoken.api.*
import com.tangem.feature.swap.choosetoken.api.ChooseTokenBridgeInternal.SearchQuery
import com.tangem.feature.swap.choosetoken.api.ChooseTokenBridgeInternal.SearchQuery.Companion.isSearchingState
import com.tangem.feature.swap.choosetoken.impl.converter.SearchBarToggleTransformer
import com.tangem.feature.swap.choosetoken.impl.converter.SearchBarUpdateQueryTransformer
import com.tangem.feature.swap.choosetoken.impl.ui.ChooseTokenFullUM
import com.tangem.feature.swap.choosetoken.impl.ui.ChooseTokenInitialUM
import com.tangem.feature.swap.converters.TokensDataConverter
import com.tangem.feature.swap.models.SwapSelectTokenStateHolder
import com.tangem.feature.swap.models.market.state.SwapMarketState
import com.tangem.feature.swap.presentation.R
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@Suppress("LongParameterList")
@ModelScoped
internal class ChooseTokenModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val settingContextUseCase: SettingContextUseCase,
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

    val stateOld: StateFlow<SwapSelectTokenStateHolder?> = combineUIOld()

    private val initialState: MutableStateFlow<ChooseTokenInitialUM> = MutableStateFlow(getInitState())
    val state: StateFlow<ChooseTokenFullUM> = combine(
        flow = initialState,
        flow2 = bridge.fullPortfolioBlock,
        flow3 = marketsStateFlow,
        transform = { initial, content, marketBlock ->
            ChooseTokenFullUM(
                initialUM = initial,
                portfolioBlock = content,
                marketsBlock = marketBlock,
            )
        },
    ).stateIn(
        scope = modelScope,
        started = SharingStarted.Eagerly,
        initialValue = ChooseTokenFullUM(
            initialUM = initialState.value,
            portfolioBlock = bridge.fullPortfolioBlock.value,
            marketsBlock = null,
        ),
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