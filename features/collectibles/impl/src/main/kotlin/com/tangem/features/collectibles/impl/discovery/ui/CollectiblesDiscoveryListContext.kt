package com.tangem.features.collectibles.impl.discovery.ui

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Frame-scoped rendering inputs the host owns — pinning, search-fold visibility, status sizing.
 *
 * @property isTabBandPinned the band is docked under the top bar: the inline item degrades to a spacer
 * @property isTabBandShown `false` only in search mode entered past the band — the item renders zero-height,
 *   so the copied scroll position of the search list keeps its meaning
 * @property leadingBlocksAlpha fades the band out over the tail of the scroll into search, so it is already
 *   invisible by the time the item is dropped
 * @property statusHeight min height of the loading/error/empty box, measured by the host so its content centres
 *   in what is left of the viewport
 */
@Immutable
internal data class CollectiblesDiscoveryListContext(
    val isTabBandPinned: Boolean,
    val isTabBandShown: Boolean,
    val leadingBlocksAlpha: Float,
    val statusHeight: Dp,
) {

    companion object {
        val Default = CollectiblesDiscoveryListContext(
            isTabBandPinned = false,
            isTabBandShown = true,
            leadingBlocksAlpha = 1f,
            statusHeight = 0.dp,
        )
    }
}

internal val LocalCollectiblesDiscoveryListContext = compositionLocalOf { CollectiblesDiscoveryListContext.Default }