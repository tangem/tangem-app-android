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
import com.tangem.feature.walletsettings.component.AddAccountTypeComponent
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorComponent
import com.tangem.features.feed.components.v2.blocks.*
import com.tangem.features.feed.ui.v2.FeedTopBlocksContent
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
    portfolioSelectorComponentFactory: PortfolioSelectorComponent.Factory,
    addAccountTypeComponentFactory: AddAccountTypeComponent.Factory,
) : ComposableContentComponent, AppComponentContext by context {

    private val blocks: List<FeedBlock> = listOf(
        ForYouBlockComponent(context = child(key = ForYouBlockComponent.KEY)),
        AddAccountBlockComponent(
            context = child(key = AddAccountBlockComponent.KEY),
            portfolioSelectorComponentFactory = portfolioSelectorComponentFactory,
            addAccountTypeComponentFactory = addAccountTypeComponentFactory,
        ),
        PredictionsBlockComponent(context = child(key = PredictionsBlockComponent.KEY)),
        CollectiblesBlockComponent(
            context = child(key = CollectiblesBlockComponent.KEY),
            portfolioSelectorComponentFactory = portfolioSelectorComponentFactory,
        ),
    )

    private val visibleBlocks: StateFlow<ImmutableList<FeedBlock>> = combine(
        flows = blocks.map { it.isVisible },
    ) { visibility ->
        blocks.filterIndexed { index, _ -> visibility[index] }.toImmutableList()
    }.stateIn(scope = componentScope, started = SharingStarted.Eagerly, initialValue = persistentListOf())

    @Composable
    override fun Content(modifier: Modifier) {
        val visible by visibleBlocks.collectAsStateWithLifecycle()

        FeedTopBlocksContent(blocks = visible, modifier = modifier)
        // Outside the row: it is lazy, so a card scrolled out of sight would take its sheet with it.
        // Drawn for every block, not only the visible ones — a block that hides itself while its
        // sheet is up should not yank the sheet away from under the user.
        blocks.forEach { block -> key(block.key) { block.Overlay() } }
    }
}