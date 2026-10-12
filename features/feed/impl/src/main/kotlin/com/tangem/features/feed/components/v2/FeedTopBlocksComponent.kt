package com.tangem.features.feed.components.v2

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.ds2.shtorka.ShtorkaExpansion
import com.tangem.feature.walletsettings.component.AddAccountTypeComponent
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorComponent
import com.tangem.features.feed.components.v2.blocks.*
import com.tangem.features.feed.components.v2.blocks.analytics.FeedBlocksAnalytics
import com.tangem.features.feed.components.v2.blocks.analytics.ProductCard
import com.tangem.features.feed.ui.v2.FeedTopBlocksContent
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

/**
 * "Part 1" of the Shtorka 2.0 feed: the horizontally scrollable row of top blocks.
 *
 * The row is a host for contributed blocks and knows nothing about any of them — each block is a
 * component of its own that decides when it is shown and what it draws (see [FeedBlock]), so a new
 * card touches only its own files and the list below.
 *
 * Order is the declaration order of [blocks] — a host decision, like `FeedTabsOrder` for the tabs.
 */
@Stable
internal class FeedTopBlocksComponent(
    context: AppComponentContext,
    params: Params,
    portfolioSelectorComponentFactory: PortfolioSelectorComponent.Factory,
    addAccountTypeComponentFactory: AddAccountTypeComponent.Factory,
    private val blocksAnalytics: FeedBlocksAnalytics,
) : ComposableContentComponent, AppComponentContext by context {

    private val blocks: List<FeedBlock> = listOf(
        ForYouBlockComponent(
            context = child(key = ForYouBlockComponent.KEY),
            blocksAnalytics = blocksAnalytics,
        ),
        AddAccountBlockComponent(
            context = child(key = AddAccountBlockComponent.KEY),
            portfolioSelectorComponentFactory = portfolioSelectorComponentFactory,
            addAccountTypeComponentFactory = addAccountTypeComponentFactory,
            blocksAnalytics = blocksAnalytics,
        ),
        TangemPayBlockComponent(
            context = child(key = TangemPayBlockComponent.KEY),
            blocksAnalytics = blocksAnalytics,
        ),
        EarnBlockComponent(
            context = child(key = EarnBlockComponent.KEY),
            callbacks = params.callbacks,
            blocksAnalytics = blocksAnalytics,
        ),
        PredictionsBlockComponent(context = child(key = PredictionsBlockComponent.KEY)),
        CollectiblesBlockComponent(
            context = child(key = CollectiblesBlockComponent.KEY),
            portfolioSelectorComponentFactory = portfolioSelectorComponentFactory,
        ),
        ReferralBlockComponent(
            context = child(key = ReferralBlockComponent.KEY),
            portfolioSelectorComponentFactory = portfolioSelectorComponentFactory,
            blocksAnalytics = blocksAnalytics,
        ),
    )

    private val blocksOnScreen = MutableStateFlow<Set<String>>(emptySet())

    private val visibleBlocks: StateFlow<ImmutableList<FeedBlock>> = combine(
        flows = blocks.map { it.isVisible },
    ) { visibility ->
        blocks.filterIndexed { index, _ -> visibility[index] }.toImmutableList()
    }.stateIn(scope = componentScope, started = SharingStarted.Eagerly, initialValue = persistentListOf())

    init {
        combine(blocksOnScreen, blocksAnalytics.shtorkaExpansion, ::shownProductCards)
            .onEach { productCards -> productCards.forEach(blocksAnalytics::onProductCardShown) }
            .launchIn(componentScope)
    }

    private fun shownProductCards(keysOnScreen: Set<String>, expansion: ShtorkaExpansion): List<ProductCard> {
        if (expansion == ShtorkaExpansion.PILL) return emptyList()

        return blocks.filter { it.key in keysOnScreen }.mapNotNull { it.productCard }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val visible by visibleBlocks.collectAsStateWithLifecycle()

        FeedTopBlocksContent(
            blocks = visible,
            onBlocksOnScreenChange = { keys -> blocksOnScreen.value = keys },
            modifier = modifier,
        )
        // Outside the row: it is lazy, so a card scrolled out of sight would take its sheet with it.
        // Drawn for every block, not only the visible ones — a block that hides itself while its
        // sheet is up should not yank the sheet away from under the user.
        blocks.forEach { block -> key(block.key) { block.Overlay() } }
    }

    data class Params(
        val callbacks: FeedBlockModalCallbacks,
    )
}