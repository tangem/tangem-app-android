package com.tangem.features.feed.components.earn

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.decompose.ComposableModularBottomSheetContentComponent
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.LocalMainBottomSheetColor
import com.tangem.features.feed.components.feed.FeedBottomSheetRoute
import com.tangem.features.feed.components.market.details.portfolio.add.AddToPortfolioPreselectedDataComponent
import com.tangem.features.feed.model.earn.EarnModel
import com.tangem.features.feed.ui.earn.EarnContent
import kotlinx.serialization.Serializable

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

    @Composable
    override fun Content(bottomSheetState: State<BottomSheetState>, modifier: Modifier) {
        val bottomSheet by bottomSheetSlot.subscribeAsState()
        val state by earnModel.state.collectAsStateWithLifecycle()

        EarnContent(
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

    @Serializable
    data class Params(
        val onBackClick: () -> Unit,
    )
}