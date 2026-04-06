package com.tangem.features.feed.components.earn

import androidx.compose.animation.core.EaseOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.R
import com.tangem.core.ui.components.appbar.TangemTopAppBar
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.core.ui.components.haze.hazeEffectTangem
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.decompose.ComposableModularBottomSheetContentComponent
import com.tangem.core.ui.extensions.clickableSingle
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.LocalMainBottomSheetColor
import com.tangem.core.ui.res.LocalRedesignEnabled
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.feed.components.feed.FeedBottomSheetRoute
import com.tangem.features.feed.components.market.details.portfolio.add.AddToPortfolioPreselectedDataComponent
import com.tangem.features.feed.model.earn.EarnModel
import com.tangem.features.feed.ui.components.FeedSearchBar
import com.tangem.features.feed.ui.earn.EarnContent
import dev.chrisbanes.haze.HazeProgressive

internal class DefaultEarnComponent(
    appComponentContext: AppComponentContext,
    private val params: Params,
    private val addToPortfolioComponentFactory: AddToPortfolioPreselectedDataComponent.Factory,
) : ComposableModularBottomSheetContentComponent, AppComponentContext by appComponentContext {

    private val earnModel = getOrCreateModel<EarnModel, Params>(params = params)

    private val bottomSheetSlot = childSlot(
        source = earnModel.bottomSheetNavigation,
        serializer = null,
        handleBackButton = false,
        childFactory = ::bottomSheetChild,
    )

    @Composable
    override fun Title(bottomSheetState: State<BottomSheetState>) {
        val background = LocalMainBottomSheetColor.current.value
        val state by earnModel.state.collectAsStateWithLifecycle()
        if (LocalRedesignEnabled.current) {
            FeedSearchBar(
                isSearchBarClickable = bottomSheetState.value == BottomSheetState.EXPANDED,
                feedListSearchBar = state.feedListSearchBar,
                modifier = Modifier
                    .drawBehind { drawRect(background) }
                    .hazeEffectTangem {
                        progressive = HazeProgressive.verticalGradient(
                            startIntensity = .55f,
                            endIntensity = 0f,
                            preferPerformance = true,
                            easing = EaseOut,
                        )
                    },
                startContent = {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_arrow_back_28),
                        contentDescription = null,
                        tint = TangemTheme.colors2.graphic.neutral.primary,
                        modifier = Modifier
                            .size(TangemTheme.dimens2.x11)
                            .background(
                                color = TangemTheme.colors2.button.backgroundSecondary,
                                shape = CircleShape,
                            )
                            .clickableSingle(
                                onClick = state.onBackClick,
                                enabled = bottomSheetState.value == BottomSheetState.EXPANDED,
                            )
                            .padding(TangemTheme.dimens2.x2),
                    )
                },
            )
        } else {
            TangemTopAppBar(
                containerColor = background,
                title = stringResourceSafe(R.string.earn_title),
                startButton = TopAppBarButtonUM.Icon(
                    iconRes = R.drawable.ic_back_24,
                    onClicked = state.onBackClick,
                    isEnabled = bottomSheetState.value == BottomSheetState.EXPANDED,
                ),
            )
        }
    }

    @Composable
    override fun Content(
        bottomSheetState: State<BottomSheetState>,
        contentPadding: PaddingValues,
        modifier: Modifier,
    ) {
        val bottomSheet by bottomSheetSlot.subscribeAsState()
        val state by earnModel.state.collectAsStateWithLifecycle()

        EarnContent(
            contentPadding = contentPadding,
            state = state,
            modifier = modifier,
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
                params = AddToPortfolioPreselectedDataComponent.Params(
                    tokenToAdd = config.tokenToAdd,
                    callback = earnModel.addToPortfolioCallback,
                    analyticsParams = AddToPortfolioPreselectedDataComponent.AnalyticsParams(config.source),
                ),
            )
        }
        is FeedBottomSheetRoute.NetworkFilter -> EarnNetworkFilterComponent(
            context = childByContext(componentContext),
            params = config.params,
        )
        is FeedBottomSheetRoute.TypeFilter -> EarnTypeFilterComponent(
            context = childByContext(componentContext),
            params = config.params,
        )
    }

    data class Params(
        val onBackClick: () -> Unit,
        val onSearchClicked: () -> Unit,
    )
}