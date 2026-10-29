package com.tangem.features.feed.components.v2.blocks

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.account.CollectiblesAccountIcon
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.ds2.ambient.TangemAmbientBackground
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorComponent
import com.tangem.features.feed.components.v2.blocks.model.CollectiblesBlockModel
import com.tangem.features.feed.impl.R
import com.tangem.features.feed.ui.v2.FeedBlockIconBadge
import com.tangem.features.feed.ui.v2.FeedPromoCard
import kotlinx.coroutines.flow.StateFlow

/**
 * Entry into Collectibles, and the wallet chooser it opens when more than one wallet is eligible —
 * see [CollectiblesBlockModel] for how the wallet and the screen are settled.
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
        FeedPromoCard(
            title = stringResourceSafe(R.string.collectibles_entrance_card_title),
            description = stringResourceSafe(R.string.collectibles_entrance_card_subtitle),
            ambientVariant = TangemAmbientBackground.Variant.Yellow,
            onClick = model::onClick,
            modifier = modifier,
            icon = { CollectiblesIcon() },
        )
    }

    @Composable
    private fun CollectiblesIcon() {
        FeedBlockIconBadge(background = CollectiblesAccountIcon.color) {
            Icon(
                imageVector = CollectiblesAccountIcon.imageVector,
                contentDescription = null,
                tint = TangemTheme.colors3.icon.staticDark,
                modifier = Modifier.size(size = 24.dp),
            )
        }
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