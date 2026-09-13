package com.tangem.features.feed.nav

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState

/**
 * Contract of a feed stack screen. Screens render inside the feed content viewport — the host's
 * pinned chrome (grabber + sheet header) stays above them.
 */
interface FeedScreenComponent {

    /**
     * Navbar of the screen, rendered in the host's pinned sheet header in place of the feed's
     * search bar (typically a DS3 `TangemTopNavigation` with `WindowInsets(0)`). The default is
     * empty: screens whose header is the shared search bar (home, search) don't override it — the
     * host renders the search bar itself for those.
     *
     * @param bottomSheetState expanded/collapsed state of the hosting shtorka
     * @param onExpandSheet raises the hosting shtorka to full
     */
    @Composable
    fun Header(bottomSheetState: State<BottomSheetState>, onExpandSheet: () -> Unit, modifier: Modifier) {
    }

    /**
     * @param bottomSheetState expanded/collapsed state of the hosting shtorka
     * @param contentPadding insets of the host's pinned chrome (top: grabber + sheet header); the
     * scrollable content draws beneath it and offsets by it
     * @param onExpandSheet raises the hosting shtorka to full
     */
    @Composable
    fun Content(
        bottomSheetState: State<BottomSheetState>,
        contentPadding: PaddingValues,
        onExpandSheet: () -> Unit,
        modifier: Modifier,
    )
}