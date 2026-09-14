package com.tangem.features.feed.search.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.tangem.core.ui.components.haze.hazeSourceTangem
import com.tangem.core.ui.ds2.fade.TangemFade
import com.tangem.core.ui.res.LocalHazeState
import com.tangem.features.feed.search.ui.state.RecentUM
import com.tangem.features.feed.ui.FeedTabRow
import com.tangem.features.feed.ui.FeedTabUM
import com.tangem.features.feed.v2.FeedV2Component
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.collections.immutable.ImmutableList

/**
 * Shown while the query is blank. There is no tab row in this state — searching has not started, so
 * there is no scope to pick.
 */
@Composable
internal fun FeedSearchPlaceholder(recent: RecentUM?, contentPadding: PaddingValues, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = contentPadding.calculateTopPadding()),
    ) {
        // TODO: [TWI-1608] Suggested + Popular
        if (recent != null) {
            FeedSearchRecent(state = recent)
        }
    }
}

/**
 * Search results: the feed's tab row over the selected tab's own list. The row is the same set and
 * order as the feed home's and does not shrink when a tab finds nothing.
 *
 * @param tabContent the selected tab page, provided by the search screen's pager
 */
@Composable
internal fun FeedSearchTabsContent(
    tabs: ImmutableList<FeedTabUM>,
    selectedTabIndex: Int,
    onTabSelect: (Int) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    tabContent: @Composable (Modifier) -> Unit,
) {
    // A local haze source — LocalHazeState here would sample the screen behind the card instead.
    val outerHazeState = LocalHazeState.current
    val searchHazeState = rememberHazeState()
    SideEffect { searchHazeState.blurEnabled = outerHazeState.blurEnabled }

    Box(modifier = modifier.fillMaxSize()) {
        tabContent(
            Modifier
                .fillMaxSize()
                .hazeSourceTangem(state = searchHazeState),
        )
        CompositionLocalProvider(LocalHazeState provides searchHazeState) {
            TangemFade(
                position = TangemFade.Position.Top,
                variant = TangemFade.Variant.Soft,
                blur = true,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .height(contentPadding.calculateTopPadding() + FeedV2Component.TabRowHeight)
                    .hazeSourceTangem(state = searchHazeState, zIndex = 1f),
            )
            FeedTabRow(
                tabs = tabs,
                selectedTabIndex = selectedTabIndex,
                onTabSelect = onTabSelect,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = contentPadding.calculateTopPadding()),
            )
        }
    }
}