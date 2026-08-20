package com.tangem.features.feed.earn.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.features.commonfeatures.api.addtoportfolio.AddToPortfolioComponent
import com.tangem.features.feed.earn.model.EarnFeedTabModel
import com.tangem.features.feed.earn.ui.EarnFeedTabContent
import com.tangem.features.feed.nav.FeedTabComponent
import com.tangem.features.promobanners.api.PromoBannersBlockComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

@Stable
internal class EarnFeedTabComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Suppress("UnusedPrivateProperty") @Assisted params: Unit,
    promoBannersBlockComponentFactory: PromoBannersBlockComponent.Factory,
    private val addToPortfolioComponentFactory: AddToPortfolioComponent.Factory,
) : FeedTabComponent, AppComponentContext by context {

    private val model: EarnFeedTabModel = getOrCreateModel()

    private val promoBannersBlockComponent: PromoBannersBlockComponent = promoBannersBlockComponentFactory.create(
        context = child("promoBannersBlockComponent"),
        params = PromoBannersBlockComponent.Params(
            placeholder = PromoBannersBlockComponent.Placeholder.FEED,
            isInitiallyVisibleOnScreen = false,
        ),
    )

    // owned by the component: the page stays CREATED while unselected, so scroll survives switches
    private val listState = LazyListState()

    private val bottomSheetSlot = childSlot(
        source = model.bottomSheetNavigation,
        serializer = null,
        handleBackButton = false,
        childFactory = ::bottomSheetChild,
    )

    @Composable
    override fun Content(bottomSheetState: State<BottomSheetState>, contentPadding: PaddingValues, modifier: Modifier) {
        val state by model.state.collectAsStateWithLifecycle()
        val bottomSheet by bottomSheetSlot.subscribeAsState()

        val sheetState = bottomSheetState.value
        LaunchedEffect(sheetState) {
            promoBannersBlockComponent.setVisibleOnScreen(isVisible = sheetState == BottomSheetState.EXPANDED)
        }

        LifecycleStartEffect(Unit) {
            model.onTabShown()
            onStopOrDispose {}
        }

        EarnFeedTabContent(
            state = state,
            listState = listState,
            contentPadding = contentPadding,
            modifier = modifier,
            promoBanners = { bannersModifier ->
                promoBannersBlockComponent.ContentWithPadding(
                    horizontalItemPadding = 16.dp,
                    walletId = null,
                    modifier = bannersModifier,
                )
            },
        )
        bottomSheet.child?.instance?.BottomSheet()
    }

    private fun bottomSheetChild(
        config: EarnBottomSheetRoute,
        componentContext: ComponentContext,
    ): ComposableBottomSheetComponent = when (config) {
        is EarnBottomSheetRoute.AddToPortfolio -> {
            addToPortfolioComponentFactory.create(
                context = childByContext(componentContext),
                params = AddToPortfolioComponent.Params(addToPortfolioManager = config.manager),
            )
        }
        is EarnBottomSheetRoute.NetworkFilter -> EarnNetworkFilterComponent(
            context = childByContext(componentContext),
            params = config.params,
        )
        is EarnBottomSheetRoute.TypeFilter -> EarnTypeFilterComponent(
            context = childByContext(componentContext),
            params = config.params,
        )
    }

    @AssistedFactory
    interface Factory {
        fun create(context: AppComponentContext, params: Unit): EarnFeedTabComponent
    }
}