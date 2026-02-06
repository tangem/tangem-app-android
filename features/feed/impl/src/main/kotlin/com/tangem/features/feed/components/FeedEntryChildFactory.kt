package com.tangem.features.feed.components

import androidx.compose.runtime.Immutable
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.navigation.Route
import com.tangem.core.ui.decompose.ComposableModularBottomSheetContentComponent
import com.tangem.domain.account.featuretoggle.AccountsFeatureToggles
import com.tangem.features.feed.components.earn.DefaultEarnComponent
import com.tangem.features.feed.components.feed.DefaultFeedComponent
import com.tangem.features.feed.components.market.details.DefaultMarketsTokenDetailsComponent
import com.tangem.features.feed.components.market.details.portfolio.add.AddToPortfolioPreselectedDataComponent
import com.tangem.features.feed.components.market.details.portfolio.api.MarketsPortfolioComponent
import com.tangem.features.feed.components.market.list.DefaultMarketsTokenListComponent
import com.tangem.features.feed.components.news.details.DefaultNewsDetailsComponent
import com.tangem.features.feed.components.news.list.DefaultNewsListComponent
import kotlinx.serialization.Serializable
import javax.inject.Inject

internal class FeedEntryChildFactory @Inject constructor(
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val accountsFeatureToggles: AccountsFeatureToggles,
    private val portfolioComponentFactory: MarketsPortfolioComponent.Factory,
    private val addToPortfolioPreselectedDataComponent: AddToPortfolioPreselectedDataComponent.Factory,
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
        data class Earn(val params: DefaultEarnComponent.Params) : Child
    }

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
                    accountsFeatureToggles = accountsFeatureToggles,
                    portfolioComponentFactory = portfolioComponentFactory,
                )
            }
            is Child.TokenList -> {
                DefaultMarketsTokenListComponent(
                    appComponentContext = appComponentContext,
                    params = child.params,
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
                    params = DefaultNewsListComponent.Params(
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
                    params = DefaultFeedComponent.FeedParams(feedClickIntents = feedEntryClickIntents),
                    addToPortfolioComponentFactory = addToPortfolioPreselectedDataComponent,
                )
            }
            is Child.Earn -> {
                DefaultEarnComponent(
                    appComponentContext = appComponentContext,
                    params = child.params,
                )
            }
        }
    }
}