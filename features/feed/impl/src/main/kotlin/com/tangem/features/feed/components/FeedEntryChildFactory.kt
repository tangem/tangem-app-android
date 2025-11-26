package com.tangem.features.feed.components

import androidx.compose.runtime.Immutable
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.navigation.Route
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.TokenMarketParams
import com.tangem.features.feed.components.feed.DefaultFeedComponent
import com.tangem.features.feed.components.market.details.DefaultMarketsTokenDetailsComponent
import com.tangem.features.feed.components.market.list.DefaultMarketsTokenListComponent
import com.tangem.features.feed.components.news.details.DefaultNewsDetailsComponent
import com.tangem.features.feed.components.news.list.DefaultNewsListComponent
import kotlinx.serialization.Serializable

internal class FeedEntryChildFactory {

    @Serializable
    @Immutable
    sealed interface Child : Route {

        @Serializable
        @Immutable
        data object Feed : Child

        @Serializable
        @Immutable
        data object TokenList : Child

        @Serializable
        @Immutable
        data class TokenDetails(val params: DefaultMarketsTokenDetailsComponent.Params) : Child

        @Serializable
        @Immutable
        data object NewsList : Child

        @Serializable
        @Immutable
        data object NewsDetails : Child
    }

    fun createChild(
        child: Child,
        appComponentContext: AppComponentContext,
        onTokenClick: (TokenMarketParams, AppCurrency) -> Unit,
    ): Any {
        return when (child) {
            is Child.TokenDetails -> {
                DefaultMarketsTokenDetailsComponent(
                    appComponentContext = appComponentContext,
                    params = child.params,
                )
            }
            is Child.TokenList -> {
                DefaultMarketsTokenListComponent(
                    appComponentContext = appComponentContext,
                    onTokenClick = onTokenClick,
                )
            }
            Child.NewsDetails -> {
                DefaultNewsDetailsComponent(
                    appComponentContext = appComponentContext,
                )
            }
            Child.NewsList -> {
                DefaultNewsListComponent(
                    appComponentContext = appComponentContext,
                )
            }
            Child.Feed -> {
                DefaultFeedComponent(
                    appComponentContext = appComponentContext,
                )
            }
        }
    }
}