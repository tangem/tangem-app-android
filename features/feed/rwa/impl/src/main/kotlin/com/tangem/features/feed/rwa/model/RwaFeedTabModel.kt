package com.tangem.features.feed.rwa.model

import arrow.core.getOrElse
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.R
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.core.ui.ds2.filter.TangemFilterItemUM
import com.tangem.core.ui.ds2.tabnavigation.TangemTabItemUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.*
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.feed.analytics.FeedSectionAnalyticsEvent
import com.tangem.features.feed.nav.FeedRoute
import com.tangem.features.feed.nav.FeedTabId
import com.tangem.features.feed.nav.analyticsSection
import com.tangem.features.feed.rwa.categories.CategorySelection
import com.tangem.features.feed.rwa.categories.CategorySelectorRoute
import com.tangem.features.feed.rwa.categories.DeeplinkCategoryFilter
import com.tangem.features.feed.rwa.categories.ReservedCategoryCodes
import com.tangem.features.feed.rwa.categories.RwaCategoryNames
import com.tangem.features.feed.rwa.categories.RwaDefaultCategory
import com.tangem.features.feed.rwa.categories.RwaRegionRestriction
import com.tangem.features.feed.rwa.categories.components.CategorySelectorComponent
import com.tangem.features.feed.rwa.model.analytics.RwaAnalyticsEvent
import com.tangem.features.feed.rwa.model.list.RwaBatchFlowManager
import com.tangem.features.feed.rwa.model.list.RwaFeaturedAssetsManager
import com.tangem.features.feed.rwa.model.list.RwaInterval
import com.tangem.features.feed.rwa.model.list.RwaListRequest
import com.tangem.features.feed.rwa.model.list.RwaSort
import com.tangem.features.feed.rwa.ui.state.RwaFeaturedUM
import com.tangem.features.feed.rwa.ui.state.RwaFeedTabUM
import com.tangem.features.feed.rwa.ui.state.RwaIntervalUM
import com.tangem.features.feed.rwa.ui.state.RwaListUM
import com.tangem.features.feed.v2.FeedEntryController
import com.tangem.features.feed.v2.FeedEntryFilter
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.collections.immutable.ImmutableList
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

