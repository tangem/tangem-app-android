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

@ModelScoped
internal class MarketCategorySelectorModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val filterController: MarketCategoryFilterController,
    private val router: Router,
    private val getMarketCategoriesUseCase: GetMarketCategoriesUseCase,
    getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    getMarketCategoryListFlowUseCase: GetMarketCategoryListFlowUseCase,
) : Model() {

    private val appCurrency: StateFlow<AppCurrency> = getSelectedAppCurrencyUseCase.invokeOrDefault()
        .stateIn(modelScope, SharingStarted.Eagerly, AppCurrency.Default)

    private var featured: List<MarketCategory> = emptyList()
    private var categories: List<MarketCategory> = emptyList()
    private var listStatus: PaginationStatus<List<MarketCategory>> = PaginationStatus.None

    private var appliedQuery: String? = null

    /** The filter as it stood when the screen opened; [onReset] returns here and the footer compares to it. */
    private val appliedSelection: MarketCategorySelection = filterController.selection.value

    /** Pending choice; [appliedSelection] stands until [onApply] publishes this one to the tab. */
    private var draftSelection: MarketCategorySelection = appliedSelection

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
            // The screen's close button sits beside the field: the one built into TangemSearch is
            // hidden while the field is idle and empty, which is when there is most to escape from.
            onCloseClick = null,
        ),
        isEdited = false,
        footer = MarketCategorySelectorFooterUM(onReset = ::onReset, onApply = ::onApply),
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

    private fun onSelectionChange(selection: MarketCategorySelection) {
        draftSelection = selection
        render()
    }

    private fun onReset() {
        draftSelection = appliedSelection
        render()
    }

    private fun onApply() {
        filterController.onSelectionApplied(draftSelection)
        router.pop()
    }

    private fun render() {
        val isEdited = draftSelection != appliedSelection

        when {
            listStatus is PaginationStatus.None || listStatus is PaginationStatus.InitialLoading ->
                renderBody(isEdited = isEdited, content = MarketCategorySelectorUM.ContentUM.Loading)
            listStatus is PaginationStatus.InitialLoadingError ->
                renderBody(isEdited = isEdited, content = MarketCategorySelectorUM.ContentUM.Error(onRetry = ::load))
            // "No results" is search copy, so an unfiltered listing that comes back empty keeps the
            // ordinary list body rather than borrowing it
            appliedQuery != null && categories.isEmpty() ->
                renderBody(isEdited = isEdited, content = MarketCategorySelectorUM.ContentUM.Empty)
            else -> uiState.update(
                transformer = MarketCategorySelectorTransformer(
                    featured = featured,
                    categories = categories,
                    selection = draftSelection,
                    appCurrency = appCurrency.value,
                    isSearching = appliedQuery != null,
                    isEdited = isEdited,
                    isLoadingMore = listStatus is PaginationStatus.NextBatchLoading,
                    onLoadMore = listManager::loadMore,
                    onSelectionChange = ::onSelectionChange,
                ),
            )
        }
    }

    private fun renderBody(isEdited: Boolean, content: MarketCategorySelectorUM.ContentUM) {
        uiState.update { it.copy(isEdited = isEdited, content = content) }
    }
}