package com.tangem.features.feed.model.search

import arrow.core.getOrElse
import com.tangem.common.ui.markets.models.MarketsListItemUM
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.common.ui.charts.state.MarketChartData
import com.tangem.common.ui.charts.state.converter.PriceAndTimePointValuesConverter
import com.tangem.common.ui.charts.state.sorted
import com.tangem.domain.markets.GetMarketsTokenListFlowUseCase
import com.tangem.domain.markets.GetTokenPriceChartUseCase
import com.tangem.domain.markets.PriceChangeInterval
import com.tangem.domain.models.account.AccountName
import com.tangem.domain.search.usecase.ClearSearchHistoryUseCase
import com.tangem.domain.search.usecase.GetSearchResultsUseCase
import com.tangem.domain.search.usecase.SaveRecentSearchTokenUseCase
import com.tangem.domain.search.usecase.SaveSearchQueryUseCase
import com.tangem.features.feed.components.search.DefaultSearchComponent
import com.tangem.features.feed.model.market.list.state.MarketsListUM
import com.tangem.features.feed.model.market.list.state.SortByTypeUM
import com.tangem.features.feed.model.market.list.statemanager.MarketsListBatchFlowManager
import com.tangem.features.feed.model.search.converter.MarketsListItemUMToRecentSearchTokenConverter
import com.tangem.features.feed.model.search.converter.MarketsListItemUMWithAppCurrency
import com.tangem.features.feed.model.search.converter.RecentSearchTokenToMarketsListItemUMConverter
import com.tangem.features.feed.model.search.converter.RecentSearchTokenWithAppCurrency
import com.tangem.features.feed.model.search.state.SearchStateController
import com.tangem.features.feed.model.search.state.transformers.*
import com.tangem.features.feed.ui.search.state.*
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val UPDATE_QUOTES_TIMER_MILLIS = 60000L
private const val MARKET_SEARCH_DEBOUNCE_MS = 500L

