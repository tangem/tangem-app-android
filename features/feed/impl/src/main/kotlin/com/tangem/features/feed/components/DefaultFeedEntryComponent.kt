package com.tangem.features.feed.components

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.value.Value
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.decompose.navigation.inner.InnerRouter
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.core.ui.decompose.ComposableModularBottomSheetContentComponent
import com.tangem.core.ui.res.LocalMainBottomSheetColor
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.TokenMarketParams
import com.tangem.domain.news.model.NewsListConfig
import com.tangem.features.feed.components.market.details.DefaultMarketsTokenDetailsComponent
import com.tangem.features.feed.components.market.list.DefaultMarketsTokenListComponent
import com.tangem.features.feed.components.news.details.DefaultNewsDetailsComponent
import com.tangem.features.feed.entry.components.FeedEntryComponent
import com.tangem.features.feed.entry.components.FeedEntryRoute
import com.tangem.features.feed.model.FeedEntryModel
import com.tangem.features.feed.model.feed.FeedModelClickIntents
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
                                screenSource = AnalyticsParam.ScreensSources.Markets,
                            )
                        },
                    ),
                ),
            )
        }

        override fun onMarketOpenClick(sortBy: SortByTypeUM?) {
            innerRouter.push(
                route = FeedEntryChildFactory.Child.TokenList(
                    params = DefaultMarketsTokenListComponent.Params(
                        onBackClicked = { onChildBack() },
                        onTokenClick = { token, currency ->
                            onMarketItemClick(
                                token = token,
                                appCurrency = currency,
                                source = AnalyticsParam.ScreensSources.Market.value,
                            )
                        },
                        preselectedSortType = sortBy ?: SortByTypeUM.Rating,
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
            innerRouter.push(FeedEntryChildFactory.Child.NewsList)
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
            isOpenedInBottomSheet = true,
        )
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val background = TangemTheme.colors.background.tertiary
        CompositionLocalProvider(
            LocalMainBottomSheetColor provides remember(background) { mutableStateOf(background) },
        ) {
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
                isOpenedInBottomSheet = false,
            )
        }
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
                ),
            )
            FeedEntryRoute.MarketTokenList -> FeedEntryChildFactory.Child.TokenList(
                DefaultMarketsTokenListComponent.Params(
                    onBackClicked = { router.pop() },
                    onTokenClick = { token, currency ->
                        clickIntents.onMarketItemClick(
                            token = token,
                            appCurrency = currency,
                            source = AnalyticsParam.ScreensSources.Market.value,
                        )
                    },
                    preselectedSortType = SortByTypeUM.Rating,
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
            null -> FeedEntryChildFactory.Child.Feed
        }
    }

    @AssistedFactory
    interface Factory : FeedEntryComponent.Factory {
        override fun create(context: AppComponentContext, entryRoute: FeedEntryRoute?): DefaultFeedEntryComponent
    }
}

internal interface FeedEntryClickIntents : FeedModelClickIntents