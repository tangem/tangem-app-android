package com.tangem.features.feed.crypto.model

import arrow.core.getOrElse
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.core.ui.ds2.filter.TangemFilterItemUM
import com.tangem.core.ui.ds2.tabnavigation.TangemTabItemUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.GetMarketIndicatorsUseCase
import com.tangem.domain.markets.GetMarketsTokenListFlowUseCase
import com.tangem.domain.markets.MarketIndicators
import com.tangem.domain.markets.toSerializableParam
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.feed.analytics.FeedSectionAnalyticsEvent
import com.tangem.features.feed.crypto.categories.MarketCategoryFilterController
import com.tangem.features.feed.crypto.categories.MarketCategoryResolver
import com.tangem.features.feed.crypto.categories.MarketCategorySelection
import com.tangem.features.feed.crypto.categories.MarketCategorySelectorRoute
import com.tangem.features.feed.crypto.impl.R
import com.tangem.features.feed.crypto.model.converter.TotalMarketCapConverter
import com.tangem.features.feed.crypto.model.entity.FeedCryptoTabBottomSheetConfig
import com.tangem.features.feed.crypto.model.list.MarketPulseBatchFlowManager
import com.tangem.features.feed.crypto.model.list.MarketPulseCategory
import com.tangem.features.feed.crypto.model.list.MarketPulseInterval
import com.tangem.features.feed.crypto.ui.state.*
import com.tangem.features.feed.nav.FeedRoute
import com.tangem.features.feed.nav.FeedTabId
import com.tangem.features.feed.nav.analyticsSection
import com.tangem.features.feed.v2.FeedEntryController
import com.tangem.features.feed.v2.FeedEntryFilter
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import com.tangem.utils.logging.TangemLogger
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
 * the same across `Inactive` and `Active` for the chip to crossfade between the two instead of being
 * rebuilt as a different item.
 */
private const val CATEGORIES_FILTER_ID = "crypto_categories"

private val DEFAULT_INTERVAL = MarketPulseInterval.H24

private data class MarketIndicatorsSnapshot(val indicators: MarketIndicators, val appCurrency: AppCurrency)

