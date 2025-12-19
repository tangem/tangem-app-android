package com.tangem.features.feed.components

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.popWhile
import com.arkivanov.decompose.value.Value
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.navigation.inner.InnerRouter
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.core.ui.decompose.ComposableModularContentComponent
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.TokenMarketParams
import com.tangem.features.feed.components.market.details.DefaultMarketsTokenDetailsComponent
import com.tangem.features.feed.components.market.list.DefaultMarketsTokenListComponent
import com.tangem.features.feed.entry.components.FeedEntryComponent
import com.tangem.features.feed.model.feed.FeedModelClickIntents
import com.tangem.features.feed.ui.EntryBottomSheetContent
import com.tangem.features.feed.ui.market.list.state.SortByTypeUM
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

@Stable
internal class DefaultFeedEntryComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    private val feedEntryChildFactory: FeedEntryChildFactory,
) : FeedEntryComponent, AppComponentContext by context {

    private val stackNavigation = StackNavigation<FeedEntryChildFactory.Child>()

    private val innerRouter = InnerRouter<FeedEntryChildFactory.Child>(
        stackNavigation = stackNavigation,
        popCallback = { onChildBack() },
    )

    private val clickIntents = object : FeedEntryClickIntents {
        override fun onMarketItemClick(token: TokenMarketParams, appCurrency: AppCurrency) {
            innerRouter.push(
                route = FeedEntryChildFactory.Child.TokenDetails(
                    params = DefaultMarketsTokenDetailsComponent.Params(
                        token = token,
                        appCurrency = appCurrency,
                        shouldShowPortfolio = true,
                        analyticsParams = DefaultMarketsTokenDetailsComponent.AnalyticsParams(
                            blockchain = null,
                            source = "Market",
                        ),
                    ),
                ),
            )
        }

        override fun onMarketOpenClick(sortBy: SortByTypeUM?) {
            innerRouter.push(
                route = FeedEntryChildFactory.Child.TokenList(
                    params = DefaultMarketsTokenListComponent.Params(
                        onBackClicked = { onChildBack() },
                        onTokenClick = { token, currency -> onMarketItemClick(token, currency) },
                        preselectedSortType = sortBy ?: SortByTypeUM.Rating,
                        shouldAlwaysShowSearchBar = sortBy == null,
                    ),
                ),
            )
        }

        override fun onArticleClick(articleId: Int) {
            innerRouter.push(FeedEntryChildFactory.Child.NewsDetails)
        }

        override fun onOpenAllNews() {
            innerRouter.push(FeedEntryChildFactory.Child.NewsList)
        }
    }

    private val stack: Value<ChildStack<FeedEntryChildFactory.Child, ComposableModularContentComponent>> = childStack(
        key = "main",
        source = stackNavigation,
        serializer = FeedEntryChildFactory.Child.serializer(),
        initialConfiguration = FeedEntryChildFactory.Child.Feed,
        handleBackButton = false,
        childFactory = { configuration, factoryContext ->
            feedEntryChildFactory.createChild(
                child = configuration,
                appComponentContext = childByContext(
                    componentContext = factoryContext,
                    router = innerRouter,
                ),
                feedEntryClickIntents = clickIntents,
            )
        },
    )

    @Composable
    override fun BottomSheetContent(
        bottomSheetState: State<BottomSheetState>,
        onHeaderSizeChange: (Dp) -> Unit,
        modifier: Modifier,
    ) {
        val stackState by stack.subscribeAsState()

        BackHandler(enabled = bottomSheetState.value == BottomSheetState.EXPANDED) {
            onChildBack()
        }

        EntryBottomSheetContent(
            stackState = stackState,
            onHeaderSizeChange = onHeaderSizeChange,
        )
    }

    private fun onChildBack() {
        if (stack.value.active.configuration !is FeedEntryChildFactory.Child.Feed) {
            stackNavigation.popWhile { it != FeedEntryChildFactory.Child.Feed }
        }
    }

    @AssistedFactory
    interface Factory : FeedEntryComponent.Factory {
        override fun create(context: AppComponentContext): DefaultFeedEntryComponent
    }
}

internal interface FeedEntryClickIntents : FeedModelClickIntents