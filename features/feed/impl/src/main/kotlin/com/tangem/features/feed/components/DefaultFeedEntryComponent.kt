package com.tangem.features.feed.components

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.stack.*
import com.arkivanov.decompose.value.Value
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.decompose.navigation.inner.InnerRouter
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.core.ui.decompose.ComposableModularBottomSheetContentComponent
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.PreselectedMarketsInterval
import com.tangem.domain.markets.PreselectedMarketsOrder
import com.tangem.domain.markets.TokenMarketParams
import com.tangem.domain.models.earn.PreselectedEarnType
import com.tangem.domain.news.model.NewsListConfig
import com.tangem.features.feed.components.earn.DefaultEarnComponent
import com.tangem.features.feed.components.market.details.DefaultMarketsTokenDetailsComponent
import com.tangem.features.feed.components.market.list.DefaultMarketsTokenListComponent
import com.tangem.features.feed.components.news.details.DefaultNewsDetailsComponent
import com.tangem.features.feed.components.news.list.DefaultNewsListComponent
import com.tangem.features.feed.entry.components.FeedEntryComponent
import com.tangem.features.feed.entry.components.FeedEntryRoute
import com.tangem.features.feed.model.FeedEntryModel
import com.tangem.features.feed.model.feed.FeedModelClickIntents
import com.tangem.features.feed.model.market.list.state.MarketsListUM
import com.tangem.features.feed.model.market.list.state.SortByTypeUM
import com.tangem.features.feed.ui.EntryContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