@Suppress("LongParameterList")
@ModelScoped
internal class CryptoFeedTabModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    getMarketsTokenListFlowUseCase: GetMarketsTokenListFlowUseCase,
    getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val getMarketIndicatorsUseCase: GetMarketIndicatorsUseCase,
    private val categoryFilterController: MarketCategoryFilterController,
    private val categoryResolver: MarketCategoryResolver,
    private val feedEntryController: FeedEntryController,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val router: Router,
    private val urlOpener: UrlOpener,
) : Model() {

    /** Expanded/collapsed state of the hosting shtorka — quotes refresh only while expanded. */
    val containerBottomSheetState = MutableStateFlow(BottomSheetState.COLLAPSED)
    val isVisibleOnScreen = MutableStateFlow(false)
    val bottomSheetNavigation: SlotNavigation<FeedCryptoTabBottomSheetConfig> = SlotNavigation()

    /**
     * Read synchronously at construction, before the first list request is issued: with a category
     * deeplink already pending, loading unfiltered first would show the wrong list and then swap it.
     */
    private var isAwaitingDeeplinkCategory: Boolean = feedEntryController.pendingFilter.value
        ?.let { it.tabId == FeedTabId.Crypto && it.categoryId != null } == true

    /**
     * Selection this tab published from a deeplink, until the controller hands it back. The selector
     * screen publishes through the same seam, so nothing but the value tells the two apart.
     */
    private var deeplinkSelection: MarketCategorySelection? = null

    private val updateQuotesJob = JobHolder()
    private val marketIndicatorsJob = JobHolder()

    /**
     * Last loaded indicators together with the currency they were requested with. The capitalisation is
     * converted server-side, so the amount is only meaningful next to that currency — pairing it with the
     * currency selected *now* would render the previous currency's figure under the new symbol.
     */
    private val marketIndicators = MutableStateFlow<MarketIndicatorsSnapshot?>(null)

    private val currentAppCurrency = getSelectedAppCurrencyUseCase().map { maybeAppCurrency ->
        maybeAppCurrency.getOrElse { AppCurrency.Default }
    }.stateIn(
        scope = modelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppCurrency.Default,
    )

    private val selectedInterval = MutableStateFlow(DEFAULT_INTERVAL)
    private val selectedCategory = MutableStateFlow(MarketPulseCategory.MarketCap)
    private val visibleItemIds = MutableStateFlow<List<CryptoCurrency.RawID>>(emptyList())

    private val listManager = MarketPulseBatchFlowManager(
        getMarketsTokenListFlowUseCase = getMarketsTokenListFlowUseCase,
        currentTrendInterval = { selectedInterval.value },
        currentAppCurrency = { currentAppCurrency.value },
        currentCategory = { selectedCategory.value },
        currentCategoryFilter = { categoryFilterController.selection.value },
        onItemClick = ::openTokenDetails,
        modelScope = modelScope,
        dispatchers = dispatchers,
    )

    val uiState: StateFlow<CryptoFeedTabUM>
        field = MutableStateFlow(initialState())

    init {
        combine(
            flow = listManager.uiItems,
            flow2 = listManager.isInInitialLoadingErrorState,
        ) { items, isInErrorState ->
            when {
                isInErrorState -> MarketPulseListUM.Error(onRetry = { listManager.reload() })
                items.isEmpty() -> MarketPulseListUM.Loading
                else -> MarketPulseListUM.Content(items = items, loadMore = listManager::loadMore)
            }
        }
            .distinctUntilChanged()
            .onEach { list -> updateMarketPulse { it.copy(list = list) } }
            .launchIn(modelScope)

        marketIndicators.filterNotNull()
            .map { (indicators, appCurrency) ->
                TotalMarketCapConverter(
                    appCurrency = appCurrency,
                    onIndexClick = ::showIndexInfo,
                    onInfoClick = ::onInfoClick,
                ).convert(indicators)
            }
            .distinctUntilChanged()
            .onEach { totalMarketCap -> uiState.update { it.copy(totalMarketCap = totalMarketCap) } }
            .launchIn(modelScope)

        // reload the list when the user's currency changes
        currentAppCurrency.drop(1)
            .onEach { loadData() }
            .launchIn(modelScope)

        // load charts for a freshly loaded batch and keep quotes fresh
        listManager.onLastBatchLoadedSuccess.onEach { batchKey ->
            listManager.loadCharts(setOf(batchKey), selectedInterval.value)
            modelScope.loadQuotesWithTimer(timeMillis = UPDATE_QUOTES_TIMER_MILLIS)
        }.launchIn(modelScope)

        // interval switch: rating order only re-renders + fetches missing charts, others re-sort
        selectedInterval.drop(1).onEach { interval ->
            updateMarketPulse { it.copy(intervalMenu = it.intervalMenu.copy(selected = interval)) }
            if (selectedCategory.value == MarketPulseCategory.MarketCap) {
                listManager.updateUIWithSameState()
                listManager.loadCharts(listManager.getBatchKeysByItemIds(visibleItemIds.value), interval)
            } else {
                listManager.reload()
            }
        }.launchIn(modelScope)

        // category = sort order, so a switch always reloads
        selectedCategory.drop(1).onEach { category ->
            updateMarketPulse { it.copy(sortTabs = sortTabs(selected = category)) }
            listManager.reload()
        }.launchIn(modelScope)

        // the selector screen publishes here; drop(1) skips the value initialState already rendered
        categoryFilterController.selection.drop(1)
            .onEach(::applyCategorySelection)
            .launchIn(modelScope)

        // fetch charts for the batches that become visible while scrolling
        visibleItemIds
            .mapNotNull { ids -> ids.takeIf { it.isNotEmpty() }?.let(listManager::getBatchKeysByItemIds) }
            .distinctUntilChanged()
            .onEach { visibleBatchKeys -> listManager.loadCharts(visibleBatchKeys, selectedInterval.value) }
            .launchIn(modelScope)

        feedEntryController.pendingFilter
            .filterNotNull()
            .filter { it.tabId == FeedTabId.Crypto }
            .onEach(::onDeeplinkFilter)
            .launchIn(modelScope)

        if (!isAwaitingDeeplinkCategory) listManager.reload()
        fetchMarketIndicators()
    }

    private suspend fun onDeeplinkFilter(filter: FeedEntryFilter) {
        feedEntryController.consumeFilter()

        val selection = filter.categoryId
            ?.let { resolveCategory(categoryId = it) }
            ?.let { MarketCategorySelection(category = it) }

        if (selection != null && selection != categoryFilterController.selection.value) {
            // applying it drives both the chip and the reload, through the controller's own collector
            deeplinkSelection = selection
            categoryFilterController.onSelectionApplied(selection)
        } else if (isAwaitingDeeplinkCategory) {
            // nothing to apply, or it is already applied and so will not re-emit: init skipped the
            // first load waiting for this, so issue it now
            listManager.reload()
        }

        isAwaitingDeeplinkCategory = false
    }

    private suspend fun resolveCategory(categoryId: String): MarketCategorySelection.Entry? {
        val entry = categoryResolver.resolve(categoryId = categoryId, currency = currentAppCurrency.value.code)

        if (entry == null) TangemLogger.i("Discover deeplink: no crypto category matches '$categoryId'")

        return entry
    }

    private fun loadData() {
        listManager.reload()
        fetchMarketIndicators()
    }

    private fun initialState(): CryptoFeedTabUM {
        return CryptoFeedTabUM(
            totalMarketCap = TotalMarketCapUM.Loading,
            marketPulse = MarketPulseUM(
                title = resourceReference(R.string.markets_pulse_common_title),
                categoriesFilter = categoriesFilterItem(categoryFilterController.selection.value),
                intervalMenu = MarketPulseIntervalUM(
                    selected = selectedInterval.value,
                    default = DEFAULT_INTERVAL,
                    onOptionClick = ::selectInterval,
                ),
                sortTabs = sortTabs(selected = selectedCategory.value),
                list = MarketPulseListUM.Loading,
                onVisibleItemsChange = { ids -> visibleItemIds.value = ids.map(CryptoCurrency::RawID) },
            ),
        )
    }

    // reported from the user's own picks only: every tab model is built when the feed opens, and a
    // category filter can reset the sort on its own
    private fun selectCategory(category: MarketPulseCategory) {
        if (selectedCategory.value == category) return
        selectedCategory.value = category
        sendSortByAnalytics(category, selectedInterval.value)
    }

    private fun sortTabs(
        selected: MarketPulseCategory,
        filter: MarketCategorySelection = categoryFilterController.selection.value,
    ): ImmutableList<TangemTabItemUM> {
        return availableCategories(filter)
            .map<MarketPulseCategory, TangemTabItemUM> { category ->
                TangemTabItemUM.Content(
                    id = category.id,
                    label = category.categoryTitle(),
                    isSelected = category == selected,
                    onClick = { selectCategory(category) },
                )
            }
            .toImmutableList()
    }

    /** The backend ranks buyers across the whole market only: under a CoinGecko category it returns nothing. */
    private fun availableCategories(filter: MarketCategorySelection): List<MarketPulseCategory> {
        return MarketPulseCategory.entries.filter { category ->
            category != MarketPulseCategory.ExperiencedBuyers || filter.category == null
        }
    }

    private fun applyCategorySelection(selection: MarketCategorySelection) {
        // `Select Category` describes a pick the user made; a deeplink arrives already filtered
        val isFromDeeplink = selection == deeplinkSelection
        deeplinkSelection = null

        if (!isFromDeeplink) {
            analyticsEventHandler.send(
                FeedSectionAnalyticsEvent.SelectCategory(
                    section = FeedTabId.Crypto.analyticsSection,
                    categoryId = selection.category?.id ?: FeedSectionAnalyticsEvent.ALL_CATEGORIES,
                ),
            )
        }
        updateMarketPulse { it.copy(categoriesFilter = categoriesFilterItem(selection)) }

        if (selectedCategory.value in availableCategories(selection)) {
            updateMarketPulse { it.copy(sortTabs = sortTabs(selected = selectedCategory.value, filter = selection)) }
            listManager.reload()
        } else {
            // its collector re-renders the tabs and reloads the list
            selectedCategory.value = MarketPulseCategory.MarketCap
        }
    }

    private fun sendSortByAnalytics(category: MarketPulseCategory, interval: MarketPulseInterval) {
        analyticsEventHandler.send(
            FeedSectionAnalyticsEvent.SortBy(
                section = FeedTabId.Crypto.analyticsSection,
                type = category.toAnalyticsType(),
                period = interval.toAnalyticsPeriod(),
            ),
        )
    }

    private fun openCategorySelector() {
        router.push(route = MarketCategorySelectorRoute)
    }

    private fun categoriesFilterItem(selection: MarketCategorySelection): TangemFilterItemUM {
        val category = selection.category
            ?: return TangemFilterItemUM.Inactive(
                id = CATEGORIES_FILTER_ID,
                label = resourceReference(R.string.market_categories),
                onClick = ::openCategorySelector,
            )

        return TangemFilterItemUM.Active(
            id = CATEGORIES_FILTER_ID,
            value = stringReference(category.name),
            onClick = ::openCategorySelector,
            onClearClick = { categoryFilterController.onSelectionApplied(MarketCategorySelection()) },
        )
    }

    private fun selectInterval(interval: MarketPulseInterval) {
        if (selectedInterval.value == interval) return
        selectedInterval.value = interval
        sendSortByAnalytics(selectedCategory.value, interval)
    }

    fun openUrl(url: String) {
        urlOpener.openUrl(url)
    }

    override fun onDestroy() {
        super.onDestroy()
        // the controller outlives the tab, so a reopened feed would otherwise start pre-filtered
        categoryFilterController.clear()
    }

    private inline fun updateMarketPulse(block: (MarketPulseUM) -> MarketPulseUM) {
        uiState.update { state -> state.copy(marketPulse = block(state.marketPulse)) }
    }

    private fun openTokenDetails(id: CryptoCurrency.RawID) {
        val token = listManager.getTokenById(id) ?: return
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

    private fun CoroutineScope.loadQuotesWithTimer(timeMillis: Long) {
        launch {
            while (true) {
                delay(timeMillis)
                // update quotes only when the shtorka is expanded and the tab is visible
                containerBottomSheetState.first { it == BottomSheetState.EXPANDED }
                isVisibleOnScreen.first { it }
                listManager.updateQuotes()
                fetchMarketIndicators()
            }
        }.saveIn(updateQuotesJob)
    }

    private fun fetchMarketIndicators() {
        modelScope.launch {
            val appCurrency = currentAppCurrency.value

            getMarketIndicatorsUseCase(appCurrency = appCurrency)
                .onRight { indicators ->
                    marketIndicators.value = MarketIndicatorsSnapshot(
                        indicators = indicators,
                        appCurrency = appCurrency,
                    )
                }
                .onLeft(::onMarketIndicatorsFailure)
        }.saveIn(marketIndicatorsJob)
    }

    private fun showIndexInfo(index: MarketIndexUM) {
        bottomSheetNavigation.activate(
            FeedCryptoTabBottomSheetConfig.MarketIndexInfo(index),
        )
    }

    /** A refresh failure keeps the readings already on screen; only a failed first load empties the block */
    private fun onMarketIndicatorsFailure(error: Throwable) {
        TangemLogger.e("Failed to load market indicators", error)

        uiState.update { state ->
            if (state.totalMarketCap is TotalMarketCapUM.Loading) {
                state.copy(totalMarketCap = TotalMarketCapUM.Empty)
            } else {
                state
            }
        }
    }

    private fun onInfoClick() {
        bottomSheetNavigation.activate(
            FeedCryptoTabBottomSheetConfig.MarketInfo,
        )
    }
}