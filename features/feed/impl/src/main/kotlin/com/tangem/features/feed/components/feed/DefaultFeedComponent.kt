package com.tangem.features.feed.components.feed

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.decompose.ComposableModularBottomSheetContentComponent
import com.tangem.features.feed.components.market.details.portfolio.add.AddToPortfolioPreselectedDataComponent
import com.tangem.features.feed.model.feed.FeedComponentModel
import com.tangem.features.feed.model.feed.FeedModelClickIntents
import com.tangem.features.feed.ui.feed.FeedList
import com.tangem.features.feed.ui.feed.FeedListHeader

internal class DefaultFeedComponent(
    appComponentContext: AppComponentContext,
    private val params: FeedParams,
    private val addToPortfolioComponentFactory: AddToPortfolioPreselectedDataComponent.Factory,
) : ComposableModularBottomSheetContentComponent, AppComponentContext by appComponentContext {

    private val feedComponentModel = getOrCreateModel<FeedComponentModel, FeedParams>(params = params)

    private val bottomSheetSlot = childSlot(
        source = feedComponentModel.bottomSheetNavigation,
        serializer = FeedPortfolioRoute.serializer(),
        handleBackButton = false,
        childFactory = ::bottomSheetChild,
    )

    @Composable
    override fun Title(bottomSheetState: State<BottomSheetState>) {
        val state by feedComponentModel.state.collectAsStateWithLifecycle()
        FeedListHeader(
            isSearchBarClickable = bottomSheetState.value == BottomSheetState.EXPANDED,
            feedListSearchBar = state.feedListSearchBar,
        )
    }

    @Composable
    override fun Content(bottomSheetState: State<BottomSheetState>, modifier: Modifier) {
        LifecycleStartEffect(Unit) {
            feedComponentModel.isVisibleOnScreen.value = true
            onStopOrDispose {
                feedComponentModel.isVisibleOnScreen.value = false
            }
        }

        val bottomSheet by bottomSheetSlot.subscribeAsState()
        val state by feedComponentModel.state.collectAsStateWithLifecycle()
        FeedList(
            modifier = modifier,
            state = state,
        )
        bottomSheet.child?.instance?.BottomSheet()
    }

    private fun bottomSheetChild(
        config: FeedPortfolioRoute,
        componentContext: ComponentContext,
    ): ComposableBottomSheetComponent = when (config) {
        is FeedPortfolioRoute.AddToPortfolio -> {
            addToPortfolioComponentFactory.create(
                context = childByContext(componentContext),
                params = AddToPortfolioPreselectedDataComponent.Params(
                    tokenToAdd = config.tokenToAdd,
                    callback = feedComponentModel.addToPortfolioCallback,
                ),
            )
        }
    }

    data class FeedParams(val feedClickIntents: FeedModelClickIntents)
}