package com.tangem.features.commonfeatures.impl.choosetoken.model

import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.commonfeatures.api.R
import com.tangem.features.commonfeatures.api.addtoportfolio.AddToPortfolioManager
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenAnalyticsPayload
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenBridge
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenBridgeInternal.SearchQuery
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenBridgeInternal.SearchQuery.Companion.isSearchingState
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenComponent
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenResult
import com.tangem.features.commonfeatures.impl.choosetoken.converter.SearchBarToggleTransformer
import com.tangem.features.commonfeatures.impl.choosetoken.converter.SearchBarUpdateQueryTransformer
import com.tangem.features.commonfeatures.impl.choosetoken.market.state.SwapMarketState
import com.tangem.features.commonfeatures.impl.choosetoken.ui.ChooseTokenFullUM
import com.tangem.features.commonfeatures.impl.choosetoken.ui.ChooseTokenInitialUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@Suppress("LongParameterList")
@ModelScoped
internal class ChooseTokenModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
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
        selectedWalletFlow = bridge.selectedWalletFlow,
        shouldShowSingleCurrencyWallets = bridge.settings.isShowSingleCurrencyWallets,
    )

    val bottomSheetNavigation get() = marketBlockDelegate.addToPortfolioSlot
    val addToPortfolioManager get() = marketBlockDelegate.addToPortfolioManager
    private val marketsStateFlow: Flow<SwapMarketState?> = if (bridge.settings.isShowMarketBlock) {
        marketBlockDelegate.marketsStateFlow
    } else {
        flowOf(null)
    }

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
            .onEach { notifyCurrencyChosen(it, isMarketTokenSelected = true) }
            .launchIn(modelScope)
        addToPortfolioManager.onAddedTokenClick.receiveAsFlow()
            .onEach { notifyCurrencyChosen(it, isMarketTokenSelected = false) }
            .launchIn(modelScope)
    }

    fun onBackClicked() {
        bridge.onClose()
    }

    private fun notifyCurrencyChosen(addedResult: AddToPortfolioManager.Result, isMarketTokenSelected: Boolean) {
        val chooseTokenResult = ChooseTokenResult(
            currency = addedResult.addedCurrency,
            account = addedResult.account,
            wallet = addedResult.wallet,
            analyticsPayload = setOf(
                ChooseTokenAnalyticsPayload.IsSearched(isSearchingState),
                ChooseTokenAnalyticsPayload.IsMarketTokenSelected(isMarketTokenSelected),
            ),
        )
        bridge.onCurrencyChosen(chooseTokenResult)
        marketBlockDelegate.addToPortfolioSlot.dismiss()
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
        isAppBarShown = bridge.settings.isAppBarShown,
        onCloseClick = ::onBackClicked,
        searchBar = getInitialSearchBar(),
    )

    companion object {
        const val DEBOUNCE_SEARCH_DELAY = 500L
    }
}