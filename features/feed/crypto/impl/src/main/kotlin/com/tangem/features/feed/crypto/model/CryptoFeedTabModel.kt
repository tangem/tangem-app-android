package com.tangem.features.feed.crypto.model

import arrow.core.getOrElse
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.core.ui.ds2.filter.TangemFilterItemUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.GetMarketIndicatorsUseCase
import com.tangem.domain.markets.GetMarketsTokenListFlowUseCase
import com.tangem.domain.markets.MarketIndicators
import com.tangem.domain.markets.toSerializableParam
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.feed.crypto.categories.MarketCategoryFilterController
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
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import com.tangem.utils.logging.TangemLogger
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val UPDATE_QUOTES_TIMER_MILLIS = 60_000L

@Suppress("LongParameterList")
@ModelScoped
internal class CryptoFeedTabModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    getMarketsTokenListFlowUseCase: GetMarketsTokenListFlowUseCase,
    getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val getMarketIndicatorsUseCase: GetMarketIndicatorsUseCase,
    private val categoryFilterController: MarketCategoryFilterController,
    private val router: Router,
) : Model() {

    /** Expanded/collapsed state of the hosting shtorka — quotes refresh only while expanded. */
    val containerBottomSheetState = MutableStateFlow(BottomSheetState.COLLAPSED)
    val isVisibleOnScreen = MutableStateFlow(false)
    val bottomSheetNavigation: SlotNavigation<FeedCryptoTabBottomSheetConfig> = SlotNavigation()

    private val updateQuotesJob = JobHolder()
    private val marketIndicatorsJob = JobHolder()

    /** Last loaded indicators, kept so a currency switch can re-format them without a refetch */
    private val marketIndicators = MutableStateFlow<MarketIndicators?>(null)

    private val currentAppCurrency = getSelectedAppCurrencyUseCase().map { maybeAppCurrency ->
        maybeAppCurrency.getOrElse { AppCurrency.Default }
    }.stateIn(
        scope = modelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppCurrency.Default,
    )

    private val selectedInterval = MutableStateFlow(MarketPulseInterval.H24)
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

        combine(
            flow = marketIndicators.filterNotNull(),
            flow2 = currentAppCurrency,
        ) { indicators, appCurrency ->
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
        selectedCategory.drop(1)
            .onEach { listManager.reload() }
            .launchIn(modelScope)

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

        listManager.reload()
        fetchMarketIndicators()
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
                    onOptionClick = ::selectInterval,
                ),
                categories = MarketPulseCategory.entries
                    .map { interval -> MarketPulseCategoryUM(id = interval.id, title = interval.categoryTitle()) }
                    .toImmutableList(),
                selectedCategoryIndex = 0,
                onCategorySelect = ::selectCategory,
                list = MarketPulseListUM.Loading,
                onVisibleItemsChange = { ids -> visibleItemIds.value = ids.map(CryptoCurrency::RawID) },
            ),
        )
    }

    private fun selectCategory(index: Int) {
        val category = MarketPulseCategory.entries.getOrNull(index) ?: return
        updateMarketPulse { it.copy(selectedCategoryIndex = index) }
        selectedCategory.value = category
    }

    private fun applyCategorySelection(selection: MarketCategorySelection) {
        updateMarketPulse { it.copy(categoriesFilter = categoriesFilterItem(selection)) }
        listManager.reload()
    }

    private fun openCategorySelector() {
        router.push(route = MarketCategorySelectorRoute)
    }

    private fun categoriesFilterItem(selection: MarketCategorySelection): TangemFilterItemUM {
        val category = selection.category
            ?: return TangemFilterItemUM.Inactive(
                id = "crypto_categories",
                label = resourceReference(R.string.market_categories),
                onClick = ::openCategorySelector,
            )

        return TangemFilterItemUM.Active(
            id = category.id,
            value = stringReference(category.name),
            onClick = ::openCategorySelector,
            onClearClick = { categoryFilterController.onSelectionApplied(MarketCategorySelection()) },
        )
    }

    private fun selectInterval(interval: MarketPulseInterval) {
        selectedInterval.value = interval
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
            getMarketIndicatorsUseCase(appCurrency = currentAppCurrency.value)
                .onRight { indicators -> marketIndicators.value = indicators }
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