package com.tangem.features.commonfeatures.impl.choosetoken.model

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.features.commonfeatures.api.R
import com.tangem.features.commonfeatures.api.addtoportfolio.AddToPortfolioManager
import com.tangem.features.commonfeatures.api.choosetoken.*
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenBridgeInternal.SearchQuery
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenBridgeInternal.SearchQuery.Companion.isSearchingState
import com.tangem.features.commonfeatures.impl.choosetoken.AddToPortfolioRoute
import com.tangem.features.commonfeatures.impl.choosetoken.converter.SearchBarToggleTransformer
import com.tangem.features.commonfeatures.impl.choosetoken.converter.SearchBarUpdateQueryTransformer
import com.tangem.features.commonfeatures.impl.choosetoken.predefined.PredefinedTokensBlockDelegate
import com.tangem.features.commonfeatures.impl.choosetoken.ui.ChooseTokenFullUM
import com.tangem.features.commonfeatures.impl.choosetoken.ui.ChooseTokenInitialUM
import com.tangem.features.commonfeatures.impl.choosetoken.ui.state.ChooserBlockUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@ModelScoped
internal class ChooseTokenModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    marketBlockDelegateFactory: MarketBlockDelegate.Factory,
    predefinedTokensBlockDelegateFactory: PredefinedTokensBlockDelegate.Factory,
    addToPortfolioManagerFactory: AddToPortfolioManager.Factory,
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params = paramsContainer.require<ChooseTokenComponent.Params>()
    private val bridge: ChooseTokenBridge = params.bridge

    private val searchQueryState: StateFlow<SearchQuery> = bridge.searchQueryState
    private val isSearchingState: Boolean get() = bridge.searchQueryState.isSearchingState
    private val screensSourcesName: String = bridge.analyticsPayload
        .filterIsInstance<ChooseTokenAnalyticsPayload.ScreensSources>()
        .firstOrNull()?.value.orEmpty()

    val bottomSheetNavigation: SlotNavigation<AddToPortfolioRoute> = SlotNavigation()
    val addToPortfolioManager: AddToPortfolioManager = addToPortfolioManagerFactory.create(
        scope = modelScope,
        settings = AddToPortfolioManager.Settings.ChooseToken,
        analyticsParams = AddToPortfolioManager.AnalyticsParams(source = screensSourcesName),
    )

    private val marketBlockDelegate: MarketBlockDelegate by lazy {
        marketBlockDelegateFactory.create(
            modelScope = modelScope,
            searchQueryState = searchQueryState,
            selectedWalletFlow = bridge.selectedWalletFlow,
            shouldShowSingleCurrencyWallets = bridge.settings.isShowSingleCurrencyWallets,
            addToPortfolioManager = addToPortfolioManager,
            addToPortfolioSlot = bottomSheetNavigation,
        )
    }

    /** Tokens the user already holds in the selected wallet — subtracted from the predefined "Other eligible" block. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val portfolioTokenKeysFlow: Flow<Set<Pair<String, String>>> = bridge.selectedWalletFlow
        .flatMapLatest { wallet -> singleAccountStatusListSupplier(wallet.walletId) }
        .map { accountStatusList -> accountStatusList.toTokenKeys() }
        .onStart { emit(emptySet()) }

    private val predefinedTokensBlockDelegate: PredefinedTokensBlockDelegate by lazy {
        val block = bridge.settings.chooserBlock as ChooserBlock.Predefined
        predefinedTokensBlockDelegateFactory.create(
            predefinedTokens = block.predefinedTokens,
            searchQueryState = searchQueryState,
            addToPortfolioManager = addToPortfolioManager,
            addToPortfolioSlot = bottomSheetNavigation,
            modelScope = modelScope,
            tokenFilter = bridge.tokenFilter,
            portfolioTokenKeys = portfolioTokenKeysFlow,
        )
    }

    private val chooserBlockFlow: Flow<ChooserBlockUM?> = when (bridge.settings.chooserBlock) {
        ChooserBlock.Market -> marketBlockDelegate.marketsStateFlow.map { it?.let(ChooserBlockUM::Market) }
        is ChooserBlock.Predefined ->
            predefinedTokensBlockDelegate.stateFlow.map { it?.let(ChooserBlockUM::Predefined) }
        ChooserBlock.None -> flowOf(null)
    }

    private val initialState: MutableStateFlow<ChooseTokenInitialUM> = MutableStateFlow(getInitState())
    val state: StateFlow<ChooseTokenFullUM> = combine(
        flow = initialState,
        flow2 = bridge.fullPortfolioBlock,
        flow3 = chooserBlockFlow,
        transform = { initial, content, chooserBlock ->
            ChooseTokenFullUM(
                initialUM = initial,
                portfolioBlock = content,
                chooserBlock = chooserBlock,
            )
        },
    ).stateIn(
        scope = modelScope,
        started = SharingStarted.Eagerly,
        initialValue = ChooseTokenFullUM(
            initialUM = initialState.value,
            portfolioBlock = bridge.fullPortfolioBlock.value,
            chooserBlock = null,
        ),
    )

    init {
        if (bridge.settings.chooserBlock == ChooserBlock.Market) {
            modelScope.launch {
                delay(MARKETS_INITIAL_LOAD_DELAY)
                marketBlockDelegate.loadDefaultMarkets()
            }
        }

        addToPortfolioManager.onDismiss.receiveAsFlow()
            .onEach { bottomSheetNavigation.dismiss() }
            .launchIn(modelScope)
        addToPortfolioManager.onSuccessAdded.receiveAsFlow()
            .onEach {
                notifyCurrencyChosen(it, isMarketTokenSelected = bridge.settings.chooserBlock == ChooserBlock.Market)
            }
            .launchIn(modelScope)
        addToPortfolioManager.onAddedTokenClick.receiveAsFlow()
            .onEach { notifyCurrencyChosen(it, isMarketTokenSelected = false) }
            .launchIn(modelScope)
    }

    private fun AccountStatusList.toTokenKeys(): Set<Pair<String, String>> =
        flattenCurrencies().mapNotNullTo(hashSetOf()) { status ->
            val rawId = status.currency.id.rawCurrencyId?.value ?: return@mapNotNullTo null
            rawId to status.currency.network.rawId
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
        bottomSheetNavigation.dismiss()
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

        /** Roughly the bottom sheet entrance animation duration. */
        private const val MARKETS_INITIAL_LOAD_DELAY = 400L
    }
}