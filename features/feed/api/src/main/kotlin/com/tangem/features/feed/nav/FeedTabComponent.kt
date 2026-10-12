package com.tangem.features.feed.nav

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState

/**
 * Contract of a feed tab page
 *
 * A tab owns *scrolling*, not *navigation*: it renders its own vertical list (pagination included),
 * while any full-screen destination is a [FeedRoute] pushed to the injected router — it lands on
 * the feed stack and covers the whole feed, tabs included.
 */
interface FeedTabComponent {

    /**
     * @param bottomSheetState expanded/collapsed state of the hosting shtorka; the tab is visible
     * only when expanded
     * @param contentPadding insets of the feed chrome pinned above the tab's list
     */
    @Composable
    fun Content(bottomSheetState: State<BottomSheetState>, contentPadding: PaddingValues, modifier: Modifier)

    /**
     * Returns the tab's list to its top.
     *
     * Called on every tab of the feed home when the hosting shtorka leaves the expanded state: the
     * top blocks re-expand with it and are drawn over the list, so a tab left scrolled would come
     * back with its middle under them. Search tabs never render under the blocks — hence the no-op
     * default.
     */
    fun resetScroll() {}
}