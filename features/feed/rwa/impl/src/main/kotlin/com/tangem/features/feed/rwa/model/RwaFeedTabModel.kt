package com.tangem.features.feed.rwa.model

import arrow.core.getOrElse
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.R
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.core.ui.ds2.filter.TangemFilterItemUM
import com.tangem.core.ui.ds2.segmentedcontrol.TangemSegmentedControl
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.*
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.feed.nav.FeedRoute
import com.tangem.features.feed.rwa.categories.CategorySelection
import com.tangem.features.feed.rwa.categories.CategorySelectorRoute
import com.tangem.features.feed.rwa.categories.ReservedCategoryCodes
import com.tangem.features.feed.rwa.categories.components.CategorySelectorComponent
import com.tangem.features.feed.rwa.model.list.RwaBatchFlowManager
import com.tangem.features.feed.rwa.model.list.RwaFeaturedAssetsManager
import com.tangem.features.feed.rwa.model.list.RwaInterval
import com.tangem.features.feed.rwa.model.list.RwaListRequest
import com.tangem.features.feed.rwa.model.list.RwaSort
import com.tangem.features.feed.rwa.ui.state.RwaFeaturedUM
import com.tangem.features.feed.rwa.ui.state.RwaFeedTabUM
import com.tangem.features.feed.rwa.ui.state.RwaListUM
import com.tangem.features.feed.rwa.ui.state.RwaTagUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val UPDATE_QUOTES_TIMER_MILLIS = 60_000L

/**
 * Stable identity of the category chip. `TangemFilterGroup` keys its row on it, so the id has to stay
 * the same across `Inactive` and `Active` for the chip to animate between the two instead of being
 * rebuilt as a different item.
 */
private const val CATEGORIES_FILTER_ID = "rwa_categories"

/**
 * Model of the Real assets tab. The Featured rail and the main list are two independent requests
 * against the same endpoint: the rail is pinned to the reserved `featured-assets` category, the list
 * follows whatever the user picks in the Category selector.
 *
 * The list is never requested unscoped — with no user filter it falls back to the `real-world-assets`
 * umbrella category, which the selector surfaces as "All". Both category ids come from the same
 * categories fetch, so the first list request waits for it.
 */
