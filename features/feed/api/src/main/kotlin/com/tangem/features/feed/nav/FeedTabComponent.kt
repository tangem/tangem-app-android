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
}