@Suppress("LongParameterList")
@ModelScoped
internal class SearchModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
    getMarketsTokenListFlowUseCase: GetMarketsTokenListFlowUseCase,
    getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val getSearchResultsUseCase: GetSearchResultsUseCase,
    private val saveSearchQueryUseCase: SaveSearchQueryUseCase,
    private val saveRecentSearchTokenUseCase: SaveRecentSearchTokenUseCase,
    private val clearSearchHistoryUseCase: ClearSearchHistoryUseCase,
    private val getTokenPriceChartUseCase: GetTokenPriceChartUseCase,
    private val stateController: SearchStateController,
) : Model() {

    private val params = paramsContainer.require<DefaultSearchComponent.Params>()

    private val updateQuotesJob = JobHolder()
    private val searchResultsJob = JobHolder()
    private val marketSearchDebounceJob = JobHolder()
    private var shouldShowAllTokensIncludingUnder100k = false

    private val currentAppCurrency = getSelectedAppCurrencyUseCase().map { maybeAppCurrency ->
        maybeAppCurrency.getOrElse { AppCurrency.Default }
    }.stateIn(
        scope = modelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppCurrency.Default,
    )

    private val marketsListItemToRecentSearchTokenConverter by lazy {
        MarketsListItemUMToRecentSearchTokenConverter()
    }

    private val recentSearchTokenToMarketsListItemConverter by lazy {
        RecentSearchTokenToMarketsListItemUMConverter()
    }

    private val priceAndTimePointValuesConverter by lazy {
        PriceAndTimePointValuesConverter(shouldFormatAxis = false)
    }

    private val searchMarketsListManager by lazy {
        MarketsListBatchFlowManager(
            getMarketsTokenListFlowUseCase = getMarketsTokenListFlowUseCase,
            batchFlowType = GetMarketsTokenListFlowUseCase.BatchFlowType.Search,
            currentAppCurrency = Provider { currentAppCurrency.value },
            currentTrendInterval = Provider { MarketsListUM.TrendInterval.H24 },
            currentSortByType = Provider { SortByTypeUM.Rating },
            currentSearchText = Provider { stateController.value.searchBar.query },
            modelScope = modelScope,
            dispatchers = dispatchers,
        )
    }

    val state: StateFlow<SearchUM> get() = stateController.uiState

    init {
        initCallbacks()
        subscribeToQueryChanges()
        subscribeToMarketUiItems()
        subscribeToQuotesPolling()
        subscribeToAppCurrencyChanges()
        loadHistory()
    }

    fun loadMore() {
        val content = stateController.value.content
        if (content is SearchContentUM.Results) {
            val market = content.marketTokens
            if (market is MarketSearchResultUM.Content) {
                searchMarketsListManager.loadMore()
            }
        }
    }

    fun clearSearchHistory() {
        modelScope.launch(dispatchers.default) {
            clearSearchHistoryUseCase()
        }
    }

    fun onTextHintClick(text: String) {
        stateController.update(UpdateSearchBarQueryTransformer(text))
    }

    fun onResultMarketTokenClick(item: MarketsListItemUM) {
        modelScope.launch(dispatchers.default) {
            val appCurrency = currentAppCurrency.value
            val input = MarketsListItemUMWithAppCurrency(
                item = item,
                appCurrencyCode = appCurrency.code,
                appCurrencySymbol = appCurrency.symbol,
            )
            saveRecentSearchTokenUseCase(marketsListItemToRecentSearchTokenConverter.convert(input))
            saveSearchQueryUseCase(stateController.value.searchBar.query)
        }
    }

    private fun initCallbacks() {
        stateController.update(object : SearchUMTransformer {
            override fun transform(prevState: SearchUM): SearchUM {
                return prevState.copy(
                    searchBar = prevState.searchBar.copy(
                        onQueryChange = ::onQueryChange,
                        onActiveChange = ::onActiveChange,
                        onClearClick = ::onClearClick,
                    ),
                )
            }
        })
    }

    private fun onQueryChange(query: String) {
        stateController.update(UpdateSearchBarQueryTransformer(query))
    }

    private fun onActiveChange(isActive: Boolean) {
        if (!isActive) params.onBackClick()
    }

    private fun onClearClick() {
        stateController.update(UpdateSearchBarQueryTransformer(""))
    }

    private fun subscribeToQueryChanges() {
        stateController.uiState
            .map { it.searchBar.query.trim() }
            .distinctUntilChanged()
            .onEach { query ->
                shouldShowAllTokensIncludingUnder100k = false
                if (query.isEmpty()) {
                    marketSearchDebounceJob.cancel()
                    searchMarketsListManager.clearStateAndStopAllActions()
                    updateQuotesJob.cancel()
                    loadHistory()
                } else {
                    stateController.update(SetSearchResultsLoadingTransformer())
                    subscribeToSearchResults(query)
                    modelScope.launch(dispatchers.default) {
                        delay(MARKET_SEARCH_DEBOUNCE_MS)
                        val latestQuery = stateController.value.searchBar.query.trim()
                        if (latestQuery.isNotEmpty()) {
                            searchMarketsListManager.reload(searchText = latestQuery)
                        }
                    }.saveIn(marketSearchDebounceJob)
                }
            }
            .launchIn(modelScope)
    }

    private fun subscribeToSearchResults(query: String) {
        modelScope.launch {
            getSearchResultsUseCase(query = query).collectLatest { searchResult ->
                val userAssets = searchResult.userAssets.map { entry ->
                    UserAssetItemUM(
                        id = "${entry.userWalletId.stringValue}_${entry.accountId.value}" +
                            "_${entry.currencyStatus.currency.id.value}",
                        tokenIconUrl = entry.currencyStatus.currency.iconUrl,
                        tokenName = entry.currencyStatus.currency.name,
                        tokenSymbol = entry.currencyStatus.currency.symbol,
                        accountName = entry.accountName.toDisplayString(),
                        onClick = {
                            // TODO in [REDACTED_TASK_KEY] while just a stub item. Will be handled in next task.
                        },
                    )
                }.toImmutableList()
                stateController.update(UpdateUserAssetsTransformer(userAssets))
            }
        }.saveIn(searchResultsJob)
    }

    private fun subscribeToQuotesPolling() {
        searchMarketsListManager.onLastBatchLoadedSuccess.onEach { batchKey ->
            searchMarketsListManager.loadCharts(setOf(batchKey), MarketsListUM.TrendInterval.H24)
            modelScope.loadQuotesWithTimer(UPDATE_QUOTES_TIMER_MILLIS)
        }.launchIn(modelScope)
    }

    private fun subscribeToAppCurrencyChanges() {
        currentAppCurrency.drop(1).onEach {
            val query = stateController.value.searchBar.query
            if (query.isNotEmpty()) {
                searchMarketsListManager.reload()
            }
        }.launchIn(modelScope)
    }

    private fun subscribeToMarketUiItems() {
        combine(
            flow = stateController.uiState.map { it.searchBar.query }.distinctUntilChanged(),
            flow2 = searchMarketsListManager.uiItems,
            flow3 = searchMarketsListManager.isSearchNotFoundState,
            flow4 = searchMarketsListManager.isInInitialLoadingErrorState,
        ) { query, uiItems, isSearchNotFound, isInErrorState ->
            if (query.isEmpty()) return@combine null
            val marketResult = when {
                isSearchNotFound -> MarketSearchResultUM.NotFound
                isInErrorState -> MarketSearchResultUM.NotFound
                uiItems.isEmpty() -> MarketSearchResultUM.Empty
                else -> buildMarketContent(uiItems)
            }
            SearchMarketBatchUiSnapshot(
                marketResult = marketResult,
                isSearchNotFound = isSearchNotFound,
                isInErrorState = isInErrorState,
            )
        }
            .filterNotNull()
            .onEach { snapshot ->
                stateController.update(ApplySearchMarketBatchTransformer(snapshot))
            }
            .launchIn(modelScope)
    }

    private fun buildMarketContent(allItems: ImmutableList<MarketsListItemUM>): MarketSearchResultUM.Content {
        if (shouldShowAllTokensIncludingUnder100k) {
            return MarketSearchResultUM.Content(items = allItems)
        }

        val filtered = allItems.filter { !it.isUnder100kMarketCap }.toImmutableList()
        val hasUnder100k = filtered.size != allItems.size

        return if (hasUnder100k) {
            MarketSearchResultUM.Content(
                items = filtered,
                shouldShowUnder100kNotification = true,
                onShowUnder100kClick = {
                    shouldShowAllTokensIncludingUnder100k = true
                    stateController.update(
                        UpdateMarketItemsTransformer(
                            MarketSearchResultUM.Content(items = allItems),
                        ),
                    )
                },
            )
        } else {
            MarketSearchResultUM.Content(items = allItems)
        }
    }

    private fun loadHistory() {
        modelScope.launch {
            getSearchResultsUseCase(query = "").collectLatest { searchResult ->
                val textHints = searchResult.textHints.map { hint ->
                    TextHintItemUM(text = hint.text)
                }.toImmutableList()

                val appCurrency = currentAppCurrency.value
                val recentTokens = searchResult.recentTokens.map { token ->
                    recentSearchTokenToMarketsListItemConverter.convert(
                        RecentSearchTokenWithAppCurrency(token = token, appCurrency = appCurrency),
                    )
                }.toImmutableList()

                stateController.update(UpdateHistoryTransformer(textHints, recentTokens))

                loadRecentTokenCharts(recentTokens, appCurrency)
            }
        }.saveIn(searchResultsJob)
    }

    private suspend fun loadRecentTokenCharts(tokens: ImmutableList<MarketsListItemUM>, appCurrency: AppCurrency) {
        coroutineScope {
            tokens.map { token ->
                async(dispatchers.io) {
                    val chart = getTokenPriceChartUseCase(
                        appCurrency = appCurrency,
                        interval = PriceChangeInterval.H24,
                        tokenId = token.id,
                        tokenSymbol = token.currencySymbol,
                        preview = true,
                    ).getOrElse { return@async }

                    val chartData = priceAndTimePointValuesConverter.convert(
                        MarketChartData.Data(
                            y = chart.priceY.toImmutableList(),
                            x = chart.timeStamps.map { it.toBigDecimal() }.toImmutableList(),
                        ).sorted(),
                    )

                    stateController.update(UpdateRecentTokenChartTransformer(token.id, chartData))
                }
            }.awaitAll()
        }
    }

    private fun AccountName.toDisplayString(): String {
        return when (this) {
            is AccountName.DefaultMain -> "Main" // TODO [REDACTED_TASK_KEY] localize
            is AccountName.Custom -> value
        }
    }

    private fun CoroutineScope.loadQuotesWithTimer(timeMillis: Long) {
        launch {
            while (true) {
                delay(timeMillis)
                searchMarketsListManager.updateQuotes()
            }
        }.saveIn(updateQuotesJob)
    }
}