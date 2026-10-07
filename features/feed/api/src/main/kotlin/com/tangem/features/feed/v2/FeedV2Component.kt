package com.tangem.features.feed.v2

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.value.Value
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.core.ui.ds2.shtorka.ShtorkaExpansion
import kotlinx.coroutines.flow.StateFlow

/**
 * Feed content of the Shtorka 2.0
 */
@Stable
interface FeedV2Component {

    val isPartialExpansionSupported: Value<Boolean>

    /**
     * Whether the active feed screen keeps the hosting shtorka's blurred glass material at
     * partial detents. Only the feed home and the search keep the glass; a screen pushed over
     * them (news details, token details, …) renders the card as a flat opaque surface instead.
     */
    val isSheetBlurEnabled: StateFlow<Boolean>

    /**
     * Content of the host's pinned sheet header: the feed's search bar, or the active feed
     * screen's own navbar when one covers the feed (e.g. token details). The host renders it
     * next to its grabber; the composable applies its own horizontal insets.
     *
     * @param sheetExpansion position the hosting shtorka is settling towards
     * @param onExpandSheet raises the hosting shtorka to full; invoked when the search bar is focused
     */
    @Composable
    fun Header(sheetExpansion: State<ShtorkaExpansion>, onExpandSheet: () -> Unit, modifier: Modifier)

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

        /** Height of the top blocks row (a column of two 92dp small blocks / one large block). */
        val TopBlocksHeight: Dp = 192.dp

        /** Height of the tab row, including its vertical insets. */
        val TabRowHeight: Dp = 60.dp

        /** Gap above the tab row while the top blocks are shown; it closes as they collapse away. */
        val TabRowTopGap: Dp = 32.dp

        /**
         * Exact height of the semi-open layout below the host's sheet header: the top blocks and
         * the tab row, no tab content. The host sizes the shtorka's semi-open detent with it.
         */
        val SemiOpenContentHeight: Dp = TopBlocksHeight + TabRowTopGap + TabRowHeight
    }
}