@ModelScoped
internal class RwaFeedTabModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    getMarketsTokenListFlowUseCase: GetMarketsTokenListFlowUseCase,
    getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val getCoinCategoriesUseCase: GetCoinCategoriesUseCase,
    private val router: Router,
) : Model(), CategorySelectorComponent.ModelCallback {

    private val currentAppCurrency = getSelectedAppCurrencyUseCase().map { maybeAppCurrency ->
        maybeAppCurrency.getOrElse { AppCurrency.Default }
    }.stateIn(
        scope = modelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppCurrency.Default,
    )

    /** Expanded/collapsed state of the hosting shtorka — quotes refresh only while expanded. */
    val containerBottomSheetState = MutableStateFlow(BottomSheetState.COLLAPSED)
    val isVisibleOnScreen = MutableStateFlow(false)

    private val updateQuotesJob = JobHolder()

    private val categoriesState = MutableStateFlow(CategoriesState())
    private val visibleItemIds = MutableStateFlow<List<CryptoCurrency.RawID>>(emptyList())

    val uiState: StateFlow<RwaFeedTabUM>
        field = MutableStateFlow(initialState())

    private val listRequest = MutableStateFlow(RwaListRequest())

    private val listManager = RwaBatchFlowManager(
        getMarketsTokenListFlowUseCase = getMarketsTokenListFlowUseCase,
        request = listRequest,
        appCurrency = currentAppCurrency,
        onItemClick = ::openTokenDetails,
        modelScope = modelScope,
        dispatchers = dispatchers,
    )

    private val featuredManager = RwaFeaturedAssetsManager(
        getMarketsTokenListFlowUseCase = getMarketsTokenListFlowUseCase,
        request = listRequest,
        appCurrency = currentAppCurrency,
        onItemClick = ::openTokenDetails,
        modelScope = modelScope,
    )

    init {
        combine(
            flow = listManager.uiItems,
            flow2 = listManager.isInInitialLoadingErrorState,
            flow3 = categoriesState.map { it.isFailed }.distinctUntilChanged(),
        ) { items, isInErrorState, isCategoriesFailed ->
            when {
                isInErrorState || isCategoriesFailed -> RwaListUM.Error(onRetry = ::reloadList)
                items.isEmpty() -> RwaListUM.Loading
                else -> RwaListUM.Content(items = items, loadMore = listManager::loadMore)
            }
        }
            .distinctUntilChanged()
            .onEach { list -> uiState.update { it.copy(list = list) } }
            .launchIn(modelScope)

        currentAppCurrency.drop(1)
            .onEach {
                reloadList()
                reloadFeatured()
            }
            .launchIn(modelScope)

        listManager.onLastBatchLoadedSuccess.onEach { batchKey ->
            listManager.loadCharts(setOf(batchKey), uiState.value.selectedInterval)
            modelScope.loadQuotesWithTimer(timeMillis = UPDATE_QUOTES_TIMER_MILLIS)
        }.launchIn(modelScope)

        visibleItemIds
            .mapNotNull { ids -> ids.takeIf { it.isNotEmpty() }?.let(listManager::getBatchKeysByItemIds) }
            .distinctUntilChanged()
            .onEach { visibleBatchKeys -> listManager.loadCharts(visibleBatchKeys, uiState.value.selectedInterval) }
            .launchIn(modelScope)

        featuredManager.state
            .onEach { featured -> uiState.update { it.copy(featured = featured) } }
            .launchIn(modelScope)

        loadCategories()
    }

    private fun initialState(): RwaFeedTabUM {
        val request = RwaListRequest()

        return RwaFeedTabUM(
            featured = RwaFeaturedUM.Loading,
            categoriesFilter = categoriesFilterItem(selection = request.selection),
            interval = intervalControl(selected = request.interval),
            tags = RwaSort.entries
                .map { RwaTagUM(id = it.id, title = it.title) }
                .toImmutableList(),
            onTagSelect = ::selectSort,
            onVisibleItemsChange = { ids -> visibleItemIds.value = ids.map(CryptoCurrency::RawID) },
            list = RwaListUM.Loading,
            selectedInterval = request.interval,
            selectedSort = request.sort,
            appliedSelection = request.selection,
        )
    }

    private fun loadCategories() {
        categoriesState.update { it.copy(isFailed = false) }

        modelScope.launch {
            getCoinCategoriesUseCase().fold(
                ifLeft = { _ ->
                    uiState.update { it.copy(featured = RwaFeaturedUM.Empty) }
                    categoriesState.update { it.copy(isFailed = true) }
                },
                ifRight = ::onCategoriesLoaded,
            )
        }
    }

    private fun onCategoriesLoaded(categories: List<CoinCategory>) {
        val featured = categories.firstOrNull { it.code == ReservedCategoryCodes.FEATURED_ASSETS }

        if (featured == null) {
            uiState.update { it.copy(featured = RwaFeaturedUM.Empty) }
        } else {
            categoriesState.update { it.copy(featuredCategoryId = featured.id) }
            reloadFeatured()
        }

        val umbrella = categories.firstOrNull { it.code == ReservedCategoryCodes.REAL_WORLD_ASSETS }

        if (umbrella == null) {
            categoriesState.update { it.copy(isFailed = true) }
        } else {
            val entry = CategorySelection.Entry(id = umbrella.id, displayName = umbrella.displayName)
            categoriesState.update { it.copy(rwaCategory = entry) }
            reloadList()
        }
    }

    private fun reloadFeatured() {
        val categoryId = categoriesState.value.featuredCategoryId ?: return
        featuredManager.load(categoryId = categoryId)
    }

    /** Reloading before the umbrella id is known would request the whole coin universe, so re-resolve it first. */
    private fun reloadList() {
        if (categoriesState.value.rwaCategory == null) {
            loadCategories()
        } else {
            syncListRequest()
            listManager.reload()
        }
    }

    private fun syncListRequest() {
        val state = uiState.value

        listRequest.value = RwaListRequest(
            interval = state.selectedInterval,
            sort = state.selectedSort,
            selection = state.appliedSelection.orUmbrella(categoriesState.value.rwaCategory),
        )
    }

    /** No user filter means the whole real-world-assets umbrella, never the entire coin universe. */
    private fun CategorySelection.orUmbrella(umbrella: CategorySelection.Entry?): CategorySelection {
        return if (category == null) CategorySelection(category = umbrella) else this
    }

    private fun categoriesFilterItem(selection: CategorySelection): TangemFilterItemUM {
        val category = selection.category
            ?: return TangemFilterItemUM.Inactive(
                id = CATEGORIES_FILTER_ID,
                label = resourceReference(R.string.common_all),
                onClick = ::openCategorySelector,
            )

        return TangemFilterItemUM.Active(
            id = CATEGORIES_FILTER_ID,
            value = stringReference(selection.sector?.displayName ?: category.displayName),
            onClick = ::openCategorySelector,
            onClearClick = { onCategorySelect(CategorySelection()) },
        )
    }

    private fun openCategorySelector() {
        router.push(
            CategorySelectorRoute(
                categorySelection = uiState.value.appliedSelection,
                callback = this,
            ),
        )
    }

    private fun intervalControl(selected: RwaInterval): TangemSegmentedControl.State {
        return TangemSegmentedControl.State.Content(
            items = RwaInterval.entries.map { interval ->
                TangemSegmentedControl.Item(
                    id = interval.name,
                    label = interval.title,
                    isSelected = interval == selected,
                    onClick = { selectInterval(interval) },
                )
            }.toImmutableList(),
        )
    }

    private fun selectInterval(interval: RwaInterval) {
        if (uiState.value.selectedInterval == interval) return

        uiState.update { it.copy(selectedInterval = interval, interval = intervalControl(selected = interval)) }

        if (uiState.value.selectedSort == RwaSort.MarketCap) {
            // ByRating does not depend on the interval, so re-render what is loaded and fill the charts in
            syncListRequest()
            listManager.updateUIWithSameState()
            listManager.loadCharts(listManager.getBatchKeysByItemIds(visibleItemIds.value), interval)
        } else {
            // the gainers/losers orders are ranked by the interval, so the rows themselves change
            reloadList()
        }
    }

    private fun selectSort(index: Int) {
        val sort = RwaSort.entries.getOrNull(index) ?: return
        if (uiState.value.selectedSort == sort) return

        uiState.update { it.copy(selectedSort = sort) }
        reloadList()
    }

    private fun openTokenDetails(id: CryptoCurrency.RawID) {
        val token = listManager.getTokenById(id) ?: featuredManager.getTokenById(id) ?: return
        router.push(
            route = FeedRoute.MarketsTokenDetails(
                token = token.toSerializableParam(),
                appCurrency = currentAppCurrency.value,
                shouldShowPortfolio = true,
                analyticsParams = FeedRoute.MarketsTokenDetails.AnalyticsParams(
                    blockchain = null,
                    source = AnalyticsParam.ScreensSources.Main.value,
                ),
            ),
        )
    }

    override fun onCategorySelect(selection: CategorySelection) {
        uiState.update {
            it.copy(appliedSelection = selection, categoriesFilter = categoriesFilterItem(selection = selection))
        }
        reloadList()
    }

    private fun CoroutineScope.loadQuotesWithTimer(timeMillis: Long) {
        launch {
            while (true) {
                delay(timeMillis)
                containerBottomSheetState.first { it == BottomSheetState.EXPANDED }
                isVisibleOnScreen.first { it }
                listManager.updateQuotes()
            }
        }.saveIn(updateQuotesJob)
    }

    /**
     * Outcome of the tab's categories fetch. [rwaCategory] doubles as the "categories are resolved" flag —
     * while it is null a reload has to re-run the fetch before it can request anything.
     */
    private data class CategoriesState(
        val rwaCategory: CategorySelection.Entry? = null,
        val featuredCategoryId: String? = null,
        val isFailed: Boolean = false,
    )
}