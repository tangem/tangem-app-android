package com.tangem.features.feed.model.feed

import androidx.compose.runtime.Stable
import arrow.core.getOrElse
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.ui.markets.models.MarketsListItemUM
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.utils.DateTimeFormatters
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.earn.usecase.FetchTopEarnTokensUseCase
import com.tangem.domain.earn.usecase.GetTopEarnTokensUseCase
import com.tangem.domain.markets.GetTopFiveMarketTokenUseCase
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.domain.markets.TokenMarketListConfig
import com.tangem.domain.markets.toSerializableParam
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.earn.EarnTokenWithCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.news.usecase.FetchTrendingNewsUseCase
import com.tangem.domain.news.usecase.ManageTrendingNewsUseCase
import com.tangem.features.feed.components.feed.DefaultFeedComponent
import com.tangem.features.feed.components.feed.FeedPortfolioRoute
import com.tangem.features.feed.components.market.details.portfolio.add.AddToPortfolioPreselectedDataComponent
import com.tangem.features.feed.entry.featuretoggle.FeedFeatureToggle
import com.tangem.features.feed.impl.R
import com.tangem.features.feed.model.feed.analytics.FeedAnalyticsEvent
import com.tangem.features.feed.model.feed.state.FeedMarketsBatchFlowManager
import com.tangem.features.feed.model.feed.state.FeedStateController
import com.tangem.features.feed.model.feed.state.transformers.*
import com.tangem.features.feed.model.market.list.state.SortByTypeUM
import com.tangem.features.feed.ui.earn.state.EarnListUM
import com.tangem.features.feed.ui.feed.state.*
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
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
@Suppress("LongParameterList", "LargeClass")
internal class FeedComponentModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val fetchTrendingNewsUseCase: FetchTrendingNewsUseCase,
    private val manageTrendingNewsUseCase: ManageTrendingNewsUseCase,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val stateController: FeedStateController,
    private val feedFeatureToggle: FeedFeatureToggle,
    private val fetchTopEarnTokensUseCase: FetchTopEarnTokensUseCase,
    private val getTopEarnTokensUseCase: GetTopEarnTokensUseCase,
    private val appRouter: AppRouter,
    getTopFiveMarketTokenUseCase: GetTopFiveMarketTokenUseCase,
    getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    paramsContainer: ParamsContainer,
) : Model() {

    private var quotesUpdateJob: Job? = null

    private val params = paramsContainer.require<DefaultFeedComponent.FeedParams>()

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

    val bottomSheetNavigation: SlotNavigation<FeedPortfolioRoute> = SlotNavigation()

    val addToPortfolioCallback = object : AddToPortfolioPreselectedDataComponent.Callback {
        override fun onDismiss() = bottomSheetNavigation.dismiss()
        override fun onSuccess(addedToken: CryptoCurrency, walletId: UserWalletId) {
            bottomSheetNavigation.dismiss()
            appRouter.push(
                AppRoute.CurrencyDetails(
                    userWalletId = walletId,
                    currency = addedToken,
                ),
            )
        }
    }

    val state: StateFlow<FeedListUM>
        get() = stateController.uiState

    val isVisibleOnScreen = MutableStateFlow(false)

    init {
        initializeState()
        updateCallbacks()
        fetchTrendingNews()
        fetchEarnData()
        fetchCharts()
        subscribeOnCurrencyUpdate()
        subscribeOnDataState()
    }

    private fun subscribeOnDataState() {
        modelScope.launch(dispatchers.default) {
            combine(
                flow = marketsBatchFlowManager.itemsByOrder,
                flow2 = marketsBatchFlowManager.loadingStatesByOrder,
                flow3 = marketsBatchFlowManager.errorStatesByOrder,
                flow4 = manageTrendingNewsUseCase.observeTrendingNews(),
                flow5 = getTopEarnTokensUseCase(),
            ) { itemsByOrder, loadingStatesByOrder, errorStatesByOrder, trendingNewsResult, earnResult ->
                val globalStateTransformer = UpdateGlobalFeedStateTransformer(
                    loadingStatesByOrder = loadingStatesByOrder,
                    errorStatesByOrder = errorStatesByOrder,
                    trendingNewsResult = trendingNewsResult,
                    earnResult = earnResult,
                    onRetryClicked = {
                        modelScope.launch(dispatchers.default) {
                            stateController.update { currentState ->
                                currentState.copy(globalState = GlobalFeedState.Loading)
                            }
                            fetchTrendingNews()
                            fetchEarnData()
                            marketsBatchFlowManager.reloadAll()
                        }
                    },
                    analyticsEventHandler = analyticsEventHandler,
                    feedFeatureToggle = feedFeatureToggle,
                )

                val currentState = stateController.value
                val newGlobalState = globalStateTransformer.transform(currentState)
                val shouldSkipIndividualStates = newGlobalState.globalState !is GlobalFeedState.Content

                stateController.update(globalStateTransformer)

                if (!shouldSkipIndividualStates) {
                    stateController.updateAll(
                        UpdateMarketChartsTransformer(
                            itemsByOrder = itemsByOrder,
                            loadingStatesByOrder = loadingStatesByOrder,
                            errorStatesByOrder = errorStatesByOrder,
                            onReload = { marketsBatchFlowManager.reloadManager(it.toOrder()) },
                            analyticsEventHandler = analyticsEventHandler,
                        ),
                        UpdateTrendingNewsStateTransformer(
                            result = trendingNewsResult,
                            onRetryClicked = ::fetchTrendingNews,
                            analyticsEventHandler = analyticsEventHandler,
                        ),
                        UpdateEarnStateTransformer(
                            isEarnEnabled = feedFeatureToggle.isEarnBlockEnabled,
                            onItemClick = ::handleEarnTokenClick,
                            onRetryClick = ::fetchEarnData,
                            earnResult = earnResult,
                        ),
                    )
                }

                val currentSortType = stateController.value.marketChartConfig.currentSortByType
                val items = itemsByOrder[currentSortType]
                val isLoading = loadingStatesByOrder[currentSortType] == true
                if (items != null && items.isNotEmpty() && !isLoading) {
                    val order = currentSortType.toOrder()
                    marketsBatchFlowManager.loadCharts(order)
                }
            }.collect()
        }
    }

    private fun subscribeOnCurrencyUpdate() {
        modelScope.launch(dispatchers.default) {
            currentAppCurrency.drop(1).collect {
                marketsBatchFlowManager.reloadAll()
            }
        }
    }

    private fun fetchTrendingNews() {
        modelScope.launch(dispatchers.default) {
            fetchTrendingNewsUseCase()
        }
    }

    private fun fetchEarnData() {
        if (!feedFeatureToggle.isEarnBlockEnabled) return
        modelScope.launch(dispatchers.default) {
            stateController.update(UpdateEarnLoadingStateTransformer())
            fetchTopEarnTokensUseCase()
        }
    }

    private fun fetchCharts() {
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

    private fun initializeState() {
        stateController.update { initialState() }
    }

    private fun initialState(): FeedListUM {
        return FeedListUM(
            currentDate = getCurrentDate(),
            feedListSearchBar = FeedListSearchBar(
                placeholderText = resourceReference(R.string.markets_search_header_title),
                onBarClick = {
                    analyticsEventHandler.send(FeedAnalyticsEvent.TokenSearchedClicked())
                    params.feedClickIntents.onMarketOpenClick(null)
                },
            ),
            feedListCallbacks = FeedListCallbacks(
                onSearchClick = {},
                onMarketOpenClick = {},
                onArticleClick = {},
                onOpenAllNews = {},
                onMarketItemClick = {},
                onSortTypeClick = {},
                onSliderScroll = {
                    analyticsEventHandler.send(FeedAnalyticsEvent.NewsCarouselScrolled())
                },
                onSliderEndReached = {
                    analyticsEventHandler.send(FeedAnalyticsEvent.NewsCarouselEndReached())
                },
                onOpenEarnPageClick = params.feedClickIntents::onOpenEarnPage,
            ),
            news = NewsUM(
                content = persistentListOf(),
                onRetryClicked = {},
                newsUMState = NewsUMState.LOADING,
            ),
            trendingArticle = null,
            marketChartConfig = MarketChartConfig(
                marketCharts = buildMap {
                    SortByTypeUM.entries.forEach {
                        put(it, MarketChartUM.Loading)
                    }
                }.toPersistentHashMap(),
                currentSortByType = SortByTypeUM.TopGainers,
            ),
            globalState = GlobalFeedState.Loading,
            earnListUM = if (feedFeatureToggle.isEarnBlockEnabled) {
                EarnListUM.Loading
            } else {
                null
            },
        )
    }

    private fun startQuotesUpdateTimer() {
        quotesUpdateJob?.cancel()
        quotesUpdateJob = modelScope.launch {
            while (true) {
                delay(DELAY_TO_FETCH_QUOTES)
                isVisibleOnScreen.first { it }
                marketsBatchFlowManager.updateQuotes()
            }
        }
    }

    private fun updateCallbacks() {
        stateController.update { feedListUM ->
            feedListUM.copy(
                feedListCallbacks = feedListUM.feedListCallbacks.copy(
                    onSortTypeClick = ::handleSortTypeClicked,
                    onMarketItemClick = ::handleMarketItemClicked,
                    onMarketOpenClick = ::handleMarketOpenClicked,
                    onArticleClick = ::handleArticleClicked,
                    onOpenAllNews = ::handleOpenAllNews,
                    onOpenEarnPageClick = params.feedClickIntents::onOpenEarnPage,
                ),
            )
        }
    }

    /* start of clicks area */
    private fun handleSortTypeClicked(sortByType: SortByTypeUM) {
        stateController.update { currentState ->
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

    private fun handleMarketItemClicked(item: MarketsListItemUM) {
        val tokenMarket = marketsBatchFlowManager.getTokenMarketById(item.id)
        if (tokenMarket != null) {
            params.feedClickIntents.onMarketItemClick(
                token = tokenMarket.toSerializableParam(),
                appCurrency = currentAppCurrency.value,
                source = AnalyticsParam.ScreensSources.Market.value,
            )
        }
    }

    private fun handleMarketOpenClicked(sortBy: SortByTypeUM) {
        when (sortBy) {
            SortByTypeUM.Rating -> {
                analyticsEventHandler.send(
                    FeedAnalyticsEvent.TokenListOpened(
                        AnalyticsParam.ScreensSources.Markets,
                    ),
                )
            }
            SortByTypeUM.Trending,
            SortByTypeUM.ExperiencedBuyers,
            SortByTypeUM.TopGainers,
            SortByTypeUM.TopLosers,
            SortByTypeUM.Staking,
            SortByTypeUM.YieldSupply,
            -> {
                analyticsEventHandler.send(
                    FeedAnalyticsEvent.TokenListOpened(
                        AnalyticsParam.ScreensSources.MarketPulse,
                    ),
                )
            }
        }
        params.feedClickIntents.onMarketOpenClick(sortBy)
    }

    private fun handleArticleClicked(articleId: Int) {
        val currentState = stateController.value
        val trendingArticleId = currentState.trendingArticle?.id
        if (articleId == trendingArticleId) {
            analyticsEventHandler.send(
                event = FeedAnalyticsEvent.NewsCarouselTrendingClicked(newsId = articleId),
            )
        }
        params.feedClickIntents.onArticleClick(
            articleId = articleId,
            preselectedArticlesId = listOfNotNull(trendingArticleId) +
                when (currentState.news.newsUMState) {
                    NewsUMState.CONTENT -> currentState.news.content.map { it.id }
                    NewsUMState.LOADING,
                    NewsUMState.ERROR,
                    -> emptyList()
                },
            paginationConfig = null,
            screenSource = AnalyticsParam.ScreensSources.Markets,
        )
    }

    private fun handleOpenAllNews(isFromCarouselButton: Boolean) {
        if (isFromCarouselButton) {
            analyticsEventHandler.send(FeedAnalyticsEvent.NewsCarouselAllNewsButton())
        } else {
            analyticsEventHandler.send(FeedAnalyticsEvent.NewsListOpened())
        }
        params.feedClickIntents.onOpenAllNews()
    }

    private fun handleEarnTokenClick(earnTokenWithCurrency: EarnTokenWithCurrency) {
        bottomSheetNavigation.activate(
            FeedPortfolioRoute.AddToPortfolio(
                tokenToAdd = AddToPortfolioPreselectedDataComponent.TokenToAdd(
                    network = TokenMarketInfo.Network(
                        networkId = earnTokenWithCurrency.earnToken.networkId,
                        isExchangeable = false,
                        contractAddress = earnTokenWithCurrency.earnToken.tokenAddress,
                        decimalCount = earnTokenWithCurrency.earnToken.decimalCount,
                    ),
                    id = CryptoCurrency.RawID(earnTokenWithCurrency.earnToken.tokenId),
                    name = earnTokenWithCurrency.earnToken.tokenName,
                    symbol = earnTokenWithCurrency.earnToken.tokenSymbol,
                ),
            ),
        )
    }
    /* end of clicks area */

    /* start of utils area */
    private fun SortByTypeUM.toOrder(): TokenMarketListConfig.Order {
        return when (this) {
            SortByTypeUM.Rating -> TokenMarketListConfig.Order.ByRating
            SortByTypeUM.Trending -> TokenMarketListConfig.Order.Trending
            SortByTypeUM.ExperiencedBuyers -> TokenMarketListConfig.Order.Buyers
            SortByTypeUM.TopGainers -> TokenMarketListConfig.Order.TopGainers
            SortByTypeUM.TopLosers -> TokenMarketListConfig.Order.TopLosers
            SortByTypeUM.Staking -> TokenMarketListConfig.Order.Staking
            SortByTypeUM.YieldSupply -> TokenMarketListConfig.Order.YieldSupply
        }
    }

    private fun getCurrentDate(): String {
        val localDate = DateTime(DateTime.now(), DateTimeZone.getDefault())
        return DateTimeFormatters.formatDate(formatter = DateTimeFormatters.dateDMMM, date = localDate)
    }
    /* end of utils area */

    companion object {
        private const val DELAY_TO_FETCH_QUOTES = 60_000L
    }
}