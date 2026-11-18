package com.tangem.features.feed.crypto.categories.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.ds2.search.TangemSearch
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.GetMarketCategoriesUseCase
import com.tangem.domain.markets.GetMarketCategoryListFlowUseCase
import com.tangem.domain.markets.MarketCategory
import com.tangem.features.feed.FeedFeatureToggles
import com.tangem.features.feed.crypto.categories.MarketCategoryFilterController
import com.tangem.features.feed.crypto.categories.MarketCategorySelection
import com.tangem.features.feed.crypto.categories.model.transformer.MarketCategorySelectorTransformer
import com.tangem.features.feed.crypto.categories.ui.state.MarketCategorySelectorFooterUM
import com.tangem.features.feed.crypto.categories.ui.state.MarketCategorySelectorUM
import com.tangem.features.feed.crypto.impl.R
import com.tangem.pagination.PaginationStatus
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.transformer.update
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A featured rail that needs scrolling is a design failure, not a paging problem. */
private const val FEATURED_LIMIT = 20

private const val QUERY_DEBOUNCE_MILLIS = 500L

@Suppress("LongParameterList")
@ModelScoped
internal class MarketCategorySelectorModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val filterController: MarketCategoryFilterController,
    private val router: Router,
    private val getMarketCategoriesUseCase: GetMarketCategoriesUseCase,
    getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    getMarketCategoryListFlowUseCase: GetMarketCategoryListFlowUseCase,
    feedFeatureToggles: FeedFeatureToggles,
) : Model() {

    /** Read once, so an open screen never switches behaviour under the user. */
    private val isRedesignEnabled = feedFeatureToggles.isCategoryFiltersRedesignEnabled

    private val appCurrency: StateFlow<AppCurrency> = getSelectedAppCurrencyUseCase.invokeOrDefault()
        .stateIn(modelScope, SharingStarted.Eagerly, AppCurrency.Default)

    private var featured: List<MarketCategory> = emptyList()
    private var categories: List<MarketCategory> = emptyList()
    private var listStatus: PaginationStatus<List<MarketCategory>> = PaginationStatus.None

    private var appliedQuery: String? = null

    /** A tap applies and closes the screen; only a discover deeplink changes the filter while it stays open. */
    private val appliedSelection: MarketCategorySelection get() = filterController.selection.value

    private val listManager = MarketCategoryListManager(
        getMarketCategoryListFlowUseCase = getMarketCategoryListFlowUseCase,
        currentAppCurrencyCode = { appCurrency.value.code },
        modelScope = modelScope,
    )

    val uiState: StateFlow<MarketCategorySelectorUM>
        field = MutableStateFlow(buildInitialState())

    init {
        combine(listManager.categories, listManager.status, ::Pair)
            .onEach { (loaded, status) ->
                categories = loaded
                listStatus = status
                render()
            }
            .launchIn(modelScope)

        filterController.selection.drop(count = 1)
            .onEach { selection ->
                uiState.update { it.copy(isResetVisible = selection.category != null) }
                render()
            }
            .launchIn(modelScope)

        observeQuery()
        load()
    }

    private fun buildInitialState() = MarketCategorySelectorUM(
        search = TangemSearch.State(
            placeholderText = resourceReference(R.string.common_search),
            query = "",
            onQueryChange = ::onQueryChange,
            isActive = false,
            onActiveChange = ::onSearchActiveChange,
            onClearClick = { onQueryChange(query = "") },
            // the field offers its own close only with the redesign; the sheet is closed from the header either way
            onCloseClick = if (isRedesignEnabled) {
                { onQueryChange(query = "") }
            } else {
                null
            },
        ),
        isResetVisible = appliedSelection.category != null,
        footer = MarketCategorySelectorFooterUM(onReset = ::onReset),
        content = MarketCategorySelectorUM.ContentUM.Loading,
    )

    /**
     * The currency is resolved before either request so both pages are priced consistently, rather
     * than the first one racing the app-currency store and going out in the default currency.
     */
    private fun load() {
        uiState.update { it.copy(content = MarketCategorySelectorUM.ContentUM.Loading) }
        modelScope.launch {
            loadFeatured()
            listManager.reload(searchText = appliedQuery)
        }
    }

    private suspend fun loadFeatured() {
        featured = getMarketCategoriesUseCase(
            currency = appCurrency.value.code,
            offset = 0,
            limit = FEATURED_LIMIT,
            featured = true,
        ).fold(
            ifLeft = { emptyList() },
            ifRight = { page -> page.categories },
        )
        render()
    }

    @OptIn(FlowPreview::class)
    private fun observeQuery() {
        uiState
            .map { it.search.query.trim() }
            .distinctUntilChanged()
            .drop(count = 1)
            .debounce(QUERY_DEBOUNCE_MILLIS)
            .onEach { query ->
                appliedQuery = query.takeIf(String::isNotEmpty)
                listManager.reload(searchText = appliedQuery)
            }
            .launchIn(modelScope)
    }

    private fun onQueryChange(query: String) {
        uiState.update { it.copy(search = it.search.copy(query = query)) }
    }

    private fun onSearchActiveChange(isActive: Boolean) {
        uiState.update { it.copy(search = it.search.copy(isActive = isActive)) }
    }

    /** Re-tapping the applied category re-publishes an equal value, which the controller's flow drops. */
    private fun onSelectionChange(selection: MarketCategorySelection) {
        filterController.onSelectionApplied(selection)
        router.pop()
    }

    private fun onReset() {
        onSelectionChange(MarketCategorySelection())
    }

    private fun render() {
        when {
            listStatus is PaginationStatus.None || listStatus is PaginationStatus.InitialLoading ->
                renderBody(content = MarketCategorySelectorUM.ContentUM.Loading)
            listStatus is PaginationStatus.InitialLoadingError ->
                renderBody(content = MarketCategorySelectorUM.ContentUM.Error(onRetry = ::load))
            // "No results" is search copy, so an unfiltered listing that comes back empty keeps the
            // ordinary list body rather than borrowing it
            appliedQuery != null && categories.isEmpty() ->
                renderBody(content = MarketCategorySelectorUM.ContentUM.Empty)
            else -> uiState.update(
                transformer = MarketCategorySelectorTransformer(
                    // without the redesign a search drops the rail: an empty featured list hides it
                    featured = if (isRedesignEnabled || appliedQuery == null) featured else emptyList(),
                    categories = categories,
                    selection = appliedSelection,
                    appCurrency = appCurrency.value,
                    query = appliedQuery,
                    isLoadingMore = listStatus is PaginationStatus.NextBatchLoading,
                    onLoadMore = listManager::loadMore,
                    onSelectionChange = ::onSelectionChange,
                ),
            )
        }
    }

    private fun renderBody(content: MarketCategorySelectorUM.ContentUM) {
        uiState.update { it.copy(content = content) }
    }
}