private val DEFAULT_INTERVAL = RwaInterval.H24

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
@Suppress("LongParameterList", "LargeClass")
internal class RwaFeedTabModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    getMarketsTokenListFlowUseCase: GetMarketsTokenListFlowUseCase,
    getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val getCoinCategoriesUseCase: GetCoinCategoriesUseCase,
    private val userCountryResolver: UserCountryResolver,
    private val feedEntryController: FeedEntryController,
    private val analyticsEventHandler: AnalyticsEventHandler,
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

    private val deeplinkCategory = DeeplinkCategoryFilter()

    /**
     * Source of truth for the trend interval and the sort; conflation is what makes re-picking the current
     * value a no-op, so neither setter needs an equality guard.
     */
    private val selectedInterval = MutableStateFlow(DEFAULT_INTERVAL)
    private val selectedSort = MutableStateFlow(RwaSort.MarketCap)

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
        interval = selectedInterval,
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
            listManager.loadCharts(setOf(batchKey), selectedInterval.value)
            modelScope.loadQuotesWithTimer(timeMillis = UPDATE_QUOTES_TIMER_MILLIS)
        }.launchIn(modelScope)

        visibleItemIds
            .mapNotNull { ids -> ids.takeIf { it.isNotEmpty() }?.let(listManager::getBatchKeysByItemIds) }
            .distinctUntilChanged()
            .onEach { visibleBatchKeys -> listManager.loadCharts(visibleBatchKeys, selectedInterval.value) }
            .launchIn(modelScope)

        // interval switch: the market-cap order does not depend on it, so re-render and fill the charts in;
        // the gainers/losers orders are ranked by it, so the row set itself changes
        selectedInterval.drop(1).onEach { interval ->
            uiState.update { it.copy(interval = it.interval.copy(selected = interval)) }

            if (selectedSort.value == RwaSort.MarketCap) {
                syncListRequest()
                listManager.updateUIWithSameState()
                listManager.loadCharts(listManager.getBatchKeysByItemIds(visibleItemIds.value), interval)
            } else {
                reloadList()
            }
        }.launchIn(modelScope)

        // the sort *is* the query, so a switch always reloads
        selectedSort.drop(1).onEach { sort ->
            uiState.update { it.copy(sortTabs = sortTabs(selected = sort)) }
            reloadList()
        }.launchIn(modelScope)

        // every tab model is built when the feed opens, so only a real user change may report
        combine(selectedSort, selectedInterval, ::Pair)
            .drop(1)
            .distinctUntilChanged()
            .onEach { (sort, interval) -> sendSortByAnalytics(sort, interval) }
            .launchIn(modelScope)

        featuredManager.state
            .onEach { featured -> uiState.update { it.copy(featured = featured) } }
            .launchIn(modelScope)

        feedEntryController.pendingFilter
            .filterNotNull()
            .filter { it.tabId == FeedTabId.RealAssets }
            .onEach(::onDeeplinkFilter)
            .launchIn(modelScope)

        loadCategories()
    }

    private fun onDeeplinkFilter(filter: FeedEntryFilter) {
        feedEntryController.consumeFilter()
        deeplinkCategory.park(categoryId = filter.categoryId)

        // still loading: onCategoriesLoaded applies the parked id as soon as the tree arrives
        val categories = categoriesState.value.all
        if (categories.isNotEmpty()) deeplinkCategory.take(categories)?.let(::applySelection)
    }

    private fun initialState(): RwaFeedTabUM {
        val request = RwaListRequest()

        return RwaFeedTabUM(
            featured = RwaFeaturedUM.Loading,
            categoriesFilter = categoriesFilterItem(selection = request.selection),
            interval = RwaIntervalUM(
                selected = selectedInterval.value,
                default = DEFAULT_INTERVAL,
                onOptionClick = ::selectInterval,
            ),
            sortTabs = sortTabs(selected = selectedSort.value),
            onVisibleItemsChange = { ids -> visibleItemIds.value = ids.map(CryptoCurrency::RawID) },
            list = RwaListUM.Loading,
            appliedSelection = request.selection,
            regionRestriction = null,
        )
    }

    private fun loadCategories() {
        categoriesState.update { it.copy(isFailed = false) }

        modelScope.launch {
            val country = userCountryResolver.resolve()
            categoriesState.update { it.copy(country = country) }

            getCoinCategoriesUseCase(country = country).fold(
                ifLeft = { _ ->
                    uiState.update { it.copy(featured = RwaFeaturedUM.Empty) }
                    categoriesState.update { it.copy(isFailed = true) }
                },
                ifRight = { categories -> onCategoriesLoaded(categories = categories, country = country) },
            )
        }
    }

    private fun onCategoriesLoaded(categories: List<CoinCategory>, country: String?) {
        val featured = categories.firstOrNull { it.code == ReservedCategoryCodes.FEATURED_ASSETS }

        if (featured == null) {
            uiState.update { it.copy(featured = RwaFeaturedUM.Empty) }
        } else {
            categoriesState.update { it.copy(featuredCategoryId = featured.id) }
            reloadFeatured()
        }

        categoriesState.update { it.copy(restrictedCategoryIds = RwaRegionRestriction.restrictedIds(categories)) }
        applyRegionRestriction(selection = uiState.value.appliedSelection)

        val umbrella = categories.firstOrNull { it.code == ReservedCategoryCodes.REAL_WORLD_ASSETS }

        if (umbrella == null) {
            categoriesState.update { it.copy(isFailed = true) }
            return
        }

        val entry = CategorySelection.Entry(
            id = umbrella.id,
            displayName = umbrella.displayName,
            code = umbrella.code,
        )
        categoriesState.update { it.copy(all = categories, rwaCategory = entry) }

        val deeplinkSelection = deeplinkCategory.take(categories = categories)

        if (deeplinkSelection != null) {
            // it outranks the regional default, which only ever picks for a user who has not chosen — and
            // asking for the umbrella is choosing too, though it leaves the selection itself empty
            categoriesState.update { it.copy(isDefaultApplied = true) }
            applySelection(deeplinkSelection)
            return
        }

        val default = defaultSelection(categories = categories, country = country)

        if (default == null) {
            reloadList()
        } else {
            onCategorySelect(selection = default)
        }
    }

    /** Applied once per model: a later reload must not overwrite whatever the user has picked since. */
    private fun defaultSelection(categories: List<CoinCategory>, country: String?): CategorySelection? {
        if (categoriesState.value.isDefaultApplied) return null

        // a load that went out without a country carries no restriction data, so spending the one-shot
        // on it would leave a restricted region unfiltered for the rest of the session
        if (country == null) return null

        categoriesState.update { it.copy(isDefaultApplied = true) }

        return RwaDefaultCategory.resolve(
            categories = categories,
            country = country,
            hasUserSelection = uiState.value.appliedSelection.category != null,
        )
    }

    private fun reloadFeatured() {
        val categoryId = categoriesState.value.featuredCategoryId ?: return
        featuredManager.load(categoryId = categoryId, country = categoriesState.value.country)
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
            interval = selectedInterval.value,
            sort = selectedSort.value,
            selection = state.appliedSelection.orUmbrella(categoriesState.value.rwaCategory),
            // without it every token comes back unrestricted, so the region marks never appear
            country = categoriesState.value.country,
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

        val sector = selection.sector

        return TangemFilterItemUM.Active(
            id = CATEGORIES_FILTER_ID,
            value = if (sector == null) {
                RwaCategoryNames.category(code = category.code, fallback = category.displayName)
            } else {
                RwaCategoryNames.sector(
                    categoryCode = category.code,
                    sectorCode = sector.code,
                    fallback = sector.displayName,
                )
            },
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

    private fun selectInterval(interval: RwaInterval) {
        selectedInterval.value = interval
    }

    private fun sendSortByAnalytics(sort: RwaSort, interval: RwaInterval) {
        analyticsEventHandler.send(
            FeedSectionAnalyticsEvent.SortBy(
                section = FeedTabId.RealAssets.analyticsSection,
                type = sort.toAnalyticsType(),
                period = interval.toAnalyticsPeriod(),
            ),
        )
    }

    private fun selectSort(sort: RwaSort) {
        selectedSort.value = sort
    }

    private fun sortTabs(selected: RwaSort): ImmutableList<TangemTabItemUM> = RwaSort.entries
        .map<RwaSort, TangemTabItemUM> { sort ->
            TangemTabItemUM.Content(
                id = sort.id,
                label = sort.title,
                isSelected = sort == selected,
                onClick = { selectSort(sort) },
            )
        }
        .toImmutableList()

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
        analyticsEventHandler.send(
            FeedSectionAnalyticsEvent.SelectCategory(
                section = FeedTabId.RealAssets.analyticsSection,
                categoryId = selection.category?.code ?: FeedSectionAnalyticsEvent.ALL_CATEGORIES,
            ),
        )
        applySelection(selection)
    }

    /** `Select Category` describes a pick the user made, so a deeplink arriving filtered is not one. */
    private fun applySelection(selection: CategorySelection) {
        uiState.update { state ->
            state.copy(
                appliedSelection = selection,
                categoriesFilter = categoriesFilterItem(selection = selection),
            )
        }
        applyRegionRestriction(selection = selection)
        reloadList()
    }

    /** The notice only ever accompanies a picked category, which the analytics event reports by name. */
    private fun applyRegionRestriction(selection: CategorySelection) {
        val banner = RwaRegionRestriction.banner(
            restrictedIds = categoriesState.value.restrictedCategoryIds,
            selection = selection,
            country = categoriesState.value.country,
        )
        uiState.update { it.copy(regionRestriction = banner) }

        val category = selection.category ?: return

        if (banner != null) {
            analyticsEventHandler.send(RwaAnalyticsEvent.RegionNotSupportedNotification(category.displayName))
        }
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
        /** The resolved tree, kept for the one filter that arrives against it later — a deeplink's category. */
        val all: List<CoinCategory> = emptyList(),
        val rwaCategory: CategorySelection.Entry? = null,
        val featuredCategoryId: String? = null,
        val isFailed: Boolean = false,
        val isDefaultApplied: Boolean = false,
        val country: String? = null,
        val restrictedCategoryIds: Set<String> = emptySet(),
    )
}