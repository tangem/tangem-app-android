package com.tangem.features.feed.components.v2.blocks

import androidx.compose.runtime.Composable
import com.tangem.core.decompose.context.AppComponentContext

/**
 * Base of the feed's own blocks: a block is a Decompose component living on the child context the row
 * creates for it, keyed by [key].
 *
 * That context is the whole point of making a card a component — the block gets a lifecycle that ends
 * with the row's, a model of its own retained across re-creation, and its own slots and back handling,
 * instead of borrowing the row's.
 */
internal abstract class FeedBlockComponent(
    context: AppComponentContext,
    override val key: String,
) : FeedBlock, AppComponentContext by context {

    @Composable
    override fun Overlay() {
        /* no-op */
    }
}