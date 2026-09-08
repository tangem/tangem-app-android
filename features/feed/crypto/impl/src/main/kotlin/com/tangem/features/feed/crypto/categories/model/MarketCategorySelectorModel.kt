package com.tangem.features.feed.crypto.categories.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.GetMarketCategoriesUseCase
import com.tangem.domain.markets.GetMarketCategoryListFlowUseCase
import com.tangem.domain.markets.MarketCategory
import com.tangem.features.feed.crypto.categories.MarketCategorySelection
import com.tangem.features.feed.crypto.categories.MarketCategorySelectorRoute
import com.tangem.features.feed.crypto.categories.model.transformer.MarketCategorySelectorTransformer
import com.tangem.features.feed.crypto.categories.ui.state.MarketCategorySelectorUM
import com.tangem.pagination.PaginationStatus
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.transformer.update
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A featured rail that needs scrolling is a design failure, not a paging problem. */
private const val FEATURED_LIMIT = 20

@ModelScoped
internal class MarketCategorySelectorModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
    private val router: Router,
    private val getMarketCategoriesUseCase: GetMarketCategoriesUseCase,
    getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    getMarketCategoryListFlowUseCase: GetMarketCategoryListFlowUseCase,
) : Model() {

    private val params = paramsContainer.require<MarketCategorySelectorRoute>()

    private val appCurrency: StateFlow<AppCurrency> = getSelectedAppCurrencyUseCase.invokeOrDefault()
        .stateIn(modelScope, SharingStarted.Eagerly, AppCurrency.Default)

    private var featured: List<MarketCategory> = emptyList()
    private var categories: List<MarketCategory> = emptyList()
    private var listStatus: PaginationStatus<List<MarketCategory>> = PaginationStatus.None

    /** Pending choice; [params] holds the applied one until [onApply] hands it back to the tab. */
    private var draftSelection: MarketCategorySelection = params.selection

    private val listManager = MarketCategoryListManager(
        getMarketCategoryListFlowUseCase = getMarketCategoryListFlowUseCase,
        currentAppCurrencyCode = { appCurrency.value.code },
        modelScope = modelScope,
    )

    val uiState: StateFlow<MarketCategorySelectorUM>
        field = MutableStateFlow<MarketCategorySelectorUM>(MarketCategorySelectorUM.Loading)

    init {
        combine(listManager.categories, listManager.status, ::Pair)
            .onEach { (loaded, status) ->
                categories = loaded
                listStatus = status
                render()
            }
            .launchIn(modelScope)

        load()
    }

    /**
     * The currency is resolved before either request so both pages are priced consistently, rather
     * than the first one racing the app-currency store and going out in the default currency.
     */
    private fun load() {
        uiState.value = MarketCategorySelectorUM.Loading
        modelScope.launch {
            loadFeatured()
            listManager.reload()
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

    private fun onSelectionChange(selection: MarketCategorySelection) {
        draftSelection = selection
        render()
    }

    private fun onReset() {
        draftSelection = params.selection
        render()
    }

    private fun onApply() {
        params.callback.onCategorySelect(draftSelection)
        router.pop()
    }

    private fun render() {
        when (listStatus) {
            is PaginationStatus.None,
            is PaginationStatus.InitialLoading,
            -> uiState.value = MarketCategorySelectorUM.Loading
            is PaginationStatus.InitialLoadingError -> uiState.value =
                MarketCategorySelectorUM.Error(onRetry = ::load)
            else -> uiState.update(
                transformer = MarketCategorySelectorTransformer(
                    featured = featured,
                    categories = categories,
                    selection = draftSelection,
                    appCurrency = appCurrency.value,
                    isEdited = draftSelection != params.selection,
                    isLoadingMore = listStatus is PaginationStatus.NextBatchLoading,
                    onLoadMore = listManager::loadMore,
                    onReset = ::onReset,
                    onApply = ::onApply,
                    onSelectionChange = ::onSelectionChange,
                ),
            )
        }
    }
}