@Stable
internal class DefaultFeedEntryComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted entryRoute: FeedEntryRoute?,
    private val feedEntryChildFactory: FeedEntryChildFactory,
) : FeedEntryComponent, AppComponentContext by context {

    private val model: FeedEntryModel = getOrCreateModel()

    private val stackNavigation = StackNavigation<FeedEntryChildFactory.Child>()

    private val innerRouter = InnerRouter<FeedEntryChildFactory.Child>(
        stackNavigation = stackNavigation,
        popCallback = { onChildBack() },
    )

    private val clickIntents = object : FeedEntryClickIntents {
        override fun onMarketItemClick(
            token: TokenMarketParams,
            appCurrency: AppCurrency,
            source: String,
            newsId: Int?,
        ) {
            innerRouter.push(
                route = FeedEntryChildFactory.Child.TokenDetails(
                    params = DefaultMarketsTokenDetailsComponent.Params(
                        token = token,
                        appCurrency = appCurrency,
                        shouldShowPortfolio = true,
                        analyticsParams = DefaultMarketsTokenDetailsComponent.AnalyticsParams(
                            blockchain = null,
                            newsId = newsId,
                            source = source,
                        ),
                        onBackClicked = { onChildBack() },
                        onArticleClick = { articleId, preselectedArticlesId ->
                            onArticleClick(
                                articleId = articleId,
                                preselectedArticlesId = preselectedArticlesId,
                                screenSource = AnalyticsParam.ScreensSources.Token,
                            )
                        },
                    ),
                ),
            )
        }

        override fun onMarketOpenClick(sortBy: SortByTypeUM?) {
            // Markets list and search can reach each other
            stackNavigation.bringToFront(
                FeedEntryChildFactory.Child.TokenList(
                    params = DefaultMarketsTokenListComponent.Params(
                        preselectedSortType = sortBy ?: SortByTypeUM.Rating,
                        preselectedInterval = MarketsListUM.TrendInterval.H24,
                        shouldAlwaysShowSearchBar = sortBy == null,
                    ),
                ),
            )
        }

        override fun onArticleClick(
            articleId: Int,
            preselectedArticlesId: List<Int>,
            screenSource: AnalyticsParam.ScreensSources,
            paginationConfig: NewsListConfig?,
        ) {
            innerRouter.push(
                FeedEntryChildFactory.Child.NewsDetails(
                    params = DefaultNewsDetailsComponent.Params(
                        screenSource = screenSource.value,
                        articleId = articleId,
                        onBackClicked = { onChildBack() },
                        preselectedArticlesId = preselectedArticlesId,
                        paginationConfig = paginationConfig,
                        onTokenClick = { token, currency ->
                            onMarketItemClick(
                                token = token,
                                appCurrency = currency,
                                source = AnalyticsParam.ScreensSources.NewsPage.value,
                                newsId = articleId,
                            )
                        },
                    ),
                ),
            )
        }

        override fun onOpenAllNews() {
            innerRouter.push(
                FeedEntryChildFactory.Child.NewsList(
                    params = buildNewsListParams(onBack = { onChildBack() }),
                ),
            )
        }

        override fun onOpenEarnPage() {
            innerRouter.push(
                FeedEntryChildFactory.Child.Earn(
                    params = buildEarnParams(onBack = { onChildBack() }),
                ),
            )
        }

        override fun openSearch(source: String) {
            stackNavigation.bringToFront(FeedEntryChildFactory.Child.Search(source))
        }
    }

    private val stack: Value<ChildStack<FeedEntryChildFactory.Child, ComposableModularBottomSheetContentComponent>> =
        childStack(
            key = "main",
            source = stackNavigation,
            serializer = FeedEntryChildFactory.Child.serializer(),
            initialConfiguration = mapEntryRouteToChild(entryRoute),
            handleBackButton = false,
            childFactory = { configuration, factoryContext ->
                feedEntryChildFactory.createChild(
                    child = configuration,
                    appComponentContext = childByContext(
                        componentContext = factoryContext,
                        router = innerRouter,
                    ),
                    feedEntryClickIntents = clickIntents,
                    onBackClicked = { onChildBack() },
                )
            },
        )

    @Composable
    override fun BottomSheetContent(
        bottomSheetState: State<BottomSheetState>,
        onHeaderSizeChange: (Dp) -> Unit,
        onExpandSheet: () -> Unit,
        modifier: Modifier,
    ) {
        val bsState by bottomSheetState
        val stackStack = stack.subscribeAsState()
        BackHandler(
            enabled = bottomSheetState.value == BottomSheetState.EXPANDED &&
                stackStack.value.active.configuration !is FeedEntryChildFactory.Child.Feed,
        ) {
            onChildBack()
        }

        LaunchedEffect(bsState) {
            model.containerBottomSheetState.value = bsState
        }

        EntryContent(
            bottomSheetState = bottomSheetState,
            stackState = stackStack,
            onHeaderSizeChange = onHeaderSizeChange,
            onExpandSheet = onExpandSheet,
            isOpenedInBottomSheet = true,
        )
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val bottomSheetState = remember {
            derivedStateOf { BottomSheetState.EXPANDED }
        }

        BackHandler {
            router.pop()
        }

        EntryContent(
            bottomSheetState = bottomSheetState,
            stackState = stack.subscribeAsState(),
            onHeaderSizeChange = {},
            onExpandSheet = {},
            isOpenedInBottomSheet = false,
        )
    }

    private fun onChildBack() {
        if (stack.value.active.configuration !is FeedEntryChildFactory.Child.Feed) {
            stackNavigation.pop()
        }
    }

    private fun mapEntryRouteToChild(entryRoute: FeedEntryRoute?): FeedEntryChildFactory.Child {
        return when (entryRoute) {
            is FeedEntryRoute.MarketTokenDetails -> FeedEntryChildFactory.Child.TokenDetails(
                params = DefaultMarketsTokenDetailsComponent.Params(
                    token = entryRoute.token,
                    appCurrency = entryRoute.appCurrency,
                    shouldShowPortfolio = entryRoute.shouldShowPortfolio,
                    analyticsParams = entryRoute.analyticsParams?.let { params ->
                        DefaultMarketsTokenDetailsComponent.AnalyticsParams(
                            blockchain = params.blockchain,
                            source = params.source,
                        )
                    },
                    onBackClicked = { router.pop() },
                    onArticleClick = { articleId, preselectedArticlesId ->
                        clickIntents.onArticleClick(
                            articleId = articleId,
                            preselectedArticlesId = preselectedArticlesId,
                            screenSource = AnalyticsParam.ScreensSources.Token,
                            paginationConfig = null,
                        )
                    },
                    preselectedSection = entryRoute.preselectedSection,
                    shouldOpenExchanges = entryRoute.shouldOpenExchanges,
                    exchangesCount = entryRoute.exchangesCount,
                ),
            )
            is FeedEntryRoute.MarketTokenList -> FeedEntryChildFactory.Child.TokenList(
                DefaultMarketsTokenListComponent.Params(
                    preselectedSortType = mapOrderToSortType(entryRoute.preselectedOrder),
                    preselectedInterval = mapIntervalToTrendInterval(entryRoute.preselectedInterval),
                    shouldAlwaysShowSearchBar = false,
                ),
            )
            is FeedEntryRoute.NewsDetail -> FeedEntryChildFactory.Child.NewsDetails(
                DefaultNewsDetailsComponent.Params(
                    screenSource = AnalyticsParam.ScreensSources.NewsLink.value,
                    articleId = entryRoute.articleId,
                    onBackClicked = { router.pop() },
                    preselectedArticlesId = entryRoute.preselectedArticlesId,
                    onTokenClick = { token, currency ->
                        clickIntents.onMarketItemClick(
                            token = token,
                            appCurrency = currency,
                            source = AnalyticsParam.ScreensSources.NewsPage.value,
                            newsId = entryRoute.articleId,
                        )
                    },
                ),
            )
            is FeedEntryRoute.NewsList -> FeedEntryChildFactory.Child.NewsList(
                params = buildNewsListParams(
                    onBack = { router.pop() },
                    preselectedCategoryId = entryRoute.preselectedCategoryId,
                ),
            )
            is FeedEntryRoute.Earn -> FeedEntryChildFactory.Child.Earn(
                params = buildEarnParams(
                    onBack = { router.pop() },
                    preselectedEarnType = entryRoute.preselectedEarnType,
                    preselectedNetworkId = entryRoute.preselectedNetworkId,
                ),
            )
            null -> FeedEntryChildFactory.Child.Feed
        }
    }

    private fun buildNewsListParams(
        onBack: () -> Unit,
        preselectedCategoryId: Int? = null,
    ): DefaultNewsListComponent.Params = DefaultNewsListComponent.Params(
        onArticleClicked = { currentArticle, prefetchedArticles, paginationConfig ->
            clickIntents.onArticleClick(
                articleId = currentArticle,
                preselectedArticlesId = prefetchedArticles,
                screenSource = AnalyticsParam.ScreensSources.NewsList,
                paginationConfig = paginationConfig,
            )
        },
        onBackClick = onBack,
        preselectedCategoryId = preselectedCategoryId,
    )

    private fun buildEarnParams(
        onBack: () -> Unit,
        preselectedEarnType: PreselectedEarnType? = null,
        preselectedNetworkId: String? = null,
    ): DefaultEarnComponent.Params = DefaultEarnComponent.Params(
        onBackClick = onBack,
        onSearchClicked = clickIntents::openSearch,
        preselectedEarnType = preselectedEarnType,
        preselectedNetworkId = preselectedNetworkId,
    )

    @AssistedFactory
    interface Factory : FeedEntryComponent.Factory {
        override fun create(context: AppComponentContext, entryRoute: FeedEntryRoute?): DefaultFeedEntryComponent
    }
}

private fun mapOrderToSortType(order: PreselectedMarketsOrder?): SortByTypeUM {
    return when (order) {
        PreselectedMarketsOrder.Rating -> SortByTypeUM.Rating
        PreselectedMarketsOrder.Trending -> SortByTypeUM.Trending
        PreselectedMarketsOrder.Buyers -> SortByTypeUM.ExperiencedBuyers
        PreselectedMarketsOrder.Gainers -> SortByTypeUM.TopGainers
        PreselectedMarketsOrder.Losers -> SortByTypeUM.TopLosers
        null -> SortByTypeUM.Rating
    }
}

private fun mapIntervalToTrendInterval(interval: PreselectedMarketsInterval?): MarketsListUM.TrendInterval {
    return when (interval) {
        PreselectedMarketsInterval.H24 -> MarketsListUM.TrendInterval.H24
        PreselectedMarketsInterval.W1 -> MarketsListUM.TrendInterval.D7
        PreselectedMarketsInterval.D30 -> MarketsListUM.TrendInterval.M1
        null -> MarketsListUM.TrendInterval.H24
    }
}

internal interface FeedEntryClickIntents : FeedModelClickIntents