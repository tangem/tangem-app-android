package com.tangem.features.feed.components.v2

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorComponent
import com.tangem.features.feed.model.v2.FeedTopBlocksModel
import com.tangem.features.feed.nav.FeedRoute
import com.tangem.features.feed.ui.v2.FeedTopBlocksContent

/**
 * "Part 1" of the Shtorka 2.0 feed: the horizontally scrollable row of top blocks (For you,
 * All services, promo cards, …). Owns its content end to end so blocks can later become
 * contributed plug-ins without touching [DefaultFeedV2Component].
 */
@Stable
internal class FeedTopBlocksComponent(
    context: AppComponentContext,
    private val isPredictionsEntryVisible: Boolean,
    private val isCollectiblesVisible: Boolean,
    private val portfolioSelectorComponentFactory: PortfolioSelectorComponent.Factory,
) : ComposableContentComponent, AppComponentContext by context {

    private val model: FeedTopBlocksModel = getOrCreateModel()

    private val bottomSheetSlot = childSlot(
        source = model.bottomSheetNavigation,
        serializer = null,
        handleBackButton = false,
        childFactory = ::bottomSheetChild,
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val bottomSheet by bottomSheetSlot.subscribeAsState()

        // TODO: [TWI-1608] real blocks; static placeholders until then
        FeedTopBlocksContent(
            onForYouClick = { router.push(FeedRoute.ForYou) },
            isPredictionsVisible = isPredictionsEntryVisible,
            isCollectiblesVisible = isCollectiblesVisible,
            onPredictionsClick = { router.push(AppRoute.Polymarket()) },
            onCollectiblesClick = model::onCollectiblesClick,
            modifier = modifier,
        )

        bottomSheet.child?.instance?.BottomSheet()
    }

    private fun bottomSheetChild(
        config: FeedTopBlocksBottomSheetConfig,
        componentContext: ComponentContext,
    ): ComposableBottomSheetComponent = when (config) {
        FeedTopBlocksBottomSheetConfig.WalletSelector -> portfolioSelectorComponentFactory.create(
            context = childByContext(componentContext),
            params = PortfolioSelectorComponent.Params(
                portfolioFetcher = model.portfolioFetcher,
                controller = model.portfolioSelectorController,
                bsCallback = model.portfolioSelectorCallback,
                settings = PortfolioSelectorComponent.Settings(isWalletSelectionOnly = true),
            ),
        )
    }
}