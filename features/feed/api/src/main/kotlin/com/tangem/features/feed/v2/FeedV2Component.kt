package com.tangem.features.feed.v2

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState

/**
 * Feed content of the Shtorka 2.0
 */
@Stable
interface FeedV2Component {

    /**
     * Content of the host's pinned sheet header: the feed's search bar, or the active feed
     * screen's own navbar when one covers the feed (e.g. token details). The host renders it
     * next to its grabber; the composable applies its own horizontal insets.
     *
     * @param bottomSheetState expanded/collapsed state of the hosting shtorka
     * @param onExpandSheet raises the hosting shtorka to full; invoked when the search bar is focused
     */
    @Composable
    fun Header(bottomSheetState: State<BottomSheetState>, onExpandSheet: () -> Unit, modifier: Modifier)

    /**
     * @param bottomSheetState expanded/collapsed state of the hosting shtorka. While not expanded
     * (collapsed or semi-open) only the top blocks and the tab row are shown; the selected tab's
     * list renders only when expanded
     * @param contentPadding insets of the host's pinned chrome (top: grabber + [Header]); the
     * scrollable content draws beneath it and offsets by it
     * @param onExpandSheet raises the hosting shtorka to full; invoked when a tab is tapped
     */
    @Composable
    fun Content(
        bottomSheetState: State<BottomSheetState>,
        contentPadding: PaddingValues,
        onExpandSheet: () -> Unit,
        modifier: Modifier,
    )

    interface Factory : ComponentFactory<Unit, FeedV2Component>

    companion object {

        /** Height of the top blocks row (a column of two small blocks / one large block). */
        val TopBlocksHeight: Dp = 144.dp

        /** Height of the tab row, including its vertical insets. */
        val TabRowHeight: Dp = 60.dp

        /**
         * Exact height of the semi-open layout below the host's sheet header: the top blocks and
         * the tab row, no tab content. The host sizes the shtorka's semi-open detent with it.
         */
        val SemiOpenContentHeight: Dp = TopBlocksHeight + TabRowHeight
    }
}