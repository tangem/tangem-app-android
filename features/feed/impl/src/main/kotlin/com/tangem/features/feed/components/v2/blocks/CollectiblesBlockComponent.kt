package com.tangem.features.feed.components.v2.blocks

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorComponent
import com.tangem.features.feed.components.v2.blocks.model.CollectiblesBlockModel
import com.tangem.features.feed.ui.v2.FeedTitleCard
import kotlinx.coroutines.flow.StateFlow

/**
 * Entry into Collectibles, and the wallet chooser it opens when more than one wallet is eligible —
 * see [CollectiblesBlockModel] for how the wallet and the screen are settled.
 *
 * A title-only card until the feature contributes its own pitch.
 */
internal class CollectiblesBlockComponent(
    context: AppComponentContext,
    private val portfolioSelectorComponentFactory: PortfolioSelectorComponent.Factory,
) : FeedBlockComponent(context = context, key = KEY) {

    private val model: CollectiblesBlockModel = getOrCreateModel()

    override val size: FeedBlock.Size = FeedBlock.Size.Large

    override val isVisible: StateFlow<Boolean> = model.isVisible

    private val bottomSheetSlot = childSlot(
        source = model.bottomSheetNavigation,
        serializer = null,
        handleBackButton = false,
    ) { _, componentContext -> walletSelectorChild(componentContext) }

    @Composable
    override fun Content(modifier: Modifier) {
        // TODO: [TWI-1608] real Collectibles pitch and its Lokalise key
        FeedTitleCard(
            title = "Collectibles",
            onClick = model::onClick,
            modifier = modifier,
        )
    }

    @Composable
    override fun Overlay() {
        val bottomSheet by bottomSheetSlot.subscribeAsState()

        bottomSheet.child?.instance?.BottomSheet()
    }

    private fun walletSelectorChild(componentContext: ComponentContext): ComposableBottomSheetComponent =
        portfolioSelectorComponentFactory.create(
            context = childByContext(componentContext),
            params = PortfolioSelectorComponent.Params(
                portfolioFetcher = model.portfolioFetcher,
                controller = model.portfolioSelectorController,
                bsCallback = model.portfolioSelectorCallback,
                settings = PortfolioSelectorComponent.Settings(isWalletSelectionOnly = true, isDs3Modal = true),
            ),
        )

    /** The block's only sheet: the chooser of the wallet the collectibles open on. */
    data object WalletSelectorConfig

    companion object {
        const val KEY = "collectibles"
    }
}