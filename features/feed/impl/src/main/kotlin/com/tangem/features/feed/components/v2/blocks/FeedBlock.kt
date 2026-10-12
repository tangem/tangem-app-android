package com.tangem.features.feed.components.v2.blocks

import androidx.compose.runtime.Composable
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.feed.components.v2.blocks.analytics.ProductCard
import com.tangem.features.feed.nav.FeedTabId
import kotlinx.coroutines.flow.StateFlow

/**
 * One card of the feed's top-blocks row.
 *
 * A block owns both halves of itself: when it belongs in the row ([isVisible]) and how it draws
 * ([Content]), including its own data, clicks and navigation. The row's host only orders the blocks
 * and hands each one the size its [size] class is laid out at — it knows nothing about what any of
 * them shows.
 *
 * Blocks of this feed are Decompose components with a context, lifecycle and model of their own —
 * see [FeedBlockComponent].
 */
internal interface FeedBlock : ComposableContentComponent {

    /** Stable identity, used both for the block's child context and for the row's item key. */
    val key: String

    val productCard: ProductCard?
        get() = null

    /** How much room the card takes. */
    val size: Size

    /**
     * Whether the card belongs in the row at all, re-evaluated as the block's data changes. A card
     * that is never hidden stays `true`.
     */
    val isVisible: StateFlow<Boolean>

    /**
     * Windows the block opens over the feed — a bottom sheet, a dialog. Drawn by the host outside the
     * row, which is lazy: a card scrolled out of sight is disposed, and would take its sheet with it.
     */
    @Composable
    fun Overlay()

    enum class Size {

        /** Half-height shortcut; consecutive small blocks are stacked into one column. */
        Small,

        /** Full-height promo card, alone in its column. */
        Large,
    }
}

/** What a block can ask of the feed host — anything it can't reach through the router. */
internal interface FeedBlockModalCallbacks {

    /** Brings one of the feed's own tabs to front; tabs are pages of the host's pager, not routes. */
    fun onOpenTab(tab: FeedTabId)
}