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
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.decompose.ComposableModularBottomSheetContentComponent
import com.tangem.core.ui.decompose.EmptyComposableBottomSheetComponent
import com.tangem.features.promobanners.api.NewPromoBannersFeatureToggles
import com.tangem.features.promobanners.api.PromoBannersBlockComponent
import com.tangem.features.feed.components.market.details.portfolio.add.AddToPortfolioPreselectedDataComponent
import com.tangem.features.feed.components.market.details.portfolio.add.AddToPortfolioPreselectedDataComponent.Params
import com.tangem.features.feed.model.feed.FeedComponentModel
import com.tangem.features.feed.model.feed.FeedModelClickIntents
import com.tangem.features.feed.ui.feed.FeedList
import com.tangem.features.feed.ui.feed.FeedListHeader

internal class DefaultFeedComponent(
    appComponentContext: AppComponentContext,
    private val params: FeedParams,
    private val addToPortfolioComponentFactory: AddToPortfolioPreselectedDataComponent.Factory,
    private val promoBannersBlockComponentFactory: PromoBannersBlockComponent.Factory,
    private val newPromoBannersFeatureToggles: NewPromoBannersFeatureToggles,
) : ComposableModularBottomSheetContentComponent, AppComponentContext by appComponentContext {

    private val feedComponentModel = getOrCreateModel<FeedComponentModel, FeedParams>(params = params)

    private val promoBannersBlockComponent: PromoBannersBlockComponent? by lazy {
        if (!newPromoBannersFeatureToggles.isNewPromoBannersEnabled) return@lazy null
        promoBannersBlockComponentFactory.create(
            context = child("promoBannersBlockComponent"),
            params = PromoBannersBlockComponent.Params(
                placeholder = PromoBannersBlockComponent.Placeholder.FEED,
            ),
        )
    }

    private val bottomSheetSlot = childSlot(
        source = feedComponentModel.bottomSheetNavigation,
        serializer = null,
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
            promoBannersBlockComponent = promoBannersBlockComponent,
        )
        bottomSheet.child?.instance?.BottomSheet()
    }

    private fun bottomSheetChild(
        config: FeedBottomSheetRoute,
        componentContext: ComponentContext,
    ): ComposableBottomSheetComponent = when (config) {
        is FeedBottomSheetRoute.AddToPortfolio -> {
            addToPortfolioComponentFactory.create(
                context = childByContext(componentContext),
                params = Params(
                    tokenToAdd = config.tokenToAdd,
                    callback = feedComponentModel.addToPortfolioCallback,
                    analyticsParams = AddToPortfolioPreselectedDataComponent.AnalyticsParams(
                        AnalyticsParam.ScreensSources.Markets.value,
                    ),
                ),
            )
        }
        is FeedBottomSheetRoute.NetworkFilter -> EmptyComposableBottomSheetComponent
        is FeedBottomSheetRoute.TypeFilter -> EmptyComposableBottomSheetComponent
    }

    data class FeedParams(val feedClickIntents: FeedModelClickIntents)
}