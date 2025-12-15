package com.tangem.features.feed.model.feed

import androidx.compose.runtime.Stable
import arrow.core.getOrElse
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.utils.DateTimeFormatters
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.GetTopFiveMarketTokenUseCase
import com.tangem.domain.markets.TokenMarketListConfig
import com.tangem.domain.news.usecase.FetchTrendingNewsUseCase
import com.tangem.domain.news.usecase.ManageTrendingNewsUseCase
import com.tangem.features.feed.impl.R
import com.tangem.features.feed.ui.feed.state.*
import com.tangem.features.feed.ui.market.state.SortByTypeUM
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentHashMap
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import javax.inject.Inject

@Stable
@ModelScoped
internal class FeedComponentModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val fetchTrendingNewsUseCase: FetchTrendingNewsUseCase,
    private val manageTrendingNewsUseCase: ManageTrendingNewsUseCase,
    getTopFiveMarketTokenUseCase: GetTopFiveMarketTokenUseCase,
    getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
) : Model() {

    private val _state = MutableStateFlow(initialState())
    val state = _state.asStateFlow()

    private var quotesUpdateJob: Job? = null

    private val currentAppCurrency = getSelectedAppCurrencyUseCase().map { maybeAppCurrency ->
        maybeAppCurrency.getOrElse { AppCurrency.Default }
    }.stateIn(
        scope = modelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppCurrency.Default,
    )

    private val marketsBatchFlowManager = FeedMarketsBatchFlowManager(
        getTopFiveMarketTokenUseCase = getTopFiveMarketTokenUseCase,
        currentAppCurrency = Provider { currentAppCurrency.value },
        modelScope = modelScope,
        dispatchers = dispatchers,
    )

    private val searchBarStateFactory by lazy(LazyThreadSafetyMode.NONE) {
        SearchBarStateFactory(
            currentStateProvider = Provider { _state.value },
            onStateUpdate = { newState -> _state.update { newState } },
        )
    }

    private val trendingNewsStateFactory by lazy(LazyThreadSafetyMode.NONE) {
        TrendingNewsStateFactory(
            currentStateProvider = Provider { _state.value },
            onStateUpdate = { newState -> _state.update { newState } },
        )
    }

    init {
        modelScope.launch(dispatchers.default) {
            fetchTrendingNewsUseCase()
            subscribeOnTrendingNews()
        }
        _state.update { feedListUM ->
            feedListUM.copy(
                searchBar = _state.value.searchBar.copy(onQueryChange = searchBarStateFactory::onSearchQueryChange),
                feedListCallbacks = feedListUM.feedListCallbacks.copy(
                    onSortTypeClick = ::onSortTypeClick,
                ),
            )
        }

        modelScope.launch(dispatchers.default) {
            combine(
                marketsBatchFlowManager.itemsByOrder,
                marketsBatchFlowManager.loadingStatesByOrder,
                marketsBatchFlowManager.errorStatesByOrder,
            ) { itemsByOrder, loadingStatesByOrder, errorStatesByOrder ->
                updateMarketCharts(itemsByOrder, loadingStatesByOrder, errorStatesByOrder)
                val currentSortType = _state.value.marketChartConfig.currentSortByType
                val items = itemsByOrder[currentSortType]
                val isLoading = loadingStatesByOrder[currentSortType] == true
                if (items != null && items.isNotEmpty() && !isLoading) {
                    val order = currentSortType.toOrder()
                    marketsBatchFlowManager.loadCharts(order)
                }
            }.collect()
        }

        modelScope.launch(dispatchers.default) {
            currentAppCurrency.drop(1).collect {
                marketsBatchFlowManager.reloadAll()
            }
        }

        modelScope.launch(dispatchers.default) {
            TokenMarketListConfig.Order.entries.forEach { order ->
                marketsBatchFlowManager.getOnLastBatchLoadedSuccessFlow(order)?.collect { batchKey ->
                    marketsBatchFlowManager.loadCharts(order)
                    if (batchKey == 0) {
                        startQuotesUpdateTimer()
                    }
                }
            }
        }
    }

    private suspend fun subscribeOnTrendingNews() {
        manageTrendingNewsUseCase().collect { articles ->
            trendingNewsStateFactory.updateTrendingNewsState(articles)
        }
    }

    private fun initialState(): FeedListUM {
        return FeedListUM(
            currentDate = getCurrentDate(),
            searchBar = SearchBarUM(
                placeholderText = resourceReference(R.string.markets_search_header_title),
                query = "",
                onQueryChange = {},
                isActive = false,
                onActiveChange = { },
            ),
            feedListCallbacks = FeedListCallbacks(
                onSearchClick = {},
                onMarketOpenClick = {},
                onArticleClick = {},
                onOpenAllNews = {},
                onMarketItemClick = {},
                onSortTypeClick = {},
            ),
            news = NewsUM.Loading,
            trendingArticle = null,
            marketChartConfig = MarketChartConfig(
                marketCharts = buildMap {
                    SortByTypeUM.entries.forEach {
                        put(it, MarketChartUM.LoadingError(onRetryClicked = marketsBatchFlowManager::reloadAll))
                    }
                }.toPersistentHashMap(),
                currentSortByType = SortByTypeUM.Trending,
            ),
        )
    }

    private fun getCurrentDate(): String {
        val localDate = DateTime(DateTime.now(), DateTimeZone.getDefault())
        return DateTimeFormatters.formatDate(formatter = DateTimeFormatters.dateDMMM, date = localDate)
    }

    private fun updateMarketCharts(
        itemsByOrder: Map<SortByTypeUM, ImmutableList<com.tangem.common.ui.markets.models.MarketsListItemUM>>,
        loadingStatesByOrder: Map<SortByTypeUM, Boolean>,
        errorStatesByOrder: Map<SortByTypeUM, Boolean>,
    ) {
        _state.update { currentState ->
            val newMarketCharts = buildMap {
                SortByTypeUM.entries.forEach { sortByType ->
                    val items = itemsByOrder[sortByType] ?: persistentListOf()
                    val isLoading = loadingStatesByOrder[sortByType] == true
                    val hasError = errorStatesByOrder[sortByType] == true

                    when {
                        hasError -> {
                            put(
                                sortByType,
                                MarketChartUM.LoadingError(
                                    onRetryClicked = {
                                        marketsBatchFlowManager.reloadAll()
                                    },
                                ),
                            )
                        }
                        isLoading -> {
                            put(sortByType, MarketChartUM.Loading)
                        }
                        items.isEmpty() -> {
                            put(
                                sortByType,
                                MarketChartUM.LoadingError(
                                    onRetryClicked = {
                                        marketsBatchFlowManager.reloadAll()
                                    },
                                ),
                            )
                        }
                        else -> {
                            put(
                                sortByType,
                                MarketChartUM.Content(
                                    items = items,
                                    sortChartConfig = SortChartConfigUM(
                                        sortByType = sortByType,
                                        isSelected = sortByType == currentState.marketChartConfig.currentSortByType,
                                    ),
                                ),
                            )
                        }
                    }
                }
            }.toPersistentHashMap()

            currentState.copy(
                marketChartConfig = currentState.marketChartConfig.copy(
                    marketCharts = newMarketCharts,
                ),
            )
        }
    }

    private fun onSortTypeClick(sortByType: SortByTypeUM) {
        _state.update { currentState ->
            val updatedCharts = currentState.marketChartConfig.marketCharts.mapValues { (chartSortType, chart) ->
                when (chart) {
                    is MarketChartUM.Content -> {
                        chart.copy(
                            sortChartConfig = chart.sortChartConfig.copy(
                                isSelected = chartSortType == sortByType,
                            ),
                        )
                    }
                    else -> chart
                }
            }

            currentState.copy(
                marketChartConfig = currentState.marketChartConfig.copy(
                    currentSortByType = sortByType,
                    marketCharts = updatedCharts.toPersistentHashMap(),
                ),
            )
        }
        modelScope.launch(dispatchers.default) {
            marketsBatchFlowManager.loadCharts(sortByType.toOrder())
        }
    }

    private fun startQuotesUpdateTimer() {
        quotesUpdateJob?.cancel()
        quotesUpdateJob = modelScope.launch {
            while (true) {
                delay(DELAY_TO_FETCH_QUOTES)
                marketsBatchFlowManager.updateQuotes()
            }
        }
    }

    private fun SortByTypeUM.toOrder(): TokenMarketListConfig.Order {
        return when (this) {
            SortByTypeUM.Rating -> TokenMarketListConfig.Order.ByRating
            SortByTypeUM.Trending -> TokenMarketListConfig.Order.Trending
            SortByTypeUM.ExperiencedBuyers -> TokenMarketListConfig.Order.Buyers
            SortByTypeUM.TopGainers -> TokenMarketListConfig.Order.TopGainers
            SortByTypeUM.TopLosers -> TokenMarketListConfig.Order.TopLosers
            SortByTypeUM.Staking -> TokenMarketListConfig.Order.Staking
        }
    }

    companion object {
        private const val DELAY_TO_FETCH_QUOTES = 60_000L
    }
}