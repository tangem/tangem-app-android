package com.tangem.features.feed.components

import androidx.compose.runtime.Immutable
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.navigation.Route
import com.tangem.core.ui.DesignFeatureToggles
import com.tangem.core.ui.decompose.ComposableModularBottomSheetContentComponent
import com.tangem.features.feed.components.earn.DefaultEarnComponent
import com.tangem.features.feed.components.feed.DefaultFeedComponent
import com.tangem.features.feed.components.feed.DefaultFeedComponent.FeedParams
import com.tangem.features.feed.components.market.details.DefaultMarketsTokenDetailsComponent
import com.tangem.features.feed.components.market.details.portfolio.add.AddToPortfolioPreselectedDataComponent
import com.tangem.features.feed.components.market.details.portfolio.api.MarketsPortfolioComponent
import com.tangem.features.feed.components.market.details.portfolioblock.PortfolioBlockComponent
import com.tangem.features.feed.components.market.list.DefaultMarketsTokenListComponent
import com.tangem.features.feed.components.news.details.DefaultNewsDetailsComponent
import com.tangem.features.feed.components.news.list.DefaultNewsListComponent
import com.tangem.features.feed.components.news.list.DefaultNewsListComponent.Params
import com.tangem.features.feed.components.search.DefaultSearchComponent
import com.tangem.features.promobanners.api.NewPromoBannersFeatureToggles
import com.tangem.features.promobanners.api.PromoBannersBlockComponent
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Suppress("LongParameterList")
internal class FeedEntryChildFactory @Inject constructor(
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val portfolioComponentFactory: MarketsPortfolioComponent.Factory,
    private val portfolioBlockComponentFactory: PortfolioBlockComponent.Factory,
    private val addToPortfolioPreselectedDataComponent: AddToPortfolioPreselectedDataComponent.Factory,
    private val promoBannersBlockComponentFactory: PromoBannersBlockComponent.Factory,
    private val newPromoBannersFeatureToggles: NewPromoBannersFeatureToggles,
    private val designFeatureToggles: DesignFeatureToggles,
) {

    @Serializable
    @Immutable
    sealed interface Child : Route {

        @Serializable
        @Immutable
        data object Feed : Child

        @Serializable
        @Immutable
        data class TokenList(val params: DefaultMarketsTokenListComponent.Params) : Child

        @Serializable
        @Immutable
        data class TokenDetails(val params: DefaultMarketsTokenDetailsComponent.Params) : Child

        @Serializable
        @Immutable
        data object NewsList : Child

        @Serializable
        @Immutable
        data class NewsDetails(val params: DefaultNewsDetailsComponent.Params) : Child

        @Serializable
        @Immutable
        data object Earn : Child

        @Serializable
        @Immutable
        data object Search : Child
    }

    @Suppress("LongMethod")
    fun createChild(
        child: Child,
        appComponentContext: AppComponentContext,
        feedEntryClickIntents: FeedEntryClickIntents,
        onBackClicked: () -> Unit,
    ): ComposableModularBottomSheetContentComponent {
        return when (child) {
            is Child.TokenDetails -> {
                DefaultMarketsTokenDetailsComponent(
                    appComponentContext = appComponentContext,
                    params = child.params,
                    analyticsEventHandler = analyticsEventHandler,
                    portfolioComponentFactory = portfolioComponentFactory,
                    portfolioBlockComponentFactory = portfolioBlockComponentFactory,
                    designFeatureToggles = designFeatureToggles,
                )
            }
            is Child.TokenList -> {
                DefaultMarketsTokenListComponent(
                    appComponentContext = appComponentContext,
                    params = child.params,
                    clickIntents = DefaultMarketsTokenListComponent.ClickIntents(
                        onBackClicked = onBackClicked,
                        onSearchClicked = feedEntryClickIntents::openSearch,
                        onTokenClick = { token, currency ->
                            feedEntryClickIntents.onMarketItemClick(
                                token = token,
                                appCurrency = currency,
                                source = AnalyticsParam.ScreensSources.Market.value,
                            )
                        },
                    ),
                )
            }
            is Child.NewsDetails -> {
                DefaultNewsDetailsComponent(
                    appComponentContext = appComponentContext,
                    params = child.params,
                )
            }
            Child.NewsList -> {
                DefaultNewsListComponent(
                    appComponentContext = appComponentContext,
                    params = Params(
                        onArticleClicked = { currentArticle, prefetchedArticles, paginationConfig ->
                            feedEntryClickIntents.onArticleClick(
                                articleId = currentArticle,
                                preselectedArticlesId = prefetchedArticles,
                                screenSource = AnalyticsParam.ScreensSources.NewsList,
                                paginationConfig = paginationConfig,
                            )
                        },
                        onBackClick = onBackClicked,
                    ),
                )
            }
            Child.Feed -> {
                DefaultFeedComponent(
                    appComponentContext = appComponentContext,
                    params = FeedParams(feedClickIntents = feedEntryClickIntents),
                    addToPortfolioComponentFactory = addToPortfolioPreselectedDataComponent,
                    promoBannersBlockComponentFactory = promoBannersBlockComponentFactory,
                    newPromoBannersFeatureToggles = newPromoBannersFeatureToggles,
                )
            }
            is Child.Earn -> {
                DefaultEarnComponent(
                    appComponentContext = appComponentContext,
                    params = DefaultEarnComponent.Params(
                        onBackClick = onBackClicked,
                        onSearchClicked = feedEntryClickIntents::openSearch,
                    ),
                    addToPortfolioComponentFactory = addToPortfolioPreselectedDataComponent,
                )
            }
            Child.Search -> DefaultSearchComponent(
                appComponentContext = appComponentContext,
                params = DefaultSearchComponent.Params(
                    onBackClick = onBackClicked,
                ),
            )
        }
    }
}